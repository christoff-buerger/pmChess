/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public final class AboutFrame extends JFrame {
	protected AboutFrame() {
		super("About pmChess");
		
		setResizable(false);
		setSize(600, 580);
		final Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((d.width - getSize().width) / 2, (d.height - getSize().height) / 2);
		
		final JLabel text1 = new JLabel(pmchess.pmChess.about[0], SwingConstants.CENTER);
		final JLabel text2 = new JLabel(pmchess.pmChess.about[1], SwingConstants.CENTER);
		
		final JTextArea licenseTextArea = new JTextArea(pmchess.pmChess.pmChessLicense);
		licenseTextArea.setFont(GUI.font_italic);
		licenseTextArea.setLineWrap(false);
		licenseTextArea.setEditable(false);
		final JScrollPane licenseScrollPane = new JScrollPane(licenseTextArea);
		licenseScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		licenseScrollPane.setPreferredSize(new Dimension(570, 425));
		
		final JButton pmChessButton = new JButton("pmChess");
		pmChessButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					licenseTextArea.setText(pmchess.pmChess.pmChessLicense);
				}
			});
		final JButton openSansButton = new JButton("Open Sans");
		openSansButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					licenseTextArea.setText(pmchess.pmChess.openSansLicense);
				}
			});
		final JButton chessPiecesButton = new JButton("Chess pieces");
		chessPiecesButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					licenseTextArea.setText(pmchess.pmChess.chessPiecesLicense);
				}
			});
		
		final JPanel licensesPanel = new JPanel();
		licensesPanel.setPreferredSize(new Dimension(590, 500));
		licensesPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createTitledBorder("Licenses"),
			BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		licensesPanel.add(pmChessButton);
		licensesPanel.add(openSansButton);
		licensesPanel.add(chessPiecesButton);
		licensesPanel.add(licenseScrollPane);
		
		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(text1, BorderLayout.PAGE_START);
		panel.add(text2, BorderLayout.CENTER);
		panel.add(licensesPanel, BorderLayout.PAGE_END);
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		add(panel);
		
		final JRootPane rootPane = getRootPane();
		final InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		final ActionMap actionMap = rootPane.getActionMap();
		inputMap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			"Close");
		actionMap.put(
			"Close",
			new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					AboutFrame.this.setVisible(false);
				}
			});
		final JScrollBar bar = licenseScrollPane.getVerticalScrollBar();
		inputMap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
			"ScrollUp");
		actionMap.put(
			"ScrollUp",
			new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					bar.setValue(bar.getValue() > 42 ? bar.getValue() - 42 : 0);
				}
			});		
		inputMap.put(
			KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
			"ScrollDown");
		actionMap.put(
			"ScrollDown",
			new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					bar.setValue(bar.getValue() + 42 > bar.getMaximum()  ?
						bar.getMaximum() :
						bar.getValue() + 42);
				}
			});		
	}
}
