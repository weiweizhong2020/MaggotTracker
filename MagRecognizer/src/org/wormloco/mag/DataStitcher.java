/*
 * DataStitcher.java	
 *
 */

package org.wormloco.mag;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.commons.math.stat.StatUtils;

/**
 * Reads points.txt, verifies stage shift is OK (fixes it when needed) and creates absolute points text file
 *
 * @author Weiwei Zhong (initial prototype), Aleman-Meza (final version)
 *
 */


public class DataStitcher {

	// convenience on println statements
	private static final PrintStream out = System.out;

	// flag for debugging purposes
	private static final boolean DEBUG = "true".equalsIgnoreCase( System.getProperty( "DEBUG" ) );

	// input data of points [frames][x=0,y=1][13]
	protected double[][][] points = null;

	// the directory containing points file
	public final String directory;

	/** total time of video in seconds, a value of -1 means data is useless or have not been read */
	public double totalTimeInSeconds = -1; 

	/** default constant for log.csv filename */
	public static final String LOG_CSV_FILENAME = "log.csv";

	public static final String BAD_FRAMES_IN_VIDEO = "(datastitcher) FATAL ERROR: bad frames in the video.";

	/** default constant for info.xml filename */
	public static final String STAGE_INFO_XML_FILE = "info.xml";

	/** default constant for abs_points.txt filename */
	public static final String ABS_POINTS_FILENAME = "abs_points.txt";

	// length of larvae at each frame (in pixels)
	protected int[] length = null;

	// mean of length of larvae at all valid frames (in pixels)
	protected int lengthMean = -1;

	// stdev of length of larvae at all valid frames (in pixels)
	protected int lengthStdev = -1;

	// keeps the stage position
	protected List<StagePosition> stagePositionList = new ArrayList<StagePosition>();

	/** formatter, for debugging purposes  */
	protected static final NumberFormat formatter3 = new DecimalFormat( "#0.000" );	

	/** another formatter, for debugging purposes  */
	protected static final NumberFormat formatter1 = new DecimalFormat( "#0.0" );	

	// remembers the average of distances between frames, already substracting for stage shifts.
	// Only small gaps are used to calculate average, 
	// small gap is when avg.differences < 3 times the average of reversed points (w.r.t. previous frame).
	protected Double avgDistancesSmallGaps = null;

	// remembers the likely places where stage-shift happens
	protected Map<Integer,Double> likelyShiftsMap = new TreeMap<Integer,Double>();

	// steps per pixel on x axis
	protected double x_steps_per_pixel = 1;

	// steps per pixel on y axis
	protected double y_steps_per_pixel = 1;

	// image width
	protected int image_width = 1;
	
	// image height
	protected int image_height = 1;

	// calculated frame rate considering total time and number of frames
	protected double actual_frame_rate = -1;

	// each frame has a time-frame value (in seconds)
	protected double[] timing = null;
	
	// each frame is related to a stage-position
	protected int[] indexInStagePosition = null;

	// absolute x, y coordinates (mm) for points	
	protected double[][][] absPoints = null; 
			

	/**
	 * Constructor, with specified points file, and stage info file
	 * @param  directory the directory where the files to be read are
	 * @param  infoFile the info filename( STAGE_INFO_XML_FILE )
	 * @param  stageFile the stage filename( LOG_CSV_FILENAME )
	 */
	public DataStitcher( String directory, String infoFile, String stageFile ) {
		if( directory.endsWith( File.separator ) == false ) {
			directory += File.separator;
		}; // if
		this.directory = directory;
		if( DEBUG == true ) {
			out.println( "\t(datastitcher) " + directory );
		}; // if
	}


	/**
	 * Default constructor
	 * @param  directory the directory where the files to be read are
	 */
	public DataStitcher( String directory ) {
		this( directory, STAGE_INFO_XML_FILE, LOG_CSV_FILENAME );
	}


	/**
	 * Reads the points file
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	public String readPoints() {
		if( directory == null ) {
			return "Directory needs to be specified!";
		}; // if
		points = readPoints( directory + Snappy.POINTS_FILENAME );
		if( points == null ) {
			return "Problem when reading " + Snappy.POINTS_FILENAME + " inside: " + directory;
		}; // if
		if( DEBUG == true ) {
			out.println( "\t(datastitcher) read " + points.length + " frames with points." );
		}; // if
		return null;
	}


	/** for testing purposes only */
	public static void main ( String[] args ) {
		if( DEBUG == true ) {
			String folder = "/data/worm_x1/";
			DataStitcher dataStitcher = new DataStitcher( folder, STAGE_INFO_XML_FILE, LOG_CSV_FILENAME );
			String error = dataStitcher.run();
			if( error != null ) {
				out.println( error );
				return;
			}; // if
		}
		else {
			out.println( "Note: Data stitcher is normally not executed as stand-alone program." );
			out.println( "bye!" );
		}; // if
	}


