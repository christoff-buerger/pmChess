/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.logic;

public final class Board
{
	public static enum GameStatus {
		  Normal
		, Check
		, Checkmate
		, Stalemate
		, Draw
	};
	public static enum DrawStatus {
		  NoDrawPotential
		, AutomaticRepetition
		, AutomaticMoveRule
		, ClaimedRepetition
		, ClaimedMoveRule
	};
	
	private final Figure[][] board =
		{
		  {
			  Figure.rook(true)
			, Figure.pawn(true)
			, null
			, null
			, null
			, null
			, Figure.pawn(false)
			, Figure.rook(false)
		  }
		, {
			  Figure.knight(true)
			, Figure.pawn(true)
			, null
			, null
			, null
			, null
			, Figure.pawn(false)
			, Figure.knight(false)
		  }
		, {
			  Figure.bishop(true)
			, Figure.pawn(true)
			, null
			, null
			, null
			, null
			, Figure.pawn(false)
			, Figure.bishop(false)
		  }
		, {
			  Figure.queen(true)
			, Figure.pawn(true)
			, null
			, null
			, null
			, null
			, Figure.pawn(false)
			, Figure.queen(false)
		  }
		, {
			  Figure.king(true)
			, Figure.pawn(true)
			, null
			, null
			, null
			, null
			, Figure.pawn(false)
			, Figure.king(false)
		  }
		, {
			  Figure.bishop(true)
			, Figure.pawn(true)
			, null
			, null
			, null
			, null
			, Figure.pawn(false)
			, Figure.bishop(false)
		  }
		, {
			  Figure.knight(true)
			, Figure.pawn(true)
			, null
			, null
			, null
			, null
			, Figure.pawn(false)
			, Figure.knight(false)
		  }
		, {
			  Figure.rook(true)
			, Figure.pawn(true)
			, null
			, null
			, null
			, null
			, Figure.pawn(false)
			, Figure.rook(false)
		  }
		};
	
	private int king_x_w = 4;
	private int king_y_w = 0;
	private int king_x_b = 4;
	private int king_y_b = 7;
	
	private int castlings_allowed = 0xF;
	private boolean castling_done_w = false; // History useful for scoring.
	private boolean castling_done_b = false; // History useful for scoring.
	
	private boolean player = true;
	private int turn = 1;
	
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
		| Move selected		| 0, if no move is selected (i.e., the frame is the last).|
		| (single array element)| Otherwise one of the frame's possible moves, potentially|
		|			| with its 'draw claim' bit set, or                       |
		|                       | 'Move.encode_moveless_draw_claim()' denoting a draw     |
                |                       | claim for the current position without moving any piece.|
		+-----------------------+---------------------------------------------------------+
		| Possible moves	| Arbitrary many. The end is denoted by the successor     |
		| (many array elements)	| frame index. IMPORTANT: Just the moves as such are      |
		|                       | stored; their 'draw claim' bit is never set             |
		+-----------------------+---------------------------------------------------------+
		
		The beginning of the current frame is indexed by the 'moves_frame' field.
	*/
	private int moves_frame = 0;
	private final int[] moves = new int[16384];
	
	/*
		Initialization of 'moves':
	*/
	{
		moves[0] = 3;
		moves[1] = -1;
		moves[2] = 0;
		moves_compute_possible();
	}
	
	private void moves_compute_possible()
	{
		for (var x = 0; x <= 7; x++)
		{
			for (var y = 0; y <= 7; y++)
			{
				final var f = board[x][y];
				if (f != null && f.owner == player)
				{
					f.compute_moves(this, x, y);
				}
			}
		}
	}
	
	/*
		Add a possible move to the current move frame.
	*/
	protected void moves_add(
		  final int x
		, final int y
		, final int X
		, final int Y
		, final Figure figure_placed)
	{
		final var successor_frame = moves[moves_frame];
		moves[successor_frame] = Move.encode_move(this, x, y, X, Y, figure_placed);
		moves[moves_frame] = successor_frame + 1;
	}
	
	protected void moves_add(final int x, final int y, final int X, final int Y)
	{
		moves_add(x, y, X, Y, board[x][y]);
	}
	
