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
	private static final Image bulb = Resources.loadImage("icons/bulb.png");
	
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
	
	private final BoardPanel boardPanel = new BoardPanel();
	private final StatusPanel statusPanel = new StatusPanel();
	private final HistoryPanel historyPanel = new HistoryPanel();
	
	protected GamePanel() {
		// Setup panel size and layout:
		setOpaque(true);
		final int border_size = 5;
		final Dimension panel_dimension = new Dimension(
			boardPanel.panel_size + historyPanel.panel_x_size + 2 * border_size,
			boardPanel.panel_size + statusPanel.panel_y_size + 2 * border_size);
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
		final JPanel dummyPanel = new JPanel();
		final Dimension dummyPanel_dimension = new Dimension(
			boardPanel.panel_size,
			boardPanel.panel_size + statusPanel.panel_y_size);
		dummyPanel.setMaximumSize(dummyPanel_dimension);
		dummyPanel.setMinimumSize(dummyPanel_dimension);
		dummyPanel.setPreferredSize(dummyPanel_dimension);
		dummyPanel.setLayout(new BoxLayout(dummyPanel, BoxLayout.Y_AXIS));
		dummyPanel.add(boardPanel);
		dummyPanel.add(statusPanel);
		add(dummyPanel);
		add(historyPanel);
		
		// Add listener for cursor movement and figure selection:
		setFocusable(true);
      		requestFocusInWindow();
		addKeyListener(new BoardListener());
		
		// Initialize and start game:
		initialize(false, false);
	}
	
	protected void initialize(final boolean computer_w, final boolean computer_b) {
		while (undo());
		this.computer_w = computer_w;
		this.computer_b = computer_b;
		runGame();
	}
	
	private boolean undo() {
		if (board.undo() != 0) {
			historyPanel.history_data.removeElementAt(
				historyPanel.history_data.size() - 1);
			selected_figure = null;
			capitulation = false;
			boardPanel.repaint();
			statusPanel.repaint();
			return true;
		}
		return false;
	}
	
	private void runGame() {
		Board.GameStatus game_status = board.status();
		while (computerTurn() &&
			(game_status == Board.GameStatus.Normal ||
			 game_status == Board.GameStatus.Check))
		{
			paintImmediately(0, 0, getWidth(), getHeight());
			final int move = search.selectMove(board, evaluator);
			capitulation = move == 0;
			if (capitulation)
				break;
			board.execute(Move.x(move), Move.y(move), Move.X(move), Move.Y(move));
			game_status = board.status();
			historyPanel.history_data.addElement(new PastMove(
				board.turn() - 1,
				move,
				game_status));
		}
		boardPanel.repaint();
		statusPanel.repaint();
	}
	
	private boolean computerTurn() {
		return board.player() ? computer_w : computer_b;
	}
	
	private final class BoardListener extends KeyAdapter {
		@Override public void keyPressed(final KeyEvent event) {
			if (computerTurn())
				return;
			final int old_x = cursor_x, old_y = cursor_y;
			final int key = event.getKeyCode();
			if (key == KeyEvent.VK_SPACE) {
				final Figure figure = board.figure(cursor_x, cursor_y);
				if (figure != null && figure.owner == board.player()) {
					selected_figure = null;
					boardPanel.drawSquare(selected_x, selected_y);
					selected_figure = figure;
					selected_x = cursor_x;
					selected_y = cursor_y;
					boardPanel.drawSquare(selected_x, selected_y);
				} else if (selected_figure != null) {
					if (board.execute(
						selected_x,
						selected_y,
						cursor_x,
						cursor_y))
					{
						historyPanel.history_data.addElement(new PastMove(
							board.turn() - 1,
							board.previousMove(board.turn() - 1),
							board.status()));
						selected_figure = null;
						runGame();
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
			boardPanel.drawSquare(old_x, old_y);
			boardPanel.drawSquare(cursor_x, cursor_y);
		}
	}
	
	private final class HistoryListener extends KeyAdapter {
		@Override public void keyPressed(final KeyEvent event) {
			final Board.GameStatus gameStatus = board.status();
			if (computerTurn() && !capitulation &&
				gameStatus != Board.GameStatus.Checkmate &&
				gameStatus != Board.GameStatus.Stalemate)
			{
				return;
			}
			final int key = event.getKeyCode();
			if (key == KeyEvent.VK_SPACE) {
				final int selected = historyPanel.history_list.getSelectedIndex();
				for (int i = board.turn() - selected - 1; i > 0; i--) {
					undo();
				}
				runGame();
			}
		}
	}
	
	private final class BoardPanel extends JPanel {
		private final int border_size = 40;	// configuration-variable
		private final int tile_size = 36;	// configuration-variable
		private final int cursor_width = 3;	// configuration-variable
		private final int panel_size = 8 * tile_size + 2 * border_size;
		
		private BoardPanel() {
			// Setup panel size and layout:
			setOpaque(true);
			final Dimension panel_dimension = new Dimension(panel_size, panel_size);
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
			
			// Draw horizontal (h) and vertical (v) border-markings:
			graphic.setFont(Resources.font_bold);
			final FontMetrics font_metrics = graphic.getFontMetrics();
			final int font_height = font_metrics.getAscent();
			final int h_y_base =
				border_size +
				(tile_size - font_height) / 2 +
				font_height;
			final int v_y_base =
				(border_size - font_height) / 2 +
				font_height +
				/* Adjust for lowercase and titled border: */ font_height / 3;
			for (int i = 0; i < 8; i++) {
				final String h_marking = String.valueOf((char)('8' - i));
				final int h_width = font_metrics.stringWidth(h_marking);
				final int h_x_base = (border_size - h_width) / 2;
				final int h_y = h_y_base + i * tile_size;
				graphic.drawString(h_marking, h_x_base, h_y);
				graphic.drawString(
					h_marking,
					panel_size - h_x_base - h_width,
					h_y);
				final String v_marking = String.valueOf((char)('a' + i));
				final int v_width = font_metrics.stringWidth(v_marking);
				final int v_x =
					border_size +
					i * tile_size +
					(tile_size - v_width) / 2;
				graphic.drawString(v_marking, v_x, v_y_base);
				graphic.drawString(
					v_marking,
					v_x,
					panel_size - v_y_base + font_height);
			}
			
			// Draw tiles:
			for (int x = 7; x >= 0; x--) {
				for (int y = 7; y >= 0; y--) {
					drawSquare(graphic, x, y);
				}
			}
		}
		
		private void drawSquare(final int x, final int y) {
			drawSquare(getGraphics(), x, y);
		}
		
		private void drawSquare(final Graphics graphic, final int x, final int y) {
			final int y_trans = 7 - y;
			
			// Draw background tile:
			Color color = ((x + y_trans) % 2) == 0 ? Color.white : Color.lightGray;
			final int lastMove = board.previousMove(board.turn() - 1);
			if (lastMove != 0) {
				final int _x_ = Move.x(lastMove);
				final int _X_ = Move.X(lastMove);
				if ((x == _x_ && y == Move.y(lastMove)) ||
					(y == Move.Y(lastMove) &&
					(x == _X_ ||
					// Check for rook positions of recent castling:
					(Move.figure_moved(lastMove).isKing() &&
					((_X_ == _x_ - 2 && (x == 0 || x == 3)) ||
					 (_X_ == _x_ + 2 && (x == 7 || x == 5)))))))
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
			final Figure figure = board.figure(x, y);
			if (figure != null) {
				graphic.drawImage(
					FigurePresentation.get(figure).image,
					x * tile_size + border_size,
					y_trans * tile_size + border_size,
					this);
			}
			
			// Draw cursor and figure selection:
			final Stroke oldStroke = ((Graphics2D)graphic).getStroke();
			((Graphics2D)graphic).setStroke(new BasicStroke(cursor_width));
			if (x == cursor_x && y == cursor_y) {
				graphic.setColor(Color.blue);
				graphic.drawRect(
					x * tile_size + border_size + 1,
					y_trans * tile_size + border_size + 1,
					tile_size - cursor_width,
					tile_size - cursor_width);
			}
			if (selected_figure != null && x == selected_x && y == selected_y) {
				graphic.setColor(Color.red);
				graphic.drawRect(
					x * tile_size + border_size + 1,
					y_trans * tile_size + border_size + 1,
					tile_size - cursor_width,
					tile_size - cursor_width);
			}
			((Graphics2D)graphic).setStroke(oldStroke);
		}
	}
	
	private final class StatusPanel extends JPanel {
		private final int panel_x_size = boardPanel.panel_size;
		private final int panel_y_size = 140; // configuration-variable
		private final int border_x_size = 10; // configuration-variable
		private final int status_x_size = panel_x_size - 4 * border_x_size;
		private final int status_y_size =
			(new FontMetrics(Resources.font_bold) {}).getHeight() +
			3 * border_x_size;
		private final int castling_x_size = panel_x_size / 2 - 2 * border_x_size;
		private final int castling_y_size = (2 * (panel_y_size - status_y_size)) / 3;
		
		private final JLabel status = new JLabel() {
			@Override public void paintComponent(final Graphics graphic) {
				super.paintComponent(graphic);
				final Board.GameStatus gameStatus = board.status();
				final int bulb_x =
					status_x_size - bulb.getWidth(StatusPanel.this);
				final int bulb_y =
					(status_y_size - bulb.getHeight(StatusPanel.this)) / 2;
				if (computerTurn() &&
					!capitulation &&
					gameStatus != Board.GameStatus.Checkmate &&
					gameStatus != Board.GameStatus.Stalemate)
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
			final Dimension panel_dimension =
				new Dimension(panel_x_size, panel_y_size);
			setMaximumSize(panel_dimension);
			setMinimumSize(panel_dimension);
			setPreferredSize(panel_dimension);
			setBorder(BorderFactory.createTitledBorder("Game status"));
			
			// Status message box:
			final Dimension status_dimension =
				new Dimension(status_x_size, status_y_size);
			status.setOpaque(true);
			status.setFont(Resources.font_bold);
			status.setMaximumSize(status_dimension);
			status.setMinimumSize(status_dimension);
			status.setPreferredSize(status_dimension);
			add(status);
			
			// Allowed castlings information boxes:
			final Dimension castling_dimension =
				new Dimension(castling_x_size, castling_y_size);
			castling_l_w.setEnabled(false);
			castling_r_w.setEnabled(false);
			castling_l_b.setEnabled(false);
			castling_r_b.setEnabled(false);
			JPanel panel = new JPanel();
			panel.setBorder(BorderFactory.createTitledBorder("White castling"));
			panel.setMaximumSize(castling_dimension);
			panel.setMinimumSize(castling_dimension);
			panel.setPreferredSize(castling_dimension);
			panel.add(castling_l_w);
			panel.add(castling_r_w);
			add(panel);
			panel = new JPanel();
			panel.setBorder(BorderFactory.createTitledBorder("Black castling"));
			panel.setMaximumSize(castling_dimension);
			panel.setMinimumSize(castling_dimension);
			panel.setPreferredSize(castling_dimension);
			panel.add(castling_l_b);
			panel.add(castling_r_b);
			add(panel);
		}
		
		@Override public void paintComponent(final Graphics graphic) {
			super.paintComponent(graphic);
			
			// Update status message:
			final String pNow = board.player() ? "White" : "Black";
			final String pNext = board.player() ? "Black" : "White";
			final Board.GameStatus gameStatus = board.status();
			final String message;
			if (capitulation) {
				message = pNow + " capitulates. " + pNext + " wins.";
			} else {
				switch (gameStatus) {
				case Checkmate:
					message = pNow + " checkmate. " + pNext + " wins.";
					break;
				case Check:
					message = pNow + " in check. " + pNow + "'s turn.";
					break;
				case Stalemate:
					message = "Stalemate. " + pNow + " cannot move.";
					break;
				default:
					message = pNow + "'s turn.";
				}
			}
			status.setBackground(board.player() ? Color.white : Color.black);
			status.setForeground(board.player() ? Color.black : Color.white);
			status.setText("  " + Integer.toString(board.move()) + ": " + message);
			
			// Update allowed castlings:
			castling_l_w.setSelected(board.castlingAllowed(true, true));
			castling_r_w.setSelected(board.castlingAllowed(false, true));
			castling_l_b.setSelected(board.castlingAllowed(true, false));
			castling_r_b.setSelected(board.castlingAllowed(false, false));
		}
	}
	
	private final class HistoryPanel extends JPanel {
		private final int panel_x_size = 150; // configuration-variable
		private final int panel_y_size = boardPanel.panel_size + statusPanel.panel_y_size;
		private final int border_size = 15; // configuration-variable
		
		private final DefaultListModel<PastMove> history_data = new DefaultListModel<>();
		private final JList<PastMove> history_list = new JList<>(history_data);
		
		private HistoryPanel() {
			// Setup panel size and layout:
			setOpaque(true);
			final Dimension panel_dimension =
				new Dimension(panel_x_size, panel_y_size);
			setMaximumSize(panel_dimension);
			setMinimumSize(panel_dimension);
			setPreferredSize(panel_dimension);
			setBorder(BorderFactory.createTitledBorder("Game history"));
			
			// History list:
			history_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			history_list.setLayoutOrientation(JList.VERTICAL);
			final JScrollPane historyScrollPane = new JScrollPane(history_list);
			historyScrollPane.setVerticalScrollBarPolicy(
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			final Dimension historyScrollPane_dimension = new Dimension(
				panel_x_size - border_size,
				panel_y_size - (5 * border_size) / 2);
			historyScrollPane.setMaximumSize(historyScrollPane_dimension);
			historyScrollPane.setMinimumSize(historyScrollPane_dimension);
			historyScrollPane.setPreferredSize(historyScrollPane_dimension);
			history_list.addKeyListener(new HistoryListener());
			history_list.setCellRenderer(new HistoryRenderer());
			history_data.addElement(new PastMove(0, 0, Board.GameStatus.Normal));
			add(historyScrollPane);
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
			final boolean isSelected,
			final boolean cellHasFocus)
		{
			super.getListCellRendererComponent(
				list,
				"",
				index,
				isSelected,
				cellHasFocus);
			
			final PastMove move = (PastMove)value;
			if (move.move == 0) {
				setText(html(Resources.font_italic, "initial position"));
				return this;
			}
			
			final FigurePresentation figure_moved =
				FigurePresentation.get(Move.figure_moved(move.move));
			final Figure figure_captured = Move.figure_destination(move.move);
			final int x = Move.x(move.move);
			final int X = Move.X(move.move);
			final int Y = Move.Y(move.move);
			
			final String notation; // algebraic notation according to FIDE
			if (figure_moved.figure.isPawn()) {
				final FigurePresentation figure_placed =
					FigurePresentation.get(Move.figure_placed(move.move));
				notation = (figure_captured == null ? "" : file(x) + "x") +
					file(X) +
					rank(Y) +
					(figure_placed.figure.isPawn() ?
						"" :
						html(figure_placed.font, figure_placed.unicode));
			} else if (figure_moved.figure.isKing() && X - x == 2) {
				notation = "0-0"; // kingside castlings
			} else if (figure_moved.figure.isKing() && x - X == 2) {
				notation = "0-0-0"; // queenside castlings
			} else {
				notation = html(figure_moved.font, figure_moved.unicode) +
					(figure_captured == null ? "" : "x") +
					file(X) +
					rank(Y);
			}
			setText(html(Resources.font_plain,
				Board.move(move.turn) +
				(move.turn % 2 == 0 ? "\u2026 " : ". ") +
				notation +
				(move.status == Board.GameStatus.Check ? "+" : "") +
				(move.status == Board.GameStatus.Checkmate ? "++" : "")));
			
			return this;
		}
		
		@Override public void setText(final String text) {
			super.setText("<html>" + text + "</html>");
		}
		
		private static String html(final Font font, final String text) {
			return "<span style=\"font-family:" +
				font.getName() +
				";font-size:" +
				Math.round(1.1f * font.getSize2D()) +
				"pt;\">" +
				text +
				"</span>";
		}
		
		private static String file(final int x) {
			return String.valueOf((char)('a' + x));
		}
		
		private static String rank(final int y) {
			return String.valueOf((char)('1' + y));
		}
	}
}
