/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.gui;

import java.io.*;

import java.nio.charset.*;

import java.awt.*;

import pmchess.logic.*;

public final class Resources {
	private Resources() { // No instances.
	}
	
	public static final String text_encoding = StandardCharsets.UTF_8.name();
	
	public static String load_text(final String file) {
		try {
			final var stream = Resources.class.getResourceAsStream(file);
			if (stream == null) {
				throw new IOException();
			}
			final var result = new ByteArrayOutputStream();
			final var buffer = new byte[1024];
			try {
				int character;
				while ((character = stream.read(buffer)) != -1) {
					result.write(buffer, 0, character);
				}
			} finally {
				stream.close();
			}
			return result.toString(text_encoding);
		} catch (IOException exception) {
			throw new RuntimeException("Failed to load text file " + file + ".");
		}
	}
	
	private static Font load_font(final String font_name) {
		return load_font(font_name, 1);
	}
	
	private static Font load_font(final String font_name, final float scale) {
		try {
			final var loaded_font = Font.createFont(
				Font.TRUETYPE_FONT,
				GUI.class.getResourceAsStream("fonts/" + font_name));
			final var environment =
				GraphicsEnvironment.getLocalGraphicsEnvironment();
			for (final var existing_font : environment.getAllFonts()) {
				if (existing_font.getFontName().equals(loaded_font.getFontName())) {
					return existing_font.deriveFont(scale * 14f);
				}
			}
			if (!environment.registerFont(loaded_font)) {
				throw new IOException();
			}
			return loaded_font.deriveFont(scale * 14f);
		} catch (IOException | FontFormatException exception) {
			throw new RuntimeException("Failed to load font " + font_name + ".");
		}
	}
	
	protected static Image load_image(final String image_name) {
		final var image_url = GamePanel.class.getResource(image_name);
		if (image_url != null) {
			return Toolkit.getDefaultToolkit().getImage(image_url);
		} else {
			throw new RuntimeException("Failed to load image " + image_name + ".");
		}
	}
	
	protected static final Font font_regular = load_font("Open-Sans-Regular.ttf");
	protected static final Font font_italic = load_font("Open-Sans-Italic.ttf");
	protected static final Font font_bold = load_font("Open-Sans-Bold.ttf");
	protected static final Font font_bold_italic = load_font("Open-Sans-BoldItalic.ttf");
	
	private static final FigurePresentation[] figures = {
		new FigurePresentation(
			Figure.pawn(true), load_image("figures/pawn-w.png"), "\u2659", ""),
		new FigurePresentation(
			Figure.rook(true), load_image("figures/rook-w.png"), "\u2656", "R"),
		new FigurePresentation(
			Figure.knight(true), load_image("figures/knight-w.png"), "\u2658", "N"),
		new FigurePresentation(
			Figure.bishop(true), load_image("figures/bishop-w.png"), "\u2657", "B"),
		new FigurePresentation(
			Figure.queen(true), load_image("figures/queen-w.png"), "\u2655", "Q"),
		new FigurePresentation(
			Figure.king(true), load_image("figures/king-w.png"), "\u2654", "K"),
		new FigurePresentation(
			Figure.pawn(false), load_image("figures/pawn-b.png"), "\u265F", ""),
		new FigurePresentation(
			Figure.rook(false), load_image("figures/rook-b.png"), "\u265C", "R"),
		new FigurePresentation(
			Figure.knight(false), load_image("figures/knight-b.png"), "\u265E", "N"),
		new FigurePresentation(
			Figure.bishop(false), load_image("figures/bishop-b.png"), "\u265D", "B"),
		new FigurePresentation(
			Figure.queen(false), load_image("figures/queen-b.png"), "\u265B", "Q"),
		new FigurePresentation(
			Figure.king(false), load_image("figures/king-b.png"), "\u265A", "K")
	};
	
	protected static final class FigurePresentation {
		protected final Figure figure;
		protected final Image image;
		protected final String unicode;
		protected final String ascii;
		protected static final Font font = load_font("Chess-Merida-Unicode.ttf", 1.2f);
		
		private FigurePresentation(
			final Figure figure,
			final Image image,
			final String unicode,
			final String ascii)
		{
			this.figure = figure;
			this.image = image;
			this.unicode = unicode;
			this.ascii = ascii;
		}
		
		protected static FigurePresentation get(final Figure figure) {
			for (final var f : Resources.figures) {
				if (f.figure == figure) {
					return f;
				}
			}
			throw new RuntimeException("ERROR: missing figure presentation.");
		}
	}
}
