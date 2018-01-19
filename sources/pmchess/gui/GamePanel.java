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

public final class GamePanel extends JPanel {
	private static final Image pawn_w = GUI.loadImage("figures/pawn-w.png");
	private static final Image pawn_b = GUI.loadImage("figures/pawn-b.png");
	private static final Image knight_w = GUI.loadImage("figures/knight-w.png");
	private static final Image knight_b = GUI.loadImage("figures/knight-b.png");
	private static final Image bishop_w = GUI.loadImage("figures/bishop-w.png");
	private static final Image bishop_b = GUI.loadImage("figures/bishop-b.png");
	private static final Image rook_w = GUI.loadImage("figures/rook-w.png");
	private static final Image rook_b = GUI.loadImage("figures/rook-b.png");
	private static final Image queen_w = GUI.loadImage("figures/queen-w.png");
	private static final Image queen_b = GUI.loadImage("figures/queen-b.png");
	private static final Image king_w = GUI.loadImage("figures/king-w.png");
	private static final Image king_b = GUI.loadImage("figures/king-b.png");
	private static final Image bulb = GUI.loadImage("icons/bulb.png");
	
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
	
	protected GamePanel() {
		setOpaque(true);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(boardPanel);
		add(statusPanel);
		
		// Add listener for cursor movement and figure selection:
		setFocusable(true);
      		requestFocusInWindow();
		addKeyListener(new CursorListener());
		
		// Initialize and start game:
		initialize(false, false);
	}
	
	public void initialize(final boolean computer_w, final boolean computer_b) {
		while (board.undo() != 0);
		this.computer_w = computer_w;
		this.computer_b = computer_b;
		selected_figure = null;
		capitulation = false;
		if (getGraphics() != null)
			boardPanel.drawBoard();
		statusPanel.drawStatus();
		runGame();
	}
	
	private boolean computerTurn() {
		return board.player() ? computer_w : computer_b;
	}
	
	private void execute(final int x, final int y, final int X, final int Y) {
		if (!capitulation && board.execute(x, y, X, Y))
			selected_figure = null;
		boardPanel.drawBoard();
		statusPanel.drawStatus();
	}
	
	public void undo() {
		selected_figure = null;
		capitulation = false;
		board.undo();
		boardPanel.drawBoard();
		statusPanel.drawStatus();
	}
	
	private void runGame() {
		Board.GameStatus gameStatus = board.status();
		while (computerTurn() &&
			!capitulation &&
			(gameStatus == Board.GameStatus.Normal ||
			 gameStatus == Board.GameStatus.Check))
		{
			final int move = search.selectMove(board, evaluator);
			capitulation = move == 0;
			execute(Move.x(move), Move.y(move), Move.X(move), Move.Y(move));
			gameStatus = board.status();
		}
	}
	
