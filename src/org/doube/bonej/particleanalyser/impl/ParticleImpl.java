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
public class ParticleImpl implements Cloneable {

	/**
	 * @author Keith Schulze 
	 * Enum that describes the methods by which this particle was hidden if it is
	 * not shown.
	 */
	public static enum HideType {
		DELETE, SIZE, FACE_TOUCHED;
	}

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

	private List<HideType> hiddenBy = new ArrayList<ParticleImpl.HideType>();

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

	/**
	 * @return the iD
	 */
	public int getID() {
		return ID;
	}

	/**
	 * @param iD
	 *            the iD to set
	 */
	public void setID(int iD) {
		ID = iD;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the visible
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * @param visible
	 *            the visible to set
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
		if (visible)
			removeAllHiddenByValues();
	}

	/**
	 * @return the hiddenBy
	 */
	public List<HideType> getHiddenBy() {
		return hiddenBy;
	}

	/**
	 * @param hidetype
	 * @return
	 */
	public boolean wasHiddenBy(HideType hidetype) {
		if (this.hiddenBy.contains(hidetype)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @param hideType
	 */
	public void setHiddenBy(HideType hideType) {
		if (!this.hiddenBy.contains(hideType))
			this.hiddenBy.add(hideType);
	}

	/**
	 * @param hiddenBy
	 *            the hiddenBy to set
	 */
	public void setHiddenBy(List<HideType> hiddenBy) {
		this.hiddenBy = hiddenBy;
	}

	/**
	 * 
	 */
	public void removeAllHiddenByValues() {
		for (int i = 0; i < hiddenBy.size(); i++)
			this.hiddenBy.remove(i);
	}

	/**
	 * @return the selected
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * @param selected
	 *            the selected to set
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * @return the touchingEdge
	 */
	public boolean isTouchingEdge(Face face) {
		if (this.edgesTouched.contains(face)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @return the edgesTouched
	 */
	public List<Face> getEdgesTouched() {
		return edgesTouched;
	}

	/**
	 * @param edgesTouched
	 *            the edgesTouched to set
	 */
	public void setEdgesTouched(List<Face> edgeTouched) {
		this.edgesTouched = edgeTouched;
	}

	/**
	 * @return the centroid
	 */
	public double[] getCentroid() {
		return centroid;
	}

	/**
	 * @param centroid
	 *            the centroid to set
	 */
	public void setCentroid(double[] centroid) {
		this.centroid = centroid;
	}

	/**
	 * @return the limits
	 */
	public int[] getLimits() {
		return limits;
	}

	/**
	 * @param limits
	 *            the limits to set
	 */
	public void setLimits(int[] limits) {
		this.limits = limits;
	}

	/**
	 * @return the calibration
	 */
	public Calibration getCalibration() {
		return calibration;
	}

	/**
	 * @param calibration
	 *            the calibration to set
	 */
	public void setCalibration(Calibration calibration) {
		this.calibration = calibration;
	}

	/**
	 * @return the particleSize
	 */
	public long getParticleSize() {
		return particleSize;
	}

	/**
	 * @param particleSize
	 *            the particleSize to set
	 */
	public void setParticleSize(long particleSize) {
		this.particleSize = particleSize;
	}

	/**
	 * @return
	 */
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

	/**
	 * @return the surfacePoints
	 */
	public List<Point3f> getSurfacePoints() {
		return surfacePoints;
	}

	/**
	 * @param surfacePoints
	 *            the surfacePoints to set
	 */
	public void setSurfacePoints(List<Point3f> surfacePoints) {
		this.surfacePoints = surfacePoints;
	}

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

	/**
	 * Draws 3 orthogonal axes defined by the centroid, unit vector and axis
	 * length.
	 * 
	 * @param centroid
	 * @param unitVector
	 * @param lengths
	 * @param red
	 * @param green
	 * @param blue
	 * @param title
	 */
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

	/**
	 * @param color
	 * @param transparency
	 * @return
	 */
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

	/**
	 * @param key
	 * @return
	 */
	public Object getParamterByKey(String key) {
		HashMap<String, Object> resultMap = this.generateResultMap();

		return resultMap.get(key);
	}
	
	private HashMap<String, Object> generateResultMap() {
		HashMap<String, Object> resultMap = new HashMap<String, Object>();
		
		resultMap.put("ID", this.ID);
		resultMap.put("Name", this.name);
		resultMap.put("<html>Vol. (" + calibration.getUnits() + "<sup>3</sup>)</html>", this.getVolume());
		resultMap.put("<html>Area (" + calibration.getUnits() + "<sup>2</sup>)</html>", this.getArea());
		resultMap.put("x Cent (" + calibration.getUnits() + ")", this.centroid[0]);
		resultMap.put("y Cent (" + calibration.getUnits() + ")", this.centroid[1]);
		resultMap.put("z Cent (" + calibration.getUnits() + ")", this.centroid[2]);
		resultMap.put("SA (" + calibration.getUnits() + "²)", this.surfaceArea);
		resultMap.put("Feret (" + calibration.getUnits() + ")", this.feretDiameter);
		resultMap.put("Encl. Vol. (" + calibration.getUnits() + "³)", this.enclosedVolume);

		if (this.eigen != null) {
			resultMap.put("I1", this.eigen.getD().get(2, 2));
			resultMap.put("I2", this.eigen.getD().get(1, 1));
			resultMap.put("I3", this.eigen.getD().get(0, 0));
			resultMap.put("vX", this.eigen.getD().get(0, 2));
			resultMap.put("vY", this.eigen.getD().get(1, 2));
			resultMap.put("vZ", this.eigen.getD().get(2, 2));
		}
		if (this.eulerCharacter != null) {
			resultMap.put("Euler (Ï‡)", this.eulerCharacter[0]);
			resultMap.put("Holes (Î²1)", this.eulerCharacter[1]);
			resultMap.put("Cavities (Î²2)", this.eulerCharacter[2]);
		}
		if (this.thickness != null) {
			resultMap.put("Thickness (" + calibration.getUnits() + ")", this.thickness[0]);
			resultMap.put("SD Thickness (" + calibration.getUnits() + ")", this.thickness[1]);
			resultMap.put("Max Thickness (" + calibration.getUnits() + ")", this.thickness[2]);
		}
		if (this.ellipsoid != null) {
			resultMap.put("Major radius (" + calibration.getUnits() + ")", this.ellipsoid[2]);
			resultMap.put("Int. radius (" + calibration.getUnits() + ")", this.ellipsoid[1]);
			resultMap.put("Minor radius (" + calibration.getUnits() + ")", this.ellipsoid[0]);
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
