/*
 * Filename: VelocityEffect.java
 */

package org.wormloco.mag;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.PrintStream;

import javax.media.*;
import javax.media.format.*;
import javax.media.format.VideoFormat.*;

import javax.media.util.ImageToBuffer;

import javax.swing.JPanel;

/**
 * Modified from that at: http://turing.une.edu.au/~comp311/Tutorials/Week09/GreyEffect.java
 * See also: http://stackoverflow.com/questions/7847217/video-effects-in-jmf
 * @original-author ashoka
 *
 * Draws plot of velocity of larvae (aka, speed)
 *
 * @author Aleman-Meza
 */

public class VelocityEffect extends BasicEffect {


	protected final double GAP = 0.2;

	private Video video;

	// for convenience on println statements
	private static final PrintStream out = System.out;


	/** Constructor with video-file */
	public VelocityEffect( File videoFile, JPanel targetPanel, Video video ) {
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

		int minValue = (int) Math.round( video.speedMin - 0.1 );
		int maxValue = (int) Math.ceil( video.speedMax );
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
			graphics.clearRect( START_DRAWING_ON_X - 3, 1, width - START_DRAWING_ON_X - 2, height - 3 );
			graphics.setColor( Color.GRAY );
			graphics.drawString( "Speed (mm/sec).", START_DRAWING_ON_X * 2, TOP_MARKER );
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
				Frame frame = video.frameList.get( index );
				double value = frame.speed; 
				if( value > 0 ) {
					int x = START_DRAWING_ON_X + i * SEPARATION;
					int y = BOTTOM_MARKER - (int) Math.ceil( ( ( value - minValue ) / segments ) * DRAW_AREA_HEIGHT );

					graphics.setColor( current == true ? Color.RED : Color.GRAY );
					graphics.fillOval( x - 1, y - 1, 3, 3 );
					if( prevX != null && prevY != null ) {
						graphics.setColor( Color.GRAY );
						graphics.drawLine( x, y, prevX, prevY );
					}; // if

					prevX = x;
					prevY = y;

				}; // if
			}; // for
		}; // if

		outputBuffer.copy( inputBuffer, false );
		return BUFFER_PROCESSED_OK;
    }

}

