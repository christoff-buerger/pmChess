/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.logic;

import java.util.Arrays;

public final class Board
{
	public static enum GameStatus {
		  Normal
		, Check
		, Checkmate
		, Stalemate
		, Draw
	}
	public static enum DrawStatus {
		  NoDrawPotential
		, AutomaticRepetition
		, AutomaticMoveRule
		, ClaimedRepetition
		, ClaimedMoveRule
	}
	
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
	
	private static final class PositionCache
	{
		boolean is_cached = false;
		int move_rules_counter = -1;
		int repetition_counter = -1;
		int[] board = {0, 0, 0, 0, 0, 0, 0, 0};
	}
	/*
		History of cached position analyses, starting from the beginning of the game to the
		current position. The first cache is not used to ease indexing by turn numbers
		(which start by 1 for the first turn, not 0).
	*/
	private final PositionCache[] position_caches = new PositionCache[
		  7 *  8 /* max pawn moves */
		+ 8 * 75 /* max other moves */];
	
	/*
		History of moves, starting from the beginning of the game to the current position.
		The moves of each position are described by a move frame consisting of all moves
		possible in that position, the actual move selected for execution and pointers to
		the previous and successor frame. The layout of individual move frames is (lowest
		to highest index within array):
		
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
		| (many array elements)	| frame index. Just the moves as such are stored; their   |
		|                       | 'draw claim' bit is never set.                          |
		+-----------------------+---------------------------------------------------------+
		
		The beginning of the current frame is indexed by the 'moves_frame' field.
	*/
	private int moves_frame = 0;
	private final int[] moves = new int[16384];
	
	/*
		Initialization of 'moves' and 'position_caches':
	*/
	{
		moves[0] = 3;
		moves[1] = -1;
		moves[2] = 0;
		moves_compute_possible();
		
		position_caches[0] = null;
		for (var i = position_caches.length - 1; i > 0; i--)
		{
			position_caches[i] = new PositionCache();
		}
		position_caches[1].is_cached = true;
		position_caches[1].move_rules_counter = 0;
		position_caches[1].repetition_counter = 0;
		position_caches[1].board[0] =
			  Figure.rook(true).key
			| (Figure.pawn(true).key << 4)
			| (Figure.pawn(false).key << 24)
			| (Figure.rook(false).key << 28);
		position_caches[1].board[1] =
			  Figure.knight(true).key
			| (Figure.pawn(true).key << 4)
			| (Figure.pawn(false).key << 24)
			| (Figure.knight(false).key << 28);
		position_caches[1].board[2] =
			  Figure.bishop(true).key
			| (Figure.pawn(true).key << 4)
			| (Figure.pawn(false).key << 24)
			| (Figure.bishop(false).key << 28);
		position_caches[1].board[3] =
			  Figure.queen(true).key
			| (Figure.pawn(true).key << 4)
			| (Figure.pawn(false).key << 24)
			| (Figure.queen(false).key << 28);
		position_caches[1].board[4] =
			  Figure.king(true).key
			| (Figure.pawn(true).key << 4)
			| (Figure.pawn(false).key << 24)
			| (Figure.king(false).key << 28);
		position_caches[1].board[5] =
			  Figure.bishop(true).key
			| (Figure.pawn(true).key << 4)
			| (Figure.pawn(false).key << 24)
			| (Figure.bishop(false).key << 28);
		position_caches[1].board[6] =
			  Figure.knight(true).key
			| (Figure.pawn(true).key << 4)
			| (Figure.pawn(false).key << 24)
			| (Figure.knight(false).key << 28);
		position_caches[1].board[7] =
			  Figure.rook(true).key
			| (Figure.pawn(true).key << 4)
			| (Figure.pawn(false).key << 24)
			| (Figure.rook(false).key << 28);
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
		its possible moves is SUCCESSFULLY executed via the 'execute' function.
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
		final var is_cached_move = moves[moves_frame + 2] == move;
		final var successor_frame = moves[moves_frame];
		if (is_cached_move)
		{
			moves_frame = successor_frame;
		}
		else
		{
			moves[moves_frame + 2] = move;
			position_caches[turn].is_cached = false;
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
		}
		return true;
	}
	
	public int undo()
	{
		if (turn == 1)
		{
			return 0;
		}
		// Restore game history (pop current moves frame):
		moves_frame = moves[moves_frame + 1];
		final var move = moves[moves_frame + 2];
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
	
	private void compute_position_caches()
	{
		final var original_turn = turn;
		
		// Backtrack to first cache hit:
		while (!position_caches[turn].is_cached)
		{
			undo();
		}
		
		// Compute caches forward (reconstructing original position):
		while (turn != original_turn)
		{
			final var previous_turn = turn;
			final var move = moves[moves_frame + 2];
			execute(move);
			
			// Compute move rules:
			position_caches[turn].move_rules_counter = Move.is_moveless_draw_claim(move)
				? position_caches[previous_turn].move_rules_counter
				: (Move.figure_moved(move).is_pawn() || Move.figure_destination(move) != null
					? 0
					: position_caches[previous_turn].move_rules_counter + 1);
			
			// Compute chessboard cache (needed for repetition tests):
			for (var x = 0; x < 8; x++)
			{
				position_caches[turn].board[x] = 0;
				for (var y = 0; y < 8; y++)
				{
					final var f = board[x][y];
					position_caches[turn].board[x] |= (f == null
						? 0
						: f.key << (y * 4));
				}
			}
			
			// Compute repetition:
			final var moves_count = moves[moves_frame] - (moves_frame + 3);
			var repetition_increase = 0;
			for (int t = 1, m = 0;
				t < turn;
				t++, m = moves[m])
			{
				if (!Move.is_moveless_draw_claim(move)
					&& position_caches[turn].move_rules_counter != 0
					&& moves[m] - (m + 3) == moves_count
					&& Arrays.equals(
						  position_caches[t].board
						, position_caches[turn].board))
				{
					repetition_increase++;
					for (var i = moves_count + 2; i > 2; i--)
					{
						if (moves[m + i] != moves[moves_frame + i])
						{
							repetition_increase--;
							break;
						}
					}
					if (repetition_increase > 0)
					{
						break;
					}
				}
			}
			position_caches[turn].repetition_counter =
				position_caches[previous_turn].repetition_counter + repetition_increase;
			
			position_caches[turn].is_cached = true;
		}
	}
	
	/*
		Number of repeating game positions so far (including current position):
	*/
	public int draw_repetition_status()
	{
		compute_position_caches();
		return position_caches[turn].repetition_counter;
	}
	
	/*
		Number of preceding turns without capture or pawn moves (excluding current turn):
	*/
	public int draw_move_rules_status()
	{
		compute_position_caches();
		return position_caches[turn].move_rules_counter;
	}
	
	/*
		Type and reason for draw, IF the position is a draw; it might still be a checkmate.
		Only 'status()' does all checks to conclude if the situation indeed is a draw.
		Hence, the result of this method is only reliable if 'status() == GameStatus.Draw'.
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
