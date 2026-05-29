package mi.irgarten;

/**
 * Immutable snapshot of the game state at an instant. It is the "contract" between
 * the {@link Game} logic and the presentation layer ({@link MainGame}).
 *
 * CANONICAL example of immutability:
 *  - ALL attributes are {@code private final}
 *  - Only getters, no setters
 *  - After construction, no object can modify its state
 *
 * Why? So the UI can read a consistent snapshot without fearing that the game
 * logic mutates it mid-render. It is a local version of the "Value Object" /
 * Data Transfer Object pattern: it carries data, not behavior.
 *
 * OOP concepts illustrated:
 *  - Immutability (private final + only getters)
 *  - Total encapsulation
 *  - Clear model ↔ view separation (the UI consumes these snapshots)
 */
public class GameState {
    private final String labyrinth;     // textual representation of the board
    private final String players;       // players description
    private final String monsters;      // monsters description
    private final int currentPlayer;    // index of the player in turn
    private final boolean winner;       // has the game ended with a winner?
    private final String log;           // message of the last event

    /**
     * Constructor that sets the snapshot. After this the object is frozen.
     */
    public GameState(String labyrinth, String players, String monsters, int currentPlayer, boolean winner, String log) {
        this.labyrinth = labyrinth;
        this.players = players;
        this.monsters = monsters;
        this.currentPlayer = currentPlayer;
        this.winner = winner;
        this.log = log;
    }

    public String getLabyrinth() {
        return labyrinth;
    }

    public String getPlayers() {
        return players;
    }

    public String getMonsters() {
        return monsters;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean getWinner() {
        return winner;
    }

    public String getLog() {
        return log;
    }
}
