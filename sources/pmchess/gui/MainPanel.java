/*
	This program and the accompanying materials are made available under the terms of the MIT
	license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.gui;

import java.util.concurrent.locks.ReentrantLock;

import java.io.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

import javax.swing.*;

import pmchess.logic.*;

import pmchess.gui.Resources.*;

public final class MainPanel extends JPanel
{
	protected final int text_height =
		(new FontMetrics(Resources.font_regular) {}).getHeight();
	protected final int border_size =
		(int) Math.ceil((2.0f * ((float) text_height)) / 3.0f);
	
	private static final Image bulb =
		Resources.load_image("icons/bulb.png");
	
	/*
		All variables except 'board', 'computer_w' and 'computer_b' are read and written by THE
		thread creating the GUI only => locking only required for these three:
	*/
	private final ReentrantLock board_lock =
		new ReentrantLock(true);
	
	private final Board board =
		new Board(); // Any access must be locked.
	private final Search search =
		new Search();
	private final Evaluator evaluator =
		new Evaluator();
	
	private boolean computer_w = false; // Any access must be locked.
	private boolean computer_b = false; // Any access must be locked.
	private boolean computer_resigned = false;
	/* Turn for which a computer search is in progress, 0 otherwise: */
	private int is_in_search = 0;
	
	private int cursor_x = 0;
	private int cursor_y = 0;
	private int selected_x = 0;
	private int selected_y = 0;
	private Figure selected_figure = null;
	
	private int invalid_internal_move = 0;
	
	private final BoardPanel board_panel =
		new BoardPanel();
	private final GamePanel game_panel =
		new GamePanel();
	private final HistoryPanel history_panel =
		new HistoryPanel();
	
	private final BoardListener board_listener =
		new BoardListener();
	
	protected final int panel_x_size =
		board_panel.panel_size + history_panel.panel_x_size + 2 * border_size;
	protected final int panel_y_size =
		board_panel.panel_size + game_panel.panel_y_size + 2 * border_size;
	
	protected MainPanel()
	{
		// Setup panel size and layout:
		setOpaque(true);
		final var panel_dimension =
			new Dimension(panel_x_size, panel_y_size);
		setMaximumSize(panel_dimension);
		setMinimumSize(panel_dimension);
		setPreferredSize(panel_dimension);
		setBorder(BorderFactory.createEmptyBorder(
			  border_size
			, border_size
			, border_size
			, border_size));
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		// Add components:
		final var dummy_panel =
			new JPanel();
		final var dummy_panel_dimension =
			new Dimension(
				  board_panel.panel_size
				, board_panel.panel_size + game_panel.panel_y_size);
		dummy_panel.setMaximumSize(dummy_panel_dimension);
		dummy_panel.setMinimumSize(dummy_panel_dimension);
		dummy_panel.setPreferredSize(dummy_panel_dimension);
		dummy_panel.setLayout(new BoxLayout(dummy_panel, BoxLayout.Y_AXIS));
		dummy_panel.add(board_panel);
		dummy_panel.add(game_panel);
		add(dummy_panel);
		add(history_panel);
		
		// Add listener for cursor movement and figure selection:
		setFocusable(true);
		requestFocusInWindow();
		addKeyListener(board_listener);
		
		// Initialize and start game:
		initialize(false, false, new InitializationStep[]{});
	}
	
	@Override public void paintComponent(final Graphics graphics)
	{
		super.paintComponent(graphics);
		Resources.configure_rendering(graphics);
	}
	
	protected void serialize_game(final ObjectOutputStream os)
		throws IOException
	{
		board_lock.lock();
		try
		{
			final var turns_to_serialize =
				(is_in_search > 0
					? is_in_search
					: board.turn())
				- 1;
			final var game = new int[2 + 2 * turns_to_serialize];
			game[0] = computer_w ? 1 : 0;
			game[1] = computer_b ? 1 : 0;
			for (var t = turns_to_serialize; t > 0; t--)
			{
				game[2 * t] = board.previous_move(t);
				game[2 * t + 1] = history_panel.history_data.get(t).search_depth;
			}
			os.writeObject(game);
		}
		finally
		{
			board_lock.unlock();
		}
	}
	
	protected void deserialize_game(final ObjectInputStream is)
		throws IOException, ClassNotFoundException
	{
		final var game =
			(int[])(is.readObject());
		final var turns_to_serialize =
			(game.length - 2) / 2;
		final InitializationStep[] initialization_steps =
			new InitializationStep[turns_to_serialize];
		for (int i = 0, j = 2; i < turns_to_serialize; i++)
		{
			initialization_steps[i] = new InitializationStep(game[j++], game[j++]);
		}
		initialize(
			  game[0] != 0
			, game[1] != 0
			, initialization_steps);
	}
	
	protected final class InitializationStep
	{
		private InitializationStep(final int move, final int search_depth)
		{
			this.move = move;
			this.search_depth = search_depth;
		}
		
		private final int move;
		private final int search_depth;
	}
	
	protected void initialize(
		  final boolean computer_w
		, final boolean computer_b
		, final InitializationStep[] initialization_steps)
	{
		if (board_lock.tryLock() /* Only try; ignore reinitialization iff busy. */) try
		{
			if (is_in_search > 0)
			{
				return;
			}
			
			while (board.undo() != 0)
			{
			}
			if (history_panel.history_data.size() > 1)
			{
				history_panel.history_data.removeRange(
					  1
					, history_panel.history_data.size() - 1);
			}
			computer_resigned = false;
			this.computer_w = computer_w;
			this.computer_b = computer_b;
			invalid_internal_move = 0;
			for (final var step : initialization_steps)
			{
				final var last_status =
					board.draw_repetition_status();
				if (!(Move.is_moveless_draw_claim(step.move)
					? board.execute_moveless_draw_claim()
					: board.execute(
						  Move.x(step.move)
						, Move.y(step.move)
						, Move.X(step.move)
						, Move.Y(step.move)
						, Move.figure_placed(step.move)
						, Move.draw_claim(step.move))))
				{
					invalid_internal_move = step.move;
					this.computer_w = false;
					this.computer_b = false;
					break;
				}
				history_panel.history_data.addElement(new PastMove(
					  board.turn() - 1
					, step.move
					, board.status()
					, board.draw_repetition_status() > last_status
					, step.search_depth));
			}
			search.set_search_depth(
				history_panel.history_data.lastElement().search_depth);
			
			run_game(); // 'run_game()' takes care of repainting.
		}
		finally
		{
			board_lock.unlock();
		}
	}
	
	private void run_game()
	{
		board_lock.lock();
		try
		{
			final var game_status =
				board.status();
			final var computer_move =
				board.player() ? computer_w : computer_b;
			final var computer_continues =
				computer_move
				&& !computer_resigned /* Once resigned stay resigned in THAT position. */
				&& (game_status == Board.GameStatus.Normal
					|| game_status == Board.GameStatus.Check);
			
			// Reset GUI:
			selected_figure = null;
			game_panel.status_panel.pawn_promotion_list.setSelectedIndex(0);
			game_panel.status_panel.pawn_promotion_list.setEnabled(!computer_move);
			game_panel.status_panel.draw_claim_button.setSelected(false);
			game_panel.status_panel.draw_claim_button.setEnabled(!computer_move);
			history_panel.history_list.setSelectedIndex(board.turn() - 1);
			history_panel.history_list.setEnabled(false);
			history_panel.history_undo_button.setEnabled(false);
			history_panel.history_pause_button.setSelected(false);
			history_panel.history_pause_button.setEnabled(!computer_move || !computer_continues);
			history_panel.history_redo_button.setEnabled(false);
			requestFocusInWindow();
			
			// Execute computer move in asynchronous co-routine:
			if (computer_continues && is_in_search == 0)
			{
				is_in_search = board.turn();
				final var forked_board =
					board.fork();
				final var search_coroutine =
					new Thread()
					{
						@Override public void run()
						{
							final var move =
								search.select_move(forked_board, evaluator);
							java.awt.EventQueue.invokeLater(
								new Runnable()
								{
									@Override public void run()
									{
										board_lock.lock();
										try
										{
											computer_resigned = move == 0;
											if (computer_resigned)
											{
												return;
											}
											final var last_status =
												board.draw_repetition_status();
											if (!(Move.is_moveless_draw_claim(move)
												? board.execute_moveless_draw_claim()
												: board.execute(
													  Move.x(move)
													, Move.y(move)
													, Move.X(move)
													, Move.Y(move)
													, Move.figure_placed(move)
													, Move.draw_claim(move))))
											{
												invalid_internal_move = move;
												computer_w = false;
												computer_b = false;
												return;
											}
											history_panel.history_data.addElement(new PastMove(
												  board.turn() - 1
												, move
												, board.status()
												, board.draw_repetition_status() > last_status
												, search.get_search_depth()));
											// Reset all GUI selections influenced by computer move:
											history_panel.history_list.setSelectedIndex(
												board.turn() - 1);
										}
										finally
										{
											is_in_search = 0;
											board_lock.unlock();
											run_game();
										}
									}
								});
						}
					};
				search_coroutine.start();
			}
			
			// Update GUI:
			board_panel.repaint();
			game_panel.repaint();
			history_panel.repaint();
		}
		finally
		{
			board_lock.unlock();
		}
	}
	
	private final class BoardListener extends KeyAdapter
	{
		@Override public void keyPressed(final KeyEvent event)
		{
			if (board_lock.tryLock() /* Only try; ignore user-selection iff busy. */) try
			{
				if (history_panel.history_pause_button.isSelected()
					|| is_in_search > 0
					|| (board.player() ? computer_w : computer_b))
				{
					return;
				}
				
				final var old_x = cursor_x;
				final var old_y = cursor_y;
				final var key = event.getKeyCode();
				
				if (key == KeyEvent.VK_SPACE)
				{
					final var figure =
						board.figure(cursor_x, cursor_y);
					if (figure != null && figure.owner == board.player())
					{
						selected_figure = figure;
						selected_x = cursor_x;
						selected_y = cursor_y;
					}
					else if (selected_figure != null)
					{
						final var figure_placed =
							(selected_figure.is_pawn() && (cursor_y == 0 || cursor_y == 7))
								? game_panel.status_panel.pawn_promotion_list
									.getSelectedValue().figure
								: selected_figure;
						final var last_status =
							board.draw_repetition_status();
						if (board.execute(
							  selected_x
							, selected_y
							, cursor_x
							, cursor_y
							, figure_placed
							, game_panel.status_panel.draw_claim_button.isSelected()))
						{
							history_panel.history_data.addElement(new PastMove(
								  board.turn() - 1
								, board.previous_move(board.turn() - 1)
								, board.status()
								, board.draw_repetition_status() > last_status
								, search.get_search_depth()));
							run_game();
							return; // 'run_game()' takes care of repainting.
						}
					}
				}
				else if (key == KeyEvent.VK_UP && cursor_y < 7)
				{
					cursor_y++;
				}
				else if (key == KeyEvent.VK_DOWN && cursor_y > 0)
				{
					cursor_y--;
				}
				else if (key == KeyEvent.VK_LEFT && cursor_x > 0)
				{
					cursor_x--;
				}
				else if (key == KeyEvent.VK_RIGHT && cursor_x < 7)
				{
					cursor_x++;
				}
				else
				{ // Unknown key or invalid cursor movement:
					return;
				}
				
				// Update GUI:
				board_panel.repaint();
			}
			finally
			{
				board_lock.unlock();
			}
		}
	}
	
	private final class HistoryListener extends KeyAdapter
	{
		@Override public void keyPressed(final KeyEvent event)
		{
			if (event.getKeyCode() != KeyEvent.VK_SPACE)
			{
				return;
			}
			if (board_lock.tryLock() /* Only try; ignore undo iff busy. */) try
			{
				final var selected =
					history_panel.history_list.getSelectedIndex();
				if (selected == board.turn() - 1)
				{
					return;
				}
				if (selected < board.turn() - 1)
				{
					for (var i = board.turn() - selected - 1; i > 0; i--)
					{
						board.undo();
					}
				}
				else // selected > board.turn() - 1:
				{
					for (var i = board.turn(); i <= selected; i++)
					{
						final var move =
							history_panel.history_data.get(i).move;
						if (!(Move.is_moveless_draw_claim(move)
							? board.execute_moveless_draw_claim()
							: board.execute(
								  Move.x(move)
								, Move.y(move)
								, Move.X(move)
								, Move.Y(move)
								, Move.figure_placed(move)
								, Move.draw_claim(move))))
						{
							invalid_internal_move = move;
							computer_w = false;
							computer_b = false;
							break;
						}
					}
				}
				search.set_search_depth(
					history_panel.history_data.get(selected).search_depth);
				computer_resigned = false;
				// Maintain enabled status of undo and redo buttons:
				history_panel.history_undo_button.setEnabled(
					board.turn() > 1);
				history_panel.history_redo_button.setEnabled(
					board.turn() < history_panel.history_data.size());
				// Update GUI:
				board_panel.repaint();
				game_panel.repaint();
				history_panel.repaint();
			}
			finally
			{
				board_lock.unlock();
			}
		}
	}
	
	private final class BoardPanel extends JPanel
	{
		private final int tile_size =
			(int)Math.ceil(2.5f * text_height);
		private final int border_size =
			(int)Math.ceil(tile_size + tile_size / 4.0f);
		private final Font chessboard_marking_font =
			Resources.font_bold.deriveFont(border_size / 3.0f);
		private final int cursor_line_width =
			(int)Math.ceil(tile_size / 10.0f) % 2 == 0
				? (int)Math.ceil(tile_size / 10.0f)
				: (int)Math.ceil(tile_size / 10.0f) + 1;
		private final int panel_size =
			8 * tile_size + 2 * border_size;
		private final Font figure_font =
			FigurePresentation.font.deriveFont(tile_size - 2.0f * cursor_line_width);
		private final Font figure_captured_font =
			FigurePresentation.font.deriveFont(0.5f * tile_size);
		
		private BoardPanel()
		{
			// Setup panel size and layout:
			setOpaque(true);
			final var panel_dimension =
				new Dimension(panel_size, panel_size);
			setMaximumSize(panel_dimension);
			setMinimumSize(panel_dimension);
			setPreferredSize(panel_dimension);
			setBorder(BorderFactory.createCompoundBorder(
				  BorderFactory.createTitledBorder("Chessboard")
				, BorderFactory.createEmptyBorder(
					  border_size
					, border_size
					, border_size
					, border_size)));
			
			addMouseListener(
				new MouseListener()
				{
					private void dispatch_key_event(final int key)
					{
						board_listener.keyPressed(new KeyEvent(
							  MainPanel.this
							, KeyEvent.KEY_PRESSED
							, System.currentTimeMillis()
							, 0 //KeyEvent.SHIFT_DOWN_MASK
							, key
							, KeyEvent.CHAR_UNDEFINED));
						board_listener.keyReleased(new KeyEvent(
							  MainPanel.this
							, KeyEvent.KEY_RELEASED
							, System.currentTimeMillis()
							, 0 //KeyEvent.SHIFT_DOWN_MASK
							, key
							, KeyEvent.CHAR_UNDEFINED));
					}
					
					@Override public void mouseClicked(final MouseEvent e)
					{
						// Compute selected tile:
						final var x =
							(e.getX() > border_size && e.getX() < 8 * tile_size + border_size)
								? (e.getX() - border_size) / tile_size
								: -1;
						final var y =
							(e.getY() > border_size && e.getY() < 8 * tile_size + border_size)
								? Math.abs(((e.getY() - border_size) / tile_size) - 7)
								: -1;
						if (x == -1 | y == -1)
						{
							return;
						}
						
						// Check, that the selection is unmistakable
						// (i.e., not to close to the tile border):
						final var x_tile_start =
							border_size + x * tile_size;
						final var y_tile_start =
							border_size + 7 * tile_size - y * tile_size;
						final var margine =
							(int)Math.floor(1.1f * cursor_line_width);
						if (e.getX() <= x_tile_start + margine
							| e.getX() >= x_tile_start + tile_size - margine
							| e.getY() <= y_tile_start + margine
							| e.getY() >= y_tile_start + tile_size - margine)
						{
							return;
						}
						
						// Simulate cursor movement:
						final var x_movement =
							x > cursor_x ? KeyEvent.VK_RIGHT : KeyEvent.VK_LEFT;
						final var y_movement =
							y > cursor_y ? KeyEvent.VK_UP : KeyEvent.VK_DOWN;
						for (int i = Math.abs(cursor_x - x); i != 0; --i)
						{
							dispatch_key_event(x_movement);
						}
						for (int i = Math.abs(cursor_y - y); i != 0; --i)
						{
							dispatch_key_event(y_movement);
						}
						dispatch_key_event(KeyEvent.VK_SPACE);
					}
					
					@Override public void mouseEntered(final MouseEvent e)
					{
					}
					
					@Override public void mouseExited(final MouseEvent e)
					{
					}
					
					@Override public void mousePressed(final MouseEvent e)
					{
					}
					
					@Override public void mouseReleased(final MouseEvent e)
					{
					}
				});
		}
		
		@Override public void paintComponent(final Graphics graphic)
		{
			super.paintComponent(graphic);
			Resources.configure_rendering(graphic);
			
			// Draw horizontal (h) and vertical (v) chessboard markings:
			graphic.setFont(chessboard_marking_font);
			final var font_metrics =
				graphic.getFontMetrics();
			final var font_height =
				font_metrics.getAscent();
			final var h_y_base =
				border_size
				+ (tile_size - font_height) / 2
				+ font_height;
			final var v_y_base =
				(border_size - font_height) / 2
				+ font_height
				+ /* Adjust for lowercase and titled border: */ font_height / 3;
			for (var i = 0; i < 8; i++)
			{
				final var h_marking =
					String.valueOf((char)('8' - i));
				final var h_width =
					font_metrics.stringWidth(h_marking);
				final var h_x_base =
					(border_size - h_width) / 2;
				final var h_y =
					h_y_base + i * tile_size;
				graphic.drawString(h_marking, h_x_base, h_y);
				graphic.drawString(
					  h_marking
					, panel_size - h_x_base - h_width
					, h_y);
				final var v_marking =
					String.valueOf((char)('a' + i));
				final var v_width =
					font_metrics.stringWidth(v_marking);
				final var v_x =
					border_size
					+ i * tile_size
					+ (tile_size - v_width) / 2;
				graphic.drawString(v_marking, v_x, v_y_base);
				graphic.drawString(
					  v_marking
					, v_x
					, panel_size - v_y_base + font_height);
			}
			
			// Draw tiles:
			draw_tiles(graphic);
		}
		
		private void draw_tiles(final Graphics graphic)
		{
			board_lock.lock();
			try
			{
				Resources.configure_rendering(graphic);
				
				final var last_move =
					board.previous_move(board.turn() - 1);
				for (var x = 7; x >= 0; x--) for (var y = 7; y >= 0; y--)
				{
					final var y_trans = 7 - y;
					Figure draw_captured_figure = null;
					
					// Draw background tile:
					var color =
						((x + y_trans) % 2) == 0
							? Color.white
							: Color.lightGray;
					if (last_move != 0 && !Move.is_moveless_draw_claim(last_move))
					{
						final var _x_ =
							Move.x(last_move);
						final var _X_ =
							Move.X(last_move);
						final var _y_ =
							Move.y(last_move);
						final var _Y_ =
							Move.Y(last_move);
						final var _figure_moved_ =
							Move.figure_moved(last_move);
						final var _figure_destination_ =
							Move.figure_destination(last_move);
						final var _en_passant_ =
							_figure_moved_.is_pawn()
							&& _X_ != _x_
							&& _figure_destination_ == null;
						
						if (_en_passant_ && x == _X_ && y == _y_)
						{ // Colour tile of en passant captured pawn:
							draw_captured_figure = Figure.pawn(board.player());
							color = new Color(255, 102, 102);
						}
						else if ((x == _x_ && y == _y_)
							|| (y == _Y_
								&& (x == _X_
									// Colour tile of rook involved in castling:
									|| (_figure_moved_.is_king()
										&& ((_X_ == _x_ - 2 && (x == 0 || x == 3))
											|| (_X_ == _x_ + 2 && (x == 7 || x == 5)))))))
						{
							if (x == _X_ && y == _Y_)
							{
								draw_captured_figure = _figure_destination_;
							}
							color = _figure_destination_ != null || _en_passant_
								? new Color(214, 13, 13)
								: new Color(77, 164, 77);
						}
					}
					graphic.setColor(color);
					graphic.fillRect(
						  x * tile_size + border_size
						, y_trans * tile_size + border_size
						, tile_size
						, tile_size);
					
					// Draw captured figure:
					if (draw_captured_figure != null)
					{
						graphic.setColor(Color.black);
						graphic.setFont(figure_captured_font);
						graphic.drawString(
							  FigurePresentation.get(draw_captured_figure).unicode
							, x * tile_size + border_size + (int)(0.65f * tile_size)
							, y_trans * tile_size + border_size + (int)(0.25f * tile_size));
					}
					
					// Draw figure:
					final var figure =
						board.figure(x, y);
					if (figure != null)
					{
						graphic.setColor(Color.black);
						graphic.setFont(figure_font);
						final var text =
							FigurePresentation.get(figure).unicode;
						final var metrics =
							graphic.getFontMetrics();
						final var width_fix =
							(tile_size - metrics.stringWidth(text)) / 2;
						final var height_fix =
							((tile_size + metrics.getAscent() + metrics.getDescent()) / 2)
							- metrics.getDescent();
						graphic.drawString(
							  text
							, x * tile_size + border_size + width_fix
							, y_trans * tile_size + border_size + height_fix);
					}
					
					// Draw cursor and figure selection:
					final var old_stroke =
						((Graphics2D)graphic).getStroke();
					((Graphics2D)graphic).setStroke(new BasicStroke(cursor_line_width));
					final var x_pos =
						x * tile_size
						+ border_size
						+ (cursor_line_width / 2)
						+ 1;
					final var y_pos =
						y_trans * tile_size
						+ border_size
						+ (cursor_line_width / 2)
						+ 1;
					final var distance =
						tile_size - cursor_line_width - 2;
					final var interaction_disabled =
						history_panel.history_pause_button.isSelected()
						|| is_in_search > 0
						|| (board.player() ? computer_w : computer_b);
					if (x == cursor_x && y == cursor_y)
					{
						graphic.setColor(interaction_disabled
							? Color.gray
							: Color.blue);
						graphic.drawRect(x_pos, y_pos, distance, distance);
					}
					if (!interaction_disabled
						&& selected_figure != null
						&& x == selected_x
						&& y == selected_y)
					{
						graphic.setColor(Color.red);
						graphic.drawRect(x_pos, y_pos, distance, distance);
					}
					((Graphics2D)graphic).setStroke(old_stroke);
				}
			}
			finally
			{
				board_lock.unlock();
			}
		}
	}
	
	private final class GamePanel extends JPanel
	{
		private final int panel_x_size =
			board_panel.panel_size;
		private final int tabs_x_size =
			panel_x_size;
		private final int tab_x_size =
			tabs_x_size - 2 * border_size;
		private final Font pawn_promotion_font =
			FigurePresentation.font.deriveFont(1.3f * FigurePresentation.font.getSize2D());
		private final int pawn_promotion_text_heigth =
			(new FontMetrics(pawn_promotion_font) {}).getHeight();
		private final int pawn_promotion_list_x_size =
			(int)Math.ceil(1.3f * pawn_promotion_text_heigth);
		private final int pawn_promotion_label_x_size =
			pawn_promotion_text_heigth;
		private final int pawn_promotion_x_size =
			pawn_promotion_list_x_size + pawn_promotion_label_x_size;
		private final int castlings_x_size =
			tab_x_size - pawn_promotion_x_size;
		private final int castling_x_size =
			(int)Math.floor(0.5f * castlings_x_size);
		private final int status_x_size =
			castlings_x_size;
		
		private final int status_y_size =
			text_height + 2 * border_size;
		private final int castling_y_size =
			(int)(text_height + pawn_promotion_text_heigth + 3.5f * border_size);
		private final int castlings_y_size =
			castling_y_size;
		private final int pawn_promotion_y_size =
			4 * pawn_promotion_text_heigth + (int)(1.5f * border_size);
		private final int pawn_promotion_list_y_size =
			pawn_promotion_y_size;
		private final int top_y_size =
			Math.max(status_y_size + castling_y_size, pawn_promotion_y_size);
		private final int draw_y_size =
			(int)(3 * text_height + 3.5f * border_size);
		private final int tab_y_size =
			top_y_size + draw_y_size + 2 * border_size;
		private final int tabs_y_size =
			tab_y_size + text_height + border_size;
		private final int panel_y_size =
			tabs_y_size + (int)Math.ceil(0.5f * border_size);
		
		private final StatusPanel status_panel =
			new StatusPanel();
		private final SettingsPanel settings_panel =
			new SettingsPanel();
		
		private GamePanel()
		{
			// Setup panel size and layout:
			setOpaque(true);
			final var panel_dimension =
				new Dimension(panel_x_size, panel_y_size);
			setMaximumSize(panel_dimension);
			setMinimumSize(panel_dimension);
			setPreferredSize(panel_dimension);
			
			// Compose tabs:
			final var tabs =
				new JTabbedPane();
			final var tabs_dimension =
				new Dimension(tabs_x_size, tabs_y_size);
			tabs.setMaximumSize(tabs_dimension);
			tabs.setMinimumSize(tabs_dimension);
			tabs.setPreferredSize(tabs_dimension);
			tabs.addTab("Game status", status_panel);
			tabs.addTab("Settings", settings_panel);
			tabs.setSelectedIndex(0);
			add(tabs);
		}
	
		private abstract class GamePanelTab extends JPanel
		{
			private GamePanelTab()
			{
				setOpaque(true);
				final var tab_dimension =
					new Dimension(tab_x_size, tab_y_size);
				setMaximumSize(tab_dimension);
				setMinimumSize(tab_dimension);
				setPreferredSize(tab_dimension);
			}
		}
		
		private final class StatusPanel extends GamePanelTab
		{
			private final JLabel status =
				new JLabel()
				{
					@Override public void paintComponent(final Graphics graphic)
					{
						super.paintComponent(graphic);
						Resources.configure_rendering(graphic);
						
						final var bulb_x =
							status_x_size - bulb.getWidth(StatusPanel.this);
						final var bulb_y =
							(status_y_size - bulb.getHeight(StatusPanel.this)) / 2;
						if (is_in_search > 0 /* safe asynchronous access */)
						{
							graphic.drawImage(bulb, bulb_x, bulb_y, StatusPanel.this);
						}
					}
				};
			
			private final JCheckBox castling_qs_w =
				new JCheckBox(FigurePresentation.get(Figure.queen(true)).unicode); // queenside
			private final JCheckBox castling_ks_w =
				new JCheckBox(FigurePresentation.get(Figure.king(true)).unicode); // kingside
			private final JCheckBox castling_qs_b =
				new JCheckBox(FigurePresentation.get(Figure.queen(false)).unicode); // queenside
			private final JCheckBox castling_ks_b =
				new JCheckBox(FigurePresentation.get(Figure.king(false)).unicode); // kingside
			
			private final DefaultListModel<FigurePresentation> pawn_promotion_w =
				new DefaultListModel<>();
			private final DefaultListModel<FigurePresentation> pawn_promotion_b =
				new DefaultListModel<>();
			private final JList<FigurePresentation> pawn_promotion_list =
				new JList<>();
			
			private final JToggleButton draw_claim_button =
				new JToggleButton("Claim draw", false)
				{
					// Static initialize with listener checking for moveless draw claim:
					{
						addItemListener(
							(final ItemEvent e) ->
							{
								if (e.getStateChange() != ItemEvent.SELECTED)
								{
									return;
								}
								if (board_lock.tryLock() /* Only try; ignore claim iff busy. */) try
								{
									if (is_in_search > 0
										|| (board.player() ? computer_w : computer_b))
									{
										setSelected(false);
										return;
									}
									if (board.execute_moveless_draw_claim())
									{
										history_panel.history_data.addElement(new PastMove(
											  board.turn() - 1
											, board.previous_move(board.turn() - 1)
											, board.status()
											, false
											, search.get_search_depth()));
										run_game(); // 'run_game()' takes care of repainting.
									}
									return;
								}
								finally
								{
									board_lock.unlock();
								}
								setSelected(false);
							});
					}
				};
			private final JLabel draw_repetition_status =
				new JLabel(String.valueOf(0), SwingConstants.CENTER)
				{
					@Override public void setText(final String text)
					{
						super.setText("<html><center>"
					  		+ "<span style=\"font-family:"
					  		+ Resources.font_bold.getFontName()
					  		+ ";\">Repetition</span>"
					  		+ "<br>"
					  		+ "<span style=\"font-family:"
					  		+ Resources.font_regular.getFontName()
					  		+ ";\">"
							+ text
							+ "</span>"
					  		+ "<span style=\"font-family:"
					  		+ Resources.font_italic.getFontName()
					  		+ ";\"> of 3/5</span></center></html>");
					}
				};
			private final JLabel draw_move_rules_status =
				new JLabel(String.valueOf(0), SwingConstants.CENTER)
				{
					@Override public void setText(final String text)
					{
						super.setText("<html><center>"
							+ "<span style=\"font-family:"
							+ Resources.font_bold.getFontName()
							+ ";\">Move rules</span>"
							+ "<br>"
							+ "<span style=\"font-family:"
							+ Resources.font_regular.getFontName()
							+ ";\">"
							+ text
							+ "</span>"
							+ "<span style=\"font-family:"
							+ Resources.font_italic.getFontName()
							+ ";\"> of 50/75</span></center></html>");
					}
				};
			
			private StatusPanel()
			{
				super();
				
				// Status message:
				
				final var status_dimension =
					new Dimension(status_x_size, status_y_size);
				status.setOpaque(true);
				status.setFont(Resources.font_bold);
				status.setMaximumSize(status_dimension);
				status.setMinimumSize(status_dimension);
				status.setPreferredSize(status_dimension);
				status.setAlignmentX(Component.CENTER_ALIGNMENT);
				
				// Allowed castlings information:
				
				final var castling_dimension =
					new Dimension(castling_x_size, castling_y_size);
				castling_qs_w.setFont(pawn_promotion_font);
				castling_ks_w.setFont(pawn_promotion_font);
				castling_qs_b.setFont(pawn_promotion_font);
				castling_ks_b.setFont(pawn_promotion_font);
				castling_qs_w.setEnabled(false);
				castling_ks_w.setEnabled(false);
				castling_qs_b.setEnabled(false);
				castling_ks_b.setEnabled(false);
				
				final var castling_w =
					new JPanel();
				castling_w.setBorder(BorderFactory.createTitledBorder("White castling"));
				castling_w.setMaximumSize(castling_dimension);
				castling_w.setMinimumSize(castling_dimension);
				castling_w.setPreferredSize(castling_dimension);
				castling_w.add(castling_qs_w);
				castling_w.add(castling_ks_w);
				
				final var castling_b =
					new JPanel();
				castling_b.setBorder(BorderFactory.createTitledBorder("Black castling"));
				castling_b.setMaximumSize(castling_dimension);
				castling_b.setMinimumSize(castling_dimension);
				castling_b.setPreferredSize(castling_dimension);
				castling_b.add(castling_qs_b);
				castling_b.add(castling_ks_b);
				
				// Pawn promotion selection:
				
				pawn_promotion_w.addElement(FigurePresentation.get(Figure.queen(true)));
				pawn_promotion_w.addElement(FigurePresentation.get(Figure.knight(true)));
				pawn_promotion_w.addElement(FigurePresentation.get(Figure.bishop(true)));
				pawn_promotion_w.addElement(FigurePresentation.get(Figure.rook(true)));
				pawn_promotion_b.addElement(FigurePresentation.get(Figure.queen(false)));
				pawn_promotion_b.addElement(FigurePresentation.get(Figure.knight(false)));
				pawn_promotion_b.addElement(FigurePresentation.get(Figure.bishop(false)));
				pawn_promotion_b.addElement(FigurePresentation.get(Figure.rook(false)));
				
				final var pawn_promotion_list_dimension =
					new Dimension(pawn_promotion_list_x_size, pawn_promotion_list_y_size);
				pawn_promotion_list.setFont(pawn_promotion_font);
				pawn_promotion_list.setBorder(BorderFactory.createLoweredBevelBorder());
				pawn_promotion_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				pawn_promotion_list.setLayoutOrientation(JList.VERTICAL);
				pawn_promotion_list.setVisibleRowCount(pawn_promotion_w.getSize());
				pawn_promotion_list.setCellRenderer(new DefaultListCellRenderer());
				pawn_promotion_list.setModel(board.player() /* safe asynchronous access */
					? pawn_promotion_w
					: pawn_promotion_b);
				pawn_promotion_list.setSelectedIndex(0);
				pawn_promotion_list.setMaximumSize(pawn_promotion_list_dimension);
				pawn_promotion_list.setMinimumSize(pawn_promotion_list_dimension);
				pawn_promotion_list.setPreferredSize(pawn_promotion_list_dimension);
				
				final var pawn_promotion_label =
					new JLabel("Promotion")
					{
						private boolean painting = false;
						
						@Override public void paintComponent(final Graphics graphics)
						{
							Resources.configure_rendering(graphics);
							final var g2d = (Graphics2D) graphics;
							g2d.rotate(Math.toRadians(-90));
							g2d.translate(-getHeight(), 0);
							painting = true;
							super.paintComponent(g2d);
							painting = false;
							g2d.rotate(-Math.toRadians(-90));
							g2d.translate(0, -getHeight());
						}
						
						@Override public Insets getInsets()
						{
							final var i =
								super.getInsets();
							return painting
								? new Insets(i.right, i.top, i.left, i.bottom)
								: i;
						}
						
						@Override public Insets getInsets(final Insets insets)
						{
							return getInsets();
						}
						
						@Override public int getWidth()
						{
							return painting ? super.getHeight() : super.getWidth();
						}
						
						@Override public int getHeight()
						{
							return painting ? super.getWidth() : super.getHeight();
						}
						
						@Override public Dimension getPreferredSize()
						{
							final var size =
								super.getPreferredSize();
							return new Dimension(size.height, size.width);
						}
						
						@Override public Dimension getMinimumSize()
						{
							return getPreferredSize();
						}
						
						@Override public Dimension getMaximumSize()
						{
							return getPreferredSize();
						}
					};
				
				// Draw status:
				
				final var draw_panel =
					new JPanel();
				final var draw_panel_dimension =
					new Dimension(tab_x_size, draw_y_size);
				draw_panel.setBorder(BorderFactory.createTitledBorder("Draw status"));
				draw_panel.setMaximumSize(draw_panel_dimension);
				draw_panel.setMinimumSize(draw_panel_dimension);
				draw_panel.setPreferredSize(draw_panel_dimension);				
				draw_panel.setLayout(new BoxLayout(draw_panel, BoxLayout.X_AXIS));
				
				final var draw_status_dimension =
					new Dimension(
						  (int)(0.28f * draw_panel_dimension.getWidth())
						, text_height + 2 * border_size);
				draw_repetition_status.setMaximumSize(draw_status_dimension);
				draw_repetition_status.setMinimumSize(draw_status_dimension);
				draw_repetition_status.setPreferredSize(draw_status_dimension);
				
				final var draw_claim_button_dimension =
					new Dimension(
						  (int)(0.40f * draw_panel_dimension.getWidth())
						, 2 * text_height + 2 * border_size);
				draw_claim_button.setMaximumSize(draw_claim_button_dimension);
				draw_claim_button.setMinimumSize(draw_claim_button_dimension);
				draw_claim_button.setPreferredSize(draw_claim_button_dimension);
				
				draw_move_rules_status.setMaximumSize(draw_status_dimension);
				draw_move_rules_status.setMinimumSize(draw_status_dimension);
				draw_move_rules_status.setPreferredSize(draw_status_dimension);
				
				draw_panel.add(Box.createHorizontalGlue());
				draw_panel.add(draw_repetition_status);
				draw_panel.add(Box.createHorizontalGlue());
				draw_panel.add(draw_claim_button);
				draw_panel.add(Box.createHorizontalGlue());
				draw_panel.add(draw_move_rules_status);
				draw_panel.add(Box.createHorizontalGlue());

				// Compose everything (status message, allowed castlings information,
				// pawn promotion selection and draw status):
				
				final var castlings_panel =
					new JPanel();
				final var castlings_panel_dimension =
					new Dimension(castlings_x_size, castlings_y_size);
				castlings_panel.setMaximumSize(castlings_panel_dimension);
				castlings_panel.setMinimumSize(castlings_panel_dimension);
				castlings_panel.setPreferredSize(castlings_panel_dimension);
				castlings_panel.setLayout(new BoxLayout(castlings_panel, BoxLayout.X_AXIS));
				castlings_panel.add(Box.createHorizontalGlue());
				castlings_panel.add(castling_w);
				castlings_panel.add(Box.createHorizontalGlue());
				castlings_panel.add(castling_b);
				castlings_panel.add(Box.createHorizontalGlue());
				
				final var left_panel =
					new JPanel();
				final var left_panel_dimension =
					new Dimension(castlings_x_size, top_y_size);
				left_panel.setMaximumSize(left_panel_dimension);
				left_panel.setMinimumSize(left_panel_dimension);
				left_panel.setPreferredSize(left_panel_dimension);
				left_panel.setLayout(new BoxLayout(left_panel, BoxLayout.Y_AXIS));
				left_panel.add(Box.createVerticalGlue());
				left_panel.add(status);
				left_panel.add(Box.createVerticalGlue());
				left_panel.add(castlings_panel);
				left_panel.add(Box.createVerticalGlue());
				
				final var right_panel =
					new JPanel();
				final var right_panel_dimension =
					new Dimension(pawn_promotion_x_size, top_y_size);
				right_panel.setMaximumSize(right_panel_dimension);
				right_panel.setMinimumSize(right_panel_dimension);
				right_panel.setPreferredSize(right_panel_dimension);
				right_panel.setLayout(new BoxLayout(right_panel, BoxLayout.X_AXIS));
				right_panel.add(Box.createHorizontalGlue());
				right_panel.add(pawn_promotion_list);
				right_panel.add(pawn_promotion_label);
				right_panel.add(Box.createHorizontalGlue());
				
				final var top_panel =
					new JPanel();
				final var top_panel_dimension =
					new Dimension(tab_x_size, top_y_size);
				top_panel.setMaximumSize(top_panel_dimension);
				top_panel.setMinimumSize(top_panel_dimension);
				top_panel.setPreferredSize(top_panel_dimension);
				top_panel.setLayout(new BoxLayout(top_panel, BoxLayout.X_AXIS));
				top_panel.add(Box.createHorizontalGlue());
				top_panel.add(left_panel);
				top_panel.add(Box.createHorizontalGlue());
				top_panel.add(right_panel);
				top_panel.add(Box.createHorizontalGlue());
				
				setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
				add(Box.createVerticalGlue());
				add(top_panel);
				add(Box.createVerticalGlue());
				add(draw_panel);
				add(Box.createVerticalGlue());
			}
			
			@Override public void paintComponent(final Graphics graphic)
			{
				board_lock.lock();
				try
				{
					super.paintComponent(graphic);
					Resources.configure_rendering(graphic);
					
					// Update status message:
					final var now =
						board.player() ? "White" : "Black";
					final var next =
						board.player() ? "Black" : "White";
					final String message;
					if (invalid_internal_move != 0)
					{
						message = "!INTERNAL ERROR ("
							+ String.valueOf(invalid_internal_move)
							+ ")!";
					}
					else if (computer_resigned)
					{
						message = now + " resigns. " + next + " wins.";
					}
					else
					{
						switch (board.status())
						{
						case Check:
							message = now + " in check. " + now + "'s turn.";
							break;
						case Checkmate:
							message = now + " checkmate. " + next + " wins.";
							break;
						case Stalemate:
							message = "Draw (stalemate). " + now + " cannot move.";
							break;
						case Draw:
							switch (board.draw_status())
							{
							case AutomaticMoveRule:
								message = "Draw (automatic). 75-move rule.";
								break;
							case AutomaticRepetition:
								message = "Draw (automatic). Repetition.";
								break;
							case ClaimedMoveRule:
								message = "Draw (" + next + " claim). 50-move rule.";
								break;
							default: // 'ClaimedRepetition' since 'status()' is 'Draw'.
								message = "Draw (" + next + " claim). Repetition.";
								break;
							}
							break;
						default: // 'Normal'
							message = now + "'s turn.";
						}
					}
					status.setBackground(board.player() ? Color.white : Color.black);
					status.setForeground(board.player() ? Color.black : Color.white);
					status.setText("  " + String.valueOf(board.move()) + ": " + message);
					
					// Update allowed castlings:
					castling_qs_w.setSelected(board.castling_allowed(true, true));
					castling_ks_w.setSelected(board.castling_allowed(false, true));
					castling_qs_b.setSelected(board.castling_allowed(true, false));
					castling_ks_b.setSelected(board.castling_allowed(false, false));
					
					// Update pawn promotion selector list:
					final var current_selection =
						pawn_promotion_list.getSelectedIndex();
					pawn_promotion_list.setModel(board.player()
						? pawn_promotion_w
						: pawn_promotion_b);
					pawn_promotion_list.setSelectedIndex(current_selection);
					
					// Update draw status:
					draw_repetition_status.setText(String.valueOf(
						board.draw_repetition_status()));
					draw_move_rules_status.setText(String.valueOf(
						board.draw_move_rules_status()));
				}
				finally
				{
					board_lock.unlock();
				}
			}
		}
		
		private final class SettingsPanel extends GamePanelTab
		{
			private final int scale_y_size =
				(int)(2 * text_height + 3.5f * border_size);
			
			private SettingsPanel()
			{
				// Scale settings:
				
				final var scale_panel =
					new JPanel();
				final var scale_panel_dimension =
					new Dimension(tab_x_size, scale_y_size);
				scale_panel.setMaximumSize(scale_panel_dimension);
				scale_panel.setMinimumSize(scale_panel_dimension);
				scale_panel.setPreferredSize(scale_panel_dimension);
				scale_panel.setBorder(BorderFactory.createTitledBorder("Scale"));
				scale_panel.setLayout(new BoxLayout(scale_panel, BoxLayout.X_AXIS));
				scale_panel.setAlignmentY(Component.CENTER_ALIGNMENT);
				
				final var scale_label =
					new Label(
						  "Current: "
						+ Integer.toString(Resources.base_scale_in_percent())
						+ "%.  New:");
				
				final var scale_button =
					new JButton("Apply");
				final var scale_button_dimensions =
					new Dimension(
						  (int)Math.ceil(1.6f * Resources.font_regular.getStringBounds(
							  "Apply it"
							, new FontRenderContext(new AffineTransform(), true, true))
						  .getWidth())
						, text_height + (int)Math.ceil(1.3f * border_size));
				scale_button.setMaximumSize(scale_button_dimensions);
				scale_button.setMinimumSize(scale_button_dimensions);
				scale_button.setPreferredSize(scale_button_dimensions);
				
				final var scale_spinner =
					new JSpinner(new SpinnerNumberModel(
						  Resources.base_scale_in_percent()
						, Resources.base_scale_min_percent
						, Resources.base_scale_max_percent
						, 1));
				scale_spinner.setMaximumSize(scale_button_dimensions);
				scale_spinner.setMinimumSize(scale_button_dimensions);
				scale_spinner.setPreferredSize(scale_button_dimensions);
				final var scale_spinner_text_field =
					((javax.swing.JSpinner.DefaultEditor) scale_spinner.getEditor())
					.getTextField();
				scale_spinner_text_field.setFocusLostBehavior(JFormattedTextField.PERSIST);
				scale_spinner_text_field.addFocusListener(new FocusAdapter()
					{
						@Override public void focusLost(final FocusEvent e)
						{
							try
							{
								scale_spinner.commitEdit();
								scale_spinner_text_field.setBackground(
									new JTextField().getBackground());
								scale_button.setEnabled(true);
							}
							catch (final java.text.ParseException exception)
							{
								scale_spinner_text_field.setBackground(
									new Color(255, 102, 102));
								scale_button.setEnabled(false);
							}
						}
					});
				scale_spinner_text_field.addKeyListener(
					new KeyAdapter()
					{
						@Override public void keyReleased(final KeyEvent e)
						{
							try
							{
								scale_spinner.commitEdit();
								scale_spinner_text_field.setBackground(
									new JTextField().getBackground());
								scale_button.setEnabled(true);
							}
							catch (final java.text.ParseException exception)
							{
								scale_spinner_text_field.setBackground(
									new Color(255, 102, 102));
								scale_button.setEnabled(false);
							}
						}
					});
				
				scale_button.addActionListener(
					(final ActionEvent e) ->
					{
						if (board_lock.tryLock() /* Only try; ignore iff busy. */) try
						{
							if (0 != JOptionPane.showOptionDialog(
								  MainPanel.this
								, "Please restart pmChess to apply the new scale."
								, "Restart required"
								, JOptionPane.DEFAULT_OPTION
								, JOptionPane.INFORMATION_MESSAGE
								, null
								, new String[]{ "Save scale and quit", "Abort" }
								, "Abort"))
							{
								return;
							}
							Resources.write_base_scale_configuration(
								(Integer)scale_spinner.getValue());
							for (var p = getParent(); p != null; p = p.getParent())
							if (p instanceof GUI)
							{
								((GUI) p).exit();
								break;
							}
						}
						finally
						{
							board_lock.unlock();
						}
					});
				
				scale_panel.add(Box.createHorizontalGlue());
				scale_panel.add(scale_label);
				scale_panel.add(Box.createHorizontalGlue());
				scale_panel.add(scale_spinner);
				scale_panel.add(Box.createHorizontalGlue());
				scale_panel.add(scale_button);
				scale_panel.add(Box.createHorizontalGlue());
				
				// Compose everything (scale settings):
				
				setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
				add(scale_panel);
				add(Box.createVerticalGlue());
			}
		}
	}
	
	private final class HistoryPanel extends JPanel
	{
		private final int panel_x_size =
			(int)Math.ceil(
				1.6f * Resources.font_italic.getStringBounds(
					  "initial position"
					, new FontRenderContext(new AffineTransform(), true, true))
				.getWidth());
		private final int panel_y_size =
			board_panel.panel_size + game_panel.panel_y_size;
		
		private final JButton history_undo_button =
			new JButton("\uE045" /* U+23EE */);
		private final JToggleButton history_pause_button =
			new JToggleButton("\uE034" /* U+23F8 */, false);
		private final JButton history_redo_button =
			new JButton("\uE044" /* U+23ED */);
		
		private final DefaultListModel<PastMove> history_data =
			new DefaultListModel<>();
		private final JList<PastMove> history_list =
			new JList<>(history_data);
		
		private final HistoryListener history_listener =
			new HistoryListener();
		
		private HistoryPanel()
		{
			// Setup panel size and layout:
			setOpaque(true);
			final var panel_dimension =
				new Dimension(panel_x_size, panel_y_size);
			setMaximumSize(panel_dimension);
			setMinimumSize(panel_dimension);
			setPreferredSize(panel_dimension);
			setBorder(BorderFactory.createTitledBorder("Game history"));
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			
			// History buttons:
			final var history_button_dimension =
				new Dimension(
					  (int)Math.floor(panel_x_size / 3.0f) - border_size
					, text_height + border_size);
			history_undo_button.setMargin(new Insets(0, 0, 0, 0));
			history_undo_button.setFont(Resources.font_media_control_symbols);
			history_undo_button.setMaximumSize(history_button_dimension);
			history_undo_button.setMinimumSize(history_button_dimension);
			history_undo_button.setPreferredSize(history_button_dimension);
			history_pause_button.setMargin(new Insets(0, 0, 0, 0));
			history_pause_button.setFont(Resources.font_media_control_symbols);
			history_pause_button.setMaximumSize(history_button_dimension);
			history_pause_button.setMinimumSize(history_button_dimension);
			history_pause_button.setPreferredSize(history_button_dimension);
			history_redo_button.setMargin(new Insets(0, 0, 0, 0));
			history_redo_button.setFont(Resources.font_media_control_symbols);
			history_redo_button.setMaximumSize(history_button_dimension);
			history_redo_button.setMinimumSize(history_button_dimension);
			history_redo_button.setPreferredSize(history_button_dimension);
			
			history_pause_button.addItemListener(
				(final ItemEvent e) ->
				{
					final var selected = e.getStateChange() == ItemEvent.SELECTED;
					if (board_lock.tryLock() /* Only try; ignore iff busy. */) try
					{
						final var computer_move =
							board.player() ? computer_w : computer_b;
						history_undo_button.setEnabled(selected && board.turn() > 1);
						history_redo_button.setEnabled(false);
						history_list.setEnabled(selected);
						game_panel.status_panel.pawn_promotion_list.setEnabled(
							   !computer_move
							&& !selected);
						game_panel.status_panel.draw_claim_button.setEnabled(
							   !computer_move
							&& !selected);
						(selected
							? history_list
							: MainPanel.this /* i.e., the board_listener */)
							.requestFocusInWindow();
						if (selected)
						{
							// Update GUI:
							board_panel.repaint();
							game_panel.repaint();
							history_panel.repaint();
						}
						else
						{
							if (board.turn() < history_panel.history_data.size())
							{
								history_panel.history_data.removeRange(
									  board.turn()
									, history_panel.history_data.size() - 1);
							}
							run_game(); // 'run_game()' takes care of repainting.
						}
						return;
					}
					finally
					{
						board_lock.unlock();
					}
					history_pause_button.setSelected(!selected);
				});
			
			history_undo_button.addActionListener(
				(final ActionEvent e) ->
				{
					if (board_lock.tryLock() /* Only try; ignore iff busy. */) try
					{
						history_list.setSelectedIndex(
							Math.max(board.turn() - 2, 0));
						history_listener.keyPressed(new KeyEvent(
							  history_undo_button
							, KeyEvent.KEY_PRESSED
							, System.currentTimeMillis()
							, 0
							, KeyEvent.VK_SPACE
							, KeyEvent.CHAR_UNDEFINED));
					}
					finally
					{
						board_lock.unlock();
					}
				});
			
			history_redo_button.addActionListener(
				(final ActionEvent e) ->
				{
					if (board_lock.tryLock() /* Only try; ignore iff busy. */) try
					{
						history_list.setSelectedIndex(
							Math.min(board.turn(), history_data.size() - 1));
						history_listener.keyPressed(new KeyEvent(
							  history_redo_button
							, KeyEvent.KEY_PRESSED
							, System.currentTimeMillis()
							, 0
							, KeyEvent.VK_SPACE
							, KeyEvent.CHAR_UNDEFINED));
					}
					finally
					{
						board_lock.unlock();
					}
				});
			
			final var history_buttons_panel =
				new JPanel();
			final var history_buttons_panel_dimension =
				new Dimension(panel_x_size, text_height + border_size);
			history_buttons_panel.setMaximumSize(history_buttons_panel_dimension);
			history_buttons_panel.setMinimumSize(history_buttons_panel_dimension);
			history_buttons_panel.setPreferredSize(history_buttons_panel_dimension);
			history_buttons_panel.setLayout(new BoxLayout(history_buttons_panel, BoxLayout.X_AXIS));
			history_buttons_panel.add(Box.createHorizontalGlue());
			history_buttons_panel.add(history_undo_button);
			history_buttons_panel.add(Box.createHorizontalGlue());
			history_buttons_panel.add(history_pause_button);
			history_buttons_panel.add(Box.createHorizontalGlue());
			history_buttons_panel.add(history_redo_button);
			history_buttons_panel.add(Box.createHorizontalGlue());
			
			// History list:
			history_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			history_list.setLayoutOrientation(JList.VERTICAL);
			final var history_scroll_pane =
				new JScrollPane(history_list);
			history_scroll_pane.setVerticalScrollBarPolicy(
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			final var history_scroll_pane_dimension =
				new Dimension(
					  panel_x_size - (int)Math.ceil(1.5f * border_size)
					, panel_y_size - (int)Math.ceil(3.5f * border_size)
					  - history_buttons_panel_dimension.height);
			history_scroll_pane.setMaximumSize(history_scroll_pane_dimension);
			history_scroll_pane.setMinimumSize(history_scroll_pane_dimension);
			history_scroll_pane.setPreferredSize(history_scroll_pane_dimension);
			history_list.addKeyListener(history_listener);
			history_list.setCellRenderer(new HistoryRenderer());
			history_list.setSelectedIndex(0);
			history_data.addElement(new PastMove(0, 0, Board.GameStatus.Normal, false, 0));
			
			add(history_scroll_pane);
			add(Box.createVerticalGlue());
			add(history_buttons_panel);
			add(Box.createVerticalGlue());
		}
		
		@Override public void paintComponent(final Graphics graphics)
		{
			super.paintComponent(graphics);
			Resources.configure_rendering(graphics);
		}
	}
	
	private static final class PastMove
	{
		private final int turn;
		private final int move;
		private final Board.GameStatus status;
		private final boolean is_repetition;
		private final int search_depth;
		
		private PastMove(
			  final int turn
			, final int move
			, final Board.GameStatus status
			, final boolean is_repetition
			, final int search_depth)
		{
			this.turn = turn;
			this.move = move;
			this.status = status;
			this.is_repetition = is_repetition;
			this.search_depth = search_depth;
		}
	}
	
	private final class HistoryRenderer extends DefaultListCellRenderer
	{
		@Override public Component getListCellRendererComponent(
			  final JList<?> list
			, final Object value
			, final int index
			, final boolean is_selected
			, final boolean cell_has_focus)
		{
			final var enabled =
				isEnabled();
			setEnabled(true); // Always render as if enabled, even when disabled...
			super.getListCellRendererComponent(
				  list
				, ""
				, index
				, is_selected
				, cell_has_focus);
			setEnabled(enabled); // ...but remember to set back to actual enabled status.
			
			final var move = (PastMove) value;
			if (move.turn == 0)
			{
				setText("initial position");
				return this;
			}
			
			String notation = ""; // algebraic notation according to FIDE
			
			if (!Move.is_moveless_draw_claim(move.move))
			{
				final var x =
					Move.x(move.move);
				final var y =
					Move.y(move.move);
				final var X =
					Move.X(move.move);
				final var Y =
					Move.Y(move.move);
				final var figure_moved =
					FigurePresentation.get(Move.figure_moved(move.move));
				final var figure_placed =
					FigurePresentation.get(Move.figure_placed(move.move));
				final var figure_captured =
					Move.figure_destination(move.move);
				
				if (figure_moved.figure.is_king() && X - x == 2)
				{
					notation = move("0-0");
				}
				else if (figure_moved.figure.is_king() && x - X == 2)
				{
					notation = move("0-0-0");
				}
				else
				{
					final var en_passant =
						figure_moved.figure.is_pawn()
						&& figure_captured == null
						&& x != X;
					notation =
						(figure_moved.figure.is_pawn()
							? ""
							: figure(figure_moved))
						+ move(file(x) + rank(y))
						+ (figure_captured == null && !en_passant
							? ""
							: info("x"))
						+ move(file(X) + rank(Y))
						+ (figure_moved.figure == figure_placed.figure
							? ""
							: figure(figure_placed))
						+ (en_passant ? info("e.p.") : "");
				}
				
				if (move.status == Board.GameStatus.Check)
				{
					notation += info("+");
				}
				else if (move.status == Board.GameStatus.Checkmate)
				{
					notation += info("++");
				}
			}
			
			if (move.status == Board.GameStatus.Draw)
			{
				notation += (notation.isEmpty() ? "" : " ") + info("(=)");
			}
			
			setBackground(move.is_repetition
				? Color.lightGray
				: Color.white);
			setForeground(index >= board.turn()
				? Color.red
				: Color.black);
			setText(Board.move(move.turn)
				+ (move.turn % 2 == 0 ? "\u2026 " : ". ")
				+ notation);
			
			return this;
		}
		
		@Override public void setText(final String text)
		{
			super.setText("<html>" + html(Resources.font_italic, text) + "</html>");
		}
		
		private static String file(final int x)
		{
			return String.valueOf((char)('a' + x));
		}
		
		private static String rank(final int y)
		{
			return String.valueOf((char)('1' + y));
		}
		
		private static String figure(final FigurePresentation figure)
		{
			return html(figure.font, figure.unicode);
		}
		
		private static String move(final String text)
		{
			return html(Resources.font_regular, text);
		}
		
		private static String info(final String text)
		{
			return html(Resources.font_bold_italic, text);
		}
		
		private static String html(final Font font, final String text)
		{
			return "<span style=\"font-family:"
				+ font.getFontName()
				+ ";font-size:"
				+ Math.round(1.1f * font.getSize2D())
				+ "pt;\">"
				+ text
				+ "</span>";
		}
		
		@Override public void paintComponent(final Graphics graphics)
		{
			super.paintComponent(graphics);
			Resources.configure_rendering(graphics);
		}
	}
}
