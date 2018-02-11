/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.logic;

public final class Board {
	public static enum GameStatus {Normal, Stalemate, Check, Checkmate};
	
	private final Figure[][] board = {
		{Figure.rook(true),	Figure.pawn(true),
			null, null, null, null,
		 Figure.pawn(false),	Figure.rook(false)},
		{Figure.knight(true),	Figure.pawn(true),
			null, null, null, null,
		 Figure.pawn(false),	Figure.knight(false)},
		{Figure.bishop(true),	Figure.pawn(true),
			null, null, null, null,
		 Figure.pawn(false),	Figure.bishop(false)},
		{Figure.queen(true),	Figure.pawn(true),
			null, null, null, null,
		 Figure.pawn(false),	Figure.queen(false)},
		{Figure.king(true),	Figure.pawn(true),
			null, null, null, null,
		 Figure.pawn(false),	Figure.king(false)},
		{Figure.bishop(true),	Figure.pawn(true),
			null, null, null, null,
		 Figure.pawn(false),	Figure.bishop(false)},
		{Figure.knight(true),	Figure.pawn(true),
			null, null, null, null,
		 Figure.pawn(false),	Figure.knight(false)},
		{Figure.rook(true),	Figure.pawn(true),
			null, null, null, null,
		 Figure.pawn(false),	Figure.rook(false)}};
	
	private int castlings_allowed = 0xF;
	private boolean castling_done_w = false; // History useful for scoring.
	private boolean castling_done_b = false; // History useful for scoring.
	
	private boolean player = true;
	private int turn = 1;
	
	private int king_x_w = 4;
	private int king_y_w = 0;
	private int king_x_b = 4;
	private int king_y_b = 7;
	
	/*
		History of game situations, starting from the beginning to the current
		constellation. Each game situation is described by a move frame consisting of all
		moves possible in that situation, the actual move selected for execution and
		pointers to the previous and successor frame. The layout of individual move frames
		is (lowest to highest index within array):
		
		+-----------------------+---------------------------------------------------------+
		| Successor frame	| Index pointing directly after the last possible move of |
		| (single array element)| the frame, which is the beginning of the successor      |
		|			| frame in case a selected move is given.                 |
		+-----------------------+---------------------------------------------------------+
		| Predecessor frame	| -1, in case there is no predecessor frame (i.e., the    |
		| (single array element)| frame is the first), otherwise index pointing to the    |
		|			| beginning of the previous frame.                        |
		+-----------------------+---------------------------------------------------------+
		| Move selected		| 0, if no move is selected (i.e., the frame is the last),|
		| (single array element)| otherwise index pointing to one of the frame's possible |
		|			| moves.                                                  |
		+-----------------------+---------------------------------------------------------+
		| Possible moves	| Arbitrary many. The end is denoted by the successor     |
		| (many array elements)	| frame index.                                            |
		+-----------------------+---------------------------------------------------------+
		
		The beginning of the current frame is indexed by the 'moves_frame' field.
	*/
	private int moves_frame = 0;
	private final int[] moves = new int[16384]; { // Initialization:
		moves[0] = 3;
		moves[1] = -1;
		moves[2] = 0;
		moves_computePossible();
	}
	
	private void moves_computePossible() {
		for (int x = 0; x <= 7; x++) for (int y = 0; y <= 7; y++) {
			final Figure f = board[x][y];
			if (f != null && f.owner == player)
				f.computeMoves(this, x, y);
		}
	}
	
	/*
		Add a possible move to the current move frame and unset its selected move.
	*/
	protected void moves_add(final int x, final int y, final int X, final int Y) {
		int successor_frame = moves[moves_frame];
		moves[successor_frame] = Move.encodeMove(this, x, y, X, Y);
		moves[moves_frame] = ++successor_frame;
		moves[moves_frame + 2] = 0;
	}
	
