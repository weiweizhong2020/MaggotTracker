/*
 * Filename: Video.java
 */

package org.wormloco.mag;

import org.apache.commons.math.stat.StatUtils;

import java.awt.geom.Line2D;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * One video file with all its data of parameters;
 * 
 * @author  Boanerges Aleman-Meza
 */

public class Video {

	/** the bending angle threshold */
	public static final double BENDING_ANGLE_THRESHOLD = 45.0;
	
	/** the threshold of distance for direction change */
	public static final double DISTANCE_THRESHOLD_FOR_DIRECTION_CHANGE = 2.25;

	/** the threshold of angle for direction change */
	public static final double ANGLE_THRESHOLD_FOR_DIRECTION_CHANGE = 25.0;

	/** the name of frametime text file */
	public static final String FRAMETIME_TXT = "frametime.txt";

	// flag for debugging purposes
	private static final boolean DEBUG = "true".equalsIgnoreCase( System.getProperty( "DEBUG" ) );

	// name of the larvae, it is obtained from the directory name
	private String larvae;

	// directory that contains the ABS_POINTS_FILENAME text-file
	private String directory;
	
	// the frame rate
	private Double frameRate;
	
	// parameter values (calculated or obtained from the video)
	protected Map<String,String> valuesMap;

	// the list of frames
	protected List<Frame> frameList;

	// the list of strides
	protected List<Stride> strideList;

	// the list of runs
	protected List<Run> runList;

	/** minimum value of x found in all frames */
	public Double minX;

	/** maximum value of x found in all frames */
	public Double maxX;

	/** minimum value of y found in all frames */
	public Double minY;

	/** maximum value of y found in all frames */
	public Double maxY;

	// maximum speed value in all frames 
	protected Double speedMax = null;

	// minimum speed value in all frames
	protected Double speedMin = null;

	// minimum body_length value in all frames
	protected Double bodyLengthMin = null;

	// maximum body_length value in all frames
	protected Double bodyLengthMax = null;

	// total # frames inside (not on the repellent)
	protected Integer totalFramesInside = null;

	// total # frames outside (on the repellent)
	protected Integer totalFramesOverRepellent = null;

	// remembers the average of number of frames in strides
	Integer avgNumberOfFramesPerStride = null;

	// for convenience on println statements
	private static final PrintStream out = System.out;

	// for formatting output purposes 
	protected static final NumberFormat formatter;
	protected static final NumberFormat formatter3;


	static { // static constructor
		formatter = new DecimalFormat( "#0.0####" );
		formatter3 = new DecimalFormat( "#0.0##" );
		if( DEBUG == true ) {
			out.println( "(Video.java) debugging is enabled." );
		}; // if
	}


	/** Default constructor */
	public Video() {
		valuesMap = new LinkedHashMap<String,String>();
		frameList = new ArrayList<Frame>();
		strideList = new ArrayList<Stride>();
		runList = new ArrayList<Run>();
		resetEverything();
	}


	/**
	 * Sets the directory for the video
	 * @param  directory  the directory containing the ABS_POINTS_FILENAME file
	 */
	public void setDirectory( String directory ) {
		if( directory.endsWith( File.separator ) == false ) {
			directory += File.separator;
		}; // if
		this.directory = directory;
		int k = directory.lastIndexOf( "/", directory.length() - 2 );
		int j = directory.lastIndexOf( "\\", directory.length() - 2 );
		int index = 0;
		index = k < j ? j : index;
		index = k > j ? k : index;
		larvae = directory.substring( index + 1, directory.length() - 1 );
		valuesMap = new LinkedHashMap<String,String>();
		valuesMap.put( "larvae", larvae );
	}

	
	/** 
	 * Read the data from ABS_POINTS_FILENAME text file,
	 * sets these values into valuesMap: frame_rate, video_length[seconds],
	 * sets the values of minX, minY, maxX, maxY
	 * @return  null if everything went OK; otherwise the error message
	 */
	public String readAbsolutePoints() {
		File file = new File( directory, Utilities.ABS_POINTS_FILENAME );
		if( file.exists() == false ) {
			return Utilities.FILE_NOT_FOUND;
		}; // if
		int c = 0;
		List<String> linesList = new ArrayList<String>();
		try {
			String line = null;
			BufferedReader bufferedReader = new BufferedReader( new FileReader( file ) );
			if( bufferedReader.ready() == false ) {
				return "Error, could not read file ( " + file.getAbsolutePath() + " ).";
			}; // if
			while( ( line = bufferedReader.readLine() ) != null ) {
				c++;
				if( c == 1 ) {
					if( line.startsWith( ">frame rate:" ) == true ) {
						frameRate = new Double( line.substring( ">frame rate:".length() ) );
						valuesMap.put( "frame_rate[fps]", format( frameRate ) );
					}; // if
				}
				else {
					linesList.add( line );
				}; // if
			}; // while
		} 
		catch( FileNotFoundException fnfe ) {
			return Utilities.FILE_NOT_FOUND;
		}
		catch( IOException ioe ) {
			return "Error around line: " + c + " ... " + ioe;
		}; // try

		if( linesList.isEmpty() ) {
			return "Data was empty, nothing to do!";
		}; // if
		if( frameRate == null ) {
			return "Frame rate unknown, can not continue!";
		}; // if
		if( frameList.size() > 0 ) {
			// just in case verification
			return "Programming error: frameList must be emptied before using it again!";
		}; // if

		String timeInSeconds = format( linesList.size() / frameRate );
		if( DEBUG == true ) {
			out.println( "\t video_length[seconds] \t" + timeInSeconds );
		}; // if
		valuesMap.put( "video_length[seconds]", timeInSeconds );

		// load and make sure all columns have data
		int frameNumber = 0;
		int k = 0;
		int point = 0;
		double x = 0;
		double y = 0;

		for( String line : linesList ) {
			String[] columns = line.split( "\t", -1 );
			if( columns.length != ( Frame.MIDLINE * 2 ) ) {
				return "Wrong number of columns (" + columns.length + ")";
			}; // if
			Frame frame = new Frame();
			k = 0;     // increments twice in the while loop
			point = 0; // increments once in the while loop
			while( k < columns.length ) {
				try {
					x = Double.parseDouble( columns[ k ] );
				}
				catch( NumberFormatException nfe ) {
					return "Error in data point (" + columns[ k ] + ") in line: " + ( frameNumber + 2 );
				}; // try
				frame.x[ point ] = x;
				if( minX == null ) {
					minX = x;
				}
				else {
					minX = Math.min( x, minX );
				}; // if
				if( maxX == null ) {
					maxX = x;
				}
				else {
					maxX = Math.max( x, maxX );
				}; // if
				k++;
				try {
					y = Double.parseDouble( columns[ k ] );
				}
				catch( NumberFormatException nfe ) {
					return "Error in data point (" + columns[ k ] + ") in line: " + ( frameNumber + 2 );
				}; // try
				frame.y[ point ] = y;
				if( minY == null ) {
					minY = y;
				}
				else {
					minY = Math.min( y, minY );
				}; // if
				if( maxY == null ) {
					maxY = y;
				}
				else {
					maxY = Math.max( y, maxY );
				}; // if
				
		 		k++;
				point++;
			}; // while

			frameList.add( frame );
			frameNumber++;
		}; // for

		// verify that number of frames is correct
		if( frameNumber != frameList.size() ) {
			return "Mismatch in number of frames in list(" + frameList.size() + ") and processed (" + frameNumber + ").";
		}; // if
		if( linesList.size() != frameList.size() ) {
			return "Mismatch in number of frames in list and lines list.";
		}; // if

		return null;
	}
	

	/**
	 * Calculates the length parameter
	 * @return  null when things are OK; otherwise it returns an error message
	 */
	public String calculateBodyLength( ) {
		if( frameList.size() < 3 ) {
			return "Unable to calculate length statistics, n = " + frameList.size();
		}; // if
		List<Double> bodyLengthList = new ArrayList<Double>();
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			frame.calculateBodyLength();
			bodyLengthList.add( frame.bodyLength );
			if( bodyLengthMin == null ) {
				bodyLengthMin = frame.bodyLength;
			}
			else {
				bodyLengthMin = Math.min( bodyLengthMin, frame.bodyLength );
			}; // if
			if( bodyLengthMax == null ) {
				bodyLengthMax = frame.bodyLength;
			}
			else {
				bodyLengthMax = Math.max( bodyLengthMax, frame.bodyLength );
			}; // if
		}; // for
		computeStatisticsAndAddThemToValuesMap( bodyLengthList, "body_length" + "[mm]" );

