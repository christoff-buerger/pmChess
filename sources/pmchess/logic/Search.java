/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.logic;

public final class Search {
	static final int searchDepth = 4;
	static final int maxScore =  999999;
	static final int minScore = -999999;
	
	public int selectMove(final Board board, final Evaluator evaluator) {
		var best_move = 0;
		var alpha = Search.minScore;
		var beta = 2 * Search.maxScore;
		for (int i = board.moves_possible(), move = board.moves_possible(i);
			move != 0;
			move = board.moves_possible(++i))
		{
			if (board.execute(move)) {
				final var score = -alphaBetaNegaMax(
					board,
					-beta,
					-alpha,
					Search.searchDepth,
					evaluator);
				if (score > alpha) {
					alpha = score;
					best_move = move;
				}
				board.undo();
			}
			// Pruning doesn't make any sense at the root.
		}
		return best_move;
	}
	
	private int alphaBetaNegaMax(
		final Board board,
		int alpha,
		final int beta,
		final int depth,
		final Evaluator evaluator)
	{
		if (depth == 0)
			return evaluator.score(board, board.player());
		var anyMoveDone = false;
		var result = Search.minScore;
		for (int i = board.moves_possible(), move = board.moves_possible(i);
			move != 0;
			move = board.moves_possible(++i))
		{
			if (board.execute(move)) {
				result = -alphaBetaNegaMax(
					board,
					-beta,
					-alpha,
					depth - 1,
					evaluator);
				board.undo();
				anyMoveDone = true;
			}
			if (result >= beta)
				return beta;
			if (result > alpha)
				alpha = result;
		}
		if (!anyMoveDone) {
			if (board.status() == Board.GameStatus.Stalemate) {
				return evaluator.score(board, board.player()) <
					evaluator.score(board, !board.player()) ?
					// Never cause remi if better:
					Search.maxScore :
					// Remi if worse maybe useful:
					evaluator.score(board, board.player());
			}
			return Search.minScore; // Current player lost.
		}
		return alpha;
	}
	
	private int selectMove_2(final Board board) {
		var depth = 0;
		var move_index = board.moves_possible();
		var move = board.moves_possible(move_index);
		var last_score = Integer.MIN_VALUE;
		var best_score = last_score;
		var best_move = 0;
		while (true) {
			if (depth == 0) {
				if (last_score > best_score) {
					best_score = last_score;
					best_move = board.moves_selected();
				}
				if (move == 0)
					return best_move;
			}
			if (move == 0) { // Evaluate early leafs and backtrack:
				if (board.moves_selected() == 0) {
					// TODO: evaluation
				}
				board.undo();
				--depth;
				move_index = board.moves_selected() + 1;
				move = board.moves_possible(move_index);
				continue;
			} else if (!board.execute(move)) { // Skip invalid moves:
				move = board.moves_possible(++move_index);
				continue;
			} else if (++depth == 4) { // Evaluate max-search-depth leafs and backtrack:
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
