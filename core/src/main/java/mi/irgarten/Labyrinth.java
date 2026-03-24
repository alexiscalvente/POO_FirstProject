package mi.irgarten;

import java.util.ArrayList;

public class Labyrinth {
    private static final char BLOCK_CHAR = 'X';
    private static final char EMPTY_CHAR = '-';
    private static final char MONSTER_CHAR = 'M';
    private static final char COMBAT_CHAR = 'C';
    private static final char EXIT_CHAR = 'E';
    private static final int ROW = 0;
    private static final int COL = 1;

    private int nRows;
    private int nCols;
    private int exitRow;
    private int exitCol;
    private char[][] labyrinth;
    private Monster[][] monsters;
    private Player[][] players;

    public Labyrinth(int nRows, int nCols, int exitRow, int exitCol) {
        this.nRows = nRows;
        this.nCols = nCols;
        this.exitRow = exitRow;
        this.exitCol = exitCol;

        labyrinth = new char[nRows][nCols];
        monsters = new Monster[nRows][nCols];
        players = new Player[nRows][nCols];

        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                labyrinth[i][j] = EMPTY_CHAR;
            }
        }

        labyrinth[exitRow][exitCol] = EXIT_CHAR;
    }

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

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

    public void addMonster(int row, int col, Monster monster) {
        if (posOK(row, col) && emptyPos(row, col)) {
            labyrinth[row][col] = MONSTER_CHAR;
            monsters[row][col] = monster;
            monster.setPos(row, col);
        }
    }

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

    private boolean canStepOn(int row, int col) {
        return posOK(row, col)
                && (labyrinth[row][col] == EMPTY_CHAR
                        || labyrinth[row][col] == MONSTER_CHAR
                        || labyrinth[row][col] == EXIT_CHAR);
    }

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

    public void spreadPlayers(ArrayList<Player> playersList) {
        for (Player player : playersList) {
            int[] pos = randomEmptyPos();
            putPlayer2D(-1, -1, pos[ROW], pos[COL], player);
        }
    }

    private Monster putPlayer2D(int oldRow, int oldCol, int row, int col, Player player) {
        Monster output = null;

        if (!canStepOn(row, col)) {
            return null;
        }

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

    public Monster putPlayer(Directions direction, Player player) {
        int oldRow = player.getRow();
        int oldCol = player.getCol();

        int[] newPos = dir2Pos(oldRow, oldCol, direction);
        return putPlayer2D(oldRow, oldCol, newPos[ROW], newPos[COL], player);
    }

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

    public void actualicePlayer(Player other) {
        if (posOK(other.getRow(), other.getCol())) {
            players[other.getRow()][other.getCol()] = other;
            labyrinth[other.getRow()][other.getCol()] = other.getNumber();
        }
    }
}