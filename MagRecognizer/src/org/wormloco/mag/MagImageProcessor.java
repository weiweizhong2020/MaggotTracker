/*
 * MagImageProcessor.java 	
 *
 */

package org.wormloco.mag;

import java.awt.*;
import ij.*;
import ij.io.*;
import ij.process.*;
import ij.plugin.filter.*;
import ij.gui.*;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedList;


/**
 * This class analyzes one worm image and recognize the spine (midline). The
 * spine is returned as 13 (x, y) points. It uses the ImageJ package.
 *
 * @author Weiwei Zhong (initial prototype) SangKyu Jung (improved and final version)
 */

public class MagImageProcessor {

    public ByteProcessor oriImage = null; //The original image	
    public ByteProcessor wormImage = null; //cropped out binary image of the worm particle
    public int worm_x0 = -1; //the x-cordinate of the worm image in regard to the original image
    public int worm_y0 = -1; //the y-cordinate of the worm image in regard to the original image
    public int[][] spine = null;
    //Spine is is a double array a[2][13]:  
    // a[0][0]-a[0][12]: the X-coordinates of the 13 points;
    // a[1][0]-a[1][12]: the Y-coordinates of the 13 points.
    //If we can't find the spine then it is null.  
    private NativeImgProcessing imgProc = new NativeImgProcessing();

    public MagImageProcessor(String filename ) {
        ImagePlus imp = new ImagePlus(filename);
        init(imp);
    }

    public MagImageProcessor(Image img ) {
        ImagePlus imp = new ImagePlus("dummy", img);
        init(imp);
    }

    /**
     * for testing purpose
     */
    public static void main(String[] args) {
        long time1 = System.nanoTime();
        for (int i = 0; i <= 35; i++) {
            String fileName = "image_" + i;
            MagImageProcessor wrm = new MagImageProcessor("sample_p1/" + fileName + ".jpg" );
            wrm.outputOverlayImage("overlay/" + fileName); //overlay the spine on the original image			

        }
        long time2 = System.nanoTime() - time1;
        double t = (double) time2 / 1000000000;
        System.out.print("\ttime:" + t + "\n");

        System.exit(0);
    }

    /**
     * Initializes the image; steps: remember original image (oriImage), calls
     * binarize, calls findWorm, calls findSpine
     *
     * @param imp the image-plus image
     */
    private void init(ImagePlus imp) {
        //load image, convert to 8-bit grayscale
        ImageConverter imgcvt = new ImageConverter(imp);
        imgcvt.convertToGray8();
        oriImage = (ByteProcessor) imp.getProcessor();

        //binarize image
        binarize(); //outputImage(wormImage, "C:/data/img0");

        //find worm particle
        findWorm();	//outputImage(wormImage, "C:/data/img1");

        //imgProc.saveImage(wormImage.getBufferedImage(),"bmp","C:\\Jung\\My Temp\\findWorm.bmp");
        if (wormImage == null) {
            return;
        }

        //find worm skeleton 
        findSpine();
    }

    /* convert the image to binary, it works on a duplicate of original image */
    void binarize() {
        wormImage = (ByteProcessor) oriImage.duplicate();

        //binarize
        wormImage.threshold(wormImage.getAutoThreshold());

        //The following steps will fill up internal holes 
        wormImage.dilate();
        wormImage.dilate();	//dilate to close gaps.
        Binary binFiller = new Binary();
        binFiller.setup("fill", null);
        binFiller.run(wormImage); //fill up holes
        wormImage.erode();
        wormImage.erode(); //restore to original size/shape	

        //remove small particles
        wormImage.erode();
        wormImage.erode();
        wormImage.dilate();
        wormImage.dilate();
    }

