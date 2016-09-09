/*
 Copyright (C) 2012 - 2014 Timo Rantalainen
 Author's email: tjrantal at gmail dot com
 The code is licensed under GPL 3.0 or newer
 */
package org.doube.bonej.pqct.selectroi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Polygon;
/**MouseListener*/
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
/*For enumeration*/
import java.util.ArrayList;

import org.doube.bonej.pqct.selectroi.liveWireEngine.LiveWireCosts;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
/*For creating the output stack images*/
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ScrollbarWithLabel;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

/** Live wire implementation */

/*
 * LiveWire ImageJ plug-in modified from ivus snakes
 * (http://ivussnakes.sourceforge.net/) ImageJ plugin Changed the implementation
 * back to the one suggested in Barret & Mortensen 1997. Interactive live-wire
 * boundary extraction. Medical Image Analysis (1996/7) volume 1, number 4, pp
 * 331-341.
 */

public class LiveWirePlugin
		implements PlugIn, MouseListener, MouseMotionListener, KeyListener, AdjustmentListener, MouseWheelListener {
	ImageCanvas canvas;
	ImagePlus imp;
	ImageWindow imw;
	ScrollbarWithLabel stackScrollbar;

	PolygonRoi roi;
	Polygon polygon;
	ArrayList<Polygon> polygons;
	RoiManager rMan;
	Overlay over;
	int width;
	int height;
	int currentSlice;
	int depth = -1;
	LiveWireCosts lwc;

	/** Implement AdjustmentListener */
	@Override
	public void adjustmentValueChanged(final AdjustmentEvent e) {
		if (currentSlice != e.getValue()) {
			final int previousSlice = currentSlice;
			currentSlice = e.getValue();
			/** Finalize ROI in the previous imp */
			imp.setSlice(previousSlice);
			imp.setPosition(previousSlice);
			finalizeRoi();
			imp.setSlice(currentSlice);
			imp.setPosition(currentSlice);
			initLW();
		}

	}

	/** Implement MouseWheelListener */
	@Override
	public void mouseWheelMoved(final MouseWheelEvent e) {
		final int rotation = e.getWheelRotation();
		if (currentSlice + rotation > 0 && currentSlice + rotation <= depth
				&& currentSlice != currentSlice + rotation) {
			final int previousSlice = currentSlice;
			currentSlice += rotation;
			/** Finalize ROI in the previous imp */
			imp.setSlice(previousSlice);
			imp.setPosition(previousSlice);
			finalizeRoi();
			imp.setSlice(currentSlice);
			imp.setPosition(currentSlice);
			initLW();
		}
	}

	/** Implement the PlugIn interface */
	@Override
	public void run(final String arg) {
		imw = WindowManager.getCurrentWindow();
		canvas = imw.getCanvas();
		imp = WindowManager.getCurrentImage();
		/* Check that an image was open */
		IJ.log("Started liveWire");
		if (WindowManager.getCurrentImage() == null) {
			IJ.noImage();
			return;
		}

		if (WindowManager.getCurrentImage().getImageStackSize() > 1) {
			depth = WindowManager.getCurrentImage().getImageStackSize();
			final Component[] list = imw.getComponents();
			for (int i = 0; i < list.length; ++i) {
				System.out.println("Enumerating components " + list[i].toString());
				if (list[i] instanceof ScrollbarWithLabel) {
					stackScrollbar = ((ScrollbarWithLabel) list[i]);
					stackScrollbar.addAdjustmentListener(this);
					imw.addMouseWheelListener(this);
					currentSlice = stackScrollbar.getValue();
					break;
				}
			}
		}

		/* Get image size and stack depth */
		width = WindowManager.getCurrentImage().getWidth();
		height = WindowManager.getCurrentImage().getHeight();

		IJ.log("Init LW " + width + " h " + height);
		initLW();

		/** Pop up Roi Manager */
		if (RoiManager.getInstance() == null) {
			rMan = new RoiManager();
		} else {
			rMan = RoiManager.getInstance();
		}
		/* Register listener */
		// ImageWindow win = imp.getWindow();
		IJ.log("Add listeners");
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		canvas.addKeyListener(this);

	}

	/** Used to reset livewire when switching to another slice in a stack */
	protected void initLW() {
		/* Init livewire */
		final double[][] pixels = new double[width][height];
		final short[] tempPointer = (short[]) imp.getProcessor().getPixels();
		for (int r = 0; r < height; ++r) {
			for (int c = 0; c < width; ++c) {
				pixels[c][r] = tempPointer[c + r * width];
			}
		}
		lwc = new LiveWireCosts(pixels);
		init();
	}

	/**
	 * Used to reset the polygon, and polygon list used to keep current polygon,
	 * and the history of the current polygon
	 */
	protected void init() {
		/* Init polygon stack for history */
		polygons = new ArrayList<Polygon>();
		polygon = new Polygon();

		/** Add overlay */
		if (imp.getOverlay() == null) {
			over = new Overlay();
			imp.setOverlay(over);
		} else {
			over = imp.getOverlay();
		}

	}

	/* Implement the MouseListener, and MouseMotionListener interfaces */
	@Override
	public void mousePressed(final MouseEvent e) {
		if (e.getClickCount() > 1) {
			finalizeRoi();
			/** Reset polygons */
			init();
		}
	}

	public void finalizeRoi() {
		if (polygon.npoints > 2) {
			// Do not remove the last point, simply connect last point, and
			// initial point
			polygon.addPoint(polygon.xpoints[0], polygon.ypoints[0]);
			/* Create the ROI */
			roi = new PolygonRoi(polygon, Roi.POLYGON);
			/* Set roi color to differentiate ROIs from each other */
			final int colorInd = over.size();
			final float[] colors = new float[] {
					0.5f + 0.5f * ((float) Math.sin(2d * Math.PI * (colorInd - 5) / 10.0)), /* R */
					0.5f + 0.5f * ((float) Math.cos(2d * Math.PI * (colorInd) / 10.0)), /* G */
					0.5f + 0.5f * ((float) Math.sin(2d * Math.PI * (colorInd) / 10.0)) /* B */

			};
			roi.setStrokeColor(new Color(colors[0], colors[1], colors[2]));
			/* Add the roi to an overlay, and set the overlay active */
			imp.setRoi(roi, true);
			over.add(roi);
			/** Add the segmented area to the roiManager */
			rMan.addRoi(roi);
		}
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		/** Ignore second and further clicks of a double click */
		if (e.getClickCount() < 2) {
			final int screenX = e.getX();
			final int screenY = e.getY();
			final int x = canvas.offScreenX(screenX);
			final int y = canvas.offScreenY(screenY);
			/* Backpedal polygon to the previous one if control is pressed */
			if (polygons.size() > 0 && ((e.getModifiersEx() & InputEvent.CTRL_MASK) != 0
					|| (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0)) {
				// Get the previous polygon
				final Polygon tempP = polygons.get(polygons.size() - 1);
				final int[] pX = new int[tempP.npoints];
				final int[] pY = new int[tempP.npoints];
				for (int i = 0; i < tempP.npoints; ++i) {
					pX[i] = tempP.xpoints[i];
					pY[i] = tempP.ypoints[i];
				}
				polygon = new Polygon(pX, pY, pX.length);
				polygons.remove(
						polygons.size() - 1); /* Remove the previous polygon */
				roi = new PolygonRoi(polygon, Roi.POLYLINE);
				imp.setRoi(roi, true);
				lwc.setSeed(pX[pX.length - 1], pY[pX.length - 1]);
			} else {
				/* Add a new segment to the polygon */
				if (polygon.npoints > 0) {
					/* Store a copy of the previous polygon */
					final int[] tX = new int[polygon.npoints];
					final int[] tY = new int[polygon.npoints];
					for (int i = 0; i < polygon.npoints; ++i) {
						tX[i] = polygon.xpoints[i];
						tY[i] = polygon.ypoints[i];
					}
					polygons.add(new Polygon(tX, tY,
							tX.length)); /* Store the previous polygon */
					/* If shift is pressed, add a straight line */
					if ((e.getModifiersEx() & InputEvent.SHIFT_MASK) != 0
							|| (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
						/* Add a straight line */
						polygon.addPoint(x, y);
					} else {
						/* Add a livewire segment */
						int[][] fromSeedToCursor;
						while ((fromSeedToCursor = lwc.returnPath(x, y)) == null) {
						}
						final int[] pX = new int[polygon.npoints + fromSeedToCursor.length];
						final int[] pY = new int[polygon.npoints + fromSeedToCursor.length];
						for (int i = 0; i < polygon.npoints; ++i) {
							pX[i] = polygon.xpoints[i];
							pY[i] = polygon.ypoints[i];
						}
						for (int i = 0; i < fromSeedToCursor.length; ++i) {
							pX[polygon.npoints + i] = fromSeedToCursor[i][0];
							pY[polygon.npoints + i] = fromSeedToCursor[i][1];
						}
						polygon = new Polygon(pX, pY, pX.length);
					}
					/* Get, and set the ROI */
					roi = new PolygonRoi(polygon, Roi.POLYLINE);
					imp.setRoi(roi, true);
				} else {
					polygon.addPoint(x, y);
					lwc.setSeed(x, y);
				}
				lwc.setSeed(x, y);

			}
		}
	}

	/** Implement the MouseListener */
	@Override
	public void mouseDragged(final MouseEvent e) {
	}

	@Override
	public void mouseExited(final MouseEvent e) {
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
	}

	@Override
	public void mouseEntered(final MouseEvent e) {
	}

	/** Visualize the segment to be added in real-time */
	@Override
	public void mouseMoved(final MouseEvent e) {
		if (polygon.npoints > 0) {
			final int screenX = e.getX();
			final int screenY = e.getY();
			final int x = canvas.offScreenX(screenX);
			final int y = canvas.offScreenY(screenY);
			int[] pX;
			int[] pY;
			/** If shift is pressed, visualize adding a straight line */
			if ((e.getModifiersEx() & InputEvent.SHIFT_MASK) != 0
					|| (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
				pX = new int[polygon.npoints + 1];
				pY = new int[polygon.npoints + 1];
				for (int i = 0; i < polygon.npoints; ++i) {
					pX[i] = polygon.xpoints[i];
					pY[i] = polygon.ypoints[i];
				}
				pX[polygon.npoints] = x;
				pY[polygon.npoints] = y;
			} else {
				/* Visualize adding livewire segment */
				int[][] fromSeedToCursor;
				while ((fromSeedToCursor = lwc.returnPath(x, y)) == null) {
				}
				pX = new int[polygon.npoints + fromSeedToCursor.length];
				pY = new int[polygon.npoints + fromSeedToCursor.length];
				for (int i = 0; i < polygon.npoints; ++i) {
					pX[i] = polygon.xpoints[i];
					pY[i] = polygon.ypoints[i];
				}
				for (int i = 0; i < fromSeedToCursor.length; ++i) {
					pX[polygon.npoints + i] = fromSeedToCursor[i][0];
					pY[polygon.npoints + i] = fromSeedToCursor[i][1];
				}
			}
			/** Add the ROI */
			imp.setRoi(new PolygonRoi(pX, pY, pX.length, Roi.POLYLINE), true);
		}
	}

	/** Implement KeyListener */
	/** Invoked when a key has been pressed. */
	@Override
	public void keyPressed(final KeyEvent e) {
		/** Shut down the plug-in */
		if (e.getKeyCode() == KeyEvent.VK_Q || e.getKeyChar() == 'q') {
			/** Remove listeners */
			canvas.removeMouseListener(this);
			canvas.removeMouseMotionListener(this);
			canvas.removeKeyListener(this);
			stackScrollbar.removeAdjustmentListener(this);
			imw.removeMouseWheelListener(this);

			/*
			 * ((ImageCanvas) e.getSource()).removeMouseListener(this);
			 * ((ImageCanvas) e.getSource()).removeMouseMotionListener(this);
			 * ((ImageCanvas) e.getSource()).removeKeyListener(this);
			 */
		}
	}

	/** Invoked when a key has been released. */
	@Override
	public void keyReleased(final KeyEvent e) {

	}

	/** Invoked when a key has been typed. */
	@Override
	public void keyTyped(final KeyEvent e) {

	}

}
