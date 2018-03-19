/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess;

import pmchess.gui.*;

public final class pmChess {
	public static final String version = "1.1.0";
	public static final String[] about = {
		"pmChess " + version + " by Christoff Bürger (christoff.buerger@gmail.com).",
		"Source code available at https://github.com/christoff-buerger/pmChess."
	};
	
	// Static => enforce licenses exist:
	public static final String pmChessLicense =
		Resources.loadText("license.txt");
	public static final String chessPiecesLicense =
		Resources.loadText("figures/LICENSE.txt");
	public static final String openSansLicense =
		Resources.loadText("fonts/Open-Sans-license.txt");
	public static final String chessMeridaUnicodeLicense =
		Resources.loadText("fonts/Chess-Merida-Unicode-license.txt");
	
	/*
		Print license and start GUI. No command line arguments supported.
	 */
	public static void main(final String[] args) {
		System.out.println(about[0]);
		System.out.println(about[1]);
		System.out.println();
		System.out.println(pmChessLicense);
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override public void run() {
				new GUI();
			}
		});
	}
}
