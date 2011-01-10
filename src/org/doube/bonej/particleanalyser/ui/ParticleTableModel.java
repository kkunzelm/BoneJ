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

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.doube.bonej.particleanalyser.Particle;
import org.doube.bonej.particleanalyser.ParticleManager;

/**
 * @author Keith Schulze
 *
 */
public class ParticleTableModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ParticleManager particleManager;
	//private List<String> columnNames = Arrays.asList("ID", "Name", "Vol.", "x Cent", "y Cent", "z Cent");
	private List<String> columnNames;
	
	private String units;
	
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
	public ParticleTableModel(ParticleManager pm) {
		super();
		this.particleManager = pm;
		
		this.units = this.particleManager.getCalibration().getUnits();
		this.columnNames = new ArrayList<String>() {{
			add("");
			add("ID");
			add("Name");
			add("<html>Vol. (" + units + "<sup>3</sup>)</html>");
			add("<html>Area (" + units + "<sup>2</sup>)</html>");
			add("x Cent (" + units + ")");
			add("y Cent (" + units + ")");
			add("z Cent (" + units + ")");
		}};
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(int column) {
		return this.columnNames.get(column);
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
		return this.columnNames.size();
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
			
			return visibleParticles.get(rowIndex).tableLookup(this.columnNames.get(columnIndex));
		}
	}
	
	public void setParticleManager(ParticleManager pm) {
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
		String surfaceArea = "SA (" + units + "²)";
		if (showSurfaceArea) {
			this.columnNames.add(columnNames.size(), surfaceArea);
		} else {
			this.columnNames.remove(surfaceArea);
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
		String feret = "Feret (" + units + ")";
		if (showFeretDiameter) {
			this.columnNames.add(columnNames.size(), feret);
		} else {
			this.columnNames.remove(feret);
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
		String ev = "Encl. Vol. (" + units + "³)";
		if (showEnclosedVolume) {
			this.columnNames.add(columnNames.size(), ev);
		} else {
			this.columnNames.remove(ev);
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
		String[] eigenValues = {"I1","I2","I3","vX","vY","vZ"};
		if (showEigens) {
			for(String ev : eigenValues){
				this.columnNames.add(columnNames.size(), ev);
			}
		} else {
			for (String ev : eigenValues) {
				this.columnNames.remove(ev);
			}
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
		String[] eulerCharacterValues = {"Euler (Ï‡)", "Holes (Î²1)", "Cavities (Î²2)"};
		if (showEulerCharacters){
			for(String ecv : eulerCharacterValues) {
				this.columnNames.add(columnNames.size(), ecv);
			}
		} else {
			for(String ecv : eulerCharacterValues) {
				this.columnNames.remove(ecv);
			}
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
		String[] thicknessValues = {"Thickness (" + units + ")", 
				"SD Thickness (" + units + ")","Max Thickness (" + units + ")"};
		if(showThickness){
			for(String tv : thicknessValues) {
				this.columnNames.add(columnNames.size(), tv);
			}
		} else {
			for (String tv : thicknessValues) {
				this.columnNames.remove(tv);
			}
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
		String[] ellipsoidValues = {"Major radius (" + units + ")",
				"Int. radius (" + units + ")","Minor radius (" + units + ")"};
		if(showEllipsoids){
			for(String ev : ellipsoidValues){
				this.columnNames.add(columnNames.size(), ev);
			}
		} else {
			for(String ev : ellipsoidValues) {
				this.columnNames.remove(ev);
			}
		}
	}
}