    /* Find the worm particle on a binary image (wormImage object).  The worm particle is defined as:
     1)does not touch the boundary of the image; 
     2)the biggest particle on the image.
     At the end, wormImage is a cropped image containing the worm.
     When no worm was found, wormImage is set to null.
     */
    void findWorm() {
        int wormArea = 0;
        Roi wormRoi = null;

        ByteProcessor binIp = (ByteProcessor) wormImage.duplicate();
        int width = binIp.getWidth();
        int height = binIp.getHeight();
        FloodFiller ff = new FloodFiller(binIp);
        Wand wand = new Wand(binIp);
        binIp.setColor(127);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (binIp.getPixel(x, y) > 0) {
                    continue; //move on if it is not a black (worm) pixel
                }
                //set the black particle as the region of interests
                wand.autoOutline(x, y, 0, 0);
                if (wand.npoints == 0) {
                    IJ.log("wand error: " + x + " " + y);
                    System.exit(1);
                }
                Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.TRACED_ROI);
                binIp.setRoi(roi);

                //if particle touches image boundry (within 10 pixels of boundary), discard particle
                Rectangle r = roi.getBounds();
                if (r.x < 10 || r.x > width - 10 || r.y < 10 || r.y > height - 10 || (r.x + r.width) > width - 10 || (r.y + r.height) > height - 10) {
                    ff.fill(x, y);
                    continue;
                }

                //If the particle is small, it is background; otherwise, it is worm 
                ImageStatistics stats = new ByteStatistics(binIp);
                if (stats.pixelCount < wormArea) {
                    ff.fill(x, y);
                    continue;
                }