		// output length values to a text file
		File file = new File( directory, "tmp_length.txt" );
		try {
			PrintWriter printWriter = new PrintWriter( new FileWriter( file ) );
			for( int f = 0; f < frameList.size(); f++ ) {
				Frame frame = frameList.get( f );
				printWriter.println( frame.bodyLength );
			}; // for
			printWriter.close();
		}
		catch( IOException ioe ) {
			// do nothing important
			ioe.printStackTrace();
		}; // try
		return null;
	}


	/** 
	 * Determines whether any of the frames is outside (i.e., over the repellent)
	 */
	public void verifyPositionOnTheRepellent() {
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			frame.verifyPositionOnTheRepellent();
		}; // for
		// figure out the total time inside, and the total time outside (on the repellent)
		int insideCount = 0;
		int repellentCount = 0;
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			if( frame.onTheRepellent == true ) {
				repellentCount++;
			}
			else {
				insideCount++;
			}; // if
		}; // for
		totalFramesInside = insideCount;
		totalFramesOverRepellent = repellentCount;
		if( totalFramesInside == 0 ) {
			out.println( "totalFramesInside is zero!  " + directory );
			System.exit( 1 );
		}
	}
	

	/**
	 * Smoothes the length, which is used in computing local maxima, minima
	 */
	public void smoothingLength() {
		Frame frameMinus1 = null;
		Frame frameMinus2 = null;
		Frame framePlus1 = null;
		Frame framePlus2 = null;
		Frame frame = null;
		for( int f = 0; f < frameList.size(); f++ ) {
			frameMinus2 = ( f - 2 ) < 0 ? null : frameList.get( f - 2 );
			frameMinus1 = ( f - 1 ) < 0 ? null : frameList.get( f - 1 );
			frame = frameList.get( f );
			framePlus1 = ( ( f + 1 ) < frameList.size() ) ? frameList.get( f + 1 ) : null;
			framePlus2 = ( ( f + 2 ) < frameList.size() ) ? frameList.get( f + 2 ) : null;
			double sum = frame.bodyLength * 6;
			int divide = 6;
			if( frameMinus2 != null ) {
				sum += frameMinus2.bodyLength * 1;
				divide += 1;
			}; // if
			if( frameMinus1 != null ) {
				sum += frameMinus1.bodyLength * 3;
				divide += 3;
			}; // if
			if( framePlus1 != null ) {
				sum += framePlus1.bodyLength * 3;
				divide += 3;
			}; // if
			if( framePlus2 != null ) {
				sum += framePlus2.bodyLength * 1;
				divide += 1;
			}; // if
			frame.smoothBodyLength = sum / divide;
		}; // for
	}
	

	/**
	 * Figures out, for each frame, whether it is a local maxima, or local minima, or neither
	 * (looks at (frame_rate)/2 frames before and frames after)
	 */
	public void localMinimaMaxima() {
		int maxIndex = 0;
		int minIndex = 0;
		int prevMaximaIndex = -1;
		int prevMinimaIndex = -1;
		int PADDING = (int) Math.round( Math.floor( frameRate / 2.0 ) );
		for( int f = 0; f < frameList.size(); f++ ) {
			if( ( f - PADDING ) < 0 || ( f + PADDING ) >= frameList.size() ) {
				continue;
			}; // if
			maxIndex = f;
			minIndex = f;
			// find the minimum and maximum within a window around current frame
			for( int m = ( f - PADDING ); m <= ( f + PADDING ); m++ ) {
				Frame frameMax = frameList.get( maxIndex );
				Frame frameMin = frameList.get( minIndex );
				Frame frameM = frameList.get( m );
				if( frameMax.smoothBodyLength <= frameM.smoothBodyLength ) {
					maxIndex = m;
				}; // if
				if( frameMin.smoothBodyLength >= frameM.smoothBodyLength ) {
					minIndex = m;
				}; // if
			}; // for
			// is the current frame the max of the window we looked at?
			if( maxIndex == f ) {
				Frame frameMax = frameList.get( maxIndex );
				if( prevMaximaIndex != -1 && prevMinimaIndex < prevMaximaIndex ) {
					// either maxIndex stays or prevMaximaIndex stays but not both
					Frame framePrevMaxima = frameList.get( prevMaximaIndex );
					if( frameMax.smoothBodyLength >= framePrevMaxima.smoothBodyLength ) {
						// when current is bigger than a previous maxima, forget the previous local-maxima
						framePrevMaxima.localMaxima = false;
					}
					else {
						// when current is smaller than a previous maxima, the previous maxima is local-maxima
						maxIndex = prevMaximaIndex;
					}; // if
				}; // if
				// marks frame maxIndex as the last maxima we've seen so far
				frameMax = frameList.get( maxIndex );
				frameMax.localMaxima = true;
				prevMaximaIndex = maxIndex;
			}; // if
			if( minIndex == f ) {
				Frame frameMin = frameList.get( minIndex );
				if( prevMinimaIndex != -1 && prevMaximaIndex < prevMinimaIndex ) {
					Frame framePreviMinima = frameList.get( prevMinimaIndex );
					// either min stays or prevMinimaIndex stays but not both
					if( frameMin.smoothBodyLength <= framePreviMinima.smoothBodyLength ) {
						framePreviMinima.localMinima = false;
					}
					else {
						minIndex = prevMinimaIndex;
					}; // if
				}; // if
				frameMin = frameList.get( minIndex );
				frameMin.localMinima = true;
				prevMinimaIndex = minIndex;
			}; // if
			// is this the last frame?
			if( f == ( frameList.size() - PADDING - 1 ) ) {
				// force a localMinima if needed 
				Frame frameF = frameList.get( f );
				if( prevMaximaIndex == -1 ) {
					out.println( "No previous maxima (frame: " + f + "), localMinimaMaxima." );
				}
				else {
					Frame framePrevMaxima = frameList.get( prevMaximaIndex );
					if( prevMinimaIndex < prevMaximaIndex && frameF.bodyLength < framePrevMaxima.bodyLength ) {
						frameF.localMinima = true;
					}; // if
				}; // if
			}; // if
		}; // for
	}


	public void localMinimaMaximaVerifications() {
		Frame prevMinimaFrame = null;
		List<Integer> indexList = new ArrayList<Integer>();
		Double threshold = new Double( valuesMap.get( "body_length[mm]_mean" ) );
		threshold = threshold / 60.0;

		// remember the frames that are local minima or maxima
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			if( frame.localMinima == true || frame.localMaxima == true ) {
				indexList.add( f );
			}; // if
		}; // for

		// detect uphills that have a meaningless minima (too small)
		for( int i = 0; i < indexList.size(); i++ ) {
			Integer f = indexList.get( i );
			Frame frame = frameList.get( f );
			if( frame.localMinima == true && ( i + 4 ) < indexList.size() ) {
				int firstMaximaIndex = indexList.get( i + 1 );
				Frame firstMaximaFrame = frameList.get( firstMaximaIndex );
				// verification just in case
				if( firstMaximaFrame.localMaxima == false ) {
					out.println( "Programming error-1!" );
					return;
				}; // if
				int middleMinimaIndex = indexList.get( i + 2 );
				Frame middleMinimaFrame = frameList.get( middleMinimaIndex );
				// verification just in case
				if( middleMinimaFrame.localMinima == false ) {
					out.println( "Programming error-2!" );
					return;
				}; // if
				// figure out the actual max value of first maxima
				double firstMaximaValue = frame.bodyLength;
				for( int q = f; q < middleMinimaIndex; q++ ) {
					Frame tmpFrame = frameList.get( q );
					if( tmpFrame.bodyLength > firstMaximaValue ) {
						firstMaximaValue = tmpFrame.bodyLength;
					}; // if
				}; // for
				int secondMaximaIndex = indexList.get( i + 3 );
				Frame secondMaximaFrame = frameList.get( secondMaximaIndex );
				// verification just in case
				if( secondMaximaFrame.localMaxima == false ) {
					out.println( "Programming error-3!" );
					return;
				}; // if
				// figure out the acutal min value of middle minima
				double secondMinimaValue = firstMaximaFrame.bodyLength;
				for( int q = firstMaximaIndex; q <= secondMaximaIndex; q++ ) {
					Frame tmpFrame = frameList.get( q );
					if( tmpFrame.bodyLength < secondMinimaValue ) {
						secondMinimaValue = tmpFrame.bodyLength;
					}; // if
				}; // for
				if( ( firstMaximaValue - secondMinimaValue ) < threshold ) {
					if( frame.bodyLength < middleMinimaFrame.bodyLength ) {
						middleMinimaFrame.localMinima = false;
					}
					else {
						frame.localMinima = false;
					}; // if
					firstMaximaFrame.localMaxima = false;
				}
			}
		}; // for
	}

	
	/** 
	 * Get angle between 2 lines
	 * (from: http://stackoverflow.com/questions/3365171/calculating-the-angle-between-two-lines-without-having-to-calculate-the-slope )
	 */
	public static double angleBetween2Lines(Line2D line1, Line2D line2) {
		double angle1 = Math.atan2(line1.getY1() - line1.getY2(), line1.getX1() - line1.getX2());
		double angle2 = Math.atan2(line2.getY1() - line2.getY2(), line2.getX1() - line2.getX2());
		return angle1-angle2;
	}
	

	/**
	 * Calculates bending angle of body and head
	 */
	public void computeBending() {
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			// Use the first two points to remember the line of the head
			Line2D.Double lineOfHead = new Line2D.Double( frame.x[ 0 ], frame.y[ 0 ], frame.x[ 1 ], frame.y[ 1 ] );

			// upper-line, from P{1,2,3} to P{4,5,6}
			double p_123_x = ( frame.x[ 1 ] + frame.x[ 2 ] + frame.x[ 3 ] ) / 3.0;
			double p_123_y = ( frame.y[ 1 ] + frame.y[ 2 ] + frame.y[ 3 ] ) / 3.0;
			double p_345_x = ( frame.x[ 3 ] + frame.x[ 4 ] + frame.x[ 5 ] ) / 3.0;
			double p_345_y = ( frame.y[ 3 ] + frame.y[ 4 ] + frame.y[ 5 ] ) / 3.0;
			
			Line2D.Double lineOfMidbody = new Line2D.Double( p_123_x, p_123_y, p_345_x, p_345_y );

			double degrees = Math.toDegrees( angleBetween2Lines( lineOfHead, lineOfMidbody ) );
			if( degrees < -180 ) {
				degrees = degrees + 360;
			}; // if
			if( degrees > 180 ) {
				degrees = 360 - degrees;
			}; // if
			frame.bendingHead = Math.abs( degrees );

			// now calculate angle for bending-body
			double p_3445_x = ( frame.x[ 3 ] + frame.x[ 4 ] * 2 + frame.x[ 5 ] ) / 4.0;
			double p_3445_y = ( frame.y[ 3 ] + frame.y[ 4 ] * 2 + frame.y[ 5 ] ) / 4.0;
			double p_789_x = ( frame.x[ 7 ] + frame.x[ 8 ] * 2 + frame.x[ 9 ] ) / 4.0;
			double p_789_y = ( frame.y[ 7 ] + frame.y[ 8 ] * 2 + frame.y[ 9 ] ) / 4.0;
			double p_012_x = ( frame.x[ 0 ] + frame.x[ 1 ] * 2 + frame.x[ 2 ] ) / 4.0;
			double p_012_y = ( frame.y[ 0 ] + frame.y[ 1 ] * 2 + frame.y[ 2 ] ) / 4.0;
			Line2D.Double lineNearHead = new Line2D.Double( p_012_x, p_012_y, p_3445_x, p_3445_y );
			double p_101112_x = ( frame.x[ 10 ] + frame.x[ 11 ] * 2 + frame.x[ 12 ] ) / 4.0;
			double p_101112_y = ( frame.y[ 10 ] + frame.y[ 11 ] * 2 + frame.y[ 12 ] ) / 4.0;
			Line2D.Double lineNearTail = new Line2D.Double( p_789_x, p_789_y, p_101112_x, p_101112_y );

			degrees = Math.toDegrees( angleBetween2Lines( lineNearHead, lineNearTail ) );
			if( degrees < -180 ) {
				degrees = degrees + 360;
			}; // if
			if( degrees > 180 ) {
				degrees = 360 - degrees;
			}; // if
			frame.bendingBody = Math.abs( degrees );
		}; // for
	}
	

	/**
	 * Calculates time bending percentages
	 */
	public void computeBendingPercentage( ) {
		computeBendingPercentageHead( );
		computeBendingPercentageBody( );
		computeBendingPercentageHeadOrBody( );
	}


	/**
	 * Calculates time bending percentages
	 */
	private void computeBendingPercentageHead( ) {
		int insideCount = 0;
		int repellentCount = 0;
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			if( frame.bendingHead >= BENDING_ANGLE_THRESHOLD ) {
				// it is bending, but, is it inside or outside (over repellent)?
				if( frame.onTheRepellent == true ) {
					repellentCount++;
				}
				else {
					insideCount++;
				}; // if
			}; // if
		}; // for
		double timeBendingOverall = ( insideCount + repellentCount ) * 1.0 /  frameList.size();
		double timeBendingInside = insideCount * 1.0 /  totalFramesInside;
		double timeBendingOutside = totalFramesOverRepellent > 0 ? ( repellentCount * 1.0 /  totalFramesOverRepellent ) : 0;
		valuesMap.put( "time_head_bending_overall" + "[%]", timeBendingOverall + "" );
		valuesMap.put( "time_head_bending_inside" + "[%]", timeBendingInside + "" );
		valuesMap.put( "time_head_bending_outside" + "[%]", timeBendingOutside + "" );
		// verification just in case
		if( timeBendingOverall < 0 || timeBendingOverall > 1 ) {
			out.println( "ERRRROR : timeBendingOverall " + timeBendingOverall );
			System.exit( 1 );
		}
		if( timeBendingInside < 0 || timeBendingInside > 1 ) {
			out.println( "ERRRROR : timeBendingInside " + timeBendingInside );
			System.exit( 1 );
		}
		if( timeBendingOutside < 0 || timeBendingOutside > 1 ) {
			out.println( "ERRRROR : timeBendingOutside " + timeBendingOutside );
			System.exit( 1 );
		}
	}


	/**
	 * Calculates time bending percentages
	 */
	private void computeBendingPercentageBody( ) {
		int insideCount = 0;
		int repellentCount = 0;
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			if( frame.bendingBody >= BENDING_ANGLE_THRESHOLD ) {
				// it is bending, but, is it inside or outside (over repellent)?
				if( frame.onTheRepellent == true ) {
					repellentCount++;
				}
				else {
					insideCount++;
				}; // if
			}; // if
		}; // for
		double timeBendingOverall = ( insideCount + repellentCount ) * 1.0 /  frameList.size();
		double timeBendingInside = insideCount * 1.0 /  totalFramesInside;
		double timeBendingOutside = totalFramesOverRepellent > 0 ? ( repellentCount * 1.0 /  totalFramesOverRepellent ) : 0;
		valuesMap.put( "time_body_bending_overall" + "[%]", timeBendingOverall + "" );
		valuesMap.put( "time_body_bending_inside" + "[%]", timeBendingInside + "" );
		valuesMap.put( "time_body_bending_outside" + "[%]", timeBendingOutside + "" );
		//verification just in case
		if( timeBendingOverall < 0 || timeBendingOverall > 1 ) {
			out.println( "ERRRROR : body timeBendingOverall " + timeBendingOverall );
			System.exit( 1 );
		}
		if( timeBendingInside < 0 || timeBendingInside > 1 ) {
			out.println( "ERRRROR : body timeBendingInside " + timeBendingInside );
			System.exit( 1 );
		}
		if( timeBendingOutside < 0 || timeBendingOutside > 1 ) {
			out.println( "ERRRROR : body timeBendingOutside " + timeBendingOutside );
			System.exit( 1 );
		}
	}


	/**
	 * Calculates time bending percentages
	 */
	private void computeBendingPercentageHeadOrBody( ) {
		int insideCount = 0;
		int repellentCount = 0;
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			if( frame.bendingHead >= BENDING_ANGLE_THRESHOLD
			||  frame.bendingBody >= BENDING_ANGLE_THRESHOLD ) {
				// it is bending, but, is it inside or outside (over repellent)?
				if( frame.onTheRepellent == true ) {
					repellentCount++;
				}
				else {
					insideCount++;
				}; // if
			}; // if
		}; // for
		double timeBendingOverall = ( insideCount + repellentCount ) * 1.0 /  frameList.size();
		double timeBendingInside = insideCount * 1.0 /  totalFramesInside;
		double timeBendingOutside = totalFramesOverRepellent > 0 ? ( repellentCount * 1.0 /  totalFramesOverRepellent ) : 0;
		valuesMap.put( "time_bending_overall" + "[%]", timeBendingOverall + "" );
		valuesMap.put( "time_bending_inside" + "[%]", timeBendingInside + "" );
		valuesMap.put( "time_bending_outside" + "[%]", timeBendingOutside + "" );
		//verification just in case
		if( timeBendingOverall < 0 || timeBendingOverall > 1 ) {
			out.println( "ERRRROR : both timeBendingOverall " + timeBendingOverall );
			System.exit( 1 );
		}
		if( timeBendingInside < 0 || timeBendingInside > 1 ) {
			out.println( "ERRRROR : both timeBendingInside " + timeBendingInside );
			System.exit( 1 );
		}
		if( timeBendingOutside < 0 || timeBendingOutside > 1 ) {
			out.println( "ERRRROR : both timeBendingOutside " + timeBendingOutside );
			System.exit( 1 );
		}
	}

	
	/**
	 * Calculates body-length when larvae is extended (and when it is contracted)
	 */
	public void lengthExtendedContracted( ) {
		List<Double> minimaList = new ArrayList<Double>();
		List<Double> maximaList = new ArrayList<Double>();
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			if( frame.localMinima == true ) {
				minimaList.add( frame.bodyLength );
			}; // if
			if( frame.localMaxima == true ) {
				maximaList.add( frame.bodyLength );
			}; // if
		}; // for
		computeStatisticsAndAddThemToValuesMap( minimaList, "body_length_contracted" + "[mm]" );
		computeStatisticsAndAddThemToValuesMap( maximaList, "body_length_extended" + "[mm]" );
	}
	

	/** 
	 * Calculates speed
	 */
	public void calculateSpeed( ) {
		final int PADDING = 2;
		int POINT = (int) Math.round( Math.floor( Frame.MIDLINE / 2.0 ) );
		// calculate speed of the center-point
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			if( ( f - PADDING ) < 0 || ( f + PADDING ) >= frameList.size() ) {
				frame.speed = -999;
				continue;
			}; // if
			// frame-padding and frame+padding are used to calculate speed
			Frame frameMinusPadding = frameList.get( f - PADDING );
			Frame framePlusPadding = frameList.get( f + PADDING );
			frame.speed = Utilities.distance( frameMinusPadding.x[ POINT ], frameMinusPadding.y[ POINT ], 
				framePlusPadding.x[ POINT ], framePlusPadding.y[ POINT ] ) / ( PADDING * 2.0 / frameRate );
		}; // for

		// calculate speed of each point of the midline
		for( int eachPoint = 0; eachPoint < Frame.MIDLINE; eachPoint++ ) {
			for( int f = 0; f < frameList.size(); f++ ) {
				Frame frame = frameList.get( f );
				if( ( f - PADDING ) < 0 || ( f + PADDING ) >= frameList.size() ) {
					frame.speedAt[ eachPoint ] = -999;
					continue;
				}; // if
				// frame-padding and frame+padding are used to calculate speed
				Frame frameMinusPadding = frameList.get( f - PADDING );
				Frame framePlusPadding = frameList.get( f + PADDING );
				frame.speedAt[ eachPoint ] = Utilities.distance( frameMinusPadding.x[ eachPoint ], frameMinusPadding.y[ eachPoint ], 
					framePlusPadding.x[ eachPoint ], framePlusPadding.y[ eachPoint ] ) / ( PADDING * 2.0 / frameRate );
			}; // for
		}; // for

		calculateSpeedStatistics( );
		calculateSpeedStatisticsAtEachPoint( );
	}


	/** 
	 * Calculates speed statistics
	 */
	public void calculateSpeedStatistics( ) {
		List<Double> overallList = new ArrayList<Double>();
		List<Double> insideList = new ArrayList<Double>();
		List<Double> outsideList = new ArrayList<Double>();
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			if( frame.speed == -999 ) {
				//out.println( f + "]] " + frame.speed );
				continue;
			}; // if
			overallList.add( frame.speed );
			if( frame.onTheRepellent == false ) {
				insideList.add( frame.speed );
			}
			else {
				outsideList.add( frame.speed );
			}; // if
			if( speedMax == null ) {
				speedMax = frame.speed;
			}
			else {
				speedMax = Math.max( speedMax, frame.speed );
			}; // if
			if( speedMin == null ) {
				speedMin = frame.speed;
			}
			else {
				speedMin = Math.min( speedMin, frame.speed );
			}; // if
		}; // for
	}


	/** 
	 * Calculates speed statistics at each of the 13 points
	 */
	public void calculateSpeedStatisticsAtEachPoint( ) {
		// calculate 'overall'
		for( int eachPoint = 0; eachPoint < Frame.MIDLINE; eachPoint++ ) {
			List<Double> overallList = new ArrayList<Double>();
			for( int f = 0; f < frameList.size(); f++ ) {
				Frame frame = frameList.get( f );
				if( frame.speedAt[ eachPoint ] == -999 ) {
					continue;
				}; // if
				overallList.add( frame.speedAt[ eachPoint ] );
			}; // for
			computeStatisticsAndAddThemToValuesMap( overallList, "speed_overall_at_point_" + eachPoint + "" + "[mm/second]" );
		}; // for
		// calculate 'inside' 
		for( int eachPoint = 0; eachPoint < Frame.MIDLINE; eachPoint++ ) {
			List<Double> insideList = new ArrayList<Double>();
			for( int f = 0; f < frameList.size(); f++ ) {
				Frame frame = frameList.get( f );
				if( frame.speedAt[ eachPoint ] == -999 ) {
					//out.println( f + "]] " + frame.speed );
					continue;
				}; // if
				if( frame.onTheRepellent == false ) {
					insideList.add( frame.speedAt[ eachPoint ] );
				}; // if
			}; // for
			computeStatisticsAndAddThemToValuesMap( insideList, "speed_inside_at_point_" + eachPoint + "" + "[mm/second]" );
		}; // for
		// calculate 'outside'
		for( int eachPoint = 0; eachPoint < Frame.MIDLINE; eachPoint++ ) {
			List<Double> outsideList = new ArrayList<Double>();
			for( int f = 0; f < frameList.size(); f++ ) {
				Frame frame = frameList.get( f );
				if( frame.speedAt[ eachPoint ] == -999 ) {
					//out.println( f + "]] " + frame.speed );
					continue;
				}; // if
				if( frame.onTheRepellent == false ) {
				}
				else {
					outsideList.add( frame.speedAt[ eachPoint ] );
				}; // if
			}; // for
			computeStatisticsAndAddThemToValuesMap( outsideList, "speed_outside_at_point_" + eachPoint + "" + "[mm/second]" );
		}; // for
	}


	/** 
	 * Finds strides;
	 * a stride is a step when body length reaches local minimum and ends till the next local minimum;
	 * The distance traveled by the tail during a stride must be over 0.2mm to be considered a stride;
	 * where the tail is one point before the last midline point 
	 * (we do not choose last point because is not that reliable)
	 */
	public void findStrides( ) {
		Frame prevMinimaFrame = null;
		int prevMinimaIndex = -1;
		int strideNumber = 0;
		Double stride_distance_threshold = new Double( valuesMap.get( "body_length[mm]_mean" ) );
		stride_distance_threshold = stride_distance_threshold / 20.0;
		
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			if( frame.localMinima == true ) {
				if( prevMinimaFrame != null ) {
					// how far did the tail move?
					double x = ( frame.x[ Frame.MIDLINE - 1 ] + frame.x[ Frame.MIDLINE - 2 ] ) / 2.0;
					double y = ( frame.y[ Frame.MIDLINE - 1 ] + frame.y[ Frame.MIDLINE - 2 ] ) / 2.0;
					double xPrev = ( prevMinimaFrame.x[ Frame.MIDLINE - 1 ] + prevMinimaFrame.x[ Frame.MIDLINE - 2 ] ) / 2.0;
					double yPrev = ( prevMinimaFrame.y[ Frame.MIDLINE - 1 ] + prevMinimaFrame.y[ Frame.MIDLINE - 2 ] ) / 2.0;
					double tailMovement = Utilities.distance( x, y, xPrev, yPrev );
					boolean bodyBendingFlag = false;
					int headBendingCount = 0;
					if( tailMovement > stride_distance_threshold ) {	
						// see whether it was bending
						for( int q = prevMinimaIndex; q <= f; q++ ) {
							Frame tmpFrame = frameList.get( q );
							if( tmpFrame.bendingBody >= Video.BENDING_ANGLE_THRESHOLD ) {
								bodyBendingFlag = true;
								break;
							}; // if
							if( tmpFrame.bendingHead >= Video.BENDING_ANGLE_THRESHOLD ) {
								headBendingCount++;
							}; // if
						}; // for
						if( bodyBendingFlag == false && headBendingCount < 2 ) {
							// figure out whether any point was on the repellent
							boolean overRepellent = false;
							for( int tmp = prevMinimaIndex; tmp <= f; tmp++ ) {
								Frame tmpFrame = frameList.get( tmp );
								if( tmpFrame.onTheRepellent == true ) {
									overRepellent = true;
									break;
								}; // if
							}; // for
							// how far did the center-point move?
							double centerpointDistance = Utilities.distance( 
							frame.x[ Frame.CENTER_POINT - 1 ], frame.y[ Frame.CENTER_POINT - 1 ], 
							prevMinimaFrame.x[ Frame.CENTER_POINT - 1 ], prevMinimaFrame.y[ Frame.CENTER_POINT - 1 ] );
							strideNumber++;
							double[] distanceAt = new double[ Frame.MIDLINE ];
							for( int eachPoint = 0; eachPoint < distanceAt.length; eachPoint++ ) {
								distanceAt[ eachPoint ] = Utilities.distance( 
								frame.x[ eachPoint ], frame.y[ eachPoint ], 
								prevMinimaFrame.x[ eachPoint ], prevMinimaFrame.y[ eachPoint ] );
							}; // for
							Stride stride = new Stride( prevMinimaIndex, f, distanceAt, strideNumber, overRepellent, centerpointDistance );
							strideList.add( stride );
							frame.setStride( stride );
						}; // if
					}; // if
				}; // if
				prevMinimaFrame = frame;
				prevMinimaIndex = f;
			}; // if
		}; // for
		// verification just in case
		if( strideNumber != strideList.size() ) {
			out.println( "Programming error: stride number mismatch!" );
			return;
		}; // if
		calculateStrideStatistics( );
		calculateStrideStatisticsAtEachPoint_speed( );
	}


	/**
	 * Calculates stride statistics: time_striding[%], speed_striding, stride_duration, stride_distance
	 */
	public void calculateStrideStatistics( ) {
		List<Double> stridingSpeedOverallList = new ArrayList<Double>();
		List<Double> stridingSpeedInsideList = new ArrayList<Double>();
		List<Double> stridingSpeedOutsideList = new ArrayList<Double>();
		List<Double> strideDurationOverallList = new ArrayList<Double>();
		List<Double> strideDurationInsideList = new ArrayList<Double>();
		List<Double> strideDurationOutsideList = new ArrayList<Double>();
		List<Double> strideDistanceOverallList = new ArrayList<Double>();
		List<Double> strideDistanceInsideList = new ArrayList<Double>();
		List<Double> strideDistanceOutsideList = new ArrayList<Double>();
		double timeStridingInside = 0.0;
		double timeStridingOutside = 0.0;
		int strideCount = 0;

		// first reset stride-flag everywhere
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			frame.stridingFlag = false;
		}; // for
		// assign flag true for frames whith stride
		for( Stride stride : strideList ) {
			for( int f = stride.indexFirstMinima; f <= stride.indexSecondMinima; f++ ) {
				Frame frame = frameList.get( f );
				frame.stridingFlag = true;
			}; // for
		}; // for

		for( Stride stride : strideList ) {
			int frames = stride.indexSecondMinima - stride.indexFirstMinima ;
			double timeSeconds = frames / frameRate;
			double speed = stride.distanceTraveledCenterPoint / ( frames / frameRate );
			stridingSpeedOverallList.add( speed );
			strideDurationOverallList.add( timeSeconds );
			strideDistanceOverallList.add( stride.distanceTraveledCenterPoint );
			if( stride.overRepellent == false ) {
				timeStridingInside += timeSeconds;
				stridingSpeedInsideList.add( speed );
				strideDurationInsideList.add( timeSeconds );
				strideDistanceInsideList.add( stride.distanceTraveledCenterPoint );
			}
			else {
				timeStridingOutside += timeSeconds;
				stridingSpeedOutsideList.add( speed );
				strideDurationOutsideList.add( timeSeconds );
				strideDistanceOutsideList.add( stride.distanceTraveledCenterPoint );
			}; // if
			strideCount++;
		}; // for
		double timeSeconds = frameList.size() / frameRate;
		double timeSecondsInside = totalFramesInside / frameRate;
		double timeSecondsOutside = totalFramesOverRepellent / frameRate;
		double stridingPercentageOverall = ( timeStridingOutside + timeStridingInside ) / timeSeconds;

		// calculate striding-time-percentage based on frame information and stride-flag
		int frameStridingCountInside = 0;
		int frameStridingCountOutside = 0;
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			if( frame.stridingFlag == true ) {
				if( frame.onTheRepellent == true ) {
					frameStridingCountOutside++;
				}
				else {
					frameStridingCountInside++;
				}; // if
			}; // if
		}; // for

		double stridingPercentageInsideViaFramesCount = frameStridingCountInside * 1.0 / totalFramesInside;
		double stridingPercentageOutsideViaFramesCount = totalFramesOverRepellent == 0 ? 0 : ( frameStridingCountOutside * 1.0 / totalFramesOverRepellent );
		valuesMap.put( "time_striding_overall" + "[%]", stridingPercentageOverall + "" );
		valuesMap.put( "time_striding_inside" + "[%]", stridingPercentageInsideViaFramesCount + "" );
		valuesMap.put( "time_striding_outside" + "[%]", stridingPercentageOutsideViaFramesCount + "" );
		valuesMap.put( "strides_per_minute", format( strideCount / ( timeSeconds / 60.0 ) ) );

		computeStatisticsAndAddThemToValuesMap( strideDurationOverallList, "stride_duration_overall" + "[second]" );
		computeStatisticsAndAddThemToValuesMap( strideDurationInsideList, "stride_duration_inside" + "[second]" );
		computeStatisticsAndAddThemToValuesMap( strideDurationOutsideList, "stride_duration_outside" + "[second]" );

		computeStatisticsAndAddThemToValuesMap( strideDistanceOverallList, "stride_distance_overall" + "[mm]" );
		computeStatisticsAndAddThemToValuesMap( strideDistanceInsideList, "stride_distance_inside" + "[mm]" );
		computeStatisticsAndAddThemToValuesMap( strideDistanceOutsideList, "stride_distance_outside" + "[mm]" );
		// verification just in case
		if( stridingPercentageInsideViaFramesCount < 0 || stridingPercentageInsideViaFramesCount > 1 ) {
			out.println( "Erroneous stridingPercentageInsideViaFramesCount: " + stridingPercentageInsideViaFramesCount );
		}; // if
		if( stridingPercentageOutsideViaFramesCount < 0 || stridingPercentageOutsideViaFramesCount > 1 ) {
			out.println( "Erroneous stridingPercentageOutsideViaFramesCount: " + stridingPercentageOutsideViaFramesCount );
		}; // if
	}


	/**
	 * Calculates stride statistics at each point: speed_striding
	 */
	public void calculateStrideStatisticsAtEachPoint_speed( ) {
		// calculate it for 'overall'
		for( int eachPoint = 0; eachPoint < Frame.MIDLINE; eachPoint++ ) {
			List<Double> stridingSpeedOverallList = new ArrayList<Double>();
			for( Stride stride : strideList ) {
				int frames = stride.indexSecondMinima - stride.indexFirstMinima ;
				double speed = stride.distanceTraveledAt[ eachPoint ] / ( frames / frameRate );
				stridingSpeedOverallList.add( speed );
			}; // for
			computeStatisticsAndAddThemToValuesMap( stridingSpeedOverallList, "speed_striding_overall_at_point_" + eachPoint + "" + "[mm/second]" );
		}; // for

		// calculate it for 'inside'
		for( int eachPoint = 0; eachPoint < Frame.MIDLINE; eachPoint++ ) {
			List<Double> stridingSpeedInsideList = new ArrayList<Double>();
			for( Stride stride : strideList ) {
				int frames = stride.indexSecondMinima - stride.indexFirstMinima ;
				double speed = stride.distanceTraveledAt[ eachPoint ] / ( frames / frameRate );
				if( stride.overRepellent == false ) {
					stridingSpeedInsideList.add( speed );
				}; // if
			}; // for
			computeStatisticsAndAddThemToValuesMap( stridingSpeedInsideList, "speed_striding_inside_at_point_" + eachPoint + "" + "[mm/second]" );
		}; // for

		// calculate it for 'outside'
		for( int eachPoint = 0; eachPoint < Frame.MIDLINE; eachPoint++ ) {
			List<Double> stridingSpeedOutsideList = new ArrayList<Double>();
			for( Stride stride : strideList ) {
				int frames = stride.indexSecondMinima - stride.indexFirstMinima ;
				double speed = stride.distanceTraveledAt[ eachPoint ] / ( frames / frameRate );
				if( stride.overRepellent == false ) {
				}
				else {
					stridingSpeedOutsideList.add( speed );
				}; // if
			}; // for
			computeStatisticsAndAddThemToValuesMap( stridingSpeedOutsideList, "speed_striding_outside_at_point_" + eachPoint + "" + "[mm/second]" );
		}; // for

	}


	/**
	 * Calculates stride statistics at each point: stride_distance,
	 * WZ said it is no longer needed
	 */
	public void calculateStrideStatisticsAtEachPoint_distance( ) {
		// calculate for 'overall'
		for( int eachPoint = 0; eachPoint < Frame.MIDLINE; eachPoint++ ) {
			List<Double> strideDistanceOverallList = new ArrayList<Double>();
			for( Stride stride : strideList ) {
				strideDistanceOverallList.add( stride.distanceTraveledAt[ eachPoint ] );
			}; // for
			computeStatisticsAndAddThemToValuesMap( strideDistanceOverallList, "stride_distance_overall_at_point_" + eachPoint + "" + "[mm]" );
		}; // for
		// calculate for 'inside'
		for( int eachPoint = 0; eachPoint < Frame.MIDLINE; eachPoint++ ) {
			List<Double> strideDistanceInsideList = new ArrayList<Double>();
			for( Stride stride : strideList ) {
				if( stride.overRepellent == false ) {
					strideDistanceInsideList.add( stride.distanceTraveledAt[ eachPoint ] );
				}; // if
			}; // for
			computeStatisticsAndAddThemToValuesMap( strideDistanceInsideList, "stride_distance_inside_at_point_" + eachPoint + "" + "[mm]" );
		}; // for
		// calculate for 'outside'
		for( int eachPoint = 0; eachPoint < Frame.MIDLINE; eachPoint++ ) {
			List<Double> strideDistanceOutsideList = new ArrayList<Double>();
			for( Stride stride : strideList ) {
				if( stride.overRepellent == false ) {
				}
				else {
					strideDistanceOutsideList.add( stride.distanceTraveledAt[ eachPoint ] );
				}; // if
			}; // for
			computeStatisticsAndAddThemToValuesMap( strideDistanceOutsideList, "stride_distance_outside_at_point_" + eachPoint + "" + "[mm]" );
		}; // for
	}


	/** 
	 * Calculates contraction rate (mm/sec) and extension rate, contraction rate is 
	 * the change rate of body-length during the contraction phase of a stride
	 */
	public void calculateContractionRate( ) {
		Set<Stride> deleteSet = new TreeSet<Stride>();
		for( Stride stride : strideList ) {
			int frames = stride.indexSecondMinima - stride.indexFirstMinima ;
			Frame firstMinimaFrame = frameList.get( stride.indexFirstMinima );
			Frame secondMinimaFrame = frameList.get( stride.indexSecondMinima );

			// find the frame when body is largest
			double extendedBodyLength = Math.min( firstMinimaFrame.bodyLength, secondMinimaFrame.bodyLength );
			int extendedIndex = 0;
			for( int f = stride.indexFirstMinima + 1; f < stride.indexSecondMinima; f++ ) {
				Frame frame = frameList.get( f );
				if( frame.bodyLength > extendedBodyLength ) {
					extendedBodyLength = frame.bodyLength;
					extendedIndex = f;
				}; // if
			}; // for
			// verify that a 'largest' value was found
			if( extendedIndex == 0 ) {
				out.println( "BIG ERROR, extendedIndex is zero" );
				return;
			}; // if

			int contractionFrames = stride.indexSecondMinima - extendedIndex;
			int extensionFrames = extendedIndex - stride.indexFirstMinima;
			double extensionSeconds = extensionFrames / frameRate;
			double contractionSeconds = contractionFrames / frameRate;
			stride.extensionRate = ( extendedBodyLength - firstMinimaFrame.bodyLength ) / extensionSeconds;
			stride.contractionRate = ( extendedBodyLength - secondMinimaFrame.bodyLength ) / contractionSeconds;
			// verify that the rate is not negative
			if( stride.extensionRate < 0 ) {
				// it is not a stride then!
				secondMinimaFrame.stride = null;
				deleteSet.add( stride );
			}; // if
			if( stride.contractionRate < 0 ) {
				// it is not a stride then!
				secondMinimaFrame.stride = null;
				deleteSet.add( stride );
			}; // if
		}; // for

		// do we have to delete strides?
		if( deleteSet.isEmpty() == false ) {
			for( Stride each : deleteSet ) {
				strideList.remove( each );
			}; // for
			// verify they were deleted
			for( Stride each : deleteSet ) {
				if( strideList.contains( each ) == true ) {
					out.println( "Unable to delete stride!" );
					System.exit( 1 );
				}; // if
			}; // for
			// re-calculate stride statistics
			calculateStrideStatistics( );
		}; // if
		calculateContractionRateStatistics( );
	}


	/**
	 * Calculates statistics for contraction rate and extension rate
	 */
	public void calculateContractionRateStatistics( ) {
		List<Double> contractionRateOverallList = new ArrayList<Double>();
		List<Double> contractionRateInsideList = new ArrayList<Double>();
		List<Double> contractionRateOutsideList = new ArrayList<Double>();
		List<Double> extensionRateOverallList = new ArrayList<Double>();
		List<Double> extensionRateInsideList = new ArrayList<Double>();
		List<Double> extensionRateOutsideList = new ArrayList<Double>();
		for( Stride stride : strideList ) {
			if( stride.contractionRate < 0 ) {
				out.println( "Skipping stride " + stride.strideNumber + ", contractionRate is < 0, " + format( stride.contractionRate ) );
				continue;
			}; // if
			if( stride.extensionRate < 0 ) {
				out.println( "Skipping stride " + stride.strideNumber + ", extensionRate is < 0, " + format( stride.extensionRate ) );
				continue;
			}; // if
			contractionRateOverallList.add( stride.contractionRate );
			extensionRateOverallList.add( stride.extensionRate );
			if( stride.overRepellent == false ) {
				contractionRateInsideList.add( stride.contractionRate );
				extensionRateInsideList.add( stride.extensionRate );
			}
			else {
				contractionRateOutsideList.add( stride.contractionRate );
				extensionRateOutsideList.add( stride.extensionRate );
			}; // if
		}; // if
		computeStatisticsAndAddThemToValuesMap( contractionRateOverallList, "contraction_rate_overall" + "[mm/second]" );
		computeStatisticsAndAddThemToValuesMap( contractionRateInsideList, "contraction_rate_inside" + "[mm/second]" );
		computeStatisticsAndAddThemToValuesMap( contractionRateOutsideList, "contraction_rate_outside" + "[mm/second]" );
		computeStatisticsAndAddThemToValuesMap( extensionRateOverallList, "extension_rate_overall" + "[mm/second]" );
		computeStatisticsAndAddThemToValuesMap( extensionRateInsideList, "extension_rate_inside" + "[mm/second]" );
		computeStatisticsAndAddThemToValuesMap( extensionRateOutsideList, "extension_rate_outside" + "[mm/second]" );
	}

		
	/**
	 * Computes the distance traveled, it does so by computing distance 
	 * of segments that consist of x frames;
	 * where x = (int) Math.round( Math.floor( frameRate ) ), i.e., 7 in our data of 7.5fps;
	 * it uses the point of the center of the worm 
	 */
	public void computeDistanceTraveled( ) {
		double timeSeconds = frameList.size() / frameRate;
		double timeSecondsInside = totalFramesInside / frameRate;
		double timeSecondsOutside = totalFramesOverRepellent / frameRate;
		double distanceTraveledInside = 0.0;
		double distanceTraveledOutside = 0.0;
		double distanceTraveledOverall = 0.0;
		int xFrames = (int) Math.round( Math.floor( frameRate ) );
		int segmentStart = 0;
		boolean overRepellentFlag = false;
		do {
			int segmentEnd = segmentStart + xFrames;
			if( segmentEnd >= frameList.size() ) {
				segmentEnd = frameList.size() - 1;
			}; // if
			if( segmentStart == segmentEnd ) {
				break;
			}; // if
			if( segmentEnd <= frameList.size() - 1 ) {
				Frame frameSegmentStart = frameList.get( segmentStart );
				Frame frameSegmentEnd = frameList.get( segmentEnd );
				overRepellentFlag = false;
				for( int f = segmentStart; f <= segmentEnd; f++ ) {
					Frame frame = frameList.get( f );
					if( frame.onTheRepellent == true ) {
						overRepellentFlag = true;
						break;
					}; // if
				}; // for
				// how far did the center-point move?
				double centerpointDistance = Utilities.distance( 
				frameSegmentStart.x[ Frame.CENTER_POINT ], frameSegmentStart.y[ Frame.CENTER_POINT ], 
				frameSegmentEnd.x[ Frame.CENTER_POINT ], frameSegmentEnd.y[ Frame.CENTER_POINT ] );
				if( overRepellentFlag == false ) {
					distanceTraveledInside += centerpointDistance;
				}
				else {
					distanceTraveledOutside += centerpointDistance;
				}; // if
			}; // if
	
			// prepare for next iteration
			segmentStart += xFrames;
		} while( segmentStart < frameList.size() );
		
		double distance = ( distanceTraveledInside + distanceTraveledOutside ) / ( timeSeconds / 60.0 );
		double distanceInside = distanceTraveledInside / ( timeSecondsInside / 60.0 );
		double distanceOutside = timeSecondsOutside > 0 ? ( distanceTraveledOutside / ( timeSecondsOutside / 60.0 ) ) : 0;
		valuesMap.put( "distance_traveled_per_minute_overall" + "[mm]", format( distance ) );
		valuesMap.put( "distance_traveled_per_minute_inside" + "[mm]", format( distanceInside ) );
		valuesMap.put( "distance_traveled_per_minute_outside" + "[mm]", format( distanceOutside ) );
	}


	/**
	 * Calculates direction change: for each point X on the track, draw a line to the point 
	 * 2.25mm before, and a line to the point 2.25mm after X;
	 * when the angle between the two lines is over 25 degrees, 
	 * then X is considered a direction change
	 */
	public void calculateDirectionChange( ) {
		// skip the first and last frame
		for( int f = 1; f < frameList.size() - 1; f++ ) {
			Frame frame = frameList.get( f );
			// does it have a line before it that is of distance 2.25mm?
			double centerpointDistance = 0.0;
			int firstPointIndex = f;
			do {
				firstPointIndex--;
				if( firstPointIndex < 0 ) {
					break;
				}; // if
				Frame firstPointFrame = frameList.get( firstPointIndex );
				centerpointDistance = Utilities.distance( 
				frame.x[ Frame.CENTER_POINT ], frame.y[ Frame.CENTER_POINT ], 
				firstPointFrame.x[ Frame.CENTER_POINT ], firstPointFrame.y[ Frame.CENTER_POINT ] );
				//out.println( "looking at frame " + f + " to " + firstPointIndex + "  distance: " + format( centerpointDistance ) );
			} while( centerpointDistance < DISTANCE_THRESHOLD_FOR_DIRECTION_CHANGE );
			if( centerpointDistance < DISTANCE_THRESHOLD_FOR_DIRECTION_CHANGE ) {
				continue;
			}; // if
			// does it have a line before it that is of distance 2.25mm?
			centerpointDistance = 0.0;
			int thirdPointIndex = f;
			do {
				thirdPointIndex++;
				if( thirdPointIndex == frameList.size() ) {
					break;
				}; // if
				Frame thirdPointFrame = frameList.get( thirdPointIndex );
				centerpointDistance = Utilities.distance( 
				frame.x[ Frame.CENTER_POINT ], frame.y[ Frame.CENTER_POINT ], 
				thirdPointFrame.x[ Frame.CENTER_POINT ], thirdPointFrame.y[ Frame.CENTER_POINT ] );
			} while( centerpointDistance < DISTANCE_THRESHOLD_FOR_DIRECTION_CHANGE );
			if( centerpointDistance < DISTANCE_THRESHOLD_FOR_DIRECTION_CHANGE ) {
				continue;
			}; // if
			Frame firstPointFrame = frameList.get( firstPointIndex );
			Frame thirdPointFrame = frameList.get( thirdPointIndex );
			
			Line2D.Double beforeLine = new Line2D.Double( 
			firstPointFrame.x[ Frame.CENTER_POINT ], firstPointFrame.y[ Frame.CENTER_POINT ],
			frame.x[ Frame.CENTER_POINT ], frame.y[ Frame.CENTER_POINT ] );

			Line2D.Double afterLine = new Line2D.Double( 
			frame.x[ Frame.CENTER_POINT ], frame.y[ Frame.CENTER_POINT ],
			thirdPointFrame.x[ Frame.CENTER_POINT ], thirdPointFrame.y[ Frame.CENTER_POINT ] );

			double degrees = Math.toDegrees( angleBetween2Lines( beforeLine, afterLine ) );
			if( degrees < -180 ) {
				degrees = degrees + 360;
			}; // if
			if( degrees > 180 ) {
				degrees = 360 - degrees;
			}; // if
			frame.setDirectionChangeAngle( Math.abs( degrees ) );
		}; // for
		calculateDirectionChangeStatistics( );
	}


	/**
	 * Calculates statistics for parameter direction-change
	 */
	public void calculateDirectionChangeStatistics( ) {
		int count = 0;
		int inside = 0;
		int outside = 0;
		// skip the first and last frame
		for( int f = 1; f < frameList.size() - 1; f++ ) {
			Frame frame = frameList.get( f );
			if( frame.directionChangeAngle == null ) {
				continue;
			}; // if
			count++;
			if( frame.directionChangeAngle < ANGLE_THRESHOLD_FOR_DIRECTION_CHANGE ) {
				continue;
			}; // if
			if( frame.onTheRepellent == false ) {
				inside++;
			}
			else {
				outside++;
			}; // if
		}; // for
		if( count == 0 ) {
			//out.println( "Never did direction change" );
			valuesMap.put( "direction_change_overall" + "[%]", "NULL" );
			valuesMap.put( "direction_change_inside" + "[%]", "NULL" );
			valuesMap.put( "direction_change_outside" + "[%]", "NULL" );
			return;
		}; // if
		double directionChange = ( inside + outside ) * 1.0 / frameList.size();
		double directionChangeInside = inside * 1.0 / totalFramesInside;
		double directionChangeOutside = totalFramesOverRepellent > 0 ? ( outside * 1.0 / totalFramesOverRepellent ) : 0;
		valuesMap.put( "direction_change_overall" + "[%]", format( directionChange ) );
		valuesMap.put( "direction_change_inside" + "[%]", format( directionChangeInside ) );
		valuesMap.put( "direction_change_outside" + "[%]", format( directionChangeOutside ) );
		// verification just in case
		if( directionChange < 0 || directionChange > 1 ) {
			out.println( "ERRRROR : directionChange " + directionChange );
			System.exit( 1 );
		}
		if( directionChangeInside < 0 || directionChangeInside > 1 ) {
			out.println( "ERRRROR : directionChangeInside " + directionChangeInside );
			System.exit( 1 );
		}
		if( directionChangeOutside < 0 || directionChangeOutside > 1 ) {
			out.println( "ERRRROR : directionChangeOutside " + directionChangeOutside );
			System.exit( 1 );
		}
	}


	/**
	 * Finds runs: a stretch of consecutive strides (minimum 3 strides)
	 */
	public void findRuns( ) {
		// find out the average number of frames that strides have
		List<Integer> frameCountList = new ArrayList<Integer>();
		for( Stride stride : strideList ) {
			int frameCount = stride.indexSecondMinima - stride.indexFirstMinima;
			//out.println( stride.indexFirstMinima + " .. " + stride.indexSecondMinima + "  " + frameCount );
			frameCountList.add( frameCount );
		}; // for
		double[] theFrames = new double[ frameCountList.size() ];
		for( int i = 0; i < theFrames.length; i++ ) {
			theFrames[ i ] = frameCountList.get( i );
		}; // for
		if( frameCountList.size() < 2 ) {
			out.println( "WARNING, frameCountList.size is 2 !!!! location: " + directory );
			return;
		}; // if
		int halfMean = (int) Math.round( Math.floor( StatUtils.mean( theFrames ) / 2.0 ) );
		//out.println( "halfMean: " + halfMean );
		int runCount = 0;
		List<Run> tmpRunList = new ArrayList<Run>();
		Run run = null;

		// find strides to add to runs
		for( int f = 0; f < frameList.size(); f++ ) {
			Frame frame = frameList.get( f );
			if( frame.stride != null ) {
				if( run == null ) {
					runCount++;
					run = new Run( runCount, halfMean );
					tmpRunList.add( run );
				}; // if
				boolean addedFlag = run.addStride( frame.stride );
				if( addedFlag == false ) {
					boolean validRun = run.isValid();
					if( validRun == true ) {
						// make a new run 
						runCount++;
						run = new Run( runCount, halfMean );
						tmpRunList.add( run );
					}
					else {
						// reset the existing stride
						run.reset();
					}; // if
					// add the stride to the run
					addedFlag = run.addStride( frame.stride );
					// just in case verification
					if( addedFlag == false ) {
						out.println( f + "]]!not!added!!!!" + frame.stride.strideNumber + " to run.(this should not happen)" );
					}; // if
				}; // if
			}; // if
		}; // for

		// keep runs that are valid
		for( Run each : tmpRunList ) {
			if( each.isValid() == true ) {
				runList.add( each );
			}; // if
		}; // for
		calculateRunStatistics( );
	}


	/**
	 * Calculates run statistics
	 */
	public void calculateRunStatistics( ) {
		// the way in which distance is calculated is exactly same as that of method computeDistanceTraveled
		int xFrames = (int) Math.round( Math.floor( frameRate ) );

		List<Double> distanceList = new ArrayList<Double>();
		List<Double> durationList = new ArrayList<Double>();
		List<Double> strideCountList = new ArrayList<Double>();

		for( Run eachRun : runList ) {
			Stride firstStride = eachRun.strideList.get( 0 );
			Stride lastStride = eachRun.strideList.get( eachRun.strideList.size() - 1 );

			double distanceTraveled = 0.0;
			int segmentStart = firstStride.indexFirstMinima;
			do {
				int segmentEnd = segmentStart + xFrames;
				if( segmentEnd > lastStride.indexSecondMinima ) {
					segmentEnd = lastStride.indexSecondMinima;
				}; // if
				if( segmentStart == segmentEnd ) {
					break;
				}; // if
				if( segmentEnd <= frameList.size() - 1 ) {
					Frame frameSegmentStart = frameList.get( segmentStart );
					Frame frameSegmentEnd = frameList.get( segmentEnd );
					// how far did the center-point move?
					double centerpointDistance = Utilities.distance( 
					frameSegmentStart.x[ Frame.CENTER_POINT ], frameSegmentStart.y[ Frame.CENTER_POINT ], 
					frameSegmentEnd.x[ Frame.CENTER_POINT ], frameSegmentEnd.y[ Frame.CENTER_POINT ] );
					distanceTraveled += centerpointDistance;
				}; // if

				// prepare for next iteration
				segmentStart += xFrames;
			} while( segmentStart < lastStride.indexSecondMinima );
			distanceList.add( distanceTraveled );
			double timeSeconds = ( lastStride.indexSecondMinima - firstStride.indexFirstMinima ) / frameRate;
			durationList.add( timeSeconds );
			strideCountList.add( eachRun.strideList.size() * 1.0 );
		}; // for
		
		computeStatisticsAndAddThemToValuesMap( distanceList, "run_distance" + "[mm]" );
		computeStatisticsAndAddThemToValuesMap( durationList, "run_duration" + "[second]" );
		computeStatisticsAndAddThemToValuesMap( strideCountList, "strides_per_run" );
		double timeSeconds = frameList.size() / frameRate;
		valuesMap.put( "runs_per_minute", format( runList.size() / ( timeSeconds / 60.0 ) ) );
	}


	/**
	 * Formats a double value with a default parameter
	 * @param  value  the value
	 * @return  formatted value, or "NULL" when value is null
	 */
	public static String format( Double value ) {
		if( value == null ) {
			return "NULL";
		}; // if
		if( value.isNaN() == true ) {
			out.println( "Got a Nan, check the code!" );
			throw new RuntimeException( "Got a Nan, check the code!" );
		}; // if
		return formatter.format( value );
	}
	


	/**
	 * Formats a double value with a default parameter (3 digits after decimal point)
	 * @param  value  the value
	 * @return  formatted value, or "NULL" when value is null
	 */
	public static String format3( Double value ) {
		if( value == null ) {
			return "NULL";
		}; // if
		if( value.isNaN() == true ) {
			out.println( "Got a Nan, check the code (format3)!" );
			throw new RuntimeException( "Got a Nan, check the code (format3)!" );
		}; // if
		return formatter3.format( value );
	}
	

	/**
	 * Computes statistics and adds them to the values map (mean, std-dev, n )
	 * @param  valuesList  the list of values
	 * @param  keyPrefix  the prefix to use as key in the values-map
	 */
	public void computeStatisticsAndAddThemToValuesMap( List<Double> valuesList, String keyPrefix ) {
		double[] values = new double[ valuesList.size() ];
		Double min = null;
		Double max = null;
		int index = 0;
		for( Double each : valuesList ) {
			values[ index ] = each;
			min = min == null ? each : min;
			max = max == null ? each : max;
			min = each < min ? each : min;
			max = each > max ? each : max;
			index++;
		}; // for
		Double mean = null;
		if( values.length > 0 ) {
			mean = StatUtils.mean( values );
		}; // if
		Double stdev = null;
		if( mean != null ) {
			stdev = Math.sqrt( StatUtils.variance( values, mean ) );
		}; // if
		valuesMap.put( keyPrefix + "_mean", format( mean ) );
		valuesMap.put( keyPrefix + "_stdev", format( stdev ) );
		//not adding min anymore, valuesMap.put( keyPrefix + "_min", format( min ) );
		//not adding max anymore, valuesMap.put( keyPrefix + "_max", format( max ) );
		int cutoff = keyPrefix.indexOf( "[" );
		if( cutoff != -1 ) {
			keyPrefix = keyPrefix.substring( 0, cutoff );
		}; // if
		valuesMap.put( keyPrefix + "_n", "" + valuesList.size() );
	}


	/**
	 * Computes statistics and adds them to the values map (mean, std-dev, min, max)
	 * @param  values  the values
	 * @param  keyPrefix  the prefix to use as key in the values-map
	 */
	public void computeStatisticsAndAddThemToValuesMap( double[] values, String keyPrefix ) {
		Double min = null;
		Double max = null;
		for( int index = 0; index < values.length; index++ ) {
			double each = values[ index ];
			min = min == null ? each : min;
			max = max == null ? each : max;
			min = each < min ? each : min;
			max = each > max ? each : max;
		}; // for
		Double mean = null;
		if( values.length > 0 ) {
			mean = StatUtils.mean( values );
		}; // if
		Double stdev = null;
		if( mean != null ) {
			stdev = Math.sqrt( StatUtils.variance( values, mean ) );
		}; // if
		valuesMap.put( keyPrefix + "_mean", format( mean ) );
		valuesMap.put( keyPrefix + "_stdev", format( stdev ) );
		int cutoff = keyPrefix.indexOf( "[" );
		if( cutoff != -1 ) {
			keyPrefix = keyPrefix.substring( 0, cutoff );
		}; // if
		valuesMap.put( keyPrefix + "_n", "" + values.length );
	}


	/**
	 * Returns the values header, separated by tabs
	 */
	public String getValuesHeader() {
		String ret = null;
		for( Entry<String,String> entry : valuesMap.entrySet() ) {
			ret = ret == null ? "" : ret + "\t";
			ret += entry.getKey();
		}; // for
		return "#" + ret;
	}

	
	/**
	 * Returns the values, separated by tabs
	 */
	public String getValues() {
		String ret = null;
		for( Entry<String,String> entry : valuesMap.entrySet() ) {
			ret = ret == null ? "" : ret + "\t";
			ret += entry.getValue();
		}; // for
		return ret;
	}


	/**
	 * Resets variables and anything associated with a video
	 */
	public void resetEverything() {
		larvae = null;
		directory = null;
		frameRate = null;
		minX = null;
		minY = null;
		maxX = null;
		maxY = null;
		valuesMap.clear();
		frameList.clear();
		strideList.clear();
		runList.clear();
	}

	/**
	 * Calculate all parameters 
	 * @return  null when things are OK; otherwise it returns an error message
	 */
	public String calculateAllParameters() {
		String error = readAbsolutePoints();
		if( error != null ) {
			return error;
		}; // if


		error = calculateBodyLength( );
		if( error != null ) {
			return error;
		}; // if
		smoothingLength();
		localMinimaMaxima();
		localMinimaMaximaVerifications();
		lengthExtendedContracted( );
		verifyPositionOnTheRepellent();
		computeBending();
		computeBendingPercentage( );
		calculateSpeed( );
		findStrides( );
		calculateContractionRate( );
		computeDistanceTraveled( );
		calculateDirectionChange( );
		findRuns( );
		double timeInside = totalFramesInside * 1.0 / frameList.size();
		valuesMap.put( "time_not_over_repellent" + "[%]", format( timeInside ) );
		// verification just in case
		if( timeInside < 0 || timeInside > 1 ) {
			out.println( "ERRRROR : timeInside " + timeInside );
			System.exit( 1 );
		}
		return null;
	}


	/**
	 */
	public String writeDetailsTextfile() {
		List<String> linesList = Utilities.readFile( directory + FRAMETIME_TXT );
		if( linesList == null || linesList.size() == 0 ) {
			return "Unable to read frame-time text file (" + directory + FRAMETIME_TXT + ")";
		}; // if

		List<String> outputList = new ArrayList<String>();
		String header = "#Timestamp (seconds)\tLength\tStride(zero means not striding)";
		for( int eachPoint = 0; eachPoint < Frame.MIDLINE; eachPoint++ ) {
			header += "\t" + "speed_pt" + ( eachPoint + 1 );
		}; // for
		for( int eachPoint = 0; eachPoint < Frame.MIDLINE; eachPoint++ ) {
			header += "\t" + "x_" + ( eachPoint + 1 ) + "\t" + "y_" + ( eachPoint + 1 );
		}; // for
		outputList.add( header );

		// stuff to know in which stride we are at each frame
		Map<Integer,Stride> frameStrideMap = new TreeMap<Integer,Stride>();
		for( Stride stride : strideList ) {
			for( int f = stride.indexFirstMinima; f <= stride.indexSecondMinima; f++ ) {
				if( frameStrideMap.containsKey( f ) == false ) {
					frameStrideMap.put( f, stride );
				}; // if
			}; // for
		}; // for

		// process each line of time-frame to get together info at each frame
		for( int i = 1; i < linesList.size(); i++ ) {
			String line = linesList.get( i );
			String[] parts = line.split( "\t" );
			String ret = null;
			if( parts.length != 2 ) {
				ret = "Line " + ( i + 1 ) + " in " + FRAMETIME_TXT + " has an unexpected number of columns. Line: " + line;
			}
			else {
				Integer frameNumber = Integer.parseInt( parts[ 0 ] );
				Double time = Double.parseDouble( parts[ 1 ] );
				String timestamp = format3( time );
				Frame frame = frameList.get( frameNumber );
				Stride stride = frameStrideMap.get( frameNumber );
				ret = timestamp + "\t" + format3( frame.bodyLength )
					+ "\t" + ( stride == null ? "0" : stride.strideNumber ) ; 
				for( int eachPoint = 0; eachPoint < Frame.MIDLINE; eachPoint++ ) {
					ret += "\t" + ( -999 == frame.speedAt[ eachPoint ] ? "" : format3( frame.speedAt[ eachPoint ] ) );
				}; // for
				for( int eachPoint = 0; eachPoint < Frame.MIDLINE; eachPoint++ ) {
					ret += "\t" + ( -999 == frame.x[ eachPoint ] ? "" : format3( frame.x[ eachPoint ] ) );
					ret += "\t" + ( -999 == frame.y[ eachPoint ] ? "" : format3( frame.y[ eachPoint ] ) );
				}; // for
			}; // if
			outputList.add( ret );
		}; // for
		Utilities.writeFile( directory + "details.txt", outputList );
		return null;
	}


	/** For testing purposes only */
	public static void main( String[] args ) {
		String directory = "C:\\data\\worm_cs-001";
		out.println( "Testing with directory: " + directory );
		Video video = new Video();
		video.setDirectory( directory );
		String error = video.calculateAllParameters();
		if( error != null ) {
			out.println( "Error: " + error );
			System.exit( 1 );
		}; // if
	}
		

} // class Video

