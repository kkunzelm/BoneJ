package org.doube.bonej.pqct.selectroi.liveWireEngine;

import java.util.PriorityQueue;

/*
	Modified by Timo Rantalainen 2012 - 2014 tjrantal at gmail dot com from IvusSnakes (http://ivussnakes.sourceforge.net/) ImageJ plugin
	A Class to calculate LiveWire paths.

	Changed the implementation back to the one suggested in Barret & Mortensen 1997.
	Interactive live-wire boundary extraction. Medical Image Analysis (1996/7) volume 1, number 4, pp 331-341.

	 The code is licensed under GPL 3.0 or newer
*/

public class LiveWireCosts implements Runnable {
	double[][] imagePixels; // stores Pixels from original image
	int[][] imageCosts; // stores Costs for every pixel
	PriorityQueue<PixelNode> pixelCosts;
	double[][] gradientrows; // stores image gradient modulus
	double[][] gradientcolumns; // stores image gradient modulus
	// it is oriented: X = LEFT TO RIGHT
	// Y = UP TO DOWN
	public double[][] gradientr; // stores image gradient RESULTANT modulus
	public double[][] laplacian;

	public int[][][] whereFrom; // stores where from path started
	boolean[][] visited; // stores whether the node was marked or not
	int rows;
	int columns;
	int sr, sc; // seed x and seed y, weight zero for this point

	private int tr, tc;// thread x and y passed as parameters

	Thread myThread = null;
	boolean myThreadRuns;// flag for thread state

	private final double gw;// Gradient Magnitude Weight
	private final double dw;// Gradient Direction Weight
	private final double zw;// Binary Laplacian Weight

