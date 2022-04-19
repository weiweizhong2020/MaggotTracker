/*
 * Filename: Utilities.java
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
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Common utilities methods
 *
 * @author Aleman-Meza
 */

public class Utilities {

	/** constant for file-name of abs_points.txt */
	public static final String ABS_POINTS_FILENAME = "abs_points.txt";
	
	public static final String FILE_NOT_FOUND = "Error, file not found, skipping it!";

	// for convenience on println statements
	private static final PrintStream out = System.out;

	/**
	 * Computes the distance between two points
	 * @param  x1  the x coordinate of the first point
	 * @param  y1  the y coordinate of the first point
	 * @param  x2  the x coordinate of the second point
	 * @param  y2  the y coordinate of the second point
	 * @return  the distance as a double
	 */
	public static double distance( double x1, double y1, double x2, double y2 ) {
		return Math.sqrt( ( x1 - x2 ) * ( x1 - x2 ) + ( y1 - y2 ) * ( y1 - y2 ) );
	}


	// read the points file and store all points in an array
	// points[f][0][0]-points[f][0][12]: the X-coordinates of the 13 points on frame f;
	// points[f][1][0]-points[f][1][12]: the Y-coordinates of the 13 points on frame f;
	// when there is some error, it returns null ( or exits via system.exit() )
	public static int[][][] readPoints( String filename ) {
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
			int[][][] points = new int[ totalImages ][ 2 ][ 13 ];
			
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
      }; // try 	
      return null;		
	}


	/**
	 * Reads a file into a list of Strings
	 * @param  filename  the file name
	 * @return  a list of strings
	 */
	public static List<String> readFile( String filename ) {
		if( filename == null || "".equals( filename.trim() ) ) {
			System.out.println( "Invalid file name (" + filename + ")" );
			return null;
		}; // if

		File file = new File( filename );
		if( file.exists() == false ) {
			System.out.println( "File does not exist (" + filename + ")" );
			return null;
		}; // if

		if( file.isFile() == false ) {
			System.out.println( "File is not a normal file (" + filename + ")" );
			return null;
		}; // if

		List<String> linesList = new ArrayList<String>();
		try {
			BufferedReader bufferedReader = new BufferedReader( new FileReader( file ) );
			String line = null;
			if( bufferedReader.ready() ) {
				while( ( line = bufferedReader.readLine() ) != null ) {
					linesList.add( line );
				}; // while
			}
			else {
				System.out.println( "Buffer is not ready ! (" + filename + ")" );
			}; // if
		}
		catch( FileNotFoundException fnfe ) {
			System.out.println( "File not found exception (" + filename + ")" );
			return null;
		}
		catch( IOException ioe ) {
			System.out.println( "File input/output exception (" + filename + ")" );
			return null;
		}; // try

		return linesList;
	}; // readFile


	/**
	 * Writes a list of lines into a filename
	 * @param  filename  the filename
	 * @param  linesList  the list of lines
	 * @return  null if everything went well, otherwise a string containing error message
	 */
	public static String writeFile( String filename, List<String> linesList ) {
		try {
			FileWriter fileWriter = new FileWriter( filename );
			BufferedWriter bufferedWriter = new BufferedWriter( fileWriter );
			PrintWriter printWriter = new PrintWriter( bufferedWriter );
			for( String line : linesList ) {
				printWriter.println( line );
			}; // for
			printWriter.close();
		}
		catch( IOException ioe ) {
			ioe.printStackTrace();
			return ioe.toString();
		}; // try
		return null;
	}; // writeFile


	/**
	 * Writes a set into a filename
	 * @param  filename  the filename
	 * @param  tmpSet  the set
	 * @return  null if everything went well, otherwise a string containing error message
	 */
	public static String writeFile( String filename, Set<String> tmpSet ) {
		try {
			FileWriter fileWriter = new FileWriter( filename );
			BufferedWriter bufferedWriter = new BufferedWriter( fileWriter );
			PrintWriter printWriter = new PrintWriter( bufferedWriter );
			for( String line : tmpSet ) {
				printWriter.println( line );
			}; // for
			printWriter.close();
		}
		catch( IOException ioe ) {
			ioe.printStackTrace();
			return ioe.toString();
		}; // try
		return null;
	}; // writeFile

} // class Utilities

