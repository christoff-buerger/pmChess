/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.logic;

public final class Move {
	/*
		+---------------------------------------+
		| 24 bit move encoding                  |
		+---------------------------------------+
		| 32 - 25 | unused                      |
		| 24 - 21 | castling permission changes |
		|         | BLACK: 24 - 23              |
		|         | WHITE: 22 - 21              |
		|         | RIGHT: 24 & 22              |
		|         | LEFT:  23 & 21              |
		| 20 - 17 | Figure at destination       |
		| 16 - 13 | Figure moved                |
		| 12 - 10 | Y-coordinate destination    |
		|  9 -  7 | X-coordinate destination    |
		|  6 -  4 | Y-coordinate origin         |
		|  3 -  1 | X-coordinate origin         |
		+---------------------------------------+
		| Castling: King moves with |X - x| > 1 |
		|           RIGHT if X - x = 2          |
		|           LEFT  if x - X = 2          |
		+---------------------------------------+
		| En passant: Pawn capture (X != x)     |
		|           without a figure at the     |
		|           destination                 |
		+---------------------------------------+
	*/
	public static int encodeMove(
		final Board board,
		final int x,
		final int y,
		final int X,
		final int Y)
	{
		final Figure figure_moved = board.figure(x, y);
		final Figure figure_destination = board.figure(X, Y);
		final boolean player = figure_moved.owner;
		
		int encoded_move = x | y << 3 | X << 6 | Y << 9 | figure_moved.key << 12 |
			(figure_destination == null ? 0 : figure_destination.key << 16);
		
		// Update castling information:
		if (figure_moved.isKing()) { // Moving king disables castlings.
			final int player_offset = player ? 0 : 2;
			if (board.castlingAllowed(true, player))
				encoded_move |= 0x100000 << player_offset;
			if (board.castlingAllowed(false, player))
				encoded_move |= 0x200000 << player_offset;
		} else if (figure_moved.isRook()) { // Moving rook from start disables castlings.
			final int player_offset = player ? 0 : 2;
			if (y == (player ? 0 : 7)) {
				if (x == 0 && board.castlingAllowed(true, player)) {
					encoded_move |= 0x100000 << player_offset;
				} else if (x == 7 && board.castlingAllowed(false, player)) {
					encoded_move |= 0x200000 << player_offset;
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
		if (figure_destination != null && figure_destination.isRook()) {
			final int player_offset = player ? 2 : 0;
			if (Y == (player ? 7 : 0)) {
				if (X == 0 && board.castlingAllowed(true, !player)) {
					encoded_move |= 0x100000 << player_offset;
				} else if (X == 7 && board.castlingAllowed(false, !player)) {
					encoded_move |= 0x200000 << player_offset;
				}
			}
		}
		
		return encoded_move;
	}
	
	public static int x(final int move) {
		return move & 0x7;
	}
	
	public static int y(final int move) {
		return (move >> 3) & 0x7;
	}
	
	public static int X(final int move) {
		return (move >> 6) & 0x7;
	}
	
	public static int Y(final int move) {
		return (move >> 9) & 0x7;
	}
	
	public static Figure figure_moved(final int move) {
		return Figure.figures[(move >> 12) & 0xF];
	}
	
	public static Figure figure_destination(final int move) {
		return Figure.figures[(move >> 16) & 0xF];
	}
	
	/*
		Figure placed considering 8th rank pawns promotions.
	*/
	public static Figure figure_placed(final int move) {
		final Figure figure_moved = Move.figure_moved(move);
		final int Y = Move.Y(move);
		return figure_moved.isPawn() && (Y == 0 | Y == 7) ?
			Figure.queen(figure_moved.owner) : figure_moved;
	}
	
	/*
		Return 4 bits representing castling changes (cf. encodeMove).
	*/
	public static int castlingChanges(final int move) {
		return move >> 20; // Castling bits are highest of encoding => just shift.
	}
}
