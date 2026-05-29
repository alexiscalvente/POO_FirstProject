package mi.irgarten;

import java.util.ArrayList;

/**
 * Board model: cell matrix + parallel matrices with players and monsters by
 * position. It knows the topology (walls, exit) and the movement rules.
 *
 * Design:
 *  - Three parallel matrices: {@code labyrinth} holds the visible character of the cell
 *    (wall X, empty -, monster M, combat C, exit E, digit = player), while
 *    {@code monsters} and {@code players} hold references to the objects at their
 *    coordinates. Keeping them separate allows a fast grid toString without
 *    traversing lists, and O(1) per-cell queries.
 *  - Methods {@code posOK}, {@code emptyPos}, {@code canStepOn} are private helpers:
 *    they encapsulate validity rules so the rest of the class and the outside do
 *    not need to know the concrete map characters.
 *
 * OOP concepts illustrated:
 *  - Encapsulation of the representation (matrices are private)
 *  - Abstraction: the outside asks via setPos/putPlayer/validMoves, it does not touch the matrix
 *  - High cohesion: everything related to "the board" lives here
 */
public class Labyrinth {
    // Characters painted in each cell. Private and final: nobody should touch them.
    private static final char BLOCK_CHAR = 'X';
    private static final char EMPTY_CHAR = '-';
    private static final char MONSTER_CHAR = 'M';
    private static final char COMBAT_CHAR = 'C';   // monster + player on the same cell
    private static final char EXIT_CHAR = 'E';
    private static final int ROW = 0;              // semantic indices for the [row, col] array
    private static final int COL = 1;

    private int nRows;
    private int nCols;
    private int exitRow;                    // exit coordinates (set on construction)
    private int exitCol;
    private char[][] labyrinth;             // visible matrix
    private Monster[][] monsters;           // reference to the monster on each cell (or null)
    private Player[][] players;             // reference to the player on each cell (or null)

