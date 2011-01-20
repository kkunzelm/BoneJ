/**
 * ParticleGetter.java Copyright 2010 Keith Schulze
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.doube.bonej.particleanalyser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

/**
 * @author Keith Schulze
 *
 * This is a class containing various static methods for extracting Particle data from a Binary
 * Image.
 */
public class ParticleGetter {

	/** Foreground value */
	public final static int FORE = -1;

	/** Background value */
	public final static int BACK = 0;
	
	/** Particle joining method */
	public final static int MULTI = 0, LINEAR = 1;
	
	
	/**
	 * Get particles, particle labels and particle sizes from a 3D ImagePlus
	 * 
	 * @param imp
	 *            Binary input image
	 * @param slicesPerChunk
	 *            number of slices per chunk. 2 is generally good.
	 * @param minVol
	 *            minimum volume particle to include
	 * @param maxVol
	 *            maximum volume particle to include
	 * @param phase
	 *            foreground or background (FORE or BACK)
	 * @param doExclude
	 *            if true, remove particles touching sides of the stack
	 * @return Object[] {byte[][], int[][]} containing a binary workArray and
	 *         particle labels.
	 */

	public static Object[] getParticles(ImagePlus imp, int labelMethod, int slicesPerChunk,
			double minVol, double maxVol, int phase) {
		byte[][] workArray = makeWorkArray(imp);
		return getParticles(imp, workArray, labelMethod, slicesPerChunk, minVol, maxVol,
				phase);
	}

	public static Object[] getParticles(ImagePlus imp, int slicesPerChunk, int phase) {
		byte[][] workArray = makeWorkArray(imp);
		int labelMethod = ParticleGetter.MULTI;
		double minVol = 0;
		double maxVol = Double.POSITIVE_INFINITY;
		return getParticles(imp, workArray, labelMethod, slicesPerChunk, minVol, maxVol,
				phase);
	}

	public static Object[] getParticles(ImagePlus imp, byte[][] workArray, int labelMethod,
			int slicesPerChunk, int phase, int method) {
		double minVol = 0;
		double maxVol = Double.POSITIVE_INFINITY;
		return getParticles(imp, workArray, labelMethod, slicesPerChunk, minVol, maxVol,
				phase);
	}

	/**
	 * Get particles, particle labels and sizes from a workArray using an
	 * ImagePlus for scale information
	 * 
	 * @param imp
	 *            input binary image
	 * @param binary
	 *            work array
	 * @param slicesPerChunk
	 *            number of slices to use for each chunk
	 * @param minVol
	 *            minimum volume particle to include
	 * @param maxVol
	 *            maximum volume particle to include
	 * @param phase
	 *            FORE or BACK for foreground or background respectively
	 * @return Object[] array containing a binary workArray, particle labels and
	 *         particle sizes
	 */
	public static Object[] getParticles(ImagePlus imp, byte[][] workArray, int labelMethod,
			int slicesPerChunk, double minVol, double maxVol, int phase) {
		String sPhase = "";
		String chunkString = "";
		if (phase == FORE) {
			sPhase = "foreground";
		} else if (phase == BACK) {
			sPhase = "background";
		} else {
			throw new IllegalArgumentException();
		}
		if (slicesPerChunk < 1) {
			throw new IllegalArgumentException();
		}
		// Set up the chunks
		final int nChunks = getNChunks(imp, slicesPerChunk);
		final int[][] chunkRanges = getChunkRanges(imp, nChunks, slicesPerChunk);
		final int[][] stitchRanges = getStitchRanges(imp, nChunks,
				slicesPerChunk);

		int[][] particleLabels = firstIDAttribution(imp, workArray, phase, sPhase);

		if (labelMethod == MULTI) {
			// connect particles within chunks
			final int nThreads = Runtime.getRuntime().availableProcessors();
			ConnectStructuresThread[] cptf = new ConnectStructuresThread[nThreads];
			for (int thread = 0; thread < nThreads; thread++) {
				cptf[thread] = new ConnectStructuresThread(thread, nThreads,
						imp, workArray, particleLabels, phase, sPhase, nChunks, chunkRanges,
						chunkString);
				cptf[thread].start();
			}
			try {
				for (int thread = 0; thread < nThreads; thread++) {
					cptf[thread].join();
				}
			} catch (InterruptedException ie) {
				IJ.error("A thread was interrupted.");
			}

			// connect particles between chunks
			if (nChunks > 1) {
				chunkString = ": stitching...";
				connectStructures(imp, workArray, particleLabels, phase, sPhase, chunkString,
						stitchRanges);
			}
		} else if (labelMethod == LINEAR) {
			joinStructures(imp, particleLabels, phase, sPhase);
		}
		minimiseLabels(particleLabels, sPhase);
		long[] particleSizes = getParticleSizes(particleLabels, sPhase);
		double[][] centroids = getCentroids(imp, particleLabels, particleSizes);
		int[][] limits = getParticleLimits(imp, particleLabels, particleSizes.length);
		List<List<Face>> edgesTouched = detectEdgesTouched(imp, particleLabels, workArray, particleSizes.length);
		
		Object[] result = { workArray, particleLabels, particleSizes, centroids, limits, edgesTouched };
		return result;
	}
	
