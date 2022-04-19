/*
 * Filename: Frame.java
 */

package org.wormloco.mag;

import java.io.PrintStream;

/**
 * One frame in a video
 *
 * @author  Boanerges Aleman-Meza
 */

public class Frame {

	// number of points collected on each skeleton midline
	public static final int MIDLINE = 13;

	/** point at the center of the midline */
	public static int CENTER_POINT = (int) Math.round( Math.floor( Frame.MIDLINE / 2.0 ) );

	/** constant for radios of 'inside' measurements */
	public static final double RADIOUS = 23.0;

	// flag for debugging purposes
	private static final boolean DEBUG = "true".equalsIgnoreCase( System.getProperty( "DEBUG" ) );

	// the x coordinates
	protected double x[] = new double[ MIDLINE ];
	
	// the y coordinates
	protected double y[] = new double[ MIDLINE ];
	
	// for convenience on println statements
	private static final PrintStream out = System.out;

	// the length of the body from first to last of its points (pixels)
	protected double bodyLength = -999;

	// the smooth length of the body by considering (two) previous and (two) next frames 
	protected double smoothBodyLength = -999;

	// whether the length of frame is actually a local-maxima
	protected boolean localMaxima = false;

	// whether the length of frame is actually a local-minima
	protected boolean localMinima = false;

	// bending angle of head with respect to the body
	protected double bendingHead = 0;

	// bending angle of the body
	protected double bendingBody = 0;

	// whether this frame is on the repellent
	protected boolean onTheRepellent = false;

	// speed of the center point over time
	protected double speed = -999;

	// speed of a point over time
	protected double[] speedAt = new double[ MIDLINE ];

	// reference to the stride to which this frame belongs to (null means none)
	protected Stride stride = null;

	// the direction change angle
	protected Double directionChangeAngle = null;

	// flag of whether it is not striding
	protected boolean stridingFlag = false;


	/** 
	 * Calculates the body length 
	 */
	public void calculateBodyLength() {
		double sum = 0.0;
		for( int p = 0; p < MIDLINE; p++ ) {
			if( p > 0 ) {
				sum += Utilities.distance( x[ p - 1 ], y[ p - 1 ], x[ p ], y[ p ] );
			}; // if
		}; // for
		bodyLength = sum;
	}


	/**
	 * Sets the stride of this frame
	 * @param  stride  the stride
	 */
	public void setStride( Stride stride ) {
		this.stride = stride;
	}


	/**
	 * Sets the direction change angle of this frame
	 * @param  angle  the direction change angle
	 */
	public void setDirectionChangeAngle( Double angle ) {
		this.directionChangeAngle = angle;
	}


	/** 
	 * Determine whether any of the points is outside (i.e., over the repellent),
	 * if so, then the 'onTheRepellent' boolean variable is set to true
	 */
	public void verifyPositionOnTheRepellent() {
		for( int p = 0; p < MIDLINE; p++ ) {
			double dist = Utilities.distance( 0, 0, x[ p ], y[ p ] );
			if( dist >= RADIOUS ) {
				onTheRepellent = true;
				break;
			}; // if
		}; // for
	}


} // class Frame

