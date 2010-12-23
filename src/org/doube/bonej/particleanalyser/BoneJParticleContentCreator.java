/**
 * BoneJContentCreator.java Copyright 2010 Keith Schulze
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

import customnode.CustomMesh;
import customnode.CustomMeshNode;
import customnode.CustomMultiMesh;
import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.UniverseSettings;

/**
 * @author Keith
 *
 */
public class BoneJParticleContentCreator {

	public static Content createContent(CustomMesh mesh, Particle particle, ParticleManager pm) {
		return createContent(mesh, particle, pm, -1);
	}

	public static Content createContent(CustomMesh mesh, Particle particle, ParticleManager pm, int tp) {
		BoneJParticleContent c = new BoneJParticleContent(particle.getName(), tp, particle, pm);
		ContentInstant content = c.getInstant(tp);
		content.setColor(mesh.getColor());
		content.setTransparency(mesh.getTransparency());
		content.setShaded(mesh.isShaded());
		content.showCoordinateSystem(
			UniverseSettings.showLocalCoordinateSystemsByDefault);
		content.display(new CustomMeshNode(mesh));
		return c;
	}
	
	public static Content createContent(CustomMultiMesh node, String name, Particle particle, ParticleManager pm) {
		return createContent(node, name,  particle, pm, -1);
	}

	public static Content createContent(CustomMultiMesh node, String name, Particle particle, ParticleManager pm, int tp) {
		BoneJParticleContent c = new BoneJParticleContent(name, tp, particle, pm);
		ContentInstant content = c.getInstant(tp);
		content.setColor(null);
		content.setTransparency(0f);
		content.setShaded(false);
		content.showCoordinateSystem(
			UniverseSettings.showLocalCoordinateSystemsByDefault);
		content.display(node);
		return c;
	}
}