	public static List<Particle> createParticleList(ImagePlus img, List<List<Face>> edgesTouched, double[][] centroids, 
			int[][] limits, long[] particleSizes){
		if (edgesTouched.size() == centroids.length && centroids.length == limits.length && limits.length == particleSizes.length) {
			List<Particle> particleList = new ArrayList<Particle>();
			Calibration cal = img.getCalibration();
			
			for (int i = 0; i < particleSizes.length; i++) {
				String name = "Particle " + i;
				Particle p = new Particle(i, name, cal, edgesTouched.get(i), centroids[i], limits[i], particleSizes[i]);
				particleList.add(p);
			}
			
			return particleList;
		} else {
			throw new IllegalArgumentException("The arguments were not the same length");
		}
	}
	
	/**
	 * Create a work array
	 * 
	 * @return byte[] work array
	 */
	public static byte[][] makeWorkArray(ImagePlus imp) {
		final int s = imp.getStackSize();
		final int p = imp.getWidth() * imp.getHeight();
		byte[][] workArray = new byte[s][p];
		ImageStack stack = imp.getStack();
		for (int z = 0; z < s; z++) {
			ImageProcessor ip = stack.getProcessor(z + 1);
			for (int i = 0; i < p; i++) {
				workArray[z][i] = (byte) ip.get(i);
			}
		}
		return workArray;
	}
	
	/**
	 * Gets number of chunks needed to divide a stack into evenly-sized sets of
	 * slices.
	 * 
	 * @param imp
	 *            input image
	 * @param slicesPerChunk
	 *            number of slices per chunk
	 * @return number of chunks
	 */
	public static int getNChunks(ImagePlus imp, int slicesPerChunk) {
		final int d = imp.getImageStackSize();
		int nChunks = (int) Math.floor((double) d / (double) slicesPerChunk);

		int remainder = d % slicesPerChunk;

		if (remainder > 0) {
			nChunks++;
		}
		return nChunks;
	}
	
	/**
	 * Get a 2 d array that defines the z-slices to scan within while connecting
	 * particles within chunkified stacks.
	 * 
	 * @param nC
	 *            number of chunks
	 * @return scanRanges int[][] containing 4 limits: int[0][] - start of outer
	 *         for; int[1][] end of outer for; int[3][] start of inner for;
	 *         int[4] end of inner 4. Second dimension is chunk number.
	 */
	public static int[][] getChunkRanges(ImagePlus imp, int nC, int slicesPerChunk) {
		final int nSlices = imp.getImageStackSize();
		int[][] scanRanges = new int[4][nC];
		scanRanges[0][0] = 0; // the first chunk starts at the first (zeroth)
		// slice
		scanRanges[2][0] = 0; // and that is what replaceLabel() will work on
		// first

		if (nC == 1) {
			scanRanges[1][0] = nSlices;
			scanRanges[3][0] = nSlices;
		} else if (nC > 1) {
			scanRanges[1][0] = slicesPerChunk;
			scanRanges[3][0] = slicesPerChunk;

			for (int c = 1; c < nC; c++) {
				for (int i = 0; i < 4; i++) {
					scanRanges[i][c] = scanRanges[i][c - 1] + slicesPerChunk;
				}
			}
			// reduce the last chunk to nSlices
			scanRanges[1][nC - 1] = nSlices;
			scanRanges[3][nC - 1] = nSlices;
		}
		return scanRanges;
	}
	