	/**
	 * Runs all the steps of data-stitcher
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	public String run() {
		String error = readPoints();
		if( error != null ) {
			return error;
		}; // if

		error = readFrametime();
		if( error != null ) {
			return error;
		}; // if

		error = calculateLength();
		if( error != null ) {
			return error;
		}; // if

		error = detectBadFramesViaLength();
		if( error != null ) {
			return error;
		}; // if

		error = findAndFixSwaps();
		if( error != null ) {
			return error;
		}; // if

		error = readStageFile();
		if( error != null ) {
			return error;
		}; // if

		readStageInformation();

		error = setupStagePositionIndexes();
		if( error != null ) {
			return error;
		}; // if

		error = absoluteScale( indexInStagePosition, null );
		if( error != null ) {
			return error;
		}; // if

		Double baselineDistance = calculateDistanceTraveled();
		if( baselineDistance == null ) {
			return "ERROR, unable to calculate distance traveled!";
		}; // if
		if( DEBUG == true ) {
			out.println( "\t(datastitcher) distance traveled baseline: " + formatter1.format( baselineDistance ) );
		}; // if

		error = fixStageMovements( baselineDistance );
		if( error != null ) {
			return error;
		}; // if

		error = absoluteScale( indexInStagePosition, null );
		if( error != null ) {
			return error;
		}; // if

		Double distance = calculateDistanceTraveled();
		if( distance == null ) {
			return "ERROR, unable to calculate distance traveled!";
		}; // if
		if( DEBUG == true ) {
			out.println( "\t(datastitcher) distance traveled : " + formatter1.format( distance ) );
		}; // if

		error = interpolateMissingPoints();
		if( error != null ) {
			return error;
		}; // if

		Double distance2 = calculateDistanceTraveled();
		if( distance2 == null ) {
			return "ERROR, unable to calculate distance traveled (2)!";
		}; // if
		if( distance.doubleValue() != distance2.doubleValue() && DEBUG == true ) {
			out.println( "\t(datastitcher) distance traveled : " + formatter1.format( distance2 ) + " (interpolated)" ); 
		}; // if

		error = calculateAveragesOfDistanceAtLarvaeEndings();
		if( error != null ) {
			return error;
		}; // if

		error = writeAbsolutePoins();
		if( error != null ) {
			return error;
		}; // if
		return null;
	}

	 
	/**
	 * Detects bad frames via length statistics
	 * (must be called after readPoints and after calculateLength)
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	public String detectBadFramesViaLength() {
		if( points == null ) {
			return "(datastitcher) Unable to detect bad frames via length. Must read points file first.";
		}; // if
		if( length == null ) {
			return "(datastitcher) Unable to detect bad frames via length. Must read calculate length first.";
		}; // if
		String error = calculateLengthStatistics();
		if( error != null ) {
			return error;
		}; // if

		Integer lengthPrev = null;
		Integer lengthNext = null;
		int invalidatedCount = 0;
		for( int frame = 0; frame < points.length; frame++ ) {
			lengthPrev = null;
			lengthNext = null;
			if( frame > 0 && points[ frame - 1 ] != null ) {
				lengthPrev = length[ frame - 1 ];
			}; // if
			if( frame < ( points.length - 1 ) && points[ frame + 1 ] != null ) {
				lengthNext = length[ frame + 1 ];
			}; // if

			// compare the frame with previous and next frames
			if( points[ frame ] != null && lengthPrev != null && lengthNext != null ) {
				int differencePrev = Math.abs( length[ frame ] - lengthPrev );
				int differenceNext = Math.abs( length[ frame ] - lengthNext );
				if( differencePrev > ( lengthStdev * 3 ) && differenceNext > ( lengthStdev * 3 ) ) {
					if( DEBUG == true ) {
						out.println( "[" + frame + "] \t" + " frame invalidated (length statistics) " );
					}; // if
					points[ frame ] = null;
					invalidatedCount++;
					continue;
				}; // if
			}; // if

			// compare the frame with respect to previous frame
			if( points[ frame ] != null && lengthPrev != null ) {
				int differencePrev = Math.abs( length[ frame ] - lengthPrev );
				if( length[ frame ] < lengthPrev && differencePrev > ( lengthStdev * 5 ) ) {
					out.println( "[" + frame + "] \t" + " frame invalidated (length statistics > 5 times stdev) +++++++++++++" );
					points[ frame ] = null;
					invalidatedCount++;
					continue;
				}; // if
			}; // if
		}; // for
		if( DEBUG == true ) {
			out.println( "\t(datastitcher) " + invalidatedCount + " frames were invalidated via length-statistics." );
		}; // if
		return null;
	}


	/**
	 * Calculates statistics of length in each frame; updates variables: lengthMean, lengthStdev;
	 * (callled by detectBadFramesViaLength)
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	protected String calculateLengthStatistics() {
		if( points == null ) {
			return "(datastitcher) Unable to calculate length statistics. Must read points file first.";
		}; // if
		if( length == null ) {
			return "(datastitcher) Unable to calculate length statistics. Must read calculate length first.";
		}; // if
		List<Double> list = new ArrayList<Double>();
		for( int frame = 0; frame < length.length; frame++ ) {
			if( points[ frame ] != null && length[ frame ] > 0 ) {
				list.add( new Double( length[ frame ] ) );
			}; // if
		}; // for
		Double tmp = calculateMean( list );
		if( tmp == null ) {
			return "(datastitcher) Unable to calculate mean of length.";
		}; // if
		lengthMean = (int) Math.ceil( tmp.doubleValue() );
		tmp = calculateStdev( list );
		if( tmp == null ) {
			return "(datastitcher) Unable to calculate stdev of length.";
		}; // if
		lengthStdev = (int) Math.ceil( tmp.doubleValue() );
		if( DEBUG == true ) {
			out.println( "\t(datastitcher) " + list.size() + " values for length statistics, mean = " + lengthMean + "  +/- " + lengthStdev );
		}; // if
		return null;
	}

		
	/**
	 * Reads the frame-time file and loads the info into timing array
	 * (must be called after readPoints)
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	public String readFrametime() {
		if( points == null ) {
			return "(datastitcher) Unable to continue. Must read points file first.";
		}; // if

		String filename = directory + Snappy.FRAME_TIME_FILENAME;
		List<String> linesList = new ArrayList<String>();
		try {
			BufferedReader bufferedReader = new BufferedReader( new FileReader( filename ) );
			String line = null;
			while( ( line = bufferedReader.readLine() ) != null ) {
				linesList.add( line );
			}; // while
		} 
		catch( FileNotFoundException e ) {
			e.printStackTrace();
			return "(datastitcher) unable to find: " + filename;
		} 
		catch( IOException e ) {
			e.printStackTrace();
			return "(datastitcher) input-output error while reading: " + filename;
		}; // try
		totalTimeInSeconds = -1;
		for( int index = 0; index < linesList.size(); index++ ) {
			String[] items = linesList.get( index ).split( "\t" );
			if( index == 0) {
				int totalFrames = Integer.parseInt( items[ 0 ] );
				timing = new double[ totalFrames ];
				totalTimeInSeconds = Double.parseDouble( items[ 1 ] );
			}
			else {
				int i = Integer.parseInt( items[ 0 ] );
				timing[ i ] = Double.parseDouble( items[ 1 ] );
			}; // if
		}; // for
		if( totalTimeInSeconds == -1 ) {
			return "(datastitcher) Unable to correctly read value of time in seconds.";
		}; // if
		if( points.length != timing.length ) {
			return "(datastitcher) Error, frames in points (" + points.length + ") is different than frames in frametime (" + timing.length + ")";
		}; // if
		actual_frame_rate = points.length / totalTimeInSeconds;
		if( DEBUG == true ) {
			out.println( "\t(datastitcher) duration (seconds) : " + formatter1.format( totalTimeInSeconds ) );
			out.println( "\t(datastitcher) actual frame rate  : " + formatter1.format( actual_frame_rate ) );
		}; // if
		return null;
	}


	/** 
	 * Reads the stage information file ( STAGE_INFO_XML_FILE )
	 */
	public void readStageInformation() {
		// read stage information
		readInfo( directory + STAGE_INFO_XML_FILE ); 
		if( DEBUG == true ) {
			out.println( "\t(datastitcher) image_width " + image_width );
			out.println( "\t(datastitcher) image_height " + image_height );
			out.println( "\t(datastitcher) x_steps_per_pixel " + x_steps_per_pixel );
			out.println( "\t(datastitcher) y_steps_per_pixel " + y_steps_per_pixel );
		}; // if
	}

	
	/**
	 * reads the stage information
	 * @param  filename  the filename (normally STAGE_INFO_XML_FILE)
	 */
	protected void readInfo( String filename ) {
		try{
			BufferedReader br = new BufferedReader( new FileReader( filename ) );
			String line = null;
			boolean steps = false;
			boolean equivalent = false;
			boolean pixels = false;
			boolean resolution = false;
			boolean frame = false;
			while( ( line = br.readLine() ) != null ) {
				if( line.indexOf( "<steps>" ) >= 0 ) { steps = true; continue; }
				if( line.indexOf( "</steps>" ) >= 0 ) { steps = false; continue; }
				if( line.indexOf( "<equivalent>" ) >= 0 ) { equivalent = true; continue; }
				if( line.indexOf( "</equivalent>" ) >= 0 ) { equivalent = false; continue; }
				if( line.indexOf( "<pixels>" ) >= 0 ) { pixels = true; continue; }
				if( line.indexOf( "</pixels>" ) >= 0 ) { pixels = false; continue; }
				if( line.indexOf( "<resolution>" ) >= 0 ) { resolution = true; continue; }
				if( line.indexOf( "</resolution>" ) >= 0 ) { resolution = false; continue; }
				if( line.indexOf( "<frame>" ) >= 0 ) { frame = true; continue; }
				if( line.indexOf( "</frame>") >= 0 ) { frame = false; continue; }
				
				if( steps && equivalent && pixels ) {
					if( line.indexOf( "<x>" ) >= 0 ) {
						line = line.substring( line.indexOf( "<x>" ) + 3, line.indexOf( "</x>" ) );
						x_steps_per_pixel = ( new Double( line ) ).doubleValue();
						continue;
					}; // continue
					if( line.indexOf( "<y>" ) >= 0 ) {
						line = line.substring( line.indexOf( "<y>" ) + 3, line.indexOf( "</y>" ) );
						y_steps_per_pixel = ( new Double( line ) ).doubleValue();
						continue;
					}; // continue
				}
				if( resolution ) {
					if( line.indexOf( "<width>" ) >= 0 ) {
						line = line.substring( line.indexOf( "<width>" ) + 7, line.indexOf( "</width>" ) );
						image_width = ( new Integer( line ) ).intValue();
						continue;
					}; // continue
					if( line.indexOf( "<height>" ) >= 0 ) {
						line = line.substring( line.indexOf( "<height>" ) + 8, line.indexOf( "</height>" ) );
						image_height=( new Integer( line ) ).intValue();
						continue;
					}; // continue
				}
				if( frame && line.indexOf( "<rate>" ) >= 0 ) {
					line = line.substring( line.indexOf( "<rate>" ) + 6, line.indexOf( "</rate>" ) );
					continue;
				}; // continue
			}
			br.close();			
		}
		catch ( Exception e ) {
	      out.println ( "\t(datastitcher::readInfo) Error: " + e );
	      System.exit( -1 );
     	}; // try 	
	}
	
