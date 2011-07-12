/**
 * ParticleContentImpl.java Copyright 2010 Keith Schulze
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

import java.awt.Color;
import java.util.TreeMap;

import javax.vecmath.Color3f;

import org.doube.bonej.particleanalyser.Particle;

import ij3d.Content;
import ij3d.ContentInstant;

/**
 * @author Keith Schulze
 *
 */
public class ParticleContentImpl extends Content {

	private Particle particle;
	private ParticleManagerImpl pm;
	private Color3f color;
	private static final Color3f selectedColor = new Color3f(new Color(165, 214, 236));
	
	public ParticleContentImpl(String name, int tp, Particle particle, ParticleManagerImpl pm, Color3f color) {
		super(name, tp);
		this.particle = particle;
		this.pm = pm;
		this.color = color;
	}

	public ParticleContentImpl(String name, Particle particle, ParticleManagerImpl pm, Color3f color) {
		super(name);
		this.particle = particle;
		this.pm = pm;
		this.color = color;
	}

	public ParticleContentImpl(String name, TreeMap<Integer, ContentInstant> contents, 
			Particle particle, ParticleManagerImpl pm, Color3f color) {
		super(name, contents);
		this.particle = particle;
		this.pm = pm;
		this.color = color;
	}

	/* (non-Javadoc)
	 * @see ij3d.Content#contentRemoved(ij3d.Content)
	 */
	@Override
	public void contentRemoved(Content c) {
		super.contentRemoved(c);
		if (!pm.isClosed() && c.getName() == particle.getName() && particle.isVisible())
			pm.hideParticle(particle, Particle.HideType.DELETE);
	}
	
	/* (non-Javadoc)
	 * @see ij3d.Content#contentSelected(ij3d.Content)
	 */
	@Override
	public void contentSelected(Content c) {
		super.contentSelected(c);
		if (!pm.isClosed() && c != null && c.getName() == particle.getName()) {
			pm.deselectAllParticles();
			pm.selectParticle(particle);
		}
	}

	/* (non-Javadoc)
	 * @see ij3d.Content#setSelected(boolean)
	 */
	@Override
	public void setSelected(boolean selected) {
		super.setSelected(selected);
		if (selected) {
			super.setColor(selectedColor);
		} else {
			super.setColor(color);
		}
	}
}
