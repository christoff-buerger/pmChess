/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.gui;

import java.util.*;

import java.io.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.desktop.*;

import javax.swing.*;
import javax.swing.event.*;

public final class GUI extends JFrame {
	protected static final Font font_plain = loadFont("OpenSans-Regular.ttf");
	protected static final Font font_italic = loadFont("OpenSans-Italic.ttf");
	protected static final Font font_bold = loadFont("OpenSans-Bold.ttf");
	private static final Image taskbar_icon = GUI.loadImage("icons/taskbar.png");
	
	private static Font loadFont(final String font_name) {
		try {
			final Font font = Font.createFont(
				Font.TRUETYPE_FONT,
				GUI.class.getResourceAsStream("fonts/" + font_name));
			return font.deriveFont(14f);
		} catch (IOException | FontFormatException e) {
			throw new RuntimeException("Failed to load font " + font_name + ".");
		}
	}
	
	protected static Image loadImage(final String image_name) {
		final java.net.URL image_url = GamePanel.class.getResource(image_name);
		if (image_url != null) {
			return Toolkit.getDefaultToolkit().getImage(image_url);
		} else {
			throw new RuntimeException("Failed to load image " + image_name + ".");
		}
	}
	
	static {
		// Initialize fonts:
		final Enumeration keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			final Object key = keys.nextElement();
			final Object value = UIManager.get(key);
			if (value != null && value instanceof javax.swing.plaf.FontUIResource)
				UIManager.put(key, font_plain);
		}
		// Setup cross-platform look:
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException |
			InstantiationException |
			IllegalAccessException |
			UnsupportedLookAndFeelException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private final JMenuItem whiteComp;
	private final JMenuItem blackComp;
	private final GamePanel gamePanel;
	
	/*
		Create the GUI and show it.
	 */
	public GUI() {
		super("pmChess");
		
		// Setup icon and "About" window:
		final AboutAction about = new AboutAction();
		Taskbar.getTaskbar().setIconImage(taskbar_icon);
		Desktop.getDesktop().setAboutHandler(about);
		
		// Setup menu bar:
		final JMenuBar menuBar = new JMenuBar();
		final JMenu gameMenu = new JMenu("Game"); // Game menu:
		gameMenu.setMnemonic(KeyEvent.VK_G);
		gameMenu.add(new NewGameAction());
		final ButtonGroup compPlayer = new ButtonGroup();
		whiteComp = new NonclosingRadioButtonMenuItem("White computer");
		whiteComp.setSelected(false);
		blackComp = new NonclosingRadioButtonMenuItem("Black computer");
		blackComp.setSelected(false);
		final JMenuItem noComp = new NonclosingRadioButtonMenuItem("No computer");
		noComp.setSelected(true);
		compPlayer.add(whiteComp);
		compPlayer.add(blackComp);
		compPlayer.add(noComp);
		gameMenu.add(whiteComp);
		gameMenu.add(blackComp);
		gameMenu.add(noComp);
		gameMenu.addSeparator();
		gameMenu.add(new ExitAction());
		menuBar.add(gameMenu);
		final JMenu helpMenu = new JMenu("Help"); // Help menu:
		helpMenu.setMnemonic(KeyEvent.VK_H);
		helpMenu.add(about);
		menuBar.add(helpMenu);
		setJMenuBar(menuBar);
		
		// Setup main panel:
		gamePanel = new GamePanel();
		setContentPane(gamePanel);
		
		// Setup window and display it:
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBackground(Color.gray);
		pack();
		setResizable(false);
		final Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(
			(screen_size.width - getSize().width) / 2,
			(screen_size.height - getSize().height) / 2);
		setVisible(true);
	}
	
	/* ************************************ menu actions ************************************ */
	
	private final class NewGameAction extends AbstractAction {
		private NewGameAction() {
			super("New game with:");
		}

		public void actionPerformed(final ActionEvent event) {
			gamePanel.initialize(whiteComp.isSelected(), blackComp.isSelected());
		}
	}
	
	private static final class ExitAction extends AbstractAction {
		private ExitAction() {
			super("Quit pmChess");
		}

		public void actionPerformed(final ActionEvent event) {
			System.exit(0);
		}
	}
	
	private static final class AboutAction extends AbstractAction implements AboutHandler {
		private static final AboutFrame info = new AboutFrame();
		
		private AboutAction() {
			super("About pmChess");
		}

		public void actionPerformed(final ActionEvent event) {
			info.setVisible(true);
		}
		
		public void handleAbout(final AboutEvent event) {
			info.setVisible(true);
		}
	}
	
	private static final class NonclosingRadioButtonMenuItem extends JRadioButtonMenuItem {
		private static MenuElement[] path;
		{ // Instance initialization:
			getModel().addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (getModel().isArmed() && isShowing()) {
						path =  MenuSelectionManager.
							defaultManager().
							getSelectedPath();
					}
				}});
  		}
		
		private NonclosingRadioButtonMenuItem(final String text) {
			super(text);
		}
		
		public void doClick(int pressTime) {
			super.doClick(pressTime);
			MenuSelectionManager.defaultManager().setSelectedPath(path);
		}
	}
}