	/**
	 * Return scan ranges for stitching. The first 2 values for each chunk are
	 * the first slice of the next chunk and the last 2 values are the range
	 * through which to replaceLabels()
	 * 
	 * Running replace labels over incrementally increasing volumes as chunks
	 * are added is OK (for 1st interface connect chunks 0 & 1, for 2nd connect
	 * chunks 0, 1, 2, etc.)
	 * 
	 * @param nC
	 *            number of chunks
	 * @return scanRanges list of scan limits for connectStructures() to stitch
	 *         chunks back together
	 */
	public static int[][] getStitchRanges(ImagePlus imp, int nC, int slicesPerChunk) {
		final int nSlices = imp.getImageStackSize();
		if (nC < 2) {
			return null;
		}
		int[][] scanRanges = new int[4][3 * (nC - 1)]; // there are nC - 1
		// interfaces

		for (int c = 0; c < nC - 1; c++) {
			scanRanges[0][c] = (c + 1) * slicesPerChunk;
			scanRanges[1][c] = (c + 1) * slicesPerChunk + 1;
			scanRanges[2][c] = c * slicesPerChunk; // forward and reverse
			// algorithm
			// scanRanges[2][c] = 0; //cumulative algorithm - reliable but O²
			// hard
			scanRanges[3][c] = (c + 2) * slicesPerChunk;
		}
		// stitch back
		for (int c = nC - 1; c < 2 * (nC - 1); c++) {
			scanRanges[0][c] = (2 * nC - c - 2) * slicesPerChunk - 1;
			scanRanges[1][c] = (2 * nC - c - 2) * slicesPerChunk;
			scanRanges[2][c] = (2 * nC - c - 3) * slicesPerChunk;
			scanRanges[3][c] = (2 * nC - c - 1) * slicesPerChunk;
		}
		// stitch forwards (paranoid third pass)
		for (int c = 2 * (nC - 1); c < 3 * (nC - 1); c++) {
			scanRanges[0][c] = (-2 * nC + c + 3) * slicesPerChunk;
			scanRanges[1][c] = (-2 * nC + c + 3) * slicesPerChunk + 1;
			scanRanges[2][c] = (-2 * nC + c + 2) * slicesPerChunk;
			scanRanges[3][c] = (-2 * nC + c + 4) * slicesPerChunk;
		}
		for (int i = 0; i < scanRanges.length; i++) {
			for (int c = 0; c < scanRanges[i].length; c++) {
				if (scanRanges[i][c] > nSlices) {
					scanRanges[i][c] = nSlices;
				}
			}
		}
		scanRanges[3][nC - 2] = nSlices;
		return scanRanges;
	}
	
	/**
	 * Go through all pixels and assign initial particle label
	 * 
	 * @param workArray
	 *            byte[] array containing pixel values
	 * @param phase
	 *            FORE or BACK for foreground of background respectively
	 * @return particleLabels int[] array containing label associating every
	 *         pixel with a particle
	 */
	private static int[][] firstIDAttribution(ImagePlus imp, final byte[][] workArray,
			final int phase, String sPhase) {
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getImageStackSize();
		final int wh = w * h;
		IJ.showStatus("Finding " + sPhase + " structures");
		int[][] particleLabels = new int[d][wh];
		int ID = 1;

		if (phase == FORE) {
			for (int z = 0; z < d; z++) {
				for (int y = 0; y < h; y++) {
					final int rowIndex = y * w;
					for (int x = 0; x < w; x++) {
						final int arrayIndex = rowIndex + x;
						if (workArray[z][arrayIndex] == phase) {
							particleLabels[z][arrayIndex] = ID;
							int minTag = ID;
							// Find the minimum particleLabel in the
							// neighbouring pixels
							for (int vZ = z - 1; vZ <= z + 1; vZ++) {
								for (int vY = y - 1; vY <= y + 1; vY++) {
									for (int vX = x - 1; vX <= x + 1; vX++) {
										if (withinBounds(vX, vY, vZ, w, h, 0, d)) {
											final int offset = getOffset(vX,
													vY, w);
											if (workArray[vZ][offset] == phase) {
												final int tagv = particleLabels[vZ][offset];
												if (tagv != 0 && tagv < minTag) {
													minTag = tagv;
												}
											}
										}
									}
								}
							}
							// assign the smallest particle label from the
							// neighbours to the pixel
							particleLabels[z][arrayIndex] = minTag;
							// increment the particle label
							if (minTag == ID) {
								ID++;
							}
						}
					}
				}
				IJ.showProgress(z, d);
			}
			ID++;
		} else if (phase == BACK) {
			for (int z = 0; z < d; z++) {
				for (int y = 0; y < h; y++) {
					final int rowIndex = y * w;
					for (int x = 0; x < w; x++) {
						final int arrayIndex = rowIndex + x;
						if (workArray[z][arrayIndex] == phase) {
							particleLabels[z][arrayIndex] = ID;
							int minTag = ID;
							// Find the minimum particleLabel in the
							// neighbouring pixels
							int nX = x, nY = y, nZ = z;
							for (int n = 0; n < 7; n++) {
								switch (n) {
								case 0:
									break;
								case 1:
									nX = x - 1;
									break;
								case 2:
									nX = x + 1;
									break;
								case 3:
									nY = y - 1;
									nX = x;
									break;
								case 4:
									nY = y + 1;
									break;
								case 5:
									nZ = z - 1;
									nY = y;
									break;
								case 6:
									nZ = z + 1;
									break;
								}
								if (withinBounds(nX, nY, nZ, w, h, 0, d)) {
									final int offset = getOffset(nX, nY, w);
									if (workArray[nZ][offset] == phase) {
										final int tagv = particleLabels[nZ][offset];
										if (tagv != 0 && tagv < minTag) {
											minTag = tagv;
										}
									}
								}
							}
							// assign the smallest particle label from the
							// neighbours to the pixel
							particleLabels[z][arrayIndex] = minTag;
							// increment the particle label
							if (minTag == ID) {
								ID++;
							}
						}
					}
				}
				IJ.showProgress(z, d);
			}
			ID++;
		}
		return particleLabels;
	}
	
