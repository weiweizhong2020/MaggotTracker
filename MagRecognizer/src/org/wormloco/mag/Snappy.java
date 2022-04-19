/*
 * Snappy.java	
 */

package org.wormloco.mag;

import java.io.*;

import java.io.IOException;

import java.awt.Graphics;
import java.awt.Image;

import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.List;

import javax.media.Buffer;
import javax.media.CannotRealizeException;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Duration;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;
import javax.media.Time;

import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;

import javax.media.format.VideoFormat;

import javax.media.util.BufferToImage;

/**
 * Access individual video frames, finds spine (13 points) and writes text files: points and frame-time .
 *
 * Utilized tips and code snippets from:
 * https://forums.oracle.com/forums/thread.jspa?threadID=2310865&tstart=45
 * http://www.oracle.com/technetwork/java/javase/tech/index-jsp-140239.html
 *
 * @author Aleman-Meza
 */

public class Snappy implements ControllerListener {

	public static final String POINTS_FILENAME = "points.txt";

	public static final String FRAME_TIME_FILENAME = "frametime.txt";

	private boolean stateTransitionOK = true;

	private Object waitSync = new Object();

	private Player player = null;

	private boolean error = false;

	// for convenience on println statements
	private static final PrintStream out = System.out;

	private static final boolean DEBUG = "true".equalsIgnoreCase( System.getProperty( "DEBUG" ) );

	/**
	 * Main program
	 */
	public static void main( String [] args ) {
		if( args.length == 0 ) {
			System.err.println( "Please specify URL" );
			System.exit( 0 );
		}; // if
		Snappy snappy = new Snappy();
		try {
			snappy.snap( "file://"+args[ 0 ], "./" );
		}
		catch( Exception e ) {
			e.printStackTrace();
		}; // try
	}


