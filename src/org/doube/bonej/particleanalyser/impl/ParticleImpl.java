/**
 * Particle.java Copyright 2010 Keith Schulze
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import org.doube.bonej.particleanalyser.Particle;
import org.doube.geometry.FitEllipsoid;
import org.doube.jama.EigenvalueDecomposition;
import org.doube.jama.Matrix;

import customnode.CustomLineMesh;
import customnode.CustomMesh;
import customnode.CustomPointMesh;
import customnode.CustomTriangleMesh;

import ij.measure.Calibration;

/**
 * @author Keith Schulze
 * 
 *         This class attempts to modularize the particle counter somewhat by
 *         creating a particle object that holds
 */
public class ParticleImpl implements Cloneable, Particle {

	/**
	 * Particle ID - refers to the parent particleLabel for this Particle.
	 */
	private int ID;

	/**
	 * Particle name
	 */
	private String name;

	/**
	 * Visibility of this particle.
	 */
	private boolean visible = true;

	private List<Particle.HideType> hiddenBy = new ArrayList<Particle.HideType>();

	/**
	 * Particle selected
	 */
	private boolean selected = false;

	/**
	 * Indicates which face(s) of the volume this particle is in contact with
	 */
	@SuppressWarnings("serial")
	private List<Face> edgesTouched = new ArrayList<Face>() {
		{
			add(Face.NONE);
		}
	};

	/**
	 * Array storing the volumetric xyz coordinates for this particle.
	 */
	private double[] centroid;

	/**
	 * Array storing the x, y, z minima and mixima for this particle. limit[0]=
	 * xmin limit[1] = xmax limit[2] = ymin limit[3] = ymax limit[4] = zmin
	 * limit[5] = zmax
	 */
	private int[] limits;

	/**
	 * ImageJ calibration for the parent image.
	 */
	private Calibration calibration;

	/**
	 * This particle objects size in pixels.
	 */
	private long particleSize;

	/**
	 * 
	 */
	private List<Point3f> surfacePoints = null;
	
	/**
	 * 
	 */
	private EigenvalueDecomposition eigen = null;
	
	/**
	 * 
	 */
	private double surfaceArea = Double.NaN;
	
	/**
	 * 
	 */
	private double feretDiameter = 0.0;
	
	/**
	 * 
	 */
	private double enclosedVolume = Double.NaN;
	
	/**
	 * 
	 */
	private double[] eulerCharacter = null;
	
	/**
	 * 
	 */
	private double[] thickness = null;
	
	/**
	 * 
	 */
	private Object[] ellipsoid = null;

	/**
	 * @param iD
	 * @param name
	 * @param visible
	 * @param touchingEdge
	 * @param edgesTouched
	 * @param centroid
	 * @param limits
	 * @param calibration
	 * @param particleSize
	 * @param surfacePoints
	 */
	public ParticleImpl(int iD, String name, boolean visible,
			List<Face> edgeTouched, double[] centroid, int[] limits,
			Calibration calibration, long particleSize,
			List<Point3f> surfacePoints) {
		super();
		this.ID = iD;
		this.name = name;
		this.visible = visible;
		this.edgesTouched = edgeTouched;
		this.centroid = centroid;
		this.limits = limits;
		this.calibration = calibration;
		this.particleSize = particleSize;
		this.surfacePoints = surfacePoints;
	}