	/*
		Return the move selected for execution or 0 if no move is selected.
		The selected move of the current move frame is automatically set whenever one of
		its possible moves is SUCCESSFULLY executed via the 'execute' function; it is unset
		by 'undo'.
	*/
	protected int moves_selected()
	{
		return moves[moves_frame + 2];
	}
	
	/*
		Return the index of the beginning of the possible moves of the current move frame.
	*/
	protected int moves_possible()
	{
		return moves_frame + 3;
	}
	
	/*
		Return a possible move of the current move frame. Returns 0 in case the given index
		is out-of-bounds (i.e., not pointing to a possible move of the current move frame).
		
		IMPORTANT: Moves threatening a player's own king are not filtered and instead
		detected when actually executed (cf. 'execute' function).
	*/
	protected int moves_possible(final int index)
	{
		return index < moves[moves_frame] & index > moves_frame + 2 ? moves[index] : 0;
	}
	
	/*
		Execute given move if, and only if, it is valid and the game is not already finished.
		Execution function for the GUI.
	*/
	public boolean execute(
		  final int x
		, final int y
		, final int X
		, final int Y
		, final Figure figure_placed
		, final boolean draw_claim)
	{
		final var game_status = status();
		if (game_status == GameStatus.Normal || game_status == GameStatus.Check)
		{
			final var moves_end = moves[moves_frame];
			for (var i = moves_frame + 3; i < moves_end; i++)
			{
				final var move = moves[i];
				if (Move.x(move) == x && Move.y(move) == y
					&& Move.X(move) == X && Move.Y(move) == Y
					&& Move.figure_placed(move) == figure_placed)
				{
					return execute(draw_claim
						? Move.encode_draw_claim(move)
						: move);
				}
			}
		}
		return false;
	}
	
	/*
		Claim a draw without moving any piece if, and only if, such claim is valid.
		Execution function for GUI.
	*/
	public boolean execute_moveless_draw_claim()
	{
		final var game_status = status();
		return (game_status == GameStatus.Normal || game_status == GameStatus.Check)
			&& (draw_move_rules_status() >= 50 || draw_repetition_status() >= 3)
			&& execute(Move.encode_moveless_draw_claim());
	}
	
	/*
		Execute the given encoded move if, and only if, it does not threaten the own king.
		The current game status is NOT checked to avoid the high costs of its computation;
		hence, moves are executed even if the game is already drawn and draw claims are
		not checked for validity.
		For internal use by game logic only (e.g., 'Search'), never the GUI.
	*/
	protected boolean execute(final int move)
	{
		// Update cached current game situation (figure constellation, king positions,
		//	castlings, active player and turn number):
		if (!Move.is_moveless_draw_claim(move))
		{
			final var x = Move.x(move);
			final var y = Move.y(move);
			final var X = Move.X(move);
			final var Y = Move.Y(move);
			final var figure_placed = Move.figure_placed(move);
			board[x][y] = null;
			board[X][Y] = figure_placed;
			if (figure_placed.is_king())
			{
				if (player)
				{
					king_x_w = X;
					king_y_w = Y;
				}
				else
				{
					king_x_b = X;
					king_y_b = Y;
				}
				if (X == x - 2)
				{ // Castling queenside:
					board[3][Y] = board[0][Y];
					board[0][Y] = null;
					if (figure_placed.owner)
					{
						castling_done_w = true;
					}
					else
					{
						castling_done_b = true;
					}
				}
				else if (X == x + 2)
				{ // Castling kingside:
					board[5][Y] = board[7][Y];
					board[7][Y] = null;
					if (figure_placed.owner)
					{
						castling_done_w = true;
					}
					else
					{
						castling_done_b = true;
					}
				}
			}
			else if (figure_placed.is_pawn()
				&& X != x
				&& Move.figure_destination(move) == null)
			{
				board[X][y] = null; // perform en passant capture
			}
			castlings_allowed ^= Move.castling_changes(move);
		}
		player = !player;
		turn++;
		// Update game history (push new current moves frame and compute possible moves):
		moves[moves_frame + 2] = move;
		final var successor_frame = moves[moves_frame];
		moves[successor_frame] = successor_frame + 3;
		moves[successor_frame + 1] = moves_frame;
		moves[successor_frame + 2] = 0;
		moves_frame = successor_frame;
		if (!Move.is_moveless_draw_claim(move) && check(!player))
		{ // Undo all changes if move threatens own king:
			undo();
			return false;
		}
		moves_compute_possible();
		return true;
	}
	
