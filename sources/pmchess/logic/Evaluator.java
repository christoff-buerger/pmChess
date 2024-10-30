/*
	This program and the accompanying materials are made available under the terms of the MIT
	license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.logic;

public final class Evaluator
{
	private static final java.util.Random random =
		new java.util.Random();
	private static final int[] random_shifts =
		{0, 0, 0, 0, 0, 5, 5, 5, 10, 10, 20};
	
	private static final int[] value_table =
		{
			  0  // null
		// White figures:
			, 1  // pawn
			, 5  // rook
			, 3  // knight
			, 3  // bishop
			, 9  // queen
			, 10 // king
		// Black figures:
			, 1
			, 5
			, 3
			, 3
			, 9
			, 10
		};
	
	private static final int[][] mobility_table =
		{
			  { // null (no figure; never accessed):
				    0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0
			  }
					/* white figures */
			, { // Pawn (no bonus or penalty):
				    0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0
			  }
			, { // Rook bonus (6-8: 3 points, 9-11: 8 points, 12-14: 14 points):
				    0,   0,   0,   0,   0,   0,   3,   3,   3,   8,   8
				,   8,  14,  14,  14,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0
			  }
			, { // Knight bonus (2 points for each field the knight can be moved to):
				    0,   2,   4,   6,   8,  10,  12,  14,  16,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0
			  }
			, { // Bishop penalty (0-3: -10 points, 4-6: -6 points, 7-9: -2 points):
				  -10, -10, -10, -10,  -6,  -6,  -6,  -2,  -2,  -2,   0
				,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0
			  }
			, { // Queen penalty (0-7: -15 points, 8-13: -6 points):
				  -15, -15, -15, -15, -15, -15, -15, -15,  -6,  -6,  -6
				,  -6,  -6,  -6,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0
			  }
			, { // King (no mobility bonus or penalty):
				    0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0
			  }
					/* black figures */
			, { // Pawn:
				    0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0
			  }
			, { // Rook:
				    0,   0,   0,   0,   0,   0,   3,   3,   3,   8,   8
				,   8,  14,  14,  14,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0
			  }
			, { // Knight:
				    0,   2,   4,   6,   8,  10,  12,  14,  16,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0
			  }
			, { // Bishop:
				  -10, -10, -10, -10,  -6,  -6,  -6,  -2,  -2,  -2,   0
				,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0
			  }
			, { // Queen:
				  -15, -15, -15, -15, -15, -15, -15, -15,  -6,  -6,  -6
				,  -6,  -6,  -6,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0
			  }
			, { // King:
				    0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0
				,   0,   0,   0,   0,   0,   0,   0,   0
			  }
		};
	
	public int score(final Board board, final boolean player)
	{
		final int[][] pawns =
			{
				  {0, 0, 0, 0, 0, 0, 0, 0, 0, 0} // player
				, {0, 0, 0, 0, 0, 0, 0, 0, 0, 0} // opponent
			};
		
		// Material evaluation (also counts pawns for later pawn formation evaluation):
		var material = 0;
		for (var x = 7; x >= 0; x--)
		{
			for (var y = 7; y >= 0; y--)
			{
				final var f = board.figure(x, y);
				if (f == null)
				{
					continue;
				}
				if (f.owner == player)
				{
					material += value_table[f.key];
					if (f.is_pawn())
					{
						pawns[0][x + 1]++;
					}
					continue;
				}
				material -= value_table[f.key];
				if (f.is_pawn())
				{
					pawns[1][x + 1]++;
				}
			}
		}
		
		// Pawn formation evaluation:
		var pawn_formation = 0;
		for (int x = 7
			, lp = pawns[0][7], mp = pawns[0][8], rp = pawns[0][9]
			, lo = pawns[1][7], mo = pawns[1][8], ro = pawns[1][9];
			x > 0;
			  rp = mp, mp = lp, lp = pawns[0][--x]
			, ro = mo, mo = lo, lo = pawns[1][x])
		{
			if (mp == 0)
			{
				continue;
			}
			pawn_formation -= (mp - 1) * 2; // Doubled pawns penalty.
			if (lp == 0 & rp == 0)
			{
				pawn_formation -= 7; // Isolated pawn penalty.
			}
			if (lo == 0 & ro == 0)
			{
				pawn_formation += 12; // Breached pawn bonus.
			}
		}
		
		// Castling bonus and penalties:
		var development = 0;
		if (board.castling_done(player))
		{
			development = 10;
		}
		else
		{
			final var right =
				board.castling_allowed(false, player);
			development = board.castling_allowed(true, player)
				? (right ? 0 : -5)
				: (right ? -5 : -12);
		}
		
		// Unmoved pawn penalties:
		final var base_rank =
			player ? 1 : 6;
		var f =
			board.figure(3, base_rank);
		if (f != null && f.is_pawn() && f.owner == player)
		{
			development -= 4;
		}
		f = board.figure(4, base_rank);
		if (f != null && f.is_pawn() && f.owner == player)
		{
			development -= 4;
		}
		f = board.figure(2, base_rank);
		if (f != null && f.is_pawn() && f.owner == player)
		{
			development -= 3;
		}
		f = board.figure(5, base_rank);
		if (f != null && f.is_pawn() && f.owner == player)
		{
			development -= 3;
		}
		
		// Mobility evaluation:
		var mobility = 0;
		var index =
			board.moves_possible();
		var move =
			board.moves_possible(index);
		var f_current =
			Move.figure_moved(move);
		var n_current = 0;
		for (int x_current = Move.x(move), y_current = Move.y(move);
			move != 0;
			move = board.moves_possible(++index), n_current++)
		{
			final var x =
				Move.x(move);
			final var y =
				Move.y(move);
			if (x_current == x & y_current == y)
			{
				continue;
			}
			mobility += mobility_table[f_current.key][n_current];
			x_current = x;
			y_current = y;
			f_current = Move.figure_moved(move);
			n_current = 0;
		}
		if (f_current != null)
		{
			mobility += mobility_table[f_current.key][n_current];
		}
		
		// Weight and sum up scoring criteria:
		return 15 * material
			+ 3 * pawn_formation
			+ 2 * development
			+ mobility
			+ random_shifts[random.nextInt(random_shifts.length)];
	}
}
