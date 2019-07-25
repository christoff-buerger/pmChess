/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.gui;

import java.io.*;

import java.net.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public final class AboutFrame extends JFrame {
	private static final String release_notes = Resources.load_text("release-notes.txt");
	private static final Image logo = Resources.load_image("logo/logo-animated.gif");
	
	private final JTabbedPane tabs = new JTabbedPane();
	
	protected void show_contact_tab() {
		tabs.setSelectedIndex(2);
		setVisible(true);
	}
	
	protected AboutFrame() {
		super("About pmChess");
		
		setSize(600, 580);
		setResizable(false);
		final var d = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((d.width - getSize().width) / 2, (d.height - getSize().height) / 2);
		
		// Release notes:
		final var release_notes_text_area = new JTextArea(release_notes);
		release_notes_text_area.setFont(Resources.font_regular);
		release_notes_text_area.setLineWrap(false);
		release_notes_text_area.setEditable(false);
		final var release_notes_scroll_pane = new JScrollPane(release_notes_text_area);
		release_notes_scroll_pane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		release_notes_scroll_pane.setPreferredSize(new Dimension(570, 460));
		
		final var release_notes_panel = new JPanel();
		release_notes_panel.add(release_notes_scroll_pane);
		
		// Licenses:
		final var license_text_area = new JTextArea(pmchess.pmChess.pmChess_license);
		license_text_area.setFont(Resources.font_italic);
		license_text_area.setLineWrap(false);
		license_text_area.setEditable(false);
		final var license_scroll_pane = new JScrollPane(license_text_area);
		license_scroll_pane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		license_scroll_pane.setPreferredSize(new Dimension(570, 425));
		
		final var pmChess_button = new JToggleButton("pmChess", true);
		final var open_sans_button = new JToggleButton("Open Sans", false);
		final var chess_merida_unicode_button =
			new JToggleButton("Chess Merida Unicode", false);
		pmChess_button.addActionListener(new ActionListener() {
				@Override public void actionPerformed(final ActionEvent event) {
					pmChess_button.setSelected(true);
					open_sans_button.setSelected(false);
					chess_merida_unicode_button.setSelected(false);
					license_text_area.setText(
						pmchess.pmChess.pmChess_license);
				}
			});
		open_sans_button.addActionListener(new ActionListener() {
				@Override public void actionPerformed(final ActionEvent event) {
					pmChess_button.setSelected(false);
					open_sans_button.setSelected(true);
					chess_merida_unicode_button.setSelected(false);
					license_text_area.setText(
						pmchess.pmChess.open_sans_license);
				}
			});
		chess_merida_unicode_button.addActionListener(new ActionListener() {
				@Override public void actionPerformed(final ActionEvent event) {
					pmChess_button.setSelected(false);
					open_sans_button.setSelected(false);
					chess_merida_unicode_button.setSelected(true);
					license_text_area.setText(
						pmchess.pmChess.chess_merida_unicode_license);
				}
			});
		
		final var licenses_panel = new JPanel();
		licenses_panel.add(pmChess_button);
		licenses_panel.add(open_sans_button);
		licenses_panel.add(chess_merida_unicode_button);
		licenses_panel.add(license_scroll_pane);
		
		// Contact:
		final var description_text_area = new JTextArea(
			"Feedback is always welcome; sharing your issues and opinion regarding "
			+ "pmChess is very kind! Please select a subject from the proposed set "
			+ "and decide if it is OK to quote your message for example on the "
			+ "pmChesss issue tracker or within its documentation. Selecting a "
			+ "feasible subject helps classifying your mail; and a quote permission "
			+ "is particularly kind for general feedback as it enables us to publicly "
			+ "share your opinion.");
		description_text_area.setFont(Resources.font_regular);
		description_text_area.setLineWrap(true);
		description_text_area.setWrapStyleWord(true);
		description_text_area.setEditable(false);
		final var description_scroll_pane = new JScrollPane(description_text_area);
		description_scroll_pane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		description_scroll_pane.setPreferredSize(new Dimension(570, 125));
		
		final var subject_label = new JLabel("Subject:", SwingConstants.LEFT);
		final var subject_combo_box = new JComboBox<>(new String[]{
			"Please select subject\u2026",
			"chess logic error (rule violation)",
			"bug report",
			"computer player issues (bad play)",
			"user interface proposal",
			"enhancement proposal",
			"general feedback"});
		subject_combo_box.setSelectedIndex(0);
		
		final var permission_label = new JLabel("Quote permission:", SwingConstants.LEFT);
		final var permission_combo_box = new JComboBox<>(new String[]{
			"Please decide whether you give us quote permission\u2026",
			"REJECTED (no permission to quote mail)",
			"GRANTED (mail can be publicly quoted)"});
		permission_combo_box.setSelectedIndex(0);
		
		final var name_label = new JLabel("Your name:", SwingConstants.LEFT);
		final var name_field = new JTextField();
		
		final var description_dimension =
			new Dimension(130, subject_label.getPreferredSize().height);
		final var selection_dimension =
			new Dimension(430, subject_combo_box.getPreferredSize().height);
		
		subject_label.setPreferredSize(description_dimension);
		subject_combo_box.setPreferredSize(selection_dimension);
		permission_label.setPreferredSize(description_dimension);
		permission_combo_box.setPreferredSize(selection_dimension);
		name_label.setPreferredSize(description_dimension);
		name_field.setPreferredSize(selection_dimension);
		
		final var message_text_area = new JTextArea();
		message_text_area.setFont(Resources.font_italic);
		message_text_area.setLineWrap(true);
		message_text_area.setWrapStyleWord(true);
		message_text_area.setEditable(true);
		final var message_scroll_pane = new JScrollPane(message_text_area);
		message_scroll_pane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		message_scroll_pane.setPreferredSize(new Dimension(570, 195));
		
		final var send_button = new JButton("send e-mail");
		send_button.setPreferredSize(new Dimension(570, selection_dimension.height));
		send_button.addActionListener(new ActionListener() {
			@Override public void actionPerformed(final ActionEvent event) {
				final var subject_index =
					subject_combo_box.getSelectedIndex();
				final var permission_index =
					permission_combo_box.getSelectedIndex();
				final var name =
					name_field.getText().trim();
				if (subject_index == 0
					|| permission_index == 0
					|| name.length() < 1)
				{
					JOptionPane.showMessageDialog(
						AboutFrame.this,
						"Please select subject, quote permission and name.",
						"Sending mail aborted",
						JOptionPane.ERROR_MESSAGE);
					return;
				}
				final var subject =
					"pmChess: " + subject_combo_box.getItemAt(subject_index);
				final var body =
					"Dear Christoff,"
					+ "\n\n"
					+ message_text_area.getText().trim()
					+ "\n\n"
					+ "Best regards,\n" + name
					+ "\n\n"
					+ "PLEASE DO NOT MODIFY THE FOLLOWING TEXT:\n"
					+ "  Quote permission: "
					+ permission_combo_box.getItemAt(permission_index) + "\n"
					+ "  pmChess version: "
					+ pmchess.pmChess.version + "\n"
					+ "  Platform: "
					+ System.getProperty("os.name");
				try {
					Desktop.getDesktop().mail(new URI(
						"mailto:Christoff.Buerger@gmail.com?"
						+ "subject=" + encode(subject) + "&"
						+ "body=" + encode(body)));
				} catch (URISyntaxException
					| UnsupportedOperationException
					| IllegalArgumentException
					| IOException
					| SecurityException exception)
				{
					JOptionPane.showMessageDialog(
						AboutFrame.this,
						"Failed to open default e-mail client.",
						"Sending mail aborted",
						JOptionPane.ERROR_MESSAGE);
				}
			}
			
			private String encode(final String text) throws IOException {
				return URLEncoder.encode(text, Resources.text_encoding)
					.replace("+", "%20");
			}
		});
		
		final var contact_panel = new JPanel();
		contact_panel.add(description_scroll_pane);
		contact_panel.add(subject_label);
		contact_panel.add(subject_combo_box);
		contact_panel.add(permission_label);
		contact_panel.add(permission_combo_box);
		contact_panel.add(name_label);
		contact_panel.add(name_field);
		contact_panel.add(message_scroll_pane);
		contact_panel.add(send_button);
		
		// Compose all:
		tabs.setPreferredSize(new Dimension(590, 500));
		tabs.addTab("Tea time", new JLabel(new ImageIcon(logo)));
		tabs.addTab("Release notes", release_notes_panel);
		tabs.addTab("Contact", contact_panel);
		tabs.addTab("Licenses", licenses_panel);
		
		final var header_1 = new JLabel(pmchess.pmChess.about[0], SwingConstants.CENTER);
		final var header_2 = new JLabel(pmchess.pmChess.about[1], SwingConstants.CENTER);
		
		final var panel = new JPanel(new BorderLayout());
		panel.add(header_1, BorderLayout.PAGE_START);
		panel.add(header_2, BorderLayout.CENTER);
		panel.add(tabs, BorderLayout.PAGE_END);
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		add(panel);
		
		// Setup user-input processing:
		final var root_pane = getRootPane();
		final var input_map = root_pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final var action_map = root_pane.getActionMap();
		input_map.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			"Close");
		action_map.put(
			"Close",
			new AbstractAction() {
				@Override public void actionPerformed(ActionEvent event) {
					AboutFrame.this.setVisible(false);
				}
			});
		input_map.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
			"ScrollUp");
		action_map.put(
			"ScrollUp",
			new AbstractAction() {
				@Override public void actionPerformed(ActionEvent event) {
					final JScrollBar bar;
					if (tabs.getSelectedIndex()
						== tabs.indexOfComponent(release_notes_panel))
					{
						bar = release_notes_scroll_pane
							.getVerticalScrollBar();
					} else if (tabs.getSelectedIndex()
						== tabs.indexOfComponent(licenses_panel))
					{
						bar = license_scroll_pane.getVerticalScrollBar();
					} else {
						return;
					}
					bar.setValue(bar.getValue() > 42
						? bar.getValue() - 42
						: 0);
				}
			});
		input_map.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
			"ScrollDown");
		action_map.put(
			"ScrollDown",
			new AbstractAction() {
				@Override public void actionPerformed(ActionEvent event) {
					final JScrollBar bar;
					if (tabs.getSelectedIndex()
						== tabs.indexOfComponent(release_notes_panel))
					{
						bar = release_notes_scroll_pane
							.getVerticalScrollBar();
					} else if (tabs.getSelectedIndex()
						== tabs.indexOfComponent(licenses_panel))
					{
						bar = license_scroll_pane.getVerticalScrollBar();
					} else {
						return;
					}
					bar.setValue(bar.getValue() + 42 > bar.getMaximum()
						? bar.getMaximum()
						: bar.getValue() + 42);
				}
			});
	}
	
	@Override public void paint(final Graphics graphics) {
		super.paint(graphics);
		Resources.configure_rendering(graphics);
	}
}