	/**
	 * Connect structures = minimisation of IDs
	 * 
	 * @param workArray
	 * @param particleLabels
	 * @param phase
	 *            foreground or background
	 * @param scanRanges
	 *            int[][] listing ranges to run connectStructures on
	 * @return particleLabels with all particles connected
	 */
	public static void connectStructures(ImagePlus imp, final byte[][] workArray,
			int[][] particleLabels, final int phase, final String sPhase, final String chunkString,
			final int[][] scanRanges) {
		IJ.showStatus("Connecting " + sPhase + " structures" + chunkString);
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getImageStackSize();
		for (int c = 0; c < scanRanges[0].length; c++) {
			final int sR0 = scanRanges[0][c];
			final int sR1 = scanRanges[1][c];
			final int sR2 = scanRanges[2][c];
			final int sR3 = scanRanges[3][c];
			if (phase == FORE) {
				for (int z = sR0; z < sR1; z++) {
					for (int y = 0; y < h; y++) {
						final int rowIndex = y * w;
						for (int x = 0; x < w; x++) {
							final int arrayIndex = rowIndex + x;
							if (workArray[z][arrayIndex] == phase
									&& particleLabels[z][arrayIndex] > 1) {
								int minTag = particleLabels[z][arrayIndex];
								// Find the minimum particleLabel in the
								// neighbours' pixels
								for (int vZ = z - 1; vZ <= z + 1; vZ++) {
									for (int vY = y - 1; vY <= y + 1; vY++) {
										for (int vX = x - 1; vX <= x + 1; vX++) {
											if (withinBounds(vX, vY, vZ, w, h,
													sR2, sR3)) {
												final int offset = getOffset(
														vX, vY, w);
												if (workArray[vZ][offset] == phase) {
													final int tagv = particleLabels[vZ][offset];
													if (tagv != 0
															&& tagv < minTag) {
														minTag = tagv;
													}
												}
											}
										}
									}
								}
								// Replacing particleLabel by the minimum
								// particleLabel found
								for (int vZ = z - 1; vZ <= z + 1; vZ++) {
									for (int vY = y - 1; vY <= y + 1; vY++) {
										for (int vX = x - 1; vX <= x + 1; vX++) {
											if (withinBounds(vX, vY, vZ, w, h,
													sR2, sR3)) {
												final int offset = getOffset(
														vX, vY, w);
												if (workArray[vZ][offset] == phase) {
													final int tagv = particleLabels[vZ][offset];
													if (tagv != 0
															&& tagv != minTag) {
														replaceLabel(
																particleLabels,
																tagv, minTag,
																sR2, sR3);
													}
												}
											}
										}
									}
								}
							}
						}
					}
					IJ.showStatus("Connecting foreground structures"
							+ chunkString);
					IJ.showProgress(z, d);
				}
			} else if (phase == BACK) {
				for (int z = sR0; z < sR1; z++) {
					for (int y = 0; y < h; y++) {
						final int rowIndex = y * w;
						for (int x = 0; x < w; x++) {
							final int arrayIndex = rowIndex + x;
							if (workArray[z][arrayIndex] == phase) {
								int minTag = particleLabels[z][arrayIndex];
								// Find the minimum particleLabel in the
								// neighbours' pixels
								int nX = x, nY = y, nZ = z;
								for (int n = 0; n < 7; n++) {
									switch (n) {
									case 0:
										break;
									case 1:
										nX = x - 1;
										break;
									case 2:
										nX = x + 1;
										break;
									case 3:
										nY = y - 1;
										nX = x;
										break;
									case 4:
										nY = y + 1;
										break;
									case 5:
										nZ = z - 1;
										nY = y;
										break;
									case 6:
										nZ = z + 1;
										break;
									}
									if (withinBounds(nX, nY, nZ, w, h, sR2, sR3)) {
										final int offset = getOffset(nX, nY, w);
										if (workArray[nZ][offset] == phase) {
											final int tagv = particleLabels[nZ][offset];
											if (tagv != 0 && tagv < minTag) {
												minTag = tagv;
											}
										}
									}
								}
								// Replacing particleLabel by the minimum
								// particleLabel found
								for (int n = 0; n < 7; n++) {
									switch (n) {
									case 0:
										nZ = z;
										break; // last switch block left nZ = z
									// + 1;
									case 1:
										nX = x - 1;
										break;
									case 2:
										nX = x + 1;
										break;
									case 3:
										nY = y - 1;
										nX = x;
										break;
									case 4:
										nY = y + 1;
										break;
									case 5:
										nZ = z - 1;
										nY = y;
										break;
									case 6:
										nZ = z + 1;
										break;
									}
									if (withinBounds(nX, nY, nZ, w, h, sR2, sR3)) {
										final int offset = getOffset(nX, nY, w);
										if (workArray[nZ][offset] == phase) {
											final int tagv = particleLabels[nZ][offset];
											if (tagv != 0 && tagv != minTag) {
												replaceLabel(particleLabels,
														tagv, minTag, sR2, sR3);
											}
										}
									}
								}
							}
						}
					}
					IJ.showStatus("Connecting background structures"
							+ chunkString);
					IJ.showProgress(z, d + 1);
				}
			}
		}
		return;
	}
	
