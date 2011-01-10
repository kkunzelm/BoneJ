/**
 * BoneJContent.java Copyright 2010 Keith Schulze
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

import java.util.TreeMap;

import ij3d.Content;
import ij3d.ContentInstant;

/**
 * @author Keith Schulze
 *
 */
public class BoneJParticleContent extends Content {

	private Particle particle;
	private ParticleManager pm;
	
	public BoneJParticleContent(String name, int tp, Particle particle, ParticleManager pm) {
		super(name, tp);
		this.particle = particle;
		this.pm = pm;
	}

	public BoneJParticleContent(String name, Particle particle, ParticleManager pm) {
		super(name);
		this.particle = particle;
		this.pm = pm;
	}

	public BoneJParticleContent(String name, TreeMap<Integer, ContentInstant> contents, 
			Particle particle, ParticleManager pm) {
		super(name, contents);
		this.particle = particle;
		this.pm = pm;
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

}
