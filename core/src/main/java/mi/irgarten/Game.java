package mi.irgarten;

import java.util.ArrayList;

/**
 * Game orchestrator: encapsulates rules, turns, combat, resurrection and global state.
 *
 * It is the public face of the model: the UI ({@link MainGame}) only interacts with Game
 * through {@link #nextStep}, {@link #getGameState}, {@link #finished} and debug getters.
 * This demonstrates a clean model ↔ view boundary: MainGame does not touch Labyrinth/Player
 * directly; it goes through Game or through the immutable {@link GameState} snapshots.
 *
 * Composition: Game HAS a Labyrinth, a list of Players and a list of Monsters.
 * It does not inherit from anything in the domain: it aggregates and coordinates.
 *
 * OOP concepts illustrated:
 *  - Composition over inheritance: Game composes Labyrinth, Player[], Monster[]
 *  - Facade / clean public API: nextStep, getGameState, finished
 *  - Dynamic polymorphism in combat() (the project's flagship line)
 *  - Encapsulation: log, indices and internal references are private
 *  - Overloaded constructor + nested enum {@link GameDifficulty}
 *  - Partial immutability: the game dimensions are set with private final
 */
public class Game {
    /**
     * Difficulty levels. Public nested enum: the UI must be able to refer to them so that
     * the user picks. Each value translates into different parameters (size, monsters, walls).
     */
    public enum GameDifficulty {
        EASY,
        NORMAL,
        HARD
    }

    // Cap on rounds per combat: if nobody dies before, it is decided by compared health.
    private static final int MAX_ROUNDS = 10;

    // Labyrinth parameters: final = set in the constructor and unchanged during the game.
    private final int rows;
    private final int cols;
    private final int numMonsters;
    private final int numBlocks;
    private final int blockLength;

    private int currentPlayerIndex;     // index of the player in turn within 'players'
    private StringBuilder log;          // messages from the last step, read by the UI
    private ArrayList<Player> players;  // current players (can be Player/Fuzzy/Super after resurrection)
    private ArrayList<Monster> monsters;// monsters alive in the labyrinth
    private Labyrinth labyrinth;        // composition: the board belongs to the game
    private Player currentPlayer;       // quick reference to the player in turn (= players.get(currentPlayerIndex))

    /**
     * Convenience constructor: NORMAL difficulty by default.
     * Constructor overloading — delegation with {@code this(...)}.
     */
    public Game(int nplayers) {
        this(nplayers, GameDifficulty.NORMAL);
    }

    /**
     * Main constructor. Configures the board according to difficulty, creates players and
     * monsters, randomly chooses who starts and distributes them across the labyrinth.
     */
    public Game(int nplayers, GameDifficulty difficulty) {
        if (nplayers < 1) {
            nplayers = 1; // defensive sanitization: never fewer than one player
        }

        if (difficulty == null) {
            difficulty = GameDifficulty.NORMAL;
        }

        // Parameter selection based on difficulty. Done in a switch so each level is
        // explicit and easy to audit.
        switch (difficulty) {
            case EASY:
                rows = 9;
                cols = 9;
                numMonsters = 5;
                numBlocks = 9;
                blockLength = 2;
                break;
            case HARD:
                rows = 11;
                cols = 11;
                numMonsters = 9;
                numBlocks = 16;
                blockLength = 2;
                break;
            case NORMAL:
            default:
                rows = 10;
                cols = 10;
                numMonsters = 7;
                numBlocks = 12;
                blockLength = 2;
                break;
        }

        players = new ArrayList<>();
        monsters = new ArrayList<>();
        log = new StringBuilder();

        // We create the players with random intelligence/strength (delegated to Dice).
        for (int i = 0; i < nplayers; i++) {
            players.add(new Player((char) ('0' + i), Dice.randomIntelligence(), Dice.randomStrength()));
        }

        currentPlayerIndex = Dice.whoStarts(nplayers);
        currentPlayer = players.get(currentPlayerIndex);

        // The exit is also drawn: every game is different.
        int exitRow = Dice.randomPos(rows);
        int exitCol = Dice.randomPos(cols);
        labyrinth = new Labyrinth(rows, cols, exitRow, exitCol);

        configureLabyrinth();             // places walls and monsters
        labyrinth.spreadPlayers(players); // and finally distributes the players

        log.append("Juego iniciado.");
    }

