**Software:** _pmChess_ (poor man's chess)

**Public _Git_ repository:** https://github.com/christoff-buerger/pmChess

**Author:** Christoff Bürger (`christoff.buerger@gmail.com`)

# Introduction

_pmChess_ is a poor man's chess. It started as student project to learn basic artificial intelligence game-playing concepts. To that end, _pmChess_ is nicely modularized, with clean interfaces for user interaction, chessboard representation, movement analysis and search strategy. It uses a common min-max search with alpha-beta pruning in combination with an intuitive scoring-heuristic for figure-constellations. The simple setup eases understanding and the integration of own scoring-heuristics. All in all, _pmChess_ provides a good starting point for wanna-be chess developers.

Of course, _pmChess_ cannot compete with professional chess programs; after all, it's just a poor man's chess. But it is a complete chess program incorporating all rules wrapped in a convenient user interface.

# Rule limitations

_pmChess_ implements the official chess rules of the _International Chess Federation_ ([FIDE](https://www.fide.com/), _Fédération Internationale des Échecs_) -- the [_FIDE Laws of Chess taking effect from 1 January 2023_](https://handbook.fide.com/chapter/E012023) -- except for the following rules:
 1. **Dead position:** It is not checked if the game is a draw because neither player can mate the opponent’s king with any series of legal moves.
 2. **Chessclock:** Any kind of player clocks limiting decision time and/or game length are not supported.

# Graphical user interface

![pmChess screenshot](releases/version-2.0.0/screenshot-normal-mode-macOS.png)

Figures are moved on the _Chessboard_ using the keyboard or mouse. When using the keyboard, a blue rectangle marks the cursor position. A red rectangle marks the currently selected figure to move. The arrow keys are used to move the cursor. Space is used to select a figure or move the currently selected figure to the cursor's position. When using the mouse, first left-click the figure to move; then left-click the tile to move to. Of course, the respective move must be allowed (otherwise it is just ignored). The last move is highlighted by drawing the involved chessboard tiles green, or red in case of a capture.

The _Game status_ tab summarizes the current player (background color of the status message), turn number, game status (checkmate, check, draw, stalemate, resign or normal move), castling possibilities considering previous moves (checkboxes for queenside and kingside castling of each player) and how close the game is to a claimable or automatic draw. The castling summary shows only whether queenside or kingside castling are impossible due to previous king or rook movements; if checked, previous movements are not prohibiting castling. The promotion-list is used to select the figure to promote to in case of pawn promotion. The _Claim draw_ button enables to claim a draw (either because the threefold repetition or 50-move rule are satisfied at the beginning of a player's turn or because they will be with his move). A bulb icon at the end of the status message signals that the computer is busy deciding its move.

The _Game history_ to the right can be used to undo moves. Its history-list is written according to the algebraic notation specified by FIDE (cf. the [_FIDE Laws of Chess taking effect from 1 January 2023_](https://handbook.fide.com/chapter/E012023), _Appendix C. Algebraic notation_). To reset the game to a previous position, the game first has to be paused using the _Pause_ button. Thereafter, the _Undo_ and _Redo_ buttons and the history-list are active. While in pause mode, moves can be undone and redone as pleased; the computer is not active. Only when pressing the _Pause_ button again, the game will resume from the current position. The moves that will be removed when resuming the game are highlighted red in the history-list. It is possible to switch directly to a certain position in the history-list instead of many undo/redo actions. To do so, select a move in the history-list using the arrow up and down keys or mouse and press space. The game will be reset to the state resulting after executing the selected move. Moves yielding a repetition -- a game position already encountered -- are highlighted with a gray background in the history-list.

The _Settings_ tab can be used to scale the whole user interface. Scaling means to increase or decrease the default size by a selected percentage. To change the scale, enter the desired new size in the spinner field of the _Scale_ group. The field highlights invalid values via a red background; select the _Apply_ button to confirm any valid value. Further dialogs will explain how to proceed (if a program restart is required etc). Don't be afraid of wrong scales causing pmChess to not fit your primary screen; pmChess checks if a scale will not work and provide reasonable fallbacks.

The tab key is used to switch between _Chessboard_, _Game history_ and the active elements of the _Game status_ and _Settings_ tabs, like promotion-list, _Claim draw_ button or _Scale_ settings; the space key can be used to select or deselect items or press buttons and the arrow keys to navigate between tabs or within lists like the history- or promotion-list.

# Releases

Official releases of _pmChess_ are available in version-numbered subdirectories within the `releases` directory. Platform independent distributions are provided in respective `portable-jar` subdirectories; they require an installed runtime environment of the _Java Platform, Standard Edition_. Alternatively, native, self-contained distributions for _macOS_ and _Microsoft Windows_ are provided in `macOS` and `Windows` subdirectories.

# License

This program and the accompanying materials are made available under the terms of the MIT license (X11 license) which accompanies this distribution (cf. `license.txt`).

# Comments

For any questions or comments don't hesitate to write me an e-mail (`christoff.buerger@gmail.com`). I appreciate any feedback.