	/**
	 * Read the points file and store all points in an array
	 * points[f][0][0]-points[f][0][12]: the X-coordinates of the 13 points on frame f;
	 * points[f][1][0]-points[f][1][12]: the Y-coordinates of the 13 points on frame f;
	 * when there is some error, it returns null ( or exits via system.exit() )
	 * @param  filename  the file name (normally POINTS_FILENAME)
	 */
	public static double[][][] readPoints( String filename ) {
		try {
			// count the total number of images
			int totalImages = 0;
			BufferedReader br = new BufferedReader( new FileReader( filename ) );
			String line = null;
			while( ( line = br.readLine() )!= null ) {
				StringTokenizer st = new StringTokenizer( line );
				String frameNo = st.nextToken();
				totalImages = ( new Integer( frameNo ) ).intValue();
			}
			br.close();
			br = new BufferedReader( new FileReader( filename ) );
			totalImages++;
			double[][][] points = new double[ totalImages ][ 2 ][ 13 ];
			
			//read points to an array
			for( int k = 0; k < totalImages; k++ ) {
				String xline = br.readLine();
				String yline = br.readLine();				
				StringTokenizer st1 = new StringTokenizer( xline );
				StringTokenizer st2 = new StringTokenizer( yline );
				// these two lines skip the token that has the frame number
				st1.nextToken();
				st2.nextToken();
				boolean valid = true;
				int c = 0;
				while( valid && st1.hasMoreTokens() ) {
					String x1 = st1.nextToken();
					String y1 = st2.nextToken();
					
					//if spine is null
					if( x1.equalsIgnoreCase( "-1.#IND00" ) ) {
						points[k] = null;
						valid = false;
						continue;
					}; // continue
					points[k][0][c] = ( new Integer( x1 ) ).intValue();
					points[k][1][c] = ( new Integer( y1 ) ).intValue();
					c++;
				}; // while
			}; // for
			br.close(); 
			return points;			
		}	
		catch( Exception e ) {
	       out.println ( "Error: " + e );
	       System.exit( -1 );
      }; // try 	
      return null;		
	}

	
	/**
	 * Calculates distance between two points
	 * @param  x1  the x of first point
	 * @param  y1  the y of first point
	 * @param  x2  the x of second point
	 * @param  y2  the y of second point
	 */
	public static double distance( double x1, double y1, double x2, double y2 ) {
		return Math.sqrt( ( x2 - x1 ) * ( x2 - x1 ) + ( y2 - y1 ) * ( y2 - y1 ) );
	}


