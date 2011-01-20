package org.doube.bonej.particleanalyser;

import ij.measure.Calibration;

import java.util.List;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import org.doube.bonej.particleanalyser.impl.Face;

import customnode.CustomMesh;
import customnode.CustomPointMesh;
import customnode.CustomTriangleMesh;

public interface Particle {

	/**
	 * @author Keith Schulze 
	 * Enum that describes the methods by which this particle was hidden if it is
	 * not shown.
	 */
	public static enum HideType {
		DELETE, SIZE, FACE_TOUCHED;
	}
	
	public Object clone() throws CloneNotSupportedException;

	/**
	 * @return the iD
	 */
	public int getID();

	/**
	 * @param iD
	 *            the iD to set
	 */
	public void setID(int iD);

	/**
	 * @return the name
	 */
	public String getName();

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name);

	/**
	 * @return the visible
	 */
	public boolean isVisible();

	/**
	 * @param visible
	 *            the visible to set
	 */
	public void setVisible(boolean visible);

	/**
	 * @return the hiddenBy
	 */
	public List<Particle.HideType> getHiddenBy();

	/**
	 * @param hidetype
	 * @return
	 */
	public boolean wasHiddenBy(Particle.HideType hidetype);

	/**
	 * @param hideType
	 */
	public void setHiddenBy(Particle.HideType hideType);

	/**
	 * @param hiddenBy
	 *            the hiddenBy to set
	 */
	public void setHiddenBy(List<Particle.HideType> hiddenBy);

	/**
	 * 
	 */
	public void removeAllHiddenByValues();

	/**
	 * @return the selected
	 */
	public boolean isSelected();

	/**
	 * @param selected
	 *            the selected to set
	 */
	public void setSelected(boolean selected);

	/**
	 * @return the touchingEdge
	 */
	public boolean isTouchingEdge(Face face);

	/**
	 * @return the edgesTouched
	 */
	public List<Face> getEdgesTouched();

	/**
	 * @param edgesTouched
	 *            the edgesTouched to set
	 */
	public void setEdgesTouched(List<Face> edgeTouched);

	/**
	 * @return the centroid
	 */
	public double[] getCentroid();

	/**
	 * @param centroid
	 *            the centroid to set
	 */
	public void setCentroid(double[] centroid);

	/**
	 * @return the limits
	 */
	public int[] getLimits();

	/**
	 * @param limits
	 *            the limits to set
	 */
	public void setLimits(int[] limits);

	/**
	 * @return the calibration
	 */
	public Calibration getCalibration();

	/**
	 * @param calibration
	 *            the calibration to set
	 */
	public void setCalibration(Calibration calibration);

	/**
	 * @return the particleSize
	 */
	public long getParticleSize();

	/**
	 * @param particleSize
	 *            the particleSize to set
	 */
	public void setParticleSize(long particleSize);

	/**
	 * @return
	 */
	public double getVolume();

	/**
	 * @return the surfacePoints
	 */
	public List<Point3f> getSurfacePoints();

	/**
	 * @param surfacePoints
	 *            the surfacePoints to set
	 */
	public void setSurfacePoints(List<Point3f> surfacePoints);

	public CustomTriangleMesh getSurfaceMesh(Color3f color);

	public CustomPointMesh getCentroidMesh(Color3f color, float transparency);

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
			float blue, String title);

	/**
	 * @param color
	 * @param transparency
	 * @return
	 */
	public CustomPointMesh getEllipsoidMesh(Color3f color, float transparency);

	/**
	 * @param key
	 * @return
	 */
	public Object getParamterByKey(String key);

}