	/**
	 * Joins semi-labelled particles using a non-recursive algorithm
	 * 
	 * @param imp
	 * @param particleLabels
	 */
	private static void joinStructures(ImagePlus imp, int[][] particleLabels, int phase, String sPhase) {
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getImageStackSize();
		long[] particleSizes = getParticleSizes(particleLabels, sPhase);
		final int nBlobs = particleSizes.length;
		ArrayList<ArrayList<short[]>> particleLists = getParticleLists(
				particleLabels, particleSizes, nBlobs, w, h, d);
		switch (phase) {
		case FORE: {
			for (int b = 1; b < nBlobs; b++) {
				IJ.showStatus("Joining substructures...");
				IJ.showProgress(b, nBlobs);
				if (particleLists.get(b).isEmpty()) {
					continue;
				}

				for (int l = 0; l < particleLists.get(b).size(); l++) {
					final short[] voxel = particleLists.get(b).get(l);
					final int x = voxel[0];
					final int y = voxel[1];
					final int z = voxel[2];
					// find any neighbours with bigger labels
					for (int zN = z - 1; zN <= z + 1; zN++) {
						for (int yN = y - 1; yN <= y + 1; yN++) {
							final int index = yN * w;
							for (int xN = x - 1; xN <= x + 1; xN++) {
								if (!withinBounds(xN, yN, zN, w, h, d))
									continue;
								final int iN = index + xN;
								int p = particleLabels[zN][iN];
								if (p > b) {
									joinBlobs(b, p, particleLabels,
											particleLists, w);
								}
							}
						}
					}
				}
			}
		}
		case BACK: {
			for (int b = 1; b < nBlobs; b++) {
				IJ.showStatus("Joining substructures...");
				IJ.showProgress(b, nBlobs);
				if (particleLists.get(b).isEmpty()) {
					continue;
				}
				for (int l = 0; l < particleLists.get(b).size(); l++) {
					final short[] voxel = particleLists.get(b).get(l);
					final int x = voxel[0];
					final int y = voxel[1];
					final int z = voxel[2];
					// find any neighbours with bigger labels
					int xN = x, yN = y, zN = z;
					for (int n = 1; n < 7; n++) {
						switch (n) {
						case 1:
							xN = x - 1;
							break;
						case 2:
							xN = x + 1;
							break;
						case 3:
							yN = y - 1;
							xN = x;
							break;
						case 4:
							yN = y + 1;
							break;
						case 5:
							zN = z - 1;
							yN = y;
							break;
						case 6:
							zN = z + 1;
							break;
						}
						if (!withinBounds(xN, yN, zN, w, h, d))
							continue;
						final int iN = yN * w + xN;
						int p = particleLabels[zN][iN];
						if (p > b) {
							joinBlobs(b, p, particleLabels, particleLists, w);
						}
					}
				}
			}
		}
		}
		return;
	}
	
	/**
	 * Check to see if the pixel at (m,n,o) is within the bounds of the current
	 * stack
	 * 
	 * @param m
	 *            x co-ordinate
	 * @param n
	 *            y co-ordinate
	 * @param o
	 *            z co-ordinate
	 * @param startZ
	 *            first Z coordinate to use
	 * 
	 * @param endZ
	 *            last Z coordinate to use
	 * 
	 * @return True if the pixel is within the bounds of the current stack
	 */
	private static boolean withinBounds(int m, int n, int o, int w, int h, int startZ,
			int endZ) {
		return (m >= 0 && m < w && n >= 0 && n < h && o >= startZ && o < endZ);
	}

	private static boolean withinBounds(int m, int n, int o, int w, int h, int d) {
		return (m >= 0 && m < w && n >= 0 && n < h && o >= 0 && o < d);
	}

	/**
	 * Find the offset within a 1D array given 2 (x, y) offset values
	 * 
	 * @param m
	 *            x difference
	 * @param n
	 *            y difference
	 * 
	 * @return Integer offset for looking up pixel in work array
	 */
	private static int getOffset(int m, int n, int w) {
		return m + n * w;
	}
	
