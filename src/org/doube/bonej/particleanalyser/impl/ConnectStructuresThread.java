package org.doube.bonej.particleanalyser.impl;

/**
 * Copyright 2009 2010 Michael Doube
 * Extracted from ParticleCounter by Keith Schulze 2010
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

import ij.ImagePlus;

public class ConnectStructuresThread extends Thread {
	final ImagePlus imp;

	final int thread, nThreads, nChunks, phase;
	
	final String sPhase;

	String chunkString;

	final byte[][] workArray;

	final int[][] particleLabels;

	final int[][] chunkRanges;

	public ConnectStructuresThread(int thread, int nThreads, ImagePlus imp,
			byte[][] workArray, int[][] particleLabels, final int phase, final String sPhase,
			int nChunks, int[][] chunkRanges, String chunkString) {
		this.imp = imp;
		this.thread = thread;
		this.nThreads = nThreads;
		this.workArray = workArray;
		this.particleLabels = particleLabels;
		this.phase = phase;
		this.sPhase = sPhase;
		this.nChunks = nChunks;
		this.chunkRanges = chunkRanges;
		this.chunkString = chunkString;
	}

	public void run() {
		for (int k = this.thread; k < this.nChunks; k += this.nThreads) {
			// assign singleChunkRange for chunk k from chunkRanges
			int[][] singleChunkRange = new int[4][1];
			for (int i = 0; i < 4; i++) {
				singleChunkRange[i][0] = this.chunkRanges[i][k];
			}
			chunkString = ": chunk " + (k + 1) + "/" + nChunks;
			ParticleGetter.connectStructures(this.imp, this.workArray,
					this.particleLabels, this.phase, this.sPhase, this.chunkString, singleChunkRange);
		}
	}
}
