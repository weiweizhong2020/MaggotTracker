/*
 * Filename: TrackEffect.java
 */

package org.wormloco.mag;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.imageio.ImageIO;
import javax.media.Buffer;
import javax.media.util.ImageToBuffer;

import javax.swing.JPanel;

/**
 * Modified from that at: http://turing.une.edu.au/~comp311/Tutorials/Week09/GreyEffect.java
 * See also: http://stackoverflow.com/questions/7847217/video-effects-in-jmf
 * @original-author ashoka
 *
 * Draws the track on a Component object
 *
 * @author Aleman-Meza (modifications from original listed above)
 */

public class TrackEffect extends BasicEffect {

	// reference to the video data
	protected Video video = null;

	/** constant for plate width (true measure is around 85 but using a different one that displays better on screen) */
	public final double PLATE_WIDTH = 65.0;

	protected int panelWidth;

	protected int panelHeight;

	protected int panelSmallerSide;
    
	// for convenience on println statements
	private static final PrintStream out = System.out;


	/** Constructor with video-file */
	public TrackEffect( File videoFile, JPanel targetPanel, Video video ) {
		super();
		this.targetPanel = targetPanel;
		
		int width = (int) Math.round( Math.abs( video.maxX ) + Math.abs( video.minX ) + 1 );
		int height = (int) Math.round( Math.abs( video.maxY ) + Math.abs( video.minY ) + 1 );

		panelWidth = targetPanel.getWidth() - 10;
		panelHeight = targetPanel.getHeight() - 10;
		panelSmallerSide = Math.min( panelWidth, panelHeight );
		this.video = video;
		drawTrackImage( videoFile );
	}


	/**
	 * Draws the track image
	 */
	public void drawTrackImage( File videoFile ) {
		BufferedImage bufferedImage = new BufferedImage( 310, 310, BufferedImage.TYPE_INT_ARGB );
		Graphics2D graphics = bufferedImage.createGraphics();
		// draw the plate (these lines are similar to the ones inside 'process' method).
		graphics.setColor( Color.LIGHT_GRAY );
		graphics.fillRect( scaleX( - PLATE_WIDTH / 2.0 ), scaleY( - PLATE_WIDTH / 2.0 ), magnitude( PLATE_WIDTH ), magnitude( PLATE_WIDTH ) );
		// draw the crawling area (these lines are similar to the ones inside 'process' method).
		graphics.setColor( Color.WHITE );
		graphics.fillOval( scaleX( - Frame.RADIOUS ), scaleY( - Frame.RADIOUS ), magnitude( Frame.RADIOUS * 2 ), magnitude( Frame.RADIOUS * 2 ) );

		for( int oldFrameIndex = 0; oldFrameIndex < video.frameList.size(); oldFrameIndex++ ) {
			Frame oldFrame = video.frameList.get( oldFrameIndex );
			// draw the track of old frames
			int x = scaleX( oldFrame.x[ Frame.CENTER_POINT ] );
			int y = scaleY( - oldFrame.y[ Frame.CENTER_POINT ] );
			if( oldFrameIndex > 0 ) {
				// draw the previous point
				Frame oldFrameMinusOne = video.frameList.get( oldFrameIndex - 1 );
				int prevX = scaleX( oldFrameMinusOne.x[ Frame.CENTER_POINT ] );
				int prevY = scaleY( - oldFrameMinusOne.y[ Frame.CENTER_POINT ] );
				if( oldFrame.directionChangeAngle == null ) {
					if( oldFrameIndex < video.frameList.size() ) {
						graphics.setColor( Color.GRAY );
					}
					else {
						graphics.setColor( Color.BLACK );
					}; // if
				}
				else if( oldFrame.directionChangeAngle < Video.ANGLE_THRESHOLD_FOR_DIRECTION_CHANGE ) {
					graphics.setColor( Color.GREEN );
				}
				else {
					graphics.setColor( Color.RED );
				}; // if
				if( oldFrame.onTheRepellent == true ) {
					graphics.setColor( graphics.getColor().darker() );
				}; // if
				// draw the previous point
				graphics.fillOval( prevX - 1, prevY - 1, 3, 3 );
				double distance = Utilities.distance( x, y, prevX, prevY );
				if( Math.ceil( distance ) >= 2 ) {
					graphics.drawLine( x, y, prevX, prevY );
				}; // if
			}; // if
		}; // for
		graphics.setColor( Color.BLACK );
		Frame frameZero = video.frameList.get( 0 );
		graphics.drawLine( scaleX( frameZero.x[ Frame.CENTER_POINT ] ), scaleY( - frameZero.y[ Frame.CENTER_POINT ] - 2 ), scaleX( frameZero.x[ Frame.CENTER_POINT ] ), scaleY( - frameZero.y[ Frame.CENTER_POINT ] + 2 ) );
		graphics.drawLine( scaleX( frameZero.x[ Frame.CENTER_POINT ] - 2 ), scaleY( - frameZero.y[ Frame.CENTER_POINT ] ), scaleX( frameZero.x[ Frame.CENTER_POINT ] + 2 ), scaleY( - frameZero.y[ Frame.CENTER_POINT ] ) );
		try {
			File outputFile = new File( videoFile.getParent(), "track-image.png" );
			ImageIO.write( bufferedImage, "png", outputFile );
		}
		catch( IOException ioe ) {
			ioe.printStackTrace();
		}; // try
	}