	/**
	 * Check whole array replacing m with n
	 * 
	 * @param m
	 *            value to be replaced
	 * @param n
	 *            new value
	 * @param startZ
	 *            first z coordinate to check
	 * @param endZ
	 *            last+1 z coordinate to check
	 */
	public static void replaceLabel(int[][] particleLabels, final int m, int n,
			int startZ, final int endZ) {
		final int s = particleLabels[0].length;
		for (int z = startZ; z < endZ; z++) {
			for (int i = 0; i < s; i++)
				if (particleLabels[z][i] == m) {
					particleLabels[z][i] = n;
				}
		}
	}
	
	/**
	 * Join particle p to particle b, relabelling p with b.
	 * 
	 * @param b
	 * @param p
	 * @param particleLabels
	 *            array of particle labels
	 * @param particleLists
	 *            list of particle voxel coordinates
	 * @param w
	 *            stack width
	 */
	public static void joinBlobs(int b, int p, int[][] particleLabels,
			ArrayList<ArrayList<short[]>> particleLists, int w) {
		ListIterator<short[]> iterB = particleLists.get(p).listIterator();
		while (iterB.hasNext()) {
			short[] voxelB = iterB.next();
			particleLists.get(b).add(voxelB);
			final int iB = voxelB[1] * w + voxelB[0];
			particleLabels[voxelB[2]][iB] = b;
		}
		particleLists.get(p).clear();
	}
	
	public static ArrayList<ArrayList<short[]>> getParticleLists(
			int[][] particleLabels, long[] particleSizes, int nBlobs, int w, int h, int d) {
		ArrayList<ArrayList<short[]>> pL = new ArrayList<ArrayList<short[]>>(
				nBlobs);
		ArrayList<short[]> background = new ArrayList<short[]>(0);
		pL.add(0, background);
		for (int b = 1; b < nBlobs; b++) {
			ArrayList<short[]> a = new ArrayList<short[]>(
					(int) particleSizes[b]);
			pL.add(b, a);
		}
		// add all the particle coordinates to the appropriate list
		for (short z = 0; z < d; z++) {
			IJ.showStatus("Listing substructures...");
			IJ.showProgress(z, d);
			for (short y = 0; y < h; y++) {
				final int i = y * w;
				for (short x = 0; x < w; x++) {
					final int p = particleLabels[z][i + x];
					if (p > 0) { // ignore background
						final short[] voxel = { x, y, z };
						pL.get(p).add(voxel);
					}
				}
			}
		}
		return pL;
	}
	
	/**
	 * Get the sizes of all the particles as a voxel count
	 * 
	 * @param particleLabels
	 * @return particleSizes
	 */
	public static long[] getParticleSizes(final int[][] particleLabels, String sPhase) {
		IJ.showStatus("Getting " + sPhase + " particle sizes");
		final int d = particleLabels.length;
		final int wh = particleLabels[0].length;
		// find the highest value particleLabel
		int maxParticle = 0;
		for (int z = 0; z < d; z++) {
			for (int i = 0; i < wh; i++) {
				maxParticle = Math.max(maxParticle, particleLabels[z][i]);
			}
		}

		long[] particleSizes = new long[maxParticle + 1];
		for (int z = 0; z < d; z++) {
			for (int i = 0; i < wh; i++) {
				particleSizes[particleLabels[z][i]]++;
			}
			IJ.showProgress(z, d);
		}
		return particleSizes;
	}
	
	/**
	 * Gets rid of redundant particle labels
	 * 
	 * @param particleLabels
	 * @return
	 */
	private static void minimiseLabels(int[][] particleLabels, String sPhase) {
		IJ.showStatus("Minimising labels...");
		final int d = particleLabels.length;
		long[] particleSizes = getParticleSizes(particleLabels, sPhase);
		final int nLabels = particleSizes.length;
		int[] newLabel = new int[nLabels];
		int minLabel = 0;
		// find the minimised labels
		for (int i = 0; i < nLabels; i++) {
			if (particleSizes[i] > 0) {
				if (i == minLabel) {
					newLabel[i] = i;
					minLabel++;
					continue;
				} else {
					newLabel[i] = minLabel;
					particleSizes[minLabel] = particleSizes[i];
					particleSizes[i] = 0;
					minLabel++;
				}
			}
		}
		// now replace labels
		final int wh = particleLabels[0].length;
		for (int z = 0; z < d; z++) {
			IJ.showProgress(z, d);
			for (int i = 0; i < wh; i++) {
				final int p = particleLabels[z][i];
				if (p > 0) {
					particleLabels[z][i] = newLabel[p];
				}
			}
		}
		return;
	}
	
