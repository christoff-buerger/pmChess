/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import pmchess.logic.*;

import pmchess.gui.Resources.*;

public final class GamePanel extends JPanel {
	private static final Image bulb = Resources.load_image("icons/bulb.png");
	
	private final Board board = new Board();
	private final Search search = new Search();
	private final Evaluator evaluator = new Evaluator();
	
	private boolean capitulation = false;
	
	private boolean computer_w = false;
	private boolean computer_b = false;
	
	private int cursor_x = 0;
	private int cursor_y = 0;
	private int selected_x = 0;
	private int selected_y = 0;
	private Figure selected_figure = null;
	
	private final BoardPanel board_panel = new BoardPanel();
	private final StatusPanel status_panel = new StatusPanel();
	private final HistoryPanel history_panel = new HistoryPanel();
	
	protected GamePanel() {
		// Setup panel size and layout:
		setOpaque(true);
		final var border_size = 5; // graphical-layout configuration-variable
		final var panel_dimension = new Dimension(
			board_panel.panel_size + history_panel.panel_x_size + 2 * border_size,
			board_panel.panel_size + status_panel.panel_y_size + 2 * border_size);
		setMaximumSize(panel_dimension);
		setMinimumSize(panel_dimension);
		setPreferredSize(panel_dimension);
		setBorder(BorderFactory.createEmptyBorder(
			border_size,
			border_size,
			border_size,
			border_size));
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		// Add components:
		final var dummy_panel = new JPanel();
		final var dummy_panel_dimension = new Dimension(
			board_panel.panel_size,
			board_panel.panel_size + status_panel.panel_y_size);
		dummy_panel.setMaximumSize(dummy_panel_dimension);
		dummy_panel.setMinimumSize(dummy_panel_dimension);
		dummy_panel.setPreferredSize(dummy_panel_dimension);
		dummy_panel.setLayout(new BoxLayout(dummy_panel, BoxLayout.Y_AXIS));
		dummy_panel.add(board_panel);
		dummy_panel.add(status_panel);
		add(dummy_panel);
		add(history_panel);
		
		// Add listener for cursor movement and figure selection:
		setFocusable(true);
      		requestFocusInWindow();
		addKeyListener(new BoardListener());
		
		// Initialize and start game:
		initialize(false, false);
	}
	
	@Override public void paintComponent(final Graphics graphics) {
		super.paintComponent(graphics);
		Resources.configure_rendering(graphics);
	}
	
	protected void initialize(final boolean computer_w, final boolean computer_b) {
		while (undo()) {
		}
		this.computer_w = computer_w;
		this.computer_b = computer_b;
		run_game();
	}
	
	private boolean undo() {
		if (board.undo() != 0) {
			history_panel.history_data.removeElementAt(
				history_panel.history_data.size() - 1);
			selected_figure = null;
			capitulation = false;
			board_panel.repaint();
			status_panel.repaint();
			return true;
		}
		return false;
	}
	
	private void run_game() {
		var game_status = board.status();
		while (computer_turn()
			&& (game_status == Board.GameStatus.Normal
				|| game_status == Board.GameStatus.Check))
		{
			paintImmediately(0, 0, getWidth(), getHeight());
			final var move = search.select_move(board, evaluator);
			capitulation = move == 0;
			if (capitulation) {
				break;
			}
			board.execute(Move.x(move), Move.y(move), Move.X(move), Move.Y(move));
			game_status = board.status();
			history_panel.history_data.addElement(new PastMove(
				board.turn() - 1,
				move,
				game_status));
		}
		board_panel.repaint();
		status_panel.repaint();
	}
	
	private boolean computer_turn() {
		return board.player() ? computer_w : computer_b;
	}
	
