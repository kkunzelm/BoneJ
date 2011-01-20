/**
 * ParticleManager.java Copyright 2010 Keith Schulze
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

import java.awt.EventQueue;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import marchingcubes.MCTriangulator;

import org.doube.bonej.Connectivity;
import org.doube.bonej.Thickness;
import org.doube.bonej.particleanalyser.ui.PAResultWindow;
import org.doube.jama.EigenvalueDecomposition;
import org.doube.jama.Matrix;

import customnode.CustomPointMesh;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij3d.Content;
import ij3d.ContentCreator;
import ij3d.Image3DUniverse;
import ij3d.ImageWindow3D;

/**
 * @author Keith Schulze
 * @author Michael Doube
 * 
 */
public class ParticleManager {

	public static enum ColorMode {
		GRADIENT, SPLIT;
	}

	private static ArrayList<ParticleManager> particleManagers = new ArrayList<ParticleManager>();
	private Image3DUniverse univ = new Image3DUniverse();

	private ImagePlus imp;
	private int[][] particleLabels;
	private byte[][] particleWorkArray;
	private List<ParticleImpl> particles;
	
	private boolean closed = false;

	private boolean surfacePointsCalculated = false;
	private boolean eigensCalculated = false;
	private boolean surfaceAreaCalculated = false;
	private boolean enclosedVolumeCalculated = false;
	private boolean feretDiametersCalculated = false;
	private boolean eulerCharactersCalculated = false;
	private boolean thicknessCalculated = false;
	private boolean ellipsoidsCalculated = false;

	private double splitValue = 0;

	private PAResultWindow resultWindow;

	public static double DEFAULT_MAX_VOLUME = Double.POSITIVE_INFINITY;
	public static double DEFAULT_MIN_VOLUME = 0.0;
	private double maxVolume = DEFAULT_MAX_VOLUME;
	private double minVolume = DEFAULT_MIN_VOLUME;

	/**
	 * @param imp
	 * @param particleLabels
	 * @param particles
	 */
	public ParticleManager(ImagePlus imp, int[][] particleLabels,
			byte[][] particleWorkArray, List<ParticleImpl> particles) {
		super();
		this.imp = imp;
		this.particleLabels = particleLabels;
		this.particleWorkArray = particleWorkArray;
		this.particles = particles;

		hideZero(this.particles);
	}

	/**
	 * Display Results
	 */
	/**
	 * ------------------------------------------------------------------------
	 * ---------------
	 */

	/**
	 * @param imp
	 * @param particleLabels
	 * @param particleWorkArray
	 * @param particles
	 * @param surfacePointsCalculated
	 * @param eigensCalculated
	 * @param surfaceAreaCalculated
	 * @param enclosedVolumeCalculated
	 * @param feretDiametersCalculated
	 * @param eulerCharactersCalculated
	 * @param thicknessCalculated
	 * @param ellipsoidsCalculated
	 */
	public ParticleManager(ImagePlus imp, int[][] particleLabels,
			byte[][] particleWorkArray, List<ParticleImpl> particles,
			boolean calculateEigens, boolean calculateSurfaceArea,
			boolean calculateEnclosedVolume, boolean calculateFeretDiameters,
			boolean calculateEulerCharacters, boolean calculateThickness,
			boolean calculateEllipsoids) {
		super();
		this.imp = imp;
		this.particleLabels = particleLabels;
		this.particleWorkArray = particleWorkArray;
		this.particles = particles;

		// TODO: Need to implement volume re-sampling selection.
		if (calculateSurfaceArea || calculateEnclosedVolume
				|| calculateEllipsoids || calculateFeretDiameters) {
			this.surfacePointsCalculated = setAllSurfacePoints(this.imp,
					this.particleLabels, this.particles, 2);
		}

		if (calculateEigens) {
			this.eigensCalculated = setAllEigens(this.imp, this.particleLabels,
					this.particles);
		}

		if (calculateSurfaceArea) {
			for (ParticleImpl p : this.particles) {
				IJ.showStatus("Calculating surface areas...");
				IJ.showProgress(p.getID(), this.particles.size());
				ParticleUtilities.calculateSurfaceArea(p);
			}

			this.surfaceAreaCalculated = true;
		}

		if (calculateEnclosedVolume) {
			for (ParticleImpl p : this.particles) {
				IJ.showStatus("Calculating enclosed volumes...");
				IJ.showProgress(p.getID(), this.particles.size());
				ParticleUtilities.calculateEnclosedVolume(p);
			}

			this.enclosedVolumeCalculated = true;
		}

		if (calculateFeretDiameters) {
			for (ParticleImpl p : this.particles)
				ParticleUtilities.calculateFeretDiameter(p);
			this.feretDiametersCalculated = true;
		}

		if (calculateEulerCharacters) {
			this.eulerCharactersCalculated = setAllEulerCharacters(this.imp,
					this.particleLabels, this.particles);
		}

		if (calculateThickness) {
			this.thicknessCalculated = setAllThicknesses(this.imp,
					this.particleLabels, this.particles);
		}

		if (calculateEllipsoids) {
			for (ParticleImpl p : this.particles)
				ParticleUtilities.generateEllipsoid(p);
		}

		hideZero(this.particles);
		particleManagers.add(this);
	}
	
