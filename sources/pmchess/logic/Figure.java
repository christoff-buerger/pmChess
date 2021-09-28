/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.logic;

public sealed abstract class Figure permits
	Figure.Pawn,
	Figure.Rook,
	Figure.Knight,
	Figure.Bishop,
	Figure.Queen,
	Figure.King
{
	private static int key_count = 1;
	
	protected static final Figure[] figures =
		{
			null,
			new Pawn(),
			new Rook(),
			new Knight(),
			new Bishop(),
			new Queen(),
			new King(),
			new Pawn(),
			new Rook(),
			new Knight(),
			new Bishop(),
			new Queen(),
			new King()
		};
	
	public static final Figure pawn(final boolean player)
	{
		return player ? figures[1] : figures[7];
	}
	
	public static final Figure rook(final boolean player)
	{
		return player ? figures[2] : figures[8];
	}
	
	public static final Figure knight(final boolean player)
	{
		return player ? figures[3] : figures[9];
	}
	
	public static final Figure bishop(final boolean player)
	{
		return player ? figures[4] : figures[10];
	}
	
	public static final Figure queen(final boolean player)
	{
		return player ? figures[5] : figures[11];
	}
	
	public static final Figure king(final boolean player)
	{
		return player ? figures[6] : figures[12];
	}
	
	public final boolean owner;
	protected final int key;
	
	private Figure()
	{
		if (key_count > 12)
		{
			throw new RuntimeException("ERROR: invalid figure instantiation.");
		}
		owner = key_count < 7;
		key = key_count++;
	}
	
	public final boolean is_pawn()
	{
		return key == 1 | key == 7;
	}
	
	public final boolean is_rook()
	{
		return key == 2 | key == 8;
	}
	
	public final boolean is_knight()
	{
		return key == 3 | key == 9;
	}
	
	public final boolean is_bishop()
	{
		return key == 4 | key == 10;
	}
	
	public final boolean is_queen()
	{
		return key == 5 | key == 11;
	}
	
	public final boolean is_king()
	{
		return key == 6 | key == 12;
	}
	
	protected abstract void compute_moves(final Board board, final int x, final int y);
	
	protected static final class Pawn extends Figure
	{
		private Pawn()
		{
		}
		
		private static int possible_enpassant(final Board board)
		{
			final var move = board.previous_move(board.turn() - 1);
			if (move == 0)
			{
				return -1;
			}
			if (Move.figure_moved(move).is_pawn())
			{
				final var distance = Move.y(move) - Move.Y(move);
				if (distance == 2 | distance == -2)
				{
					return Move.x(move);
				}
			}
			return -1;
		}
		
		private void moves_add(
			final Board board,
			final int x,
			final int y,
			final int X,
			final int Y)
		{
			if (Y == 0 | Y == 7)
			{ // Pawn promotions:
				board.moves_add(x, y, X, Y, Figure.queen(owner));
				board.moves_add(x, y, X, Y, Figure.rook(owner));
				board.moves_add(x, y, X, Y, Figure.knight(owner));
				board.moves_add(x, y, X, Y, Figure.bishop(owner));
				return;
			}
			board.moves_add(x, y, X, Y);
		}
		
		@Override protected void compute_moves(
			final Board board,
			final int x,
			final int y)
		{
			Figure f;
			final var xm1 = x - 1;
			final var xp1 = x + 1;
			if (owner)
			{
				final var en_passant = y == 4 ? possible_enpassant(board) : -1;
				var Y = y + 1;
				if (xm1 >= 0)
				{
					f = board.figure(xm1, Y);
					if (f == null ? xm1 == en_passant : f.owner != owner)
					{
						moves_add(board, x, y, xm1, Y);
					}
				}
				if (xp1 <= 7)
				{
					f = board.figure(xp1, Y);
					if (f == null ? xp1 == en_passant : f.owner != owner)
					{
						moves_add(board, x, y, xp1, Y);
					}
				}
				if (board.figure(x, Y) == null)
				{
					moves_add(board, x, y, x, Y);
					if (y == 1 && board.figure(x, ++Y) == null)
					{
						board.moves_add(x, y, x, Y);
					}
				}
			}
			else
			{
				final var en_passant = y == 3 ? possible_enpassant(board) : -1;
				var Y = y - 1;
				if (xm1 >= 0)
				{
					f = board.figure(xm1, Y);
					if (f == null ? xm1 == en_passant : f.owner != owner)
					{
						moves_add(board, x, y, xm1, Y);
					}
				}
				if (xp1 <= 7)
				{
					f = board.figure(xp1, Y);
					if (f == null? xp1 == en_passant : f.owner != owner)
					{
						moves_add(board, x, y, xp1, Y);
					}
				}
				if (board.figure(x, Y) == null)
				{
					moves_add(board, x, y, x, Y);
					if (y == 6 && board.figure(x, --Y) == null)
					{
						board.moves_add(x, y, x, Y);
					}
				}
			}
		}
	}
	
	protected static final class Rook extends Figure
	{
		private Rook()
		{
		}
		
		@Override protected void compute_moves(
			final Board board,
			final int x,
			final int y)
		{
			Figure f;
			for (var X = x - 1; X >= 0; X--)
			{
				f = board.figure(X, y);
				if (f == null)
				{
					board.moves_add(x, y, X, y);
					continue;
				}
				if (f.owner != owner)
				{
					board.moves_add(x, y, X, y);
				}
				break;
			}
			for (var X = x + 1; X <= 7; X++)
			{
				f = board.figure(X, y);
				if (f == null)
				{
					board.moves_add(x, y, X, y);
					continue;
				}
				if (f.owner != owner)
				{
					board.moves_add(x, y, X, y);
				}
				break;
			}
			for (var Y = y - 1; Y >= 0; Y--)
			{
				f = board.figure(x, Y);
				if (f == null)
				{
					board.moves_add(x, y, x, Y);
					continue;
				}
				if (f.owner != owner)
				{
					board.moves_add(x, y, x, Y);
				}
				break;
			}
			for (var Y = y + 1; Y <= 7; Y++)
			{
				f = board.figure(x, Y);
				if (f == null)
				{
					board.moves_add(x, y, x, Y);
					continue;
				}
				if (f.owner != owner)
				{
					board.moves_add(x, y, x, Y);
				}
				break;
			}
		}
	}
	
	protected static final class Knight extends Figure
	{
		private Knight()
		{
		}
		
		@Override protected void compute_moves(
			final Board board,
			final int x,
			final int y)
		{
			Figure f;
			final var xm1 = x - 1;
			final var xp1 = x + 1;
			final var ym1 = y - 1;
			final var ym2 = y - 2;
			final var yp1 = y + 1;
			final var yp2 = y + 2;
			final var ym1_valid = ym1 >= 0;
			final var ym2_valid = ym2 >= 0;
			final var yp1_valid = yp1 <= 7;
			final var yp2_valid = yp2 <= 7;
			if (xm1 >= 0)
			{
				if (ym2_valid)
				{
					f = board.figure(xm1, ym2);
					if (f == null || f.owner != owner)
					{
						board.moves_add(x, y, xm1, ym2);
					}
				}
				if (yp2_valid)
				{
					f = board.figure(xm1, yp2);
					if (f == null || f.owner != owner)
					{
						board.moves_add(x, y, xm1, yp2);
					}
				}
				final var xm2 = x - 2;
				if (xm2 >= 0)
				{
					if (ym1_valid)
					{
						f = board.figure(xm2, ym1);
						if (f == null || f.owner != owner)
						{
							board.moves_add(x, y, xm2, ym1);
						}
					}
					if (yp1_valid)
					{
						f = board.figure(xm2, yp1);
						if (f == null || f.owner != owner)
						{
							board.moves_add(x, y, xm2, yp1);
						}
					}
				}
			}
			if (xp1 <= 7)
			{
				if (ym2_valid)
				{
					f = board.figure(xp1, ym2);
					if (f == null || f.owner != owner)
					{
						board.moves_add(x, y, xp1, ym2);
					}
				}
				if (yp2_valid)
				{
					f = board.figure(xp1, yp2);
					if (f == null || f.owner != owner)
					{
						board.moves_add(x, y, xp1, yp2);
					}
				}
				final var xp2 = x + 2;
				if (xp2 <= 7)
				{
					if (ym1_valid)
					{
						f = board.figure(xp2, ym1);
						if (f == null || f.owner != owner)
						{
							board.moves_add(x, y, xp2, ym1);
						}
					}
					if (yp1_valid)
					{
						f = board.figure(xp2, yp1);
						if (f == null || f.owner != owner)
						{
							board.moves_add(x, y, xp2, yp1);
						}
					}
				}
			}
		}
	}
		
	protected static final class Bishop extends Figure
	{
		private Bishop()
		{
		}
		
		@Override protected void compute_moves(
			final Board board,
			final int x,
			final int y)
		{
			Figure f;
			final var xm1 = x - 1;
			final var xp1 = x + 1;
			final var ym1 = y - 1;
			final var yp1 = y + 1;
			for (int X = xm1, Y = ym1; X >= 0 & Y >= 0; X--, Y--)
			{
				f = board.figure(X, Y);
				if (f == null)
				{
					board.moves_add(x, y, X, Y);
					continue;
				}
				if (f.owner != owner)
				{
					board.moves_add(x, y, X, Y);
				}
				break;
			}
			for (int X = xp1, Y = ym1; X <= 7 & Y >= 0; X++, Y--)
			{
				f = board.figure(X, Y);
				if (f == null)
				{
					board.moves_add(x, y, X, Y);
					continue;
				}
				if (f.owner != owner)
				{
					board.moves_add(x, y, X, Y);
				}
				break;
			}
			for (int X = xm1, Y = yp1; X >= 0 & Y <= 7; X--, Y++)
			{
				f = board.figure(X, Y);
				if (f == null)
				{
					board.moves_add(x, y, X, Y);
					continue;
				}
				if (f.owner != owner)
				{
					board.moves_add(x, y, X, Y);
				}
				break;
			}
			for (int X = xp1, Y = yp1; X <= 7 & Y <= 7; X++, Y++)
			{
				f = board.figure(X, Y);
				if (f == null)
				{
					board.moves_add(x, y, X, Y);
					continue;
				}
				if (f.owner != owner)
				{
					board.moves_add(x, y, X, Y);
				}
				break;
			}
		}
	}
	
	protected static final class Queen extends Figure
	{
		private Queen()
		{
		}
		
		@Override protected void compute_moves(
			final Board board,
			final int x,
			final int y)
		{
			Figure.rook(owner).compute_moves(board, x, y);
			Figure.bishop(owner).compute_moves(board, x, y);
		}
	}
	
	protected static final class King extends Figure
	{
		private King()
		{
		}
		
		@Override protected void compute_moves(
			final Board board,
			final int x,
			final int y)
		{
			Figure f;
			final var xm1 = x - 1;
			final var xp1 = x + 1;
			final var ym1 = y - 1;
			final var yp1 = y + 1;
			final var ym1_valid = ym1 >= 0;
			final var yp1_valid = yp1 <= 7;
			if (xm1 >= 0)
			{
				f = board.figure(xm1, y);
				if (f == null || f.owner != owner)
				{
					board.moves_add(x, y, xm1, y);
				}
				if (ym1_valid)
				{
					f = board.figure(xm1, ym1);
					if (f == null || f.owner != owner)
					{
						board.moves_add(x, y, xm1, ym1);
					}
				}
				if (yp1_valid)
				{
					f = board.figure(xm1, yp1);
					if (f == null || f.owner != owner)
					{
						board.moves_add(x, y, xm1, yp1);
					}
				}
			}
			if (xp1 <= 7)
			{
				f = board.figure(xp1, y);
				if (f == null || f.owner != owner)
				{
					board.moves_add(x, y, xp1, y);
				}
				if (ym1_valid)
				{
					f = board.figure(xp1, ym1);
					if (f == null || f.owner != owner)
					{
						board.moves_add(x, y, xp1, ym1);
					}
				}
				if (yp1_valid)
				{
					f = board.figure(xp1, yp1);
					if (f == null || f.owner != owner)
					{
						board.moves_add(x, y, xp1, yp1);
					}
				}
			}
			if (ym1_valid)
			{
				f = board.figure(x, ym1);
				if (f == null || f.owner != owner)
				{
					board.moves_add(x, y, x, ym1);
				}
			}
			if (yp1_valid)
			{
				f = board.figure(x, yp1);
				if (f == null || f.owner != owner)
				{
					board.moves_add(x, y, x, yp1);
				}
			}
			final var opponent = !owner;
			if ((board.castling_allowed(true, owner)
				& board.figure(1, y) == null
				& board.figure(2, y) == null
				& board.figure(3, y) == null)
				&& !(board.threatens(opponent, x, y)
					|| board.threatens(opponent, 2, y)
					|| board.threatens(opponent, 3, y)))
			{
				board.moves_add(x, y, 2, y);
			}
			if ((board.castling_allowed(false, owner)
				& board.figure(5, y) == null
				& board.figure(6, y) == null)
				&& !(board.threatens(opponent, x, y)
					|| board.threatens(opponent, 5, y)
					|| board.threatens(opponent, 6, y)))
			{
				board.moves_add(x, y, 6, y);
			}
		}
	}
}