	/*
		Return the index of the move selected for execution or 0 if no move is selected.
		The selected move of the current move frame is automatically set whenever one of
		its possible moves is SUCCESSFULLY executed via the 'execute' function. It is unset
		by 'moves_add' but NOT 'undo'.
	*/
	protected int moves_selected() {
		return moves[moves_frame + 2];
	}
	
	/*
		Return the index of the beginning of the possible moves of the current move frame.
	*/
	protected int moves_possible() {
		return moves_frame + 3;
	}
	
	/*
		Return a possible move of the current move frame. Returns 0 in case the given index
		is out-of-bounds (i.e., not pointing to a possible move of the current move frame).
		
		IMPORTANT: Moves threatening a player's own king are not filtered and instead
		detected when actually executed (cf. 'execute' function).
	*/
	protected int moves_possible(final int index) {
		return index < moves[moves_frame] & index > moves_frame + 2 ? moves[index] : 0;
	}
	
	public boolean execute(final int x, final int y, final int X, final int Y) {
		final int moves_end = moves[moves_frame];
		for (int i = moves_frame + 3; i < moves_end; i++) {
			final int move = moves[i];
			if (Move.x(move) == x && Move.y(move) == y &&
				Move.X(move) == X && Move.Y(move) == Y)
			{
				return execute(move);
			}
		}
		return false;
	}
	
	protected boolean execute(final int move) {
		// Update cached current game situation (figure constellation, king positions,
		//	castlings, active player and turn number):
		final int x = Move.x(move);
		final int X = Move.X(move);
		final int Y = Move.Y(move);
		final Figure figure_placed = Move.figure_placed(move);
		board[x][Move.y(move)] = null;
		board[X][Y] = figure_placed;
		if (figure_placed.isKing()) {
			if (player) {
				king_x_w = X;
				king_y_w = Y;
			} else {
				king_x_b = X;
				king_y_b = Y;
			}
			if (X == x - 2) { // Castling left:
				board[3][Y] = board[0][Y];
				board[0][Y] = null;
				if (figure_placed.owner) {
					castling_done_w = true;
				} else {
					castling_done_b = true;
				}
			} else if (X == x + 2) { // Castling right:
				board[5][Y] = board[7][Y];
				board[7][Y] = null;
				if (figure_placed.owner) {
					castling_done_w = true;
				} else {
					castling_done_b = true;
				}
			}
		}
		castlings_allowed ^= Move.castlingChanges(move);
		player = !player;
		turn++;
		// Update game history (push new current moves frame and compute possible moves):
		final int old_move_selected = moves[moves_frame + 2];
		moves[moves_frame + 2] = move; // TODO: push move INDEX, not the move, as selected.
		final int successor_frame = moves[moves_frame];
		moves[successor_frame] = successor_frame + 3;
		moves[successor_frame + 1] = moves_frame;
		moves[successor_frame + 2] = 0;
		moves_frame = successor_frame;
		if (check(!player)) { // Undo all changes if move was invalid (threatens own king):
			undo();
			moves[moves_frame + 2] = old_move_selected;
			return false;
		}
		moves_computePossible();
		return true;
	}
	
	public int undo() {
		// Restore game history (pop current moves frame):
		final int predecessor_frame = moves[moves_frame + 1];
		if (predecessor_frame == -1)
			return 0;
		moves_frame = predecessor_frame;
		// Restore cached current game situation (figure constellation, king positions,
		//	castlings, active player and turn number):
		final int move = moves[moves_frame + 2];// TODO: fix when moves_selected contains index not move
		final int x = Move.x(move);
		final int y = Move.y(move);
		final int X = Move.X(move);
		final int Y = Move.Y(move);
		final Figure figure_moved = Move.figure_moved(move);
		board[x][y] = figure_moved;
		board[X][Y] = Move.figure_destination(move);
		if (figure_moved.isKing()) {
			if (player) {
				king_x_b = x;
				king_y_b = y;
			} else {
				king_x_w = x;
				king_y_w = y;
			}
			if (X == x - 2) { // Castling left:
				board[0][Y] = board[3][Y];
				board[3][Y] = null;
				if (figure_moved.owner) {
					castling_done_w = false;
				} else {
					castling_done_b = false;
				}
			} else if (X == x + 2) { // Castling right:
				board[7][Y] = board[5][Y];
				board[5][Y] = null;
				if (figure_moved.owner) {
					castling_done_w = false;
				} else {
					castling_done_b = false;
				}
			}
		}
		castlings_allowed ^= Move.castlingChanges(move);
		player = !player;
		turn--;
		return move;
	}
	