	private final class CursorListener extends KeyAdapter {
		public void keyPressed(final KeyEvent e) {
			final int old_x = cursor_x, old_y = cursor_y;
			final int key = e.getKeyCode();
			if (key == KeyEvent.VK_SPACE) {
				if (computerTurn())
					return;
				final Figure figure = board.figure(cursor_x, cursor_y);
				if (figure != null && figure.owner == board.player()) {
					selected_figure = null;
					boardPanel.drawSquare(selected_x, selected_y);
					selected_figure = figure;
					selected_x = cursor_x;
					selected_y = cursor_y;
					boardPanel.drawSquare(selected_x, selected_y);
				} else if (selected_figure != null) {
					execute(selected_x, selected_y, cursor_x, cursor_y);
					runGame();
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
	
	private final class BoardPanel extends JPanel {
		private BoardPanel() {
			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Chessboard"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
			setMaximumSize(new Dimension(310, 330));
			setMinimumSize(new Dimension(310, 330));
			setPreferredSize(new Dimension(310, 330));
		}
		
		public void paint(Graphics graphic) {
			super.paint(graphic);
			drawBoard(graphic);
		}
		
		private void drawBoard() {
			drawBoard(getGraphics());
		}
		
		private void drawBoard(final Graphics graphic) {
			for (int x = 7; x >= 0; x--)
				for (int y = 7; y >= 0; y--)
					drawSquare(graphic, x, y);
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
			graphic.fillRect(x * 36 + 10, y_trans * 36 + 20, 36, 36);
			
			// Draw figure:
			final Figure figure = board.figure(x, y);
			if (figure != null) {
				final Image image;
				if (figure.isPawn()) {
					image = figure.owner ? pawn_w : pawn_b;
				} else if (figure.isKnight()) {
					image = figure.owner ? knight_w : knight_b;
				} else if (figure.isBishop()) {
					image = figure.owner ? bishop_w : bishop_b;
				} else if (figure.isRook()) {
					image = figure.owner ? rook_w : rook_b;
				} else if (figure.isQueen()) {
					image = figure.owner ? queen_w : queen_b;
				} else {
					image = figure.owner ? king_w : king_b;
				}
				graphic.drawImage(image, x * 36 + 10, y_trans * 36 + 20, this);
			}
			
			// Draw cursor and figure selection:
			final Stroke oldStroke = ((Graphics2D)graphic).getStroke();
			((Graphics2D)graphic).setStroke(new BasicStroke(3));
			if (x == cursor_x && y == cursor_y) {
				graphic.setColor(Color.blue);
				graphic.drawRect(x * 36 + 11, y_trans * 36 + 21, 33, 33);
			}
			if (selected_figure != null && x == selected_x && y == selected_y) {
				graphic.setColor(Color.red);
				graphic.drawRect(x * 36 + 11, y_trans * 36 + 21, 33, 33);
			}
			((Graphics2D)graphic).setStroke(oldStroke);
		}
	}
	
	private final class StatusPanel extends JPanel {
		private final JLabel status = new JLabel();
		private final JCheckBox castling_l_w = new JCheckBox("left");
		private final JCheckBox castling_r_w = new JCheckBox("right");
		private final JCheckBox castling_l_b = new JCheckBox("left");
		private final JCheckBox castling_r_b = new JCheckBox("right");
		
		private StatusPanel() {
			setBorder(BorderFactory.createTitledBorder("Game status"));
			setMaximumSize(new Dimension(310, 130));
			setMinimumSize(new Dimension(310, 130));
			setPreferredSize(new Dimension(310, 130));
			// Status message box:
			status.setOpaque(true);
			status.setFont(GUI.font_bold);
			status.setMaximumSize(new Dimension(280, 38));
			status.setMinimumSize(new Dimension(280, 38));
			status.setPreferredSize(new Dimension(280, 38));
			add(status);
			// Allowed castlings information boxes:
			castling_l_w.setEnabled(false);
			castling_r_w.setEnabled(false);
			castling_l_b.setEnabled(false);
			castling_r_b.setEnabled(false);
			JPanel panel = new JPanel();
			panel.setBorder(BorderFactory.createTitledBorder("White castling"));
			panel.setMaximumSize(new Dimension(140, 60));
			panel.setMinimumSize(new Dimension(140, 60));
			panel.setPreferredSize(new Dimension(140, 60));
			panel.add(castling_l_w);
			panel.add(castling_r_w);
			add(panel);
			panel = new JPanel();
			panel.setBorder(BorderFactory.createTitledBorder("Black castling"));
			panel.setMaximumSize(new Dimension(140, 60));
			panel.setMinimumSize(new Dimension(140, 60));
			panel.setPreferredSize(new Dimension(140, 60));
			panel.add(castling_l_b);
			panel.add(castling_r_b);
			add(panel);
		}
		
		public void paint(Graphics graphic) {
			super.paint(graphic);
			drawStatus(graphic);
		}
		
		private void drawStatus() {
			drawStatus(getGraphics());
		}
		
		private void drawStatus(final Graphics graphic) {
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
					message = pNow + " in check. " + pNow + "'s move.";
					break;
				case Stalemate:
					message = "Stalemate. " + pNow + " cannot move.";
					break;
				default:
					message = pNow + "'s move.";
				}
			}
			status.setBackground(board.player() ? Color.white : Color.black);
			status.setForeground(board.player() ? Color.black : Color.white);
			status.setText("  " + Integer.toString(board.turn()) + ": " + message);
			status.paint(status.getGraphics());
			// Update if computer is busy deciding move:
			if (computerTurn() &&
				!capitulation &&
				gameStatus != Board.GameStatus.Checkmate &&
				gameStatus != Board.GameStatus.Stalemate)
			{
				graphic.drawImage(bulb, 263, 31, this);
			}
			// Update allowed castlings:
			castling_l_w.setSelected(board.castlingAllowed(true, true));
			castling_r_w.setSelected(board.castlingAllowed(false, true));
			castling_l_b.setSelected(board.castlingAllowed(true, false));
			castling_r_b.setSelected(board.castlingAllowed(false, false));
		}
	}
}