    public Labyrinth getLabyrinth() {
        return labyrinth;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public String getLastLog() {
        return log.toString();
    }

    /** Has the game ended? Delegation to the Labyrinth: it knows who is at the exit. */
    public boolean finished() {
        return labyrinth.haveAWinner();
    }

    /**
     * Builds an immutable snapshot with the current game state for the UI.
     * Example of a "Data Transfer Object": GameState has no behavior, it only transports data.
     */
    public GameState getGameState() {
        return new GameState(
                labyrinth.toString(),
                players.toString(),
                monsters.toString(),
                currentPlayerIndex,
                finished(),
                log.toString());
    }

    /** Readable dump of a player's state for the debug panel. */
    public String getPlayerDebugString(Player p) {
        if (p == null) {
            return "Sin jugador";
        }

        return "Nombre: " + p.getName()
                + " | Vida: " + p.getHealth()
                + " | Int: " + p.getIntelligence()
                + " | Fue: " + p.getStrength()
                + " | Pos: (" + p.getRow() + "," + p.getCol() + ")"
                + " | Armas: " + p.weaponsInfo()
                + " | Escudos: " + p.shieldsInfo();
    }

    /** Readable dump of alive monsters as a single string. */
    public String getMonstersDebugString() {
        if (monsters == null || monsters.isEmpty()) {
            return "No hay monstruos";
        }

        StringBuilder sb = new StringBuilder();

        for (Monster m : monsters) {
            if (m != null && !m.dead()) {
                sb.append(m.getName())
                        .append(" [V:")
                        .append(m.getHealth())
                        .append(", Int:")
                        .append(m.getIntelligence())
                        .append(", Fue:")
                        .append(m.getStrength())
                        .append(", Pos:")
                        .append(m.getRow())
                        .append(",")
                        .append(m.getCol())
                        .append("]  ");
            }
        }

        if (sb.length() == 0) {
            return "No hay monstruos vivos";
        }

        return sb.toString();
    }

    /**
     * Fills the labyrinth with walls and monsters at the start. Private: initialization detail.
     */
    private void configureLabyrinth() {
        // Places numBlocks walls with random orientation
        for (int i = 0; i < numBlocks; i++) {
            int[] pos = labyrinth.randomEmptyPos();
            Orientation orientation = Dice.randomPos(2) == 0 ? Orientation.HORIZONTAL : Orientation.VERTICAL;
            labyrinth.addBlock(orientation, pos[0], pos[1], blockLength);
        }

        // Places numMonsters monsters with random attributes
        for (int i = 0; i < numMonsters; i++) {
            Monster monster = new Monster("Monstruo " + i, Dice.randomIntelligence(), Dice.randomStrength());
            monsters.add(monster);

            int[] pos = labyrinth.randomEmptyPos();
            labyrinth.addMonster(pos[0], pos[1], monster);
        }
    }

    /** Passes the turn to the next player (circular rotation). */
    private void nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        currentPlayer = players.get(currentPlayerIndex);
    }

    // ---- Centralized log messages. Keeping them as methods avoids repeated strings
    // throughout the code and allows the UI to do stable substring searches. ----

    private void logPlayerWon() {
        log.append(" Has ganado el combate.");
    }

    private void logMonsterWon() {
        log.append(" El monstruo ha ganado el combate.");
    }

    private void logResurrected() {
        log.append(" El jugador ha resucitado.");
    }

