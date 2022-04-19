/*
 * LineMath.java
 * 
 */

package org.wormloco.mag;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * Contains linear formulae
 *
 * @author SangKyu Jung
 */

public class LineMath {

    //Get a location of innter point on a line
    //return a point
    public Point2D get_InnnerPointInLine(Point2D P1, Point2D P2,
            double pointLocationInLength) {
        Point2D innerPoint = new Point2D.Double();
        double distance;

        distance = get_Distance(P1, P2);


        innerPoint.setLocation((pointLocationInLength * P1.getX()
                + (distance - pointLocationInLength) * P2.getX())
                / distance,
                (pointLocationInLength * P1.getY()
                + (distance - pointLocationInLength) * P2.getY())
                / distance);
        return innerPoint;
    }

    //Get distance between two points
    public double get_Distance(Point2D P1, Point2D P2) {
        double distance;

        distance = Math.sqrt(Math.pow(P1.getX() - P2.getX(), 2)
                + Math.pow(P1.getY() - P2.getY(), 2));

        return distance;
    }

    //Get perpendicular line from two points
    //return Point2D array
    //Point2D[0]: first point of tengent line
    //Point2D[1]: second point of tengent line
    public Point2D[] get_TangentLinePoints(Point2D CenterPoint, Point2D VectorPoint,
            double TangentLineLength) {
        Point2D[] returnPointArray = new Point2D[2];
        returnPointArray[0] = new Point2D.Double();
        returnPointArray[1] = new Point2D.Double();
        
        double S1A_X;
        double S1A_Y;
        double S1B_X;
        double S1B_Y;
        double S1S2Angle;

        S1S2Angle = Math.atan2(VectorPoint.getX(), VectorPoint.getY());

        S1A_X = CenterPoint.getX() + Math.sin(S1S2Angle + 0.5 * Math.PI) * TangentLineLength / 2;
        S1A_Y = CenterPoint.getY() + Math.cos(S1S2Angle + 0.5 * Math.PI) * TangentLineLength / 2;
        S1B_X = CenterPoint.getX() + Math.sin(S1S2Angle - 0.5 * Math.PI) * TangentLineLength / 2;
        S1B_Y = CenterPoint.getY() + Math.cos(S1S2Angle - 0.5 * Math.PI) * TangentLineLength / 2;

        returnPointArray[0].setLocation(S1A_X, S1A_Y);
        returnPointArray[1].setLocation(S1B_X, S1B_Y);

        return returnPointArray;
    }

    //Calculate visible region of a line
    //return Point2D array
    //Point2D[0]: first point of tengent line
    //Point2D[1]: second point of tengent line
    //return null if no valid point found
    public Point2D[] find_visibleRegionPointsOfLine(Point2D regionP1, Point2D regionP2,
            Point2D lineP1, Point2D lineP2) {

        Point2D[] returnLinePointArray = new Point2D[2];
        returnLinePointArray[0] = new Point2D.Double(-1,-1);
        returnLinePointArray[1] = new Point2D.Double(-1,-1);


        Point curLocation = new Point();
        Point lastLocation = new Point(-1, -1);
        boolean wasInnerRegion = false;
        Rectangle rectRegion = new Rectangle((int) (regionP1.getX()),
                (int) (regionP1.getY()),
                (int) (regionP2.getX() - regionP1.getX() + 1),
                (int) (regionP2.getY() - regionP1.getY() + 1));
        double distance = get_Distance(lineP1, lineP2);

        for (float curDistance = 0; curDistance <= distance; curDistance++) {
            Point2D tempCurLocation = get_InnnerPointInLine(lineP1, lineP2, curDistance);
            
            curLocation.setLocation((int) (tempCurLocation.getX()),
                                    (int) (tempCurLocation.getY()));
            if (curLocation.equals(lastLocation)) {
                continue;
            }

            if (wasInnerRegion == false) {
                if (rectRegion.contains(curLocation.getX(), curLocation.getY())) {
                    wasInnerRegion = true;
                    returnLinePointArray[0].setLocation(curLocation);
                    returnLinePointArray[1].setLocation(curLocation);
                }
            }

            if (wasInnerRegion) {
                if (rectRegion.contains(curLocation.getX(), curLocation.getY())) {
                    returnLinePointArray[1].setLocation(curLocation);

                } else {
                    int debugx=0;
                    break;
                }
            }


            lastLocation.setLocation(curLocation);
        }


        //if no point is found, return null
        if (returnLinePointArray[0].equals(new Point2D.Double(-1, -1))) {
            return null;
        }


        return returnLinePointArray;
    }

    //Find a center location
    //return a point of center location
    //return null if no center found
    public Point2D find_CenterLocation(Point2D srcLinePoint1, Point2D srcLinePoint2,
            Point2D detectingRegionP1, Point2D detectingRegionP2,
            int tangentLineWidth, short[][] srcGrayShortArray) {

        
        
        //Obtain two points (linePoint1, linePoint2)
        //of a given line in a visible viewport (detectingRegionP1, detectingRegionP2)
        Point2D linePoint1 = new Point2D.Double();
        Point2D linePoint2 = new Point2D.Double();

        Point2D[] visibleRegionPointOfLine = find_visibleRegionPointsOfLine(
                detectingRegionP1, detectingRegionP2, srcLinePoint1, srcLinePoint2);
        if (visibleRegionPointOfLine == null) {
            return null;
        }
        linePoint1.setLocation(visibleRegionPointOfLine[0]);
        linePoint2.setLocation(visibleRegionPointOfLine[1]);

        
        
        //Calculate length (distance) and vector of the given line
        double distance = get_Distance(linePoint1, linePoint2);
        Point2D vectorOfLine = new Point2D.Double(
                                linePoint2.getX() - linePoint1.getX(),
                                linePoint2.getY() - linePoint1.getY());
        
        
        Point2D curCenterLocation = new Point2D.Double();
        int maxPixelCount = 0; 
        
        //Scan a point along the line
        for (float curDistance = 0; curDistance <= distance; curDistance++) {

            
            int pixelCount = 0;
            Point2D curLocation =  get_InnnerPointInLine(linePoint1, linePoint2, curDistance);
            Point2D curScanningLocation = new Point2D.Double();
            //Scan a point along the tangent line (perpendicular line)
            for (float curWidth = -tangentLineWidth; curWidth <= tangentLineWidth; curWidth++) {
                curScanningLocation = get_InnnerPointInLine(linePoint1, linePoint2, curDistance+curWidth);
                
                //find tangent line
                Point2D[] tangentLine = get_TangentLinePoints(curScanningLocation, vectorOfLine, 20);
                
                
                double tangentDistance = get_Distance(tangentLine[0], tangentLine[1]);
                for (float curTangentDistance = 0; curTangentDistance <= tangentDistance; curTangentDistance++) {
                    Point2D curLocationOnTangentLine = 
                            get_InnnerPointInLine(tangentLine[0], tangentLine[1], curTangentDistance);
                    int curX = (int) (curLocationOnTangentLine.getX());
                    int curY = (int) (curLocationOnTangentLine.getY());
                    if (srcGrayShortArray[curX][curY] == 255) {
                        pixelCount++;
                    }
                }
                
            }
            
            if (pixelCount > maxPixelCount) {
                maxPixelCount = pixelCount;
                curCenterLocation.setLocation(curLocation);
            }
        }


        return curCenterLocation;
    }
    
    
    //convert angle of two points
    public double get_AngleInRadian_Of_TwoPoints(Point2D point1, Point2D point2) {
        return Math.atan2(point2.getX() - point1.getX(), point2.getY() - point1.getY());
    }
}

