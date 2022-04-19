/*
 * MagRecognizer.java	
 *
 */

package org.wormloco.mag;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;

/**
 * Main class to process videos.
 *
 * @author Weiwei Zhong (initial prototype), Aleman-Meza (final version)
 *
 */

public class MagRecognizer{
	
	/** constant for prefix of already-processed folders */
	public static final String PREFIX_FOLDER = "worm_";

	/** constant for movie.avi filename */
	public static final String MOVIE_AVI = "movie.avi";

	// flag for debugging
	private static final boolean DEBUG = "true".equalsIgnoreCase( System.getProperty( "DEBUG" ) );

	// for convenience on println statements
	private static final PrintStream out = System.out;
	
	/**
	 * Runs processing of videos of a given directory
	 * @param  args  the first parameter must be a folder name
	 */
	public static void main( String[] args ) {
		if( DEBUG == true ) {
			out.println( "debug: " + DEBUG + " " + args.length + " arguments." );
		}; // if
		if( args.length == 0 ) {
			if( DEBUG == false ) {
				errorMsg();
				System.exit( 1 );
			} // if
			args = new String[] { "C:\\data\\testdataLarvae001" };
		}; // if

		String folder = args[ 0 ];
		if( folder == null || "".equals( folder.trim() ) == true ) {
			errorMsg();
			System.exit( 1 );
		}; // if
		boolean againFlag = false;
		if( args.length == 2 && "--again".equalsIgnoreCase( args[ 1 ] ) == true ) {
			againFlag = true;
		}; // if
		processFolder( folder, againFlag );
	}
	
	
	/**
	 * Displays error message that indicates usage of the command-line parameters 
	 */
	public static void errorMsg() {
		out.println( "Usage: java MagRecognizer foldername --again" );
		out.println( "\nfoldername:\nthe folder that contains files xx.avi, xx.info.xml, and xx.log.csv." );
		out.println( "\nFor example, I have a folder named cs_11-27-2008.  It contains all the worm videos. The command I would use is: " );		
		out.println( "java MagRecognizer cs_11-27-2008" );
		out.println( "--again is optional, it indicates that a folder should be re-processed," );
		out.println( "        such as re-processing a folder named 'worm_cs01-female-fur'," );
		out.println( "        and in this case no subfolder will be created." );
	}
	
	
	/**
	 * Processes a folder that contains videos
	 * @param  dirName  the directory name
	 * @param  againFlag  when true, a folder is re-processed in placed (i.e., no subfolder is created for the video, but MOVIE_AVI is expected to be in the folder).
	 */
	public static void processFolder( String dirName, boolean againFlag ) {	
		out.println( "\n\n********************************************" );
		out.println( "Processing Folder: " + dirName );
		out.println( "********************************************" );

		// make sure it exists
		File baseFolder = new File( dirName );
		if( baseFolder.exists() == false ) {
			out.println( dirName + " does not exist!" );
			out.println( "Bye." );
			return;
		}; // if
		// make sure it is a folder
		if( baseFolder.isDirectory() == false ) {
			out.println( dirName + " is not a folder!" );
			out.println( "Bye." );
			return;
		}; // if

		File[] folderContents = baseFolder.listFiles();
		Set<File> aviSet = new TreeSet<File>();

		// find the .avi movie files
		for( File eachFile : folderContents ) {
			if( eachFile.isDirectory() == true ) {
				continue;
			}; // if
			if( eachFile.getName().startsWith( "bad" ) == true ) {
				out.println( "Skipping video marked as 'bad' --> " + eachFile.getName() );
				continue;
			}; // if
			if( eachFile.getName().endsWith( ".avi" ) == true ) {
				aviSet.add( eachFile );
			}; // if
			// complain when a movie file name is MOVIE_AVI and we're not in --again option
			if( MOVIE_AVI.equals( eachFile.getName() ) == true && againFlag == false ) {
				out.println( "Movie files named: " + MOVIE_AVI + " can only be used under --again option." );
				out.println( "Unable to continue." );
				return;
			}; // if
		}; // for

		// see how many avi files were found
		if( aviSet.isEmpty() == true ) {
			if( againFlag == false ) {
				out.println( "Did not find movie files. Nothing to do!" );
				return;
			}; // if
			for( File eachFile : folderContents ) {
				if( eachFile.isDirectory() == false ) {
					continue;
				}; // if
				File movieFile = new File( eachFile, MOVIE_AVI );
				if( movieFile.exists() == true ) {
					if( movieFile.getName().startsWith( "bad" ) == true ) {
						out.println( "Skipping video marked as 'bad' --> " + movieFile.getName() );
						continue;
					}; // if
					aviSet.add( movieFile );
				}; // if
			}; // for
			out.println( "( --again option ) Found " + aviSet.size() + " subfolders with " + MOVIE_AVI + " files." );
		}; // if
		for( File a : aviSet ) {
			out.println( a );
		}

		List<String> errorsList = new ArrayList<String>();

		int aviCount = 0;
		try {
			Snappy snappy = new Snappy(); 
			
			for( File aviFile : aviSet ) {
				aviCount++;
				int extension = aviFile.getName().indexOf( ".avi" );
				if( extension == -1 ) {
					out.println( "Unable to find .avi extension in movie file: " + aviFile );
					out.println( "Skipping this file." );
					continue;
				}; // if
				String movieFileWithoutExtention = aviFile.getName().substring( 0, extension );

				// make a new folder based on the name of the worm (avi) video file
				File targetDirectory = new File( baseFolder.getAbsolutePath() + File.separator + PREFIX_FOLDER + movieFileWithoutExtention );
				if( againFlag == true && MOVIE_AVI.equals( aviFile.getName() ) == false ) {
					out.println( "--again  option expects movie file: " + MOVIE_AVI );
					out.println( "Unable to continue." );
					return;
				}; // if

				// are we re-processing the folder?
				if( againFlag == true ) {
					targetDirectory = new File( aviFile.getParent() );
					// delete file.0overlay.jpg files, if any
					int count = 0;
					while( count != -1 ) {
						File overlayFile = new File( targetDirectory, "file." + count + "overlay.jpg" );
						if( overlayFile.exists() == false ) {
							break;
						}; // if
						count++;
						overlayFile.delete();
					}; // while
					// figure out historical versions of three files, if so, find next available number
					int number = 0;
					File historicalFile = null;
					do { 
						number++;
						historicalFile = new File( targetDirectory, "historical." + number + ".abs_points.txt" );
						if( historicalFile.exists() == true ) {
							continue;
						}; // if
						historicalFile = new File( targetDirectory, "historical." + number + "." + Snappy.POINTS_FILENAME );
						if( historicalFile.exists() == true ) {
							continue;
						}; // if
						historicalFile = new File( targetDirectory, "historical." + number + ".frametime.txt" );
					} while( historicalFile.exists() == true );
					for( String each : new String[] { "abs_points.txt", Snappy.POINTS_FILENAME, "frametime.txt" } ) {
						historicalFile = new File( targetDirectory, "historical." + number + "." + each );
						File file = new File( targetDirectory, each );
						if( file.exists() == true ) {
							file.renameTo( historicalFile );
						}; // if
						//out.println( "rename " + each + " to " + historicalFile.getName() );
					}; // for
				}; // if

				out.println();
				out.println( "(" + aviCount + " of " + aviSet.size() + ")  " + aviFile.getName() + "  --> " + targetDirectory.getName() );

				// create the target folder when it does not already exists
				if( targetDirectory.exists() == false ) { 
					boolean doneFlag = targetDirectory.mkdir();
					if( doneFlag == false ) {
						out.println( "\tERROR: Unable to create folder: " + targetDirectory.getAbsolutePath() );
						continue;
					}; // if
				}; // if

				// skip target-directory when name startw with 'bad_'
				if( targetDirectory.getName().startsWith( "bad_" ) == true ) {
					out.println( "\tSkipping directory named with 'bad' --> " + targetDirectory.getName() );
					continue;
				}; // if
				
				// create the points file if not already existing in the folder
				File pointsFile = new File( targetDirectory, Snappy.POINTS_FILENAME );
				if( pointsFile.exists() == false ) {
					String error = null;
					try {
						error = snappy.snap( "file://" + aviFile.getAbsolutePath(), targetDirectory.getAbsolutePath() );
					}
					catch( Exception e ) {
						out.println( "Error in Snappy!" );
						e.printStackTrace();
						if( error == null ) {
							error = "";
						}; // if
						error += " " + e.getMessage();
					}; // if

					// verify whether we had errors
					if( error != null ) {
						out.println( "          failed: " + aviFile.getName() );
						out.println( "            info: " + error );
						if( againFlag == false ) {
							File[] tempDirContents = targetDirectory.listFiles();
							for( File each : tempDirContents ) {
								each.delete();
							}; // for
							targetDirectory.delete();
						}; // if
						continue;
					}; // if
				}
				else {
					out.println( "            " + Snappy.POINTS_FILENAME + " already exists. Nothing was changed." );
					errorsList.add( aviFile.getAbsolutePath() + " \t " + Snappy.POINTS_FILENAME + " already exists. Nothing was changed." );
					continue;
				}; // if
				
				String[] originals = new String[] { ".avi", ".info.xml", ".log.csv" };
				String[] destinationFilename = new String[] { MOVIE_AVI, "info.xml", "log.csv" };
				if( againFlag == false ) {
					for( int k = 0; k < originals.length; k++ ) {
						File originalFile = new File( baseFolder, movieFileWithoutExtention + originals[ k ] );
						File destinationFile = new File( targetDirectory, destinationFilename[ k ] );
						if( DEBUG == false ) {
							// move files into the (target) worm folder
							boolean flag = originalFile.renameTo( destinationFile );
							if( flag == false ) { 
								out.println( "Failed to move: " + originalFile );
								return;
							}; // if
						}
						else {
							// copy the files only
							try {
								FileUtils.copyFile( originalFile, destinationFile );
							}
							catch( Exception e ) {
								out.println( "Failed to copy: " + originalFile );
								e.printStackTrace();
								return;
							}; // try
						}; // if
					}; // for
				}; // if
				
				// data-sticher wil create abs_points text file
				DataStitcher dataStitcher = new DataStitcher( targetDirectory.getAbsolutePath() );
				String error = dataStitcher.run();
				if( error == null ) {
					out.println( "            done: " + aviFile.getName() );
				}
				else {
					// see whether it is an error that can be handled
					if( error.startsWith( DataStitcher.BAD_FRAMES_IN_VIDEO ) == true ) {
						// write the error in a text file
						try {
							BufferedWriter bufferedWriter = new BufferedWriter( new FileWriter( new File( targetDirectory, "errors.txt" ) ) );	
							PrintWriter printWriter = new PrintWriter( bufferedWriter );
							printWriter.println( aviFile.getAbsolutePath() );
							printWriter.println( error );
							printWriter.println( "" );
							printWriter.close();
						}
						catch( Exception e ) {
							e.printStackTrace();
						}; // try
						// rename the folder to a 'bad' folder
						File destination = new File( targetDirectory.getParent(), "bad__" + targetDirectory.getName() );
						boolean flag = targetDirectory.renameTo( destination );
						if( flag == false ) { 
							errorsList.add( aviFile.getAbsolutePath() + " \t " + "Failed to rename to (bad) folder: " + destination );
							out.println( "========== errors found so far: " );
							for( String e : errorsList ) {
								out.println( e );
							}; // for
							out.println( "==================== " );
						}; // if
					}
					else {
						errorsList.add( aviFile.getAbsolutePath() + " \t " + error );
						out.println( "========== errors found so far: " );
						for( String e : errorsList ) {
							out.println( e );
						}; // for
						out.println( "==================== " );
					}; // if
				}; // if
			}; // for	
		}			
		catch( Exception e ) {
			e.printStackTrace();
			System.exit( 1 );
		}; // try  
		
	}
			

} // class MagRecognizer

