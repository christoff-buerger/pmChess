/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.logic;

public final class Search
{
	static final int search_depth = 4;
	static final int max_score =  999999;
	static final int min_score = -999999;
	
	public int select_move(final Board board, final Evaluator evaluator)
	{
		var best_move = 0;
		var alpha = Search.min_score;
		var beta = 2 * Search.max_score;
		for (int i = board.moves_possible(), move = board.moves_possible(i);
			move != 0;
			move = board.moves_possible(++i))
		{
			if (board.execute(move))
			{
				final var score = -alpha_beta_nega_max(
					board,
					-beta,
					-alpha,
					Search.search_depth,
					evaluator);
				if (score > alpha)
				{
					alpha = score;
					best_move = move;
				}
				board.undo();
			}
			// Pruning doesn't make any sense at the root.
		}
		return best_move;
	}
	
	private int alpha_beta_nega_max(
		final Board board,
		int alpha,
		final int beta,
		final int depth,
		final Evaluator evaluator)
	{
		if (depth == 0)
		{
			return evaluator.score(board, board.player());
		}
		var any_move_done = false;
		var result = Search.min_score;
		for (int i = board.moves_possible(), move = board.moves_possible(i);
			move != 0;
			move = board.moves_possible(++i))
		{
			if (board.execute(move))
			{
				result = -alpha_beta_nega_max(
					board,
					-beta,
					-alpha,
					depth - 1,
					evaluator);
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
		var move_index = board.moves_possible();
		var move = board.moves_possible(move_index);
		var last_score = Integer.MIN_VALUE;
		var best_score = last_score;
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
				move_index = board.moves_selected() + 1;
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
				move_index = board.moves_selected() + 1;
				move = board.moves_possible(move_index);
				continue;
			} // Proceed depth-first-search:
			move_index = board.moves_possible();
			move = board.moves_possible(move_index);
		}
	}
}