	public Figure figure(final int x, final int y) {
		return board[x][y];
	}
	
	public boolean castlingAllowed(final boolean left, final boolean player) {
		return ((castlings_allowed >> ((left ? 0 : 1) + (player ? 0 : 2))) & 0x1) != 0;
	}
	
	public boolean castlingDone(final boolean player) {
		return player ? castling_done_w : castling_done_b;
	}
	
	public boolean player() {
		return player;
	}
	
	public int turn() {
		return turn;
	}
	
	public GameStatus status() {
		final int old_move_selected = moves[moves_frame + 2];
		final int moves_end = moves[moves_frame];
		for (int i = moves_frame + 3; i < moves_end; i++) {
			if (execute(/*TODO: just i when move index*/moves[i])) {
				undo();
				moves[moves_frame + 2] = old_move_selected;
				return check(player) ? GameStatus.Check : GameStatus.Normal;
			}
		}
		return check(player) ? GameStatus.Checkmate : GameStatus.Stalemate;
	}
	
	private boolean check(final boolean player) {
		return player ?
			threatens(false, king_x_w, king_y_w) :
			threatens(true, king_x_b, king_y_b);
	}
	
	public int previousMove(final int turn) {
		if (turn < 1 | turn >= this.turn)
			return 0;
		int previous_frame = moves_frame;
		for (int i = this.turn - turn; i-- > 0; previous_frame = moves[previous_frame + 1]);
		return moves[previous_frame + 2];
	}
	