	private final class BoardListener extends KeyAdapter {
		@Override public void keyPressed(final KeyEvent event) {
			if (computer_turn()) {
				return;
			}
			final var old_x = cursor_x;
			final var old_y = cursor_y;
			final var key = event.getKeyCode();
			if (key == KeyEvent.VK_SPACE) {
				final var figure = board.figure(cursor_x, cursor_y);
				if (figure != null && figure.owner == board.player()) {
					selected_figure = null;
					board_panel.draw_square(selected_x, selected_y);
					selected_figure = figure;
					selected_x = cursor_x;
					selected_y = cursor_y;
					board_panel.draw_square(selected_x, selected_y);
				} else if (selected_figure != null) {
					if (board.execute(
						selected_x,
						selected_y,
						cursor_x,
						cursor_y))
					{
						history_panel.history_data.addElement(new PastMove(
							board.turn() - 1,
							board.previous_move(board.turn() - 1),
							board.status()));
						selected_figure = null;
						run_game();
					}
				}
				return;
			} else if (key == KeyEvent.VK_UP && cursor_y < 7) {
				cursor_y++;
			} else if (key == KeyEvent.VK_DOWN && cursor_y > 0) {
				cursor_y--;
			} else if (key == KeyEvent.VK_LEFT && cursor_x > 0) {
				cursor_x--;
			} else if (key == KeyEvent.VK_RIGHT && cursor_x < 7) {
				cursor_x++;
			} else { // Unknown key or invalid movement:
				return;
			}
			board_panel.draw_square(old_x, old_y);
			board_panel.draw_square(cursor_x, cursor_y);
		}
	}
	
	private final class HistoryListener extends KeyAdapter {
		@Override public void keyPressed(final KeyEvent event) {
			final var game_status = board.status();
			if (computer_turn() && !capitulation
				&& game_status != Board.GameStatus.Checkmate
				&& game_status != Board.GameStatus.Stalemate)
			{
				return;
			}
			final var key = event.getKeyCode();
			if (key == KeyEvent.VK_SPACE) {
				final var selected = history_panel.history_list.getSelectedIndex();
				for (var i = board.turn() - selected - 1; i > 0; i--) {
					undo();
				}
				run_game();
			}
		}
	}
	
	private final class BoardPanel extends JPanel {
		private final int border_size = 40;	// graphical-layout configuration-variable
		private final int tile_size = 36;	// graphical-layout configuration-variable
		private final int cursor_line_width =
			(int)Math.ceil(tile_size / 10.0f) % 2 == 0
			? (int)Math.ceil(tile_size / 10.0f)
			: (int)Math.ceil(tile_size / 10.0f) + 1;
		private final int panel_size = 8 * tile_size + 2 * border_size;
		private final Font figure_font = FigurePresentation.font.deriveFont(
			tile_size - 2.0f * cursor_line_width);
		