	/**
	 * Interpolates frames (when possible) and updates 'absPoints' in-place;
	 * (must be called after stage-shift has been fixed)
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	public String interpolateMissingPoints() {
		// find invalid frames at the beginning and end
		int startFrame = 0;
		int endFrame = absPoints.length - 1;
		while( startFrame < absPoints.length && absPoints[ startFrame ] == null ) {
			startFrame++;
		}; // while
		if( startFrame > 0 ) {
			return BAD_FRAMES_IN_VIDEO + " At the beggining, " + startFrame + " bad frames.";
		}; // if
		while( endFrame >= 0 && absPoints[ endFrame ] == null ) {
			endFrame--;
		}; // while
		if( endFrame < ( absPoints.length - 1 ) ) {
			return BAD_FRAMES_IN_VIDEO + " At the end, " + ( absPoints.length - 1 - endFrame ) + " bad frames.";
		}; // if
		if( endFrame < startFrame ) {
			return "(datastitcher) FATAL ERROR: All frames in the video are invalid!";
		}; // if

		// find null frames and attempt to interpolate them
		boolean gap = false;
		int validFrameStart = startFrame;
		int fixedFramesCount = 0;
		for( int frame = startFrame; frame < endFrame; frame++ ) {
			if( absPoints[ frame ] == null ) {
				gap = true;
				continue;
			}; // if

			// have we reached the end of a gap?
			if( gap == true ) {
				gap = false;
				// only fix gap when it is not too big (5 seconds)
				if( ( frame - validFrameStart ) < ( actual_frame_rate * 5 ) ) {
					for( int j = ( validFrameStart + 1 ); j < frame; j++ ) {
						fixedFramesCount++;
						absPoints[ j ] = new double[ 2 ][ 13 ];
						for( int k = 0; k < 13; k++ ) {
							absPoints[ j ][ 0 ][ k ] = absPoints[ validFrameStart ][ 0 ][ k ] + 1.0 * ( absPoints[ frame ][ 0 ][ k ] - absPoints[ validFrameStart ][ 0 ][ k ] ) / ( frame - validFrameStart ) * ( j - validFrameStart );
							absPoints[ j ][ 1 ][ k ] = absPoints[ validFrameStart ][ 1 ][ k ] + 1.0 * ( absPoints[ frame ][ 1 ][ k ] - absPoints[ validFrameStart ][ 1 ][ k ] ) / ( frame - validFrameStart ) * ( j - validFrameStart );
						}; // for
						if( DEBUG == true ) {
							out.println( "\t(datastitcher) interpolateMissingPoints, fixed frame " + j );
						}; // if
					}; // for
				}; // if
			}; // if

			validFrameStart = frame;
		}; // for
		
		if( DEBUG == true && fixedFramesCount > 0 ) {
			out.println( "\t(datastitcher) fixed " + fixedFramesCount + " frames via extrapolation" );
		}; // if
		return null;
	}

	

	/**
	 * Writes absolute-points to text-file
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	public String writeAbsolutePoins() {
		if( absPoints == null ) {
			out.println( "ERROR, absPoints is null, in DataStitcher::writeAbsolutePoins" );
			return null;
		}; // if
		try {
			BufferedWriter bufferedWriter = new BufferedWriter( new FileWriter( directory + ABS_POINTS_FILENAME ) );	
			PrintWriter printWriter = new PrintWriter( bufferedWriter );
			printWriter.println( ">frame rate:\t" + actual_frame_rate );	
			for( int i = 0; i < absPoints.length; i++ ) {
				if( absPoints[ i ] == null ) {
					for( int j = 0; j < 12; j++ ) {
						printWriter.write ( "-1.#IND00\t-1.#IND00\t" );
					}; // for
					printWriter.println( "-1.#IND00\t-1.#IND00" );
					continue;
				}; // continue
				for( int j = 0; j < 12; j++ ) {
					printWriter.write( absPoints[i][0][j] + "\t" + absPoints[i][1][j] + "\t" );
				}; // for
				printWriter.println( absPoints[i][0][12] + "\t" + absPoints[i][1][12] ); 
			}; // for
			printWriter.close();
		}
		catch( Exception e ) {
			e.printStackTrace();
			return e.getMessage();
		}; // try
		return null;
	}


	/**
	 * Calculates length of each frame (in pixels)
	 * (must be called after readPoints)
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	public String calculateLength() {
		if( points == null ) {
			return "(datastitcher) Unable to calculate length. Must read points file first.";
		}; // if
		length = new int[ points.length ];
		for( int frame = 0; frame < points.length; frame++ ) {
			double len = -1;
			if( points[ frame ] == null ) {
				len = -1;
			}
			else {
				len = 0.0;
				for( int p = 0; p < 13; p++ ) {
					if( p > 0 ) {
						double x = points[ frame ][ 0 ][ p - 1 ];
						double y = points[ frame ][ 1 ][ p - 1 ];
						double x2 = points[ frame ][ 0 ][ p ];
						double y2 = points[ frame ][ 1 ][ p ];
						len = len + distance( x, y, x2, y2 );
					}; // if
				}; // for
			}; // if
			length[ frame ] = (int) Math.ceil( len );
		}; // for
		return null;
	}


	/** 
	 * Calculates the mean of a given set of values
	 * @param  valuesList  the list of values
	 * @return  the mean, or null when unable to calculate it
	 */
	public static Double calculateMean( List<Double> valuesList ) {
		double[] values = new double[ valuesList.size() ];
		int index = 0;
		for( Double each : valuesList ) {
			values[ index ] = each;
			index++;
		}; // for
		if( values.length == 0 ) {
			return null;
		}; // if
		return StatUtils.mean( values );
	}


	/** 
	 * Calculates the stdev of a given set of values
	 * @param  valuesList  the list of values
	 * @return  the stdev, or null when unable to calculate it
	 */
	public static Double calculateStdev( List<Double> valuesList ) {
		double[] values = new double[ valuesList.size() ];
		int index = 0;
		for( Double each : valuesList ) {
			values[ index ] = each;
			index++;
		}; // for
		if( values.length == 0 ) {
			return null;
		}; // if
		double mean = StatUtils.mean( values );
		return Math.sqrt( StatUtils.variance( values, mean ) );
	}


