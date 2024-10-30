/*
	This program and the accompanying materials are made available under the terms of the MIT
	license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.logic;

public final class Move
{
	private Move() // No instances.
	{
	}
		
	/*
		+---------------------------------------+
		| 29 bit move encoding                  |
		+---------------------------------------+
		| 32 - 30 | unused                      |
		| 29      | draw claim                  |
		| 28 - 25 | castling permission changes |
		|         | Black: 28 - 27              |
		|         | White: 26 - 25              |
		|         | kingside: 28 & 26           |
		|         | queenside:  27 & 25         |
		| 24 - 21 | figure placed               |
		|         | (needed for pawn promotion) |
		| 20 - 17 | figure at destination       |
		| 16 - 13 | figure moved                |
		| 12 - 10 | y-coordinate destination    |
		|  9 -  7 | x-coordinate destination    |
		|  6 -  4 | y-coordinate origin         |
		|  3 -  1 | x-coordinate origin         |
		+---------------------------------------+
		| Castling: King moves with |X - x| > 1 |
		|           kingside  if X - x = 2      |
		|           queenside if x - X = 2      |
		+---------------------------------------+
		| En passant: pawn capture (X != x)     |
		|           without a figure at the     |
		|           destination                 |
		+---------------------------------------+
	*/
	protected static int encode_move(
		  final Board board
		, final int x
		, final int y
		, final int X
		, final int Y
		, final Figure figure_placed)
	{
		final var figure_moved =
			board.figure(x, y);
		final var figure_destination =
			board.figure(X, Y);
		final var player =
			figure_moved.owner;
		
		var encoded_move =
			x
			| y << 3
			| X << 6
			| Y << 9
			| figure_moved.key << 12
			| (figure_destination == null ? 0 : figure_destination.key << 16)
			| figure_placed.key << 20;
		
		// Update castling information:
		if (figure_moved.is_king())
		{ // Moving king disables castlings:
			final var player_offset =
				player ? 0 : 2;
			if (board.castling_allowed(true, player))
			{
				encoded_move |= 0x1000000 << player_offset;
			}
			if (board.castling_allowed(false, player))
			{
				encoded_move |= 0x2000000 << player_offset;
			}
		}
		else if (figure_moved.is_rook())
		{ // Moving rook from start disables castlings:
			final var player_offset =
				player ? 0 : 2;
			if (y == (player ? 0 : 7))
			{
				if (x == 0 && board.castling_allowed(true, player))
				{
					encoded_move |= 0x1000000 << player_offset;
				}
				else if (x == 7 && board.castling_allowed(false, player))
				{
					encoded_move |= 0x2000000 << player_offset;
				}
			}
		}
		/*
			The following handles the VERY RARE case of:
			 - an opponent's rook is killed at its start position
			 - later, the opponent moves his other rook to the killed ones position
			 - and uses it for castling
			Thus, killing a rook on a start position disables its castling:
		*/
		if (figure_destination != null && figure_destination.is_rook())
		{
			final var player_offset =
				player ? 2 : 0;
			if (Y == (player ? 7 : 0))
			{
				if (X == 0 && board.castling_allowed(true, !player))
				{
					encoded_move |= 0x1000000 << player_offset;
				}
				else if (X == 7 && board.castling_allowed(false, !player))
				{
					encoded_move |= 0x2000000 << player_offset;
				}
			}
		}
		
		return encoded_move;
	}
	
	/*
		Set 'draw claim' bit of given move.
	*/
	protected static int encode_draw_claim(final int move)
	{
		return move | 0x20000000;
	}
	
	/*
		Draw claim without moving any piece (i.e., for current position).
	*/
	protected static int encode_moveless_draw_claim()
	{
		return 0xFFFFFFFF;
	}
	
	public static boolean is_moveless_draw_claim(final int move)
	{
		return move == 0xFFFFFFFF;
	}
	
	public static int x(final int move)
	{
		return move & 0x7;
	}
	
	public static int y(final int move)
	{
		return (move >> 3) & 0x7;
	}
	
	public static int X(final int move)
	{
		return (move >> 6) & 0x7;
	}
	
	public static int Y(final int move)
	{
		return (move >> 9) & 0x7;
	}
	
	public static Figure figure_moved(final int move)
	{
		return Figure.figures[(move >> 12) & 0xF];
	}
	
	public static Figure figure_destination(final int move)
	{
		return Figure.figures[(move >> 16) & 0xF];
	}
	
	public static Figure figure_placed(final int move)
	{
		return Figure.figures[(move >> 20) & 0xF];
	}
	
	/*
		Return 4 bits representing castling changes (cf. 'encode_move').
	*/
	public static int castling_changes(final int move)
	{
		return (move >> 24) & 0xF;
	}
	
	public static boolean draw_claim(final int move)
	{
		return (move & 0x20000000) != 0;
	}
}
