/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.logic;

public abstract class Figure {
	private static int key_count = 1;
	
	protected static final Figure[] figures = {
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
	
	public static final Figure pawn(final boolean player) {
		return player ? figures[1] : figures[7];
	}
	
	public static final Figure rook(final boolean player) {
		return player ? figures[2] : figures[8];
	}
	
	public static final Figure knight(final boolean player) {
		return player ? figures[3] : figures[9];
	}
	
	public static final Figure bishop(final boolean player) {
		return player ? figures[4] : figures[10];
	}
	
	public static final Figure queen(final boolean player) {
		return player ? figures[5] : figures[11];
	}
	
	public static final Figure king(final boolean player) {
		return player ? figures[6] : figures[12];
	}
	
	public final boolean owner;
	protected final int key;
	
	private Figure() {
		if (key_count > 12) {
			throw new RuntimeException(
				"IMPLEMENTATION ERROR: Invalid figure instantiation.");
		}
		this.owner = key_count < 7;
		this.key = key_count++;
	}
	
	public final boolean isPawn() {
		return key == 1 | key == 7;
	}
	
	public final boolean isRook() {
		return key == 2 | key == 8;
	}
	
	public final boolean isKnight() {
		return key == 3 | key == 9;
	}
	
	public final boolean isBishop() {
		return key == 4 | key == 10;
	}
	
	public final boolean isQueen() {
		return key == 5 | key == 11;
	}
	
	public final boolean isKing() {
		return key == 6 | key == 12;
	}
	
	protected abstract void computeMoves(final Board board, final int x, final int y);
	
	private static final class Pawn extends Figure {
		private static int possibleEnPassant(final Board board) {
			final int move = board.previousMove(board.turn() - 1);
			if (move == 0)
				return -1;
			if (Move.figure_moved(move).isPawn()) {
				final int distance = Move.y(move) - Move.Y(move);
				if (distance == 2 | distance == -2);
					return Move.x(move);
			}
			return -1;
		}
				
		@Override protected void computeMoves(final Board board, final int x, final int y) {
			Figure f;
			final int xm1 = x - 1;
			final int xp1 = x + 1;
			if (owner) {
				final int en_passant = y == 4 ? possibleEnPassant(board) : -1;
				int Y = y + 1;
				if (xm1 >= 0) {
					f = board.figure(xm1, Y);
					if (f == null) {
						if (xm1 == en_passant)
							board.moves_add(x, 4, xm1, Y);
					} else if (f.owner != owner) {
						board.moves_add(x, y, xm1, Y);
					}
				}
				if (xp1 <= 7) {
					f = board.figure(xp1, Y);
					if (f == null) {
						if (xp1 == en_passant)
							board.moves_add(x, 4, xp1, Y);
					} else if (f.owner != owner) {
						board.moves_add(x, y, xp1, Y);
					}
				}
				if (board.figure(x, Y) == null) {
					board.moves_add(x, y, x, Y);
					if (y == 1 && board.figure(x, ++Y) == null)
						board.moves_add(x, y, x, Y);
				}
			} else {
				final int en_passant = y == 3 ? possibleEnPassant(board) : -1;
				int Y = y - 1;
				if (xm1 >= 0) {
					f = board.figure(xm1, Y);
					if (f == null) {
						if (xm1 == en_passant)
							board.moves_add(x, 3, xm1, Y);
					} else if (f.owner != owner) {
						board.moves_add(x, y, xm1, Y);
					}
				}
				if (xp1 <= 7) {
					f = board.figure(xp1, Y);
					if (f == null) {
						if (xp1 == en_passant)
							board.moves_add(x, 3, xp1, Y);
					} else if (f.owner != owner) {
						board.moves_add(x, y, xp1, Y);
					}
				}
				if (board.figure(x, Y) == null) {
					board.moves_add(x, y, x, Y);
					if (y == 6 && board.figure(x, --Y) == null)
						board.moves_add(x, y, x, Y);
				}
			}
		}
	}
	
	private static final class Rook extends Figure {
		@Override protected void computeMoves(final Board board, final int x, final int y) {
			Figure f;
			for (int X = x - 1; X >= 0; X--) {
				f = board.figure(X, y);
				if (f == null) {
					board.moves_add(x, y, X, y);
					continue;
				}
				if (f.owner != owner)
					board.moves_add(x, y, X, y);
				break;
			}
			for (int X = x + 1; X <= 7; X++) {
				f = board.figure(X, y);
				if (f == null) {
					board.moves_add(x, y, X, y);
					continue;
				}
				if (f.owner != owner)
					board.moves_add(x, y, X, y);
				break;
			}
			for (int Y = y - 1; Y >= 0; Y--) {
				f = board.figure(x, Y);
				if (f == null) {
					board.moves_add(x, y, x, Y);
					continue;
				}
				if (f.owner != owner)
					board.moves_add(x, y, x, Y);
				break;
			}
			for (int Y = y + 1; Y <= 7; Y++) {
				f = board.figure(x, Y);
				if (f == null) {
					board.moves_add(x, y, x, Y);
					continue;
				}
				if (f.owner != owner)
					board.moves_add(x, y, x, Y);
				break;
			}
		}
	}
	
	private static final class Knight extends Figure {
		@Override protected void computeMoves(final Board board, final int x, final int y) {
			Figure f;
			final int xm1 = x - 1;
			final int xp1 = x + 1;
			final int ym1 = y - 1;
			final int ym2 = y - 2;
			final int yp1 = y + 1;
			final int yp2 = y + 2;
			final boolean ym1_valid = ym1 >= 0;
			final boolean ym2_valid = ym2 >= 0;
			final boolean yp1_valid = yp1 <= 7;
			final boolean yp2_valid = yp2 <= 7;
			if (xm1 >= 0) {
				if (ym2_valid) {
					f = board.figure(xm1, ym2);
					if (f == null || f.owner != owner)
						board.moves_add(x, y, xm1, ym2);
				}
				if (yp2_valid) {
					f = board.figure(xm1, yp2);
					if (f == null || f.owner != owner)
						board.moves_add(x, y, xm1, yp2);
				}
				final int xm2 = x - 2;
				if (xm2 >= 0) {
					if (ym1_valid) {
						f = board.figure(xm2, ym1);
						if (f == null || f.owner != owner)
							board.moves_add(x, y, xm2, ym1);
					}
					if (yp1_valid) {
						f = board.figure(xm2, yp1);
						if (f == null || f.owner != owner)
							board.moves_add(x, y, xm2, yp1);
					}
				}
			}
			if (xp1 <= 7) {
				if (ym2_valid) {
					f = board.figure(xp1, ym2);
					if (f == null || f.owner != owner)
						board.moves_add(x, y, xp1, ym2);
				}
				if (yp2_valid) {
					f = board.figure(xp1, yp2);
					if (f == null || f.owner != owner)
						board.moves_add(x, y, xp1, yp2);
				}
				final int xp2 = x + 2;
				if (xp2 <= 7) {
					if (ym1_valid) {
						f = board.figure(xp2, ym1);
						if (f == null || f.owner != owner)
							board.moves_add(x, y, xp2, ym1);
					}
					if (yp1_valid) {
						f = board.figure(xp2, yp1);
						if (f == null || f.owner != owner)
							board.moves_add(x, y, xp2, yp1);
					}
				}
			}
		}
	}
		
	private static final class Bishop extends Figure {
		@Override protected void computeMoves(final Board board, final int x, final int y) {
			Figure f;
			final int xm1 = x - 1;
			final int xp1 = x + 1;
			final int ym1 = y - 1;
			final int yp1 = y + 1;
			for (int X = xm1, Y = ym1; X >= 0 & Y >= 0; X--, Y--) {
				f = board.figure(X, Y);
				if (f == null) {
					board.moves_add(x, y, X, Y);
					continue;
				}
				if (f.owner != owner)
					board.moves_add(x, y, X, Y);
				break;
			}
			for (int X = xp1, Y = ym1; X <= 7 & Y >= 0; X++, Y--) {
				f = board.figure(X, Y);
				if (f == null) {
					board.moves_add(x, y, X, Y);
					continue;
				}
				if (f.owner != owner)
					board.moves_add(x, y, X, Y);
				break;
			}
			for (int X = xm1, Y = yp1; X >= 0 & Y <= 7; X--, Y++) {
				f = board.figure(X, Y);
				if (f == null) {
					board.moves_add(x, y, X, Y);
					continue;
				}
				if (f.owner != owner)
					board.moves_add(x, y, X, Y);
				break;
			}
			for (int X = xp1, Y = yp1; X <= 7 & Y <= 7; X++, Y++) {
				f = board.figure(X, Y);
				if (f == null) {
					board.moves_add(x, y, X, Y);
					continue;
				}
				if (f.owner != owner)
					board.moves_add(x, y, X, Y);
				break;
			}
		}
	}
	
	private static final class Queen extends Figure {
		@Override protected void computeMoves(final Board board, final int x, final int y) {
			Figure.rook(owner).computeMoves(board, x, y);
			Figure.bishop(owner).computeMoves(board, x, y);
		}
	}
	
	private static final class King extends Figure {
		@Override protected void computeMoves(final Board board, final int x, final int y) {
			Figure f;
			final int xm1 = x - 1;
			final int xp1 = x + 1;
			final int ym1 = y - 1;
			final int yp1 = y + 1;
			final boolean ym1_valid = ym1 >= 0;
			final boolean yp1_valid = yp1 <= 7;
			if (xm1 >= 0) {
				f = board.figure(xm1, y);
				if (f == null || f.owner != owner)
					board.moves_add(x, y, xm1, y);
				if (ym1_valid) {
					f = board.figure(xm1, ym1);
					if (f == null || f.owner != owner)
						board.moves_add(x, y, xm1, ym1);
				}
				if (yp1_valid) {
					f = board.figure(xm1, yp1);
					if (f == null || f.owner != owner)
						board.moves_add(x, y, xm1, yp1);
				}
			}
			if (xp1 <= 7) {
				f = board.figure(xp1, y);
				if (f == null || f.owner != owner)
					board.moves_add(x, y, xp1, y);
				if (ym1_valid) {
					f = board.figure(xp1, ym1);
					if (f == null || f.owner != owner)
						board.moves_add(x, y, xp1, ym1);
				}
				if (yp1_valid) {
					f = board.figure(xp1, yp1);
					if (f == null || f.owner != owner)
						board.moves_add(x, y, xp1, yp1);
				}
			}
			if (ym1_valid) {
				f = board.figure(x, ym1);
				if (f == null || f.owner != owner)
					board.moves_add(x, y, x, ym1);
			}
			if (yp1_valid) {
				f = board.figure(x, yp1);
				if (f == null || f.owner != owner)
					board.moves_add(x, y, x, yp1);
			}
			final boolean opponent = !owner;
			if ((board.castlingAllowed(true, owner) &
				board.figure(1, y) == null &
				board.figure(2, y) == null &
				board.figure(3, y) == null) && !(
				board.threatens(opponent, x, y) ||
				board.threatens(opponent, 2, y) ||
				board.threatens(opponent, 3, y)))
			{
				board.moves_add(x, y, 2, y);
			}
			if ((board.castlingAllowed(false, owner) &
				board.figure(5, y) == null &
				board.figure(6, y) == null) && !(
				board.threatens(opponent, x, y) ||
				board.threatens(opponent, 5, y) ||
				board.threatens(opponent, 6, y)))
			{
				board.moves_add(x, y, 6, y);
			}
		}
	}
}
