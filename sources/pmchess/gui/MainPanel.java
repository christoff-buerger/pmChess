/*
	This program and the accompanying materials are made available under the
	terms of the MIT license (X11 license) which accompanies this distribution.
	
	Author: Christoff Bürger
*/

package pmchess.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

import javax.swing.*;

import pmchess.logic.*;

import pmchess.gui.Resources.*;

public final class MainPanel extends JPanel
{
	private final int text_height =
		(new FontMetrics(Resources.font_regular) {}).getHeight();
	private final int border_size =
		(int)Math.ceil(2 * text_height / 3.0f);
	
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
	private final GamePanel game_panel = new GamePanel();
	private final HistoryPanel history_panel = new HistoryPanel();
	
	protected MainPanel()
	{
		// Setup panel size and layout:
		setOpaque(true);
		final var panel_dimension = new Dimension(
			board_panel.panel_size + history_panel.panel_x_size + 2 * border_size,
			board_panel.panel_size + game_panel.panel_y_size + 2 * border_size);
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
			board_panel.panel_size + game_panel.panel_y_size);
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
		addKeyListener(new BoardListener());
		
		// Initialize and start game:
		initialize(false, false);
	}
	
	@Override public void paintComponent(final Graphics graphics)
	{
		super.paintComponent(graphics);
		Resources.configure_rendering(graphics);
	}
	
	protected void initialize(final boolean computer_w, final boolean computer_b)
	{
		while (undo())
		{
		}
		this.computer_w = computer_w;
		this.computer_b = computer_b;
		run_game();
	}
	
	private boolean undo()
	{
		if (board.undo() != 0)
		{
			history_panel.history_data.removeElementAt(
				history_panel.history_data.size() - 1);
			selected_figure = null;
			capitulation = false;
			board_panel.repaint();
			game_panel.repaint();
			return true;
		}
		return false;
	}
	
	private void run_game()
	{
		var game_status = board.status();
		while (computer_turn()
			&& (game_status == Board.GameStatus.Normal
				|| game_status == Board.GameStatus.Check))
		{
			paintImmediately(0, 0, getWidth(), getHeight());
			final var move = search.select_move(board, evaluator);
			capitulation = move == 0;
			if (capitulation)
			{
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
		game_panel.repaint();
	}
	
	private boolean computer_turn()
	{
		return board.player() ? computer_w : computer_b;
	}
	
	private final class BoardListener extends KeyAdapter
	{
		@Override public void keyPressed(final KeyEvent event)
		{
			if (computer_turn())
			{
				return;
			}
			final var old_x = cursor_x;
			final var old_y = cursor_y;
			final var key = event.getKeyCode();
			if (key == KeyEvent.VK_SPACE)
			{
				final var figure = board.figure(cursor_x, cursor_y);
				if (figure != null && figure.owner == board.player())
				{
					selected_figure = null;
					board_panel.draw_square(selected_x, selected_y);
					selected_figure = figure;
					selected_x = cursor_x;
					selected_y = cursor_y;
					board_panel.draw_square(selected_x, selected_y);
				}
				else if (selected_figure != null)
				{
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
			{ // Unknown key or invalid movement:
				return;
			}
			board_panel.draw_square(old_x, old_y);
			board_panel.draw_square(cursor_x, cursor_y);
		}
	}
	
	private final class HistoryListener extends KeyAdapter
	{
		@Override public void keyPressed(final KeyEvent event)
		{
			final var game_status = board.status();
			if (computer_turn() && !capitulation
				&& game_status != Board.GameStatus.Checkmate
				&& game_status != Board.GameStatus.Stalemate)
			{
				return;
			}
			final var key = event.getKeyCode();
			if (key == KeyEvent.VK_SPACE)
			{
				final var selected = history_panel.history_list.getSelectedIndex();
				for (var i = board.turn() - selected - 1; i > 0; i--)
				{
					undo();
				}
				run_game();
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
		
		private BoardPanel()
		{
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
		
		@Override public void paintComponent(final Graphics graphic)
		{
			super.paintComponent(graphic);
			Resources.configure_rendering(graphic);
			
			// Draw horizontal (h) and vertical (v) chessboard markings:
			graphic.setFont(chessboard_marking_font);
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
			for (var i = 0; i < 8; i++)
			{
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
			for (var x = 7; x >= 0; x--)
			{
				for (var y = 7; y >= 0; y--)
				{
					draw_square(graphic, x, y);
				}
			}
		}
		
		private void draw_square(final int x, final int y)
		{
			draw_square(getGraphics(), x, y);
		}
		
		private void draw_square(final Graphics graphic, final int x, final int y)
		{
			Resources.configure_rendering(graphic);
			
			final var y_trans = 7 - y;
			
			// Draw background tile:
			var color = ((x + y_trans) % 2) == 0 ? Color.white : Color.lightGray;
			final var last_move = board.previous_move(board.turn() - 1);
			if (last_move != 0)
			{
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
			if (figure != null)
			{
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
			if (x == cursor_x && y == cursor_y)
			{
				graphic.setColor(Color.blue);
				graphic.drawRect(x_pos, y_pos, distance, distance);
			}
			if (selected_figure != null && x == selected_x && y == selected_y)
			{
				graphic.setColor(Color.red);
				graphic.drawRect(x_pos, y_pos, distance, distance);
			}
			((Graphics2D)graphic).setStroke(old_stroke);
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
			(int)(2 * text_height + 3.5f * border_size);
		private final int castlings_y_size =
			castling_y_size;
		private final int pawn_promotion_y_size =
			4 * pawn_promotion_text_heigth
			+ 2 * border_size;
		private final int pawn_promotion_list_y_size =
			pawn_promotion_y_size;
		private final int tab_y_size =
			Math.max(status_y_size + castling_y_size, pawn_promotion_y_size)
			+ 2 * border_size;
		private final int tabs_y_size =
			tab_y_size + text_height + border_size;
		private final int panel_y_size =
			tabs_y_size + (int)Math.ceil(0.5f * border_size);
		
		private GamePanel()
		{
			// Setup panel size and layout:
			setOpaque(true);
			final var panel_dimension = new Dimension(panel_x_size, panel_y_size);
			setMaximumSize(panel_dimension);
			setMinimumSize(panel_dimension);
			setPreferredSize(panel_dimension);
			
			// Compose tabs:
			final var tabs = new JTabbedPane();
			final var tabs_dimension = new Dimension(tabs_x_size, tabs_y_size);
			tabs.setMaximumSize(tabs_dimension);
			tabs.setMinimumSize(tabs_dimension);
			tabs.setPreferredSize(tabs_dimension);
			tabs.addTab("Game status", new StatusPanel());
			tabs.setSelectedIndex(0);
			add(tabs);
		}
	
		private abstract class GamePanelTab extends JPanel
		{
			private GamePanelTab()
			{
				setOpaque(true);
				final var tab_dimension = new Dimension(tab_x_size, tab_y_size);
				setMaximumSize(tab_dimension);
				setMinimumSize(tab_dimension);
				setPreferredSize(tab_dimension);
			}
		}
		
		private final class StatusPanel extends GamePanelTab
		{
			private final JLabel status = new JLabel()
				{
					@Override public void paintComponent(final Graphics graphic)
					{
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
			
			private final DefaultListModel<FigurePresentation> pawn_promotion_w =
				new DefaultListModel<>();
			private final DefaultListModel<FigurePresentation> pawn_promotion_b =
				new DefaultListModel<>();
			private final JList<FigurePresentation> pawn_promotion_list = new JList<>();
			
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
				final var castling_b = new JPanel();
				castling_b.setBorder(BorderFactory.createTitledBorder("Black castling"));
				castling_b.setMaximumSize(castling_dimension);
				castling_b.setMinimumSize(castling_dimension);
				castling_b.setPreferredSize(castling_dimension);
				castling_b.add(castling_l_b);
				castling_b.add(castling_r_b);
				
				// Pawn promotion selection:
				pawn_promotion_w.addElement(FigurePresentation.get(Figure.queen(true)));
				pawn_promotion_w.addElement(FigurePresentation.get(Figure.knight(true)));
				pawn_promotion_w.addElement(FigurePresentation.get(Figure.bishop(true)));
				pawn_promotion_w.addElement(FigurePresentation.get(Figure.rook(true)));
				pawn_promotion_b.addElement(FigurePresentation.get(Figure.queen(false)));
				pawn_promotion_b.addElement(FigurePresentation.get(Figure.knight(false)));
				pawn_promotion_b.addElement(FigurePresentation.get(Figure.bishop(false)));
				pawn_promotion_b.addElement(FigurePresentation.get(Figure.rook(false)));
				final var pawn_promotion_list_dimensions = new Dimension(
					pawn_promotion_list_x_size,
					pawn_promotion_list_y_size);
				pawn_promotion_list.setBorder(BorderFactory.createLoweredBevelBorder());
				pawn_promotion_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				pawn_promotion_list.setLayoutOrientation(JList.VERTICAL);
				pawn_promotion_list.setVisibleRowCount(pawn_promotion_w.getSize());
				pawn_promotion_list.setCellRenderer(new DefaultListCellRenderer());
				pawn_promotion_list.setModel(board.player()
					? pawn_promotion_w
					: pawn_promotion_b);
				pawn_promotion_list.setSelectedIndex(0);
				pawn_promotion_list.setMaximumSize(pawn_promotion_list_dimensions);
				pawn_promotion_list.setMinimumSize(pawn_promotion_list_dimensions);
				pawn_promotion_list.setPreferredSize(pawn_promotion_list_dimensions);
				final var pawn_promotion_label = new JLabel("Promotion")
					{
						private boolean painting = false;
						
						@Override public void paintComponent(final Graphics graphics)
						{
							Resources.configure_rendering(graphics);
							final var g2d = (Graphics2D)graphics;
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
							final var i = super.getInsets();
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
							final var size = super.getPreferredSize();
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
				
				final var castlings_panel = new JPanel();
				final var castlings_panel_dimension = new Dimension(
					castlings_x_size,
					castlings_y_size);
				castlings_panel.setMaximumSize(castlings_panel_dimension);
				castlings_panel.setMinimumSize(castlings_panel_dimension);
				castlings_panel.setPreferredSize(castlings_panel_dimension);
				castlings_panel.setLayout(new BoxLayout(castlings_panel, BoxLayout.X_AXIS));
				castlings_panel.add(Box.createHorizontalGlue());
				castlings_panel.add(castling_w);
				castlings_panel.add(Box.createHorizontalGlue());
				castlings_panel.add(castling_b);
				castlings_panel.add(Box.createHorizontalGlue());
				
				final var left_panel = new JPanel();
				final var left_panel_dimension = new Dimension(castlings_x_size, tab_y_size);
				left_panel.setMaximumSize(left_panel_dimension);
				left_panel.setMinimumSize(left_panel_dimension);
				left_panel.setPreferredSize(left_panel_dimension);
				left_panel.setLayout(new BoxLayout(left_panel, BoxLayout.Y_AXIS));
				left_panel.add(Box.createVerticalGlue());
				left_panel.add(status);
				left_panel.add(Box.createVerticalGlue());
				left_panel.add(castlings_panel);
				left_panel.add(Box.createVerticalGlue());
				
				final var right_panel = new JPanel();
				final var right_panel_dimension = new Dimension(
					pawn_promotion_x_size,
					tab_y_size);
				right_panel.setMaximumSize(right_panel_dimension);
				right_panel.setMinimumSize(right_panel_dimension);
				right_panel.setPreferredSize(right_panel_dimension);
				right_panel.setLayout(new BoxLayout(right_panel, BoxLayout.X_AXIS));
				right_panel.add(Box.createHorizontalGlue());
				right_panel.add(pawn_promotion_list);
				right_panel.add(pawn_promotion_label);
				right_panel.add(Box.createHorizontalGlue());
				
				setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
				add(Box.createHorizontalGlue());
				add(left_panel);
				add(Box.createHorizontalGlue());
				add(right_panel);
				add(Box.createHorizontalGlue());
			}
			
			@Override public void paintComponent(final Graphics graphic)
			{
				super.paintComponent(graphic);
				Resources.configure_rendering(graphic);
				
				// Update status message:
				final var now = board.player() ? "White" : "Black";
				final var next = board.player() ? "Black" : "White";
				final var game_status = board.status();
				final String message;
				if (capitulation)
				{
					message = now + " capitulates. " + next + " wins.";
				}
				else
				{
					switch (game_status)
					{
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
				
				// Update pawn promotion selector list:
				final var current_selection = pawn_promotion_list.getSelectedIndex();
				pawn_promotion_list.setFont(pawn_promotion_font);
				pawn_promotion_list.setModel(board.player()
					? pawn_promotion_w
					: pawn_promotion_b);
				pawn_promotion_list.setSelectedIndex(current_selection);
			}
		}
	}
	
	private final class HistoryPanel extends JPanel
	{
		private final int panel_x_size =
			(int)Math.ceil(
				1.6f * Resources.font_italic.getStringBounds(
					"initial position",
					new FontRenderContext(new AffineTransform(), true, true))
				.getWidth());
		private final int panel_y_size =
			board_panel.panel_size + game_panel.panel_y_size;
		
		private final DefaultListModel<PastMove> history_data = new DefaultListModel<>();
		private final JList<PastMove> history_list = new JList<>(history_data);
		
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
			
			// History list:
			history_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			history_list.setLayoutOrientation(JList.VERTICAL);
			final var history_scroll_pane = new JScrollPane(history_list);
			history_scroll_pane.setVerticalScrollBarPolicy(
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			final var history_scroll_pane_dimension = new Dimension(
				panel_x_size - (int)Math.ceil(1.5f * border_size),
				panel_y_size - (int)Math.ceil(3.5f * border_size));
			history_scroll_pane.setMaximumSize(history_scroll_pane_dimension);
			history_scroll_pane.setMinimumSize(history_scroll_pane_dimension);
			history_scroll_pane.setPreferredSize(history_scroll_pane_dimension);
			history_list.addKeyListener(new HistoryListener());
			history_list.setCellRenderer(new HistoryRenderer());
			history_list.setSelectedIndex(0);
			history_data.addElement(new PastMove(0, 0, Board.GameStatus.Normal));
			add(history_scroll_pane);
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
		
		private PastMove(final int turn, final int move, final Board.GameStatus status)
		{
			this.turn = turn;
			this.move = move;
			this.status = status;
		}
	}
	
	private static final class HistoryRenderer extends DefaultListCellRenderer
	{
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
			if (move.move == 0)
			{
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
			setText(Board.move(move.turn)
				+ (move.turn % 2 == 0 ? "\u2026 " : ". ")
				+ notation
				+ (move.status == Board.GameStatus.Check ? info("+") : "")
				+ (move.status == Board.GameStatus.Checkmate ? info("++") : ""));
			
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
