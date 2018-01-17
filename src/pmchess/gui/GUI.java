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

import javax.swing.*;
import javax.swing.event.*;

public final class GUI extends JFrame {
	public static final Font font_plain = loadFont("OpenSans-Regular.ttf");
	public static final Font font_italic = loadFont("OpenSans-Italic.ttf");
	public static final Font font_bold = loadFont("OpenSans-Bold.ttf");
	private static final Image icon = GUI.loadImage("icons/icon-taskbar.png");
	
	private static Font loadFont(final String fontName) {
		try {
			final Font font = Font.createFont(
				Font.TRUETYPE_FONT,
				GUI.class.getResourceAsStream("fonts/" + fontName));
			return font.deriveFont(14f);
		} catch (IOException | FontFormatException e) {
			throw new RuntimeException("Failed to load font.");
		}
	}
	
	protected static Image loadImage(final String path) {
		final java.net.URL imgURL = GamePanel.class.getResource(path);
		if (imgURL != null) {
			return Toolkit.getDefaultToolkit().getImage(imgURL);
		} else {
			throw new RuntimeException("Failed to load image " + path + ".");
		}
	}
		
	static { // Initialize fonts:
		final Enumeration keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			final Object key = keys.nextElement();
			final Object value = UIManager.get(key);
			if (value != null && value instanceof javax.swing.plaf.FontUIResource)
				UIManager.put(key, font_plain);
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
		
		// Setup icon:
		final String os = System.getProperty("os.name");
		if (os.equals("Mac OS X")) try {
			final Class<?> cls = Class.forName("com.apple.eawt.Application");
			final Object app = cls.getMethod("getApplication").invoke(null);
			app.getClass().getMethod("setDockIconImage", Image.class).invoke(app, icon);
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
			java.lang.reflect.InvocationTargetException e)
		{
			throw new RuntimeException(e);
		} else {
			setIconImage(icon);
		}
		
		// Setup the window:
		final Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBackground(Color.gray);
		setSize(325, 520);
		setLocation(
			(d.width - getSize().width) / 2,
			(d.height - getSize().height) / 2);
		
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
		gameMenu.add(new UndoMoveAction());
		gameMenu.addSeparator();
		gameMenu.add(new ExitAction());
		menuBar.add(gameMenu);
		final JMenu helpMenu = new JMenu("Help"); // Help menu:
		helpMenu.setMnemonic(KeyEvent.VK_H);
		helpMenu.add(new AboutAction());
		menuBar.add(helpMenu);
		setJMenuBar(menuBar);
		
		// Setup main panel:
		gamePanel = new GamePanel();
		setContentPane(gamePanel);
		
		// Display window:
		setResizable(false);
		setVisible(true);
	}
	
	/* ************************************ menu actions ************************************ */
	
	private final class UndoMoveAction extends AbstractAction {
		private UndoMoveAction() {
			super("Undo last move");
		}

		public void actionPerformed(final ActionEvent event) {
			gamePanel.undo();
		}
	}
	
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
			super("Exit");
		}

		public void actionPerformed(final ActionEvent event) {
			System.exit(0);
		}
	}
	
	private static final class AboutAction extends AbstractAction {
		private static final AboutFrame info = new AboutFrame();
		
		private AboutAction() {
			super("About pmChess");
		}

		public void actionPerformed(final ActionEvent event) {
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
		
		public NonclosingRadioButtonMenuItem(final String text) {
			super(text);
		}
		
		public void doClick(int pressTime) {
			super.doClick(pressTime);
			MenuSelectionManager.defaultManager().setSelectedPath(path);
		}
	}
}