	/**
	 * Get the centroids of all the particles in real units
	 * 
	 * @param imp
	 * @param particleLabels
	 * @param particleSizes
	 * @return double[][] containing all the particles' centroids
	 */
	private static double[][] getCentroids(ImagePlus imp, int[][] particleLabels,
			long[] particleSizes) {
		final int nParticles = particleSizes.length;
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getImageStackSize();
		double[][] sums = new double[nParticles][3];
		for (int z = 0; z < d; z++) {
			for (int y = 0; y < h; y++) {
				final int index = y * w;
				for (int x = 0; x < w; x++) {
					final int particle = particleLabels[z][index + x];
					sums[particle][0] += x;
					sums[particle][1] += y;
					sums[particle][2] += z;
				}
			}
		}
		Calibration cal = imp.getCalibration();
		double[][] centroids = new double[nParticles][3];
		for (int p = 0; p < nParticles; p++) {
			centroids[p][0] = cal.pixelWidth * sums[p][0] / particleSizes[p];
			centroids[p][1] = cal.pixelHeight * sums[p][1] / particleSizes[p];
			centroids[p][2] = cal.pixelDepth * sums[p][2] / particleSizes[p];
		}
		return centroids;
	}
	
	/**
	 * Get the minimum and maximum x, y and z coordinates of each particle
	 * 
	 * @param imp
	 *            ImagePlus (used for stack size)
	 * @param particleLabels
	 *            work array containing labelled particles
	 * @param nParticles
	 *            number of particles in the stack
	 * @return int[][] containing x, y and z minima and maxima.
	 */
	private static int[][] getParticleLimits(ImagePlus imp, int[][] particleLabels,
			int nParticles) {
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getImageStackSize();
		int[][] limits = new int[nParticles][6];
		for (int i = 0; i < nParticles; i++) {
			limits[i][0] = Integer.MAX_VALUE; // x min
			limits[i][1] = 0; // x max
			limits[i][2] = Integer.MAX_VALUE; // y min
			limits[i][3] = 0; // y max
			limits[i][4] = Integer.MAX_VALUE; // z min
			limits[i][5] = 0; // z max
		}
		for (int z = 0; z < d; z++) {
			for (int y = 0; y < h; y++) {
				final int index = y * w;
				for (int x = 0; x < w; x++) {
					final int i = particleLabels[z][index + x];
					limits[i][0] = Math.min(limits[i][0], x);
					limits[i][1] = Math.max(limits[i][1], x);
					limits[i][2] = Math.min(limits[i][2], y);
					limits[i][3] = Math.max(limits[i][3], y);
					limits[i][4] = Math.min(limits[i][4], z);
					limits[i][5] = Math.max(limits[i][5], z);
				}
			}
		}
		return limits;
	}
	
	/**
	 * Scans edge voxels and set all touching particles to background
	 * 
	 * @param particleLabels
	 * @param nLabels
	 * @param w
	 * @param h
	 * @param d
	 */
	private static List<List<Face>> detectEdgesTouched(ImagePlus imp, int[][] particleLabels,
			byte[][] workArray, int nLabels) {
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getImageStackSize();
		List<List<Face>> newLabel = new LinkedList<List<Face>>();
		
		for (int i = 0; i < nLabels; i++) {
			List<Face> temp = new ArrayList<Face>();
			temp.add(Face.NONE);
			newLabel.add(temp);
		}

		// scan faces
		// top and bottom faces
		for (int y = 0; y < h; y++) {
			final int index = y * w;
			for (int x = 0; x < w; x++) {
				final int pt = particleLabels[0][index + x];
				if (pt > 0) {
					if (newLabel.get(pt).get(0) == Face.NONE) {
						newLabel.get(pt).remove(0);
						newLabel.get(pt).add(Face.TOP);
					} else if (!newLabel.get(pt).contains(Face.TOP)) {
						newLabel.get(pt).add(Face.TOP);
					}
				}
				final int pb = particleLabels[d - 1][index + x];
				if (pb > 0)
					if (newLabel.get(pb).get(0) == Face.NONE) {
						newLabel.get(pb).remove(0);
						newLabel.get(pb).add(Face.BOTTOM);
					} else if (!newLabel.get(pb).contains(Face.BOTTOM)) {
						newLabel.get(pb).add(Face.BOTTOM);
					}
			}
		}

		// west and east faces
		for (int z = 0; z < d; z++) {
			for (int y = 0; y < h; y++) {
				final int pw = particleLabels[z][y * w];
				final int pe = particleLabels[z][y * w + w - 1];
				if (pw > 0)
					if (newLabel.get(pw).get(0) == Face.NONE) {
						newLabel.get(pw).remove(0);
						newLabel.get(pw).add(Face.WEST);
					} else if (!newLabel.get(pw).contains(Face.WEST)) {
						newLabel.get(pw).add(Face.WEST);
					}
				if (pe > 0)
					if (newLabel.get(pe).get(0) == Face.NONE) {
						newLabel.get(pe).remove(0);
						newLabel.get(pe).add(Face.EAST);
					} else if (!newLabel.get(pe).contains(Face.EAST)) {
						newLabel.get(pe).add(Face.EAST);
					}
			}
		}

		// north and south faces
		final int lastRow = w * (h - 1);
		for (int z = 0; z < d; z++) {
			for (int x = 0; x < w; x++) {
				final int pn = particleLabels[z][x];
				final int ps = particleLabels[z][lastRow + x];
				if (pn > 0)
					if (newLabel.get(pn).get(0) == Face.NONE) {
						newLabel.get(pn).remove(0);
						newLabel.get(pn).add(Face.NORTH);
					} else if (!newLabel.get(pn).contains(Face.NORTH)) {
						newLabel.get(pn).add(Face.NORTH);
					}
				if (ps > 0)
					if (newLabel.get(ps).get(0) == Face.NONE) {
						newLabel.get(ps).remove(0);
						newLabel.get(ps).add(Face.SOUTH);
					} else if (!newLabel.get(ps).contains(Face.SOUTH)) {
						newLabel.get(ps).add(Face.SOUTH);
					}
			}
		}

		return newLabel;
	}