	/**
	 * Constructor
	 * 
	 * @param imagePixels
	 *            2D gray scale image in
	 */
	// initializes Dijkstra with the image
	public LiveWireCosts(final double[][] imagePixels) {

		// initializes weights for edge cost taken from Barret 1997
		// these are default values
		gw = 0.43;
		zw = 0.43;
		dw = 0.13;
		// initializes all other matrices
		rows = imagePixels.length;
		columns = imagePixels[0].length;
		this.imagePixels = imagePixels;
		pixelCosts = new PriorityQueue<PixelNode>();
		whereFrom = new int[rows][columns][2];
		visited = new boolean[rows][columns];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				visited[i][j] = false;
			}
		}
		initGradient();
		initLaplacian();
		// inits the thread
		// myThread = new Thread(this);
	}

	/*
	 * initializes gradient image
	 */
	private void initGradient() {
		gradientrows = new double[rows][columns];
		gradientcolumns = new double[rows][columns];
		gradientr = new double[rows][columns];
		// Using sobel
		// for gx convolutes the following matrix
		//
		// |-1 0 1|
		// Gx = |-2 0 2|
		// |-1 0 1|

		for (int i = 1; i < rows - 1; ++i) {
			for (int j = 1; j < columns - 1; ++j) {
				gradientrows[i][j] = -1 * (imagePixels[i - 1][j - 1]) + 1 * (imagePixels[i + 1][j - 1])
						- 2 * (imagePixels[i - 1][j]) + 2 * (imagePixels[i + 1][j]) - 1 * (imagePixels[i - 1][j + 1])
						+ 1 * (imagePixels[i + 1][j + 1]);
			}
		}

		// for gy convolutes the following matrix
		//
		// |-1 -2 -1|
		// Gy = | 0 0 0|
		// |+1 +2 +1|
		//
		for (int i = 1; i < rows - 1; ++i) {
			for (int j = 1; j < columns - 1; ++j) {
				gradientcolumns[i][j] = -1 * (imagePixels[i - 1][j - 1]) + 1 * (imagePixels[i - 1][j + 1])
						- 2 * (imagePixels[i][j - 1]) + 2 * (imagePixels[i][j + 1]) - 1 * (imagePixels[i + 1][j - 1])
						+ 1 * (imagePixels[i + 1][j + 1]);
			}
		}
		for (int i = 1; i < rows - 1; i++) {
			for (int j = 1; j < columns - 1; j++) {
				gradientr[i][j] = Math
						.sqrt(gradientrows[i][j] * gradientrows[i][j] + gradientcolumns[i][j] * gradientcolumns[i][j]);
			}
		}

		final double grMax = arrMax(gradientr);
		for (int i = 0; i < gradientr.length; ++i) {
			for (int j = 0; j < gradientr[i].length; ++j) {
				gradientr[i][j] = 1.0 - gradientr[i][j] / grMax;
			}
		}
	}

	public static double arrMax(final double[] matrix) {
		double maximum = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < matrix.length; ++i) {
			if (matrix[i] > maximum)
				maximum = matrix[i];
		}
		return maximum;
	}

	public static double arrMax(final double[][] matrix) {
		double maximum = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < matrix.length; ++i) {
			for (int j = 0; j < matrix[i].length; ++j) {
				if (matrix[i][j] > maximum)
					maximum = matrix[i][j];
			}
		}
		return maximum;
	}

	/*
	 * initializes laplacian image zero-crossings. Marks zero-crossings with 0,
	 * otherwise the value is 1
	 */
	private void initLaplacian() {
		laplacian = new double[rows][columns];

		// Using finite differences
		// convolute with
		//
		// |0 1 0|
		// Gx = |1 -4 1|
		// |0 1 0|
		final double[][] laplacianKernel = { { 0, 1, 0 }, { 1, -4, 1 }, { 0, 1, 0 } };

		for (int i = 1; i < rows - 1; i++) {
			for (int j = 1; j < columns - 1; j++) {
				laplacian[i][j] = 0;
				for (int j2 = -1; j2 <= 1; ++j2) {
					for (int i2 = -1; i2 <= 1; ++i2) {
						laplacian[i][j] += imagePixels[i + i2][j + j2] * laplacianKernel[i2 + 1][j2 + 1];
					}
				}
			}
		}

		/* Search for zero crossing to binarize the result */
		double[][] tempLap = new double[rows][columns];
		for (int i = 0; i < tempLap.length; ++i) {
			for (int j = 0; j < tempLap[i].length; ++j) {
				tempLap[i][j] = 1d;
			}
		}
		/* Check pixel neighbourhoods for zero-crossings */
		final int[][] neighbourhood = new int[8][2]; // 8 connected
														// neighbourhood
		final int[][] neighbourIndices = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 }, { 1, 0 }, { 1, -1 },
				{ 0, -1 } };
		for (int i = 1; i < rows - 1; i++) {
			for (int j = 1; j < columns - 1; j++) {
				tempLap[i][j] = 1;
				if (laplacian[i][j] == 0) { /* No need to check neighbours */
					tempLap[i][j] = 0;
				} else { /* Check neighbours */
					// Check 8-connected neighbour
					for (int th = 0; th < 8; ++th) {
						neighbourhood[th][0] = i + neighbourIndices[th][0];
						neighbourhood[th][1] = j + neighbourIndices[th][1];
					}
					final int[] centre = { i, j };
					tempLap = checkNeighbours(tempLap, neighbourhood, centre);

				}
			}
		}

		/* OverWrite Laplacian */
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				laplacian[i][j] = tempLap[i][j];
			}
		}
	}

	/*
	 * Check neigbours for Laplacian zero-crossing
	 * 
	 * @param tempLap mask of zero-crossings
	 * 
	 * @param neigbourhood the neighbourhood to check
	 * 
	 * @param centre the coordinates of the centre pixel to check the
	 * neighbourhood for
	 * 
	 * @return tempLap the zero-crossings mask
	 */
	protected double[][] checkNeighbours(final double[][] tempLap, final int[][] neighbourhood, final int[] centre) {
		int[] coordinates;
		for (int r = 0; r < neighbourhood.length; ++r) {
			coordinates = neighbourhood[r];
			if (Math.signum(laplacian[coordinates[0]][coordinates[1]]) != Math
					.signum(laplacian[centre[0]][centre[1]])) { /*
																 * Signs differ,
																 * mark border
																 */
				/* Mark the one, which is closer to zero */
				if (Math.abs(laplacian[centre[0]][centre[1]]) < Math.abs(laplacian[coordinates[0]][coordinates[1]])) {
					tempLap[centre[0]][centre[1]] = 0; /*
														 * zero-crossing
														 * detected, change to 0
														 * to disable laplacian
														 */
				}
			}
		}
		return tempLap;
	}

	/*
	 * Calculates edge direction cost
	 * 
	 * @param sr source pixel x-coordinate
	 * 
	 * @param sc source pixel y-coordinate
	 * 
	 * @param dr destination pixel x-coordinate
	 * 
	 * @param dc destination pixel y-coordinate
	 * 
	 * @return edgeDirectionCostValue edge direction cost
	 * 
	 */
	private double edgeDirectionCost(final int sr, final int sc, final int dr, final int dc) {
		final Vector2d Dp = (new Vector2d(gradientrows[sr][sc], -gradientcolumns[sr][sc])).getUnit();
		final Vector2d Dq = (new Vector2d(gradientrows[dr][dc], -gradientcolumns[dr][dc])).getUnit();
		final Vector2d p = new Vector2d(sr, sc);
		final Vector2d q = new Vector2d(dr, dc);
		Vector2d L;
		if (Dp.dotProduct(q.sub(p)) >= 0) {
			L = q.sub(p).getUnit();
		} else {
			L = p.sub(q).getUnit();
		}
		/* Barret 1996/1997 eqs 3 & 4 */
		final double edgeDirectionCostValue = 2.0 / (3.0 * Math.PI)
				* (Math.acos(Dp.dotProduct(L)) + Math.acos(L.dotProduct(Dq)));
		return edgeDirectionCostValue;

	}

	// returns the edge cost of going from sx,sy to dx,dy
	private double edgeCost(final int sr, final int sc, final int dr, final int dc) {
		// fg is the Gradient Magnitude
		/* Debugging, test liveWire without gradient direction... */
		double edgeCostSum = gw * gradientr[dr][dc] + zw * laplacian[dr][dc];
		edgeCostSum += edgeDirectionCost(sr, sc, dr, dc) * dw;
		return edgeCostSum;
	}

	/*
	 * updates Costs and Paths for a given point calculated over 8 directions N,
	 * NE, E, SE, S, SW, W, NW
	 * 
	 * @param r target pixel x-coordinate
	 * 
	 * @param c target pixel y-coordinate
	 */
	private void updateCosts(final int r, final int c, final double mycost) {

		pixelCosts.poll();
		final int[][] neighbourhood = new int[8][2]; // 8 connected
														// neighbourhood
		final int[][] neighbourIndices = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 }, { 1, 0 }, { 1, -1 },
				{ 0, -1 } };
		// Check 8-connected neighbour
		for (int th = 0; th < 8; ++th) {
			neighbourhood[th][0] = r + neighbourIndices[th][0];// (int)
																// Math.round(Math.sin((double)
																// th));
			neighbourhood[th][1] = c + neighbourIndices[th][1];// +(int)
																// Math.round(Math.cos((double)
																// th));
		}
		int[] coordinates;
		for (int i = 0; i < neighbourhood.length; ++i) {
			coordinates = neighbourhood[i];
			if (coordinates[0] >= 0 && coordinates[0] < rows && coordinates[1] >= 0 && coordinates[1] < columns) {
				final int[] fromCoords = new int[2];
				fromCoords[0] = r;
				fromCoords[1] = c;
				final int[] pixelCoords = new int[2];
				pixelCoords[0] = neighbourhood[i][0];
				pixelCoords[1] = neighbourhood[i][1];
				pixelCosts.add(new PixelNode(pixelCoords,
						mycost + edgeCost(r, c, neighbourhood[i][0], neighbourhood[i][1]), fromCoords));
			}
		}
		visited[r][c] = true;
	}

	/**
	 * Returns the path from seed point to point r,c
	 * 
	 * @param r
	 *            x-coordinate of the target
	 * @param c
	 *            y-coordinate of the target
	 * @return m x 2 array of the m-length path with x-, and y-coordinates
	 */
	public int[][] returnPath(final int r, final int c) {
		// returns the path given mouse position

		final int[][] pathCoordinates = new int[rows * columns][2];

		if (visited[r][c] == false) {
			// attempt to get path before creating it
			// this might occur because of the thread
			return null;
		}
		int length = 0;
		int myr = r;
		int myc = c;
		int nextr;
		int nextc;
		pathCoordinates[length][0] = r;
		pathCoordinates[length][1] = c;
		// System.out.println("sr "+sr+" sc "+sc);
		do { // while we haven't found the seed
			++length;
			nextr = whereFrom[myr][myc][0];
			nextc = whereFrom[myr][myc][1];
			myr = nextr;
			myc = nextc;
			pathCoordinates[length][0] = nextr;
			pathCoordinates[length][1] = nextc;
			// System.out.println("nr "+nextr+" nc "+nextc);
		} while (!((myr == sr) && (myc == sc)));

		// path is from last point to first
		// we need to invert it
		final int[][] pathToReturn = new int[length + 1][2];
		for (int i = 0; i <= length; i++) {

			pathToReturn[i][0] = pathCoordinates[length - i][0];
			pathToReturn[i][1] = pathCoordinates[length - i][1];
		}
		return pathToReturn;
	}

	/*
	 * set the seed point to start Dijkstra
	 * 
	 * @param r x-coordinate of the seed
	 * 
	 * @param c y-coordinate of the seed
	 */
	public void setSeed(final int r, final int c) {
		// System.out.println("Setting Seed r "+r+" c "+c);
		myThreadRuns = false;
		if (myThread != null) {
			try {
				myThread.join();
			} catch (final InterruptedException e) {
				System.out.println("Bogus Exception");
			}
		}
		// System.out.println("Setting Seed joined");
		tr = r;
		tc = c;
		myThreadRuns = true;
		myThread = new Thread(this);
		myThread.start();

	}

	/** Implement the Runnable interface */
	@Override
	public void run() {
		// runs set point in parallel
		final int r = tr;
		final int c = tc;

		int[] nextIndex;
		final int nextR;
		final int nextC;
		sr = r;
		sc = c;

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				visited[i][j] = false;
			}
		}

		visited[r][c] = true; // mark as visited
		int[] coordinates = { r, c };
		whereFrom[r][c] = coordinates;

		// update costs
		updateCosts(r, c, 0);

		while ((pixelCosts.peek() != null) && (myThreadRuns)) {
			nextIndex = pixelCosts.peek().getIndex();
			whereFrom[nextIndex[0]][nextIndex[1]] = pixelCosts.peek().getWhereFrom();

			updateCosts(nextIndex[0], nextIndex[1], pixelCosts.peek().getDistance());

			// removes pixels that are already visited and went to the queue
			while (true) {
				if (pixelCosts.peek() == null)
					break;
				coordinates = pixelCosts.peek().getIndex();
				if (visited[coordinates[0]][coordinates[1]] == false)
					break;
				pixelCosts.poll();
			}
		}
		/* Empty the pixelCosts queue */
		pixelCosts.clear();
	}

}
