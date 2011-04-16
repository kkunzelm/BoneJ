/**
 * ParticleTableModel.java Copyright 2010 Keith Schulze
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
package org.doube.bonej.particleanalyser.ui;

import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.doube.bonej.particleanalyser.Particle;
import org.doube.bonej.particleanalyser.impl.ParticleImpl;
import org.doube.bonej.particleanalyser.impl.ParticleManagerImpl;

/**
 * @author Keith Schulze
 *
 */
public class ParticleTableModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ParticleManagerImpl particleManager;
	//private List<String> columnNames = Arrays.asList("ID", "Name", "Vol.", "x Cent", "y Cent", "z Cent");
	private List<Particle.ParameterKey> columns;
	private Calibration calibration;
	
	private boolean showSurfaceArea = false;
	private boolean showFeretDiameter = false;
	private boolean showEnclosedVolume = false;
	private boolean showEigens = false;
	private boolean showEulerCharacters = false;
	private boolean showThickness = false;
	private boolean showEllipsoids = false;
	
	
	/**
	 * @param particles
	 */
	@SuppressWarnings("serial")
	public ParticleTableModel(ParticleManagerImpl pm) {
		super();
		this.particleManager = pm;
		
		this.calibration = this.particleManager.getCalibration();
		this.columns = new ArrayList<Particle.ParameterKey>() {{
			add(Particle.ParameterKey.NUMBER);
			add(Particle.ParameterKey.ID);
			add(Particle.ParameterKey.NAME);
			add(Particle.ParameterKey.VOLUME);
			add(Particle.ParameterKey.X_CENTROID);
			add(Particle.ParameterKey.Y_CENTROID);
			add(Particle.ParameterKey.Z_CENTROID);
		}};
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(int column) {
		return this.columns.get(column).getStringValue(this.calibration);
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	@Override
	public int getRowCount() {
		return this.particleManager.getVisibleParticles().size();
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return this.columns.size();
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == 0) {
			return rowIndex+1;
		} else {
			List<Particle> visibleParticles = this.particleManager.getVisibleParticles();
			
			return visibleParticles.get(rowIndex).getParamterByKey(this.columns.get(columnIndex));
		}
	}
	
	public void setParticleManager(ParticleManagerImpl pm) {
		this.particleManager = pm;
	}

	/**
	 * @return the showSurfaceArea
	 */
	public boolean isShowSurfaceArea() {
		return showSurfaceArea;
	}

	/**
	 * @param showSurfaceArea the showSurfaceArea to set
	 */
	public void setShowSurfaceArea(boolean showSurfaceArea) {
		this.showSurfaceArea = showSurfaceArea;
		if (showSurfaceArea) {
			this.columns.add(columns.size(), Particle.ParameterKey.SURFACE_AREA);
		} else {
			this.columns.remove(Particle.ParameterKey.SURFACE_AREA);
		}
	}

	/**
	 * @return the showFeretDiameter
	 */
	public boolean isShowFeretDiameter() {
		return showFeretDiameter;
	}

	/**
	 * @param showFeretDiameter the showFeretDiameter to set
	 */
	public void setShowFeretDiameter(boolean showFeretDiameter) {
		this.showFeretDiameter = showFeretDiameter;
		if (showFeretDiameter) {
			this.columns.add(columns.size(), Particle.ParameterKey.FERET_DIAMETER);
		} else {
			this.columns.remove(Particle.ParameterKey.FERET_DIAMETER);
		}
	}

	/**
	 * @return the showEnclosedVolume
	 */
	public boolean isShowEnclosedVolume() {
		return showEnclosedVolume;
	}

	/**
	 * @param showEnclosedVolume the showEnclosedVolume to set
	 */
	public void setShowEnclosedVolume(boolean showEnclosedVolume) {
		this.showEnclosedVolume = showEnclosedVolume;
		if (showEnclosedVolume) {
			this.columns.add(columns.size(), Particle.ParameterKey.ENCLOSED_VOLUME);
		} else {
			this.columns.remove(Particle.ParameterKey.ENCLOSED_VOLUME);
		}
	}

	/**
	 * @return the showEigens
	 */
	public boolean isShowEigens() {
		return showEigens;
	}

	/**
	 * @param showEigens the showEigens to set
	 */
	public void setShowEigens(boolean showEigens) {
		this.showEigens = showEigens;
		if (showEigens) {
			this.columns.add(columns.size(), Particle.ParameterKey.EIGEN_I1);
			this.columns.add(columns.size(), Particle.ParameterKey.EIGEN_I2);
			this.columns.add(columns.size(), Particle.ParameterKey.EIGEN_I3);
			this.columns.add(columns.size(), Particle.ParameterKey.EIGEN_VX);
			this.columns.add(columns.size(), Particle.ParameterKey.EIGEN_VY);
			this.columns.add(columns.size(), Particle.ParameterKey.EIGEN_VZ);
		} else {
			this.columns.remove(Particle.ParameterKey.EIGEN_I1);
			this.columns.remove(Particle.ParameterKey.EIGEN_I2);
			this.columns.remove(Particle.ParameterKey.EIGEN_I3);
			this.columns.remove(Particle.ParameterKey.EIGEN_VX);
			this.columns.remove(Particle.ParameterKey.EIGEN_VY);
			this.columns.remove(Particle.ParameterKey.EIGEN_VZ);
		}
	}

	/**
	 * @return the showEulerCharacters
	 */
	public boolean isShowEulerCharacters() {
		return showEulerCharacters;
	}

	/**
	 * @param showEulerCharacters the showEulerCharacters to set
	 */
	public void setShowEulerCharacters(boolean showEulerCharacters) {
		this.showEulerCharacters = showEulerCharacters;
		if (showEulerCharacters){
			this.columns.add(columns.size(), Particle.ParameterKey.EULER_CHARACTER);
			this.columns.add(columns.size(), Particle.ParameterKey.EULER_HOLES);
			this.columns.add(columns.size(), Particle.ParameterKey.EULER_CAVATIES);
		} else {
			this.columns.remove(Particle.ParameterKey.EULER_CHARACTER);
			this.columns.remove(Particle.ParameterKey.EULER_HOLES);
			this.columns.remove(Particle.ParameterKey.EULER_CAVATIES);
		}
	}

	/**
	 * @return the showThickness
	 */
	public boolean isShowThickness() {
		return showThickness;
	}

	/**
	 * @param showThickness the showThickness to set
	 */
	public void setShowThickness(boolean showThickness) {
		this.showThickness = showThickness;
		if(showThickness){
			this.columns.add(columns.size(), Particle.ParameterKey.THICKNESS);
			this.columns.add(columns.size(), Particle.ParameterKey.THICKNESS_SD);
			this.columns.add(columns.size(), Particle.ParameterKey.THICKNESS_MAX);
		} else {
			this.columns.remove(Particle.ParameterKey.THICKNESS);
			this.columns.remove(Particle.ParameterKey.THICKNESS_SD);
			this.columns.remove(Particle.ParameterKey.THICKNESS_MAX);
		}
	}

	/**
	 * @return the showEllipsoids
	 */
	public boolean isShowEllipsoids() {
		return showEllipsoids;
	}

	/**
	 * @param showEllipsoids the showEllipsoids to set
	 */
	public void setShowEllipsoids(boolean showEllipsoids) {
		this.showEllipsoids = showEllipsoids;
		if(showEllipsoids){
			this.columns.add(columns.size(), Particle.ParameterKey.ELLIPSOID_MAJOR_RADIUS);
			this.columns.add(columns.size(), Particle.ParameterKey.ELLIPSOID_INT_RADIUS);
			this.columns.add(columns.size(), Particle.ParameterKey.ELLIPSOID_MINOR_RADIUS);
		} else {
			this.columns.remove(Particle.ParameterKey.ELLIPSOID_MAJOR_RADIUS);
			this.columns.remove(Particle.ParameterKey.ELLIPSOID_INT_RADIUS);
			this.columns.remove(Particle.ParameterKey.ELLIPSOID_MINOR_RADIUS);
		}
	}
}