	public void close() {
		for (int i = 0; i < getAllParticles().size(); i++)
			particles.remove(i);
		
		this.particles = null;
		this.imp.close();
		this.imp = null;
		this.particleLabels = null;
		this.particleWorkArray = null;
		this.resultWindow = null;
		particleManagers.remove(this);
		this.closed = true;
	}

	/**
	 * @return the imp
	 */
	public ImagePlus getImp() {
		return imp;
	}

	/**
	 * @param imp
	 *            the imp to set
	 */
	public void setImp(ImagePlus imp) {
		this.imp = imp;
	}
	
	public Calibration getCalibration() {
		return this.imp.getCalibration();
	}

	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	/**
	 * @return the particleLabels
	 */
	public int[][] getParticleLabels() {
		return particleLabels;
	}

	/**
	 * @param particleLabels
	 *            the particleLabels to set
	 */
	public void setParticleLabels(int[][] particleLabels) {
		this.particleLabels = particleLabels;
	}

	/**
	 * @return the particleWorkArray
	 */
	public byte[][] getParticleWorkArray() {
		return particleWorkArray;
	}

	/**
	 * @param particleWorkArray
	 *            the particleWorkArray to set
	 */
	public void setParticleWorkArray(byte[][] particleWorkArray) {
		this.particleWorkArray = particleWorkArray;
	}

	public ParticleImpl getParticle(int index) {
		return particles.get(index);
	}

	public ParticleImpl getVisibleParticle(int index) {
		return getVisibleParticles().get(index);
	}

	public void setParticle(ParticleImpl p, int index) {
		this.particles.set(index, p);
	}

	/**
	 * @return the particles
	 */
	public List<ParticleImpl> getAllParticles() {
		return particles;
	}

	/**
	 * @param particles
	 *            the particles to set
	 */
	public void setParticles(List<ParticleImpl> particles) {
		this.particles = particles;
	}

	public List<ParticleImpl> getVisibleParticles() {
		List<ParticleImpl> visibleParticles = new ArrayList<ParticleImpl>();
		for (ParticleImpl p : particles)
			if (p.isVisible())
				visibleParticles.add(p);

		return visibleParticles;
	}

	public void hideParticle(int index, ParticleImpl.HideType hiddenBy) {
		hideParticle(getParticle(index), hiddenBy);
	}

	public void hideParticle(ParticleImpl particle, ParticleImpl.HideType hiddenBy) {
		particle.setVisible(false);
		particle.setHiddenBy(hiddenBy);

		if (univ.getWindow() != null && univ.contains(particle.getName())) {
			univ.removeContent(particle.getName());
			this.reAdjustView();
		}

		resultWindow.getParticleTableModel().fireTableDataChanged();
	}