	/**
	 * @param iD
	 * @param name
	 * @param touchingEdge
	 * @param edgesTouched
	 * @param centroid
	 * @param limits
	 * @param calibration
	 * @param particleSize
	 * @param surfacePoints
	 */
	public ParticleImpl(int iD, String name, Calibration calibration,
			List<Face> edgeTouched, double[] centroid, int[] limits,
			long particleSize) {
		super();
		this.ID = iD;
		this.name = name;
		this.edgesTouched = edgeTouched;
		this.centroid = centroid;
		this.limits = limits;
		this.calibration = calibration;
		this.particleSize = particleSize;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getID()
	 */
	@Override
	public int getID() {
		return ID;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#setID(int)
	 */
	@Override
	public void setID(int iD) {
		ID = iD;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#isVisible()
	 */
	@Override
	public boolean isVisible() {
		return visible;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean visible) {
		this.visible = visible;
		if (visible)
			removeAllHiddenByValues();
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getHiddenBy()
	 */
	@Override
	public List<Particle.HideType> getHiddenBy() {
		return hiddenBy;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#wasHiddenBy(org.doube.bonej.particleanalyser.impl.ParticleImpl.HideType)
	 */
	@Override
	public boolean wasHiddenBy(Particle.HideType hidetype) {
		if (this.hiddenBy.contains(hidetype)) {
			return true;
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#setHiddenBy(org.doube.bonej.particleanalyser.impl.ParticleImpl.HideType)
	 */
	@Override
	public void setHiddenBy(Particle.HideType hideType) {
		if (!this.hiddenBy.contains(hideType))
			this.hiddenBy.add(hideType);
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#setHiddenBy(java.util.List)
	 */
	@Override
	public void setHiddenBy(List<Particle.HideType> hiddenBy) {
		this.hiddenBy = hiddenBy;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#removeAllHiddenByValues()
	 */
	@Override
	public void removeAllHiddenByValues() {
		for (int i = 0; i < hiddenBy.size(); i++)
			this.hiddenBy.remove(i);
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#isSelected()
	 */
	@Override
	public boolean isSelected() {
		return selected;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#setSelected(boolean)
	 */
	@Override
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#isTouchingEdge(org.doube.bonej.particleanalyser.impl.Face)
	 */
	@Override
	public boolean isTouchingEdge(Face face) {
		if (this.edgesTouched.contains(face)) {
			return true;
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getEdgesTouched()
	 */
	@Override
	public List<Face> getEdgesTouched() {
		return edgesTouched;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#setEdgesTouched(java.util.List)
	 */
	@Override
	public void setEdgesTouched(List<Face> edgeTouched) {
		this.edgesTouched = edgeTouched;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getCentroid()
	 */
	@Override
	public double[] getCentroid() {
		return centroid;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#setCentroid(double[])
	 */
	@Override
	public void setCentroid(double[] centroid) {
		this.centroid = centroid;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getLimits()
	 */
	@Override
	public int[] getLimits() {
		return limits;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#setLimits(int[])
	 */
	@Override
	public void setLimits(int[] limits) {
		this.limits = limits;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getCalibration()
	 */
	@Override
	public Calibration getCalibration() {
		return calibration;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#setCalibration(ij.measure.Calibration)
	 */
	@Override
	public void setCalibration(Calibration calibration) {
		this.calibration = calibration;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getParticleSize()
	 */
	@Override
	public long getParticleSize() {
		return particleSize;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#setParticleSize(long)
	 */
	@Override
	public void setParticleSize(long particleSize) {
		this.particleSize = particleSize;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getVolume()
	 */
	@Override
	public double getVolume() {
		final double voxelVolume = this.calibration.pixelWidth
				* this.calibration.pixelHeight * this.calibration.pixelDepth;

		return voxelVolume * particleSize;
	}
	
	private double getArea() {
		final double pixelArea = this.calibration.pixelWidth
		* this.calibration.pixelHeight;
		return pixelArea * particleSize;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getSurfacePoints()
	 */
	@Override
	public List<Point3f> getSurfacePoints() {
		return surfacePoints;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#setSurfacePoints(java.util.List)
	 */
	@Override
	public void setSurfacePoints(List<Point3f> surfacePoints) {
		this.surfacePoints = surfacePoints;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getSurfaceMesh(javax.vecmath.Color3f)
	 */
	@Override
	public CustomTriangleMesh getSurfaceMesh(Color3f color) {
		if (surfacePoints != null) {
			CustomTriangleMesh tmesh = new CustomTriangleMesh(
					this.getSurfacePoints(), color, 0);
			return tmesh;
		} else {
			throw new NullPointerException("Surface Points not calculated");
		}
	}

	// ////////////////////////////////////////////////////////////////////////////////////
	/**
	 * @return the eigen
	 */
	protected EigenvalueDecomposition getEigen() {
		return this.eigen;
	}

	/**
	 * @param eigen
	 *            the eigen to set
	 */
	protected void setEigen(EigenvalueDecomposition eigen) {
		this.eigen = eigen;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getCentroidMesh(javax.vecmath.Color3f, float)
	 */
	@Override
	public CustomPointMesh getCentroidMesh(Color3f color, float transparency) {
		Point3f cent = new Point3f();
		cent.x = (float) this.centroid[0];
		cent.y = (float) this.centroid[1];
		cent.z = (float) this.centroid[2];

		List<Point3f> point = new ArrayList<Point3f>();
		point.add(cent);
		CustomPointMesh mesh = new CustomPointMesh(point);
		mesh.setPointSize(5.0f);
		mesh.setColor(color);

		return mesh;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getAxesMesh(double[], float, float, float, java.lang.String)
	 */
	@Override
	public CustomMesh getAxesMesh(double[] lengths, float red, float green,
			float blue, String title) {
		if (eigen != null) {
			final Matrix eVec = eigen.getV();
			double[][] unitVector = eVec.getArray();
			final double cX = centroid[0];
			final double cY = centroid[1];
			final double cZ = centroid[2];
			final double eVec1x = unitVector[0][0];
			final double eVec1y = unitVector[1][0];
			final double eVec1z = unitVector[2][0];
			final double eVec2x = unitVector[0][1];
			final double eVec2y = unitVector[1][1];
			final double eVec2z = unitVector[2][1];
			final double eVec3x = unitVector[0][2];
			final double eVec3y = unitVector[1][2];
			final double eVec3z = unitVector[2][2];
			final double l1 = lengths[0];
			final double l2 = lengths[1];
			final double l3 = lengths[2];

			List<Point3f> mesh = new ArrayList<Point3f>();
			Point3f start1 = new Point3f();
			start1.x = (float) (cX - eVec1x * l1);
			start1.y = (float) (cY - eVec1y * l1);
			start1.z = (float) (cZ - eVec1z * l1);
			mesh.add(start1);

			Point3f end1 = new Point3f();
			end1.x = (float) (cX + eVec1x * l1);
			end1.y = (float) (cY + eVec1y * l1);
			end1.z = (float) (cZ + eVec1z * l1);
			mesh.add(end1);

			Point3f start2 = new Point3f();
			start2.x = (float) (cX - eVec2x * l2);
			start2.y = (float) (cY - eVec2y * l2);
			start2.z = (float) (cZ - eVec2z * l2);
			mesh.add(start2);

			Point3f end2 = new Point3f();
			end2.x = (float) (cX + eVec2x * l2);
			end2.y = (float) (cY + eVec2y * l2);
			end2.z = (float) (cZ + eVec2z * l2);
			mesh.add(end2);

			Point3f start3 = new Point3f();
			start3.x = (float) (cX - eVec3x * l3);
			start3.y = (float) (cY - eVec3y * l3);
			start3.z = (float) (cZ - eVec3z * l3);
			mesh.add(start3);

			Point3f end3 = new Point3f();
			end3.x = (float) (cX + eVec3x * l3);
			end3.y = (float) (cY + eVec3y * l3);
			end3.z = (float) (cZ + eVec3z * l3);
			mesh.add(end3);

			Color3f aColour = new Color3f(red, green, blue);

			return new CustomLineMesh(mesh, CustomLineMesh.PAIRWISE, aColour, 0);
		} else {
			throw new NullPointerException("Eigen not calculated");
		}

	}

	/**
	 * @return the surfaceArea
	 */
	protected double getSurfaceArea() {
		return this.surfaceArea;
	}

	/**
	 * @param surfaceArea
	 *            the surfaceArea to set
	 */
	protected void setSurfaceArea(double surfaceArea) {
		this.surfaceArea = surfaceArea;
	}

	/**
	 * @return the ferets
	 */
	protected double getFeretDiameter() {
		return this.feretDiameter;
	}

	/**
	 * @param ferets
	 *            the ferets to set
	 */
	protected void setFeretDiameter(double feretDiameter) {
		this.feretDiameter = feretDiameter;
	}

	/**
	 * @return the enclosedVolume
	 */
	protected double getEnclosedVolume() {
		return this.enclosedVolume;
	}

	/**
	 * @param enclosedVolume
	 *            the enclosedVolume to set
	 */
	protected void setEnclosedVolume(double enclosedVolume) {
		this.enclosedVolume = enclosedVolume;
	}

	/**
	 * @return the eulerCharacter
	 */
	protected double[] getEulerCharacter() {
		return this.eulerCharacter;
	}

	/**
	 * @param eulerCharacter
	 *            the eulerCharacter to set
	 */
	protected void setEulerCharacter(double[] eulerCharacter) {
		this.eulerCharacter = eulerCharacter;
	}

	/**
	 * @return the thickness
	 */
	protected double[] getThickness() {
		return this.thickness;
	}

	/**
	 * @param thickness
	 *            the thickness to set
	 */
	protected void setThickness(double[] thickness) {
		this.thickness = thickness;
	}

	/**
	 * @return the ellipsoid
	 */
	protected Object[] getEllipsoid() {
		return this.ellipsoid;
	}

	/**
	 * @param ellipsoid
	 *            the ellipsoid to set
	 */
	protected void setEllipsoid(Object[] ellipsoid) {
		this.ellipsoid = ellipsoid;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getEllipsoidMesh(javax.vecmath.Color3f, float)
	 */
	@Override
	public CustomPointMesh getEllipsoidMesh(Color3f color, float transparency) {
		double[] centre = (double[]) this.ellipsoid[0];
		double[] radii = (double[]) this.ellipsoid[1];
		double[][] eV = (double[][]) this.ellipsoid[2];

		for (int r = 0; r < 3; r++) {
			Double s = radii[r];
			if (s.equals(Double.NaN))
				throw new NullPointerException("Radius was null");
		}

		final double a = radii[0];
		final double b = radii[1];
		final double c = radii[2];
		double[][] ellipsoid = FitEllipsoid.testEllipsoid(a, b, c, 0, 0, 0, 0,
				0, 1000, false);
		final int nPoints = ellipsoid.length;

		for (int p = 0; p < nPoints; p++) {
			final double x = ellipsoid[p][0];
			final double y = ellipsoid[p][1];
			final double z = ellipsoid[p][2];
			ellipsoid[p][0] = x * eV[0][0] + y * eV[0][1] + z * eV[0][2]
					+ centre[0];
			ellipsoid[p][1] = x * eV[1][0] + y * eV[1][1] + z * eV[1][2]
					+ centre[1];
			ellipsoid[p][2] = x * eV[2][0] + y * eV[2][1] + z * eV[2][2]
					+ centre[2];
		}

		List<Point3f> points = new ArrayList<Point3f>();
		for (int p = 0; p < nPoints; p++) {
			Point3f e = new Point3f();
			e.x = (float) ellipsoid[p][0];
			e.y = (float) ellipsoid[p][1];
			e.z = (float) ellipsoid[p][2];
			points.add(e);
		}

		CustomPointMesh mesh = new CustomPointMesh(points);
		mesh.setColor(color);

		return mesh;
	}

	/* (non-Javadoc)
	 * @see org.doube.bonej.particleanalyser.impl.Paricle#getParamterByKey(java.lang.String)
	 */
	@Override
	public Object getParamterByKey(Particle.ParameterKey key) {
		HashMap<ParameterKey, Object> resultMap = this.generateResultMap();

		return resultMap.get(key);
	}
	
	private HashMap<Particle.ParameterKey, Object> generateResultMap() {
		HashMap<Particle.ParameterKey, Object> resultMap = new HashMap<Particle.ParameterKey, Object>();
		
		resultMap.put(ParameterKey.ID, this.ID);
		resultMap.put(ParameterKey.NAME, this.name);
		resultMap.put(ParameterKey.VOLUME, this.getVolume());
		resultMap.put(ParameterKey.VOLUME, this.getArea());
		resultMap.put(ParameterKey.X_CENTROID, this.centroid[0]);
		resultMap.put(ParameterKey.Y_CENTROID, this.centroid[1]);
		resultMap.put(ParameterKey.Z_CENTROID, this.centroid[2]);
		resultMap.put(ParameterKey.SURFACE_AREA, this.surfaceArea);
		resultMap.put(ParameterKey.FERET_DIAMETER, this.feretDiameter);
		resultMap.put(ParameterKey.ENCLOSED_VOLUME, this.enclosedVolume);

		if (this.eigen != null) {
			resultMap.put(ParameterKey.EIGEN_I1, this.eigen.getD().get(2, 2));
			resultMap.put(ParameterKey.EIGEN_I2, this.eigen.getD().get(1, 1));
			resultMap.put(ParameterKey.EIGEN_I3, this.eigen.getD().get(0, 0));
			resultMap.put(ParameterKey.EIGEN_VX, this.eigen.getD().get(0, 2));
			resultMap.put(ParameterKey.EIGEN_VY, this.eigen.getD().get(1, 2));
			resultMap.put(ParameterKey.EIGEN_VZ, this.eigen.getD().get(2, 2));
		}
		if (this.eulerCharacter != null) {
			resultMap.put(ParameterKey.EULER_CHARACTER, this.eulerCharacter[0]);
			resultMap.put(ParameterKey.EULER_HOLES, this.eulerCharacter[1]);
			resultMap.put(ParameterKey.EULER_CAVATIES, this.eulerCharacter[2]);
		}
		if (this.thickness != null) {
			resultMap.put(ParameterKey.THICKNESS, this.thickness[0]);
			resultMap.put(ParameterKey.THICKNESS_SD, this.thickness[1]);
			resultMap.put(ParameterKey.THICKNESS_MAX, this.thickness[2]);
		}
		if (this.ellipsoid != null) {
			resultMap.put(ParameterKey.ELLIPSOID_MAJOR_RADIUS, this.ellipsoid[2]);
			resultMap.put(ParameterKey.ELLIPSOID_INT_RADIUS, this.ellipsoid[1]);
			resultMap.put(ParameterKey.ELLIPSOID_MINOR_RADIUS, this.ellipsoid[0]);
		}
		
		return resultMap;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}