	/**
	 * Finds frames where the 13 points change direction from one frame to the other,
	 * and fixes them in-place , remembers frames likely to be stage shifts
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	public String findAndFixSwaps() {
		Integer prevFrame = null;
		double[] distances = new double[ 13 ];
		double[] xDifferences = new double[ 13 ];
		double[] yDifferences = new double[ 13 ];
		double[] reversedDistances = new double[ 13 ];
		double[] holdx = new double[ 13 ];
		double[] holdy = new double[ 13 ];
		List<Double> avgDistancesSmallGapList = new ArrayList<Double>();
		for( int frame = 0; frame < points.length; frame++ ) {
			boolean flagMessages = false;
			if( frame < 0 ) {
				flagMessages = true;
				out.println( "[" + frame + "] " + ( points[ frame ] == null ? " points is null!" : "" ) );
			}
			if( points[ frame ] != null ) {
				// check whether we saw a previous-frame
				if( prevFrame == null ) {
					// remember the reference frame
					prevFrame = frame;
				}
				else {
					// calculate avg distances between frames, as they are, and also reversed.
					double[] x = points[ frame ][ 0 ];
					double[] y = points[ frame ][ 1 ];
					for( int i = 0; i < distances.length; i++ ) {
						double xPrev = points[ prevFrame ][ 0 ][ i ];
						double yPrev = points[ prevFrame ][ 1 ][ i ];
						distances[ i ] = distance( x[ i ], y[ i ], xPrev, yPrev );
					}; // for
					for( int i = 0; i < distances.length; i++ ) {
						double xPrev = points[ prevFrame ][ 0 ][ i ];
						double yPrev = points[ prevFrame ][ 1 ][ i ];
						reversedDistances[ i ] = distance( x[ distances.length - i - 1 ], y[ distances.length - i - 1 ], xPrev, yPrev );
					}; // for
					double avgDistances = StatUtils.mean( distances );
					double avgReversedDistances = StatUtils.mean( reversedDistances );
					boolean swapNeeded = false;
					Double rememberAvgDistance = null;
					if( ( avgReversedDistances * 3 ) < avgDistances ) {
						swapNeeded = true;
						rememberAvgDistance = avgReversedDistances;
						avgDistancesSmallGapList.add( avgReversedDistances );
					}; // if
					if( ( avgDistances * 3 ) < avgReversedDistances ) {
						rememberAvgDistance = avgDistances;
						avgDistancesSmallGapList.add( avgDistances );
					}; // if
					if( flagMessages == true ) {
						out.println( "[" + frame + "]  (easy-case) avgDistances: " + formatter1.format( avgDistances ) + "  \tif swapped: " + formatter1.format( avgReversedDistances )
						+ " (avgReversedDistances)  " + ( swapNeeded ? "swapit" : ". noop. ")
						+ "  " + (rememberAvgDistance == null ? "-remember-avg-distance-is-null-" : "" ) );
					}
					if( swapNeeded == true ) {
						holdx = Arrays.copyOf( x, x.length );
						holdy = Arrays.copyOf( y, y.length );
						for( int p = 0; p < x.length; p++ ) {
							x[ p ] = holdx[ x.length - p - 1 ];
							y[ p ] = holdy[ y.length - p - 1 ];
						}; // for
					}
					else {
						// see whether we have was a non-easy case
						// (some repeated code here due to similar case but considering the differences)
						if( rememberAvgDistance == null ) {
							//out.println( "[" + frame + "]  lotsofmovement, likely stage-shift " );
							likelyShiftsMap.put( frame, new Double( frame ) );
							for( int p = 0; p < x.length; p++ ) {
								double xPrev = points[ prevFrame ][ 0 ][ p ];
								double yPrev = points[ prevFrame ][ 1 ][ p ];
								xDifferences[ p ] = x[ p ] - xPrev;
								yDifferences[ p ] = y[ p ] - yPrev;
								//out.println( "[" + frame + "]  lotsofmovement x " + formatter1.format( x[ p ] ) + "\t" + formatter1.format( xPrev ) + "\t" + formatter1.format( xDifferences[ p ] )
								//+ " , \t" + formatter1.format( y[p]) + "  " + formatter1.format(yPrev) + "  " + formatter1.format(yDifferences[p]));
							}; // for
							int xAvgDifference = (int) Math.ceil( StatUtils.mean( xDifferences ) );
							int yAvgDifference = (int) Math.ceil( StatUtils.mean( yDifferences ) );
							int xStdevDifference = (int) Math.ceil( Math.sqrt( StatUtils.variance( xDifferences ) ) );
							int yStdevDifference = (int) Math.ceil( Math.sqrt( StatUtils.variance( yDifferences ) ) );
							//out.println( "[" + frame + "]  lotsofmovement x__ " + xAvgDifference + " y__ " + yAvgDifference + " ,stdev: " + xStdevDifference + " " + yStdevDifference );
							// again, but with reversed points (in case that the previous frame was swapped)
							for( int p = 0; p < x.length; p++ ) {
								double xPrev = points[ prevFrame ][ 0 ][ p ];
								double yPrev = points[ prevFrame ][ 1 ][ p ];
								xDifferences[ p ] = x[ x.length - p - 1 ] - xPrev;
								yDifferences[ p ] = y[ y.length - p - 1 ] - yPrev;
								//out.println( "[" + frame + "]  lotsofmovement x " + formatter1.format( x[ p ] ) + "\t" + formatter1.format( xPrev ) + "\t" + formatter1.format( xDifferences[ p ] )
								//+ " , \t" + formatter1.format( y[p]) + "  " + formatter1.format(yPrev) + "  " + formatter1.format(yDifferences[p]));
							}; // for
							int xStdevDifferenceReversed = (int) Math.ceil( Math.sqrt( StatUtils.variance( xDifferences ) ) );
							int yStdevDifferenceReversed = (int) Math.ceil( Math.sqrt( StatUtils.variance( yDifferences ) ) );
							if( xStdevDifferenceReversed < xStdevDifference 
							&& yStdevDifferenceReversed < yStdevDifference ) {
								//out.println( "[" + frame + "]  lotsofmovement x__ " + "  " + " ,stdev: " + xStdevDifferenceReversed + " " + yStdevDifferenceReversed );
								xAvgDifference = (int) Math.ceil( StatUtils.mean( xDifferences ) );
								yAvgDifference = (int) Math.ceil( StatUtils.mean( yDifferences ) );
								//out.println( "[" + frame + "]  lotsofmovement x_2_ " + xAvgDifference + "  " + yAvgDifference + " ,stdev: " + xStdevDifference + " " + yStdevDifference );
							}; // if
							// calculate distances using the differences
							for( int i = 0; i < distances.length; i++ ) {
								double xPrev = points[ prevFrame ][ 0 ][ i ];
								double yPrev = points[ prevFrame ][ 1 ][ i ];
								distances[ i ] = distance( x[ i ] - xAvgDifference, y[ i ] - yAvgDifference, xPrev, yPrev );
							}; // for
							avgDistances = StatUtils.mean( distances );
							for( int i = 0; i < distances.length; i++ ) {
								double xPrev = points[ prevFrame ][ 0 ][ i ];
								double yPrev = points[ prevFrame ][ 1 ][ i ];
								reversedDistances[ i ] = distance( x[ distances.length - i - 1 ] - xAvgDifference, y[ distances.length - i - 1 ] - yAvgDifference, xPrev, yPrev );
							}; // for
							avgReversedDistances = StatUtils.mean( reversedDistances );
							if( flagMessages == true ) {
								out.println( "[" + frame + "]  lotsofmovement avgDistances " + formatter1.format( avgDistances) + " == " + formatter1.format(avgReversedDistances) );
							}
							rememberAvgDistance = null;
							swapNeeded = false;
							if( ( avgReversedDistances * 3 ) < avgDistances ) {
								swapNeeded = true;
								rememberAvgDistance = avgReversedDistances;
								avgDistancesSmallGapList.add( avgReversedDistances );
							}; // if
							if( ( avgDistances * 3 ) < avgReversedDistances ) {
								rememberAvgDistance = avgDistances;
								avgDistancesSmallGapList.add( avgDistances );
							}; // if
							if( swapNeeded == true ) {
								holdx = Arrays.copyOf( x, x.length );
								holdy = Arrays.copyOf( y, y.length );
								for( int p = 0; p < x.length; p++ ) {
									x[ p ] = holdx[ x.length - p - 1 ];
									y[ p ] = holdy[ y.length - p - 1 ];
								}; // for
								if( flagMessages == true ) {
									out.println( "[" + frame + "]  swappit!" );
								}
							}; // if
							if( rememberAvgDistance == null ) {
								out.println( "\t[" + frame + "]  rememberAvgDistance is null !" );
							}; // if
						}; // if
					}; // if
					prevFrame = frame;
				}; // if
			}
			else {
				prevFrame = null;
			}
		}; // for

		// we need at some (arbitrary number of) frames to calculate avg of small gaps,
		if( avgDistancesSmallGapList.size() > 99 ) {
			avgDistancesSmallGaps = calculateMean( avgDistancesSmallGapList );
			if( avgDistancesSmallGaps != null && DEBUG == true ) {
				out.println( "\t(datastitcher) the mean of small gaps is " + formatter1.format( avgDistancesSmallGaps ) );
			}; // if
		}; // if
		return null;
	}


	/**
	 * Calculates average of distances for first four, and last four points of each frame with respect to previous frame
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	public String calculateAveragesOfDistanceAtLarvaeEndings() {
		Integer prevFrame = null;
		int count = 0;
		double[] first4 = new double[ 4 ];
		double[] last4 = new double[ 4 ];
		List<Double> first4AverageList = new ArrayList<Double>();
		List<Double> last4AverageList = new ArrayList<Double>();

		for( int frame = 0; frame < absPoints.length; frame++ ) {
			if( absPoints[ frame ] != null ) {
				if( prevFrame == null ) {
					// remember the refrence frame (previous frame)
					prevFrame = frame;
				}
				else {
					// previous frame is not null
					double[] x = absPoints[ frame ][ 0 ];
					double[] y = absPoints[ frame ][ 1 ];
					for( int i = 0; i < 4; i++ ) {
						double xPrev = absPoints[ prevFrame ][ 0 ][ i ];
						double yPrev = absPoints[ prevFrame ][ 1 ][ i ];
						double hop = distance( x[ i ], y[ i ], xPrev, yPrev );
						first4[ i ] = hop;
						xPrev = absPoints[ prevFrame ][ 0 ][ i + 9 ];
						yPrev = absPoints[ prevFrame ][ 1 ][ i + 9 ];
						hop = distance( x[ i + 9 ], y[ i + 9 ], xPrev, yPrev );
						last4[ i ] = hop;
					}; // for
					first4AverageList.add( StatUtils.mean( first4 ) );
					last4AverageList.add( StatUtils.mean( last4 ) );
					prevFrame = frame;
				}; // if
			}
			else {
				// null frame
				prevFrame = null;
			}; // if
		}; // for
		Double first4Mean = calculateMean( first4AverageList );
		Double first4Stdev = calculateStdev( first4AverageList );
		Double last4Mean = calculateMean( last4AverageList );
		Double last4Stdev = calculateStdev( last4AverageList );

		if( DEBUG == true ) {
			out.println( "\t(datastitcher) first4 mean: " + first4Mean + " +- " + first4Stdev );
			out.println( "\t(datastitcher) last4 mean: " + last4Mean + " +- " + last4Stdev );
		}; // if
		if( first4Mean == null || first4Stdev == null ) {
			return "Unable to calculate statistics on first 4 points!";
		}; // if
		if( last4Mean == null || last4Stdev == null ) {
			return "Unable to calculate statistics on last 4 points!";
		}; // if
		if( first4Mean < last4Mean ) {
			// swapping of points is required
			if( DEBUG == true ) {
				out.println( "\t(datastitcher) swapping of points is required." );
			}
			double[] holdx = new double[ 13 ];
			double[] holdy = new double[ 13 ];
			for( int frame = 0; frame < absPoints.length; frame++ ) {
				if( absPoints[ frame ] == null && DEBUG == true ) {
					out.println( "\t(datastitcher::calculateAveragesOfDistanceAtLarvaeEndings) Why null frames here?" );
					continue;
				}; // if
				double[] x = absPoints[ frame ][ 0 ];
				double[] y = absPoints[ frame ][ 1 ];
				holdx = Arrays.copyOf( x, x.length );
				holdy = Arrays.copyOf( y, y.length );
				for( int p = 0; p < x.length; p++ ) {
					x[ p ] = holdx[ x.length - p - 1 ];
					y[ p ] = holdy[ y.length - p - 1 ];
				}; // for
			}; // for
		}; // if
		return null;
	}


	/** 
	 * Fixes stage movements; stage indexes are adjusted every time one is found to 
	 * make the distance traveled smaller than the baseline
	 * @param  baselineDistance  the distance traveled (baseline)
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	public String fixStageMovements( Double baselineDistance ) {
		// working copy of indexInStagePosition
		int[] indexInStageWork = Arrays.copyOf( indexInStagePosition, indexInStagePosition.length );
		int indexOfBest = 0;
		Double bestDistance = baselineDistance;
		// we start attempting to adjust index 1
		int stageIndex = 1;
		int stageDebug = -990; // used in debugging only
		while( stageIndex < stagePositionList.size() ) {
			// find the first frame with stageIndexPosition same as stageIndex
			int foundItInFrame = -1;
			for( int frame = 0; frame < points.length; frame++ ) {
				if( indexInStageWork[ frame ] == stageIndex ) {
					foundItInFrame = frame;
					//out.println( "  found stageIndex " + stageIndex + " in frame: " + foundItInFrame );
					break;
				}; // if
			}; // for
			// when not found, then might as well leave (possible error if such thing happens)
			if( foundItInFrame == -1 ) {
				out.println( "WARNING, foundItInFrame is -1, stageIndex is: " + stageIndex );
				break;
			}; // if
			// we analyze 10 frames before and 10 frames after
			int startFrame = Math.max( 0, foundItInFrame - 10 );
			int endingFrame = Math.min( points.length - 1, foundItInFrame + 10 );
			if( stageIndex >= stageDebug && stageIndex < ( stageDebug + 5 ) ) {
				out.println( "Adjust stage-index: " + stageIndex + " @frame " + foundItInFrame + " , analyze frame " + startFrame + " ... " + endingFrame );
			}
			indexOfBest = foundItInFrame;  // when no change happens, things will stay the same
			for( int analyzeFrame = startFrame; analyzeFrame <= endingFrame; analyzeFrame++ ) {
				// skip frames having too smaller index or bigger than the one we're interested
				if( indexInStageWork[ analyzeFrame ] < ( stageIndex - 1 ) 
				|| indexInStageWork[ analyzeFrame ] > stageIndex ) {
					continue;
				}; // if
				//out.println( "    analyzeFrame " + analyzeFrame + " stageIndx: " + indexInStageWork[ analyzeFrame ] );
				// modify 'work' array to see whether moving stage-index results in improvement
				for( int changeFrame = startFrame; changeFrame <= endingFrame; changeFrame++ ) {
					// skip frames having too smaller index or bigger than the one we're interested
					if( indexInStageWork[ changeFrame ] < ( stageIndex - 1 ) 
					|| indexInStageWork[ changeFrame ] > stageIndex ) {
						continue;
					}; // if
					if( changeFrame < analyzeFrame ) {
						indexInStageWork[ changeFrame ] = stageIndex - 1;
					}
					else {
						indexInStageWork[ changeFrame ] = stageIndex;
					}; // if
					//out.println( "      changeFrame " +   changeFrame + " indexInStageWork: " + indexInStageWork[ changeFrame  ] );
				}; // for
				// re-calculate the absolute-coordinates
				String error = absoluteScale( indexInStageWork, null );
				if( error != null ) {
					out.println( "\t(fixStageMovements)" + error );
					return error;
				}; // if
				Double distance = calculateDistanceTraveled();
				//out.println( "        distance: " +  formatter3.format( distance ) );
				if( distance != null && distance < bestDistance ) {
					bestDistance = distance;
					indexOfBest = analyzeFrame;
					//out.println( "         best     " +  formatter3.format( bestDistance ) );
				}; // if

			}; // for
			if( stageIndex >= stageDebug && stageIndex < ( stageDebug + 5 ) ) {
			out.println( "     -- best     " +  formatter3.format( bestDistance ) + " @ " + indexOfBest );
			}
			// update indexInStageWork with indexOfBest (almost same loop as earlier)
			for( int update = startFrame; update <= endingFrame; update++ ) {
				// skip frames having too smaller index or bigger than the one we're interested
				if( indexInStageWork[ update ] < ( stageIndex - 1 ) 
				|| indexInStageWork[ update ] > stageIndex ) {
					continue;
				}; // if
				if( update < indexOfBest ) {
					indexInStageWork[ update ] = stageIndex - 1;
				}
				else {
					indexInStageWork[ update ] = stageIndex;
				}; // if
				if( stageIndex >= stageDebug && stageIndex < ( stageDebug + 5 ) ) {
				out.println( "      updateFrame " +   update + " indexInStageWork: " + indexInStageWork[ update  ] );
				}
			}; // for

			stageIndex++;
		}; // while
		indexInStagePosition = Arrays.copyOf( indexInStageWork, indexInStageWork.length );
		return null;
	}


	/** 
	 * Reads the stage file (folder + LOG_CSV_FILENAME)
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	public String readStageFile() {
		String filename = directory + LOG_CSV_FILENAME;
		
		try {
			BufferedReader br = new BufferedReader( new FileReader( filename ) );
			String line = null;

			int frameCount = 0;
			int frame = 0;
			double stageX = 0;
			double stageY = 0;
			Double prevStageX = null;
			Double prevStageY = null;
			
			// read the log file
			while( ( line = br.readLine() ) != null ) {
				StringTokenizer st = new StringTokenizer( line, "," );
				if( st.countTokens()<5 ) {
					out.println( "skipping__" + line );
					continue; 
				}; // continue
				st.nextToken(); //skip "real time"
				String mediaT = st.nextToken(); //get media time
				
				String stage = st.nextToken();
				if( !stage.equalsIgnoreCase( "STAGE" ) ) {
					continue;
				}; // continue
				
				int p = mediaT.indexOf( ":" );
				double frameTime = ( new Integer( mediaT.substring( 0, p ) ) ).intValue();
				mediaT = mediaT.substring( p + 1 );
				p = mediaT.indexOf( ":" );
				frameTime = frameTime*60 + ( new Integer( mediaT.substring( 0,p ) ) ).intValue();
				mediaT = mediaT.substring( p+1 );
				frameTime = frameTime*60 + ( new Double( mediaT ) ).doubleValue(); //in seconds
				
				stageX = ( new Double( st.nextToken() ) ).doubleValue();
				stageY = ( new Double( st.nextToken() ) ).doubleValue();
				if( prevStageX == null && prevStageY == null ) {
					stagePositionList.add( new StagePosition( frameTime, stageX, stageY ) );
				}; // if
				if( prevStageX != null && prevStageY != null ) {
					if( prevStageX != stageX || prevStageY != stageY ) {
						stagePositionList.add( new StagePosition( frameTime, stageX, stageY ) );
					}; // if
				}; // if
				prevStageX = stageX;
				prevStageY = stageY;
			}; // while
			br.close();
		}
		catch( Exception e ) {
			e.printStackTrace();
			return "Error reading stage file: " + e.getMessage();
		}; // try
		if( DEBUG == true ) {
			out.println( "\t(datastitcher) counted " + stagePositionList.size() + " stage-coordinates" );
		}; // if
		return null;
	}

	
	/** 
	 * Sets up the array of position-index, each frame has a reference to stage position
	 * (uses array: timing)
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	public String setupStagePositionIndexes() {
		if( DEBUG == true ) {
			out.println( "\t(datastitcher) setup stage position indexes" );
		}; // if
		// just in case verifications
		if( points.length != timing.length ) {
			return "FATAL ERROR, points and timing arrays are of different size!";
		}; // if
		if( stagePositionList.size() == 0 ) {
			return "FATAL ERROR, the stage positions must at least have one item!";
		}; // if
		indexInStagePosition = new int[ points.length ];
		
		int positionIndex = 0;
		StagePosition currentStagePosition = stagePositionList.get( positionIndex );
		StagePosition nextStagePosition = currentStagePosition;
		if( stagePositionList.size() > ( positionIndex + 1 ) ) {
			nextStagePosition = stagePositionList.get( positionIndex + 1 );
		}; // if

		for( int frame = 0; frame < points.length; frame++ ) {
			indexInStagePosition[ frame ] = positionIndex;
			if( timing[ frame ] >= currentStagePosition.timeframe
			&& nextStagePosition != null
			&& timing[ frame ] > nextStagePosition.timeframe ) {
				positionIndex++;
				if( stagePositionList.size() > positionIndex ) {
					currentStagePosition = stagePositionList.get( positionIndex );
					indexInStagePosition[ frame ] = positionIndex;
				}; // if
				if( stagePositionList.size() > ( positionIndex + 1 ) ) {
					nextStagePosition = stagePositionList.get( positionIndex + 1 );
				}
				else {
					nextStagePosition = null;
				}; // if
			}; // if
		}; // for
		if( nextStagePosition != null ) {
			// see whether we can insert it at the end (only when at least 2 frames have the same position index)
			if( indexInStagePosition[ points.length - 1 ] == positionIndex 
			&&  indexInStagePosition[ points.length - 2 ] == positionIndex ) {
				indexInStagePosition[ points.length - 2 ] = positionIndex + 1;
			}; // if
		}
		return null;
	}


	/**
	 * Calculates distance traveled using absolute points using centroid (index #6)
	 * @return  the distance traveled in a double value, or null when there was an error
	 */
	public Double calculateDistanceTraveled() {
		if( absPoints == null ) {
			out.println( "ERROR, absPoints is null, in DataStitcher::calculateDistanceTraveled" );
			return null;
		}; // if
		double distance = 0.0;
		Double xPrev = null;
		Double yPrev = null;
		double x = 0;
		double y = 0;
		double howFar = 0;
		for( int frame = 0; frame < absPoints.length; frame++ ) {
			if( absPoints[ frame ] == null ) {
				continue;
			}; // if
			howFar = 0;
			x = absPoints[ frame ][ 0 ][ 6 ];
			y = absPoints[ frame ][ 1 ][ 6 ];
			if( xPrev != null && yPrev != null ) {
				howFar = distance( x, y, xPrev, yPrev );
				distance += howFar;
			}; // if
			xPrev = x;
			yPrev = y;
			if( frame <0) {
				out.println( "\t" + frame + "  " + formatter3.format( x ) + " , " + formatter3.format( y ) 
				+ " hop: " + formatter3.format( howFar ) 
				+ " sofar: " + formatter3.format( distance ) );
			}
		}; // for
		return new Double( distance );
	}


