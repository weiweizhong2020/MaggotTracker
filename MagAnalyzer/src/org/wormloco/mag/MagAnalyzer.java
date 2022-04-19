/*
 * Filename: MagAnalyzer.java
 */

package org.wormloco.mag;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Processing of a directory containing many sub-folders with videos that were already processed by MagRecognizer
 * @author  Boanerges Aleman-Meza
 *
 */

public class MagAnalyzer {

	// for convenience on println statements
	private static final PrintStream out = System.out;

	/** Everything happens here */
	public static void main( String[] args ) {
		if( args.length != 1 ) {
			out.println( "USAGE: specify directory containing many folders." );
			out.println( "       (folder and/or sub-folders are the ones created by MagRecognizer.)\n\n" );
			System.exit( 1 );
		}; // if

		String directory = args[ 0 ];
		if( directory.endsWith( "/" ) == false && directory.endsWith( "\\" ) == false ) {
			directory = directory + File.separator;
		}; // if

		File file = new File( directory );
		if( file.exists() == false ) {
			out.println( "Error, directory does not exist!\n" );
			out.println( "USAGE: specify directory containing many folders." );
			out.println( "       (folder and/or sub-folders are the ones created by MagRecognizer.)\n\n" );
			out.println( "Bye!\n\n" );
			System.exit( 1 );
		}; // if

		List<File> foldersList = new ArrayList<File>();
		String error = findSubfoldersWithData( file, foldersList );
		if( error != null ) {
			out.println( "Errors: " + error );
			out.println( "Bye!\n\n" );
			System.exit( 1 );
		}; // if

		Video video = new Video();
		String headerLine = null;
		PrintWriter printWriter = null;


		// process each folder
		int i = 0;
		for( File each : foldersList ) {
			if( each.isDirectory() == false ) {
				out.println( "ERROR, not a folder! " + each.getName() );
				return;
			}; // if

			i++;
			String parentFolder = each.getParent().substring( each.getParent().lastIndexOf( File.separator ) + 1 );
			String nameOnly = each.getAbsolutePath().substring( each.getAbsolutePath().lastIndexOf( File.separator ) + 1 );
			if( nameOnly.startsWith( "bad" ) == true ) {
				out.println( "[ " + i + " of " + foldersList.size() + " ] ----- " + nameOnly + " --- " + parentFolder + " (bad)" );
				continue;
			}; // if
			video.resetEverything();
			out.println( "[ " + i + " of " + foldersList.size() + " ] ----- " + nameOnly + " --- " + parentFolder );
			
			video.setDirectory( each.getAbsolutePath() );
			error = video.calculateAllParameters();
			if( error != null ) {
				out.println( "\t" + error );
				continue;
			}; // if
			
			if( printWriter == null ) {
				try {
					printWriter = new PrintWriter( new FileWriter( directory + "datadm.txt" ) );
				}
				catch( IOException ioe ) {
					out.println( "Error when creating output file (data.txt)" );
					System.exit( 1 );
				}; // try
			}; // if

			if( headerLine == null ) {
				headerLine = video.getValuesHeader();
				printWriter.println( headerLine );
			}; // if
			printWriter.println( parentFolder + File.separator + video.getValues() );
			printWriter.flush();

			// just in case verification
			if( headerLine.equals( video.getValuesHeader() ) == false ) {
				out.println( "WARNING, header lines are different! , see below" );
				out.println( headerLine );
				out.println( video.getValuesHeader() );
			}; // if
		}; // for
	}


	/**
	 * Resursively finds folders containing data, i.e., points.txt and movie.avi
	 * @param  file  the folder to start with (normally a directory)
	 * @param  foldersList  the list into which to put results
	 * @return  null when things go OK, otherwise it returns an error message
	 */
	public static String findSubfoldersWithData( File file, List<File> foldersList ) {
		if( file == null ) {
			return "file is null, cannot do anything.";
		}; // if
		if( file.isDirectory() == false ) {
			return null;
		}; // if
		// skip 'bad' folders
		if( file.getName().startsWith( "bad" ) == true ) {
			return null;
		}; // if
		File[] directoryContents = file.listFiles();
		// first see whether it contains both movie.avi and points.txt
		boolean movieFlag = false;
		boolean pointsFlag = false;
		boolean absPointsFlag = false;
		String[] wanted = new String[] { "movie.avi", "points.txt", Utilities.ABS_POINTS_FILENAME, Video.FRAMETIME_TXT, "info.xml", "log.csv" };
		boolean[] wantedFlag = new boolean[ wanted.length ];
		int hits = 0;
		for( File each : directoryContents ) {
			if( each.isDirectory() == true ) {
				continue;
			}; // if
			int i = 0;
			for( String filename : wanted ) {
				if( filename.equals( each.getName() ) == true ) {
					wantedFlag[ i ] = true;
					hits++;
				}; // if
				i++;
			}; // for
		}; // for
		if( hits == wanted.length ) {
			foldersList.add( file );
		}
		else if( hits > 0 ) {
			out.println( "Incomplete files in folder: " + file.getAbsolutePath() );
			int i = 0;
			for( String filename : wanted ) {
				if( wantedFlag[ i ] == false ) {
					out.println( "\tmissing: " + filename );
				}; // if
				i++;
			}; // for
		}; // if
		// now do the recursive call
		for( File each : directoryContents ) {
			if( each.isDirectory() == true ) {
				String error = findSubfoldersWithData( each, foldersList );
				if( error != null ) {
					return error;
				}; // if
			}; // if
		}; // for
		return null;
	}
}

