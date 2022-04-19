/*
 * NativeImgProcessing.java
 *
 */
package org.wormloco.mag;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * This class contains native image processing modules
 */

public class NativeImgProcessing {

    LinkedList<Point> FloodFillQue = new LinkedList<Point>();
    //RegionLabeling_ColorTable: The first dimension for IDnumber (starts from 1)
    //                           The second dimension for color value
    //                                      0:R, 1:G, 2:B
    public short[][] RegionLabeling_ColorTable;
    public short[][] RegionLabeling_LabelIDMap;
    public float[][] LUT_RGB_To_PartialGray = new float[4][256];
    public boolean[][] _FloodFilledDoneMap = new boolean[1][1];
    public BufferedImage MagicThreshold_BWImage;
    public BufferedImage AdaptiveThreshold_BWImage;
    public short[][] AdaptiveThreshold_BWPixelArray;
    public int MagicThreshold_OptimalThresholdValue;
    public int AdaptiveThreshold_OptimalThresholdValue;
    public final float AdaptiveThreshold_tPercent = 0.3f;
    public LineMath lineMath = new LineMath();

    public NativeImgProcessing() {
        int i;

        //Generate LUT for gray scaling
        //Formula: Gray=(0.299 * r) + (0.587 * g) + (0.114 * b)
        for (i = 0; i <= 255; i++) {
            LUT_RGB_To_PartialGray[1][i] = (float) (0.299 * i);
            LUT_RGB_To_PartialGray[2][i] = (float) (0.587 * i);
            LUT_RGB_To_PartialGray[3][i] = (float) (0.114 * i);
        }
    }

    /**
     * Load source image
     *
     * @param ref source file name including full path
     * @return BufferedImage
     */
    public BufferedImage loadImage(String ref) {
        BufferedImage bimg;
        try {

            bimg = ImageIO.read(new File(ref));
        } catch (Exception e) {
            bimg = null;
        }
        return bimg;
    }

    /**
     * Save source image to destination
     *
     * @param srcImage source image
     * @param fileName file name including full path
     */
    public void saveImage(BufferedImage srcImage, String imgFormat, String fileName) {
        try {
            ImageIO.write(srcImage, imgFormat, new File(fileName));
        } catch (IOException e) {
            //do nothing
        }
    }

    //Resize image
    public BufferedImage resizeImage(Image originalImage, int type,
            Integer img_width, Integer img_height) {
        BufferedImage resizedImage = new BufferedImage(img_width, img_height, type);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, img_width, img_height, null);
        g.dispose();

