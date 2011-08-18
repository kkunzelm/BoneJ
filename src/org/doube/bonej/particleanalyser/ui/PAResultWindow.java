package org.doube.bonej.particleanalyser.ui;

import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JScrollPane;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.doube.bonej.particleanalyser.Particle;
import org.doube.bonej.particleanalyser.impl.Face;
import org.doube.bonej.particleanalyser.impl.ParticleImpl;
import org.doube.bonej.particleanalyser.impl.ParticleManagerImpl;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PAResultWindow implements Runnable {

	private ParticleManagerImpl pm;
	private ExecutorService pool;

	private JFrame frame;
	private JPanel contentPane;
	private JTable resultTable;
	private ParticleTableModel ptm;
	private JCheckBoxMenuItem chckbxmntmShowSurfaceArea;
	private JCheckBoxMenuItem chckbcmntmShowMaxXYArea;
	private JCheckBoxMenuItem chckbxmntmShowFeretDiameter;
	private JCheckBoxMenuItem chckbxmntmShowEnclosedVolume;
	private JCheckBoxMenuItem chckbxmntmShowEigens;
	private JCheckBoxMenuItem chckbxmntmShowEulerCharacters;
	private JCheckBoxMenuItem chckbxmntmShowThickness;
	private JCheckBoxMenuItem chckbxmntmShowEllipsoids;
	private JTextField textFieldVolMax;
	private JTextField textFieldVolMin;

	private JCheckBox chckbxTop;
	private JCheckBox chckbxNorth;
	private JCheckBox chckbxBottom;
	private JCheckBox chckbxSouth;
	private JCheckBox chckbxEast;
	private JCheckBox chckbxWest;
	private List<Face> excludedEdges = new ArrayList<Face>();
	
	private static final String LINE_BREAK = "\n"; 
    private static final String CELL_BREAK = "\t"; 
    private static final Clipboard CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();
	
	/**
	 * Create the frame.
	 */
	public PAResultWindow(ParticleManagerImpl pm) {
		this.pm = pm;
		this.pool = Executors.newCachedThreadPool();
	}

	/**
	 * @wbp.parser.entryPoint
	 */
	public void run() {
		try {
			initialize();
			frame.pack();
			frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void initialize() {
		frame = new JFrame("Particles");
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				pool.shutdownNow();
				pm.close();
				pool = null;
			}
		});
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setBounds(0, 0, 800, 680);

		ptm = new ParticleTableModel(this.pm);

		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmSaveAs = new JMenuItem("Save as...");
		mntmSaveAs.setEnabled(false);
		mnFile.add(mntmSaveAs);

		JSeparator mntmSep1 = new JSeparator(SwingConstants.HORIZONTAL);
		mnFile.add(mntmSep1);

		JMenuItem mntmClose = new JMenuItem("Close");
		mntmClose.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				e.consume();
				closeWindow();
			}
		});
		mnFile.add(mntmClose);

		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);

		JMenuItem mntmCopy = new JMenuItem("Copy");
		mntmCopy.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				e.consume();
				copySelectedRowsToClipboard();
			}
		});
		mnEdit.add(mntmCopy);

		JSeparator separator = new JSeparator();
		mnEdit.add(separator);

		JMenuItem mntmSelectAll = new JMenuItem("Select All");
		mntmSelectAll.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				e.consume();
				selectAllRowsInTable();
			}
		});
		mnEdit.add(mntmSelectAll);

		JMenu mnView = new JMenu("View");
		menuBar.add(mnView);

		chckbxmntmShowSurfaceArea = new JCheckBoxMenuItem("Show Surface Area");
		chckbxmntmShowSurfaceArea.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				showSurfaceAreaResults(chckbxmntmShowSurfaceArea.isSelected());
			}
		});
		mnView.add(chckbxmntmShowSurfaceArea);
		
		chckbcmntmShowMaxXYArea = new JCheckBoxMenuItem("Show Max XY Areas");
		chckbcmntmShowMaxXYArea.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				showXYAreas(chckbcmntmShowMaxXYArea.isSelected());
			}
		});
		mnView.add(chckbcmntmShowMaxXYArea);

		chckbxmntmShowFeretDiameter = new JCheckBoxMenuItem(
				"Show Feret Diameter");
		chckbxmntmShowFeretDiameter.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				showFeretDiameters(chckbxmntmShowFeretDiameter.isSelected());
			}
		});
		mnView.add(chckbxmntmShowFeretDiameter);

		chckbxmntmShowEnclosedVolume = new JCheckBoxMenuItem(
				"Show Enclosed Volume");
		chckbxmntmShowEnclosedVolume.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				showEnclosedVolume(chckbxmntmShowEnclosedVolume.isSelected());
			}
		});
		mnView.add(chckbxmntmShowEnclosedVolume);

		chckbxmntmShowEigens = new JCheckBoxMenuItem("Show Eigens");
		chckbxmntmShowEigens.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				showEigens(chckbxmntmShowEigens.isSelected());
			}
		});
		mnView.add(chckbxmntmShowEigens);

		chckbxmntmShowEulerCharacters = new JCheckBoxMenuItem(
				"Show Euler Characters");
		chckbxmntmShowEulerCharacters.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				showEulerCharacters(chckbxmntmShowEulerCharacters.isSelected());
			}
		});
		mnView.add(chckbxmntmShowEulerCharacters);

		chckbxmntmShowThickness = new JCheckBoxMenuItem("Show Thickness");
		chckbxmntmShowThickness.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				showThickness(chckbxmntmShowThickness.isSelected());
			}
		});
		mnView.add(chckbxmntmShowThickness);

		chckbxmntmShowEllipsoids = new JCheckBoxMenuItem("Show Ellipsoids");
		chckbxmntmShowEllipsoids.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				showEllipsoids(chckbxmntmShowEllipsoids.isSelected());
			}
		});
		mnView.add(chckbxmntmShowEllipsoids);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));

		resultTable = new JTable();
		resultTable.setModel(ptm);
		resultTable.setFillsViewportHeight(true);
		resultTable.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE
						|| e.getKeyCode() == KeyEvent.VK_DELETE) {
					e.consume();
					removeSelectedRows();
				} else if (e.getKeyCode() == KeyEvent.VK_DOWN
						|| e.getKeyCode() == KeyEvent.VK_UP) {
					e.consume();
					setSelectedParticles();
				}
			}
		});
		
		
		resultTable.addKeyListener(new KeyAdapter() {

			String vers = System.getProperty("os.name").toLowerCase();
			@Override
			public void keyPressed(KeyEvent e) {
				if (vers.indexOf("windows") != -1) {
					if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C) {
						e.consume();
						copySelectedRowsToClipboard();
					}
				} else if (vers.indexOf("mac") != -1) {
					if (e.isMetaDown() && e.getKeyCode() == KeyEvent.VK_C) {
						e.consume();
						copySelectedRowsToClipboard();
					}
				} else if (vers.indexOf("linux") != -1) {
					if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C) {
						e.consume();
						copySelectedRowsToClipboard();
					}
				}
			}
		});

		resultTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				e.consume();
				setSelectedParticles();
			}
		});
		
		resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		JScrollPane centralScrollPane = new JScrollPane(resultTable);
		contentPane.add(centralScrollPane, BorderLayout.CENTER);

		JPanel southPanel = new JPanel();
		southPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));

		contentPane.add(southPanel, BorderLayout.SOUTH);
		frame.setContentPane(contentPane);

		JPanel eastPanel = new JPanel();
		contentPane.add(eastPanel, BorderLayout.EAST);
		eastPanel.setLayout(new GridLayout(4, 0, 0, 0));

		JPanel panel_2 = new JPanel();
		eastPanel.add(panel_2);
		panel_2.setLayout(new GridLayout(5, 0, 0, 0));

		JLabel lbldViewer = new JLabel("3D Viewer");
		lbldViewer.setBorder(UIManager.getBorder("DesktopIcon.border"));
		lbldViewer.setHorizontalAlignment(SwingConstants.CENTER);
		panel_2.add(lbldViewer);

		JButton btnShowSurfacesd = new JButton("Show Surfaces (3D)");
		panel_2.add(btnShowSurfacesd);
		btnShowSurfacesd.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				e.consume();
				showSurface3D();
			}
		});

		JButton btnShowAxesd = new JButton("Show Axes (3D)");
		panel_2.add(btnShowAxesd);
		btnShowAxesd.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				e.consume();
				showAxes3D();
			}
		});

		JButton btnShowCentroidsd = new JButton("Show Centroids (3D)");
		panel_2.add(btnShowCentroidsd);
		btnShowCentroidsd.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				e.consume();
				showCentroids3D();
			}
		});

		JButton btnShowEllipsoidsd = new JButton("Show Ellipsoids (3D)");
		panel_2.add(btnShowEllipsoidsd);
		btnShowEllipsoidsd.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				e.consume();
				showEllipsoids3D();
			}
		});

		JPanel panel_1 = new JPanel();
		eastPanel.add(panel_1);
		panel_1.setLayout(new GridLayout(2, 1, 0, 0));

		JPanel panel_6 = new JPanel();
		panel_1.add(panel_6);
		panel_6.setLayout(new GridLayout(2, 0, 0, 0));

		JSeparator separator_1 = new JSeparator();
		panel_6.add(separator_1);

		JLabel lblEdgeExclusion = new JLabel("Edge Exclusion");
		lblEdgeExclusion.setBorder(UIManager.getBorder("DesktopIcon.border"));
		panel_6.add(lblEdgeExclusion);
		lblEdgeExclusion.setHorizontalAlignment(SwingConstants.CENTER);

		JPanel panel_5 = new JPanel();
		panel_1.add(panel_5);
		panel_5.setLayout(new GridLayout(3, 2, 0, 0));

		chckbxTop = new JCheckBox("Top");
		chckbxTop.setHorizontalAlignment(SwingConstants.CENTER);
		chckbxTop.setSelected(true);
		chckbxTop.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				excludeEdge(chckbxTop.isSelected(), Face.TOP);
			}
		});
		panel_5.add(chckbxTop);

		chckbxBottom = new JCheckBox("Bottom");
		chckbxBottom.setHorizontalAlignment(SwingConstants.CENTER);
		chckbxBottom.setSelected(true);
		chckbxBottom.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				excludeEdge(chckbxBottom.isSelected(), Face.BOTTOM);
			}
		});
		panel_5.add(chckbxBottom);

		chckbxNorth = new JCheckBox("North");
		chckbxNorth.setHorizontalAlignment(SwingConstants.CENTER);
		chckbxNorth.setSelected(true);
		chckbxNorth.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				excludeEdge(chckbxNorth.isSelected(), Face.NORTH);
			}
		});
		panel_5.add(chckbxNorth);

		chckbxSouth = new JCheckBox("South");
		chckbxSouth.setHorizontalAlignment(SwingConstants.CENTER);
		chckbxSouth.setSelected(true);
		chckbxSouth.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				excludeEdge(chckbxSouth.isSelected(), Face.SOUTH);
			}
		});
		panel_5.add(chckbxSouth);

		chckbxEast = new JCheckBox("East");
		chckbxEast.setHorizontalAlignment(SwingConstants.CENTER);
		chckbxEast.setSelected(true);
		chckbxEast.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				excludeEdge(chckbxEast.isSelected(), Face.EAST);
			}
		});
		panel_5.add(chckbxEast);

		chckbxWest = new JCheckBox("West");
		chckbxWest.setHorizontalAlignment(SwingConstants.CENTER);
		chckbxWest.setSelected(true);
		chckbxWest.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				excludeEdge(chckbxWest.isSelected(), Face.WEST);
			}
		});
		panel_5.add(chckbxWest);

		JPanel panel = new JPanel();
		eastPanel.add(panel);
		panel.setLayout(new GridLayout(4, 0, 0, 0));

		JPanel panel_7 = new JPanel();
		panel.add(panel_7);
		panel_7.setLayout(new GridLayout(2, 0, 0, 0));

		JSeparator separator_3 = new JSeparator();
		panel_7.add(separator_3);

		JLabel lblSizeExclusion = new JLabel("Size exclusion");
		lblSizeExclusion.setBorder(UIManager.getBorder("DesktopIcon.border"));
		lblSizeExclusion.setHorizontalAlignment(SwingConstants.CENTER);
		panel_7.add(lblSizeExclusion);

		JPanel panel_3 = new JPanel();
		panel.add(panel_3);

		JLabel lblMinVol = new JLabel("Min Vol.:");
		panel_3.add(lblMinVol);

		textFieldVolMin = new JTextField();
		textFieldVolMin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setMinVolume(Double.valueOf(textFieldVolMin.getText()));
			}
		});
		textFieldVolMin.setHorizontalAlignment(SwingConstants.RIGHT);
		textFieldVolMin.setText(Double.toString(pm.getMinVolume()));
		panel_3.add(textFieldVolMin);
		textFieldVolMin.setColumns(10);

		JPanel panel_4 = new JPanel();

		JLabel lblMaxVol = new JLabel("Max. Vol.:");
		lblMaxVol.setHorizontalAlignment(SwingConstants.LEFT);
		panel_4.add(lblMaxVol);

		textFieldVolMax = new JTextField();
		textFieldVolMax.addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						double result = Double.valueOf(textFieldVolMax.getText());
						setMaxVolume(result);
					}
				});
		textFieldVolMax.setHorizontalAlignment(SwingConstants.RIGHT);
		textFieldVolMax.setText(Double.toString(pm.getMaxVolume()));
		panel_4.add(textFieldVolMax);
		textFieldVolMax.setColumns(10);
		panel.add(panel_4);

		JPanel panel_9 = new JPanel();
		panel.add(panel_9);

		JButton btnResetDefaults = new JButton("Reset Default Sizes");
		btnResetDefaults.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				e.consume();
				resetDefaultSizes();
			}
		});
		panel_9.add(btnResetDefaults);

		JPanel panel_8 = new JPanel();
		eastPanel.add(panel_8);
		panel_8.setLayout(new GridLayout(2, 0, 0, 0));

		JSeparator separator_4 = new JSeparator();
		panel_8.add(separator_4);
		JButton btnResetParticles = new JButton("Reset Particles");
		panel_8.add(btnResetParticles);
		btnResetParticles.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				e.consume();
				resetParticles();
			}
		});
		btnResetParticles.setToolTipText("This resets all removed particles");
	}

	public JFrame getFrame() {
		return this.frame;
	}

	private void closeWindow() {
		frame.dispose();
	}

	public ParticleTableModel getParticleTableModel() {
		return this.ptm;
	}

	private void showSurface3D() {
		SwingWorker<Boolean, Void> sw = new SwingWorker<Boolean, Void>() {

			@Override
			protected Boolean doInBackground() throws Exception {
				pm.displaySurfaces(ParticleManagerImpl.ColorMode.GRADIENT);
				return Boolean.TRUE;
			}

			/* (non-Javadoc)
			 * @see javax.swing.SwingWorker#done()
			 */
			@Override
			protected void done() {
				super.done();
			}
		};
		pool.execute(sw);
	}

	private void showAxes3D() {
		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
			@Override
			protected Boolean doInBackground() throws Exception {
				pm.displayAxes();
				return Boolean.TRUE;
			}
			
			/* (non-Javadoc)
			 * @see javax.swing.SwingWorker#done()
			 */
			@Override
			protected void done() {
				super.done();
			}
		};
		pool.execute(worker);
	}

	private void showEllipsoids3D() {
		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
			@Override
			protected Boolean doInBackground() throws Exception {
				pm.displayEllipsoids();
				return Boolean.TRUE;
			}
			
			/* (non-Javadoc)
			 * @see javax.swing.SwingWorker#done()
			 */
			@Override
			protected void done() {
				super.done();
			}
		};
		pool.execute(worker);
	}

	private void showCentroids3D() {
		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

			@Override
			protected Boolean doInBackground() throws Exception {
				pm.displayCentroids();
				return Boolean.TRUE;
			}
			
			/* (non-Javadoc)
			 * @see javax.swing.SwingWorker#done()
			 */
			@Override
			protected void done() {
				super.done();
			}

		};
		pool.execute(worker);
	}

	private void copySelectedRowsToClipboard() {
		/*TransferHandler th = resultTable.getTransferHandler();
		if (th != null) {
			Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			th.exportToClipboard(resultTable, cb, TransferHandler.COPY);
		}*/
		
		int numCols=resultTable.getSelectedColumnCount(); 
        int numRows=resultTable.getSelectedRowCount(); 
        int[] rowsSelected=resultTable.getSelectedRows(); 
        int[] colsSelected=resultTable.getSelectedColumns(); 
        if (numRows!=rowsSelected[rowsSelected.length-1]-rowsSelected[0]+1 || numRows!=rowsSelected.length || 
                        numCols!=colsSelected[colsSelected.length-1]-colsSelected[0]+1 || numCols!=colsSelected.length) {

                IJ.showMessageWithCancel("Invalid Copy Selection", "Invalid Copy Selection");
                return; 
        } 
        
        StringBuffer excelStr=new StringBuffer(); 
        for (int i=0; i<numRows; i++) { 
                for (int j=0; j<numCols; j++) { 
                        excelStr.append(escape(resultTable.getValueAt(rowsSelected[i], colsSelected[j]))); 
                        if (j<numCols-1) { 
                                excelStr.append(CELL_BREAK); 
                        } 
                } 
                excelStr.append(LINE_BREAK); 
        } 
        
        StringSelection sel  = new StringSelection(excelStr.toString()); 
        CLIPBOARD.setContents(sel, sel); 
	}
	
	 private String escape(Object cell) { 
         return cell.toString().replace(LINE_BREAK, " ").replace(CELL_BREAK, " "); 
	 } 

	private void selectAllRowsInTable() {
		this.resultTable.selectAll();
	}

	private void resetParticles() {
		if (IJ.showMessageWithCancel("Reset particles",
				"If your 3D viewer is open this may take some time to add all the particles\n"
						+ "Are you sure you want to continue?")) {
			pm.resetParticles();
			includeAllEdges(true);
			resetDefaultSizes();
			ptm.fireTableDataChanged();
		} else {
			return;
		}
	}

	public void selectParticle(Particle particle) {
		resultTable
				.changeSelection(
						pm.getVisibleParticles().lastIndexOf(particle), 0,
						false, false);
	}

	public Particle getSelectedParticle() {
		int[] selectedRows = resultTable.getSelectedRows();
		if (selectedRows.length > 1) {
			return null;
		} else if (selectedRows.length < 1) {
			return null;
		} else if (selectedRows[0] > pm.getVisibleParticles().size()) {
			return null;
		} else {
			return pm.getVisibleParticle(selectedRows[0]);
		}
	}

	public void setSelectedParticles() {
		int[] selectedRows = resultTable.getSelectedRows();
		pm.deselectAllParticles();

		if (selectedRows.length > 1) {
			for (int row : selectedRows)
				pm.getVisibleParticle(row).setSelected(true);
		} else if (selectedRows.length < 1) {
			return;
		} else {
			pm.selectParticle(pm.getVisibleParticle(selectedRows[0]));
		}
	}

	public void removeSelectedRows() {
		int[] selectedRows = resultTable.getSelectedRows();
		List<Particle> visibleParticles = pm.getVisibleParticles();

		for (int i = 0; i < selectedRows.length; i++) {
			pm.hideParticle(visibleParticles.get(selectedRows[i]), Particle.HideType.DELETE);
		}
	}

	public void show() {
		this.frame.setVisible(true);
	}

	private void showSurfaceAreaResults(boolean show) {
		ptm.setShowSurfaceArea(show);
		ptm.fireTableStructureChanged();
	}
	
	private void showXYAreas(boolean show) {
		ptm.setShowMaxXYArea(show);
		ptm.fireTableStructureChanged();
	}

	private void showEnclosedVolume(boolean show) {
		ptm.setShowEnclosedVolume(show);
		ptm.fireTableStructureChanged();
	}

	private void showFeretDiameters(boolean show) {
		ptm.setShowFeretDiameter(show);
		ptm.fireTableStructureChanged();
	}

	private void showEigens(boolean show) {
		ptm.setShowEigens(show);
		ptm.fireTableStructureChanged();
	}

	private void showEulerCharacters(boolean show) {
		ptm.setShowEulerCharacters(show);
		ptm.fireTableStructureChanged();
	}

	private void showThickness(boolean show) {
		ptm.setShowThickness(show);
		ptm.fireTableStructureChanged();
	}

	private void showEllipsoids(boolean show) {
		ptm.setShowEllipsoids(show);
		ptm.fireTableStructureChanged();
	}

	private void excludeEdge(final boolean show, final Face edge) {
		if (show) {
			excludedEdges.remove(edge);
		} else {
			excludedEdges.add(edge);
		}
		
		SwingWorker<Boolean, Void> sw = new SwingWorker<Boolean, Void>() {

			@Override
			protected Boolean doInBackground() throws Exception {
				pm.excludeOnEdge(show, edge);
				return Boolean.TRUE;
			}

			/* (non-Javadoc)
			 * @see javax.swing.SwingWorker#done()
			 */
			@Override
			protected void done() {
				super.done();
			}
			
		};
		pool.execute(sw);
	}
	
	public List<Face> getExcludedEdges() {
		return this.excludedEdges;
	}
	
	private void includeAllEdges(boolean selected) {
		chckbxTop.setSelected(selected);
		chckbxBottom.setSelected(selected);
		chckbxNorth.setSelected(selected);
		chckbxSouth.setSelected(selected);
		chckbxEast.setSelected(selected);
		chckbxWest.setSelected(selected);
	}
	
	private void setMaxVolume(final double volume) {
		SwingWorker<Boolean, Void> sw = new SwingWorker<Boolean, Void>() {

			@Override
			protected Boolean doInBackground() throws Exception {
				pm.setMaxVolume(volume);;
				return Boolean.TRUE;
			}
			
			/* (non-Javadoc)
			 * @see javax.swing.SwingWorker#done()
			 */
			@Override
			protected void done() {
				super.done();
			}
		};
		pool.execute(sw);
	}
	
	private void setMinVolume(final double volume) {
		SwingWorker<Boolean, Void> sw = new SwingWorker<Boolean, Void>() {

			@Override
			protected Boolean doInBackground() throws Exception {
				pm.setMinVolume(volume);;
				return Boolean.TRUE;
			}
			
			/* (non-Javadoc)
			 * @see javax.swing.SwingWorker#done()
			 */
			@Override
			protected void done() {
				super.done();
			}
		};
		pool.execute(sw);
	}
	
	private void resetDefaultSizes() {
		setMaxVolume(Double.POSITIVE_INFINITY);
		setMinVolume(0.0);
		textFieldVolMax.setText(Double.toString(Double.POSITIVE_INFINITY));
		textFieldVolMin.setText(Double.toString(0.0));
	}
}
