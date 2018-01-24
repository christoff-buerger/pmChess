**Software:** _pmChess_ (a poor man's chess)

**Public Git Repository:** https://github.com/christoff-buerger/pmChess

**Author:** Christoff Bürger (`christoff.buerger@gmail.com`)

# Introduction

_pmChess_ is a poor man's chess. It started as student project to learn basic artificial intelligence game-playing concepts. To that end, _pmChess_ is nicely modularised, with clean interfaces for user interaction, chessboard representation, movement analysis and search strategy. It uses a common min-max search with alpha-beta pruning in combination with an intuitive scoring-heuristic for figure-constellations. The simple setup eases understanding and the integration of own scoring-heuristics. All in all, _pmChess_ provides a good starting point for wanna-be chess developers.

Of course, _pmChess_ cannot compete with professional chess programs; after all, it's just a poor man's chess.

# Rule limitations

_pmChess_ implements the official rules of _FIDE_ (_Fédération Internationale des Échecs_) except for en passant moves and the following draw rules:
 1. **Threefold repetition** The same position occurs three times with the same player to move.
 2. **Fifty-moves rule** The last fifty successive moves made by both players contain no capture or pawn move.
 3. **Insufficient material** Neither player has a theoretical possibility to checkmate the opponent.

# Graphical user interface

![pmChess screenshot](releases/version-1.0.0/screenshot.png)

Figures are moved using the keyboard. A blue rectangle marks the cursor position; a red rectangle marks the currently selected figure to move. The arrow keys are used to move the cursor. Space is used to select a figure or to move the currently selected figure to the cursor's position if the respective move is allowed. The last move is highlighted by drawing the involved chessboard tiles green.

The _Game status_ section summarises the current player (background color of the status message), turn number, game status (checkmate, check, stalemate, capitulation or normal move) and castling possibilities considering previous moves (checkboxes for left and right castling of each player). The castling summary shows only whether left or right castling are impossible due to previous king or rook movements; if checked, previous movements are not prohibiting castling. A bulb icon at the end of the status message signals that the computer is busy deciding its move.

# Offical, stable releases

Releases of _pmChess_ are available in version-numbered subdirectories of `releases`. Platform independent distributions are provided in respective `portable-jar` subdirectories; they require an installed runtime environment of the _Java Platform, Standard Edition_ of at least version 9 (_Java SE 9_). Native, self-contained distributions for _macOS_ are provided in `macOS` subdirectories.

# License

This program and the accompanying materials are made available under the terms of the MIT license (X11 license) which accompanies this distribution (cf. `license.txt`).

# Comments

For any questions or comments don't hesitate to write me an e-mail (`christoff.buerger@gmail.com`). I appreciate any feedback.
