/**
 * ParticleCounterUtil.java Copyright 2010 Keith Schulze
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
package org.doube.bonej.particleanalyser.impl;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import org.doube.bonej.MeasureSurface;
import org.doube.geometry.FitEllipsoid;

import customnode.CustomTriangleMesh;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;

/**
 * @author Keith Schulze Set of static utilities for the Particle Counter
 */
public class ParticleUtilities {

	/**
	 * Remove particles outside user-specified volume thresholds
	 * 
	 * @param imp
	 *            ImagePlus, used for calibration
	 * @param workArray
	 *            binary foreground and background information
	 * @param particleLabels
	 *            Packed 3D array of particle labels
	 * @param minVol
	 *            minimum (inclusive) particle volume
	 * @param maxVol
	 *            maximum (inclusive) particle volume
	 * @param phase
	 *            phase we are interested in
	 * @param sPhase
	 *            String describing the phase we're in - foreground or
	 *            background.
	 */
	@Deprecated
	public static void filterParticles(ImagePlus imp, byte[][] workArray,
			int[][] particleLabels, double minVol, double maxVol, int phase,
			String sPhase) {
		if (minVol == 0 && maxVol == Double.POSITIVE_INFINITY)
			return;
		final int d = imp.getImageStackSize();
		final int wh = workArray[0].length;
		long[] particleSizes = ParticleGetter.getParticleSizes(particleLabels,
				sPhase);
		double[] particleVolumes = getVolumes(imp, particleSizes);
		byte flip = 0;
		if (phase == 1) {
			flip = (byte) 0;
		} else {
			flip = (byte) 255;
		}
		for (int z = 0; z < d; z++) {
			for (int i = 0; i < wh; i++) {
				final int p = particleLabels[z][i];
				final double v = particleVolumes[p];
				if (v < minVol || v > maxVol) {
					workArray[z][i] = flip;
					particleLabels[z][i] = 0;
				}
			}
		}
	}

	@Deprecated
	private static double[] getVolumes(ImagePlus imp, long[] particleSizes) {
		Calibration cal = imp.getCalibration();
		final double voxelVolume = cal.pixelWidth * cal.pixelHeight
				* cal.pixelDepth;
		final int nLabels = particleSizes.length;
		double[] particleVolumes = new double[nLabels];
		for (int i = 0; i < nLabels; i++) {
			particleVolumes[i] = voxelVolume * particleSizes[i];
		}
		return particleVolumes;
	}

	/**
	 * Get the mean and standard deviation of pixel values above a minimum value
	 * for each particle in a particle label work array
	 * 
	 * @param imp
	 *            Input image containing pixel values
	 * @param particleLabels
	 *            workArray containing particle labels
	 * @param particleSizes
	 *            array of particle sizes as pixel counts
	 * @param threshold
	 *            restrict calculation to values > i
	 * @return array containing mean, std dev and max pixel values for each
	 *         particle
	 */
	public static double[][] getMeanStdDev(ImagePlus imp,
			int[][] particleLabels, List<ParticleImpl> particles,
			final int threshold) {
		final int nParticles = particles.size();
		final int d = imp.getImageStackSize();
		final int wh = imp.getWidth() * imp.getHeight();
		ImageStack stack = imp.getImageStack();
		double[] sums = new double[nParticles];
		for (int z = 0; z < d; z++) {
			float[] pixels = (float[]) stack.getPixels(z + 1);
			int[] labelPixels = particleLabels[z];
			for (int i = 0; i < wh; i++) {
				final double value = pixels[i];
				if (value > threshold) {
					sums[labelPixels[i]] += value;
				}
			}
		}
		double[][] meanStdDev = new double[nParticles][3];
		for (int p = 1; p < nParticles; p++) {
			meanStdDev[p][0] = sums[p] / particles.get(p).getParticleSize();
		}

		double[] sumSquares = new double[nParticles];
		for (int z = 0; z < d; z++) {
			float[] pixels = (float[]) stack.getPixels(z + 1);
			int[] labelPixels = particleLabels[z];
			for (int i = 0; i < wh; i++) {
				final double value = pixels[i];
				if (value > threshold) {
					final int p = labelPixels[i];
					final double residual = value - meanStdDev[p][0];
					sumSquares[p] += residual * residual;
					meanStdDev[p][2] = Math.max(meanStdDev[p][2], value);
				}
			}
		}
		for (int p = 1; p < nParticles; p++) {
			meanStdDev[p][1] = Math.sqrt(sumSquares[p]
					/ particles.get(p).getParticleSize());
		}
		return meanStdDev;
	}

	/**
	 * Static method for calculation the surface area using a particles surface
	 * points.
	 * 
	 * @param surfacePoints
	 * @return
	 */
	public static void calculateSurfaceArea(ParticleImpl particle) {
		List<Point3f> surfacePoints = particle.getSurfacePoints();
		if (surfacePoints != null) {
			particle.setSurfaceArea(MeasureSurface
					.getSurfaceArea(surfacePoints));
		} else {
			throw new NullPointerException("Surface Points were null");
		}
	}