	/**
	 * Scales a value to the coordinate-x of the panel in pixels
	 */
	public int scaleX( double value ) {
		return (int) Math.ceil( ( value / ( PLATE_WIDTH / 2.0 ) ) * ( panelSmallerSide ) / 2.0 + panelWidth / 2.0 ) + 3;
	}


	/**
	 * Scales a value to the coordinate-y of the panel in pixels
	 */
	public int scaleY( double value ) {
		return (int) Math.ceil( ( value / ( PLATE_WIDTH / 2.0 ) ) * ( panelSmallerSide ) / 2.0 + panelHeight / 2.0 ) + 3;
	}

	public int magnitude( double value ) {
		//return (int) Math.ceil( Math.abs( scaleX( value + value ) - scaleX( value ) ) );
		return (int) Math.ceil( ( value / ( PLATE_WIDTH ) ) * ( panelSmallerSide ) );
	}

	/** do the processing **/
	public int process(Buffer inputBuffer, Buffer outputBuffer){
		int frameNumber = (int) inputBuffer.getSequenceNumber() - 1;
		Graphics graphics = targetPanel.getGraphics();
		if( graphics != null ) {
			// draw the plate
			graphics.setColor( Color.LIGHT_GRAY );
			graphics.fillRect( scaleX( - PLATE_WIDTH / 2.0 ), scaleY( - PLATE_WIDTH / 2.0 ), magnitude( PLATE_WIDTH ), magnitude( PLATE_WIDTH ) );

			// draw the crawling area
			graphics.setColor( Color.WHITE );
			graphics.fillOval( scaleX( - Frame.RADIOUS ), scaleY( - Frame.RADIOUS ), magnitude( Frame.RADIOUS * 2 ), magnitude( Frame.RADIOUS * 2 ) );
			graphics.setColor( Color.GREEN );
			Frame frameZero = video.frameList.get( 0 );
			graphics.drawLine( scaleX( frameZero.x[ Frame.CENTER_POINT ] ), scaleY( - frameZero.y[ Frame.CENTER_POINT ] - 2 ), scaleX( frameZero.x[ Frame.CENTER_POINT ] ), scaleY( - frameZero.y[ Frame.CENTER_POINT ] + 2 ) );
			graphics.drawLine( scaleX( frameZero.x[ Frame.CENTER_POINT ] - 2 ), scaleY( - frameZero.y[ Frame.CENTER_POINT ] ), scaleX( frameZero.x[ Frame.CENTER_POINT ] + 2 ), scaleY( - frameZero.y[ Frame.CENTER_POINT ] ) );

			for( int oldFrameIndex = 0; oldFrameIndex < ( frameNumber - 1 ); oldFrameIndex++ ) {
				Frame oldFrame = video.frameList.get( oldFrameIndex );
				// draw the track of old frames
				int x = scaleX( oldFrame.x[ Frame.CENTER_POINT ] );
				int y = scaleY( - oldFrame.y[ Frame.CENTER_POINT ] );
				if( oldFrameIndex > 0 ) {
					// draw the previous point
					Frame oldFrameMinusOne = video.frameList.get( oldFrameIndex - 1 );
					int prevX = scaleX( oldFrameMinusOne.x[ Frame.CENTER_POINT ] );
					int prevY = scaleY( - oldFrameMinusOne.y[ Frame.CENTER_POINT ] );
					if( oldFrame.directionChangeAngle == null ) {
						if( oldFrameIndex < video.frameList.size() ) {
							graphics.setColor( Color.GRAY );
						}
						else {
							graphics.setColor( Color.BLACK );
						}; // if
					}
					else if( oldFrame.directionChangeAngle < Video.ANGLE_THRESHOLD_FOR_DIRECTION_CHANGE ) {
						graphics.setColor( Color.GREEN );
					}
					else {
						graphics.setColor( Color.RED );
					}; // if
					if( oldFrame.onTheRepellent == true ) {
						graphics.setColor( graphics.getColor().darker() );
					}; // if
					// draw the previous point
					graphics.fillOval( prevX - 1, prevY - 1, 3, 3 );
					double distance = Utilities.distance( x, y, prevX, prevY );
					if( Math.ceil( distance ) >= 2 ) {
						graphics.drawLine( x, y, prevX, prevY );
					}; // if
				}; // if
			}; // for

			// draw the current frame's larvae
			graphics.setColor( Color.BLUE );
			graphics.setColor( graphics.getColor().brighter() );
			Frame frame = video.frameList.get( frameNumber );
			int x = scaleX( frame.x[ 0 ] );
			int y = scaleX( - frame.y[ 0 ] );
			if( frame.onTheRepellent == true ) {
				graphics.setColor( graphics.getColor().darker() );
			}; // if
			int otherX = scaleX( frame.x[ 3 ] );
			int otherY = scaleY( - frame.y[ 3 ] );
			graphics.drawLine( x, y, otherX, otherY );
			x = scaleX( frame.x[ 6 ] );
			y = scaleX( - frame.y[ 6 ] );
			graphics.drawLine( x, y, otherX, otherY );
			otherX = scaleX( frame.x[ 9 ] );
			otherY = scaleY( - frame.y[ 9 ] );
			graphics.drawLine( x, y, otherX, otherY );
			x = scaleX( frame.x[ 12 ] );
			y = scaleX( - frame.y[ 12 ] );
			graphics.drawLine( x, y, otherX, otherY );
		}; // if

		outputBuffer.copy( inputBuffer, false );
		return BUFFER_PROCESSED_OK;
    }

}

