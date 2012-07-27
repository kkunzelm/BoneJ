package org.bonej;

import imagej.ext.plugin.ImageJPlugin;
import imagej.ext.plugin.Parameter;
import imagej.ext.plugin.Plugin;
import imagej.platform.DefaultPlatformService;
import imagej.util.Log;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

@Plugin(menuPath = "Help>About Plugins>BoneJ...")
public class Help implements ImageJPlugin {

	/**
	 * BoneJ version
	 */
	public static final String bonejVersion = "2.0.0-SNAPSHOT";

	@Parameter
	private DefaultPlatformService platformService;

	public void run() {
		showAbout();
	}

	private void showAbout() {
		JEditorPane htmlPane = new JEditorPane(
				"text/html",
				"<html>\n"
						+ "  <body>\n"
						+ "<p><b>BoneJ version "
						+ bonejVersion
						+ "</b>	</p>"
						+ "<p>BoneJ is an ImageJ plugin designed for (but not limited to) bone image analysis.</p>"
						+ "<p>User and developer documentation can be found at <a href=http://bonej.org/>bonej.org</a></p>"
						+ "\n" + "  </body>\n" + "</html>");
		htmlPane.setEditable(false);
		htmlPane.setOpaque(false);
		htmlPane.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType()))
					try {
						platformService.open(new URL(e.getURL().toString()));
					} catch (MalformedURLException e1) {
						Log.error(e1);
					} catch (IOException e1) {
						Log.error(e1);
					}
				;
			}
		});

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(htmlPane, BorderLayout.CENTER);

		final JFrame frame = new JFrame("About BoneJ...");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
	}
}