	/**
	 * Static method for calculation the enclosed volume using a particles
	 * surface points.
	 * 
	 * @param surfacePoints
	 * @return
	 */
	public static void calculateEnclosedVolume(ParticleImpl particle) {
		List<Point3f> surfacePoints = particle.getSurfacePoints();
		final Color3f colour = new Color3f(0.0f, 0.0f, 0.0f);
		double surfaceVolume = 0;
		if (null != surfacePoints) {
			CustomTriangleMesh surface = new CustomTriangleMesh(surfacePoints,
					colour, 0.0f);
			surfaceVolume = Math.abs(surface.getVolume());
			particle.setEnclosedVolume(surfaceVolume);
		} else {
			throw new NullPointerException("Surface Points were null");
		}
	}

	/**
	 * Get the Feret diameter of a surface using the particles surface points.
	 * Uses an inefficient brute-force algorithm.
	 * 
	 * @param surfacePoints
	 * @return
	 */
	public static void calculateFeretDiameter(ParticleImpl particle) {
		List<Point3f> surfacePoints = particle.getSurfacePoints();
		double feret = Double.NaN;
		if (surfacePoints != null) {
			Point3f a;
			Point3f b;
			ListIterator<Point3f> ita = surfacePoints.listIterator();
			ListIterator<Point3f> itb;

			while (ita.hasNext()) {
				a = ita.next();
				itb = surfacePoints.listIterator(ita.nextIndex());
				while (itb.hasNext()) {
					b = itb.next();
					feret = Math.max(feret, a.distance(b));
				}
			}
			particle.setFeretDiameter(feret);
		} else {
			throw new NullPointerException("Surface Points were null");
		}
	}

	public static void generateEllipsoid(ParticleImpl particle) {
		List<Point3f> surfacePoints = particle.getSurfacePoints();
		if (surfacePoints == null) {
			return;
		}

		Iterator<Point3f> pointIter = surfacePoints.iterator();
		double[][] coOrdinates = new double[surfacePoints.size()][3];
		int i = 0;
		while (pointIter.hasNext()) {
			Point3f point = pointIter.next();
			coOrdinates[i][0] = point.x;
			coOrdinates[i][1] = point.y;
			coOrdinates[i][2] = point.z;
			i++;
		}
		try {
			particle.setEllipsoid(FitEllipsoid.yuryPetrov(coOrdinates));
		} catch (RuntimeException re) {
			IJ.log("Could not fit ellipsoid to surface " + particle.getName());
			return;
		}
	}

	protected static ImagePlus getBinaryParticle(ParticleImpl p, ImagePlus imp,
			int[][] particleLabels, int padding) {

		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getImageStackSize();
		final int xMin = Math.max(0, p.getLimits()[0] - padding);
		final int xMax = Math.min(w - 1, p.getLimits()[1] + padding);
		final int yMin = Math.max(0, p.getLimits()[2] - padding);
		final int yMax = Math.min(h - 1, p.getLimits()[3] + padding);
		final int zMin = Math.max(0, p.getLimits()[4] - padding);
		final int zMax = Math.min(d - 1, p.getLimits()[5] + padding);
		final int stackWidth = xMax - xMin + 1;
		final int stackHeight = yMax - yMin + 1;
		final int stackSize = stackWidth * stackHeight;
		ImageStack stack = new ImageStack(stackWidth, stackHeight);
		for (int z = zMin; z <= zMax; z++) {
			byte[] slice = new byte[stackSize];
			int i = 0;
			for (int y = yMin; y <= yMax; y++) {
				final int sourceIndex = y * w;
				for (int x = xMin; x <= xMax; x++) {
					if (particleLabels[z][sourceIndex + x] == p.getID()) {
						slice[i] = (byte) (255 & 0xFF);
					}
					i++;
				}
			}
			stack.addSlice(imp.getStack().getSliceLabel(z + 1), slice);
		}
		ImagePlus binaryImp = new ImagePlus("Particle_" + p, stack);
		Calibration cal = imp.getCalibration();
		binaryImp.setCalibration(cal);
		return binaryImp;
	}

	protected static int getNCavities(ImagePlus imp) {
		Object[] result = ParticleGetter.getParticles(imp, 4,
				ParticleGetter.BACK);
		long[] particleSizes = (long[]) result[2];
		final int nParticles = particleSizes.length;
		final int nCavities = nParticles - 2; // 1 particle is the background
		return nCavities;
	}

	public static Color3f getGradientColor(int particleIndex,
			int particlePopulationSize) {
		float red = 1.0f - (float) particleIndex
				/ (float) particlePopulationSize;
		float green = 1.0f - red;
		float blue = (float) particleIndex
				/ (2.0f * (float) particlePopulationSize);
		return new Color3f(red, green, blue);
	}
}
