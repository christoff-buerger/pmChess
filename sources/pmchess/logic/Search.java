/*
	This program and the accompanying materials are made available under the terms of the MIT
	license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.logic;

public final class Search
{
	private static final int max_score = 999999;
	private static final int min_score = -999999;
	private static final long search_budget = 15000000000l; // 15s
	private static final int search_depth_min = 4;
	
	private Object state_lock =
		new Object(); // Support asynchronous use => protect state.
	private long search_duration =
		search_budget;
	private int search_depth =
		search_depth_min;
	
	public int get_search_depth()
	{
		synchronized (state_lock)
		{
			return search_depth;
		}
	}
	
	public void set_search_depth(final int search_depth)
	{
		synchronized (state_lock)
		{
			this.search_depth = search_depth < search_depth_min
				? search_depth_min
				: search_depth;
			search_duration = search_budget;
		}
	}
	
	public int select_move(final Board board, final Evaluator evaluator)
	{
		final int search_depth;
		synchronized (state_lock)
		{
			search_depth = (0.5 * search_duration * board.moves_possible_count())
				< search_budget
				? this.search_depth + 1
				: this.search_depth;
		}
		
		final var start_time =
			System.nanoTime();
		
		var best_move = 0;
		var alpha =
			Search.min_score;
		final var beta =
			2 * Search.max_score;
		for (int i = board.moves_possible(), move = board.moves_possible(i);
			move != 0;
			move = board.moves_possible(++i))
		{
			if (board.execute(move))
			{
				final var score =
					-alpha_beta_nega_max(
						  board
						, -beta
						, -alpha
						, search_depth
						, evaluator);
				if (score > alpha)
				{
					alpha = score;
					best_move = move;
				}
				board.undo();
			}
			// Pruning doesn't make any sense at the root.
		}
		
		final var end_time =
			System.nanoTime();
		
		synchronized (state_lock)
		{
			search_duration = end_time - start_time;
			if (search_duration > search_budget)
			{
				this.search_depth -= (search_duration / search_budget);
			}
			else
			{
				this.search_depth = search_depth;
			}
			if (best_move != 0
				&& !Move.is_moveless_draw_claim(best_move)
				&& Move.figure_moved(best_move).is_pawn()
				&& !Move.figure_placed(best_move).is_pawn())
			{ // Pawn promotion adds many NEW moves in high search-depth late games:
				this.search_depth--;
			}
			if (this.search_depth < search_depth_min)
			{
				this.search_depth = search_depth_min;
			}
		}
		
		return best_move;
	}
	
	private int alpha_beta_nega_max(
		  final Board board
		, int alpha
		, final int beta
		, final int depth
		, final Evaluator evaluator)
	{
		if (depth == 0)
		{
			return evaluator.score(board, board.player());
		}
		var any_move_done = false;
		var result =
			Search.min_score;
		for (int i = board.moves_possible(), move = board.moves_possible(i);
			move != 0;
			move = board.moves_possible(++i))
		{
			if (board.execute(move))
			{
				result = -alpha_beta_nega_max(
					  board
					, -beta
					, -alpha
					, depth - 1
					, evaluator);
				board.undo();
				any_move_done = true;
			}
			if (result >= beta)
			{
				return beta;
			}
			if (result > alpha)
			{
				alpha = result;
			}
		}
		if (!any_move_done)
		{
			return board.check(board.player())
				? Search.min_score // current player lost
				: evaluator.score(board, board.player()); // stalemate
		}
		return alpha;
	}
	
	private int select_move_2(final Board board)
	{
		var depth = 0;
		var move_index =
			board.moves_possible();
		var move =
			board.moves_possible(move_index);
		var last_score =
			Integer.MIN_VALUE;
		var best_score =
			last_score;
		var best_move = 0;
		while (true)
		{
			if (depth == 0) {
				if (last_score > best_score)
				{
					best_score = last_score;
					best_move = board.moves_selected();
				}
				if (move == 0)
				{
					return best_move;
				}
			}
			if (move == 0) { // Evaluate early leafs and backtrack:
				if (board.moves_selected() == 0)
				{
					// TODO: evaluation
				}
				board.undo();
				--depth;
				move_index = board.moves_selected(
					/*
						Requires method to return index of selected move, not selected move.
					*/) + 1;
				move = board.moves_possible(move_index);
				continue;
			}
			else if (!board.execute(move))
			{ // Skip invalid moves:
				move = board.moves_possible(++move_index);
				continue;
			}
			else if (++depth == 4)
			{ // Evaluate max-search-depth leafs and backtrack:
				// TODO: evaluation
				board.undo();
				--depth;
				move_index = board.moves_selected(
					/*
						Requires method to return index of selected move, not selected move.
					*/) + 1;
				move = board.moves_possible(move_index);
				continue;
			} // Proceed depth-first-search:
			move_index = board.moves_possible();
			move = board.moves_possible(move_index);
		}
	}
}
