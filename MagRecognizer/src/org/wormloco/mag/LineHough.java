/*
 * LineHough.java
 * 
 */

package org.wormloco.mag;

/**
 * Conducts line Hough transform to detect lines
 *
 * @author SangKyu Jung
 */
public class LineHough {

    double[] luT_Of_CosTheta = new double[180];
    double[] luT_Of_SinTheta = new double[180];
    public NativeImgProcessing imgProc = new NativeImgProcessing();

    public LineHough() {

        //Initialize look-up-table
        for (int theta = 0; theta < 180; theta++) {
            luT_Of_CosTheta[theta] = Math.cos((theta * Math.PI) / 180);
            luT_Of_SinTheta[theta] = Math.sin((theta * Math.PI) / 180);

        }
    }

    //Main routine to conduct Hough transform
    //Process entire image
    public int[][] process(short[][] srcGrayShortArray) {
        int srcImageWidth = srcGrayShortArray.length;
        int srcImageHeight = srcGrayShortArray[0].length;
        int[][] accumulator;

        int rmax = (int) Math.sqrt(Math.pow(srcImageWidth, 2)
                + Math.pow(srcImageHeight, 2));

        accumulator = new int[rmax * 2][180];
        int r;
        
        //Generating Hough accumulator
        for (int x = 0; x < srcImageWidth; x++) {
            for (int y = 0; y < srcImageHeight; y++) {

                if ((srcGrayShortArray[x][y]) == 255) {

                    for (int theta = 0; theta < 180; theta++) {
                        // y = (-cos(theata)/sin(theta))*x + (r/sin(theta))
                        // r = x*cos(theta) + y*sin(theta)                        
                        r = (int) (x * luT_Of_CosTheta[theta] + y * luT_Of_SinTheta[theta]);
                        if ((r >= -rmax) && (r <= rmax)) {
                            accumulator[r + rmax][theta] = accumulator[r + rmax][theta] + 1;
                        }
                    }
                }
            }
        }

        return accumulator;
    }

    
    //Process only defined region
    public int[][] process(short[][] srcGrayShortArray, int x1, int y1, int x2, int y2) {
        int srcImageWidth = srcGrayShortArray.length;
        int srcImageHeight = srcGrayShortArray[0].length;
        int[][] accumulator;

        int rmax = (int) Math.sqrt(Math.pow(srcImageWidth, 2)
                + Math.pow(srcImageHeight, 2));

        accumulator = new int[rmax * 2][180];
        int r;

        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {

                if ((srcGrayShortArray[x][y]) == 255) {

                    for (int theta = 0; theta < 180; theta++) {
                        // y = (-cos(theata)/sin(theta))*x + (r/sin(theta))
                        // r = x*cos(theta) + y*sin(theta)                        
                        r = (int) (x * luT_Of_CosTheta[theta] + y * luT_Of_SinTheta[theta]);
                        if ((r >= -rmax) && (r <= rmax)) {
                            accumulator[r + rmax][theta] = accumulator[r + rmax][theta] + 1;
                        }
                    }
                }
            }
        }

        return accumulator;
    }

    //Delete similar lines
    public int[][] clearSimilarLines(int[][] srcAccumulator,
            int target_r, int target_theta,
            int margin_r, int margin_theta) {

        int[][] srcAccu = imgProc.multiArrayCopy(srcAccumulator);

        int rmax = srcAccu.length / 2;

        int r1 = Math.max(target_r - margin_r, -rmax);
        int r2 = Math.min(target_r + margin_r, rmax);
        int theta1 = Math.max(target_theta - margin_theta, 0);
        int theta2 = Math.min(target_theta + margin_theta, 180);


        for (int r = r1; r < r2; r++) {
            for (int theta = theta1; theta < theta2; theta++) {
                srcAccu[r + rmax][theta] = 0;
            }
        }

        return srcAccu;
    }

    //Find the most strong line
    //Return as integer array
    //resultIntArray[0] = r at maxima
    //resultIntArray[1] = theta at maxima
    //resultIntArray[2] = total count at maxima
    public int[] findMaxima(int[][] srcAccumulator) {
        int[] resultIntArray = new int[3];

        int rmax = srcAccumulator.length / 2;

        resultIntArray[2] = -1;
        for (int r = -rmax; r < rmax; r++) {
            for (int theta = 0; theta < 180; theta++) {
                if (srcAccumulator[r + rmax][theta] > resultIntArray[2]) {
                    resultIntArray[0] = r;
                    resultIntArray[1] = theta;
                    resultIntArray[2] = srcAccumulator[r + rmax][theta];
                }
            }
        }

        return resultIntArray;
    }

    //Draw line
    public short[][] drawPolarLine(short[][] srcGrayShortArray, int r, int theta, short grayColor) {
        short[][] outGrayShortArray = imgProc.multiArrayCopy(srcGrayShortArray);

        int srcImageWidth = srcGrayShortArray.length;
        int srcImageHeight = srcGrayShortArray[0].length;

        for (int x = 0; x < srcImageWidth; x++) {

            for (int y = 0; y < srcImageHeight; y++) {

                int temp = (int) (x * luT_Of_CosTheta[theta] + y * luT_Of_SinTheta[theta]);
                if (temp == r) {
                    outGrayShortArray[x][y] = grayColor;
                }

            }
        }

        return outGrayShortArray;

    }

    //Get y value from x, r, and theta
    public int getY_from_XrThetaLine(int x, int r, int theta) {
        int retY;
        if (theta == 0) {
            return r;
        }        
        retY = (int) (-luT_Of_CosTheta[theta] / luT_Of_SinTheta[theta] * x
                + r / luT_Of_SinTheta[theta]);

        return retY;
    }

    
    //Get y value from x, r, and theta
    public int getX_from_YrThetaLine(int y, int r, int theta) {
        int retX;
        if (theta == 0) {
            return r;
        }
        
        retX = (int) (-(luT_Of_SinTheta[theta] / luT_Of_CosTheta[theta]) * (y
                - r / luT_Of_SinTheta[theta]));

        return retX;
    }
    
    
    
    //Get slope from r, and theta
    public double get_Slope_from_rThetaLine(int r, int theta) {
        double retSlope = -luT_Of_CosTheta[theta] / luT_Of_SinTheta[theta];
        return retSlope;
    }

    
    //Get y intercept value from r and theta
    public double get_yIntercept_from_rThetaLine(int r, int theta) {
        double retYIntercept = r / luT_Of_SinTheta[theta];
        return retYIntercept;
    }
}