	/** 
	 * Finds absolute scale, unit: mm, center of the plate is [0,0] 
	 * (uses array: timing),
	 * skipping one measurement of step-to-micron conversion because we know that Prior stage 1 step is 1 micron
	 * @param  indexInStage  the index in the stage position
	 * @param  spacing  spacing for debugging purposes (indentation)
	 * @return  null when things go OK; otherwise it returns an error message
	 */
	public String absoluteScale( int[] indexInStage, String spacing ) {
		if( indexInStage == null ) {
			return "Unable to calculate absolute scale values without indexInStage!";
		}; // if
		if( absPoints == null ) {
			absPoints = new double[ points.length ][ 2 ][ 13 ];
		}; // if
		boolean messageFlag = false;
		int positionIndex = -1;
		StagePosition stagePosition = null;

		for( int frame = 0; frame < points.length; frame++ ) {
			messageFlag = false;
			if( frame < 33 ) {
				messageFlag = true;
			}
			if( points[ frame ] == null ) {
				absPoints[ frame ] = null;
				continue;
			}; // if
			positionIndex = indexInStage[ frame ];
			stagePosition = stagePositionList.get( positionIndex );
			for( int i = 0; i < 13; i++ ) { //see wormlab.rice.edu/labnotes/weiwei/2010/03/03/stage-positions/
				absPoints[ frame ][ 0 ][ i ] = ( (double) ( points[ frame ][ 0 ][ i ] - image_width / 2.0 ) * x_steps_per_pixel - stagePosition.x ) / 1000.0;
				absPoints[ frame ][ 1 ][ i ] = ( (double) ( image_height / 2.0 - points[ frame ][ 1 ][ i ]) * y_steps_per_pixel + stagePosition.y ) / 1000.0;
			}; // for
			if( messageFlag == true && spacing != null ) {
				out.println( spacing + "[" + frame + "] stage: " + positionIndex 
				+ " , " + points[ frame ][ 0 ][ 6 ] + "," + points[ frame ][ 1 ][ 6 ]
				+ " , " + formatter3.format( absPoints[ frame ][ 0 ][ 6 ] ) + "," + formatter3.format( absPoints[ frame ][ 1 ][ 6 ] )
				);
			}
		}
		return null;
	}
	
} // class DataStitcher