    /**
     * Builds an empty labyrinth with the exit already marked.
     * Walls and monsters are added afterwards by {@link Game#configureLabyrinth()}.
     */
    public Labyrinth(int nRows, int nCols, int exitRow, int exitCol) {
        this.nRows = nRows;
        this.nCols = nCols;
        this.exitRow = exitRow;
        this.exitCol = exitCol;

        labyrinth = new char[nRows][nCols];
        monsters = new Monster[nRows][nCols];
        players = new Player[nRows][nCols];

        // Initializes all cells as empty.
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                labyrinth[i][j] = EMPTY_CHAR;
            }
        }

        labyrinth[exitRow][exitCol] = EXIT_CHAR; // marks the exit
    }

    /** There is a winner when a Player stands on the exit cell. */
    public boolean haveAWinner() {
        return players[exitRow][exitCol] != null;
    }

    public char[][] getMatrix() {
        return labyrinth;
    }

    public Player[][] getPlayers() {
        return players;
    }

    public Monster[][] getMonsters() {
        return monsters;
    }

    public int getNumRows() {
        return nRows;
    }

    public int getNumCols() {
        return nCols;
    }

    /**
     * Textual grid representation of the board with numeric headers.
     * Used by the console mode and by GameState for the UI.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Header with column numbers
        sb.append("\t");
        for (int i = 1; i <= nCols; i++) {
            sb.append(i).append("\t");
        }
        sb.append("\n");

        for (int i = 0; i < nRows; i++) {
            sb.append(i + 1).append("\t");
            for (int j = 0; j < nCols; j++) {
                sb.append(labyrinth[i][j]).append("\t");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Places a monster at (row, col) if the cell is valid and empty.
     * Updates the visible matrix and the monsters matrix in parallel.
     */
    public void addMonster(int row, int col, Monster monster) {
        if (posOK(row, col) && emptyPos(row, col)) {
            labyrinth[row][col] = MONSTER_CHAR;
            monsters[row][col] = monster;
            monster.setPos(row, col);
        }
    }

    // ---- Validation helpers. All private: they encapsulate internal rules. ----

    private boolean posOK(int row, int col) {
        return row >= 0 && row < nRows && col >= 0 && col < nCols;
    }

    private boolean emptyPos(int row, int col) {
        return posOK(row, col) && labyrinth[row][col] == EMPTY_CHAR;
    }

    private boolean monsterPos(int row, int col) {
        return posOK(row, col) && labyrinth[row][col] == MONSTER_CHAR;
    }

    private boolean combatPos(int row, int col) {
        return posOK(row, col) && labyrinth[row][col] == COMBAT_CHAR;
    }

    private boolean exitCoordinates(int row, int col) {
        return row == exitRow && col == exitCol;
    }

    /** A cell is steppable if it is empty, contains a monster or is the exit. Walls are NOT. */
    private boolean canStepOn(int row, int col) {
        return posOK(row, col)
                && (labyrinth[row][col] == EMPTY_CHAR
                        || labyrinth[row][col] == MONSTER_CHAR
                        || labyrinth[row][col] == EXIT_CHAR);
    }

    /**
     * Restores the visible character of a cell a player has left:
     *  - if it was the exit, it becomes E again
     *  - if there was combat, it becomes monster again
     *  - otherwise, it stays empty
     */
    private void updateOldPos(int row, int col) {
        if (!posOK(row, col)) {
            return;
        }

        if (exitCoordinates(row, col)) {
            labyrinth[row][col] = EXIT_CHAR;
        } else if (combatPos(row, col)) {
            labyrinth[row][col] = MONSTER_CHAR;
        } else {
            labyrinth[row][col] = EMPTY_CHAR;
        }
    }

    /**
     * Translates a direction into a destination (row, col) pair. Does not verify validity.
     * It is a private helper; the caller decides what to do with the result.
     */
    private int[] dir2Pos(int row, int col, Directions direction) {
        int newRow = row;
        int newCol = col;

        switch (direction) {
            case UP:
                newRow--;
                break;
            case DOWN:
                newRow++;
                break;
            case LEFT:
                newCol--;
                break;
            case RIGHT:
                newCol++;
                break;
            default:
                break;
        }

        return new int[] { newRow, newCol };
    }

    /**
     * Returns a random empty position. Retries until finding one.
     * Used to place walls, monsters and initial players.
     */
    public int[] randomEmptyPos() {
        int[] sol = new int[2];
        int fila = Dice.randomPos(nRows);
        int col = Dice.randomPos(nCols);

        while (!emptyPos(fila, col)) {
            fila = Dice.randomPos(nRows);
            col = Dice.randomPos(nCols);
        }

        sol[ROW] = fila;
        sol[COL] = col;
        return sol;
    }

    /**
     * Directions actually possible from (row, col): each steppable neighbor contributes
     * its Direction to the list. It is the input that {@link Player#move} and
     * {@link Dice#nextStep} use to decide the effective movement.
     */
    public ArrayList<Directions> validMoves(int row, int col) {
        ArrayList<Directions> directions = new ArrayList<>();

        if (canStepOn(row + 1, col)) {
            directions.add(Directions.DOWN);
        }
        if (canStepOn(row - 1, col)) {
            directions.add(Directions.UP);
        }
        if (canStepOn(row, col + 1)) {
            directions.add(Directions.RIGHT);
        }
        if (canStepOn(row, col - 1)) {
            directions.add(Directions.LEFT);
        }

        return directions;
    }

    /**
     * Distributes the list of players over random empty cells at the start of the game.
     * Polymorphism: the list contains Players (it may also contain Fuzzy/Super after a
     * later resurrection), but here the common type is enough.
     */
    public void spreadPlayers(ArrayList<Player> playersList) {
        for (Player player : playersList) {
            int[] pos = randomEmptyPos();
            putPlayer2D(-1, -1, pos[ROW], pos[COL], player);
        }
    }

    /**
     * Places a player at (row, col) freeing its previous cell (if it had one).
     * If the destination cell contains a monster, it marks COMBAT and returns the monster
     * so Game can process the combat. Otherwise, it paints the player's number.
     *
     * @return the monster found at the destination (if any), or null
     */
    private Monster putPlayer2D(int oldRow, int oldCol, int row, int col, Player player) {
        Monster output = null;

        if (!canStepOn(row, col)) {
            return null; // blocked movement: nothing is done
        }

        // Free the previous cell ONLY if this same player was still there
        // (defense against resurrections that have changed the reference).
        if (posOK(oldRow, oldCol)) {
            Player previous = players[oldRow][oldCol];
            if (previous == player) {
                updateOldPos(oldRow, oldCol);
                players[oldRow][oldCol] = null;
            }
        }

        if (monsterPos(row, col)) {
            labyrinth[row][col] = COMBAT_CHAR;
            output = monsters[row][col];
        } else {
            labyrinth[row][col] = player.getNumber();
        }

        player.setPos(row, col);
        players[row][col] = player;

        return output;
    }

    /**
     * Moves a player in a direction. Public facade that relies on putPlayer2D.
     * @return the monster found at the destination (if any), or null
     */
    public Monster putPlayer(Directions direction, Player player) {
        int oldRow = player.getRow();
        int oldCol = player.getCol();

        int[] newPos = dir2Pos(oldRow, oldCol, direction);
        return putPlayer2D(oldRow, oldCol, newPos[ROW], newPos[COL], player);
    }

    /**
     * Applies the result of a combat on the cell:
     *  - If the player wins: erases the monster and the cell ends up with the player's number.
     *  - If the monster wins: the player disappears from the matrix and the cell becomes M again.
     *
     * Important: with this call Game finishes synchronizing state after combat.
     */
    public void resolveCombat(int row, int col, boolean playerWins, Player player) {
        if (!posOK(row, col)) {
            return;
        }

        if (playerWins) {
            monsters[row][col] = null;
            players[row][col] = player;
            labyrinth[row][col] = player.getNumber();
        } else {
            players[row][col] = null;
            labyrinth[row][col] = MONSTER_CHAR;
        }
    }

    /**
     * Paints a wall of length {@code length} starting at (startRow, startCol) in the
     * requested orientation. Stops on edge, non-empty cell or when length is exhausted.
     */
    public void addBlock(Orientation orientation, int startRow, int startCol, int length) {
        int incRow = 0;
        int incCol = 0;

        if (orientation == Orientation.VERTICAL) {
            incRow = 1;
        } else if (orientation == Orientation.HORIZONTAL) {
            incCol = 1;
        }

        int row = startRow;
        int col = startCol;

        while (posOK(row, col) && emptyPos(row, col) && length > 0) {
            labyrinth[row][col] = BLOCK_CHAR;
            length--;
            row += incRow;
            col += incCol;
        }
    }

    /**
     * Replaces the reference in the players matrix with the new instance
     * (typically a Fuzzy/Super just created after resurrection). Key piece for
     * the labyrinth to see the "new" player without repositioning it.
     */
    public void actualicePlayer(Player other) {
        if (posOK(other.getRow(), other.getCol())) {
            players[other.getRow()][other.getCol()] = other;
            labyrinth[other.getRow()][other.getCol()] = other.getNumber();
        }
    }
}