	public int undo()
	{
		if (turn == 1)
		{
			return 0;
		}
		// Restore game history (pop current moves frame and reset selected move):
		moves_frame = moves[moves_frame + 1];
		final var move = moves[moves_frame + 2];
		moves[moves_frame + 2] = 0;
		// Restore cached current game situation (figure constellation, king positions,
		//	castlings, active player and turn number):
		if (!Move.is_moveless_draw_claim(move))
		{
			final var x = Move.x(move);
			final var y = Move.y(move);
			final var X = Move.X(move);
			final var Y = Move.Y(move);
			final var figure_moved = Move.figure_moved(move);
			final var figure_destination = Move.figure_destination(move);
			board[x][y] = figure_moved;
			board[X][Y] = figure_destination;
			if (figure_moved.is_king())
			{
				if (player)
				{
					king_x_b = x;
					king_y_b = y;
				}
				else
				{
					king_x_w = x;
					king_y_w = y;
				}
				if (X == x - 2)
				{ // Castling queenside:
					board[0][Y] = board[3][Y];
					board[3][Y] = null;
					if (figure_moved.owner)
					{
						castling_done_w = false;
					}
					else
					{
						castling_done_b = false;
					}
				}
				else if (X == x + 2)
				{ // Castling kingside:
					board[7][Y] = board[5][Y];
					board[5][Y] = null;
					if (figure_moved.owner)
					{
						castling_done_w = false;
					}
					else
					{
						castling_done_b = false;
					}
				}
			}
			else if (figure_moved.is_pawn() && X != x && figure_destination == null)
			{ // Undo en passant capture:
				board[X][y] = Figure.pawn(!figure_moved.owner);
			}
			castlings_allowed ^= Move.castling_changes(move);
		}
		player = !player;
		turn--;
		return move;
	}
	
	public int previous_move(final int turn)
	{
		if (turn < 1 | turn >= this.turn)
		{
			return 0;
		}
		var previous_frame = moves_frame;
		for (var i = this.turn - turn;
			i-- > 0;
			previous_frame = moves[previous_frame + 1])
		{
		}
		return moves[previous_frame + 2];
	}
	
	public Figure figure(final int x, final int y)
	{
		return board[x][y];
	}
	
	public boolean castling_allowed(final boolean queenside, final boolean player)
	{
		return ((castlings_allowed >> ((queenside ? 0 : 1) + (player ? 0 : 2))) & 0x1) != 0;
	}
	
	public boolean castling_done(final boolean player)
	{
		return player ? castling_done_w : castling_done_b;
	}
	
	public boolean player()
	{
		return player;
	}
	
	/*
		Current ply number (number of performed half-moves plus one).
	*/
	public int turn()
	{
		return turn;
	}
	
	/*
		Current move number (each move has two turns).
	*/
	public int move()
	{
		return player ? (turn / 2) + 1 : turn / 2;
	}
	
	/*
		Move number the given ply is part of.
	*/
	public static int move(final int turn)
	{
		return turn % 2 == 0 ? turn / 2 : (turn / 2) + 1;
	}
	
	/*
		Number of repeating game positions so far (including current position):
	*/
	public int draw_repetition_status()
	{
		/*
		// TODO: Broken because the other player's moves must be equivalent too!
		//       And because we need to count ALL repetitions (i.e., consider any
		//       previous repeated game positions as well).
		var repetitions_count = 0;
		final var moves_count = moves[moves_frame] - (moves_frame + 3);
		for (var previous_frame = moves[moves_frame + 1];
			previous_frame >= 0;
			previous_frame = moves[previous_frame + 1])
		{
			if (moves[previous_frame] - (previous_frame + 3) != moves_count)
			{
				continue;
			}
			repetitions_count++;
			for (var i = moves_count + 2; i > 2; i--)
			{
				if (moves[previous_frame + i] != moves[moves_frame + i])
				{
					repetitions_count--;
					break;
				}
			}
		}
		return repetitions_count;
		//*/
		return 0;
	}
	
