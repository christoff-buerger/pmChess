/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess;

import java.lang.*;

import java.util.function.*;

import pmchess.gui.*;

public final class pmChess
{
	public static final String version = "2.0.0";
	public static final String[] about = {
		  "pmChess " + version + " by Christoff Bürger (christoff.buerger@gmail.com)."
		, "Source code available at https://github.com/christoff-buerger/pmChess."};
	
	// Static => enforce licenses exist:
	public static final String[] licenses_subjects =
		{
			  "pmChess license"
			, "Open Sans license"
			, "Chess Merida Unicode license"
			, "Material Symbols license"
			, "DSEG license"
			, "OpenJDK license"
			, "Eclipse Temurin license"
		};
	public static final String[] licenses =
		{
			  Resources.load_text("licenses/pmChess-license.txt")
			, Resources.load_text("licenses/Open-Sans-license.txt")
			, Resources.load_text("licenses/Chess-Merida-Unicode-license.txt")
			, Resources.load_text("licenses/Material-Symbols-license.txt")
			, Resources.load_text("licenses/DSEG-license.txt")
			, Resources.load_text("licenses/OpenJDK-license.txt")
			, Resources.load_text("licenses/Eclipse-Temurin-license.txt")
		};
	
	/*
		Process command line arguments or, in case there are none, start GUI.
	*/
	public static void main(final String[] args)
	{
		final IntConsumer to_many_arguments = (limit) -> {
			if (args.length > limit)
			{
				System.out.println(" !!! ERROR: Too many command line arguments !!!");
				System.exit(1);
			}		
		};
		
		if (args.length >= 1)
		switch (args[0])
		{
		case "--help":
			to_many_arguments.accept(1);
			System.out.println("""
				Usage:
				  No arguments: Start pmChess.
				  --help:       Print command line documentation.
				  --version:    Print pmChess version and license.
				  --scale n:    Scale graphical user interface by n%.
				                n must be a positive integer.""");
			System.exit(0);
		case "--version":
			to_many_arguments.accept(1);
			System.out.println(about[0]);
			System.out.println(about[1]);
			System.out.println();
			System.out.println(licenses[0]);
			System.exit(0);
		case "--scale":
			to_many_arguments.accept(2);
			try
			{
				Resources.write_base_scale_configuration(Integer.valueOf(args[1]));
			}
			catch (final Exception e)
			{
				System.out.println(" !!! ERROR: Invalid or missing scale !!!");
				System.exit(1);
			}
			System.out.println(
				  "Graphical user interface now scaled to "
				+ Math.round(Resources.read_base_scale_configuration() / 0.14f)
				+ "%.");
			System.exit(0);
		default:
			System.out.println(" !!! ERROR: Unknown command line arguments !!!");
			System.exit(1);
		}
		
		java.awt.EventQueue.invokeLater(
			new Runnable()
			{
				@Override public void run()
				{
					final GUI gui = new GUI();
					gui.load_game(Resources.adjourned_game_file);
					Runtime.getRuntime().addShutdownHook(
						new Thread()
						{
							@Override public void run()
							{
								gui.save_game(Resources.adjourned_game_file);
							}
						});
				}
			});
	}
}
