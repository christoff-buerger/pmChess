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

public final class GUI extends JFrame
{	
	static
	{
		// Initialize fonts:
		final var keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements())
		{
			final var key = keys.nextElement();
			final var value = UIManager.get(key);
			if (value != null && value instanceof javax.swing.plaf.FontUIResource)
			{
				UIManager.put(key, Resources.font_regular);
			}
		}
		
		// Setup cross-platform look:
		try
		{
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		}
		catch (ClassNotFoundException
			| InstantiationException
			| IllegalAccessException
			| UnsupportedLookAndFeelException exception)
		{
			throw new RuntimeException(exception);
		}
		
		// Render text of disabled check boxes normal (e.g., do not dim such):
		UIManager.put("CheckBox.disabledText", UIManager.get("CheckBox.foreground"));
	}
	
	private final JMenuItem white_computer;
	private final JMenuItem black_computer;
	private final MainPanel main_panel;
	
	/*
		Create the GUI and show it.
	 */
	public GUI()
	{
		super("pmChess");
		
		// Setup "About"-window:
		final var about_action = new AboutAction();
		try
		{
			Desktop.getDesktop().setAboutHandler(about_action);
		}
		catch (SecurityException | UnsupportedOperationException exception)
		{
		}
		
		// Set window and taskbar icon:
		setIconImage(Resources.pmChess_icon);
		try
		{
			Taskbar.getTaskbar().setIconImage(Resources.pmChess_icon);
		}
		catch (SecurityException | UnsupportedOperationException exception)
		{
		}
		
		// Setup menu bar:
		final var menu_bar = new JMenuBar();
		final var game_menu = new JMenu("Game"); // Game menu:
		game_menu.setMnemonic(KeyEvent.VK_G);
		game_menu.add(new NewGameAction());
		final var computer_player = new ButtonGroup();
		white_computer = new NonclosingRadioButtonMenuItem("White computer");
		white_computer.setSelected(false);
		black_computer = new NonclosingRadioButtonMenuItem("Black computer");
		black_computer.setSelected(false);
		final var no_computer = new NonclosingRadioButtonMenuItem("No computer");
		no_computer.setSelected(true);
		computer_player.add(white_computer);
		computer_player.add(black_computer);
		computer_player.add(no_computer);
		game_menu.add(white_computer);
		game_menu.add(black_computer);
		game_menu.add(no_computer);
		game_menu.addSeparator();
		game_menu.add(new ExitAction());
		menu_bar.add(game_menu);
		final var help_menu = new JMenu("Help"); // Help menu:
		help_menu.setMnemonic(KeyEvent.VK_H);
		help_menu.add(about_action);
		help_menu.add(new ContactAction());
		menu_bar.add(help_menu);
		setJMenuBar(menu_bar);
		
		// Setup main panel:
		main_panel = new MainPanel();
		setContentPane(main_panel);
		
		// Setup window:
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBackground(Color.gray);
		pack();
		setResizable(false);
		final var screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(
			(int)Math.ceil((screen_size.width - getSize().width) / 2.0f),
			(int)Math.ceil((screen_size.height - getSize().height) / 2.0f));
		setVisible(true);
	}
	
	@Override public void paint(final Graphics graphics)
	{
		super.paint(graphics);
		Resources.configure_rendering(graphics);
	}
	
	/* *********************************** menu  actions *********************************** */
	
	private final class NewGameAction extends AbstractAction
	{
		private NewGameAction()
		{
			super("New game with:");
		}

		@Override public void actionPerformed(final ActionEvent event)
		{
			main_panel.initialize(
				white_computer.isSelected(),
				black_computer.isSelected());
		}
	}
	
	private static final class ExitAction extends AbstractAction
	{
		private ExitAction()
		{
			super("Quit pmChess");
		}

		@Override public void actionPerformed(final ActionEvent event)
		{
			System.exit(0);
		}
	}
	
	private static final class AboutAction extends AbstractAction implements AboutHandler
	{
		private static final AboutFrame about_frame = new AboutFrame();
		
		private AboutAction()
		{
			super("About pmChess");
		}

		@Override public void actionPerformed(final ActionEvent event)
		{
			about_frame.setVisible(true);
		}
		
		@Override public void handleAbout(final AboutEvent event)
		{
			about_frame.setVisible(true);
		}
	}
	
	private static final class ContactAction extends AbstractAction
	{
		private ContactAction()
		{
			super("Contact and feedback");
		}
		
		@Override public void actionPerformed(final ActionEvent event)
		{
			AboutAction.about_frame.show_contact_tab();
		}
	}
	
	private static final class NonclosingRadioButtonMenuItem extends JRadioButtonMenuItem
	{
		private static MenuElement[] path;
		
		{ // Instance initialization:
			getModel().addChangeListener(new ChangeListener()
				{
					@Override public void stateChanged(final ChangeEvent e)
					{
						if (getModel().isArmed() && isShowing())
						{
							path = MenuSelectionManager
								.defaultManager()
								.getSelectedPath();
						}
					}
				});
  		}
		
		private NonclosingRadioButtonMenuItem(final String text)
		{
			super(text);
		}
		
		@Override public void doClick(final int press_time)
		{
			super.doClick(press_time);
			MenuSelectionManager.defaultManager().setSelectedPath(path);
		}
	}
}
