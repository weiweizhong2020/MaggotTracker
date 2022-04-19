/*
 * Filename: PositionEffect.java
 */

package org.wormloco.mag;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.PrintStream;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.VideoFormat.*;

import javax.media.util.ImageToBuffer;

import javax.swing.JPanel;
import javax.swing.JOptionPane;

/**
 * Position of the animal 
 *
 * Modified from that at: http://turing.une.edu.au/~comp311/Tutorials/Week09/GreyEffect.java
 * See also: http://stackoverflow.com/questions/7847217/video-effects-in-jmf
 * @original-author ashoka
 *
 * @author Aleman-Meza (modifications from original listed above)
 */
public class PositionEffect extends BasicEffect {

	protected int[][][] points = null;

	// the video data
	protected Video video = null;

	// the video file
	protected File videoFile = null;
    
	// for convenience on println statements
	private static final PrintStream out = System.out;

	// remember the stride number at each frame
	private Map<Integer,Integer> frameToStrideNumberMap = new TreeMap<Integer,Integer>();

	// remember the stride info at each frame
	private Map<Integer,String> frameToStrideInfoMap = new TreeMap<Integer,String>();

	// remember the frames of strides having at least one bending of body or head
	private Set<Integer> bendingFramesSet = new TreeSet<Integer>();


	/** 
	 * Constructor
	 */
	public PositionEffect( File videoFile, JPanel targetPanel, Video video ) {
		this.targetPanel = targetPanel;
		String pointsFilename = videoFile.getParent() + File.separator + "points.txt";
		File file = new File( pointsFilename );
		if( file.exists() == false ) {
			JOptionPane.showMessageDialog( null, "Unable to find points.txt file.\nLocation: " + file.getAbsolutePath() , "Unable to mark midline of larva.", JOptionPane.ERROR_MESSAGE );
			return;
		}; // if
		points = Utilities.readPoints( pointsFilename );
		if( points != null ) {
			for( int frameNumber = 0; frameNumber < points.length; frameNumber++ ) {
				if( points[ frameNumber ] == null ) {
					continue;
				}; // if
				int[] x = points[ frameNumber ][ 0 ];
				int[] y = points[ frameNumber ][ 1 ];
				Frame frame = video.frameList.get( frameNumber );
				int width = x[ 12 ] - x[ 0 ];
				double widthMm = frame.x[ 12 ] - frame.x[ 0 ];
				// at least 20 pixels (arbitrary value)
				if( Math.abs( width ) > 20 && Math.abs( widthMm ) > 0.5 ) {
					if( width < 0 && widthMm > 0 ) { 
						out.println( frameNumber + "(1)  " + width + "   mm: " + widthMm );
					}
					if( width < 0 && widthMm < 0 ) { 
						out.println( frameNumber + "(2)  " + width + "   mm: " + widthMm );
					}
					if( width > 0 && widthMm < 0 ) { 
						// swapping is needed
						int[] originalx = new int[ x.length ];
						int[] originaly = new int[ x.length ];
						for( int p = 0; p < x.length; p++ ) {
							originalx[ p ] = x[ p ];
							originaly[ p ] = y[ p ];
						}; // for
						for( int p = 0; p < x.length; p++ ) {
							x[ x.length - p - 1 ] = originalx[ p ];
							y[ x.length - p - 1 ] = originaly[ p ];
						}; // for
						continue;
					}; // if
				}
				int heigth = y[ 12 ] - y[ 0 ];
				double heightMm = frame.y[ 12 ] - frame.y[ 0 ];
				// at least 20 pixels (arbitrary value)
				if( Math.abs( heigth ) > 20 && Math.abs( heightMm ) > 0.5 ) {
					if( heigth < 0 && heightMm < 0 ) { 
						// swapping is needed
						int[] originalx = new int[ x.length ];
						int[] originaly = new int[ x.length ];
						for( int p = 0; p < x.length; p++ ) {
							originalx[ p ] = x[ p ];
							originaly[ p ] = y[ p ];
						}; // for
						for( int p = 0; p < x.length; p++ ) {
							x[ x.length - p - 1 ] = originalx[ p ];
							y[ x.length - p - 1 ] = originaly[ p ];
						}; // for
						continue;
					}; // if
					if( heigth > 0 && heightMm > 0 ) { 
						// swapping is needed
						int[] originalx = new int[ x.length ];
						int[] originaly = new int[ x.length ];
						for( int p = 0; p < x.length; p++ ) {
							originalx[ p ] = x[ p ];
							originaly[ p ] = y[ p ];
						}; // for
						for( int p = 0; p < x.length; p++ ) {
							x[ x.length - p - 1 ] = originalx[ p ];
							y[ x.length - p - 1 ] = originaly[ p ];
						}; // for
						continue;
					}; // if
				}; // if
			}; // for
		}; // if
		this.video = video;
		this.videoFile = videoFile;
		
		// remember stride number and other info
		for( int f = 0; f < video.frameList.size(); f++ ) {
			Frame frame = video.frameList.get( f );
			if( frame.stride != null ) {
				Stride stride = frame.stride;
				boolean seenMaximaFlag = false;
				int count = 0;
				boolean bendingFlag = false;
				for( int index = stride.indexFirstMinima; index <= stride.indexSecondMinima; index++ ) {
					Frame insideFrame = video.frameList.get( index );
					if( insideFrame.localMaxima == true ) {
						seenMaximaFlag = true;
					}; // if
					count = seenMaximaFlag == true ? ( count - 1 ) : ( count + 1 );
					String label = putspaces( count, seenMaximaFlag == true ? "contracting" : "extending", insideFrame.localMaxima );
					frameToStrideNumberMap.put( index, stride.strideNumber );
					frameToStrideInfoMap.put( index, label );
					if( insideFrame.bendingHead >= Video.BENDING_ANGLE_THRESHOLD 
					||  insideFrame.bendingBody >= Video.BENDING_ANGLE_THRESHOLD ) {
						bendingFlag = true;
					}; // if
				}; // for
				if( bendingFlag == true ) {
					for( int index = stride.indexFirstMinima; index < stride.indexSecondMinima; index++ ) {
						bendingFramesSet.add( index );
					}; // for
				}; // if
			}; // if
		}; // for
	}