    private void logPlayerSkipTurn() {
        log.append(" El jugador estaba muerto y pierde el turno.");
    }

    private void logPlayerNoOrders() {
        log.append(" El jugador no pudo seguir exactamente la dirección pedida.");
    }

    private void logNoMonster() {
        log.append(" Movimiento realizado.");
    }

    private void logBlocked() {
        log.append(" Movimiento bloqueado.");
    }

    private void logRounds(int rounds, int max) {
        log.append(" Rondas de combate: ").append(rounds).append("/").append(max).append(".");
    }

    /**
     * Asks the player which effective direction to take given its preference and the valid moves.
     * Dynamic polymorphism: if the player is a FuzzyPlayer, its move() adds random noise;
     * if it is a regular Player, it simply respects the preference or picks the first valid one.
     */
    private Directions actualDirection(Directions preferredDirection) {
        int currentRow = currentPlayer.getRow();
        int currentCol = currentPlayer.getCol();
        ArrayList<Directions> validMoves = labyrinth.validMoves(currentRow, currentCol);
        return currentPlayer.move(preferredDirection, validMoves);
    }

    /**
     * Resolves a round-by-round combat between the player in turn and a monster.
     *
     * HERE is dynamic polymorphism in its clearest form:
     *   monster.defend(currentPlayer.attack())
     *
     * In that expression:
     *  - currentPlayer.attack() runs Player.attack, FuzzyPlayer.attack or SuperPlayer.attack
     *    depending on the REAL type of the object at runtime (dynamic dispatch).
     *  - monster.defend(...) runs Monster.defend, the only implementation, but
     *    Game calls it through the abstract type without knowing details.
     *
     * If nobody dies in MAX_ROUNDS, whoever has the most health left wins.
     *
     * @return PLAYER if the player wins, MONSTER if the monster wins
     */
    private GameCharacter combat(Monster monster) {
        int rounds = 0;

        while (rounds < MAX_ROUNDS) {
            rounds++;

            // The player attacks first; the monster defends and may die.
            boolean monsterDead = monster.defend(currentPlayer.attack());
            if (monsterDead) {
                logRounds(rounds, MAX_ROUNDS);
                return GameCharacter.PLAYER;
            }

            // If the monster survives, it counter-attacks and the player defends.
            boolean playerDead = currentPlayer.defend(monster.attack());
            if (playerDead) {
                logRounds(rounds, MAX_ROUNDS);
                return GameCharacter.MONSTER;
            }
        }

        logRounds(rounds, MAX_ROUNDS);

        // Tie by time: remaining health decides.
        if (currentPlayer.getHealth() >= monster.getHealth()) {
            return GameCharacter.PLAYER;
        }
        return GameCharacter.MONSTER;
    }

    /**
     * RESURRECTION MECHANIC — flagship piece of the project.
     *
     * When the currentPlayer has died, we decide at random whether it resurrects:
     *  1. {@link Dice#resurrectPlayer()} decides yes/no (coin flip).
     *  2. If YES: {@code currentPlayer.resurrect()} is called to reset its state
     *     (full health, no inventory, no accumulated hits).
     *  3. {@link Dice#fuzzyOrSuper()} decides which SUBCLASS to be reborn as (Fuzzy or Super).
     *  4. A NEW instance of the subclass is built passing the currentPlayer to the
     *     copy constructor. This preserves name, position and board identity but
     *     CHANGES the object's dynamic type — and therefore its behavior in
     *     attack/defend/move (dynamic polymorphism).
     *  5. The reference in players[currentPlayerIndex] and in the labyrinth is replaced
     *     through {@link Labyrinth#actualicePlayer}.
     *  6. If it does NOT resurrect: it is logged that the turn is lost (it stays dead in the array,
     *     waiting for the next opportunity).
     *
     * It is the clearest example of "replacing an object with one of another class while preserving
     * state": the engine through which the copy constructor and polymorphism gain meaning.
     */
    private void manageResurrection() {
        boolean resurrect = Dice.resurrectPlayer();

        if (resurrect) {
            currentPlayer.resurrect();                                  // state reset of the dead player

            // Choice of the concrete REBIRTH SUBCLASS: the copy constructor is used here.
            if (ResurrectedPlayer.FUZZY == Dice.fuzzyOrSuper()) {
                currentPlayer = new FuzzyPlayer(currentPlayer);         // FuzzyPlayer inherits from Player
            } else {
                currentPlayer = new SuperPlayer(currentPlayer);         // SuperPlayer inherits from Player
            }

            // Reference replacement in the players list and in the labyrinth matrix.
            players.set(currentPlayerIndex, currentPlayer);
            labyrinth.actualicePlayer(currentPlayer);
            logResurrected();
        } else {
            logPlayerSkipTurn();
        }
    }