	public void hideParticles(int startIndex, int endIndex,
			ParticleImpl.HideType hiddenBy) {
		for (int i = startIndex; i <= endIndex; i++)
			hideParticle(i, hiddenBy);
	}

	public void showParticle(int index) {
		showParticle(getParticle(index));
	}

	public void showParticle(ParticleImpl particle) {
		particle.setVisible(true);

		if (univ.getWindow() != null && !univ.contains(particle.getName())) {
			Color3f pColor = ParticleUtilities.getGradientColor(
					particle.getID(), particles.size());
			Content pContent = BoneJParticleContentCreator.createContent(
					particle.getSurfaceMesh(pColor), particle, this);

			univ.addContent(pContent).setLocked(true);
			this.reAdjustView();
		}

		resultWindow.getParticleTableModel().fireTableDataChanged();
	}

	public void showAllParticles() {
		for (ParticleImpl p : particles) {
			if (!p.isVisible() && p.getID() > 0) {
				p.setVisible(true);

				if (univ.getWindow() != null && !univ.contains(p.getName())) {
					Color3f pColor = ParticleUtilities.getGradientColor(
							p.getID(), particles.size());
					Content pContent = BoneJParticleContentCreator
							.createContent(p.getSurfaceMesh(pColor), p, this);

					univ.addContent(pContent).setLocked(true);
					this.reAdjustView();
				}
			}
		}
		resultWindow.getParticleTableModel().fireTableDataChanged();
	}

	public void resetParticles() {
		if (univ.getWindow() == null) {
			univ = new Image3DUniverse();
		}

		this.showAllParticles();
	}

