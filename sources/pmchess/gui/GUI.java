/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.gui;

import java.io.*;

import java.nio.file.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.desktop.*;

import javax.swing.*;
import javax.swing.event.*;

public final class GUI extends JFrame
{
	private static final GraphicsConfiguration graphics_configuration;
	
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
		
		// Retrieve default screen configuration:
		graphics_configuration = GraphicsEnvironment
			.getLocalGraphicsEnvironment()
			.getDefaultScreenDevice()
			.getDefaultConfiguration();
	}
	
	private final JMenuItem white_computer;
	private final JMenuItem black_computer;
	private final MainPanel main_panel;
	
	/*
		Create the GUI and show it.
	 */
	public GUI()
	{
		super("pmChess", GUI.graphics_configuration);
		
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
		setLocationRelativeTo(null); // center window
		
		// Check window size fits:
		final var graphics_bounds = GUI.graphics_configuration.getBounds();
		final var screen_insets = Toolkit.getDefaultToolkit().getScreenInsets(
			GUI.graphics_configuration);
		final var max_width = (int) Math.floor(0.97f * (float)(
			graphics_bounds.getWidth() - screen_insets.left - screen_insets.right));
		final var max_height = (int) Math.floor(0.97f * (float)(
			graphics_bounds.getHeight() - screen_insets.top - screen_insets.bottom));
		if (getWidth() >= max_width || getHeight() >= max_height)
		{
			final var autoscale = (int) Math.floor(
				  0.98f
				* Math.min(
					  ((float) max_width) / ((float) getWidth())
					, ((float) max_height) / ((float) getHeight()))
				* ((float) Resources.base_scale_in_percent()));
			UIManager.put(
				  "OptionPane.messageFont"
				, Resources.font_regular.deriveFont(Resources.base_scale_default));
			UIManager.put(
				  "OptionPane.buttonFont"
				, Resources.font_regular.deriveFont(Resources.base_scale_default));
			final var selection = JOptionPane.showOptionDialog(
				  this
				, String.format(
					"""
					<html>
					The configured scale of pmChess exceeds the resolution of<br>
					the primary screen. The pmChess window likely will not fit.
					<br>
					<br>
					The current scale configuration is %d%%.<br>
					The recommended maximum is %d%%.
					<br>
					<br>
					If you continue, pmChess is started with the configured scale.<br>
					You can also reset to the default scale of 100%%.<br>
					Or autoscale to the recommeded maximum.
					<br>
					<br>
					Reset and autoscale will quit pmChess after updating the scale.<br>
					The new scale will be applied next time you start pmChess.
					</html>"""
					, Resources.base_scale_in_percent()
					, autoscale)
				, "Warning: Scale exceeds primary screen resolution"
				, JOptionPane.DEFAULT_OPTION
				, JOptionPane.WARNING_MESSAGE
				, null
				, new String[]{ "Reset scale", "Autoscale", "Continue" }
				, "Reset scale");
			switch (selection)
			{
			case 0: // Reset scale:
				Resources.write_base_scale_configuration(100);
				exit();
				return;
			case 1: // Autoscale:
				Resources.write_base_scale_configuration(autoscale);
				exit();
				return;
			default: // Continue:
				UIManager.put("OptionPane.messageFont", Resources.font_regular);
				UIManager.put("OptionPane.buttonFont", Resources.font_regular);
			}
		}
		setVisible(true);
	}
	
	protected void exit()
	{
		for (final var frame : Frame.getFrames())
		{
			frame.dispose();
		}
		getToolkit().getSystemEventQueue().postEvent(new WindowEvent(
			  this
			, WindowEvent.WINDOW_CLOSING)); // EXIT_ON_CLOSE => clean System.exit(0);
	}
	
	public boolean save_game(final String game_file)
	{
		try (final var os = new ObjectOutputStream(
			new FileOutputStream(game_file, false)))
		{
			main_panel.serialize_game(os);
			return true;
		}
		catch (final Exception e1)
		{
			try
			{
				Files.deleteIfExists(Paths.get(game_file));
			}
			catch (final Exception e2)
			{
			}
			return false;
		}
	}
	
	public void load_game(final String game_file)
	{
		try {
			if (Files.exists(Paths.get(game_file)))
			try (final var is = new ObjectInputStream(
				new FileInputStream(game_file)))
			{
				main_panel.deserialize_game(is);
			}
		}
		catch (final Exception e)
		{
			main_panel.initialize(false, false, new MainPanel.InitializationStep[]{});
		}
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
				  white_computer.isSelected()
				, black_computer.isSelected()
				, new MainPanel.InitializationStep[]{});
		}
	}
	
	private final class ExitAction extends AbstractAction
	{
		private ExitAction()
		{
			super("Quit pmChess");
		}

		@Override public void actionPerformed(final ActionEvent event)
		{
			GUI.this.exit();
		}
	}
	
	private static final class AboutAction extends AbstractAction implements AboutHandler
	{
		private static final AboutFrame about_frame = new AboutFrame(
			GUI.graphics_configuration);
		
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
