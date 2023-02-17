/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess;

import pmchess.gui.*;

public final class pmChess
{
	public static final String version = "2.0.0";
	public static final String[] about = {
		  "pmChess " + version + " by Christoff Bürger (christoff.buerger@gmail.com)."
		, "Source code available at https://github.com/christoff-buerger/pmChess."};
	
	// Static => enforce licenses exist:
	public static final String pmChess_license =
		Resources.load_text("license.txt");
	public static final String open_sans_license =
		Resources.load_text("fonts/Open-Sans-license.txt");
	public static final String chess_merida_unicode_license =
		Resources.load_text("fonts/Chess-Merida-Unicode-license.txt");
	public static final String dseg_license =
		Resources.load_text("fonts/DSEG-license.txt");
	
	/*
		Process command line arguments or, in case there are none, start GUI.
	*/
	public static void main(final String[] args)
	{
		if (args.length > 2)
		{
			System.out.println(" !!! ERROR: Too many command line arguments !!!");
			System.exit(1);
		}
		if (args.length >= 1)
		switch (args[0])
		{
		case "--help":
			System.out.println("""
				Usage:
				  No arguments: Start pmChess.
				  --help:       Print command line documentation.
				  --version:    Print pmChess version and license.
				  --scale n:    Scale graphical user interface by n%.
				                n must be a positive integer.""");
			System.exit(0);
		case "--version":
			System.out.println(about[0]);
			System.out.println(about[1]);
			System.out.println();
			System.out.println(pmChess_license);
			System.exit(0);
		case "--scale":
			try
			{
				Resources.write_base_scale_configuration(Integer.valueOf(args[1]));
			}
			catch (final Exception e)
			{
				System.out.println(" !!! ERROR: Invalid scale !!!");
				System.exit(1);
			}
			System.out.println(
				  "Graphical user interface now scaled to "
				+ (int)(Resources.read_base_scale_configuration() / 0.14f)
				+ "%.");
			System.exit(0);
			break;
		default:
			System.out.println(" !!! ERROR: Unknown command line arguments !!!");
			System.exit(1);
		}
		
		java.awt.EventQueue.invokeLater(
			new Runnable()
			{
				@Override public void run()
				{
					new GUI();
				}
			});
	}
}
