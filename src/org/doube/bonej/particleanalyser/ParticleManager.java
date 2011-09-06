package org.doube.bonej.particleanalyser;

import ij.measure.Calibration;

import java.util.List;

import org.doube.bonej.particleanalyser.impl.Face;

public interface ParticleManager {

	public static enum ColorMode {
		GRADIENT, SPLIT;
	}
	
	/**
	 * Closes the Particle manager and ends particle analysis.
	 */
	public void close();
	
	/**
	 * Determines whether the ParticleManager has been closed.
	 * @return
	 */
	public boolean isClosed();

	/**
	 * Gets the calibration of the original image and thereby the particles.
	 * @return image calibration
	 */
	public Calibration getCalibration();

	/**
	 * Returns the particle at the given index.
	 * @param index
	 * @return particle
	 */
	public Particle getParticle(int index);

	/**
	 * Returns visible partice at a given index.
	 * @param index
	 * @return
	 */
	public Particle getVisibleParticle(int index);

	/**
	 * Returns a list of all particles.
	 * @return the particles
	 */
	public List<Particle> getAllParticles();

	/**
	 * Returns a list of all visible Particles.
	 * @return visible particles.
	 */
	public List<Particle> getVisibleParticles();

	/**
	 * Hides the particle at the given index using a certain <enum>Particle.HideType</enum>
	 * @param index
	 * @param hiddenBy
	 */
	public void hideParticle(int index, Particle.HideType hiddenBy);

	/**
	 * Hides a given particle using a certain <enum>Particle.HideType</enum>
	 * @param particle
	 * @param hiddenBy
	 */
	public void hideParticle(Particle particle, Particle.HideType hiddenBy);

	/**
	 * Hides consecutive particles between start and end indexes by a certain <enum>Particle.HideType</enum>
	 * @param startIndex
	 * @param endIndex
	 * @param hiddenBy
	 */
	public void hideParticles(int startIndex, int endIndex,
			Particle.HideType hiddenBy);

	/**
	 * Resets all particles. Shows all hidden particles.
	 */
	public void resetParticles();

	/**
	 * Selects the given particle.
	 * @param particle
	 */
	public void selectParticle(Particle particle);

	/**
	 * Deselects all selected particles.
	 */
	public void deselectAllParticles();

	/**
	 * Handles the inclusion/exclusion of particles that touch a given side or <enum>Face</enum> of the volume.
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
	 * Display the original binary stack in the 3D Viewer. 
	 */
	public void displayOriginal3DImage();

}