	public static List<ParticleImpl> createParticleBackup(List<ParticleImpl> particles) {
		List<ParticleImpl> temp = new ArrayList<ParticleImpl>(particles.size());
		for (ParticleImpl p : particles) {
			ParticleImpl pClone = null;
			try {
				pClone = (ParticleImpl) p.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			temp.add(pClone);
		}
		return temp;
	}

	public void createResultTable() {
		resultWindow = new PAResultWindow(this);
		EventQueue.invokeLater(resultWindow);
	}

	public void selectParticle(ParticleImpl particle) {
		if (univ.getWindow() != null && univ.getWindow().isShowing())
			if (univ.getSelected() != univ.getContent(particle.getName())) {
				univ.select(univ.getContent(particle.getName()));
			}

		if (!particle.isSelected())
			particle.setSelected(true);

		if (resultWindow.getSelectedParticle() != particle)
			resultWindow.selectParticle(particle);
	}

	public void deselectAllParticles() {
		for (ParticleImpl p : particles)
			p.setSelected(false);
	}

	/**
	 * @return the surfacePointsCalculated
	 */
	public boolean isSurfacePointsCalculated() {
		return surfacePointsCalculated;
	}

	/**
	 * @param surfacePointsCalculated
	 *            the surfacePointsCalculated to set
	 */
	public void setSurfacePointsCalculated(boolean surfacePointsCalculated) {
		this.surfacePointsCalculated = surfacePointsCalculated;
	}

	/**
	 * @return the eigensCalculated
	 */
	public boolean isEigensCalculated() {
		return eigensCalculated;
	}

	/**
	 * @param eigensCalculated
	 *            the eigensCalculated to set
	 */
	public void setEigensCalculated(boolean eigensCalculated) {
		this.eigensCalculated = eigensCalculated;
	}

	/**
	 * @return the surfaceAreaCalculated
	 */
	public boolean isSurfaceAreaCalculated() {
		return surfaceAreaCalculated;
	}

	/**
	 * @param surfaceAreaCalculated
	 *            the surfaceAreaCalculated to set
	 */
	public void setSurfaceAreaCalculated(boolean surfaceAreaCalculated) {
		this.surfaceAreaCalculated = surfaceAreaCalculated;
	}

	/**
	 * @return the enclosedVolumeCalculated
	 */
	public boolean isEnclosedVolumeCalculated() {
		return enclosedVolumeCalculated;
	}

	/**
	 * @param enclosedVolumeCalculated
	 *            the enclosedVolumeCalculated to set
	 */
	public void setEnclosedVolumeCalculated(boolean enclosedVolumeCalculated) {
		this.enclosedVolumeCalculated = enclosedVolumeCalculated;
	}

	/**
	 * @return the feretDiametersCalculated
	 */
	public boolean isFeretDiametersCalculated() {
		return feretDiametersCalculated;
	}

	/**
	 * @param feretDiametersCalculated
	 *            the feretDiametersCalculated to set
	 */
	public void setFeretDiametersCalculated(boolean feretDiametersCalculated) {
		this.feretDiametersCalculated = feretDiametersCalculated;
	}

	/**
	 * @return the eulerCharactersCalculated
	 */
	public boolean isEulerCharactersCalculated() {
		return eulerCharactersCalculated;
	}

	/**
	 * @param eulerCharactersCalculated
	 *            the eulerCharactersCalculated to set
	 */
	public void setEulerCharactersCalculated(boolean eulerCharactersCalculated) {
		this.eulerCharactersCalculated = eulerCharactersCalculated;
	}

	/**
	 * @return the thicknessCalculated
	 */
	public boolean isThicknessCalculated() {
		return thicknessCalculated;
	}

	/**
	 * @param thicknessCalculated
	 *            the thicknessCalculated to set
	 */
	public void setThicknessCalculated(boolean thicknessCalculated) {
		this.thicknessCalculated = thicknessCalculated;
	}

	/**
	 * @return the ellipsoidsCalculated
	 */
	public boolean isEllipsoidsCalculated() {
		return ellipsoidsCalculated;
	}

	/**
	 * @param ellipsoidsCalculated
	 *            the ellipsoidsCalculated to set
	 */
	public void setEllipsoidsCalculated(boolean ellipsoidsCalculated) {
		this.ellipsoidsCalculated = ellipsoidsCalculated;
	}

	/**
	 * Handles the inclusion/exclusion of particles that touch a given side.
	 * TODO: need to handle particles hidden by other means i.e.
	 * Particle.HideTypes.DELETE and SIZE
	 * 
	 * @param show
	 * @param edge
	 */
	public synchronized void excludeOnEdge(boolean show, Face edge) {
		for (ParticleImpl p : this.getAllParticles()) {
			if (p.getEdgesTouched().size() == 1 && p.isTouchingEdge(edge)) {
				if (show) {
					if (p.wasHiddenBy(ParticleImpl.HideType.DELETE)
							|| p.wasHiddenBy(ParticleImpl.HideType.SIZE)) {
						continue;
					} else {
						showParticle(p);
					}
				} else if (!show){
					hideParticle(p, ParticleImpl.HideType.FACE_TOUCHED);
				}
			} else if (p.getEdgesTouched().size() > 1 && p.isTouchingEdge(edge)) {
				boolean touchesAnotherExcludedFace = false;
				for (Face excludedFace : resultWindow
						.getExcludedEdges()) {
					if (p.isTouchingEdge(excludedFace))
						touchesAnotherExcludedFace = true;
				}

				if (!touchesAnotherExcludedFace && show) {
					if (p.wasHiddenBy(ParticleImpl.HideType.DELETE)
							|| p.wasHiddenBy(ParticleImpl.HideType.SIZE)) {
						continue;
					} else {
						showParticle(p);
					}
				} else if (!show) {
					hideParticle(p, ParticleImpl.HideType.FACE_TOUCHED);
				}
			}
		}
	}

	/**
	 * @return the maxVolume
	 */
	public double getMaxVolume() {
		return maxVolume;
	}

	/**
	 * @param newMaxVolume
	 *            the maxVolume to set
	 */
	public void setMaxVolume(double newMaxVolume) {
		if (newMaxVolume < this.maxVolume) {
			for (ParticleImpl particle : this.getAllParticles()) {
				if (particle.getID() > 0 && particle.getVolume() > newMaxVolume) {
					this.hideParticle(particle, ParticleImpl.HideType.SIZE);
				}
			}
		} else {
			for (ParticleImpl particle : this.getAllParticles()) {
				if (particle.getID() > 0 && particle.getVolume() < newMaxVolume
						&& particle.getVolume() >= this.maxVolume)
					if (particle.wasHiddenBy(ParticleImpl.HideType.DELETE)
							|| particle.wasHiddenBy(ParticleImpl.HideType.FACE_TOUCHED)) {
						continue;
					} else {
						showParticle(particle);
					}
			}
		}

		this.maxVolume = newMaxVolume;
	}

	/**
	 * @return the minVolume
	 */
	public double getMinVolume() {
		return minVolume;
	}

	/**
	 * @param minVolume
	 *            the minVolume to set
	 */
	public void setMinVolume(double minVolume) {
		if (minVolume > this.minVolume) {
			for (ParticleImpl particle : this.getAllParticles()) {
				if (particle.getID() > 0 && particle.getVolume() < minVolume) {
					this.hideParticle(particle, ParticleImpl.HideType.SIZE);
				}
			}
		} else {
			for (ParticleImpl particle : this.getAllParticles()) {
				if (particle.getID() > 0 && particle.getVolume() > minVolume
						&& particle.getVolume() <= this.minVolume)
					if (particle.wasHiddenBy(ParticleImpl.HideType.DELETE)
							|| particle.wasHiddenBy(ParticleImpl.HideType.FACE_TOUCHED)) {
						continue;
					} else {
						showParticle(particle);
					}
			}
		}

		this.minVolume = minVolume;
	}

	/**
	 * ------------------------------------------------------------------------
	 * ---------------
	 */
	/**
	 * Display the result summary table.
	 */
	public void displayResulTable() {
		resultWindow.show();
	}

	/**
	 * TODO: Need to handle particle hiding as this is still hardwired to the particleLabels raw data.
	 * Display the visible particle stack.
	 */
	private void displayParticleImage() {
		ParticleGetter.displayParticleLabels(particleLabels, imp).show();
		IJ.run("Fire");
	}

	/**
	 * TODO: What is this and is it hardwired to raw data.
	 * Display the particle sizes stack
	 */
	private void displayParticleSizeImage() {
		throw new NotImplementedException();
	}

	/**
	 * ------------------------------------------------------------------------
	 * ---------------
	 */
	// 3D Renderings

	public void show3DViewer() {
		univ.getCanvas().addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE
						|| e.getKeyCode() == KeyEvent.VK_DELETE) {
					e.consume();
					univ.removeContent(univ.getSelected().getName());
				}
			}
		});
		ImageWindow3D win = univ.getWindow();

		if (win == null || !win.isVisible())
			this.univ.show();
	}

	public void reAdjustView() {
		try {
			if (univ.contains(imp.getTitle())) {
				Content c = univ.getContent(imp.getTitle());
				univ.adjustView(c);
			}
		} catch (NullPointerException npe) {
			IJ.log("3D Viewer was closed before rendering completed.");
		}
	}

	/**
	 * Display visible particle surfaces in the 3D Viewer.
	 */
	public void displaySurfaces(ParticleManager.ColorMode mode) {
		if (surfacePointsCalculated == false) {
			setAllSurfacePoints(imp, particleLabels, particles, 2);
			this.setSurfaceAreaCalculated(true);
		}
		show3DViewer();
		int p = 0;
		List<ParticleImpl> visibleParticles = getVisibleParticles();
		Iterator<ParticleImpl> iter = visibleParticles.iterator();
		while (iter.hasNext()) {
			ParticleImpl particle = iter.next();

			IJ.showStatus("Rendering surfaces...");
			IJ.showProgress(p, visibleParticles.size());

			if (particle.getID() > 0 && particle.getSurfacePoints().size() > 0) {
				Color3f pColor = new Color3f(0, 0, 0);

				switch (mode) {
				case GRADIENT:
					pColor = ParticleUtilities.getGradientColor(
							particle.getID(), particles.size());
					break;
				case SPLIT:
					if (particle.getVolume() > splitValue) {
						// red if over
						pColor = new Color3f(1.0f, 0.0f, 0.0f);
					} else {
						// yellow if under
						pColor = new Color3f(1.0f, 1.0f, 0.0f);
					}
					break;
				}

				Content pContent = BoneJParticleContentCreator.createContent(
						particle.getSurfaceMesh(pColor), particle, this);

				univ.addContent(pContent).setLocked(true);
			}
			p++;
		}
		this.reAdjustView();
	}

	/**
	 * Display visible particle centroids in the 3D Viewer.
	 */
	public void displayCentroids() {
		show3DViewer();
		int p = 0;

		List<ParticleImpl> visibleParticles = getVisibleParticles();
		Iterator<ParticleImpl> iter = visibleParticles.iterator();
		while (iter.hasNext()) {
			ParticleImpl particle = iter.next();
			IJ.showStatus("Rendering centroids...");
			IJ.showProgress(p, visibleParticles.size());
			if (particle.getID() > 0) {

				float red = 0.0f;
				float green = 0.5f * (float) p
						/ (float) getVisibleParticles().size();
				float blue = 1.0f;
				Color3f cColor = new Color3f(red, green, blue);

				CustomPointMesh mesh = particle.getCentroidMesh(cColor, 1.0f);

				Content pContent = ContentCreator.createContent(mesh,
						"Centroid " + particle.getID());

				try {
					univ.addContent(pContent).setLocked(true);
				} catch (NullPointerException e) {
					IJ.log("3D Viewer was closed before rendering completed.");
					return;
				}
			}
			p++;

		}
		reAdjustView();
	}

	/**
	 * Display visible particle axes in the 3D Viewer.
	 */
	public void displayAxes() {
		if (!surfacePointsCalculated && !eigensCalculated) {
			setAllSurfacePoints(imp, particleLabels, particles, 2);
			this.setSurfaceAreaCalculated(true);
			setAllEigens(imp, particleLabels, particles);
			this.setEigensCalculated(true);
		} else if (!eigensCalculated) {
			setAllEigens(imp, particleLabels, particles);
			this.setEigensCalculated(true);
		}

		show3DViewer();

		double[][] lengths = ParticleGetter.getMaxDistances(imp,
				particleLabels, particles);

		int p = 0;
		List<ParticleImpl> visibleParticles = getVisibleParticles();
		Iterator<ParticleImpl> iter = visibleParticles.iterator();
		while (iter.hasNext()) {
			ParticleImpl particle = iter.next();
			IJ.showStatus("Rendering axes...");
			IJ.showProgress(p, visibleParticles.size());

			if (particle.getID() > 0 && particle.getSurfacePoints().size() > 0) {
				Content pContent = ContentCreator.createContent(particle
						.getAxesMesh(lengths[particle.getID()], 1.0f, 0.0f,
								0.0f, "Principle Axes " + particle.getID()),
						"Principle Axes" + particle.getID());
				if (univ.getContents().contains(pContent.getName())) {
					IJ.error("Mesh named '" + pContent.getName()
							+ "' exists already");
					return;
				}

				try {
					univ.addContent(pContent).setLocked(true);
				} catch (NullPointerException e) {
					IJ.log("3D Viewer was closed before rendering completed.");
					return;
				}
			}
			p++;
		}
		this.reAdjustView();
	}

	/**
	 * Display visible particle ellipsoids in the 3D Viewer.
	 */
	public void displayEllipsoids() {
		if (!surfacePointsCalculated && !ellipsoidsCalculated) {
			setAllSurfacePoints(imp, particleLabels, particles, 2);
			this.setSurfaceAreaCalculated(true);
			for (ParticleImpl p : particles) {
				IJ.showStatus("Generating ellipsoids...");
				IJ.showProgress(p.getID(), particles.size());
				ParticleUtilities.generateEllipsoid(p);
			}
			this.setEllipsoidsCalculated(true);
		} else if (!ellipsoidsCalculated) {
			for (ParticleImpl p : particles) {
				IJ.showStatus("Generating ellipsoids...");
				IJ.showProgress(p.getID(), particles.size());
				ParticleUtilities.generateEllipsoid(p);
			}
			this.setEllipsoidsCalculated(true);
		}

		show3DViewer();

		int p = 0;
		Iterator<ParticleImpl> iter = getVisibleParticles().iterator();
		while (iter.hasNext()) {
			ParticleImpl particle = iter.next();
			IJ.showStatus("Rendering ellipsoids...");
			IJ.showProgress(p, getVisibleParticles().size());

			if (particle.getID() > 0 && particle.getEllipsoid() != null) {
				Color3f cColor = new Color3f(0.0f, 0.5f, 1.0f);
				CustomPointMesh mesh = null;
				try {
					mesh = particle.getEllipsoidMesh(cColor, 1.0f);
				} catch (NullPointerException e) {
					e.printStackTrace();
				}

				Content pContent = ContentCreator.createContent(mesh,
						"Ellipsoid " + particle.getID());

				if (mesh != null) {
					try {
						univ.addContent(pContent).setLocked(true);
					} catch (NullPointerException npe) {
						IJ.log("3D Viewer was closed before rendering completed.");
						return;
					}
				} else {
					continue;
				}
			}
			p++;
		}
		reAdjustView();
	}

	/**
	 * Display the original binary stack in the 3D Viewer. TODO: Add option for
	 * setting re-sampling.
	 */
	public void displayOriginal3DImage() {
		Color3f colour = new Color3f(1.0f, 1.0f, 1.0f);
		boolean[] channels = { true, true, true };
		try {
			univ.addVoltex(imp, colour, imp.getTitle(), 0, channels, 2)
					.setLocked(true);
		} catch (NullPointerException npe) {
			IJ.log("3D Viewer was closed before rendering completed.");
		}
		return;
	}

	/**
	 * Static methods for calculating particle parameters.
	 */
	/**
	 * ------------------------------------------------------------------------
	 * ---------------
	 */

	private static boolean setAllSurfacePoints(ImagePlus imp,
			int[][] particleLabels, List<ParticleImpl> particles, int resampling) {
		Calibration cal = imp.getCalibration();
		final boolean[] channels = { true, false, false };
		for (ParticleImpl p : particles) {
			IJ.showStatus("Getting surface meshes...");
			IJ.showProgress(p.getID(), particles.size());

			ImagePlus binaryImp = ParticleUtilities.getBinaryParticle(p, imp,
					particleLabels, resampling);
			MCTriangulator mct = new MCTriangulator();
			@SuppressWarnings("unchecked")
			List<Point3f> points = (List<Point3f>) mct.getTriangles(binaryImp,
					128, channels, resampling);
			final double xOffset = (p.getLimits()[0] - 1) * cal.pixelWidth;
			final double yOffset = (p.getLimits()[2] - 1) * cal.pixelHeight;
			final double zOffset = (p.getLimits()[4] - 1) * cal.pixelDepth;
			Iterator<Point3f> pIter = points.iterator();
			while (pIter.hasNext()) {
				Point3f point = pIter.next();
				point.x += xOffset;
				point.y += yOffset;
				point.z += zOffset;
			}
			p.setSurfacePoints(points);
			if (points.size() == 0) {
				IJ.log("Particle " + p + " resulted in 0 surface points");
			}
		}
		return true;
	}

	private static boolean setAllEigens(ImagePlus imp, int[][] particleLabels,
			List<ParticleImpl> particles) {
		Calibration cal = imp.getCalibration();
		final double vW = cal.pixelWidth;
		final double vH = cal.pixelHeight;
		final double vD = cal.pixelDepth;
		final double voxVhVd = (vH * vH + vD * vD) / 12;
		final double voxVwVd = (vW * vW + vD * vD) / 12;
		final double voxVhVw = (vH * vH + vW * vW) / 12;
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getImageStackSize();
		double[][] momentTensors = new double[particles.size()][6];
		for (int z = 0; z < d; z++) {
			IJ.showStatus("Calculating particle moments...");
			IJ.showProgress(z, d);
			final double zVd = z * vD;
			for (int y = 0; y < h; y++) {
				final double yVh = y * vH;
				final int index = y * w;
				for (int x = 0; x < w; x++) {
					final int p = particleLabels[z][index + x];
					if (p > 0) {
						final double xVw = x * vW;
						final double dx = xVw
								- particles.get(p).getCentroid()[0];
						final double dy = yVh
								- particles.get(p).getCentroid()[1];
						final double dz = zVd
								- particles.get(p).getCentroid()[2];
						momentTensors[p][0] += dy * dy + dz * dz + voxVhVd; // Ixx
						momentTensors[p][1] += dx * dx + dz * dz + voxVwVd; // Iyy
						momentTensors[p][2] += dy * dy + dx * dx + voxVhVw; // Izz
						momentTensors[p][3] += dx * dy; // Ixy
						momentTensors[p][4] += dx * dz; // Ixz
						momentTensors[p][5] += dy * dz; // Iyz
					}
				}
			}
			for (int p = 1; p < particles.size(); p++) {
				double[][] inertiaTensor = new double[3][3];
				inertiaTensor[0][0] = momentTensors[p][0];
				inertiaTensor[1][1] = momentTensors[p][1];
				inertiaTensor[2][2] = momentTensors[p][2];
				inertiaTensor[0][1] = -momentTensors[p][3];
				inertiaTensor[0][2] = -momentTensors[p][4];
				inertiaTensor[1][0] = -momentTensors[p][3];
				inertiaTensor[1][2] = -momentTensors[p][5];
				inertiaTensor[2][0] = -momentTensors[p][4];
				inertiaTensor[2][1] = -momentTensors[p][5];
				Matrix inertiaTensorMatrix = new Matrix(inertiaTensor);
				EigenvalueDecomposition E = new EigenvalueDecomposition(
						inertiaTensorMatrix);
				particles.get(p).setEigen(E);
			}
		}

		return true;
	}

	private static boolean setAllEulerCharacters(ImagePlus imp,
			int[][] particleLabels, List<ParticleImpl> particles) {
		Connectivity con = new Connectivity();
		ListIterator<ParticleImpl> iter = particles.listIterator();
		while (iter.hasNext()) {
			ParticleImpl p = iter.next();
			ImagePlus particleImp = ParticleUtilities.getBinaryParticle(p, imp,
					particleLabels, 1);
			double euler = con.getSumEuler(particleImp);
			double cavities = ParticleUtilities.getNCavities(particleImp);
			// Calculate number of holes and cavities using
			// Euler = particles - holes + cavities
			// where particles = 1
			double holes = cavities - euler + 1;
			double[] bettis = { euler, holes, cavities };
			p.setEulerCharacter(bettis);
		}
		return true;
	}

	private static boolean setAllThicknesses(ImagePlus imp,
			int[][] particleLabels, List<ParticleImpl> particles) {
		Thickness th = new Thickness();
		ImagePlus thickImp = th.getLocalThickness(imp, false);
		double[][] meanStdDev = ParticleUtilities.getMeanStdDev(thickImp,
				particleLabels, particles, 0);
		for (int i = 1; i < meanStdDev.length; i++)
			particles.get(i).setThickness(meanStdDev[i]);

		return true;
	}

	public static void hideZero(List<ParticleImpl> particles) {
		for (ParticleImpl p : particles)
			if (p.getID() == 0)
				p.setVisible(false);
	}
}