	/*
		Number of preceding turns without capture or pawn moves (excluding current turn):
	*/
	public int draw_move_rules_status()
	{
		return 0; // TODO
	}
	
	/*
		Type and reason for draw, IF the position is a draw; it might still be a checkmate.
		Only 'status()' does all checks to conclude if the situation indeed is a draw.
		Hence, this method is only reliable in its answer if 'status() == GameStatus.Draw'.
	*/
	public DrawStatus draw_status()
	{
		final var moves = draw_move_rules_status();
		final var repetitions = draw_repetition_status();
		if (Move.draw_claim(previous_move(turn - 1)))
		{
			if (moves >= 50)
			{
				return DrawStatus.ClaimedMoveRule;
			}
			if (repetitions >= 3)
			{
				return DrawStatus.ClaimedRepetition;
			}
		}
		if (moves == 75)
		{
			return DrawStatus.AutomaticMoveRule;
		}
		if (repetitions == 5)
		{
			return DrawStatus.AutomaticRepetition;
		}
		return DrawStatus.NoDrawPotential;
	}
	
	public GameStatus status()
	{
		final var moves_end = moves[moves_frame];
		for (var i = moves_frame + 3; i < moves_end; i++)
		{
			if (execute(moves[i]))
			{
				undo();
				if (draw_status() != DrawStatus.NoDrawPotential)
				{
					return GameStatus.Draw;
				}
				return check(player) ? GameStatus.Check : GameStatus.Normal;
			}
		}
		return check(player) ? GameStatus.Checkmate : GameStatus.Stalemate;
	}
	
	protected boolean check(final boolean player)
	{
		return player
			? threatens(false, king_x_w, king_y_w)
			: threatens(true, king_x_b, king_y_b);
	}
	
