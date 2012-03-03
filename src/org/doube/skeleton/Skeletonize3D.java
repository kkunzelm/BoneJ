package org.doube.skeleton;

import org.doube.util.ImageCheck;

import ij.IJ;
import ij.plugin.PlugIn;

/**
 * Main class.
 * This class is a plugin for the ImageJ interface for 2D and 3D thinning 
 * (skeletonization) of binary images (2D/3D).
 *
 * <p>
 * This work is an implementation by Ignacio Arganda-Carreras of the 3D thinning
 * algorithm from Lee et al.
 * </p>
 */
public class Skeletonize3D implements PlugIn {
	
	public void run(String arg) {
		if (!ImageCheck.checkEnvironment())
			return;
	
		IJ.run("Skeletonize3D_");
	}
}
