package org.doube.bonej.particleanalyser;

import ij.measure.Calibration;

import java.util.List;

import org.doube.bonej.particleanalyser.impl.Face;

public interface ParticleManager {

	public static enum ColorMode {
		GRADIENT, SPLIT;
	}
	
	public void close();

	public Calibration getCalibration();

	public boolean isClosed();

	public Particle getParticle(int index);

	public Particle getVisibleParticle(int index);

	/**
	 * @return the particles
	 */
	public List<Particle> getAllParticles();

	public List<Particle> getVisibleParticles();

	public void hideParticle(int index, Particle.HideType hiddenBy);

	public void hideParticle(Particle particle, Particle.HideType hiddenBy);

	public void hideParticles(int startIndex, int endIndex,
			Particle.HideType hiddenBy);

	public void resetParticles();

	public void selectParticle(Particle particle);

	public void deselectAllParticles();

	/**
	 * Handles the inclusion/exclusion of particles that touch a given side.
	 * TODO: need to handle particles hidden by other means i.e.
	 * Particle.HideTypes.DELETE and SIZE
	 * 
	 * @param show
	 * @param edge
	 */
	public void excludeOnEdge(boolean show, Face edge);

	/**
	 * @return the maxVolume
	 */
	public double getMaxVolume();

	/**
	 * @param newMaxVolume
	 *            the maxVolume to set
	 */
	public void setMaxVolume(double newMaxVolume);

	/**
	 * @return the minVolume
	 */
	public double getMinVolume();

	/**
	 * @param minVolume
	 *            the minVolume to set
	 */
	public void setMinVolume(double minVolume);

	/**
	 * Display visible particle surfaces in the 3D Viewer.
	 */
	public void displaySurfaces(ParticleManager.ColorMode mode);

	/**
	 * Display visible particle centroids in the 3D Viewer.
	 */
	public void displayCentroids();

	/**
	 * Display visible particle axes in the 3D Viewer.
	 */
	public void displayAxes();

	/**
	 * Display visible particle ellipsoids in the 3D Viewer.
	 */
	public void displayEllipsoids();

	/**
	 * Display the original binary stack in the 3D Viewer. TODO: Add option for
	 * setting re-sampling.
	 */
	public void displayOriginal3DImage();

}