	public boolean threatens(final boolean player, final int X, final int Y) {
		Figure f;
		
		// Check for pawns:
		final int ym1 = Y - 1;
		final int yp1 = Y + 1;
		final int xm1 = X - 1;
		final boolean xm1_valid = xm1 >= 0;
		final int xp1 = X + 1;
		final boolean xp1_valid = xp1 <= 7;
		final int y_pawn = player ? ym1 : yp1;
		if (y_pawn > 0 & y_pawn < 7) {
			if (xm1_valid) {
				f = board[xm1][y_pawn];
				if (f != null && f.owner == player && f.isPawn())
					return true;
			}
			if (xp1_valid) {
				f = board[xp1][y_pawn];
				if (f != null && f.owner == player && f.isPawn())
					return true;
			}
		}
		
		// Check for rooks and straight-line queens:
		for (int x = xm1; x >= 0; x--) {
			f = board[x][Y];
			if (f == null) continue;
			if (f.owner != player) break;
			if (f.isRook() || f.isQueen()) return true;
			break;
		}
		for (int x = xp1; x <= 7; x++) {
			f = board[x][Y];
			if (f == null) continue;
			if (f.owner != player) break;
			if (f.isRook() || f.isQueen()) return true;
			break;
		}
		for (int y = ym1; y >= 0; y--) {
			f = board[X][y];
			if (f == null) continue;
			if (f.owner != player) break;
			if (f.isRook() || f.isQueen()) return true;
			break;
		}
		for (int y = yp1; y <= 7; y++) {
			f = board[X][y];
			if (f == null) continue;
			if (f.owner != player) break;
			if (f.isRook() || f.isQueen()) return true;
			break;
		}
		
		// Check for bishops and diagonal-line queens:
		for (int x = xm1, y = ym1; x >= 0 & y >= 0; x--, y--) {
			f = board[x][y];
			if (f == null) continue;
			if (f.owner != player) break;
			if (f.isBishop() || f.isQueen()) return true;
			break;
		}
		for (int x = xp1, y = ym1; x <= 7 & y >= 0; x++, y--) {
			f = board[x][y];
			if (f == null) continue;
			if (f.owner != player) break;
			if (f.isBishop() || f.isQueen()) return true;
			break;
		}
		for (int x = xm1, y = yp1; x >= 0 & y <= 7; x--, y++) {
			f = board[x][y];
			if (f == null) continue;
			if (f.owner != player) break;
			if (f.isBishop() || f.isQueen()) return true;
			break;
		}
		for (int x = xp1, y = yp1; x <= 7 & y <= 7; x++, y++) {
			f = board[x][y];
			if (f == null) continue;
			if (f.owner != player) break;
			if (f.isBishop() || f.isQueen()) return true;
			break;
		}
		
		// Check for knights:
		final int ym2 = Y - 2;
		final boolean ym2_valid = ym2 >= 0;
		if (xm1_valid & ym2_valid) {
			f = board[xm1][ym2];
			if (f != null && f.owner == player && f.isKnight())
				return true;
		}
		final boolean ym1_valid = ym1 >= 0;
		final int xm2 = X - 2;
		final boolean xm2_valid = xm2 >= 0;
		if (xm2_valid & ym1_valid) {
			f = board[xm2][ym1];
			if (f != null && f.owner == player && f.isKnight())
				return true;
		}
		if (xp1_valid & ym2_valid) {
			f = board[xp1][ym2];
			if (f != null && f.owner == player && f.isKnight())
				return true;
		}
		final int xp2 = X + 2;
		final boolean xp2_valid = xp2 <= 7;
		if (xp2_valid & ym1_valid) {
			f = board[xp2][ym1];
			if (f != null && f.owner == player && f.isKnight())
				return true;
		}
		final int yp2 = Y + 2;
		final boolean yp2_valid = yp2 <= 7;
		if (xm1_valid & yp2_valid) {
			f = board[xm1][yp2];
			if (f != null && f.owner == player && f.isKnight())
				return true;
		}
		final boolean yp1_valid = yp1 <= 7;
		if (xm2_valid & yp1_valid) {
			f = board[xm2][yp1];
			if (f != null && f.owner == player && f.isKnight())
				return true;
		}
		if (xp1_valid & yp2_valid) {
			f = board[xp1][yp2];
			if (f != null && f.owner == player && f.isKnight())
				return true;
		}
		if (xp2_valid & yp1_valid) {
			f = board[xp2][yp1];
			if (f != null && f.owner == player && f.isKnight())
				return true;
		}
		
		// Check for king:
		if (xm1_valid) {
			f = board[xm1][Y];
			if (f != null && f.owner == player && f.isKing())
				return true;
			if (ym1_valid) {
				f = board[xm1][ym1];
				if (f != null && f.owner == player && f.isKing())
					return true;
			}
			if (yp1_valid) {
				f = board[xm1][yp1];
				if (f != null && f.owner == player && f.isKing())
					return true;
			}
		}
		if (xp1_valid) {
			f = board[xp1][Y];
			if (f != null && f.owner == player && f.isKing())
				return true;
			if (ym1_valid) {
				f = board[xp1][ym1];
				if (f != null && f.owner == player && f.isKing())
					return true;
			}
			if (yp1_valid) {
				f = board[xp1][yp1];
				if (f != null && f.owner == player && f.isKing())
					return true;
			}
		}
		if (ym1_valid) {
			f = board[X][ym1];
			if (f != null && f.owner == player && f.isKing())
				return true;
		}
		if (yp1_valid) {
			f = board[X][yp1];
			if (f != null && f.owner == player && f.isKing())
				return true;
		}
		
		return false;
	}	
}
