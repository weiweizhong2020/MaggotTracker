/*
 * StagePosition.java	
 *
 */

package org.wormloco.mag;

/**
 * Keeps information of x,y coordinates at a given timeframe
 *
 * @author Aleman-Meza
 */

public class StagePosition {
	public final double timeframe;

	public final double x;
	public final double y;

	public StagePosition( double timeframe, double x, double y ) {
		this.timeframe = timeframe;
		this.x = x;
		this.y = y;
	}

	public String toString() {
		return "( " + timeframe + " : " + x + " , " + y + " )";
	}
}