                //update worm record
                ff.fill(x, y);
                wormArea = stats.pixelCount;
                wormRoi = roi;
            }
        }

        if (wormArea == 0) {
            wormImage = null;
            return; //a white page -> no worms
        }

        //set ROI to the worm particle
        wormImage.setRoi(wormRoi);
        Rectangle r = wormImage.getRoi();
        worm_x0 = r.x;
        worm_y0 = r.y;
        wormImage = (ByteProcessor) wormImage.crop();
    }
    

    /* find 13 equal-distance points along the worm skeleton (using wormImage object).
     * It fills the points of spine[2][13] (spine[0][] = x values, spine[1][] = y values)
     */
    void findSpine() {
        short colorOfSkeletonCurve = 0;
        short colorOfBackground = 255;

        ByteProcessor skeletonIp = (ByteProcessor) wormImage.duplicate();
        short[][] wormImgArray =
                imgProc.convert_Image_To_GrayShortArray(skeletonIp.getBufferedImage());

        //get the skeleton
        skeletonIp.skeletonize();

        
        // Convert skeletonIP to image and image array
        BufferedImage skeletonImg = skeletonIp.getBufferedImage();
        short[][] skeletonImgArray =
                imgProc.convert_Image_To_GrayShortArray(skeletonImg);

        
        // Create perfect skeleton curve
        skeletonImgArray = imgProc.createSingleSkeletonCurve(skeletonImgArray,
                wormImgArray, colorOfSkeletonCurve, colorOfBackground, 20, true);
                
        
        //Final check if skeleton curve is perfect
        if (imgProc.findBranchPoints(skeletonImgArray, 0, 3).length > 0
                || imgProc.findEndPoints(skeletonImgArray, 0).length != 2) {
            
            // no valid spline curve found
            return;
        }

        
        // Obtain spine points
        int spinePointCount = 13;
        spine = imgProc.get_SpinePoint(skeletonImgArray, spinePointCount, colorOfSkeletonCurve);
        for (int k=0; k < spine[0].length; k++) {
            spine[0][k] = spine[0][k] + worm_x0;
            spine[1][k] = spine[1][k] + worm_y0;
        }
    }

    
    /* find 13 equal-distance points along the worm skeleton (using wormImage object).
     * It fills the points of spine[2][13] (spine[0][] = x values, spine[1][] = y values)
     */
    void findSpine_old() {
        //get the skeleton
        ByteProcessor skeletonIp = (ByteProcessor) wormImage.duplicate();
        skeletonIp.skeletonize(); //outputImage(skeletonIp, "C:/data/img2");

        imgProc.saveImage(skeletonIp.getBufferedImage(), "bmp", "C:\\Jung\\My Temp\\skeletonIp.bmp");
        int[] ends = pruning(skeletonIp); //prune the skeleton to remove branches



        if (ends == null) {
            return;
        }

        //find the total length of the spine
        byte[] imgpixels = (byte[]) skeletonIp.getPixels();
        int len = 0;
        for (int i = 0; i < imgpixels.length; i++) {
            if (imgpixels[i] == (byte) 0) {
                len++;
            }
        }
        //System.out.println(len);

        //walk along the skeleton looking for 13 equal-distance points
        double step = (double) (len) / 12;
        int width = skeletonIp.getWidth();
        spine = new int[2][13];
        int currentPixel = ends[0];
        int distanceWalked = 0;

        for (int i = 1; i < 13; i++) {
            //record current point
            int yy = currentPixel / width;
            int xx = currentPixel - yy * width;
            spine[0][i - 1] = xx;
            spine[1][i - 1] = yy;

            int n_pixels = (int) (step * i - distanceWalked);

            int nextPixel = traceback(skeletonIp, currentPixel, n_pixels, true); //find the next point
            if (nextPixel == currentPixel) { //shouldn't get here.  This is for debugging.
                //System.out.println (skeletonIp.get(currentPixel));
                //System.out.println (i+":"+currentPixel+","+n_pixels+"\t"+xx+","+yy+" wrong! Shouldn't get here!");
                spine = null;
                return;
            }
            currentPixel = nextPixel; //walk to the next point
            distanceWalked += n_pixels;
        }

        int yy = ends[1] / width;
        int xx = ends[1] - yy * width;
        spine[0][12] = xx;
        spine[1][12] = yy;

        for (int i = 0; i < 13; i++) {
            spine[0][i] += worm_x0;
            spine[1][i] += worm_y0;
        }
    }

    static int countValidNeighborhoodPixel(short[][] srcGrayArray, int searchPixelGrayColor,
            int i, int j) {

        int clipwidth = srcGrayArray.length;
        int clipheight = srcGrayArray[0].length;
        int neighborhoodPixelCount = 0;



        if ((i - 1 >= 0) && (j - 1 >= 0)) {
            if (srcGrayArray[i - 1][j - 1] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }

        }

        if (j - 1 >= 0) {
            if (srcGrayArray[i][j - 1] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }
        }

        if ((i + 1 < clipwidth) && (j - 1 >= 0)) {
            if (srcGrayArray[i + 1][j - 1] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }
        }
        //-----------------------
        if (i - 1 >= 0) {
            if (srcGrayArray[i - 1][j] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }
        }

        if (i + 1 < clipwidth) {
            if (srcGrayArray[i + 1][j] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }
        }
        //------------------------
        if ((i - 1 >= 0) && (j + 1 < clipheight)) {
            if (srcGrayArray[i - 1][j + 1] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }
        }

        if (j + 1 < clipheight) {
            if (srcGrayArray[i][j + 1] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }
        }

        if ((i + 1 < clipwidth) && (j + 1 < clipheight)) {
            if (srcGrayArray[i + 1][j + 1] == searchPixelGrayColor) {
                neighborhoodPixelCount++;
            }

        }

        return neighborhoodPixelCount;
    }


    /**
     * return the list of end points in a give skel of worm
     *
     * @param skelWorm The binary skeleton of the worm
     * @param foreground The value of foreground pixel
     */
    public static LinkedList<int[]> findEndPoints(ByteProcessor skelWorm, int foreground) {
        int width = skelWorm.getWidth();
        int height = skelWorm.getHeight();
        LinkedList<int[]> endList = new LinkedList<int[]>();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (skelWorm.getPixel(i, j) == foreground) {
                    int starti = i - 1;
                    int startj = j - 1;
                    int endi = i + 1;
                    int endj = j + 1;
                    if (i == 0) {
                        starti = i;
                    }
                    if (i == width - 1) {
                        endi = width - 1;
                    }
                    if (j == 0) {
                        startj = j;
                    }
                    if (j == height - 1) {
                        endj = height - 1;
                    }
                    int sum = 0;
                    for (int m = starti; m <= endi; m++) {
                        for (int n = startj; n <= endj; n++) {
                            if (skelWorm.getPixel(m, n) == foreground) {
                                sum++;
                            }
                        }
                    }
                    if (sum == 2) {
                        int[] cord = {i, j};
                        endList.add(cord);
                    }
                }
            }//end of loop j
        }// end of loop i
        return endList;
    }


    /**
     * return the list of spur points in a give skel of worm
     *
     * @param skelWorm The binary skeleton of the worm
     * @param foreground The value of foreground pixel
     */
    public static LinkedList<int[]> reportSpurPoints(ByteProcessor skelWorm, int foreground) {
        int width = skelWorm.getWidth();
        int height = skelWorm.getHeight();
        LinkedList<int[]> cordList = new LinkedList<int[]>();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (skelWorm.getPixel(i, j) == foreground) {
                    int starti = i - 1;
                    int startj = j - 1;
                    int endi = i + 1;
                    int endj = j + 1;
                    if (i == 0) {
                        starti = i;
                    }
                    if (i == width - 1) {
                        endi = width - 1;
                    }
                    if (j == 0) {
                        startj = j;
                    }
                    if (j == height - 1) {
                        endj = height - 1;
                    }
                    int sum = 0;
                    for (int m = starti; m <= endi; m++) {
                        for (int n = startj; n <= endj; n++) {
                            if (skelWorm.getPixel(m, n) == foreground) {
                                sum++;
                            }
                        }
                    }
                    if (sum == 2 || sum == 1) {
                        int[] cord = {i, j};
                        cordList.add(cord);
                    }
                }
            }//end of loop j
        }// end of loop i
        return cordList;
    }

  
    //prune the skeleton, returns an array that stores the two end pixels
    //returns null if the number of ends is not 2 (Something is wrong.)
    int[] pruning(ByteProcessor skeletonIp) {
        //check for branches
        int[] ends = findEnds(skeletonIp);
        if (ends == null) {
            return null;
        }
        int endCount = ends.length;

        //there are branches, prune them.
        int[] newEnds = new int[endCount];
        int newEndCount = 0;
        while (endCount > 2) {
            newEnds = new int[endCount];
            newEndCount = 0;
            for (int i = 0; i < endCount; i++) {
                int newPt = traceback(skeletonIp, ends[i], 1, true); //erase the branch by 1 pixel
                if (newPt == ends[i]) {
                    continue;
                }
                newEnds[newEndCount] = newPt; //mark the new end				
                newEndCount++;
            }
            ends = newEnds;
            endCount = newEndCount;
        }
        if (endCount != 2) {
            return null;  //Something is wrong. Maybe the animal formed a circle?
        }
        //extend trimmed skeleton to particle boundry
        newEnds = new int[2];
        for (int i = 0; i < 2; i++) {
            int y1 = ends[i] / skeletonIp.getWidth();
            int x1 = ends[i] - y1 * skeletonIp.getWidth();

            //trace back 20 pixels and project a line to the boundry
            int startPt = traceback(skeletonIp, ends[i], 20, false);
            if (startPt == ends[i]) {
                newEnds[i] = ends[i];
                continue;
            }

            //set the trajectory
            int y0 = startPt / skeletonIp.getWidth();
            int x0 = startPt - y0 * skeletonIp.getWidth();

            double dx = ((double) (x1 - x0)) / 20;
            double dy = ((double) (y1 - y0)) / 20;

            double xdbl = (double) x1;
            int x = (int) xdbl;
            double ydbl = (double) y1;
            int y = (int) ydbl;

            //mark along the trajectory till it reaches the worm particle boundry or the image boundry
            while (x >= 0 && x < skeletonIp.getWidth() && y >= 0 && y < skeletonIp.getHeight() && wormImage.get(x, y) == 0) {
                newEnds[i] = y * skeletonIp.getWidth() + x;
                skeletonIp.set(newEnds[i], 0);
                xdbl += dx;
                ydbl += dy;
                x = (int) xdbl;
                y = (int) ydbl;

            }
        }
        return newEnds;
    }


    //find branch ends of a skeleton image.  Branch end is a black pixel that has one black neighbor pixel.
    int[] findEnds(ByteProcessor skeletonIp) {
        byte[] imgpixels = (byte[]) skeletonIp.getPixels();
        int[] ends = new int[imgpixels.length];
        int endCount = 0;
        for (int i = 0; i < imgpixels.length; i++) {
            if (imgpixels[i] != (byte) 0) {
                continue;
            }
            int pixelType = pixelType(skeletonIp, i);

            if (pixelType == 0) {
                imgpixels[i] = (byte) 255;
                continue; //background
            }

            if (pixelType == 1) { //an end pixel
                ends[endCount] = i;
                endCount++;
                continue;
            }
        }
        if (endCount == 0) {
            return null;
        }
        int[] newEnds = new int[endCount];
        for (int i = 0; i < endCount; i++) {
            newEnds[i] = ends[i];
        }
        return newEnds;
    }


    //0-background pixel; 
    //1-end pixel: a pixel that is linked to only one pixel; 
    //2-special end pixel (think of 3 pixels arranged like "L", the two ends there have 2 black neighbors) 
    //3-link pixel
    int pixelType(ByteProcessor skeletonIp, int i) {
        byte[] imgpixels = (byte[]) skeletonIp.getPixels();
        if (imgpixels[i] != (byte) 0) {
            return 0;
        }

        int[] blackNeighbors = findNeighbors(skeletonIp, i, 0);
        if (blackNeighbors == null) {
            return 0; //a lone pixel, useless, mark it as background
        }
        if (blackNeighbors.length == 1) {
            return 1; //it is linked to only one pixel, so it is an end pixel
        }
        /*Delete this pixel,
         if that creates >1 ends, it is a link pixel
         if that creates 1 end, it is a special end pixel.*/
        imgpixels[i] = (byte) 255;  //delete the pixel
        int ends = 0;
        for (int j = 0; j < blackNeighbors.length; j++) { //count # of ends created
            int[] blackNeighbors1 = findNeighbors(skeletonIp, blackNeighbors[j], 0);
            if (blackNeighbors1 == null || blackNeighbors1.length == 1) {
                ends++;
            }
        }
        imgpixels[i] = (byte) 0; //restore the pixel

        if (ends == 1) {
            return 2; //created 1 end: it is a special end pixel.
        }
        if (ends == 0) {
            return 0; //deletion of this pixel doesn't break anything, we treat it as background
        }
        return 3; //created >1 ends: it is a link pixel.	
    }


    //start from the end of the line, trace along the line for n pixels (either erase or keep the traced part of the line), return the new end position 
    int traceback(ByteProcessor skeletonIp, int startPt, int n_pixels, boolean erase) {
        int endPt = startPt;
        int marker = 255;
        if (!erase) {
            marker = 100;
        }
        while (n_pixels > 0) {
            if (skeletonIp.get(endPt) != 0) {
                break;
            }

            skeletonIp.set(endPt, marker); //mark off end
            int[] blackNeighbors = findNeighbors(skeletonIp, endPt, 0); //find the next pixel on the line
            if (blackNeighbors == null) {
                break; //no more to follow
            }
            if (blackNeighbors.length == 1) {
                endPt = blackNeighbors[0]; //mark the new end
                n_pixels--;
                continue;
            }

            int foundNextEnd = 0;
            for (int i = 0; i < blackNeighbors.length; i++) { //check which pixel to follow 
                int t = pixelType(skeletonIp, blackNeighbors[i]);
                if (t == 0) {
                    skeletonIp.set(blackNeighbors[i], marker);
                }
                if (t == 1 || t == 2) {//found the next end pixel
                    endPt = blackNeighbors[i]; //mark the new end
                    foundNextEnd++;
                }
            }

            if (foundNextEnd == 1) {
                n_pixels--;//walk one pixel
                continue;
            }
            if (foundNextEnd == 0) {
                break; //no more to follow
            }
            //shouldn't get here
            skeletonIp.set(endPt, 0); //restore
            break;
        }
        if (erase) {
            return endPt;
        }

        //restore the line, change the marker back
        int restorePt = startPt;
        while (restorePt != endPt) {
            if (skeletonIp.get(restorePt) != 100) {
                break;
            }
            skeletonIp.set(restorePt, 0); //restore
            int[] blackNeighbors = findNeighbors(skeletonIp, restorePt, 100); //find the next pixel 
            if (blackNeighbors == null || blackNeighbors.length > 1) {
                break;
            }
            restorePt = blackNeighbors[0]; //walk one pixel			
        }
        return endPt;
    }


    //Find neighbors of a specific color
    int[] findNeighbors(ByteProcessor bp, int pixelIndex, int color) {
        int width = bp.getWidth();
        int height = bp.getHeight();
        byte[] imgpixels = (byte[]) bp.getPixels();
        int[] neighbors = new int[8];
        int c = 0;

        int y = pixelIndex / width;
        int x = pixelIndex - y * width;
        if (x != 0 && imgpixels[pixelIndex - 1] == (byte) color) {
            neighbors[c] = pixelIndex - 1;
            c++;
        }
        if (x != width - 1 && imgpixels[pixelIndex + 1] == (byte) color) {
            neighbors[c] = pixelIndex + 1;
            c++;
        }
        if (y != 0 && x != 0 && imgpixels[pixelIndex - width - 1] == (byte) color) {
            neighbors[c] = pixelIndex - width - 1;
            c++;
        }
        if (y != 0 && imgpixels[pixelIndex - width] == (byte) color) {
            neighbors[c] = pixelIndex - width;
            c++;
        }
        if (y != 0 && x != width - 1 && imgpixels[pixelIndex - width + 1] == (byte) color) {
            neighbors[c] = pixelIndex - width + 1;
            c++;
        }
        if (y != height - 1 && x != 0 && imgpixels[pixelIndex + width - 1] == (byte) color) {
            neighbors[c] = pixelIndex + width - 1;
            c++;
        }
        if (y != height - 1 && imgpixels[pixelIndex + width] == (byte) color) {
            neighbors[c] = pixelIndex + width;
            c++;
        }
        if (y != height - 1 && x != width - 1 && imgpixels[pixelIndex + width + 1] == (byte) color) {
            neighbors[c] = pixelIndex + width + 1;
            c++;
        }
        if (c == 0) {
            return null;
        }

        int[] neighbors2 = new int[c];
        for (int i = 0; i < c; i++) {
            neighbors2[i] = neighbors[i];
        }
        return neighbors2;
    }


    /**
     * Writes an image to a specified filename (jpeg extension is added when
     * needed)
     *
     * @param ip the image-processor
     * @param filename the file name
     */
    public void outputImage(ImageProcessor ip, String filename) {
        ImagePlus imgPls = new ImagePlus(null, ip);
        FileSaver fs = new FileSaver(imgPls);
        if (filename.indexOf(".jpg") < 0 && filename.indexOf(".JPG") < 0) {
            filename = filename + ".jpg";
        }; // if
        fs.saveAsJpeg(filename);
    }
    

	/**
	 * Super-impose the spine points (red) on the original image, and then writes the image to a filename
	 * @param  overlayImage  the name given to the file (jpg default) that will have overlay points 
	 */
    public void outputOverlayImage(String overlayImage) {
        if (spine == null) {
            outputImage(oriImage, overlayImage); //no valid spine
            return;
        }

        //load original image, convert to RGB
        ImagePlus imp = new ImagePlus("dummy", oriImage);
        ImageConverter imgcvt = new ImageConverter(imp);
        imgcvt.convertToRGB();
        ImageProcessor ip = imp.getProcessor();
        ip.setColor(Color.red);

        //draw a red circle at each spine pont
        int x = 0, y = 0;
        for (int i = 0; i < spine[0].length; i++) {
            x = (int) (spine[0][i] + 0.5); //points[][] is double, round it up to the closest integer (pixel)
            y = (int) (spine[1][i] + 0.5);
            if (i == 0) {
                ip.moveTo(x, y);
            } else {
                ip.lineTo(x, y);   //draw a line from previous point
            }
            ip.drawOval(x - 2, y - 2, 5, 5); //draw a circle of radius 5, centered at the point 
            ip.moveTo(x, y);
        }
        ip.drawString(x + " , " + y + " (point " + spine[ 0].length + ")", x, (y + 20));
        x = (int) (spine[ 0][ 0] + 0.5);
        y = (int) (spine[ 1][ 0] + 0.5);
        ip.drawString(x + " , " + y + " (point " + 1 + ")", x, (y - 20));
        outputImage(ip, overlayImage);
    }

}

