/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess;

import java.io.*;

import pmchess.gui.*;

public final class pmChess {
	public static final String[] about = {
		"pmChess version 1.1.0 by Christoff Bürger (christoff.buerger@gmail.com).",
		"Source code available at https://github.com/christoff-buerger/pmChess."
	};
	
	// Static => enforce licenses exist:
	public static final String pmChessLicense = loadText("license.txt");
	public static final String openSansLicense = loadText("gui/fonts/LICENSE.txt");
	public static final String chessPiecesLicense = loadText("gui/figures/LICENSE.txt");
	
	private static String loadText(final String file) {
		try {
			final InputStream stream = pmChess.class.getResourceAsStream(file);
			final ByteArrayOutputStream result = new ByteArrayOutputStream();
			final byte[] buffer = new byte[1024];
			int character;
			try {
				while ((character = stream.read(buffer)) != -1) {
					result.write(buffer, 0, character);
				}
			} finally {
				stream.close();
			}
			return result.toString("UTF-8");
		} catch (IOException e) {
			throw new RuntimeException("Failed to load text file " + file + ".");
		}
	}
	
	/*
		Print license and start GUI. No command line arguments supported.
	 */
	public static void main(final String[] args) {
		System.out.println(about[0]);
		System.out.println(about[1]);
		System.out.println();
		System.out.println(pmChessLicense);
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new GUI();
			}
		});
	}
}
