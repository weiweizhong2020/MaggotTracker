/*
 * Filename: FileFilterVideo.java
 */

package org.wormloco.mag;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * Basic selection of video files (e.g., .avi)
 */

public class FileFilterVideo extends FileFilter {

	@Override
	public boolean accept( File file ) {
		if( file.isDirectory() == true ) {
			return true;
		}; // if
		if( file.getName().toLowerCase().endsWith( ".avi" ) == true ) {
			return true;
		}; // if
		return false;
	}

	@Override
	public String getDescription() {
		return "Video Files ( *.avi )";
	}

}