		private BoardPanel() {
			// Setup panel size and layout:
			setOpaque(true);
			final var panel_dimension = new Dimension(panel_size, panel_size);
			setMaximumSize(panel_dimension);
			setMinimumSize(panel_dimension);
			setPreferredSize(panel_dimension);
			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Chessboard"),
				BorderFactory.createEmptyBorder(
					border_size,
					border_size,
					border_size,
					border_size)));
		}
		
		@Override public void paintComponent(final Graphics graphic) {
			super.paintComponent(graphic);
			Resources.configure_rendering(graphic);
			
			// Draw horizontal (h) and vertical (v) border-markings:
			graphic.setFont(Resources.font_bold);
			final var font_metrics = graphic.getFontMetrics();
			final var font_height = font_metrics.getAscent();
			final var h_y_base =
				border_size
				+ (tile_size - font_height) / 2
				+ font_height;
			final var v_y_base =
				(border_size - font_height) / 2
				+ font_height
				+ /* Adjust for lowercase and titled border: */ font_height / 3;
			for (var i = 0; i < 8; i++) {
				final var h_marking = String.valueOf((char)('8' - i));
				final var h_width = font_metrics.stringWidth(h_marking);
				final var h_x_base = (border_size - h_width) / 2;
				final var h_y = h_y_base + i * tile_size;
				graphic.drawString(h_marking, h_x_base, h_y);
				graphic.drawString(
					h_marking,
					panel_size - h_x_base - h_width,
					h_y);
				final var v_marking = String.valueOf((char)('a' + i));
				final var v_width = font_metrics.stringWidth(v_marking);
				final var v_x =
					border_size
					+ i * tile_size
					+ (tile_size - v_width) / 2;
				graphic.drawString(v_marking, v_x, v_y_base);
				graphic.drawString(
					v_marking,
					v_x,
					panel_size - v_y_base + font_height);
			}
			
			// Draw tiles:
			for (var x = 7; x >= 0; x--) {
				for (var y = 7; y >= 0; y--) {
					draw_square(graphic, x, y);
				}
			}
		}
		
		private void draw_square(final int x, final int y) {
			draw_square(getGraphics(), x, y);
		}
		
		private void draw_square(final Graphics graphic, final int x, final int y) {
			Resources.configure_rendering(graphic);
			
			final var y_trans = 7 - y;
			
			// Draw background tile:
			var color = ((x + y_trans) % 2) == 0 ? Color.white : Color.lightGray;
			final var last_move = board.previous_move(board.turn() - 1);
			if (last_move != 0) {
				final var _x_ = Move.x(last_move);
				final var _X_ = Move.X(last_move);
				if ((x == _x_ && y == Move.y(last_move))
					|| (y == Move.Y(last_move)
					&& (x == _X_
					// Check for rook positions of recent castling:
					|| (Move.figure_moved(last_move).is_king()
					&& ((_X_ == _x_ - 2 && (x == 0 || x == 3))
						|| (_X_ == _x_ + 2 && (x == 7 || x == 5)))))))
				{
					color = new Color(77, 164, 77);
				}
			}
			graphic.setColor(color);
			graphic.fillRect(
				x * tile_size + border_size,
				y_trans * tile_size + border_size,
				tile_size,
				tile_size);
			
			// Draw figure:
			final var figure = board.figure(x, y);
			if (figure != null) {
				graphic.setColor(Color.black);
				graphic.setFont(figure_font);
				final var text = FigurePresentation.get(figure).unicode;
				final var metrics = graphic.getFontMetrics();
				final var width_fix =
					(tile_size - metrics.stringWidth(text)) / 2;
				final var height_fix =
					(tile_size + metrics.getAscent() + metrics.getDescent()) / 2
					- metrics.getDescent();
				graphic.drawString(
					text,
					x * tile_size + border_size + width_fix,
					y_trans * tile_size + border_size + height_fix);
			}
			
			// Draw cursor and figure selection:
			final var old_stroke = ((Graphics2D)graphic).getStroke();
			((Graphics2D)graphic).setStroke(new BasicStroke(cursor_line_width));
			final var x_pos = x * tile_size
				+ border_size
				+ (cursor_line_width / 2)
				+ 1;
			final var y_pos = y_trans * tile_size
				+ border_size
				+ (cursor_line_width / 2)
				+ 1;
			final var distance = tile_size - cursor_line_width - 2;
			if (x == cursor_x && y == cursor_y) {
				graphic.setColor(Color.blue);
				graphic.drawRect(x_pos, y_pos, distance, distance);
			}
			if (selected_figure != null && x == selected_x && y == selected_y) {
				graphic.setColor(Color.red);
				graphic.drawRect(x_pos, y_pos, distance, distance);
			}
			((Graphics2D)graphic).setStroke(old_stroke);
		}
	}
	
	private final class StatusPanel extends JPanel {
		private final int panel_x_size = board_panel.panel_size;
		private final int panel_y_size = 140; // graphical-layout configuration-variable
		private final int border_x_size = 10; // graphical-layout configuration-variable
		private final int status_x_size = panel_x_size - 4 * border_x_size;
		private final int status_y_size =
			(new FontMetrics(Resources.font_bold) {}).getHeight()
			+ 3 * border_x_size;
		private final int castling_x_size = panel_x_size / 2 - 2 * border_x_size;
		private final int castling_y_size = (2 * (panel_y_size - status_y_size)) / 3;
		
		private final JLabel status = new JLabel() {
			@Override public void paintComponent(final Graphics graphic) {
				super.paintComponent(graphic);
				Resources.configure_rendering(graphic);
				
				final var game_status = board.status();
				final var bulb_x =
					status_x_size - bulb.getWidth(StatusPanel.this);
				final var bulb_y =
					(status_y_size - bulb.getHeight(StatusPanel.this)) / 2;
				if (computer_turn()
					&& !capitulation
					&& game_status != Board.GameStatus.Checkmate
					&& game_status != Board.GameStatus.Stalemate)
				{
					graphic.drawImage(bulb, bulb_x, bulb_y, StatusPanel.this);
				}
			}
		};
		private final JCheckBox castling_l_w = new JCheckBox("left");
		private final JCheckBox castling_r_w = new JCheckBox("right");
		private final JCheckBox castling_l_b = new JCheckBox("left");
		private final JCheckBox castling_r_b = new JCheckBox("right");
		
		private StatusPanel() {
			// Setup panel size and layout:
			setOpaque(true);
			final var panel_dimension =
				new Dimension(panel_x_size, panel_y_size);
			setMaximumSize(panel_dimension);
			setMinimumSize(panel_dimension);
			setPreferredSize(panel_dimension);
			setBorder(BorderFactory.createTitledBorder("Game status"));
			
			// Status message box:
			final var status_dimension =
				new Dimension(status_x_size, status_y_size);
			status.setOpaque(true);
			status.setFont(Resources.font_bold);
			status.setMaximumSize(status_dimension);
			status.setMinimumSize(status_dimension);
			status.setPreferredSize(status_dimension);
			add(status);
			
			// Allowed castlings information boxes:
			final var castling_dimension =
				new Dimension(castling_x_size, castling_y_size);
			castling_l_w.setEnabled(false);
			castling_r_w.setEnabled(false);
			castling_l_b.setEnabled(false);
			castling_r_b.setEnabled(false);
			final var castling_w = new JPanel();
			castling_w.setBorder(BorderFactory.createTitledBorder("White castling"));
			castling_w.setMaximumSize(castling_dimension);
			castling_w.setMinimumSize(castling_dimension);
			castling_w.setPreferredSize(castling_dimension);
			castling_w.add(castling_l_w);
			castling_w.add(castling_r_w);
			add(castling_w);
			final var castling_b = new JPanel();
			castling_b.setBorder(BorderFactory.createTitledBorder("Black castling"));
			castling_b.setMaximumSize(castling_dimension);
			castling_b.setMinimumSize(castling_dimension);
			castling_b.setPreferredSize(castling_dimension);
			castling_b.add(castling_l_b);
			castling_b.add(castling_r_b);
			add(castling_b);
		}
		
		@Override public void paintComponent(final Graphics graphic) {
			super.paintComponent(graphic);
			Resources.configure_rendering(graphic);
			
			// Update status message:
			final var now = board.player() ? "White" : "Black";
			final var next = board.player() ? "Black" : "White";
			final var game_status = board.status();
			final String message;
			if (capitulation) {
				message = now + " capitulates. " + next + " wins.";
			} else {
				switch (game_status) {
				case Checkmate:
					message = now + " checkmate. " + next + " wins.";
					break;
				case Check:
					message = now + " in check. " + now + "'s turn.";
					break;
				case Stalemate:
					message = "Stalemate. " + now + " cannot move.";
					break;
				default:
					message = now + "'s turn.";
				}
			}
			status.setBackground(board.player() ? Color.white : Color.black);
			status.setForeground(board.player() ? Color.black : Color.white);
			status.setText("  " + Integer.toString(board.move()) + ": " + message);
			
			// Update allowed castlings:
			castling_l_w.setSelected(board.castling_allowed(true, true));
			castling_r_w.setSelected(board.castling_allowed(false, true));
			castling_l_b.setSelected(board.castling_allowed(true, false));
			castling_r_b.setSelected(board.castling_allowed(false, false));
		}
	}
	
	private final class HistoryPanel extends JPanel {
		private final int panel_x_size = 150; // graphical-layout configuration-variable
		private final int panel_y_size = board_panel.panel_size
			+ status_panel.panel_y_size;
		private final int border_size = 15; // graphical-layout configuration-variable
		
		private final DefaultListModel<PastMove> history_data = new DefaultListModel<>();
		private final JList<PastMove> history_list = new JList<>(history_data);
		
		private HistoryPanel() {
			// Setup panel size and layout:
			setOpaque(true);
			final var panel_dimension =
				new Dimension(panel_x_size, panel_y_size);
			setMaximumSize(panel_dimension);
			setMinimumSize(panel_dimension);
			setPreferredSize(panel_dimension);
			setBorder(BorderFactory.createTitledBorder("Game history"));
			
			// History list:
			history_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			history_list.setLayoutOrientation(JList.VERTICAL);
			final var history_scroll_pane = new JScrollPane(history_list);
			history_scroll_pane.setVerticalScrollBarPolicy(
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			final var history_scroll_pane_dimension = new Dimension(
				panel_x_size - border_size,
				panel_y_size - (5 * border_size) / 2);
			history_scroll_pane.setMaximumSize(history_scroll_pane_dimension);
			history_scroll_pane.setMinimumSize(history_scroll_pane_dimension);
			history_scroll_pane.setPreferredSize(history_scroll_pane_dimension);
			history_list.addKeyListener(new HistoryListener());
			history_list.setCellRenderer(new HistoryRenderer());
			history_data.addElement(new PastMove(0, 0, Board.GameStatus.Normal));
			add(history_scroll_pane);
		}
		
		@Override public void paintComponent(final Graphics graphics) {
			super.paintComponent(graphics);
			Resources.configure_rendering(graphics);
		}
	}
	
	private static final class PastMove {
		private final int turn;
		private final int move;
		private final Board.GameStatus status;
		
		private PastMove(final int turn, final int move, final Board.GameStatus status) {
			this.turn = turn;
			this.move = move;
			this.status = status;
		}
	}
	
	private static final class HistoryRenderer extends DefaultListCellRenderer {
		@Override public Component getListCellRendererComponent(
			final JList<?> list,
			final Object value,
			final int index,
			final boolean is_selected,
			final boolean cell_has_focus)
		{
			super.getListCellRendererComponent(
				list,
				"",
				index,
				is_selected,
				cell_has_focus);
			
			final var move = (PastMove)value;
			if (move.move == 0) {
				setText("initial position");
				return this;
			}
			
			final var x = Move.x(move.move);
			final var y = Move.y(move.move);
			final var X = Move.X(move.move);
			final var Y = Move.Y(move.move);
			final var figure_moved =
				FigurePresentation.get(Move.figure_moved(move.move));
			final var figure_placed =
				FigurePresentation.get(Move.figure_placed(move.move));
			final var figure_captured = Move.figure_destination(move.move);
			
			final String notation; // algebraic notation according to FIDE
			if (figure_moved.figure.is_king() && X - x == 2) {
				notation = move("0-0");
			} else if (figure_moved.figure.is_king() && x - X == 2) {
				notation = move("0-0-0");
			} else {
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
			setText(Board.move(move.turn)
				+ (move.turn % 2 == 0 ? "\u2026 " : ". ")
				+ notation
				+ (move.status == Board.GameStatus.Check ? info("+") : "")
				+ (move.status == Board.GameStatus.Checkmate ? info("++") : ""));
			
			return this;
		}
		
		@Override public void setText(final String text) {
			super.setText("<html>" + html(Resources.font_italic, text) + "</html>");
		}
		
		private static String file(final int x) {
			return String.valueOf((char)('a' + x));
		}
		
		private static String rank(final int y) {
			return String.valueOf((char)('1' + y));
		}
		
		private static String figure(final FigurePresentation figure) {
			return html(figure.font, figure.unicode);
		}
		
		private static String move(final String text) {
			return html(Resources.font_regular, text);
		}
		
		private static String info(final String text) {
			return html(Resources.font_bold_italic, text);
		}
		
		private static String html(final Font font, final String text) {
			return "<span style=\"font-family:"
				+ font.getFontName()
				+ ";font-size:"
				+ Math.round(1.1f * font.getSize2D())
				+ "pt;\">"
				+ text
				+ "</span>";
		}
		
		@Override public void paintComponent(final Graphics graphics) {
			super.paintComponent(graphics);
			Resources.configure_rendering(graphics);
		}
	}
}
