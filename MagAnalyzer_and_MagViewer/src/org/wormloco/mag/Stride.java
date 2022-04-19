/*
 * Filename: Stride.java
 */

package org.wormloco.mag;

import java.io.PrintStream;

/**
 * One Stride: a step when body length reaches local minimum and ends till the next local minimum.
 *
 * @author  Boanerges Aleman-Meza
 */

public class Stride {


	// for convenience on println statements
	private static final PrintStream out = System.out;

	/** distance traveled by the stride (at each point) */
	public final double[] distanceTraveledAt;

	/** distance traveled by the stride (using center point) */
	public final double distanceTraveledCenterPoint;

	/** index of the first local minima */
	public final int indexFirstMinima;

	/** index of the second local minima */
	public final int indexSecondMinima;

	/** number of this stride */
	public final int strideNumber;

	/** whether any point was over the repellent */
	public final boolean overRepellent;

	// the contraction rate
	protected double contractionRate = -999;

	// the extension rate
	protected double extensionRate = -999;


	/**
	 * Default constructor
	 */
	public Stride( int indexFirstMinima, int indexSecondMinima, double[] distanceTraveledAt, int strideNumber, boolean overRepellent, double distanceTraveledCenterPoint ) {
		this.indexFirstMinima = indexFirstMinima;
		this.indexSecondMinima = indexSecondMinima;
		this.distanceTraveledAt = distanceTraveledAt;
		this.strideNumber = strideNumber;
		this.overRepellent = overRepellent;
		this.distanceTraveledCenterPoint = distanceTraveledCenterPoint;
	}


} // class Stride


