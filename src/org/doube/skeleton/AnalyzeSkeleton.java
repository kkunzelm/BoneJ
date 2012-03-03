package org.doube.skeleton;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import org.doube.util.ImageCheck;

import skeleton_analysis.AnalyzeSkeleton_;

/**
 * Wrapper for AnalyzeSkeleton_ plugin for ImageJ(C) and Fiji. This plugin
 * merely presents the API of the original JAR as a migratory step towards
 * leaving the code out of BoneJ altogheter, and only providing a dependency
 * 
 */
public class AnalyzeSkeleton implements PlugInFilter {
	private AnalyzeSkeleton_ analyser;

	public int setup(String arg, ImagePlus imp) {
		return analyser.setup(arg, imp);
	}

	/**
	 * Process the image: tag skeleton and show results.
	 */
	public void run(ImageProcessor ip) {
		if (!ImageCheck.checkEnvironment())
			return;
		analyser.run(ip);
	}

	/**
	 * This method is intended for non-interactively using this plugin.
	 * <p>
	 * 
	 * @param pruneIndex
	 *            The pruneIndex, as asked by the initial gui dialog.
	 * @param pruneEnds
	 *            flag to prune end-point-ending branches
	 * @param shortPath
	 *            flag to calculate the longest shortest path
	 * @param origIP
	 *            original input image
	 * @param silent
	 * @param verbose
	 *            flag to display running information
	 */
	public skeleton_analysis.SkeletonResult run(int pruneIndex,
			boolean pruneEnds, boolean shortPath, ImagePlus origIP,
			boolean silent, boolean verbose) {
		return analyser.run(pruneIndex, pruneEnds, shortPath, origIP, silent,
				verbose);
	}

	/**
	 * Get the graphs of the current skeletons
	 * 
	 * @return array of graphs (one per tree/skeleton)
	 */
	public skeleton_analysis.Graph[] getGraphs() {
		return analyser.getGraphs();
	}

	/**
	 * A simpler standalone running method, for analyzation without pruning or
	 * showing images.
	 * <p>
	 * This one just calls run(AnalyzeSkeleton_.NONE, false, null, true, false)
	 */
	public skeleton_analysis.SkeletonResult run() {
		return analyser.run(AnalyzeSkeleton_.NONE, false, false, null, true, false);
	}

	/**
	 * Process skeleton: tag image, mark trees and visit.
	 * 
	 * @param inputImage2
	 *            input skeleton image to process
	 */
	public void processSkeleton(ImageStack inputImage2) {
		analyser.processSkeleton(inputImage2);
	}

	/**
	 * Returns one of the two result images in an ImageStack object.
	 * 
	 * @param longestShortestPath
	 *            Get the tagged longest shortest paths instead of the standard
	 *            tagged image
	 * 
	 * @return The results image with a tagged skeleton
	 */
	public ImageStack getResultImage(boolean longestShortestPath) {
		return analyser.getResultImage(longestShortestPath);
	}

	/**
	 * Find vertex in an array given a specific vertex point.
	 * 
	 * @param vertex
	 *            array of search
	 * @param p
	 *            vertex point
	 * @return vertex containing that point
	 */
	public skeleton_analysis.Vertex findPointVertex(
			skeleton_analysis.Vertex[] vertex, skeleton_analysis.Point p) {
		return analyser.findPointVertex(vertex, p);
	}

	/**
	 * Get average neighborhood pixel value of a given point.
	 * 
	 * @param image
	 *            input image
	 * @param p
	 *            image coordinates
	 * @param x_offset
	 *            x- neighborhood offset
	 * @param y_offset
	 *            y- neighborhood offset
	 * @param z_offset
	 *            z- neighborhood offset
	 * @return average neighborhood pixel value
	 */
	public static double getAverageNeighborhoodValue(final ImageStack image,
			final skeleton_analysis.Point p, final int x_offset,
			final int y_offset, final int z_offset) {
		return skeleton_analysis.AnalyzeSkeleton_.getAverageNeighborhoodValue(
				image, p, x_offset, y_offset, z_offset);
	}

	/**
	 * Get neighborhood of a pixel in a 3D image (0 border conditions).
	 * 
	 * @param image
	 *            3D image (ImageStack)
	 * @param p
	 *            point coordinates
	 * @param x_offset
	 *            x- neighborhood offset
	 * @param y_offset
	 *            y- neighborhood offset
	 * @param z_offset
	 *            z- neighborhood offset
	 * @return corresponding neighborhood (0 if out of image)
	 */
	public static byte[] getNeighborhood(final ImageStack image,
			final skeleton_analysis.Point p, final int x_offset,
			final int y_offset, final int z_offset) {
		return AnalyzeSkeleton_.getNeighborhood(image, p, x_offset, y_offset,
				z_offset);
	}

	/**
	 * Get pixel in 3D image (0 border conditions)
	 * 
	 * @param image
	 *            3D image
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate (in image stacks the indexes start at 1)
	 * @return corresponding pixel (0 if out of image)
	 */
	public static byte getPixel(final ImageStack image, final int x,
			final int y, final int z) {
		return AnalyzeSkeleton_.getPixel(image, x, y, z);
	}
}
