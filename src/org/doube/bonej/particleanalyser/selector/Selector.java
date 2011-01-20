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
package org.doube.bonej.particleanalyser.selector;

import java.util.List;

import javax.swing.JPanel;

import org.doube.bonej.particleanalyser.ParticleImpl;

/**
 * @author Keith Schulze
 *
 * <p>
 * The objective of this interface is to provide a means by which 3rd party developers can write
 * methods for selecting various particles based on their own algorithm.
 * </p>
 * 
 * <p>
 * Implementations of this interface are required to provide a UI which is integrated into the 
 * main UI of the ParticleAnalyser. 
 * </p> 
 */
public interface Selector {

	/**
	 * Generate UI element for this Selector
	 * @return JPanel containing UI.
	 */
	public JPanel getUI();
	
	/**
	 * Specifies which from the list of visible particles is to be selected.
	 * @param visible particles
	 */
	public void selectParticles(List<ParticleImpl> particles);
}