	/**
	 * Get the maximum distances from the centroid in x, y, and z axes, and
	 * transformed x, y and z axes
	 * 
	 * @param imp
	 * @param particleLabels
	 * @param centroids
	 * @param E
	 * @return array containing two nPoints * 3 arrays with max and max
	 *         transformed distances respectively
	 * 
	 */
	public static double[][] getMaxDistances(ImagePlus imp,
			int[][] particleLabels, List<Particle> particles) {
		Calibration cal = imp.getCalibration();
		final double vW = cal.pixelWidth;
		final double vH = cal.pixelHeight;
		final double vD = cal.pixelDepth;
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getImageStackSize();
		final int nParticles = particles.size();
		double[][] maxD = new double[nParticles][3];
		double[][] maxDt = new double[nParticles][3];
		for (int z = 0; z < d; z++) {
			for (int y = 0; y < h; y++) {
				final int index = y * w;
				for (int x = 0; x < w; x++) {
					final int p = particleLabels[z][index + x];
					if (p > 0) {
						final double dX = x * vW
								- particles.get(p).getCentroid()[0];
						final double dY = y * vH
								- particles.get(p).getCentroid()[1];
						final double dZ = z * vD
								- particles.get(p).getCentroid()[2];
						maxD[p][0] = Math.max(maxD[p][0], Math.abs(dX));
						maxD[p][1] = Math.max(maxD[p][1], Math.abs(dY));
						maxD[p][2] = Math.max(maxD[p][2], Math.abs(dZ));
						final double[][] eV = particles.get(p).getEigen()
								.getV().getArray();
						final double dXt = dX * eV[0][0] + dY * eV[0][1] + dZ
								* eV[0][2];
						final double dYt = dX * eV[1][0] + dY * eV[1][1] + dZ
								* eV[1][2];
						final double dZt = dX * eV[2][0] + dY * eV[2][1] + dZ
								* eV[2][2];
						maxDt[p][0] = Math.max(maxDt[p][0], Math.abs(dXt));
						maxDt[p][1] = Math.max(maxDt[p][1], Math.abs(dYt));
						maxDt[p][2] = Math.max(maxDt[p][2], Math.abs(dZt));
					}
				}
			}
		}
		for (int p = 0; p < nParticles; p++) {
			Arrays.sort(maxDt[p]);
			double[] temp = new double[3];
			for (int i = 0; i < 3; i++) {
				temp[i] = maxDt[p][2 - i];
			}
			maxDt[p] = temp.clone();
		}
		final Object[] maxDistances = { maxD, maxDt };
		return (double[][]) maxDistances[1];
	}

	/**
	 * Display the particle labels as an ImagePlus
	 * 
	 * @param particleLabels
	 * @param imp
	 *            original image, used for image dimensions, calibration and
	 *            titles
	 */
	public static ImagePlus displayParticleLabels(int[][] particleLabels,
			ImagePlus imp) {
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getImageStackSize();
		final int wh = w * h;
		ImageStack stack = new ImageStack(w, h);
		double max = 0;
		for (int z = 0; z < d; z++) {
			float[] slicePixels = new float[wh];
			for (int i = 0; i < wh; i++) {
				slicePixels[i] = (float) particleLabels[z][i];
				max = Math.max(max, slicePixels[i]);
			}
			stack.addSlice(imp.getImageStack().getSliceLabel(z + 1),
					slicePixels);
		}
		ImagePlus impParticles = new ImagePlus(imp.getShortTitle() + "_parts",
				stack);
		impParticles.setCalibration(imp.getCalibration());
		impParticles.getProcessor().setMinAndMax(0, max);
		return impParticles;
	}
}
