/*
	This program and the accompanying materials are made available under the terms of the MIT
	license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.gui;

import java.io.*;

import java.net.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public final class AboutFrame extends JFrame
{
	protected static final int frame_x_size =
		(Resources.base_scale_in_percent() * 600) / 100;
	protected static final int frame_y_size =
		(Resources.base_scale_in_percent() * 580) / 100;
	
	private static final Image logo =
		Resources.load_image("logo/logo-animated.gif");
	
	private final JTabbedPane tabs =
		new JTabbedPane();
	
	protected void show_contact_tab()
	{
		tabs.setSelectedIndex(2);
		setVisible(true);
	}
	
	protected AboutFrame(final GraphicsConfiguration graphics_configuration)
	{
		super("About pmChess", graphics_configuration);
		
		// Set window and taskbar icon:
		setIconImage(Resources.pmChess_icon);
		try
		{
			Taskbar.getTaskbar().setIconImage(Resources.pmChess_icon);
		}
		catch (SecurityException | UnsupportedOperationException exception)
		{
		}
		
		// Compute component sizes:
		final var text_height =
			(new FontMetrics(Resources.font_regular) {}).getHeight();
		final var border_size =
			(int) Math.ceil(2 * text_height / 3.0f);
		final var frame_insets =
			Resources.compute_insets();
		final var panel_x_size =
			frame_x_size - (frame_insets.left + frame_insets.right);
		final var panel_y_size =
			frame_y_size - (frame_insets.top + frame_insets.bottom);
		final var header_x_size =
			panel_x_size;
		final var header_y_size =
			(int)Math.ceil(2.5f * text_height) + border_size;
		final var tabs_x_size =
			panel_x_size;
		final var tabs_y_size =
			panel_y_size - header_y_size;
		final var tab_x_size =
			tabs_x_size - 2 * border_size;
		final var tab_y_size =
			tabs_y_size - text_height - 3 * border_size;
		final var licenses_combo_box_x_size =
			tab_x_size;
		final var licenses_combo_box_y_size =
			text_height + 2 * border_size;
		final var licenses_scroll_pane_x_size =
			tab_x_size;
		final var licenses_scroll_pane_y_size =
			tab_y_size - licenses_combo_box_y_size - border_size;
		final var contact_row_x_size =
			tab_x_size;
		final var contact_row_y_size =
			text_height + border_size;
		final var contact_rows_x_size =
			contact_row_x_size;
		final var contact_rows_y_size =
			3 * contact_row_y_size + 4 * border_size;
		final var contact_send_button_x_size =
			tab_x_size;
		final var contact_send_button_y_size =
			(int)Math.ceil(1.5f * contact_row_y_size);
		final var contact_send_button_panel_x_size =
			contact_send_button_x_size;
		final var contact_send_button_panel_y_size =
			contact_send_button_y_size + (int)Math.ceil(0.5f * border_size);
		final var contact_description_scroll_pane_x_size =
			tab_x_size;
		final var contact_description_scroll_pane_y_size =
			(int)Math.floor(
				0.45f * (tab_y_size
				- contact_rows_y_size
				- contact_send_button_panel_y_size));
		final var contact_message_scroll_pane_x_size =
			tab_x_size;
		final var contact_message_scroll_pane_y_size =
			tab_y_size
			- contact_rows_y_size
			- contact_send_button_panel_y_size
			- contact_description_scroll_pane_y_size;
		
		final var tab_dimension =
			new Dimension(tab_x_size, tab_y_size);
		final var contact_row_dimension =
			new Dimension(contact_row_x_size, contact_row_y_size);
		final var contact_row_label_dimension =
			new Dimension((int)Math.floor(0.3f * contact_row_x_size), contact_row_y_size);
		final var contact_row_field_dimension =
			new Dimension((int)Math.floor(0.7f * contact_row_x_size), contact_row_y_size);
		
		// Header:
		final var header_1 =
			new JLabel(pmchess.pmChess.about[0]);
		header_1.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		final var header_2 =
			new JLabel(pmchess.pmChess.about[1]);
		header_2.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		final var header_panel =
			new JPanel();
		final var header_panel_dimension =
			new Dimension(header_x_size, header_y_size);
		header_panel.setMaximumSize(header_panel_dimension);
		header_panel.setMinimumSize(header_panel_dimension);
		header_panel.setPreferredSize(header_panel_dimension);
		header_panel.setAlignmentX(Component.CENTER_ALIGNMENT);
		header_panel.setLayout(new BoxLayout(
			  header_panel
			, BoxLayout.Y_AXIS));
		header_panel.add(Box.createVerticalGlue());
		header_panel.add(header_1);
		header_panel.add(header_2);
		header_panel.add(Box.createVerticalGlue());
		
		// Release notes:
		final var release_notes_text_area =
			new JTextArea(Resources.load_text("release-notes.txt"));
		release_notes_text_area.setFont(Resources.font_regular);
		release_notes_text_area.setLineWrap(false);
		release_notes_text_area.setEditable(false);
		
		final var release_notes_scroll_pane =
			new JScrollPane(release_notes_text_area);
		release_notes_scroll_pane.setMaximumSize(tab_dimension);
		release_notes_scroll_pane.setMinimumSize(tab_dimension);
		release_notes_scroll_pane.setPreferredSize(tab_dimension);
		release_notes_scroll_pane.setAlignmentX(Component.CENTER_ALIGNMENT);
		release_notes_scroll_pane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		final var release_notes_panel =
			new JPanel();
		release_notes_panel.setMaximumSize(tab_dimension);
		release_notes_panel.setMinimumSize(tab_dimension);
		release_notes_panel.setPreferredSize(tab_dimension);
		release_notes_panel.setAlignmentX(Component.CENTER_ALIGNMENT);
		release_notes_panel.setLayout(new BoxLayout(
			  release_notes_panel
			, BoxLayout.Y_AXIS));
		release_notes_panel.add(Box.createVerticalGlue());
		release_notes_panel.add(release_notes_scroll_pane);
		release_notes_panel.add(Box.createVerticalGlue());
		
		// Licenses:
		final var licenses_text_area =
			new JTextArea(pmchess.pmChess.licenses[0]);
		licenses_text_area.setFont(Resources.font_italic);
		licenses_text_area.setLineWrap(false);
		licenses_text_area.setEditable(false);
		
		final var licenses_scroll_pane =
			new JScrollPane(licenses_text_area);
		final var license_scroll_pane_dimension =
			new Dimension(licenses_scroll_pane_x_size, licenses_scroll_pane_y_size);
		licenses_scroll_pane.setMaximumSize(license_scroll_pane_dimension);
		licenses_scroll_pane.setMinimumSize(license_scroll_pane_dimension);
		licenses_scroll_pane.setPreferredSize(license_scroll_pane_dimension);
		licenses_scroll_pane.setAlignmentX(Component.CENTER_ALIGNMENT);
		licenses_scroll_pane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		final var licenses_combo_box =
			new JComboBox<>(pmchess.pmChess.licenses_subjects);
		final var licenses_combo_box_dimension =
			new Dimension(licenses_combo_box_x_size, licenses_combo_box_y_size);
		licenses_combo_box.setMaximumSize(licenses_combo_box_dimension);
		licenses_combo_box.setMinimumSize(licenses_combo_box_dimension);
		licenses_combo_box.setPreferredSize(licenses_combo_box_dimension);
		licenses_combo_box.setFont(Resources.font_bold.deriveFont(1.1f * Resources.base_scale));
		licenses_combo_box.setSelectedIndex(0);
		licenses_combo_box.addActionListener(
			new ActionListener()
			{
				@Override public void actionPerformed(final ActionEvent event)
				{
					licenses_text_area.setText(
						pmchess.pmChess.licenses[licenses_combo_box.getSelectedIndex()]);
				}
			});
		
		final var licenses_panel =
			new JPanel();
		licenses_panel.setMaximumSize(tab_dimension);
		licenses_panel.setMinimumSize(tab_dimension);
		licenses_panel.setPreferredSize(tab_dimension);
		licenses_panel.setAlignmentX(Component.CENTER_ALIGNMENT);
		licenses_panel.setLayout(new BoxLayout(
			  licenses_panel
			, BoxLayout.Y_AXIS));
		licenses_panel.add(Box.createVerticalGlue());
		licenses_panel.add(licenses_combo_box);
		licenses_panel.add(Box.createVerticalGlue());
		licenses_panel.add(licenses_scroll_pane);
		licenses_panel.add(Box.createVerticalGlue());
		
		// Contact:
		final var contact_description_text_area =
			new JTextArea(
				"Feedback is always welcome; sharing your issues and opinion regarding "
				+ "pmChess is very kind! Please select a subject from the proposed set "
				+ "and decide if it is OK to quote your message for example on the "
				+ "pmChesss issue tracker or within its documentation. Selecting a "
				+ "feasible subject helps classifying your mail; and a quote permission "
				+ "is particularly kind for general feedback as it enables us to publicly "
				+ "share your opinion.");
		contact_description_text_area.setFont(Resources.font_regular);
		contact_description_text_area.setLineWrap(true);
		contact_description_text_area.setWrapStyleWord(true);
		contact_description_text_area.setEditable(false);
		
		final var contact_description_scroll_pane =
			new JScrollPane(contact_description_text_area);
		final var contact_description_scroll_pane_dimension =
			new Dimension(
				  contact_description_scroll_pane_x_size
				, contact_description_scroll_pane_y_size);
		contact_description_scroll_pane.setMaximumSize(contact_description_scroll_pane_dimension);
		contact_description_scroll_pane.setMinimumSize(contact_description_scroll_pane_dimension);
		contact_description_scroll_pane.setPreferredSize(contact_description_scroll_pane_dimension);
		contact_description_scroll_pane.setAlignmentX(Component.CENTER_ALIGNMENT);
		contact_description_scroll_pane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		final var contact_subject_label =
			new JLabel("Subject:", SwingConstants.LEFT);
		contact_subject_label.setMaximumSize(contact_row_label_dimension);
		contact_subject_label.setMinimumSize(contact_row_label_dimension);
		contact_subject_label.setPreferredSize(contact_row_label_dimension);
		final var contact_subject_combo_box =
			new JComboBox<>(new String[]{
				  "Please select subject\u2026"
				, "chess logic error (rule violation)"
				, "bug report"
				, "computer player issues (bad play)"
				, "user interface proposal"
				, "enhancement proposal"
				, "general feedback"});
		contact_subject_combo_box.setMaximumSize(contact_row_field_dimension);
		contact_subject_combo_box.setMinimumSize(contact_row_field_dimension);
		contact_subject_combo_box.setPreferredSize(contact_row_field_dimension);
		contact_subject_combo_box.setSelectedIndex(0);
		final var contact_subject_panel =
			new JPanel();
		contact_subject_panel.setMaximumSize(contact_row_dimension);
		contact_subject_panel.setMinimumSize(contact_row_dimension);
		contact_subject_panel.setPreferredSize(contact_row_dimension);
		contact_subject_panel.setAlignmentX(Component.CENTER_ALIGNMENT);
		contact_subject_panel.setLayout(new BoxLayout(
			  contact_subject_panel
			, BoxLayout.X_AXIS));
		contact_subject_panel.add(Box.createHorizontalGlue());
		contact_subject_panel.add(contact_subject_label);
		contact_subject_panel.add(contact_subject_combo_box);
		contact_subject_panel.add(Box.createHorizontalGlue());
		
		final var contact_permission_label =
			new JLabel("Quote permission:", SwingConstants.LEFT);
		contact_permission_label.setMaximumSize(contact_row_label_dimension);
		contact_permission_label.setMinimumSize(contact_row_label_dimension);
		contact_permission_label.setPreferredSize(contact_row_label_dimension);
		final var contact_permission_combo_box =
			new JComboBox<>(new String[]{
				  "Please decide whether you give us quote permission\u2026"
				, "Rejected (no permission to quote mail)"
				, "Granted (mail can be publicly quoted)"});
		contact_permission_combo_box.setMaximumSize(contact_row_field_dimension);
		contact_permission_combo_box.setMinimumSize(contact_row_field_dimension);
		contact_permission_combo_box.setPreferredSize(contact_row_field_dimension);
		contact_permission_combo_box.setSelectedIndex(0);
		final var contact_permission_panel =
			new JPanel();
		contact_permission_panel.setMaximumSize(contact_row_dimension);
		contact_permission_panel.setMinimumSize(contact_row_dimension);
		contact_permission_panel.setPreferredSize(contact_row_dimension);
		contact_permission_panel.setAlignmentX(Component.CENTER_ALIGNMENT);
		contact_permission_panel.setLayout(new BoxLayout(
			  contact_permission_panel
			, BoxLayout.X_AXIS));
		contact_permission_panel.add(Box.createHorizontalGlue());
		contact_permission_panel.add(contact_permission_label);
		contact_permission_panel.add(contact_permission_combo_box);
		contact_permission_panel.add(Box.createHorizontalGlue());
		
		final var contact_name_label =
			new JLabel("Your name:", SwingConstants.LEFT);
		contact_name_label.setMaximumSize(contact_row_label_dimension);
		contact_name_label.setMinimumSize(contact_row_label_dimension);
		contact_name_label.setPreferredSize(contact_row_label_dimension);
		final var contact_name_field =
			new JTextField();
		contact_name_field.setMaximumSize(contact_row_field_dimension);
		contact_name_field.setMinimumSize(contact_row_field_dimension);
		contact_name_field.setPreferredSize(contact_row_field_dimension);
		final var contact_name_panel =
			new JPanel();
		contact_name_panel.setMaximumSize(contact_row_dimension);
		contact_name_panel.setMinimumSize(contact_row_dimension);
		contact_name_panel.setPreferredSize(contact_row_dimension);
		contact_name_panel.setAlignmentX(Component.CENTER_ALIGNMENT);
		contact_name_panel.setLayout(new BoxLayout(
			  contact_name_panel
			, BoxLayout.X_AXIS));
		contact_name_panel.add(Box.createHorizontalGlue());
		contact_name_panel.add(contact_name_label);
		contact_name_panel.add(contact_name_field);
		contact_name_panel.add(Box.createHorizontalGlue());
		
		final var contact_rows_panel =
			new JPanel();
		final var contact_rows_dimension =
			new Dimension(contact_rows_x_size, contact_rows_y_size);
		contact_rows_panel.setMaximumSize(contact_rows_dimension);
		contact_rows_panel.setMinimumSize(contact_rows_dimension);
		contact_rows_panel.setPreferredSize(contact_rows_dimension);
		contact_rows_panel.setAlignmentX(Component.CENTER_ALIGNMENT);
		contact_rows_panel.setLayout(new BoxLayout(
			  contact_rows_panel
			, BoxLayout.Y_AXIS));
		contact_rows_panel.add(Box.createVerticalGlue());
		contact_rows_panel.add(contact_subject_panel);
		contact_rows_panel.add(Box.createVerticalGlue());
		contact_rows_panel.add(contact_permission_panel);
		contact_rows_panel.add(Box.createVerticalGlue());
		contact_rows_panel.add(contact_name_panel);
		contact_rows_panel.add(Box.createVerticalGlue()); 
		
		final var contact_message_text_area =
			new JTextArea();
		contact_message_text_area.setFont(Resources.font_italic);
		contact_message_text_area.setLineWrap(true);
		contact_message_text_area.setWrapStyleWord(true);
		contact_message_text_area.setEditable(true);
		
		final var contact_message_scroll_pane =
			new JScrollPane(contact_message_text_area);
		final var contact_message_scroll_pane_dimension =
			new Dimension(contact_message_scroll_pane_x_size, contact_message_scroll_pane_y_size);
		contact_message_scroll_pane.setMaximumSize(contact_message_scroll_pane_dimension);
		contact_message_scroll_pane.setMinimumSize(contact_message_scroll_pane_dimension);
		contact_message_scroll_pane.setPreferredSize(contact_message_scroll_pane_dimension);
		contact_message_scroll_pane.setAlignmentX(Component.CENTER_ALIGNMENT);
		contact_message_scroll_pane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		final var contact_send_button =
			new JButton("send e-mail");
		final var contact_send_button_dimension =
			new Dimension(contact_send_button_x_size, contact_send_button_y_size);
		contact_send_button.setMaximumSize(contact_send_button_dimension);
		contact_send_button.setMinimumSize(contact_send_button_dimension);
		contact_send_button.setPreferredSize(contact_send_button_dimension);
		contact_send_button.setAlignmentX(Component.CENTER_ALIGNMENT);
		contact_send_button.addActionListener(
			new ActionListener()
			{
				@Override public void actionPerformed(final ActionEvent event)
				{
					final var subject_index =
						contact_subject_combo_box.getSelectedIndex();
					final var permission_index =
						contact_permission_combo_box.getSelectedIndex();
					final var name =
						contact_name_field.getText().trim();
					if (subject_index == 0
						|| permission_index == 0
						|| name.length() < 1)
					{
						JOptionPane.showMessageDialog(
							  AboutFrame.this
							, "Please select subject, quote permission and name."
							, "Sending mail aborted"
							, JOptionPane.ERROR_MESSAGE);
						return;
					}
					final var subject =
						"pmChess: " + contact_subject_combo_box.getItemAt(subject_index);
					final var body =
						"Dear Christoff,"
						+ "\n\n"
						+ contact_message_text_area.getText().trim()
						+ "\n\n"
						+ "Best regards,\n" + name
						+ "\n\n"
						+ "PLEASE DO NOT MODIFY THE FOLLOWING TEXT:\n"
						+ "  Quote permission: "
						+ contact_permission_combo_box.getItemAt(permission_index) + "\n"
						+ "  pmChess version: "
						+ pmchess.pmChess.version + "\n"
						+ "  Platform: "
						+ System.getProperty("os.name");
					try
					{
						Desktop.getDesktop().mail(new URI(
							"mailto:Christoff.Buerger@gmail.com?"
							+ "subject=" + encode(subject) + "&"
							+ "body=" + encode(body)));
					}
					catch (URISyntaxException
						| UnsupportedOperationException
						| IllegalArgumentException
						| IOException
						| SecurityException exception)
					{
						JOptionPane.showMessageDialog(
							  AboutFrame.this
							, "Failed to open default e-mail client."
							, "Sending mail aborted"
							, JOptionPane.ERROR_MESSAGE);
					}
				}
				
				private String encode(final String text) throws IOException
				{
					return URLEncoder.encode(text, Resources.text_encoding)
						.replace("+", "%20");
				}
			});
		final var contact_send_button_panel =
			new JPanel();
		final var contact_send_button_panel_dimension =
			new Dimension(contact_send_button_panel_x_size, contact_send_button_panel_y_size);
		contact_send_button_panel.setMaximumSize(contact_send_button_panel_dimension);
		contact_send_button_panel.setMinimumSize(contact_send_button_panel_dimension);
		contact_send_button_panel.setPreferredSize(contact_send_button_panel_dimension);
		contact_send_button_panel.setAlignmentX(Component.CENTER_ALIGNMENT);
		contact_send_button_panel.setLayout(new BoxLayout(
			  contact_send_button_panel
			, BoxLayout.Y_AXIS));
		contact_send_button_panel.add(Box.createVerticalGlue());
		contact_send_button_panel.add(contact_send_button);
		
		final var contact_panel =
			new JPanel();
		contact_panel.setMaximumSize(tab_dimension);
		contact_panel.setMinimumSize(tab_dimension);
		contact_panel.setPreferredSize(tab_dimension);
		contact_panel.setAlignmentX(Component.CENTER_ALIGNMENT);
		contact_panel.setLayout(new BoxLayout(
			  contact_panel
			, BoxLayout.Y_AXIS));
		contact_panel.add(Box.createVerticalGlue());
		contact_panel.add(contact_description_scroll_pane);
		contact_panel.add(contact_rows_panel);
		contact_panel.add(contact_message_scroll_pane);
		contact_panel.add(contact_send_button_panel);
		contact_panel.add(Box.createVerticalGlue());
		
		// Tabs:
		final var tabs_dimensions =
			new Dimension(tabs_x_size, tabs_y_size);
		tabs.setMaximumSize(tabs_dimensions);
		tabs.setMinimumSize(tabs_dimensions);
		tabs.setPreferredSize(tabs_dimensions);
		tabs.setAlignmentX(Component.CENTER_ALIGNMENT);
		tabs.addTab("Tea time", new JLabel(new ImageIcon(logo)));
		tabs.addTab("Release notes", release_notes_panel);
		tabs.addTab("Contact", contact_panel);
		tabs.addTab("Licenses", licenses_panel);
		
		// Compose all:
		final var panel =
			new JPanel();
		final var panel_dimension =
			new Dimension(panel_x_size, panel_y_size);
		panel.setMaximumSize(panel_dimension);
		panel.setMinimumSize(panel_dimension);
		panel.setPreferredSize(panel_dimension);
		panel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.setLayout(new BoxLayout(
			  panel
			, BoxLayout.Y_AXIS));
		panel.add(Box.createVerticalGlue());
		panel.add(header_panel);
		panel.add(tabs);
		panel.add(Box.createVerticalGlue());
		
		add(panel);
		
		// Setup window:
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setBackground(Color.gray);
		setSize(frame_x_size, frame_y_size);
		pack();
		setResizable(false);
		setLocationRelativeTo(null); // center window
		setVisible(false);
		
		// Setup user-input processing:
		final var root_pane =
			getRootPane();
		final var input_map =
			root_pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final var action_map =
			root_pane.getActionMap();
		input_map.put(
			  KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
			, "Close");
		action_map.put(
			  "Close"
			, new AbstractAction()
			  {
				@Override public void actionPerformed(ActionEvent event)
				{
					AboutFrame.this.setVisible(false);
				}
			  });
		input_map.put(
			  KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)
			, "ScrollUp");
		action_map.put(
			  "ScrollUp"
			, new AbstractAction()
			  {
				@Override public void actionPerformed(ActionEvent event)
				{
					final JScrollBar bar;
					if (tabs.getSelectedIndex() == tabs.indexOfComponent(release_notes_scroll_pane))
					{
						bar = release_notes_scroll_pane.getVerticalScrollBar();
					}
					else if (tabs.getSelectedIndex() == tabs.indexOfComponent(licenses_panel))
					{
						bar = licenses_scroll_pane.getVerticalScrollBar();
					}
					else
					{
						return;
					}
					bar.setValue(bar.getValue() > 42
						? bar.getValue() - 42
						: 0);
				}
			  });
		input_map.put(
			  KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)
			, "ScrollDown");
		action_map.put(
			  "ScrollDown"
			, new AbstractAction()
			  {
				@Override public void actionPerformed(ActionEvent event)
				{
					final JScrollBar bar;
					if (tabs.getSelectedIndex() == tabs.indexOfComponent(release_notes_scroll_pane))
					{
						bar = release_notes_scroll_pane.getVerticalScrollBar();
					}
					else if (tabs.getSelectedIndex() == tabs.indexOfComponent(licenses_panel))
					{
						bar = licenses_scroll_pane.getVerticalScrollBar();
					}
					else
					{
						return;
					}
					bar.setValue(bar.getValue() + 42 > bar.getMaximum()
						? bar.getMaximum()
						: bar.getValue() + 42);
				}
			  });
	}
	
	@Override public void paint(final Graphics graphics)
	{
		super.paint(graphics);
		Resources.configure_rendering(graphics);
	}
}