	/**
	 * Takes snaps of the video, find the spine (13 points along the midline of the animal) in each frame, and write the results to a text file.
	 * @param  filename  name of the file
	 * @param	pointsFileDirectory  name of the directory to store text file (POINTS_FILENAME)
	 * @return  null if things go well, otherwise it returns a string with an error message
	 */
	public String snap( String filename, String pointsFileDirectory ) {

		MediaLocator mediaLocator = new MediaLocator( filename );
		if( DEBUG == true ) {
			out.println( "mediaLocator: " + mediaLocator );
		}; // if

		// select the 'native modular' player
		Manager.setHint( Manager.PLUGIN_PLAYER, new Boolean( true ) );

		try {
			player = Manager.createRealizedPlayer( mediaLocator );
		}
		catch( IOException ioe ) {
			ioe.printStackTrace();
			return ioe.getMessage();
		}
		catch( NoPlayerException npe ) {
			npe.printStackTrace();
			return npe.getMessage();
		}
		catch( CannotRealizeException cre ) {
			cre.printStackTrace();
			return cre.getMessage();
		}; // try

		player.addControllerListener( this );

		FramePositioningControl framePositioningControl = null;
		try {
			framePositioningControl = (FramePositioningControl) player.getControl( "javax.media.control.FramePositioningControl" );
		}
		catch( Exception e ) {
			out.println( "Exception (Snappy::framePositioningControl)" );
			e.printStackTrace();
		}; // try

		if( framePositioningControl == null ) {
			return "FramePositioningControl is null !";
		}; // if
		//out.println( "FramePositioningControl is ok" );

		FrameGrabbingControl frameGrabbingControl = (FrameGrabbingControl) player.getControl( "javax.media.control.FrameGrabbingControl" );
		if( frameGrabbingControl == null ) {
			return "FrameGrabbingControl is null !";
		}; // if

		Time duration = player.getDuration();
		int totalFrames = FramePositioningControl.FRAME_UNKNOWN;
		double seconds = -1;
		if( duration != Duration.DURATION_UNKNOWN ) {
			seconds = duration.getSeconds();
			out.println( "\t(duration) \t" + DataStitcher.formatter1.format( seconds ) + " seconds" );
			totalFrames = framePositioningControl.mapTimeToFrame( duration );
			if( totalFrames != FramePositioningControl.FRAME_UNKNOWN ) {
				out.println( "\t(frames) \t" + ( totalFrames + 1 ) );
			}
			else {
				out.println( "\t(frames) \tunknown" );
			}; // if
			// the following line may work or may have no effect at all (supposedly accelerates the processing of the video)
			player.setRate( 100 );
		}; // if

		//System.err.println( "\tbefore prefetch " );
		try {
			player.prefetch();
		}
		catch( Exception e ) {
			e.printStackTrace();
			return "Exception when prefetching: " + e;
		}; // try

		//System.err.println( "\tbefore player.prefetch " );
		if( ! waitForState( Player.Prefetched ) ) {
			return "Failed to prefetch";
		}; // if

		//System.err.println( "\tprefetch ok" );

		List<String> timeLines = new ArrayList<String>();
		int nextFrame = 0;
		try {
			BufferedWriter outfile = new BufferedWriter( new FileWriter( pointsFileDirectory + File.separator + POINTS_FILENAME ) );
			do {
				int currentFrame = framePositioningControl.seek( nextFrame );
				if( currentFrame != nextFrame) {
					break;
				}; // if
				nextFrame++;

				if( currentFrame != FramePositioningControl.FRAME_UNKNOWN ) {
					int mappedCurrentFrame = framePositioningControl.mapTimeToFrame( player.getMediaTime() );
					Time currentTime = framePositioningControl.mapFrameToTime( currentFrame );
					if( currentFrame != mappedCurrentFrame ){
						out.println( "Error, frames should be exactly the same, frame:" + currentFrame + "\t" + mappedCurrentFrame + "\ttime:" + currentTime.getSeconds() );
					}
					timeLines.add( currentFrame + "\t" + currentTime.getSeconds() );
				}; // if
						
				Buffer buffer = frameGrabbingControl.grabFrame();
				VideoFormat videoFormat = (VideoFormat) buffer.getFormat();
				BufferToImage bufferToImage = new BufferToImage( videoFormat );
				Image image = bufferToImage.createImage( buffer );

				if( error == true ) {
					outfile.close();
					player.close();
					return "Error happened, this video does not seem good";
				}; // if

				BufferedImage bufferedImage = new BufferedImage( videoFormat.getSize().width, videoFormat.getSize().height, BufferedImage.TYPE_3BYTE_BGR );
	
				Graphics graphics = bufferedImage.getGraphics();
				graphics.drawImage( image, 0, 0, videoFormat.getSize().width, videoFormat.getSize().height, null );

				MagImageProcessor mag = new MagImageProcessor( bufferedImage );
				if( ( currentFrame % 100 ) == 0 ) {
					mag.outputOverlayImage( pointsFileDirectory + File.separator + "file." + currentFrame + "overlay" );
				}; // if

				outfile.write( "" + currentFrame );
				if( mag.spine == null ) {
					for( int i = 0; i < 13; i++ ) {
						outfile.write( "\t-1.#IND00" );
					}
					outfile.write( "\n" + currentFrame ); 
					for( int i = 0; i < 13; i++ ) {
						outfile.write( "\t-1.#IND00" );
					}
					outfile.write("\n"); 
					continue;
				}
				for (int i=0; i<13; i++){
					outfile.write ("\t"+mag.spine[0][i]);
				}
				outfile.write("\n"+currentFrame); 
				for (int i=0; i<13; i++){
					outfile.write ("\t"+mag.spine[1][i]);
				}
				outfile.write("\n"); 
			} while( true );

			player.close();
			outfile.close();
		}
		catch( Exception e ) {
			e.printStackTrace();
			return e + "";
		}; // try

		// write the frame-times values
		if( timeLines.size() > 0 ) {
			if( totalFrames == FramePositioningControl.FRAME_UNKNOWN ) {
				timeLines.add( 0, ( totalFrames + 1 ) + "\t" + seconds );
			}
			else {
				timeLines.add( 0, nextFrame + "\t" + seconds );
			}; // if
			try {
				PrintWriter printWriter = new PrintWriter( new BufferedWriter( new FileWriter( pointsFileDirectory + File.separator + FRAME_TIME_FILENAME ) ) );
				for( String each : timeLines ) {
					printWriter.println( each );
				}; // for
				printWriter.close();
			}
			catch( IOException ioe ) {
				ioe.printStackTrace();
				return ioe.getMessage();
			}; // try
		}; // if
		return null;
	}


	boolean waitForState( int state ) {
		synchronized( waitSync ) {
			try {
				while( player.getState() != state && stateTransitionOK ) {
					//out.println( "Player state: " + player.getState() );
					waitSync.wait();
				}; // while
			}
			catch( InterruptedException ie ) {
				ie.printStackTrace();
			}
			catch( Exception e ) {
				out.println( "waiting and then exception happens!" );
				e.printStackTrace();
			}; // try
		}; // synchronized
		return stateTransitionOK;
	}


	public void controllerUpdate( ControllerEvent controllerEvent ) {
		if( error == true ) {
			return;
		}; // if
		try {
			//out.println( controllerEvent );
			if( controllerEvent instanceof javax.media.ControllerErrorEvent ) {
				throw new Exception( "ControllerErrorEvent, sorry, cannot process this video, bye" );
			}; // if
			if( controllerEvent instanceof PrefetchCompleteEvent ) {
				synchronized( waitSync ) {
					stateTransitionOK = true;
					waitSync.notifyAll();
				}
			}; // if
		}
		catch( Exception e ) {
			out.println( "Exception in the controllerupdate" );
			error = true;
			synchronized( waitSync ) {
				stateTransitionOK = true;
				waitSync.notifyAll();
			}
		}; // try
	}


} // class Snappy

