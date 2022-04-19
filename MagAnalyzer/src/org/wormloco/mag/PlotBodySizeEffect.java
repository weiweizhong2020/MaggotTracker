/*
 * Filename: PlotBodySizeEffect.java
 */

package org.wormloco.mag;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.PrintStream;

import javax.media.Buffer;

import javax.media.util.ImageToBuffer;

import javax.swing.JPanel;


/**
 * Modified from that at: http://turing.une.edu.au/~comp311/Tutorials/Week09/GreyEffect.java
 * See also: http://stackoverflow.com/questions/7847217/video-effects-in-jmf
 * @original-author ashoka
 *
 * Draws the body-size of larvae on a Component object
 *
 * @author Aleman-Meza
 */

public class PlotBodySizeEffect extends BasicEffect {

	// reference to the video data
	protected Video video = null;

	// for convenience on println statements
	private static final PrintStream out = System.out;

	/** Constructor with video-file */
	public PlotBodySizeEffect( File videoFile, JPanel targetPanel, Video video ) {
		this.video = video;
		this.targetPanel = targetPanel;
	}


	/** do the processing **/
	public int process(Buffer inputBuffer, Buffer outputBuffer){
		int frameNumber = (int) inputBuffer.getSequenceNumber() - 1;
		Graphics graphics = targetPanel.getGraphics();
		int width = targetPanel.getWidth();
		int height = targetPanel.getHeight();
		final int A_HANDFUL_OF_FRAMES = 20;
		final int TOP_MARKER = 20;
		final int BOTTOM_MARKER = height - 20;
		final int DRAW_AREA_HEIGHT = BOTTOM_MARKER - TOP_MARKER;
		final int X_OF_Y_AXIS = 20;
		final int MARKER_LENGTH = 6;
		final int START_DRAWING_ON_X = X_OF_Y_AXIS + MARKER_LENGTH + 10;
		final int SEPARATION = 3;

		int minValue = (int) Math.round( video.bodyLengthMin - 0.5 );

		int maxValue = (int) Math.ceil( video.bodyLengthMax );
		int segments = maxValue - minValue;

		if( graphics != null ) {
			graphics.setFont( new Font( null, Font.PLAIN, 16 ) );
			if( frameNumber < A_HANDFUL_OF_FRAMES ) {
				// draw the y-axis
				int added = 0;
				int i = 0;
				for( int marker = maxValue; marker >= minValue; marker-- ) {
					added = (int) Math.round( i * DRAW_AREA_HEIGHT * 1.0 / segments );
					graphics.setColor( Color.GRAY );
					graphics.drawString( marker + "", X_OF_Y_AXIS - 10, TOP_MARKER + added + 4 );
					graphics.setColor( Color.BLACK );
					graphics.drawLine( X_OF_Y_AXIS, TOP_MARKER + added, X_OF_Y_AXIS + MARKER_LENGTH, TOP_MARKER + added );
					i++;
				}; // for
				graphics.drawLine( X_OF_Y_AXIS + MARKER_LENGTH, TOP_MARKER - 10, X_OF_Y_AXIS + MARKER_LENGTH, BOTTOM_MARKER + 10 );
			}; // if

			int maxLines = (int) Math.round( ( width - START_DRAWING_ON_X * 2.0 ) / SEPARATION );
			Frame frame = video.frameList.get( frameNumber );

			graphics.clearRect( START_DRAWING_ON_X - 3, 1, width - START_DRAWING_ON_X - 2, height - 3 );
			graphics.setColor( Color.GRAY );
			graphics.drawString( format( frame.bodyLength ) + " mm. length",
				START_DRAWING_ON_X * 2, TOP_MARKER );
			Integer prevSmoothX = null;
			Integer prevSmoothY = null;
			Integer prevX = null;
			Integer prevY = null;
			for( int i = 0; i < maxLines; i++ ) {
				int index = i;
				boolean current = i == frameNumber;
				if( frameNumber > maxLines ) {
					index = i + ( frameNumber - maxLines );
					current = i == ( maxLines - 1 );
				}
				else if( i > frameNumber ) {
					break;
				}; // if
				Frame eachFrame = video.frameList.get( index );
				double value = eachFrame.bodyLength;
				double valueSmooth = eachFrame.smoothBodyLength;
				if( valueSmooth <= 0.0 ) {
					prevSmoothX = null;
					prevSmoothY = null;
				}; // if
				// do the drawing
				if( value > 0 ) {
					int x = START_DRAWING_ON_X + i * SEPARATION;
					int y = BOTTOM_MARKER - (int) Math.ceil( ( ( value - minValue ) / segments ) * DRAW_AREA_HEIGHT );
					int xSmooth = x;
					int ySmooth = BOTTOM_MARKER - (int) Math.ceil( ( ( valueSmooth - minValue ) / segments ) * DRAW_AREA_HEIGHT );
					// draw the smooth-length line first
					if( prevSmoothX != null && prevSmoothY != null ) {
						graphics.setColor( Color.BLUE );
						graphics.drawLine( xSmooth, ySmooth, prevSmoothX, prevSmoothY );
					}; // if

					graphics.setColor( current == true ? Color.RED : Color.GRAY );
					graphics.fillOval( x - 1, y - 1, 3, 3 );
					if( prevX != null && prevY != null ) {
						graphics.setColor( Color.GRAY );
						graphics.drawLine( x, y, prevX, prevY );
					}; // if

					// show local maxima
					if( eachFrame.localMaxima == true ) {
						graphics.drawString( "*", x, y - 6 );
					}; // if
					// show local minima
					if( eachFrame.localMinima == true ) {
						graphics.drawString( "*", x, y + 22 );
					}; // if
					if( eachFrame.stride != null ) {
						int xFirstMinima = START_DRAWING_ON_X + ( i - ( eachFrame.stride.indexSecondMinima - eachFrame.stride.indexFirstMinima ) ) * SEPARATION;
						int xText = ( x + xFirstMinima ) / 2;
						// was any frame bending?
						Color theColor = Color.GRAY;
						graphics.setColor( theColor );
						if( xText > ( START_DRAWING_ON_X + SEPARATION + 12 ) ) {
							graphics.drawString( "" + eachFrame.stride.strideNumber, xText, y + 22 + 22 );
							//graphics.drawString( "" + format( eachFrame.stride.distanceTraveled ), xText - 12, y + 40 + 22 );
						}; // if
						graphics.drawLine( x, y + 20, Math.max( xFirstMinima, START_DRAWING_ON_X ), y + 20 );
					}; // if
					prevX = x;
					prevY = y;

					prevSmoothX = xSmooth;
					prevSmoothY = ySmooth;
				}; // if
			}; // for
		}; // if

		outputBuffer.copy( inputBuffer, false );
		return BUFFER_PROCESSED_OK;
    }

}