    /**
     * Processes the result of a combat: rewards the player if it won, appropriate log either way.
     */
    private void manageReward(GameCharacter winner) {
        if (winner == GameCharacter.PLAYER) {
            currentPlayer.receiveReward();
            logPlayerWon();
        } else {
            logMonsterWon();
        }
    }

    /**
     * MAIN entry point of the game from the UI: advances one step from the
     * direction requested by the user and returns whether the game has ended.
     *
     * Flow:
     *  1. If the player in turn is dead → manageResurrection and pass turn.
     *  2. If alive → compute effective direction, move, and if it collides with a monster, combat.
     *  3. Apply reward or death over the labyrinth.
     *  4. If the game has not ended, pass turn.
     *
     * @param preferredDirection direction the user wanted to take
     * @return true if the game has ended in this step
     */
    public boolean nextStep(Directions preferredDirection) {
        log.setLength(0); // clear the log: we only keep the last event

        if (currentPlayer.dead()) {
            manageResurrection();
            if (!finished()) {
                nextPlayer();
            }
            return finished();
        }

        Directions direction = actualDirection(preferredDirection);
        if (direction != preferredDirection) {
            logPlayerNoOrders(); // FuzzyPlayer or another cause changed the direction
        }

        int oldRow = currentPlayer.getRow();
        int oldCol = currentPlayer.getCol();

        Monster monster = labyrinth.putPlayer(direction, currentPlayer);

        // If it did not move and there is no monster, the movement was blocked by wall/edge.
        if (currentPlayer.getRow() == oldRow && currentPlayer.getCol() == oldCol && monster == null) {
            logBlocked();
            return finished();
        }

        if (monster == null) {
            logNoMonster();
        } else {
            // Encounter happened: combat and consequences.
            GameCharacter winner = combat(monster);
            manageReward(winner);

            int row = currentPlayer.getRow();
            int col = currentPlayer.getCol();

            if (winner == GameCharacter.PLAYER) {
                labyrinth.resolveCombat(row, col, true, currentPlayer);
                monsters.remove(monster);
            } else {
                labyrinth.resolveCombat(row, col, false, currentPlayer);
            }
        }

        boolean endGame = finished();
        if (!endGame) {
            nextPlayer();
        }

        return endGame;
    }

    /**
     * Dump of alive monsters as a list of independent lines (for the UI panel).
     */
    public ArrayList<String> getMonstersDebugLines() {
        ArrayList<String> lines = new ArrayList<>();

        if (monsters == null || monsters.isEmpty()) {
            lines.add("No hay monstruos");
            return lines;
        }

        for (Monster m : monsters) {
            if (m != null && !m.dead()) {
                lines.add(
                        m.getName()
                                + " | Vida: " + m.getHealth()
                                + " | Int: " + m.getIntelligence()
                                + " | Fue: " + m.getStrength()
                                + " | Pos: (" + m.getRow() + "," + m.getCol() + ")");
            }
        }

        if (lines.isEmpty()) {
            lines.add("No hay monstruos vivos");
        }

        return lines;
    }
}