	public static String putspaces( int n, String txt, boolean maximaFlag ) {
		if( n <= 0 ) {
			return txt;
		}; // if
		String spaces = "";
		for( int sp = 0; sp < n; sp++ ) {
			if( maximaFlag == true ) {
				spaces += "-";
			}
			else {
				spaces += " ";
			}; // if
		}; // for
		String ret = "";
		for( int i = 0; i < txt.length(); i++ ) {
			ret += txt.charAt( i ) + spaces;
		}; // for
		return ret;
	}


	/** do the processing **/
	public int process(Buffer inputBuffer, Buffer outputBuffer){
		int frameNumber = (int) inputBuffer.getSequenceNumber() - 1;
		BufferedImage image = buffer2Image( inputBuffer );
		if( image == null ) {
			out.println( "buffer2image returned null, leaving" );
			System.exit( 1 );
		}; // if

		Graphics panelGraphics = targetPanel.getGraphics();
		if( panelGraphics == null ) {
			out.println( "panelGraphics is null" );
		}
		else {
			if( points == null ) {
				panelGraphics.drawImage( image, 0, 0, targetPanel.getWidth() - 1, targetPanel.getHeight() - 1, targetPanel );
				return BUFFER_PROCESSED_OK;
			}; // if
			Graphics graphics = image.getGraphics();
			graphics.setColor( Color.BLUE );
			graphics.setFont( new Font(null, Font.PLAIN, 16) );
			graphics.drawString( "Frame: " + frameNumber  + " of " + video.frameList.size(), 20, 40 );
			graphics.setColor( Color.RED );
			Frame frame = video.frameList.get( frameNumber );

			if( targetPanel.getName().contains( MagViewer.STRIDE_INFO ) == true ) {
				Integer strideNumber = frameToStrideNumberMap.get( frameNumber );
				if( strideNumber != null ) {
					graphics.drawString( "stride " + strideNumber + "  " + frameToStrideInfoMap.get( frameNumber ), 140, 120 );
				}; // if
			}; // if

			int x = 0; 
			int y = 0;
			int prevX = 0; 
			int prevY = 0;
			if( points != null ) {
				for( int i = 0; i < 13; i++ ) {
					if( points[ frameNumber ] == null ) {
						continue;
					}; // if
					x = points[ frameNumber ][ 0 ][ i ];
					y = points[ frameNumber ][ 1 ][ i ];
					if( frame.onTheRepellent == true ) {
						graphics.fillOval( x - 3, y - 3, 5, 5 );
						graphics.drawLine( x - 10, y - 10, x + 10, y + 10 );
						graphics.drawLine( x - 10, y + 10, x + 10, y - 10 );
					}
					else {
						if( i > 0 ) {
							graphics.drawOval( x - 3, y - 3, 5, 5 );
						}
						else {
							graphics.fillOval( x - 3, y - 3, 5, 5 );
						}
					}; // if
					if( i > 0 ) {
						graphics.drawLine( x, y, prevX, prevY );
					}; // if
					prevX = x;
					prevY = y;
				}; // for
				// points used in body-angle
				int p_3445_x = (int) Math.round( ( points[ frameNumber ][ 0 ][ 3 ] + points[ frameNumber ][ 0 ][ 4 ] * 2 + points[ frameNumber ][ 0 ][ 5 ] ) / 4.0 );
				int p_3445_y = (int) Math.round( ( points[ frameNumber ][ 1 ][ 3 ] + points[ frameNumber ][ 1 ][ 4 ] * 2 + points[ frameNumber ][ 1 ][ 5 ] ) / 4.0 );
					int p_789_x = (int) Math.round( ( points[ frameNumber ][ 0 ][ 7 ] + points[ frameNumber ][ 0 ][ 8 ] * 2 + points[ frameNumber ][ 0 ][ 9 ] ) / 4.0 );
					int p_789_y = (int) Math.round( ( points[ frameNumber ][ 1 ][ 7 ] + points[ frameNumber ][ 1 ][ 8 ] * 2 + points[ frameNumber ][ 1 ][ 9 ] ) / 4.0 );
				if( targetPanel.getName().contains( MagViewer.HEAD_ANGLE ) == true ) {
					// draw the head-line, from P0 to P1
					int p0x = points[ frameNumber ][ 0 ][ 0 ];
					int p0y = points[ frameNumber ][ 1 ][ 0 ];
					int p1x = points[ frameNumber ][ 0 ][ 1 ];
					int p1y = points[ frameNumber ][ 1 ][ 1 ];
					x = p0x - p1x;
					y = p0y - p1y;
					graphics.setColor( Color.ORANGE );
					graphics.drawLine( p0x - x * 4, p0y - y * 4, p1x + x * 6, p1y + y * 6 );
					// upperbody-line, from P{1,2,3} to P{4,5,6}
					int p_123_x = (int) Math.round( ( points[ frameNumber ][ 0 ][ 1 ] + points[ frameNumber ][ 0 ][ 2 ] + points[ frameNumber ][ 0 ][ 3 ] ) / 3.0 );
					int p_123_y = (int) Math.round( ( points[ frameNumber ][ 1 ][ 1 ] + points[ frameNumber ][ 1 ][ 2 ] + points[ frameNumber ][ 1 ][ 3 ] ) / 3.0 );
					int p_345_x = (int) Math.round( ( points[ frameNumber ][ 0 ][ 3 ] + points[ frameNumber ][ 0 ][ 4 ] + points[ frameNumber ][ 0 ][ 5 ] ) / 3.0 );
					int p_345_y = (int) Math.round( ( points[ frameNumber ][ 1 ][ 3 ] + points[ frameNumber ][ 1 ][ 4 ] + points[ frameNumber ][ 1 ][ 5 ] ) / 3.0 );
					graphics.drawOval( p_123_x - 3, p_123_y - 3, 5, 5 );
					graphics.drawOval( p_345_x - 3, p_345_y - 3, 5, 5 );
					x = p_123_x - p_345_x;
					y = p_123_y - p_345_y;
					graphics.drawLine( p_123_x - x * 2, p_123_y - y * 2, p_345_x + x * 2, p_345_y + y * 2 );
				}; // if
				if( targetPanel.getName().contains( MagViewer.BODY_ANGLE ) == true ) {
					// draw the line-near-head
					int p_012_x = (int) Math.round( ( points[ frameNumber ][ 0 ][ 0 ] + points[ frameNumber ][ 0 ][ 1 ] * 2 + points[ frameNumber ][ 0 ][ 2 ] ) / 4.0 );
					int p_012_y = (int) Math.round( ( points[ frameNumber ][ 1 ][ 0 ] + points[ frameNumber ][ 1 ][ 1 ] * 2 + points[ frameNumber ][ 1 ][ 2 ] ) / 4.0 );
					graphics.setColor( Color.GREEN );
					graphics.drawOval( p_012_x - 3, p_012_y - 3, 5, 5 );
					graphics.drawOval( p_3445_x - 3, p_3445_y - 3, 5, 5 );
					x = p_012_x - p_3445_x;
					y = p_012_y - p_3445_y;
					graphics.drawLine( p_012_x - x * 3, p_012_y - y * 3, p_3445_x + x * 2, p_3445_y + y * 2 );
					// draw the line-near-tail
					graphics.drawOval( p_789_x - 3, p_789_y - 3, 5, 5 );
					int p_101112_x = (int) Math.round( ( points[ frameNumber ][ 0 ][ 10 ] + points[ frameNumber ][ 0 ][ 11 ] * 2 + points[ frameNumber ][ 0 ][ 12 ] ) / 4.0 );
					int p_101112_y = (int) Math.round( ( points[ frameNumber ][ 1 ][ 10 ] + points[ frameNumber ][ 1 ][ 11 ] * 2 + points[ frameNumber ][ 1 ][ 12 ] ) / 4.0 );
					graphics.drawOval( p_101112_x - 3, p_101112_y - 3, 5, 5 );
					x = p_789_x - p_101112_x;
					y = p_789_y - p_101112_y;
					graphics.drawLine( p_789_x - x * 3, p_789_y - y * 3, p_101112_x + x * 3, p_101112_y + y * 3 );
				}
			}; // if

			// is the head bending?
			int angleHeadDegrees = (int) Math.round( frame.bendingHead );
			if( angleHeadDegrees >= Video.BENDING_ANGLE_THRESHOLD ) {
				graphics.setColor( Color.BLUE );
				graphics.drawString( "Bending (head) " + angleHeadDegrees + " degrees.", 20, 440 );
			}; // if

			// is the body bending?
			int angleBodyDegrees = (int) Math.round( frame.bendingBody );
			if( angleBodyDegrees >= Video.BENDING_ANGLE_THRESHOLD ) {
				graphics.setColor( Color.BLUE );
				graphics.drawString( "Bending (body) " + angleBodyDegrees + " degrees.", 20, 460 );
			}; // if

			// is it on the repellent?
			if( frame.onTheRepellent == true ) {
				graphics.setColor( Color.BLUE );
				graphics.drawString( "Over/near repellent", 500, 440 );
			}; // if

			if( frame.directionChangeAngle != null
			&&  frame.directionChangeAngle >= Video.ANGLE_THRESHOLD_FOR_DIRECTION_CHANGE ) {
				graphics.setColor( Color.BLUE );
				graphics.drawString( "Direction change: " + format0( Math.floor( frame.directionChangeAngle ) ) + " degrees.", 400, 400 );
			}; // if

			panelGraphics.drawImage( image, 0, 0, targetPanel.getWidth() - 1, targetPanel.getHeight() - 1, targetPanel );

		}; // if
		return BUFFER_PROCESSED_OK;
    }


    /**
    * Utility: validate that the Buffer object's data size is at least
    * newSize bytes.
    * @return array with sufficient capacity
    **/
    protected byte[] validateByteArraySize(Buffer buffer,int newSize) {
        Object objectArray=buffer.getData();
        byte[] typedArray;
        if (objectArray instanceof byte[]) { // is correct type AND not null
            typedArray=(byte[])objectArray;
            if (typedArray.length >= newSize ) // is sufficient capacity
                return typedArray;
        }
        typedArray = new byte[newSize];
        buffer.setData(typedArray);
        return typedArray;
    }
}

