/*
 * Filename: Run.java
 */

package org.wormloco.mag;

import java.io.PrintStream;

import java.util.ArrayList;
import java.util.List;


/**
 * One Run: a sequence of consecutive strides
 *
 * @author  Boanerges Aleman-Meza
 */

public class Run {


	/** constant for minimum number of strides to have so that a run is valid */
	public static final int MINIMUM_STRIDES_FOR_BEING_VALID_STRIDE = 3;

	// for convenience on println statements
	private static final PrintStream out = System.out;

	// the list of strides of the run
	protected List<Stride> strideList;

	// the number of frames to tolerate when two strides are not adjacent to each other
	public final int cushion;

	// the run number (for the purpose of identifying different runs in same video)
	public final int runNumber;


	/**
	 * Default constructor
	 * @param  runNumber  the arbitrary number assigned to the run
	 * @param  cushion  number of frames to tolerate when two strides are not adjacent 
	 */
	public Run( int runNumber, int cushion ) {
		this.runNumber = runNumber;
		this.cushion = cushion;
		strideList = new ArrayList<Stride>();
	}


	/**
	 * Adds a stride to the run
	 * @param  stride  the stride
	 * @return  true when it was added; false otherwise
	 */
	public boolean addStride( Stride stride ) {
		if( stride == null ) {
			return false;
		}; // if
		if( strideList.isEmpty() == true ) {
			strideList.add( stride );
			return true;
		}; // if
		Stride lastStride = strideList.get( strideList.size() - 1 );
		if( stride.indexFirstMinima > ( lastStride.indexSecondMinima + cushion ) ) {
			return false;
		}; // if
		strideList.add( stride );
		return true;
	}


	/**
	 * Returns whether the run is valid (e.g., it has at least a minimum of strides)
	 * @return  true when valid; false otherwise
	 */
	public boolean isValid() {
		if( strideList.size() >= MINIMUM_STRIDES_FOR_BEING_VALID_STRIDE ) {
			return true;
		}; // if
		return false;

	}


	/**
	 * Resets the run by deleting all strides in it
	 */
	public void reset() {
		strideList.clear();
	}


	public String toString() {
		return " run" + runNumber + "(#strides:" + strideList.size() + ")";
	}


} // class Run


