/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.gui;

import java.util.*;

import java.io.*;

import java.nio.charset.*;

import java.awt.*;

import pmchess.logic.*;

public final class Resources
{
	private Resources() // No instances.
	{
	}
	
	protected static final String text_encoding = StandardCharsets.UTF_8.name();
	
	public static String load_text(final String file)
	{
		try
		{
			final var stream = Resources.class.getResourceAsStream(file);
			if (stream == null)
			{
				throw new IOException();
			}
			final var result = new ByteArrayOutputStream();
			final var buffer = new byte[1024];
			try
			{
				int character;
				while ((character = stream.read(buffer)) != -1)
				{
					result.write(buffer, 0, character);
				}
			}
			finally
			{
				stream.close();
			}
			return result.toString(text_encoding);
		}
		catch (IOException exception)
		{
			throw new RuntimeException("Failed to load text file " + file + ".");
		}
	}
	
	// Default font size all GUI-layout is derived from:
	private static final float base_scale = 14f;
	
	private static Font load_font(final String font_name)
	{
		return load_font(font_name, 1);
	}
	
	private static Font load_font(final String font_name, final float scale)
	{
		try
		{
			final var loaded_font = Font.createFont(
				Font.TRUETYPE_FONT,
				Resources.class.getResourceAsStream("fonts/" + font_name));
			final var environment =
				GraphicsEnvironment.getLocalGraphicsEnvironment();
			for (final var existing_font : environment.getAllFonts())
			{
				if (existing_font.getFontName().equals(loaded_font.getFontName()))
				{
					return existing_font.deriveFont(scale * base_scale);
				}
			}
			environment.registerFont(loaded_font);
			return loaded_font.deriveFont(scale * base_scale);
		}
		catch (IOException | FontFormatException exception)
		{
			throw new RuntimeException("Failed to load font " + font_name + ".");
		}
	}
	
	protected static Image load_image(final String image_name)
	{
		final var image_url = Resources.class.getResource(image_name);
		if (image_url != null)
		{
			return Toolkit.getDefaultToolkit().getImage(image_url);
		}
		else
		{
			throw new RuntimeException("Failed to load image " + image_name + ".");
		}
	}
	
	protected static void configure_rendering(final Graphics graphics)
	{
		final var g2d = (Graphics2D)graphics;
		
		// Setup general and image rendering:
		g2d.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(
			RenderingHints.KEY_ALPHA_INTERPOLATION,
			RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2d.setRenderingHint(
			RenderingHints.KEY_COLOR_RENDERING,
			RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g2d.setRenderingHint(
			RenderingHints.KEY_DITHERING,
			RenderingHints.VALUE_DITHER_ENABLE);
		g2d.setRenderingHint(
			RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.setRenderingHint(
			RenderingHints.KEY_RENDERING,
			RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(
			RenderingHints.KEY_STROKE_CONTROL,
			RenderingHints.VALUE_STROKE_NORMALIZE);
		
		// Setup text font rendering:
		final var desktophints = (Map<?, ?>)Toolkit.getDefaultToolkit()
			.getDesktopProperty("awt.font.desktophints");
		if (desktophints != null)
		{
			g2d.addRenderingHints(desktophints);
		}
	}
	
	protected static final Font font_regular = load_font("Open-Sans-Regular.ttf");
	protected static final Font font_italic = load_font("Open-Sans-Italic.ttf");
	protected static final Font font_bold = load_font("Open-Sans-Bold.ttf");
	protected static final Font font_bold_italic = load_font("Open-Sans-BoldItalic.ttf");
	
	private static final FigurePresentation[] figures =
		{
			new FigurePresentation(
				Figure.pawn(true), "\u2659", ""),
			new FigurePresentation(
				Figure.rook(true), "\u2656", "R"),
			new FigurePresentation(
				Figure.knight(true), "\u2658", "N"),
			new FigurePresentation(
				Figure.bishop(true), "\u2657", "B"),
			new FigurePresentation(
				Figure.queen(true), "\u2655", "Q"),
			new FigurePresentation(
				Figure.king(true), "\u2654", "K"),
			new FigurePresentation(
				Figure.pawn(false), "\u265F", ""),
			new FigurePresentation(
				Figure.rook(false), "\u265C", "R"),
			new FigurePresentation(
				Figure.knight(false), "\u265E", "N"),
			new FigurePresentation(
				Figure.bishop(false), "\u265D", "B"),
			new FigurePresentation(
				Figure.queen(false), "\u265B", "Q"),
			new FigurePresentation(
				Figure.king(false), "\u265A", "K")
		};
	
	protected static final class FigurePresentation
	{
		protected final Figure figure;
		protected final String unicode;
		protected final String ascii;
		protected static final Font font = load_font("Chess-Merida-Unicode.ttf", 1.2f);
		
		private FigurePresentation(
			final Figure figure,
			final String unicode,
			final String ascii)
		{
			this.figure = figure;
			this.unicode = unicode;
			this.ascii = ascii;
		}
		
		protected static FigurePresentation get(final Figure figure)
		{
			for (final var f : Resources.figures)
			{
				if (f.figure == figure)
				{
					return f;
				}
			}
			throw new RuntimeException("ERROR: missing figure presentation.");
		}
		
		@Override public String toString()
		{
			return unicode;
		}
	}
}
