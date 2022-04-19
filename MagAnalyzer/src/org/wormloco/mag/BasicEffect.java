/*
 * Filename: BasicEffect.java
 */

package org.wormloco.mag;

import java.awt.image.BufferedImage;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.media.Buffer;
import javax.media.Effect;
import javax.media.Format;
import javax.media.ResourceUnavailableException;

import javax.media.format.VideoFormat;
import javax.media.format.RGBFormat;

import javax.media.util.BufferToImage;
import javax.media.util.ImageToBuffer;

import javax.swing.JPanel;

import com.sun.media.BasicCodec;

/**
 * (Modified from that of WormTracker 2.0, by simplifying the original code)
 * The <code>BasicEffect</code> class simplifies the task of processing video effects. Much of the code was
 * copied from com.sun.media.effects.JAIEffect and then commented. For convenience purposes, subclasses need
 * only implement the <code>getName</code> and <code>process</code> methods.
 * Copyright (c) 2007 Eviatar Yemini & Medical Research Council, UK
 * All rights reserved.
 * 
 * @original-author Eviatar Yemini 
 * @author Aleman-Meza (modifications from the original)
 */
public abstract class BasicEffect extends BasicCodec implements Effect{


	// reference to the panel onto which effect is shown
	protected JPanel targetPanel;

	/**
	 * A utility to convert a <code>javax.media.Buffer</code> into a <code>java.awt.Image</code>.
	 */
	private BufferToImage bufferToImage = null;

	// for formatting output purposes 
	protected static final NumberFormat formatter;

	// for formatting output purposes, zero decimals
	protected static final NumberFormat formatter0;

	static { // static constructor
		formatter = new DecimalFormat( "#0.0#" );
		formatter0 = new DecimalFormat( "#0" );
	}

	/**
	 * Constructs a <code>BasicEffect</code> for subclasses.
	 */
	protected BasicEffect()
	{
		// Initalize the supported input formats (defined in the BasicCodec class).
		inputFormats = new Format[] {
				new RGBFormat(null,
						Format.NOT_SPECIFIED,
						Format.byteArray,
						Format.NOT_SPECIFIED,
						24,
						3, 2, 1,
						3, Format.NOT_SPECIFIED,
						Format.TRUE,
						Format.NOT_SPECIFIED)
		};

		// Initalize the supported output formats (defined in the BasicCodec class).
		outputFormats = new Format[] {
				new RGBFormat(null,
						Format.NOT_SPECIFIED,
						Format.byteArray,
						Format.NOT_SPECIFIED,
						24,
						3, 2, 1,
						3, Format.NOT_SPECIFIED,
						Format.TRUE,
						Format.NOT_SPECIFIED)
		};
	}
	
	@Override
	public Format[] getSupportedOutputFormats(Format input) {
		// Return all output formats.
		if (input == null) {
			return outputFormats;
		}

		// Return output formats matching the input format.
		if (matches(input, inputFormats) != null) {
			return new Format[] { outputFormats[0].intersects(input) };
		} else {
			return new Format[0];
		}
	}

	/**
	 * Convert a <code>javax.media.Buffer</code> into a <code>java.awt.image.BufferedImage</code>.
	 * (coppied from MonitorStream.java of WormTracker/src/camera/media )
	 * 
	 * @param 	buffer
	 * 			The <code>Buffer</code> to convert.
	 * 
	 * @return
	 * 			The resulting <code>BufferedImage</code>.
	 */
	protected BufferedImage buffer2Image(Buffer buffer)
	{
		// Create a converter to change the data buffer into an image.
		if (bufferToImage == null)
		{
			VideoFormat format = (VideoFormat) buffer.getFormat();
			bufferToImage = new BufferToImage(format);
		}
		
		// Convert the data buffer to an image.
		Image image = bufferToImage.createImage(buffer);		

		// The Image is a BufferedImage.
		if (image instanceof BufferedImage)
			return (BufferedImage) image;

		// Convert the Image into a BufferedImage.
		BufferedImage bufferedImage = null;
		if (image != null)
		{
			System.out.println( "buffer2image converto to bufferedimage" );
			bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
					BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = bufferedImage.createGraphics();
			graphics.drawImage(image, 0, 0, null);
		}
		
		// Create a blank BufferedImage.
		else
		{
			Dimension size = ((VideoFormat) buffer.getFormat()).getSize();
			bufferedImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
		}
		return bufferedImage;
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
		return formatter.format( value );
	}


	/**
	 * Formats a double value with a default parameter
	 * @param  value  the value
	 * @return  formatted value with no decimals, or "NULL" when value is null
	 */
	public static String format0( Double value ) {
		if( value == null ) {
			return "NULL";
		}; // if
		return formatter0.format( value );
	}


	/** get the resources needed by this effect **/
	public void open() throws ResourceUnavailableException {
	}


	/** free the resources allocated by this codec **/
	public void close() {
	}


	/** reset the codec **/
	public void reset() {
	}


	/** no controls for this simple effect **/
	public Object[] getControls() {
		return null;
	}

    
	/**
	 * Return the control.
	 */
	public Object getControl(String controlType) {
		return null;
	}


	/************** format methods *************/
	/** set the input format **/
	public Format setInputFormat(Format input) {
		// the following code assumes valid Format
		inputFormat = input;
		return inputFormat;
	}

	/** set the output format **/
	public Format setOutputFormat(Format output) {
		// the following code assumes valid Format
		outputFormat = output;
		return outputFormat;
	}

	/** get the input format **/
	protected Format getInputFormat() {
		return inputFormat;
	}
    
	/** get the output format **/
	protected Format getOutputFormat() {
		return outputFormat;
	}


	/** return effect name **/
	public String getName() {
		String ret = getClass().getName();
		int index = ret.lastIndexOf( "." );
		if( index > 0 && ( index + 1 ) < ret.length() ) {
			ret = ret.substring( index + 1 );
		}; // if
		return ret;
	}

}