	protected boolean threatens(final boolean player, final int X, final int Y)
	{
		Figure f;
		
		// Check for pawns:
		final var ym1 = Y - 1;
		final var yp1 = Y + 1;
		final var xm1 = X - 1;
		final var xm1_valid = xm1 >= 0;
		final var xp1 = X + 1;
		final var xp1_valid = xp1 <= 7;
		final var y_pawn = player ? ym1 : yp1;
		if (y_pawn > 0 & y_pawn < 7)
		{
			if (xm1_valid)
			{
				f = board[xm1][y_pawn];
				if (f != null && f.owner == player && f.is_pawn())
				{
					return true;
				}
			}
			if (xp1_valid)
			{
				f = board[xp1][y_pawn];
				if (f != null && f.owner == player && f.is_pawn())
				{
					return true;
				}
			}
		}
		
		// Check for rooks and straight-line queens:
		for (var x = xm1; x >= 0; x--)
		{
			f = board[x][Y];
			if (f == null)
			{
				continue;
			}
			if (f.owner != player)
			{
				break;
			}
			if (f.is_rook() || f.is_queen())
			{
				return true;
			}
			break;
		}
		for (var x = xp1; x <= 7; x++)
		{
			f = board[x][Y];
			if (f == null)
			{
				continue;
			}
			if (f.owner != player)
			{
				break;
			}
			if (f.is_rook() || f.is_queen())
			{
				return true;
			}
			break;
		}
		for (var y = ym1; y >= 0; y--)
		{
			f = board[X][y];
			if (f == null)
			{
				continue;
			}
			if (f.owner != player)
			{
				break;
			}
			if (f.is_rook() || f.is_queen())
			{
				return true;
			}
			break;
		}
		for (var y = yp1; y <= 7; y++)
		{
			f = board[X][y];
			if (f == null)
			{
				continue;
			}
			if (f.owner != player)
			{
				break;
			}
			if (f.is_rook() || f.is_queen())
			{
				return true;
			}
			break;
		}
		
		// Check for bishops and diagonal-line queens:
		for (int x = xm1, y = ym1; x >= 0 & y >= 0; x--, y--)
		{
			f = board[x][y];
			if (f == null)
			{
				continue;
			}
			if (f.owner != player)
			{
				break;
			}
			if (f.is_bishop() || f.is_queen())
			{
				return true;
			}
			break;
		}
		for (int x = xp1, y = ym1; x <= 7 & y >= 0; x++, y--)
		{
			f = board[x][y];
			if (f == null)
			{
				continue;
			}
			if (f.owner != player)
			{
				break;
			}
			if (f.is_bishop() || f.is_queen())
			{
				return true;
			}
			break;
		}
		for (int x = xm1, y = yp1; x >= 0 & y <= 7; x--, y++)
		{
			f = board[x][y];
			if (f == null)
			{
				continue;
			}
			if (f.owner != player)
			{
				break;
			}
			if (f.is_bishop() || f.is_queen())
			{
				return true;
			}
			break;
		}
		for (int x = xp1, y = yp1; x <= 7 & y <= 7; x++, y++)
		{
			f = board[x][y];
			if (f == null)
			{
				continue;
			}
			if (f.owner != player)
			{
				break;
			}
			if (f.is_bishop() || f.is_queen())
			{
				return true;
			}
			break;
		}
		
		// Check for knights:
		final var ym2 = Y - 2;
		final var ym2_valid = ym2 >= 0;
		if (xm1_valid & ym2_valid)
		{
			f = board[xm1][ym2];
			if (f != null && f.owner == player && f.is_knight())
			{
				return true;
			}
		}
		final var ym1_valid = ym1 >= 0;
		final var xm2 = X - 2;
		final var xm2_valid = xm2 >= 0;
		if (xm2_valid & ym1_valid)
		{
			f = board[xm2][ym1];
			if (f != null && f.owner == player && f.is_knight())
			{
				return true;
			}
		}
		if (xp1_valid & ym2_valid)
		{
			f = board[xp1][ym2];
			if (f != null && f.owner == player && f.is_knight())
			{
				return true;
			}
		}
		final var xp2 = X + 2;
		final var xp2_valid = xp2 <= 7;
		if (xp2_valid & ym1_valid)
		{
			f = board[xp2][ym1];
			if (f != null && f.owner == player && f.is_knight())
			{
				return true;
			}
		}
		final var yp2 = Y + 2;
		final var yp2_valid = yp2 <= 7;
		if (xm1_valid & yp2_valid)
		{
			f = board[xm1][yp2];
			if (f != null && f.owner == player && f.is_knight())
			{
				return true;
			}
		}
		final var yp1_valid = yp1 <= 7;
		if (xm2_valid & yp1_valid)
		{
			f = board[xm2][yp1];
			if (f != null && f.owner == player && f.is_knight())
			{
				return true;
			}
		}
		if (xp1_valid & yp2_valid)
		{
			f = board[xp1][yp2];
			if (f != null && f.owner == player && f.is_knight())
			{
				return true;
			}
		}
		if (xp2_valid & yp1_valid)
		{
			f = board[xp2][yp1];
			if (f != null && f.owner == player && f.is_knight())
			{
				return true;
			}
		}
		
		// Check for king:
		if (xm1_valid)
		{
			f = board[xm1][Y];
			if (f != null && f.owner == player && f.is_king())
			{
				return true;
			}
			if (ym1_valid)
			{
				f = board[xm1][ym1];
				if (f != null && f.owner == player && f.is_king())
				{
					return true;
				}
			}
			if (yp1_valid)
			{
				f = board[xm1][yp1];
				if (f != null && f.owner == player && f.is_king())
				{
					return true;
				}
			}
		}
		if (xp1_valid)
		{
			f = board[xp1][Y];
			if (f != null && f.owner == player && f.is_king())
			{
				return true;
			}
			if (ym1_valid)
			{
				f = board[xp1][ym1];
				if (f != null && f.owner == player && f.is_king())
				{
					return true;
				}
			}
			if (yp1_valid)
			{
				f = board[xp1][yp1];
				if (f != null && f.owner == player && f.is_king())
				{
					return true;
				}
			}
		}
		if (ym1_valid)
		{
			f = board[X][ym1];
			if (f != null && f.owner == player && f.is_king())
			{
				return true;
			}
		}
		if (yp1_valid)
		{
			f = board[X][yp1];
			if (f != null && f.owner == player && f.is_king())
			{
				return true;
			}
		}
		
		return false;
	}
}
