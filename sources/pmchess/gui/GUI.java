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
	private static final Image taskbar_icon = Resources.loadImage("icons/taskbar.png");
	
	static {
		// Initialize fonts:
		final var keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			final var key = keys.nextElement();
			final var value = UIManager.get(key);
			if (value != null && value instanceof javax.swing.plaf.FontUIResource)
				UIManager.put(key, Resources.font_regular);
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
		
		// Setup "About"-window and icon:
		final var about = new AboutAction();
		try {
			Desktop.getDesktop().setAboutHandler(about);
			Taskbar.getTaskbar().setIconImage(taskbar_icon);
		} catch (SecurityException | UnsupportedOperationException exception) {
			setIconImage(taskbar_icon); // Fallback for older operating systems.
		}
		
		// Setup menu bar:
		final var menuBar = new JMenuBar();
		final var gameMenu = new JMenu("Game"); // Game menu:
		gameMenu.setMnemonic(KeyEvent.VK_G);
		gameMenu.add(new NewGameAction());
		final var compPlayer = new ButtonGroup();
		whiteComp = new NonclosingRadioButtonMenuItem("White computer");
		whiteComp.setSelected(false);
		blackComp = new NonclosingRadioButtonMenuItem("Black computer");
		blackComp.setSelected(false);
		final var noComp = new NonclosingRadioButtonMenuItem("No computer");
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
		final var helpMenu = new JMenu("Help"); // Help menu:
		helpMenu.setMnemonic(KeyEvent.VK_H);
		helpMenu.add(about);
		helpMenu.add(new ContactAction());
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
		final var screen_size = Toolkit.getDefaultToolkit().getScreenSize();
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

		@Override public void actionPerformed(final ActionEvent event) {
			gamePanel.initialize(whiteComp.isSelected(), blackComp.isSelected());
		}
	}
	
	private static final class ExitAction extends AbstractAction {
		private ExitAction() {
			super("Quit pmChess");
		}

		@Override public void actionPerformed(final ActionEvent event) {
			System.exit(0);
		}
	}
	
	private static final class AboutAction extends AbstractAction implements AboutHandler {
		private static final AboutFrame about_frame = new AboutFrame();
		
		private AboutAction() {
			super("About pmChess");
		}

		@Override public void actionPerformed(final ActionEvent event) {
			about_frame.setVisible(true);
		}
		
		@Override public void handleAbout(final AboutEvent event) {
			about_frame.setVisible(true);
		}
	}
	
	private static final class ContactAction extends AbstractAction {
		private ContactAction() {
			super("Contact and feedback");
		}
		
		@Override public void actionPerformed(final ActionEvent event) {
			AboutAction.about_frame.showContactTab();
		}
	}
	
	private static final class NonclosingRadioButtonMenuItem extends JRadioButtonMenuItem {
		private static MenuElement[] path;
		{ // Instance initialization:
			getModel().addChangeListener(new ChangeListener() {
				@Override public void stateChanged(ChangeEvent e) {
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
		
		@Override public void doClick(int pressTime) {
			super.doClick(pressTime);
			MenuSelectionManager.defaultManager().setSelectedPath(path);
		}
	}
}
