package org.doube.bonej.particleanalyser.impl;

/**
 * 	Enum Face.java
 * 	Copyright 2011 Keith Schulze
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

/**
 * @author Keith Schulze 
 * This enum describes the faces of a volume. It is generally used to describe
 * which face a Particle object is touching.
 */
public enum Face {
	NONE, TOP, BOTTOM, NORTH, EAST, SOUTH, WEST;
}