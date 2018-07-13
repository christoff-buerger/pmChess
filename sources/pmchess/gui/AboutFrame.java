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
	private static final String releaseNotes = Resources.loadText("release-notes.txt");
	private static final Image logo = Resources.loadImage("logo/logo-animated.gif");
	
	private final JTabbedPane tabs = new JTabbedPane();
	
	protected void showContactTab() {
		tabs.setSelectedIndex(2);
		setVisible(true);
	}
	
	protected AboutFrame() {
		super("About pmChess");
		
		setSize(600, 580);
		setResizable(false);
		final Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((d.width - getSize().width) / 2, (d.height - getSize().height) / 2);
		
		// Release notes:
		final JTextArea releaseNotesTextArea = new JTextArea(releaseNotes);
		releaseNotesTextArea.setFont(Resources.font_regular);
		releaseNotesTextArea.setLineWrap(false);
		releaseNotesTextArea.setEditable(false);
		final JScrollPane releaseNotesScrollPane = new JScrollPane(releaseNotesTextArea);
		releaseNotesScrollPane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		releaseNotesScrollPane.setPreferredSize(new Dimension(570, 460));
		
		final JPanel releaseNotesPanel = new JPanel();
		releaseNotesPanel.add(releaseNotesScrollPane);
		
		// Licenses:
		final JTextArea licenseTextArea = new JTextArea(pmchess.pmChess.pmChessLicense);
		licenseTextArea.setFont(Resources.font_italic);
		licenseTextArea.setLineWrap(false);
		licenseTextArea.setEditable(false);
		final JScrollPane licenseScrollPane = new JScrollPane(licenseTextArea);
		licenseScrollPane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		licenseScrollPane.setPreferredSize(new Dimension(570, 425));
		
		final JToggleButton pmChessButton = new JToggleButton("pmChess", true);
		final JToggleButton openSansButton = new JToggleButton("Open Sans", false);
		final JToggleButton chessMeridaUnicodeButton =
			new JToggleButton("Chess Merida Unicode", false);
		final JToggleButton chessPiecesButton = new JToggleButton("Chess pieces", false);
		pmChessButton.addActionListener(new ActionListener() {
				@Override public void actionPerformed(final ActionEvent e) {
					pmChessButton.setSelected(true);
					openSansButton.setSelected(false);
					chessMeridaUnicodeButton.setSelected(false);
					chessPiecesButton.setSelected(false);
					licenseTextArea.setText(pmchess.pmChess.pmChessLicense);
				}
			});
		openSansButton.addActionListener(new ActionListener() {
				@Override public void actionPerformed(final ActionEvent e) {
					pmChessButton.setSelected(false);
					openSansButton.setSelected(true);
					chessMeridaUnicodeButton.setSelected(false);
					chessPiecesButton.setSelected(false);
					licenseTextArea.setText(pmchess.pmChess.openSansLicense);
				}
			});
		chessMeridaUnicodeButton.addActionListener(new ActionListener() {
				@Override public void actionPerformed(final ActionEvent e) {
					pmChessButton.setSelected(false);
					openSansButton.setSelected(false);
					chessMeridaUnicodeButton.setSelected(true);
					chessPiecesButton.setSelected(false);
					licenseTextArea.setText(
						pmchess.pmChess.chessMeridaUnicodeLicense);
				}
			});
		chessPiecesButton.addActionListener(new ActionListener() {
				@Override public void actionPerformed(final ActionEvent e) {
					pmChessButton.setSelected(false);
					openSansButton.setSelected(false);
					chessMeridaUnicodeButton.setSelected(false);
					chessPiecesButton.setSelected(true);
					licenseTextArea.setText(pmchess.pmChess.chessPiecesLicense);
				}
			});
		
		final JPanel licensesPanel = new JPanel();
		licensesPanel.add(pmChessButton);
		licensesPanel.add(openSansButton);
		licensesPanel.add(chessMeridaUnicodeButton);
		licensesPanel.add(chessPiecesButton);
		licensesPanel.add(licenseScrollPane);
		
		// Contact:
		final JTextArea descriptionTextArea = new JTextArea(
			"Feedback is always welcome; sharing your issues and opinion regarding " +
			"pmChess is very kind! Please select a subject from the proposed set " +
			"and decide if it is OK to quote your message for example on the " +
			"pmChesss issue tracker or within its documentation. Selecting a " +
			"feasible subject helps classifying your mail; and a quote permission " +
			"is particularly kind for general feedback as it enables us to publicly " +
			"share your opinion.");
		descriptionTextArea.setFont(Resources.font_regular);
		descriptionTextArea.setLineWrap(true);
		descriptionTextArea.setWrapStyleWord(true);
		descriptionTextArea.setEditable(false);
		final JScrollPane descriptionScrollPane = new JScrollPane(descriptionTextArea);
		descriptionScrollPane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		descriptionScrollPane.setPreferredSize(new Dimension(570, 125));
		
		final JLabel subjectLabel = new JLabel("Subject:", SwingConstants.LEFT);
		final JComboBox<String> subjectComboBox = new JComboBox<>(new String[]{
			"Please select subject\u2026",
			"chess logic error (rule violation)",
			"bug report",
			"computer player issues (bad play)",
			"user interface proposal",
			"enhancement proposal",
			"general feedback"});
		subjectComboBox.setSelectedIndex(0);
		
		final JLabel permissionLabel = new JLabel("Quote permission:", SwingConstants.LEFT);
		final JComboBox<String> permissionComboBox = new JComboBox<>(new String[]{
			"Please decide whether you give us quote permission\u2026",
			"REJECTED (no permission to quote mail)",
			"GRANTED (mail can be publicly quoted)"});
		permissionComboBox.setSelectedIndex(0);
		
		final JLabel nameLabel = new JLabel("Your name:", SwingConstants.LEFT);
		final JTextField nameField = new JTextField();
		
		final Dimension description_dimension =
			new Dimension(130, subjectLabel.getPreferredSize().height);
		final Dimension selection_dimension =
			new Dimension(430, subjectComboBox.getPreferredSize().height);
		
		subjectLabel.setPreferredSize(description_dimension);
		subjectComboBox.setPreferredSize(selection_dimension);
		permissionLabel.setPreferredSize(description_dimension);
		permissionComboBox.setPreferredSize(selection_dimension);
		nameLabel.setPreferredSize(description_dimension);
		nameField.setPreferredSize(selection_dimension);
		
		final JTextArea messageTextArea = new JTextArea();
		messageTextArea.setFont(Resources.font_italic);
		messageTextArea.setLineWrap(true);
		messageTextArea.setWrapStyleWord(true);
		messageTextArea.setEditable(true);
		final JScrollPane messageScrollPane = new JScrollPane(messageTextArea);
		messageScrollPane.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		messageScrollPane.setPreferredSize(new Dimension(570, 195));
		
		final JButton sendButton = new JButton("send e-mail");
		sendButton.setPreferredSize(new Dimension(570, selection_dimension.height));
		sendButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(final ActionEvent e) {
				final int subject_index = subjectComboBox.getSelectedIndex();
				final int permission_index = permissionComboBox.getSelectedIndex();
				final String name = nameField.getText().trim();
				if (subject_index == 0 || permission_index == 0 ||
					name.length() < 2)
				{
					JOptionPane.showMessageDialog(
						AboutFrame.this,
						"Please select subject, quote permission and name.",
						"Sending mail aborted",
						JOptionPane.ERROR_MESSAGE);
					return;
				}
				final String subject =
					"pmChess: " + subjectComboBox.getItemAt(subject_index);
				final String body =
					"Dear Christoff," +
					"\n\n" +
					messageTextArea.getText().trim() +
					"\n\n" +
					"Best regards,\n" + name +
					"\n\n" +
					"PLEASE DO NOT MODIFY THE FOLLOWING TEXT:\n" +
					"  Quote permission: " +
					permissionComboBox.getItemAt(permission_index) + "\n" +
					"  pmChess version: " +
					pmchess.pmChess.version + "\n" +
					"  Platform: " +
					System.getProperty("os.name");
				try {
					Desktop.getDesktop().mail(new URI(
						"mailto:Christoff.Buerger@gmail.com?" +
						"subject=" + encode(subject) + "&" +
						"body=" + encode(body)));
				} catch (URISyntaxException |
					UnsupportedOperationException |
					IllegalArgumentException |
					IOException |
					SecurityException exception)
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
		
		final JPanel contactPanel = new JPanel();
		contactPanel.add(descriptionScrollPane);
		contactPanel.add(subjectLabel);
		contactPanel.add(subjectComboBox);
		contactPanel.add(permissionLabel);
		contactPanel.add(permissionComboBox);
		contactPanel.add(nameLabel);
		contactPanel.add(nameField);
		contactPanel.add(messageScrollPane);
		contactPanel.add(sendButton);
		
		// Compose all:
		tabs.setPreferredSize(new Dimension(590, 500));
		tabs.addTab("Tea time", new JLabel(new ImageIcon(logo)));
		tabs.addTab("Release notes", releaseNotesPanel);
		tabs.addTab("Contact", contactPanel);
		tabs.addTab("Licenses", licensesPanel);
		
		final JLabel header_1 = new JLabel(pmchess.pmChess.about[0], SwingConstants.CENTER);
		final JLabel header_2 = new JLabel(pmchess.pmChess.about[1], SwingConstants.CENTER);
		
		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(header_1, BorderLayout.PAGE_START);
		panel.add(header_2, BorderLayout.CENTER);
		panel.add(tabs, BorderLayout.PAGE_END);
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		add(panel);
		
		// Setup user-input processing:
		final JRootPane rootPane = getRootPane();
		final InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = rootPane.getActionMap();
		inputMap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			"Close");
		actionMap.put(
			"Close",
			new AbstractAction() {
				@Override public void actionPerformed(ActionEvent e) {
					AboutFrame.this.setVisible(false);
				}
			});
		inputMap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
			"ScrollUp");
		actionMap.put(
			"ScrollUp",
			new AbstractAction() {
				@Override public void actionPerformed(ActionEvent e) {
					final JScrollBar bar;
					if (tabs.getSelectedIndex() ==
						tabs.indexOfComponent(releaseNotesPanel))
					{
						bar = releaseNotesScrollPane.getVerticalScrollBar();
					} else if (tabs.getSelectedIndex() ==
						tabs.indexOfComponent(licensesPanel))
					{
						bar = licenseScrollPane.getVerticalScrollBar();
					} else {
						return;
					}
					bar.setValue(bar.getValue() > 42 ? bar.getValue() - 42 : 0);
				}
			});
		inputMap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
			"ScrollDown");
		actionMap.put(
			"ScrollDown",
			new AbstractAction() {
				@Override public void actionPerformed(ActionEvent e) {
					final JScrollBar bar;
					if (tabs.getSelectedIndex() ==
						tabs.indexOfComponent(releaseNotesPanel))
					{
						bar = releaseNotesScrollPane.getVerticalScrollBar();
					} else if (tabs.getSelectedIndex() ==
						tabs.indexOfComponent(licensesPanel))
					{
						bar = licenseScrollPane.getVerticalScrollBar();
					} else {
						return;
					}
					bar.setValue(bar.getValue() + 42 > bar.getMaximum() ?
						bar.getMaximum() :
						bar.getValue() + 42);
				}
			});
	}
}