        return resizedImage;
    }

    /**
     * Convert RGB array to image
     *
     * @param SrcRGBShortArray source RGB array
     * @return BufferedImage
     */
    public BufferedImage convert_RGBShortArray_To_Image(short[][][] SrcRGBShortArray) {
        int x_upperBound = SrcRGBShortArray.length - 1;
        int y_upperBound = SrcRGBShortArray[0].length - 1;
        int rgb;


        BufferedImage outImage = new BufferedImage(x_upperBound + 1,
                y_upperBound + 1, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y <= y_upperBound; y++) {
            for (int x = 0; x <= x_upperBound; x++) {
                rgb = new Color(SrcRGBShortArray[x][y][0],
                        SrcRGBShortArray[x][y][1],
                        SrcRGBShortArray[x][y][2]).getRGB();
                outImage.setRGB(x, y, rgb);
            }
        }
        return outImage;
    }

    /**
     * Get RGB array from image
     *
     * @param SrcImage source image
     * @return output array
     */
    public short[][][] convert_Image_To_RGBShortArray(BufferedImage SrcImage) {
        int x_upperBound = SrcImage.getWidth();
        int y_upperBound = SrcImage.getHeight();
        int red, green, blue;


        short[][][] outShortArray = new short[x_upperBound][y_upperBound][3];

        for (int y = 0; y < y_upperBound; y++) {
            for (int x = 0; x < x_upperBound; x++) {
                int rgb = SrcImage.getRGB(x, y);
                red = (rgb >> 16) & 0x000000FF;
                green = (rgb >> 8) & 0x000000FF;
                blue = (rgb) & 0x000000FF;

                outShortArray[x][y][0] = (short) red;
                outShortArray[x][y][1] = (short) green;
                outShortArray[x][y][2] = (short) blue;
            }
        }
        return outShortArray;
    }

    /**
     * Convert gray array to image
     *
     * @param SrcGrayShortArray source array
     * @return BufferedImage
     */
    public BufferedImage convert_GrayShortArray_To_Image(short[][] SrcGrayShortArray) {
        int x_upperBound = SrcGrayShortArray.length - 1;
        int y_upperBound = SrcGrayShortArray[0].length - 1;
        int rgb;
        short grayValue;

        BufferedImage outImage = new BufferedImage(x_upperBound + 1,
                y_upperBound + 1, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y <= y_upperBound; y++) {
            for (int x = 0; x <= x_upperBound; x++) {
                grayValue = SrcGrayShortArray[x][y];
                if (grayValue > 255) {
                    grayValue = 255;
                } else {
                    if (grayValue < 0) {
                        grayValue = 0;
                    }
                }

                rgb = new Color(grayValue, grayValue,
                        grayValue).getRGB();
                outImage.setRGB(x, y, rgb);
            }
        }
        return outImage;
    }

    /**
     * Convert RGB to Gray value
     *
     * @return BufferedImage
     */
    public short RGBToGray(int r, int g, int b) {
        return (short) Math.rint(LUT_RGB_To_PartialGray[1][r]
                + LUT_RGB_To_PartialGray[2][g]
                + LUT_RGB_To_PartialGray[3][b]);
    }

    /**
     * Convert gray array to RGB array
     *
     * @param SrcGrayShortArray source array
     * @return short array
     */
    public short[][][] convert_GrayShortArray_To_RGBShortArray(short[][] SrcGrayShortArray) {
        int x_upperBound = SrcGrayShortArray.length - 1;
        int y_upperBound = SrcGrayShortArray[0].length - 1;

        short[][][] OutRGBShortArray =
                new short[SrcGrayShortArray.length][SrcGrayShortArray[0].length][3];


        for (int y = 0; y <= y_upperBound; y++) {
            for (int x = 0; x <= x_upperBound; x++) {
                OutRGBShortArray[x][y][0] = SrcGrayShortArray[x][y];
                OutRGBShortArray[x][y][1] = SrcGrayShortArray[x][y];
                OutRGBShortArray[x][y][2] = SrcGrayShortArray[x][y];
            }
        }

        return OutRGBShortArray;
    }

    /**
     * Convert RGB array to gray array
     *
     * @param SrcRGBShortArray source RGB array
     * @return Gray array
     */
    public short[][] convert_RGBShortArray_To_GrayShortArray(short[][][] SrcRGBShortArray) {
        int x_upperBound = SrcRGBShortArray.length - 1;
        int y_upperBound = SrcRGBShortArray[0].length - 1;

        short[][] OutGrayShortArray =
                new short[SrcRGBShortArray.length][SrcRGBShortArray[0].length];


        for (int y = 0; y <= y_upperBound; y++) {
            for (int x = 0; x <= x_upperBound; x++) {
                OutGrayShortArray[x][y] = RGBToGray(SrcRGBShortArray[x][y][0],
                        SrcRGBShortArray[x][y][1], SrcRGBShortArray[x][y][2]);
            }
        }

        return OutGrayShortArray;
    }

    /**
     * Convert image to gray array
     *
     * @param SrcImage source image
     * @return Gray array
     */
    public short[][] convert_Image_To_GrayShortArray(BufferedImage SrcImage) {
        int x_upperBound = SrcImage.getWidth();
        int y_upperBound = SrcImage.getHeight();
        int rgb, red, green, blue;


        short[][] outShortArray = new short[x_upperBound][y_upperBound];

        for (int y = 0; y < y_upperBound; y++) {
            for (int x = 0; x < x_upperBound; x++) {
                rgb = SrcImage.getRGB(x, y);
                red = (rgb >> 16) & 0x000000FF;
                green = (rgb >> 8) & 0x000000FF;
                blue = (rgb) & 0x000000FF;

                outShortArray[x][y] = RGBToGray(red, green, blue);
            }
        }
        return outShortArray;
    }

    /**
     * copy of multiple array, what we call deepCopy
     *
     * @param source source array
     * @return array
     */
    public short[][] multiArrayCopy(short[][] source) {
        short[][] destination = new short[source.length][source[0].length];

        for (int a = 0; a < source.length; a++) {
            System.arraycopy(source[a], 0, destination[a], 0, source[a].length);
        }

        return destination;
    }

    public int[][] multiArrayCopy(int[][] source) {
        int[][] destination = new int[source.length][source[0].length];

        for (int a = 0; a < source.length; a++) {
            System.arraycopy(source[a], 0, destination[a], 0, source[a].length);
        }

        return destination;
    }

    /**
     * copy of multiple array, what we call deepCopy
     *
     * @param source source array
     * @return array
     */
    public short[][][] multiArrayCopy(short[][][] source) {
        short[][][] destination = new short[source.length][source[0].length][source[0][0].length];

        for (int a = 0; a < source.length; a++) {
            for (int b = 0; a < source[0].length; a++) {
                System.arraycopy(source[a][b], 0, destination[a][b], 0, source[a][b].length);
            }
        }

        return destination;
    }

    public int[][][] multiArrayCopy(int[][][] source) {
        int[][][] destination = new int[source.length][source[0].length][source[0][0].length];

        for (int a = 0; a < source.length; a++) {
            for (int b = 0; a < source[0].length; a++) {
                System.arraycopy(source[a][b], 0, destination[a][b], 0, source[a][b].length);
            }
        }

        return destination;
    }

    /**
     * gap filling
     *
     * @param SrcGrayShortArray source array
     * @return array
     */
    public short[][] gapFilling(short[][] SrcGrayShortArray) {
        int SrcImgWidth = SrcGrayShortArray.length - 1;
        int SrcImgHeight = SrcGrayShortArray[0].length - 1;
        short[][] outPixels = multiArrayCopy(SrcGrayShortArray);


        for (int y = 1; y <= SrcImgHeight - 1; y++) {
            for (int x = 1; x <= SrcImgWidth - 1; x++) {
                if (outPixels[x][y] == 0) {
                    if (outPixels[x][y - 1] == 255 && outPixels[x][y + 1] == 255) {
                        outPixels[x][y] = 255;
                    }

                    if (outPixels[x - 1][y] == 255 && outPixels[x + 1][y] == 255) {
                        outPixels[x][y] = 255;
                    }
                }
            }
        }

        return outPixels;
    }

    /**
     * flood filling
     *
     * @param SrcGrayShortArray source array
     * @param CenterX center x
     * @param CenterY center y
     * @return array
     */
    public short[][] floodFill(short[][] SrcGrayShortArray, int CenterX, int CenterY,
            short FillColorG, int ToleranceG) {
        int SrcImageWidth = SrcGrayShortArray.length;
        int SrcImageHeight = SrcGrayShortArray[0].length;
        short[][] outArray;

        _FloodFilledDoneMap = new boolean[SrcImageWidth][SrcImageHeight];


        outArray = floodFill_Core(SrcGrayShortArray, CenterX, CenterY, FillColorG, ToleranceG);

        return outArray;

    }

    /**
     * flood filling, core routine
     *
     * @param SrcGrayShortArray source array
     * @param CenterX center x
     * @param CenterY center y
     * @param FillColorG fill color in gray
     * @param ToleranceG tolerance
     * @return array
     */
    public short[][] floodFill_Core(short[][] SrcGrayShortArray, int CenterX, int CenterY,
            short FillColorG, int ToleranceG) {


        int SrcImageWidth = SrcGrayShortArray.length;
        int SrcImageHeight = SrcGrayShortArray[0].length;


        Point Cur_Location = new Point();
        Point Upper_Location;
        Point Lower_Location;
        Point Left_Location;
        Point Right_Location;
        int CenterColor_G;

        boolean[][] QueueMap = new boolean[SrcImageWidth][SrcImageHeight];

        short[][] outPixels = multiArrayCopy(SrcGrayShortArray);


        Cur_Location.x = CenterX;
        Cur_Location.y = CenterY;
        FloodFillQue.add(Cur_Location);
        QueueMap[Cur_Location.x][Cur_Location.y] = true;
        _FloodFilledDoneMap[Cur_Location.x][Cur_Location.y] = true;

        CenterColor_G = SrcGrayShortArray[Cur_Location.x][Cur_Location.y];

        do {
            Cur_Location.x = FloodFillQue.peek().x;
            Cur_Location.y = FloodFillQue.peek().y;

            outPixels[Cur_Location.x][Cur_Location.y] = FillColorG;
            _FloodFilledDoneMap[Cur_Location.x][Cur_Location.y] = true;



            FloodFillQue.poll();
            QueueMap[Cur_Location.x][Cur_Location.y] = false;


            Upper_Location = new Point(Cur_Location.x, Cur_Location.y - 1);
            Lower_Location = new Point(Cur_Location.x, Cur_Location.y + 1);
            Left_Location = new Point(Cur_Location.x - 1, Cur_Location.y);
            Right_Location = new Point(Cur_Location.x + 1, Cur_Location.y);



            if (Cur_Location.y == 0) {
                Upper_Location.x = -1;
            }

            if (Cur_Location.y == SrcImageHeight - 1) {
                Lower_Location.x = -1;
            }

            if (Cur_Location.x == 0) {
                Left_Location.x = -1;
            }

            if (Cur_Location.x == SrcImageWidth - 1) {
                Right_Location.x = -1;
            }

            if (Upper_Location.x != -1) {
                if (QueueMap[Upper_Location.x][Upper_Location.y] == false) {
                    if (_FloodFilledDoneMap[Upper_Location.x][Upper_Location.y] == false
                            && Math.abs(CenterColor_G
                            - SrcGrayShortArray[Upper_Location.x][Upper_Location.y]) <= ToleranceG) {
                        FloodFillQue.add(Upper_Location);
                        QueueMap[Upper_Location.x][Upper_Location.y] = true;
                    }
                }
            }


            if (Lower_Location.x != -1) {
                if (QueueMap[Lower_Location.x][Lower_Location.y] == false) {
                    if (_FloodFilledDoneMap[Lower_Location.x][Lower_Location.y] == false
                            && Math.abs(CenterColor_G
                            - SrcGrayShortArray[Lower_Location.x][Lower_Location.y]) <= ToleranceG) {
                        FloodFillQue.add(Lower_Location);
                        QueueMap[Lower_Location.x][Lower_Location.y] = true;
                    }
                }
            }


            if (Right_Location.x != -1) {
                if (QueueMap[Right_Location.x][Right_Location.y] == false) {
                    if (_FloodFilledDoneMap[Right_Location.x][Right_Location.y] == false
                            && Math.abs(CenterColor_G
                            - SrcGrayShortArray[Right_Location.x][Right_Location.y]) <= ToleranceG) {
                        FloodFillQue.add(Right_Location);
                        QueueMap[Right_Location.x][Right_Location.y] = true;
                    }
                }
            }

            if (Left_Location.x != -1) {
                if (QueueMap[Left_Location.x][Left_Location.y] == false) {
                    if (_FloodFilledDoneMap[Left_Location.x][Left_Location.y] == false
                            && Math.abs(CenterColor_G
                            - SrcGrayShortArray[Left_Location.x][Left_Location.y]) <= ToleranceG) {
                        FloodFillQue.add(Left_Location);
                        QueueMap[Left_Location.x][Left_Location.y] = true;
                    }
                }
            }


        } while (FloodFillQue.size() != 0);


        return outPixels;
    }

    /**
     * negative (invert)
     *
     * @param SrcGrayShortArray source array
     * @return array
     */
    public short[][] negative_UsingGrayShortArray(short[][] SrcGrayShortArray) {
        int SrcImgWidth = SrcGrayShortArray.length - 1;
        int SrcImgHeight = SrcGrayShortArray[0].length - 1;
        short[][] outPixels = multiArrayCopy(SrcGrayShortArray);


        for (int y = 0; y <= SrcImgHeight; y++) {
            for (int x = 0; x <= SrcImgWidth; x++) {
                outPixels[x][y] = (short) (255 - outPixels[x][y]);
            }
        }

        return outPixels;
    }

    /**
     * negative (invert)
     *
     * @param SrcRGBShortArray RGB source array
     * @return array
     */
    public short[][][] negative_UsingRGBShortArray(short[][][] SrcRGBShortArray) {
        int SrcImgWidth = SrcRGBShortArray.length - 1;
        int SrcImgHeight = SrcRGBShortArray[0].length - 1;
        short[][][] outPixels = multiArrayCopy(SrcRGBShortArray);


        for (int y = 0; y <= SrcImgHeight; y++) {
            for (int x = 0; x <= SrcImgWidth; x++) {
                outPixels[x][y][0] = (short) (255 - outPixels[x][y][0]);
                outPixels[x][y][1] = (short) (255 - outPixels[x][y][1]);
                outPixels[x][y][2] = (short) (255 - outPixels[x][y][2]);
            }
        }

        return outPixels;
    }

    /**
     * Conduct region labeling
     *
     * @param SrcImage source image
     * @param BkGrayColor background gray color (0~255)
     * @return image
     */
    public BufferedImage regionExtract_RasterScanning(BufferedImage SrcImage, int BkGrayColor) {
        short[][] SrcPixels;

        SrcPixels = convert_Image_To_GrayShortArray(SrcImage);

        return regionExtract_RasterScanning(SrcPixels, BkGrayColor);
    }

    /**
     * Conduct region labeling
     *
     * @param SrcPixels gray array
     * @param BkGrayColor background gray color (0~255)
     * @return array in RGB
     */
    public BufferedImage regionExtract_RasterScanning(short[][] SrcPixels, int BkGrayColor) {
        short[][] OutPixels;
        int x, y;
        short[][][] OutPixels_C;

        OutPixels = multiArrayCopy(SrcPixels);


        int SrcBitmapWidth = SrcPixels.length;
        int SrcBitmapHeight = SrcPixels[0].length;
        Random randomGenerator = new Random();

        OutPixels_C = new short[SrcBitmapWidth][SrcBitmapHeight][3];


        short[] Linked1D;
        boolean[][] Linked2D;
        short CurrentID;
        int IDCountMax = 10000;
        short[][] IDMap = new short[SrcBitmapWidth][SrcBitmapHeight];
        int Pixel_Center;
        int Pixel_1;
        int Pixel_2;
        int Pixel_3;
        int Pixel_4;
        int BitmapHeightUpper;
        int BitmapWidthUpper;
        int BorderHeightUpper;
        int BorderWidthUpper;
        int BoxSizeHalf = 1;


        BitmapHeightUpper = SrcBitmapHeight - 1;
        BitmapWidthUpper = SrcBitmapWidth - 1;
        BorderHeightUpper = SrcBitmapHeight - BoxSizeHalf - 1;
        BorderWidthUpper = SrcBitmapWidth - BoxSizeHalf - 1;



        //Initialize IDmap
        for (y = 0; y <= BitmapHeightUpper; y++) {
            for (x = 0; x <= BitmapWidthUpper; x++) {
                IDMap[x][y] = 0;
            }
        }

        //First pass
        CurrentID = 0;
        for (y = BoxSizeHalf; y <= BorderHeightUpper; y++) {
            for (x = BoxSizeHalf; x <= BorderWidthUpper; x++) {

                if (SrcPixels[x][y] != BkGrayColor) {


                    Pixel_Center = IDMap[x][y];
                    Pixel_1 = IDMap[x - 1][y - 1];
                    Pixel_2 = IDMap[x][y - 1];
                    Pixel_3 = IDMap[x + 1][y - 1];
                    Pixel_4 = IDMap[x - 1][y];


                    if (Pixel_1 == 0 && Pixel_2 == 0 && Pixel_3 == 0 && Pixel_4 == 0) {

                        if (CurrentID < IDCountMax) {
                            CurrentID = (short) (CurrentID + 1);
                            IDMap[x][y] = CurrentID;
                        }
                    } else {
                        IDMap[x][y] =
                                (short) get_MinValue(Pixel_Center,
                                Pixel_1, Pixel_2, Pixel_3, Pixel_4);
                    }
                }
            }

        }

        int Repeat_Twice;
        int MinValue;
        Linked1D = new short[CurrentID + 1];
        Linked2D = new boolean[CurrentID + 1][CurrentID + 1];

        for (Repeat_Twice = 1; Repeat_Twice <= 2; Repeat_Twice++) {

            //Second pass and record their equivalence relationship
            for (y = 0; y <= BorderHeightUpper; y++) {
                for (x = 0; x <= BorderWidthUpper; x++) {

                    if (IDMap[x][y] != 0) {

                        Pixel_Center = IDMap[x][y];
                        Pixel_1 = IDMap[x + 1][y];
                        Pixel_2 = IDMap[x][y + 1];
                        Pixel_3 = IDMap[x + 1][y + 1];
                        Pixel_4 = IDMap[x - 1][y + 1];


                        Linked2D[Pixel_Center][Pixel_Center] = true;

                        if (Pixel_1 != 0) {
                            Linked2D[Pixel_Center][Pixel_1] = true;
                            Linked2D[Pixel_1][Pixel_Center] = true;
                        }
                        if (Pixel_2 != 0) {
                            Linked2D[Pixel_Center][Pixel_2] = true;
                            Linked2D[Pixel_2][Pixel_Center] = true;
                        }
                        if (Pixel_3 != 0) {
                            Linked2D[Pixel_Center][Pixel_3] = true;
                            Linked2D[Pixel_3][Pixel_Center] = true;
                        }
                        if (Pixel_4 != 0) {
                            Linked2D[Pixel_Center][Pixel_4] = true;
                            Linked2D[Pixel_4][Pixel_Center] = true;
                        }
                    }
                }

            }

            //Reduce equivalence relationship
            for (x = 1; x <= CurrentID; x++) {

                MinValue = x;
                Linked1D[x] = (short) x;

                for (y = x; y >= 1; y--) {

                    if (Linked2D[x][y]) {
                        if (MinValue > y) {
                            MinValue = y;
                        }
                    }
                }

                Linked1D[x] = Linked1D[MinValue];
            }



            //Remove equivalent relationship
            for (y = BoxSizeHalf; y <= BorderHeightUpper; y++) {
                for (x = BoxSizeHalf; x <= BorderWidthUpper; x++) {
                    IDMap[x][y] = Linked1D[IDMap[x][y]];
                }
            }
        }

        //Setup color code randomly
        int q;
        int w;
        boolean IsExistSameColor;
        short[][] ColorCodeTable = new short[CurrentID + 1][3];


        for (q = 1; q <= CurrentID; q++) {
            do {
                do {
                    ColorCodeTable[q][0] = (short) (randomGenerator.nextInt(255));
                    ColorCodeTable[q][1] = (short) (randomGenerator.nextInt(255));
                    ColorCodeTable[q][2] = (short) (randomGenerator.nextInt(255));
                } while (ColorCodeTable[q][0] == 0 && ColorCodeTable[q][1] == 0
                        && ColorCodeTable[q][2] == 0);


                IsExistSameColor = false;
                for (w = 1; w < q; w++) {
                    if (ColorCodeTable[q][0] == ColorCodeTable[w][0]
                            && ColorCodeTable[q][1] == ColorCodeTable[w][1]
                            && ColorCodeTable[q][2] == ColorCodeTable[w][2]) {
                        IsExistSameColor = true;
                        break;
                    }
                }


            } while (IsExistSameColor == true);
        }

        //Draw final image
        for (y = BoxSizeHalf; y <= BorderHeightUpper; y++) {
            for (x = BoxSizeHalf; x <= BorderWidthUpper; x++) {
                OutPixels[x][y] = ColorCodeTable[IDMap[x][y]][0];
                OutPixels_C[x][y][0] = ColorCodeTable[IDMap[x][y]][0];
                OutPixels_C[x][y][1] = ColorCodeTable[IDMap[x][y]][1];
                OutPixels_C[x][y][2] = ColorCodeTable[IDMap[x][y]][2];

            }
        }

        RegionLabeling_LabelIDMap = IDMap;
        RegionLabeling_ColorTable = ColorCodeTable;

        return convert_RGBShortArray_To_Image(OutPixels_C);
    }

    /**
     * find min value
     *
     */
    public final float get_MinValue(float Value1, float Value2,
            float Value3, float Value4, float Value5) {
        float CurMin;

        CurMin = get_MaxValue(Value1, Value2, Value3, Value4, Value5);
        //If CurMin <> 1 Then Stop
        if (Value1 != 0F) {
            CurMin = Math.min(CurMin, Value1);
        }

        if (Value2 != 0F) {
            CurMin = Math.min(CurMin, Value2);
        }

        if (Value3 != 0F) {
            CurMin = Math.min(CurMin, Value3);
        }

        if (Value4 != 0F) {
            CurMin = Math.min(CurMin, Value4);
        }

        if (Value5 != 0F) {
            CurMin = Math.min(CurMin, Value5);
        }

        return CurMin;
    }

    /**
     * find max value
     *
     */
    public final float get_MaxValue(float Value1, float Value2,
            float Value3, float Value4, float Value5) {
        float CurMax;

        CurMax = Value1;
        CurMax = Math.max(CurMax, Value2);
        CurMax = Math.max(CurMax, Value3);
        CurMax = Math.max(CurMax, Value4);
        CurMax = Math.max(CurMax, Value5);

        return CurMax;
    }

    /**
     * Conduct simple global thresholding
     *
     * @param SourceImage source image
     * @param ThresholdValue threshold value in gray (0~255)
     * @param IsInvertedImage if true, conduct inversion
     * @return image
     */
    public BufferedImage bwLeveling(BufferedImage SourceImage, int ThresholdValue,
            boolean IsInvertedImage) {

        short[] LUT_Digitization = new short[256];
        int i;
        short[][] SrcPixels, OutPixels;
        int SrcBitmapWidth = SourceImage.getWidth();
        int SrcBitmapHeight = SourceImage.getHeight();


        SrcPixels = convert_Image_To_GrayShortArray(SourceImage);
        OutPixels = multiArrayCopy(SrcPixels);



        if (ThresholdValue < 1) {
            ThresholdValue = 1;
        }
        if (ThresholdValue > 255) {
            ThresholdValue = 255;
        }


        //Generate LUT
        if (IsInvertedImage == false) {
            for (i = 0; i <= 255; i++) {
                if (i > ThresholdValue) {
                    LUT_Digitization[i] = 255;
                } else {
                    LUT_Digitization[i] = 0;
                }
            }
        } else {
            for (i = 0; i <= 255; i++) {
                if (i > ThresholdValue) {
                    LUT_Digitization[i] = 0;
                } else {
                    LUT_Digitization[i] = (short) 255;
                }
            }
        }

        short Gray;

        for (int y = 0; y <= SrcBitmapHeight - 1; y++) {
            for (int x = 0; x <= SrcBitmapWidth - 1; x++) {
                Gray = LUT_Digitization[SrcPixels[x][y]];
                OutPixels[x][y] = Gray;
            }
        }

        return convert_GrayShortArray_To_Image(OutPixels);
    }

    /**
     * Deep copy of image
     *
     * @param bi source image
     * @return image
     */
    public BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    /**
     * magic thresholding
     *
     * @return image
     */
    public BufferedImage magicThresholding_Core(BufferedImage SourceImage,
            int ExcludeGrayRange_Min, int ExcludeGrayRange_Max) {


        short[][] SrcPixels, OutPixels;
        int SrcBitmapWidth = SourceImage.getWidth();
        int SrcBitmapHeight = SourceImage.getHeight();


        SrcPixels = convert_Image_To_GrayShortArray(SourceImage);
        OutPixels = new short[SrcBitmapWidth][SrcBitmapHeight];


        _FloodFilledDoneMap = new boolean[SrcBitmapWidth][SrcBitmapHeight];
        int ToleranceOfExclusion = 20;

        for (int y = 0; y < SrcBitmapHeight; y++) {

            for (int x = 0; x < SrcBitmapWidth; x++) {


                if (_FloodFilledDoneMap[x][y] == false) {
                    if (SrcPixels[x][y] >= ExcludeGrayRange_Min
                            && SrcPixels[x][y] <= ExcludeGrayRange_Max) {

                        floodFill_Core(SrcPixels, x, y,
                                (short) 255, ToleranceOfExclusion);
                    }
                }

            }
        }


        for (int y = 0; y < SrcBitmapHeight; y++) {

            for (int x = 0; x < SrcBitmapWidth; x++) {
                if (_FloodFilledDoneMap[x][y]) {
                    OutPixels[x][y] = 0;
                } else {
                    OutPixels[x][y] = 255;
                }

            }

        }

        return convert_GrayShortArray_To_Image(OutPixels);
    }

    /**
     * Create integral image
     *
     * @param SrcGrayArrayMap source gray array
     * @return array
     */
    public long[][] build_IntegralGrayArrayMap(short[][] SrcGrayArrayMap) {
        int SrcBitmapWidth = SrcGrayArrayMap.length;
        int SrcBitmapHeight = SrcGrayArrayMap[0].length;


        long[][] IntegralGrayArrayMap = new long[SrcBitmapWidth][SrcBitmapHeight];

        int x1 = 0;
        int x2 = SrcBitmapWidth - 1;
        int y1 = 0;
        int y2 = SrcBitmapHeight - 1;



        IntegralGrayArrayMap[x1][y1] = SrcGrayArrayMap[x1][y1];
        for (int x = x1 + 1; x <= x2; x++) {
            IntegralGrayArrayMap[x][y1] = IntegralGrayArrayMap[x - 1][y1]
                    + SrcGrayArrayMap[x1][y1];
        }
        //Computing integral sum of the first column
        for (int y = y1 + 1; y <= y2; y++) {
            IntegralGrayArrayMap[x1][y] = IntegralGrayArrayMap[x1][y - 1]
                    + SrcGrayArrayMap[x1][y1];
        }

        //Computing integral sum for the rest of points
        for (int y = y1 + 1; y <= y2; y++) {
            for (int x = x1 + 1; x <= x2; x++) {
                IntegralGrayArrayMap[x][y] = SrcGrayArrayMap[x][y]
                        + IntegralGrayArrayMap[x - 1][y]
                        + IntegralGrayArrayMap[x][y - 1]
                        - IntegralGrayArrayMap[x - 1][y - 1];

            }
        }

        return IntegralGrayArrayMap;
    }

    /**
     * Calculate average gray value for given selection region
     *
     * @param IntegralGrayImageMap integral gray map
     * @param LocalBoxSize box size for processing
     * @param CenterX center of selection box
     * @param CenterY center of selection box
     * @return optimal threshold value
     */
    private int calculate_LocalGrayAvgValueUsingIntegralMap(long[][] IntegralGrayImageMap,
            int LocalBoxSize, int CenterX, int CenterY) {
        long LocalBoxSum = 0;
        int LocalBoxAvg = 0;
        int LocalBoxX1 = 0;
        int LocalBoxX2 = 0;
        int LocalBoxY1 = 0;
        int LocalBoxY2 = 0;
        int LocalBoxHalfSize = (int) ((LocalBoxSize - 1) / 2.0);
        int ImageWidth = 0;
        int ImageHeight = 0;

        ImageWidth = IntegralGrayImageMap.length - 1 + 1;
        ImageHeight = IntegralGrayImageMap[0].length - 1 + 1;

        LocalBoxX1 = Math.max(CenterX - LocalBoxHalfSize, 0);
        LocalBoxY1 = Math.max(CenterY - LocalBoxHalfSize, 0);

        LocalBoxX2 = Math.min(CenterX + LocalBoxHalfSize, ImageWidth - 1);
        LocalBoxY2 = Math.min(CenterY + LocalBoxHalfSize, ImageHeight - 1);


        LocalBoxSum = IntegralGrayImageMap[LocalBoxX2][LocalBoxY2];
        if (LocalBoxY1 - 1 >= 0) {
            LocalBoxSum -= IntegralGrayImageMap[LocalBoxX2][LocalBoxY1 - 1];
        }
        if (LocalBoxX1 - 1 >= 0) {
            LocalBoxSum -= IntegralGrayImageMap[LocalBoxX1 - 1][LocalBoxY2];
        }
        if (LocalBoxX1 - 1 >= 0 && LocalBoxY1 - 1 >= 0) {
            LocalBoxSum += IntegralGrayImageMap[LocalBoxX1 - 1][LocalBoxY1 - 1];
        }


        LocalBoxAvg = (int) (LocalBoxSum / (double) ((LocalBoxX2 - LocalBoxX1 + 1) * (LocalBoxY2 - LocalBoxY1 + 1)));

        return LocalBoxAvg;
    }

    /**
     * Conduct adaptive thresholding
     *
     * @param SrcPixels source gray image
     * @param boxSize box size for processing
     * @param tpercent t percent value for comparison between gray value and
     * average gray value of selected box
     * @param GrayLimit pixels whose gray value is higher than GrayLimit will be
     * excluded
     * @return optimal threshold value
     */
    public BufferedImage adaptiveThresholding_Core(short[][] SrcPixels,
            int boxSize, float tpercent,
            int GrayLimit) {


        short[][] OutPixels;
        int SrcBitmapWidth = SrcPixels.length;
        int SrcBitmapHeight = SrcPixels[0].length;

        OutPixels = new short[SrcBitmapWidth][SrcBitmapHeight];

        long[][] temp_IntegralGrayMap;
        int LocalGrayAvg = 0;
        float rPercentValue = (float) (1 - tpercent);

        temp_IntegralGrayMap = build_IntegralGrayArrayMap(SrcPixels);


        for (int y = 0; y < SrcBitmapHeight; y++) {
            for (int x = 0; x < SrcBitmapWidth; x++) {

                LocalGrayAvg = calculate_LocalGrayAvgValueUsingIntegralMap(
                        temp_IntegralGrayMap, boxSize, x, y);
                if ((SrcPixels[x][y] < LocalGrayAvg * rPercentValue) && (SrcPixels[x][y] < GrayLimit)) {
                    OutPixels[x][y] = 255;
                } else {
                    OutPixels[x][y] = 0;
                }

            }
        }
        AdaptiveThreshold_BWPixelArray = OutPixels;
        return convert_GrayShortArray_To_Image(OutPixels);
    }

    /**
     * Find optimal threshold value for magic thresholding Optimal threshold
     * value will be one of 100, 75, and 50.
     *
     * @param SourceGrayImage source gray image
     * @return optimal threshold value
     */
    public int find_OptimalThreshold_For_MagicThresholding(BufferedImage SourceGrayImage) {
        BufferedImage BWImage;
        int TestValue;

        float[][] BasicAnalysis;
        int NumbValidBlob;
        boolean IsGoodValueFound = false;

        BWImage = null;

        //Find optimal threshold valueL maximum three values will be tested (100,75, 50)
        for (TestValue = 100; TestValue >= 50; TestValue -= 25) {
            //Run magic threshold
            BWImage = magicThresholding_Core(SourceGrayImage, TestValue, 255);

            //conduct region lanbeling
            regionExtract_RasterScanning(BWImage, 0);

            //Conduct basic analysis of detected regions such as center x and y, size
            BasicAnalysis = regionExtract_BasicAnalysis(RegionLabeling_LabelIDMap,
                    RegionLabeling_ColorTable);


            //Count number of blobs whose size is higher than 20 pixels
            NumbValidBlob = 0;
            if (BasicAnalysis.length > 0) {
                for (int tt = 1; tt <= BasicAnalysis.length - 1; tt++) {
                    if (BasicAnalysis[tt][2] > 20) {
                        NumbValidBlob += 1;
                    }
                }
            }


            if (NumbValidBlob < 100) {
                MagicThreshold_BWImage = BWImage;
                return TestValue;
            }
        }


        if (IsGoodValueFound == false) {
            MagicThreshold_BWImage = BWImage;
            return 50;
        }

        return 50;
    }

    /**
     * Find optimal threshold value for adaptive thresholding Optimal threshold
     * value will be between 150 to 50 with step size -10.
     *
     * @param SourceGrayImage source gray image
     * @return optimal threshold value
     */
    public int find_OptimalThreshold_For_AdaptiveThresholding(BufferedImage SourceGrayImage) {
        short[][] SrcPixels = convert_Image_To_GrayShortArray(SourceGrayImage);
        int returnValue = find_OptimalThreshold_For_AdaptiveThresholding(SrcPixels);

        return returnValue;
    }

    /**
     * Find optimal threshold value for adaptive thresholding Optimal threshold
     * value will be between 150 to 50 with step size -10.
     *
     * @param SrcGrayPixelArray source gray array
     * @return optimal threshold value
     */
    public int find_OptimalThreshold_For_AdaptiveThresholding(short[][] SrcGrayPixelArray) {
        BufferedImage BWImage = null;

        int TestValue;
        float[][] BasicAnalysis;
        int NumbValidBlob;
        boolean IsGoodValueFound = false;


        //Find optimal threshold value
        for (TestValue = 150; TestValue >= 50; TestValue -= 25) {
            //Run magic threshold
            BWImage = adaptiveThresholding_Core(SrcGrayPixelArray, 15,
                    AdaptiveThreshold_tPercent, TestValue);

            //conduct region lanbeling
            regionExtract_RasterScanning(BWImage, 0);

            //Conduct basic analysis of detected regions such as center x and y, size
            BasicAnalysis = regionExtract_BasicAnalysis(RegionLabeling_LabelIDMap,
                    RegionLabeling_ColorTable);


            //Count number of blobs whose size is higher than 20 pixels
            NumbValidBlob = 0;
            if (BasicAnalysis.length > 0) {
                for (int tt = 1; tt <= BasicAnalysis.length - 1; tt++) {
                    if (BasicAnalysis[tt][2] > 20) {
                        NumbValidBlob += 1;
                    }
                }
            }


            if (NumbValidBlob < 100) {
                AdaptiveThreshold_BWImage = BWImage;
                return TestValue;
            }
        }


        if (IsGoodValueFound == false) {
            AdaptiveThreshold_BWImage = BWImage;
            return 50;
        }

        return 50;
    }

    /**
     * Analyze detected objects
     *
     * @param Src_IDMap Source IDMap
     * @param Src_ColorTable Source color table
     * @return 2D array
     */
    public float[][] regionExtract_BasicAnalysis(short[][] Src_IDMap, short[][] Src_ColorTable) {
        //Returning array of float[region ID][analysis value]
        // region ID: index starts from 1        
        // if analysis value=0,   center X
        //    analysis value=1,   center Y
        //    analysis value=2,   pixel count
        //    anlaysis value=3,   left X
        //    analysis value=4,   right X
        //    anlaysis value=5,   top Y
        //    analysis value=6,   bottom Y
        //    analysis value=7,   average radius
        //    analysis value=8,   validity
        //                         if value=0:OK,   if value=1:invalid)



        int RegionCount = Src_ColorTable.length - 1;
        float[][] ReturnArray = new float[RegionCount + 1][9];
        int ImageWidthUpperBound = Src_IDMap.length - 1;
        int ImageHeightUpperBound = Src_IDMap[0].length - 1;



        for (int y = 0; y <= ImageHeightUpperBound; y++) {
            for (int x = 0; x <= ImageWidthUpperBound; x++) {
                ReturnArray[Src_IDMap[x][y]][0] += x;
                ReturnArray[Src_IDMap[x][y]][1] += y;
                ReturnArray[Src_IDMap[x][y]][2] += 1;

                if (ReturnArray[Src_IDMap[x][y]][3] > x) {
                    ReturnArray[Src_IDMap[x][y]][3] = x;
                }
                if (ReturnArray[Src_IDMap[x][y]][5] > y) {
                    ReturnArray[Src_IDMap[x][y]][5] = y;
                }
                if (ReturnArray[Src_IDMap[x][y]][4] < x) {
                    ReturnArray[Src_IDMap[x][y]][4] = x;
                }
                if (ReturnArray[Src_IDMap[x][y]][6] < y) {
                    ReturnArray[Src_IDMap[x][y]][6] = y;
                }
            }
        }



        //Calculating average value
        for (int q = 1; q <= RegionCount; q++) {

            if (ReturnArray[q][2] > 0) {
                ReturnArray[q][0] = (int) (ReturnArray[q][0] / ReturnArray[q][2]);
                ReturnArray[q][1] = (int) (ReturnArray[q][1] / ReturnArray[q][2]);
            }
        }


        //Calculating average radius

        for (int q = 1; q <= RegionCount; q++) {

            ReturnArray[q][7] = 0;
            for (int x = (int) ReturnArray[q][3]; x <= (int) ReturnArray[q][4]; x++) {
                for (int y = (int) ReturnArray[q][5]; y <= (int) ReturnArray[q][6]; y++) {
                    if (Src_IDMap[x][y] == q) {
                        ReturnArray[q][7] = ReturnArray[q][7]
                                + (float) Math.sqrt(
                                (ReturnArray[q][0] - x) * (ReturnArray[q][0] - x)
                                + (ReturnArray[q][1] - y) * (ReturnArray[q][1] - y));
                    }
                }
            }
            ReturnArray[q][7] = ReturnArray[q][7] / ReturnArray[q][2];
        }

        return ReturnArray;
    }

    /**
     * Conduct magic thresholding
     *
     * @param IsConductBlurring not used. Keep this parameter for future
     * implementation
     * @param IsFindThresholdValue set true to find optimal threshold value
     * @param DefaultThresholdValue if IsFindThresholdValue is false, set this
     * value.
     * @return Black and white image
     */
    public final BufferedImage do_MagicThresholding(BufferedImage SourceGrayImage,
            boolean IsConductBlurring, boolean IsFindThresholdValue, int DefaultThresholdValue) {

        BufferedImage BWImage = null;

        if (IsFindThresholdValue) {
            MagicThreshold_OptimalThresholdValue = find_OptimalThreshold_For_MagicThresholding(
                    SourceGrayImage);
            BWImage = deepCopy(MagicThreshold_BWImage);
        } else {
            BWImage = magicThresholding_Core(SourceGrayImage, DefaultThresholdValue, 255);
        }


        return BWImage;

    }

    /**
     * Modified version of adaptive thresholding for bigimage
     *
     * @param SourceGrayImage source gray image
     * @param SliceWidth width of slice
     * @param SliceHeight height of slice
     * @return array in RGB
     */
    public BufferedImage do_AdaptiveThresholding_For_BigImage(BufferedImage SourceGrayImage,
            int SliceWidth, int SliceHeight) {

        int srcImageWidth = SourceGrayImage.getWidth();
        int srcImageHeight = SourceGrayImage.getHeight();
        short[][] srcImagePixelArray = convert_Image_To_GrayShortArray(SourceGrayImage);
        short[][] cropImagePixelArray = new short[SliceWidth][SliceHeight];
        BufferedImage outputImage;

        for (int i = 0; i < srcImageWidth; i = i + 640) {
            for (int j = 0; j < srcImageHeight; j = j + 480) {

                //Cropping big image to get slice image
                for (int x = 0; x < 640; x++) {
                    for (int y = 0; y < 480; y++) {
                        cropImagePixelArray[x][y] = srcImagePixelArray[i + x][j + y];
                    }
                }

                //Run adaptive thresholding
                AdaptiveThreshold_OptimalThresholdValue =
                        find_OptimalThreshold_For_AdaptiveThresholding(
                        cropImagePixelArray);

                //Copying slice image to big image
                for (int x = 0; x < 640; x++) {
                    for (int y = 0; y < 480; y++) {
                        srcImagePixelArray[i + x][j + y] = AdaptiveThreshold_BWPixelArray[x][y];

                    }
                }
            }
        }

        outputImage = convert_GrayShortArray_To_Image(srcImagePixelArray);

        return outputImage;
    }

    /**
     * Conduct adaptive thresholding
     *
     * @param SourceGrayImage source gray image
     * @param IsFindOptimalThresholdValue set true to find optimal threshold
     * value
     * @param DefaultThresholdValue if IsFindThresholdValue is false, set this
     * @return Black and white image
     */
    public BufferedImage do_AdaptiveThresholding(BufferedImage SourceGrayImage,
            boolean IsFindOptimalThresholdValue, int DefaultThresholdValue) {

        BufferedImage BWImage = null;

        if (IsFindOptimalThresholdValue) {
            AdaptiveThreshold_OptimalThresholdValue = find_OptimalThreshold_For_AdaptiveThresholding(
                    SourceGrayImage);
            BWImage = deepCopy(AdaptiveThreshold_BWImage);
        } else {
            short[][] SrcPixels = convert_Image_To_GrayShortArray(SourceGrayImage);

            BWImage = adaptiveThresholding_Core(SrcPixels, 15, AdaptiveThreshold_tPercent, DefaultThresholdValue);
        }


        return BWImage;

    }

    /**
     * Evaluate two images to determine whether alignment is needed
     *
     * @param srcImage1 gray source image 1
     * @param srcImage2 gray source image 2
     * @param maxShift extent of shift
     * @param samplingInterval interval of sample
     * @return BufferedImage if alignment is needed, otherwise return null
     */
    public BufferedImage alignImage(BufferedImage srcImage1, BufferedImage srcImage2,
            int maxShift, int samplingInterval) {

        short[][] grayImage1 = convert_GrayImage_To_GrayShortArray(srcImage1);
        short[][] grayImage2 = convert_GrayImage_To_GrayShortArray(srcImage2);


        int width = grayImage1.length;
        int height = grayImage1[0].length;
        int arrayMax = (int) Math.pow(maxShift * 2 + 1, 2);

        int[] arrayShiftInX = new int[arrayMax];
        int[] arrayShiftInY = new int[arrayMax];
        long[] arrayDiffCount = new long[arrayMax];
        int arrayCount = 0;


        for (int shiftX = -maxShift; shiftX <= maxShift; shiftX++) {
            for (int shiftY = -maxShift; shiftY <= maxShift; shiftY++) {
                arrayShiftInX[arrayCount] = shiftX;
                arrayShiftInY[arrayCount] = shiftY;

                //Counting  difference of array
                arrayDiffCount[arrayCount] = 0;
                for (int x = maxShift; x < width - maxShift; x = x + samplingInterval) {
                    for (int y = maxShift; y < height - maxShift; y = y + samplingInterval) {
                        arrayDiffCount[arrayCount] = arrayDiffCount[arrayCount]
                                + Math.abs(grayImage1[x + shiftX][y + shiftY] - grayImage2[x][y]);

                    }
                }

                arrayCount++;
            }
        }


        //Find minimum difference count and array index
        long curMinDiffCount = arrayDiffCount[0];
        int curArrayIndex = 0;
        for (int q = 1; q < arrayCount; q++) {
            if (arrayDiffCount[q] < curMinDiffCount) {
                curArrayIndex = q;
                curMinDiffCount = arrayDiffCount[q];
            }
        }




        if (arrayShiftInX[curArrayIndex] == 0
                && arrayShiftInY[curArrayIndex] == 0) {
            return null;
        } else {

            //Add padding to the image1 and save to image2
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if ((x + arrayShiftInX[curArrayIndex] >= 0)
                            && (x + arrayShiftInX[curArrayIndex] < width)
                            && (y + arrayShiftInY[curArrayIndex] >= 0)
                            && (y + arrayShiftInY[curArrayIndex] < height)) {
                        grayImage2[x][y] = grayImage1[x + arrayShiftInX[curArrayIndex]][y + arrayShiftInY[curArrayIndex]];
                    } else {
                        grayImage2[x][y] = 255;
                    }
                }
            }
            BufferedImage outputImage = convert_GrayShortArray_To_GrayImage(grayImage2);

            return outputImage;
        }

    }

    /**
     * Convert gray image to gray array
     *
     * @param SrcImage source image
     * @return gray array
     */
    public short[][] convert_GrayImage_To_GrayShortArray(BufferedImage SrcImage) {
        int x_upperBound = SrcImage.getWidth();
        int y_upperBound = SrcImage.getHeight();
        int rgb, gray;

        short[][] outShortArray = new short[x_upperBound][y_upperBound];

        for (int y = 0; y < y_upperBound; y++) {
            for (int x = 0; x < x_upperBound; x++) {
                rgb = SrcImage.getRGB(x, y);
                gray = (rgb >> 16) & 0x000000FF;
                //green = (rgb >> 8) & 0x000000FF;
                //blue = (rgb) & 0x000000FF;

                outShortArray[x][y] = (short) gray;
            }
        }
        return outShortArray;
    }

    /**
     * Convert gray array to gray image
     *
     * @param SrcGrayShortArray gray array
     * @return gray image
     */
    public BufferedImage convert_GrayShortArray_To_GrayImage(short[][] SrcGrayShortArray) {
        int x_upperBound = SrcGrayShortArray.length - 1;
        int y_upperBound = SrcGrayShortArray[0].length - 1;
        int rgb;
        short grayValue;

        BufferedImage outImage = new BufferedImage(x_upperBound + 1,
                y_upperBound + 1, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y <= y_upperBound; y++) {
            for (int x = 0; x <= x_upperBound; x++) {
                grayValue = SrcGrayShortArray[x][y];
                if (grayValue > 255) {
                    grayValue = 255;
                } else {
                    if (grayValue < 0) {
                        grayValue = 0;
                    }
                }

                rgb = new Color(grayValue, grayValue,
                        grayValue).getRGB();
                outImage.setRGB(x, y, rgb);
            }
        }
        return outImage;
    }

       
    
    // Remove short branches in a skeleton curve
    public short[][] removeShortBranchesInSkeletonCurve(
            short[][] srcGrayArray, short colorOfSkeletonCurve,
            short colorOfBackground,
            int lengthThreshold,
            boolean isLeaveOnlyTwoEnds) {

        // Find end points
        Point[] endPoints = findEndPoints(srcGrayArray, colorOfSkeletonCurve);


        // Trace all branches
        // If branch point is found, stop tracing
        LinkedList<Point[]> branches = new LinkedList<Point[]>();
        for (int i = 0; i < endPoints.length; i++) {
            Point[] newBranch =
                    trace_SkeletonCurve(srcGrayArray,
                    endPoints[i].x, endPoints[i].y, colorOfSkeletonCurve, 0, 1000, true);
            branches.add(newBranch);
        }


        // Sorting elements of branches based on length of element
        boolean swapped = true;
        int m = 0;
        while (swapped) {
            swapped = false;
            m++;
            for (int k = 0; k < branches.size() - m; k++) {
                if (branches.get(k).length < branches.get(k + 1).length) {
                    Collections.swap(branches, k, k + 1);
                    swapped = true;
                }
            }
        }


        // Delete branch if its length is shorter than lengthThreshold
        if (isLeaveOnlyTwoEnds == false) {
            for (int k = 0; k < branches.size(); k++) {
                if (branches.get(k).length < lengthThreshold) {

                    Point[] branch = branches.get(k);

                    for (int h = 0; h < branch.length - 1; h++) {
                        srcGrayArray[branch[h].x][branch[h].y] = colorOfBackground;
                    }
                    
                    if (get_Diff_Of_EndPoint_AfterRemovingPoint(srcGrayArray,
                            colorOfSkeletonCurve, colorOfBackground,
                            branch[branch.length - 1].x, branch[branch.length - 1].y) <= 0) {
                        srcGrayArray[branch[branch.length - 1].x][branch[branch.length - 1].y] =
                                colorOfBackground;
                    }
                }
            }
        } else {

            for (int k = 2; k < branches.size(); k++) {

                Point[] branch = branches.get(k);

                for (int h = 0; h < branch.length - 1; h++) {
                    srcGrayArray[branch[h].x][branch[h].y] = colorOfBackground;
                }
                
                if (get_Diff_Of_EndPoint_AfterRemovingPoint(srcGrayArray,
                        colorOfSkeletonCurve, colorOfBackground,
                        branch[branch.length - 1].x, branch[branch.length - 1].y) <= 0) {
                    srcGrayArray[branch[branch.length - 1].x][branch[branch.length - 1].y] =
                            colorOfBackground;
                }
             }
        }




        return srcGrayArray;
    }

    
    
    // Remove angular spur dots
    public short[][] removeSpurDots(short[][] srcGrayArray, short foreColor, short backColor) {
        int clipwidth = srcGrayArray.length;
        int clipheight = srcGrayArray[0].length;

        for (int i = 0; i < clipwidth; i++) {
            for (int j = 0; j < clipheight; j++) {

                if (srcGrayArray[i][j] == foreColor) {
                    boolean[] isNeighborExist =
                            findValidNeighborhoodPixel(srcGrayArray,
                            foreColor, i, j);

                    if ((isNeighborExist[1] && isNeighborExist[4]
                            && !isNeighborExist[3] && !isNeighborExist[5] && !isNeighborExist[6])
                            || (isNeighborExist[4] && isNeighborExist[6]
                            && !isNeighborExist[0] && !isNeighborExist[1] && !isNeighborExist[3])
                            || (isNeighborExist[6] && isNeighborExist[3]
                            && !isNeighborExist[1] && !isNeighborExist[2] && !isNeighborExist[4])
                            || (isNeighborExist[3] && isNeighborExist[1]
                            && !isNeighborExist[4] && !isNeighborExist[7] && !isNeighborExist[6])) {

                        srcGrayArray[i][j] = backColor;

                        continue;

                    }
                }
            }
        }
        
          return srcGrayArray;
    }
    
                
                
         
    public int get_Diff_Of_EndPoint_AfterRemovingPoint(short[][] srcGrayArray,
            short colorOfSkeletonCurve, short colorOfBackground, int x, int y) {

        if (x < 0 || y < 0 || x >= srcGrayArray.length || y >= srcGrayArray[0].length) {
            return 0;
        }

        short origValue = srcGrayArray[x][y];
        
        int beforeRemovalFinalPoint =
                findEndPoints(srcGrayArray, colorOfSkeletonCurve).length;

        srcGrayArray[x][y] = colorOfBackground;

        int afterRemovalFinalPoint =
                findEndPoints(srcGrayArray, colorOfSkeletonCurve).length;

        
        // Restore value
        srcGrayArray[x][y] = origValue;
        return afterRemovalFinalPoint - beforeRemovalFinalPoint;
    }

    
    // Return array of end points
    public Point[] findEndPoints(short[][] srcGrayArray, int colorOfSkeletonCurve) {
        int clipwidth = srcGrayArray.length;
        int clipheight = srcGrayArray[0].length;

        LinkedList<Point> endPoints = new LinkedList<Point>();

        for (int i = 0; i < clipwidth; i++) {
            for (int j = 0; j < clipheight; j++) {

                //Finding end point
                if (srcGrayArray[i][j] == colorOfSkeletonCurve) {
                    boolean[] neighborhoodMap =
                            findValidNeighborhoodPixel(srcGrayArray,
                            colorOfSkeletonCurve, i, j);

                    int validNeighboorhoodCount = 0;
                    for (int k = 0; k < neighborhoodMap.length; k++) {
                        if (neighborhoodMap[k]) {
                            validNeighboorhoodCount++;
                        }
                    }

                    if ((validNeighboorhoodCount <= 1)
                            || (validNeighboorhoodCount == 2
                            && ((neighborhoodMap[0] & neighborhoodMap[1])
                            || (neighborhoodMap[1] & neighborhoodMap[2])
                            || (neighborhoodMap[2] & neighborhoodMap[4])
                            || (neighborhoodMap[4] & neighborhoodMap[7])
                            || (neighborhoodMap[7] & neighborhoodMap[6])
                            || (neighborhoodMap[6] & neighborhoodMap[5])
                            || (neighborhoodMap[5] & neighborhoodMap[3])
                            || (neighborhoodMap[3] & neighborhoodMap[0])))
                            || (validNeighboorhoodCount == 3
                            && ((neighborhoodMap[0] & neighborhoodMap[1] & neighborhoodMap[2])
                            || (neighborhoodMap[2] & neighborhoodMap[4] & neighborhoodMap[7])
                            || (neighborhoodMap[5] & neighborhoodMap[6] & neighborhoodMap[7])
                            || (neighborhoodMap[0] & neighborhoodMap[3] & neighborhoodMap[5])))
                            ) {
                        endPoints.add(new Point(i, j));
                    }
                }
            }
        }

        return endPoints.toArray(new Point[endPoints.size()]);
    }

    // Return array of branch points
    public Point[] findBranchPoints(short[][] srcGrayArray, int colorOfInterest,
            int minConnectivity) {
        int clipwidth = srcGrayArray.length;
        int clipheight = srcGrayArray[0].length;

        LinkedList<Point> endPoints = new LinkedList<Point>();

        for (int i = 0; i < clipwidth; i++) {
            for (int j = 0; j < clipheight; j++) {

                //Finding end point
                if (srcGrayArray[i][j] == colorOfInterest) {
                    if (countValidNeighborhoodPixel(srcGrayArray,
                            colorOfInterest, i, j) >= minConnectivity) {
                        endPoints.add(new Point(i, j));
                    }


                }

            }
        }

        return endPoints.toArray(new Point[endPoints.size()]);
    }

    // Check if bifurcation points exist
    public boolean isBifurcationPointFound(Point[] foundTailLocation) {

        int foundTailLocationLength = foundTailLocation.length;


        for (int i = 0; i < foundTailLocationLength; i++) {
            int neightborhoodPixelCount = 0;
            for (int j = 0; j < foundTailLocationLength; j++) {
                if ((Math.abs(foundTailLocation[i].x - foundTailLocation[j].x) <= 1)
                        && (Math.abs(foundTailLocation[i].y - foundTailLocation[j].y) <= 1)) {
                    neightborhoodPixelCount++;
                }
            }

            if (neightborhoodPixelCount >= 4) {
                return true;
            }

        }

        return false;

    }

    // Createmap of existence of neighborhod pixels
    public boolean[] findValidNeighborhoodPixel(short[][] srcGrayArray, int searchPixelGrayColor,
            int i, int j) {

        int clipwidth = srcGrayArray.length;
        int clipheight = srcGrayArray[0].length;


        boolean[] retArray = new boolean[8];


        if ((i - 1 >= 0) && (j - 1 >= 0)) {
            if (srcGrayArray[i - 1][j - 1] == searchPixelGrayColor) {
                retArray[0] = true;
            } else {
                retArray[0] = false;
            }

        }

        if (j - 1 >= 0) {
            if (srcGrayArray[i][j - 1] == searchPixelGrayColor) {
                retArray[1] = true;
            }else {
                retArray[1] = false;
            }
        }

        if ((i + 1 < clipwidth) && (j - 1 >= 0)) {
            if (srcGrayArray[i + 1][j - 1] == searchPixelGrayColor) {
                retArray[2] = true;
            }else {
                retArray[2] = false;
            }
        }
        //-----------------------
        if (i - 1 >= 0) {
            if (srcGrayArray[i - 1][j] == searchPixelGrayColor) {
                retArray[3] = true;
            }else {
                retArray[3] = false;
            }
        }

        if (i + 1 < clipwidth) {
            if (srcGrayArray[i + 1][j] == searchPixelGrayColor) {
                retArray[4] = true;
            }else {
                retArray[4] = false;
            }
        }
        //------------------------
        if ((i - 1 >= 0) && (j + 1 < clipheight)) {
            if (srcGrayArray[i - 1][j + 1] == searchPixelGrayColor) {
                retArray[5] = true;
            }else {
                retArray[5] = false;
            }
        }

        if (j + 1 < clipheight) {
            if (srcGrayArray[i][j + 1] == searchPixelGrayColor) {
                retArray[6] = true;
            }else {
                retArray[6] = false;
            }
        }

        if ((i + 1 < clipwidth) && (j + 1 < clipheight)) {
            if (srcGrayArray[i + 1][j + 1] == searchPixelGrayColor) {
                retArray[7] = true;
            }else {
                retArray[7] = false;
            }

        }


        return retArray;
    }

    public int countValidNeighborhoodPixel(short[][] srcGrayArray, int searchPixelGrayColor,
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

    public short[][] extend_SkeletonCurve(short[][] srcGrayArray,
            short colorOfSkeletonCurve,
            short[][] maskGrayArray,
            short colorOfBackgroundInMask,
            short tailLength) {


        int clipwidth = srcGrayArray.length;
        int clipheight = srcGrayArray[0].length;


        for (int i = 0; i < clipwidth; i++) {
            for (int j = 0; j < clipheight; j++) {

                //Finding end point
                if (srcGrayArray[i][j] == colorOfSkeletonCurve) {
                    int neighborhoodPixelCount = 0;
                    boolean processedTailFound = false;
                    boolean emptyMaskAreaFound = false;

                    neighborhoodPixelCount =
                            countValidNeighborhoodPixel(srcGrayArray, colorOfSkeletonCurve, i, j);


                    if (countValidNeighborhoodPixel(srcGrayArray, 125, i, j) > 0) {
                        processedTailFound = true;
                    }

                    if (countValidNeighborhoodPixel(maskGrayArray, colorOfBackgroundInMask, i, j) > 0) {
                        emptyMaskAreaFound = true;
                    }


                    if ((neighborhoodPixelCount == 1)
                            && (processedTailFound == false)
                            && (emptyMaskAreaFound == false)) {


                        //Finding skeleton tail
                        Point[] foundTailLocation = new Point[tailLength];


                        foundTailLocation = trace_SkeletonCurve(
                                srcGrayArray, i, j, (short) 125, 0, tailLength, false);




                        if (foundTailLocation.length > 0) {

                            if (isBifurcationPointFound(foundTailLocation) == false) {

                                //Calculating vector
                                double avgvectorX = 0;
                                double avgvectorY = 0;
                                for (int k = 0; k < foundTailLocation.length - 1; k++) {
                                    avgvectorX = avgvectorX
                                            + foundTailLocation[k].x - foundTailLocation[k + 1].x;
                                    avgvectorY = avgvectorY
                                            + foundTailLocation[k].y - foundTailLocation[k + 1].y;
                                }
                                avgvectorX = avgvectorX / tailLength;
                                avgvectorY = avgvectorY / tailLength;


                                //Extrapolate vector to empty region in maskimage
                                float startX = foundTailLocation[0].x;
                                float startY = foundTailLocation[0].y;
                                Point lastValidPoint = new Point();
                                Point curValidPoint = new Point();
                                lastValidPoint.x = foundTailLocation[0].x;
                                lastValidPoint.y = foundTailLocation[0].y;

                                while (true) {
                                    startX = (float) (startX + avgvectorX / 5);
                                    startY = (float) (startY + avgvectorY / 5);

                                    curValidPoint.x = Math.round(startX);
                                    curValidPoint.y = Math.round(startY);

                                    if (curValidPoint.x < 0 || curValidPoint.x > clipwidth - 1
                                            || curValidPoint.y < 0 || curValidPoint.y > clipheight - 1) {
                                        break;
                                    }


                                    if ((lastValidPoint.x != curValidPoint.x)
                                            || (lastValidPoint.y != curValidPoint.y)) {
                                        lastValidPoint.x = curValidPoint.x;
                                        lastValidPoint.y = curValidPoint.y;


                                        if (maskGrayArray[lastValidPoint.x][lastValidPoint.y]
                                                == colorOfBackgroundInMask) {
                                            break;
                                        }
                                        srcGrayArray[lastValidPoint.x][lastValidPoint.y] =
                                                colorOfSkeletonCurve;

                                    }

                                }
                            }

                        }
                    }
                }


            }
        }


        return srcGrayArray;
    }

    /**
     * Search tails of skeleton
     *
     * @param SrcGrayShortArray source gray array
     * @return Point array
     */
    public Point[] trace_SkeletonCurve(short[][] SrcGrayShortArray, int CenterX, int CenterY,
            short FillColorG, int ToleranceG, int MaxTrackingCount, boolean isStopAtBranchPoint) {


        int SrcImageWidth = SrcGrayShortArray.length;
        int SrcImageHeight = SrcGrayShortArray[0].length;
        int curTrackingCount = 0;

        Point Cur_Location = new Point();
        Point[] neighborhoodPixelLocation = new Point[10];
        Point[] foundPixelLocation = new Point[MaxTrackingCount];

        int CenterColor_G;

        boolean[][] QueueMap = new boolean[SrcImageWidth][SrcImageHeight];

        short[][] outPixels = multiArrayCopy(SrcGrayShortArray);
        _FloodFilledDoneMap = new boolean[SrcImageWidth][SrcImageHeight];

        FloodFillQue.clear();
        Cur_Location.x = CenterX;
        Cur_Location.y = CenterY;
        FloodFillQue.add(Cur_Location);
        QueueMap[Cur_Location.x][Cur_Location.y] = true;
        _FloodFilledDoneMap[Cur_Location.x][Cur_Location.y] = true;

        CenterColor_G = SrcGrayShortArray[Cur_Location.x][Cur_Location.y];



        do {
            Cur_Location.x = FloodFillQue.peek().x;
            Cur_Location.y = FloodFillQue.peek().y;

            outPixels[Cur_Location.x][Cur_Location.y] = FillColorG;
            _FloodFilledDoneMap[Cur_Location.x][Cur_Location.y] = true;


            curTrackingCount++;
            foundPixelLocation[curTrackingCount - 1] = new Point(Cur_Location);

            if (isStopAtBranchPoint) {
                if (curTrackingCount > 1
                        && countValidNeighborhoodPixel(SrcGrayShortArray,
                        CenterColor_G, Cur_Location.x, Cur_Location.y) >= 3) {
                    break;
                }
            }

            if (curTrackingCount == MaxTrackingCount) {
                break;
            }


            FloodFillQue.poll();
            QueueMap[Cur_Location.x][Cur_Location.y] = false;


            //Initializing pixel locations
            neighborhoodPixelLocation[1] = new Point(Cur_Location.x - 1, Cur_Location.y - 1);
            neighborhoodPixelLocation[2] = new Point(Cur_Location.x, Cur_Location.y - 1);
            neighborhoodPixelLocation[3] = new Point(Cur_Location.x + 1, Cur_Location.y - 1);
            neighborhoodPixelLocation[4] = new Point(Cur_Location.x - 1, Cur_Location.y);
            neighborhoodPixelLocation[5] = new Point(-1, -1);
            neighborhoodPixelLocation[6] = new Point(Cur_Location.x + 1, Cur_Location.y);
            neighborhoodPixelLocation[7] = new Point(Cur_Location.x - 1, Cur_Location.y + 1);
            neighborhoodPixelLocation[8] = new Point(Cur_Location.x, Cur_Location.y + 1);
            neighborhoodPixelLocation[9] = new Point(Cur_Location.x + 1, Cur_Location.y + 1);



            //Excluding invalid pixel locations
            if (Cur_Location.y == 0) {
                neighborhoodPixelLocation[1].x = -1;
                neighborhoodPixelLocation[2].x = -1;
                neighborhoodPixelLocation[3].x = -1;
            }

            if (Cur_Location.y == SrcImageHeight - 1) {
                neighborhoodPixelLocation[7].x = -1;
                neighborhoodPixelLocation[8].x = -1;
                neighborhoodPixelLocation[9].x = -1;
            }

            if (Cur_Location.x == 0) {
                neighborhoodPixelLocation[1].x = -1;
                neighborhoodPixelLocation[4].x = -1;
                neighborhoodPixelLocation[7].x = -1;
            }

            if (Cur_Location.x == SrcImageWidth - 1) {
                neighborhoodPixelLocation[3].x = -1;
                neighborhoodPixelLocation[6].x = -1;
                neighborhoodPixelLocation[9].x = -1;
            }


            //Processing for valid pixel locations
            for (int i = 1; i < 10; i++) {

                if (neighborhoodPixelLocation[i].x != -1) {
                    if (QueueMap[neighborhoodPixelLocation[i].x][neighborhoodPixelLocation[i].y] == false) {
                        if (_FloodFilledDoneMap[neighborhoodPixelLocation[i].x][neighborhoodPixelLocation[i].y] == false
                                && Math.abs(CenterColor_G
                                - SrcGrayShortArray[neighborhoodPixelLocation[i].x][neighborhoodPixelLocation[i].y]) <= ToleranceG) {
                            FloodFillQue.add(neighborhoodPixelLocation[i]);
                            QueueMap[neighborhoodPixelLocation[i].x][neighborhoodPixelLocation[i].y] = true;
                        }
                    }
                }
            }

        } while (FloodFillQue.size() != 0);


        Point[] retPoint = new Point[curTrackingCount];
        System.arraycopy(foundPixelLocation, 0, retPoint, 0, curTrackingCount);
        return retPoint;
    }

    /**
     * Conduct adaptive thresholding
     *
     * @param SrcPixels source gray image
     * @param boxSize box size for processing
     * @param tpercent t percent value for comparison between gray value and
     * average gray value of selected box
     * @param GrayLimit pixels whose gray value is higher than GrayLimit will be
     * excluded
     * @return optimal threshold value
     */
    public BufferedImage adaptiveThresholding_Core_To_Gray(short[][] SrcPixels,
            int boxSize, float tpercent,
            int GrayLimit) {


        short[][] OutPixels;
        int SrcBitmapWidth = SrcPixels.length;
        int SrcBitmapHeight = SrcPixels[0].length;

        OutPixels = new short[SrcBitmapWidth][SrcBitmapHeight];

        long[][] temp_IntegralGrayMap;
        int LocalGrayAvg = 0;
        float rPercentValue = (float) (1 - tpercent);

        temp_IntegralGrayMap = build_IntegralGrayArrayMap(SrcPixels);


        for (int y = 0; y < SrcBitmapHeight; y++) {
            for (int x = 0; x < SrcBitmapWidth; x++) {

                LocalGrayAvg = calculate_LocalGrayAvgValueUsingIntegralMap(
                        temp_IntegralGrayMap, boxSize, x, y);
                if (SrcPixels[x][y] < LocalGrayAvg * rPercentValue && SrcPixels[x][y] < GrayLimit) {
                    OutPixels[x][y] = 255;
                } else {
                    OutPixels[x][y] = 0;
                }

            }
        }
        AdaptiveThreshold_BWPixelArray = OutPixels;
        return convert_GrayShortArray_To_GrayImage(OutPixels);
    }

    public long countPixelInCircle(short[][] SrcGrayShortArray,
            int centerX, int centerY,
            int circleRadius, int circleWidth,
            short pixelValue) {

        int srcImageWidth = SrcGrayShortArray.length;
        int srcImageHeight = SrcGrayShortArray[0].length;

        double curX, curY;
        double prevX = 0;
        double prevY = 0;
        double oneCycleRad = 2 * Math.PI;
        double circleStepIncrease = oneCycleRad / 360;

        long pixelCountSum = 0;

        for (int curCircleRadius = circleRadius - circleWidth / 2;
                curCircleRadius < circleRadius + circleWidth / 2;
                curCircleRadius++) {

            for (double radAngle = 0; radAngle < oneCycleRad;
                    radAngle = radAngle + circleStepIncrease) {

                curX = Math.sin(radAngle) * curCircleRadius + centerX;
                curY = Math.cos(radAngle) * curCircleRadius + centerY;

                if ((int) curX < 0
                        || (int) curY < 0
                        || (int) curX >= srcImageWidth
                        || (int) curY >= srcImageHeight) {
                    //if the point is out of range, skip it
                    continue;
                }

                if ((int) curX == prevX && (int) curY == prevY) {
                    //if the point is the same as the previous one, skip it
                    continue;
                }

                if (SrcGrayShortArray[(int) curX][(int) curY] == pixelValue) {
                    pixelCountSum++;
                }

                prevX = (int) curX;
                prevY = (int) curY;

            }
        }

        return pixelCountSum;

    }

    public BufferedImage rotateImage(BufferedImage src, int centerX,
            int centerY, double degrees, boolean isAntialiasingOn,
            Color bkgroundColor) {

        double radians = Math.toRadians(degrees);

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        BufferedImage result = new BufferedImage(srcWidth, srcHeight,
                src.getType());

        Graphics2D g = result.createGraphics();
        if (isAntialiasingOn) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        g.setBackground(bkgroundColor);
        g.fillRect(0, 0, srcWidth, srcHeight);
        g.rotate(radians, centerX, centerY);
        g.drawRenderedImage(src, null);

        return result;
    }

    public BufferedImage shiftImage(BufferedImage src, int stepX,
            int stepY, boolean isAntialiasingOn,
            Color bkgroundColor) {


        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        BufferedImage result = new BufferedImage(srcWidth, srcHeight,
                src.getType());

        Graphics2D g = result.createGraphics();

        if (isAntialiasingOn) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        g.setBackground(bkgroundColor);
        g.fillRect(0, 0, srcWidth, srcHeight);
        g.translate(stepX, stepY);
        g.drawRenderedImage(src, null);

        return result;
    }

    public BufferedImage rotateAndShiftImage(BufferedImage src, int centerX,
            int centerY, double degrees, boolean isAntialiasingOn,
            Color bkgroundColor) {

        BufferedImage outImage =
                rotateImage(src, centerX, centerY,
                degrees, isAntialiasingOn, bkgroundColor);
        outImage = shiftImage(outImage, Math.round(src.getWidth() / 2 - centerX),
                Math.round(src.getHeight() / 2 - centerY),
                isAntialiasingOn, bkgroundColor);

        return outImage;
    }

    public BufferedImage cropImage(BufferedImage srcImage, int x, int y,
            int width, int height) {

        BufferedImage dest = new BufferedImage(width, height,
                srcImage.getType());

        Graphics2D g2 = dest.createGraphics();

        g2.drawImage(srcImage.getSubimage(x, y, width, height), 0, 0, null);

        return dest;
    }

    /**
     * extend (maximize)
     *
     * @param SrcGrayShortArray source array
     * @return array
     */
    public short[][] extend_UsingGrayShortArray(short[][] SrcGrayShortArray) {
        int SrcImgWidthIndex = SrcGrayShortArray.length - 2;
        int SrcImgHeightIndex = SrcGrayShortArray[0].length - 2;
        short[][] outPixels = multiArrayCopy(SrcGrayShortArray);


        for (int y = 1; y <= SrcImgHeightIndex; y++) {
            for (int x = 1; x <= SrcImgWidthIndex; x++) {
                if (SrcGrayShortArray[x][y] == 255) {
                    outPixels[x - 1][y] = 255;
                    outPixels[x + 1][y] = 255;
                    outPixels[x][y - 1] = 255;
                    outPixels[x][y + 1] = 255;
                }
            }
        }

        return outPixels;
    }

    /**
     * erode (minimize)
     *
     * @param SrcGrayShortArray source array
     * @return array
     */
    public short[][] erode_UsingGrayShortArray(short[][] SrcGrayShortArray) {
        int SrcImgWidthIndex = SrcGrayShortArray.length - 2;
        int SrcImgHeightIndex = SrcGrayShortArray[0].length - 2;
        short[][] outPixels = multiArrayCopy(SrcGrayShortArray);


        for (int y = 1; y <= SrcImgHeightIndex; y++) {
            for (int x = 1; x <= SrcImgWidthIndex; x++) {
                if (SrcGrayShortArray[x][y] == 0) {
                    outPixels[x - 1][y] = 0;
                    outPixels[x + 1][y] = 0;
                    outPixels[x][y - 1] = 0;
                    outPixels[x][y + 1] = 0;
                }
            }
        }

        return outPixels;
    }

    /**
     * Calculate average gray value
     *
     * @param SrcGrayShortArray gray array
     * @param samplingInterval interval of sampling
     * @return number
     */
    public float calculate_AverageGray(short[][] SrcGrayShortArray, int samplingInterval) {
        int x_upperBound = SrcGrayShortArray.length - 1;
        int y_upperBound = SrcGrayShortArray[0].length - 1;
        double accu_grayValue;
        double accu_PixelCount;

        accu_grayValue = 0;
        accu_PixelCount = 0;
        for (int y = 0; y <= y_upperBound; y = y + samplingInterval) {
            for (int x = 0; x <= x_upperBound; x = x + samplingInterval) {
                accu_grayValue = accu_grayValue + SrcGrayShortArray[x][y];
                accu_PixelCount++;
            }
        }


        return (float) (accu_grayValue / accu_PixelCount);
    }

    /**
     * check if image is blank
     *
     * @param SrcGrayShortArray gray array
     * @param samplingInterval interval of sampling
     * @return boolean
     */
    public boolean isBlankImage(short[][] SrcGrayShortArray, int samplingInterval) {
        int x_upperBound = SrcGrayShortArray.length - 1;
        int y_upperBound = SrcGrayShortArray[0].length - 1;

        for (int y = 0; y <= y_upperBound; y = y + samplingInterval) {
            for (int x = 0; x <= x_upperBound; x = x + samplingInterval) {
                if (SrcGrayShortArray[x][y] != 0) {
                    return false;
                }
            }
        }

        return true;
    }

    //Conduct Hough transform and get finding
    //Return as integer array
    //double[0]: center x
    //double[1]: center y
    //double[2]: angle
    public double[] calculate_CenterAndAngle(BufferedImage srcBufferedImage) {
        double[] resultIntArray = new double[3];

        //Alignment will be done using small size image
        final float resizingPercent = 100;   //No resize
        final int resizedImageWidth =
                (int) (srcBufferedImage.getWidth() * resizingPercent / 100);
        final int resizedImageHeight =
                (int) (srcBufferedImage.getHeight() * resizingPercent / 100);

        //Assign region to detect the center of a plate
        final int detectingRegionX1 =
                (int) (resizedImageWidth * 0.3);
        final int detectingRegionY1 =
                (int) (resizedImageHeight * 0.3);
        final int detectingRegionX2 =
                (int) (resizedImageWidth * 0.7);
        final int detectingRegionY2 =
                (int) (resizedImageHeight * 0.7);


        //Create small size image
        BufferedImage resized = resizeImage(
                srcBufferedImage,
                BufferedImage.TYPE_BYTE_GRAY,
                resizedImageWidth, resizedImageHeight);


        //Conduct resizing and adaptive thresholding to create binary image
        short[][] srcGrayShortArray =
                convert_GrayImage_To_GrayShortArray(resized);
        resized = adaptiveThresholding_Core(srcGrayShortArray, 25, 0.2f, 255);

        srcGrayShortArray = erode_UsingGrayShortArray(
                AdaptiveThreshold_BWPixelArray);
        srcGrayShortArray = erode_UsingGrayShortArray(
                srcGrayShortArray);
        srcGrayShortArray = erode_UsingGrayShortArray(
                srcGrayShortArray);
        //imgProc.saveImage(imgProc.convert_GrayShortArray_To_GrayImage(srcGrayShortArray),
        //        "gif", "C:\\Jung\\My Temp\\adaptive.gif");


        //Conduct Hough transform for line detection
        LineHough lineHough = new LineHough();
        int[][] accumulator = lineHough.process(srcGrayShortArray, detectingRegionX1,
                detectingRegionY1, detectingRegionX2, detectingRegionY2);


        //Find a single central line
        int resultArray[] = lineHough.findMaxima(accumulator);

        //Do not conduct alignment if no distinct central line is found
        if (resultArray[2] < 10) {
            return null;
        }


        //Calculate line parameter
        int r = resultArray[0];
        int theta = resultArray[1];

        int y1;
        int x1;
        int y2;
        int x2;


        if (theta < 45 || theta > 135) {
            y1 = 0;
            x1 = lineHough.getX_from_YrThetaLine(y1, r, theta);
            y2 = resized.getHeight();
            x2 = lineHough.getX_from_YrThetaLine(y2, r, theta);
        } else {
            x1 = 0;
            y1 = lineHough.getY_from_XrThetaLine(x1, r, theta);
            x2 = resized.getWidth();
            y2 = lineHough.getY_from_XrThetaLine(x2, r, theta);
        }

        //Find center point of a plate image
        Point2D centerPoint =
                lineMath.find_CenterLocation(
                new Point2D.Double(x1, y1),
                new Point2D.Double(x2, y2),
                new Point2D.Double(detectingRegionX1, detectingRegionY1),
                new Point2D.Double(detectingRegionX2, detectingRegionY2),
                3,
                srcGrayShortArray);

        //Do not conduct alignment if no center point is found
        if (centerPoint == null) {
            return null;
        }

        //Calculate angle
        double angleInRad = lineMath.get_AngleInRadian_Of_TwoPoints(
                new Point2D.Double(x1, y1),
                new Point2D.Double(x2, y2));


        resultIntArray[0] = centerPoint.getX() * 100 / resizingPercent;
        resultIntArray[1] = centerPoint.getY() * 100 / resizingPercent;


        if (theta < 45 || theta > 135) {
            resultIntArray[2] = angleInRad / Math.PI * 180 + 90;
        } else {
            resultIntArray[2] = angleInRad / Math.PI * 180;
        }

        return resultIntArray;
    }

    public BufferedImage alignImage(BufferedImage originalImage,
            double[] axisAndAngleArray) {

        //Do not conduct alignment if no central line is found
        if (axisAndAngleArray == null) {
            return originalImage;
        }

        //Align images
        //Rotate and shift image, and then save images
        int centerX = (int) axisAndAngleArray[0];
        int centerY = (int) axisAndAngleArray[1];
        double angleInDegree = axisAndAngleArray[2] - 90;

        BufferedImage alignedImage = rotateAndShiftImage(originalImage,
                centerX, centerY, angleInDegree, true, Color.black);
        return alignedImage;
    }

    public void draw_OutlinedText(Graphics graphics,
            String text, int left, int top, Color cOut, Color cIn) {
        graphics.setColor(cOut);
        graphics.drawString(text, left - 1, top);
        graphics.setColor(cOut);
        graphics.drawString(text, left + 1, top);
        graphics.setColor(cOut);
        graphics.drawString(text, left, top - 1);
        graphics.setColor(cOut);
        graphics.drawString(text, left, top + 1);
        graphics.setColor(cIn);
        graphics.drawString(text, left, top);
    }
    
    
    
    // Create perfect skeleton curve without branches and spur dots
    // Please do not delete any code for debugging in the future
    public short[][] createSingleSkeletonCurve(
            short[][] skeletonImgArray, 
            short[][] maskImgArray,
            short colorOfSkeletonCurve,
            short colorOfBackground,
            int lengthThreshold,
            boolean isLeaveOnlyTwoEnds) {
        
        
        boolean isDebugmode = false;
        String destFolderWhileDebugging = "C:\\Jung\\My Temp\\";
        
        
        
        if (isDebugmode) {
            saveImage(convert_GrayShortArray_To_GrayImage(maskImgArray),
                    "gif", destFolderWhileDebugging + "0orginal.gif");
            
            saveImage(convert_GrayShortArray_To_GrayImage(skeletonImgArray),
                    "gif", destFolderWhileDebugging + "1skeleton.gif");
        }            
        
        
        // Remove short branches and spur dots
        for (int k = 0; k < 5; k++) {
           skeletonImgArray = removeSpurDots(
                    skeletonImgArray, colorOfSkeletonCurve, colorOfBackground);

            if (isDebugmode) {
                saveImage(convert_GrayShortArray_To_GrayImage(skeletonImgArray),
                        "gif", destFolderWhileDebugging + "2pruned" + k + " A spur.gif");
            }

           
            skeletonImgArray = removeShortBranchesInSkeletonCurve(
                    skeletonImgArray, colorOfSkeletonCurve, 
                    colorOfBackground, lengthThreshold, false);

            if (isDebugmode) {
                saveImage(convert_GrayShortArray_To_GrayImage(skeletonImgArray),
                        "gif", destFolderWhileDebugging + "2pruned" + k + " B branch.gif");
            }
            
            
            if (findEndPoints(skeletonImgArray,
                    colorOfSkeletonCurve).length == 2
                && findBranchPoints(skeletonImgArray,
                    colorOfSkeletonCurve, 3).length == 0) {
                break;
            }
        }
        if (isDebugmode) {
            saveImage(convert_GrayShortArray_To_GrayImage(skeletonImgArray),
                    "gif", destFolderWhileDebugging + "3pruned_final.gif");
        }                
        
        
        //Leave only two ends
        if (isLeaveOnlyTwoEnds) {
            if (findEndPoints(skeletonImgArray, colorOfSkeletonCurve).length > 2) {
                skeletonImgArray = removeShortBranchesInSkeletonCurve(
                        skeletonImgArray, colorOfSkeletonCurve, colorOfBackground, 20,
                        true);
            }
        }
        if (isDebugmode) {
            saveImage(convert_GrayShortArray_To_GrayImage(skeletonImgArray),
                    "gif", destFolderWhileDebugging + "4twopoints.gif");
        }        
        
        
        // Extend skeleton curve
        skeletonImgArray = extend_SkeletonCurve(
                skeletonImgArray, colorOfSkeletonCurve,
                maskImgArray, colorOfBackground, (short) 20);
        if (isDebugmode) {
            saveImage(convert_GrayShortArray_To_GrayImage(skeletonImgArray),
                    "gif",destFolderWhileDebugging + "5curveextend.gif");
        }
        
        
        // Remove short branches and spur dots
        for (int k = 0; k < 5; k++) {
           skeletonImgArray = removeSpurDots(
                    skeletonImgArray, colorOfSkeletonCurve, colorOfBackground);
            if (isDebugmode) {
                saveImage(convert_GrayShortArray_To_GrayImage(skeletonImgArray),
                        "gif", destFolderWhileDebugging + "6pruned" + k + " A spur.gif");
            }

           
            skeletonImgArray = removeShortBranchesInSkeletonCurve(
                    skeletonImgArray, colorOfSkeletonCurve, 
                    colorOfBackground, lengthThreshold, false);
            if (isDebugmode) {
                saveImage(convert_GrayShortArray_To_GrayImage(skeletonImgArray),
                        "gif", destFolderWhileDebugging + "6pruned" + k + " B branch.gif");
            }
            
            
            if (findEndPoints(skeletonImgArray,
                    colorOfSkeletonCurve).length == 2
                && findBranchPoints(skeletonImgArray,
                    colorOfSkeletonCurve, 3).length == 0) {
                break;
            }
        }
        if (isDebugmode) {
            saveImage(convert_GrayShortArray_To_GrayImage(skeletonImgArray),
                    "gif", destFolderWhileDebugging + "7pruned_final.gif");
        }    
        
        
        return skeletonImgArray;
    }
    
    
    
    public int[][] get_SpinePoint(short[][] skeletonImgArray, int pointSize,
            short colorOfSkeletonCurve) {
        
        int clipWidth = skeletonImgArray.length;
        int clipHeight = skeletonImgArray[0].length;
        int skeletonPixelCount = 0;
        int[][] spine;

        
        // Count length
        for (int i = 0; i < clipWidth; i++) {
            for (int j = 0; j < clipHeight; j++) {
                if (skeletonImgArray[i][j] == colorOfSkeletonCurve) {
                    skeletonPixelCount++;
                }
            }
        }


        // Get all points of skeleton curve
        Point[] skeletonPoints;
        for (int i = 0; i < clipWidth; i++) {
            for (int j = 0; j < clipHeight; j++) {

                //Find end point
                if (skeletonImgArray[i][j] == colorOfSkeletonCurve) {
                    int neighborhoodPixelCount;

                    neighborhoodPixelCount =
                            countValidNeighborhoodPixel(skeletonImgArray, colorOfSkeletonCurve, i, j);


                    //End found
                    if ((neighborhoodPixelCount == 1)) {
                        skeletonPoints = trace_SkeletonCurve(
                                skeletonImgArray, i, j, (short) 125, 0, skeletonPixelCount, false);

                        spine = new int[2][13];
                        int curSpinePointIndex = 0;
                        float spinePointIntervalLength = ((float) skeletonPixelCount) / (pointSize -1 );
                        for (float k = 0; k < skeletonPoints.length; k = k + spinePointIntervalLength) {
                            if (Math.round(k) > skeletonPoints.length - 1) {
                                break;
                            }

                            spine[0][curSpinePointIndex] =
                                    skeletonPoints[Math.round(k)].x;
                            spine[1][curSpinePointIndex] =
                                    skeletonPoints[Math.round(k)].y;
                            curSpinePointIndex++;

                            if (curSpinePointIndex > pointSize) {
                                break;
                            }
                        }

                        spine[0][12] =
                                skeletonPoints[skeletonPoints.length - 1].x;
                        spine[1][12] =
                                skeletonPoints[skeletonPoints.length - 1].y;

                        return spine;
                    }
                }
            }  //for
        } // for
        
        return null;
    }
 }

