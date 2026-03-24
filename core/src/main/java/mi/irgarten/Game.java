package mi.irgarten;

import java.util.ArrayList;

public class Game {
    public enum GameDifficulty {
        EASY,
        NORMAL,
        HARD
    }

    private static final int MAX_ROUNDS = 10;

    private final int rows;
    private final int cols;
    private final int numMonsters;
    private final int numBlocks;
    private final int blockLength;

    private int currentPlayerIndex;
    private StringBuilder log;
    private ArrayList<Player> players;
    private ArrayList<Monster> monsters;
    private Labyrinth labyrinth;
    private Player currentPlayer;

    public Game(int nplayers) {
        this(nplayers, GameDifficulty.NORMAL);
    }

    public Game(int nplayers, GameDifficulty difficulty) {
        if (nplayers < 1) {
            nplayers = 1;
        }

        if (difficulty == null) {
            difficulty = GameDifficulty.NORMAL;
        }

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

        for (int i = 0; i < nplayers; i++) {
            players.add(new Player((char) ('0' + i), Dice.randomIntelligence(), Dice.randomStrength()));
        }

        currentPlayerIndex = Dice.whoStarts(nplayers);
        currentPlayer = players.get(currentPlayerIndex);

        int exitRow = Dice.randomPos(rows);
        int exitCol = Dice.randomPos(cols);
        labyrinth = new Labyrinth(rows, cols, exitRow, exitCol);

        configureLabyrinth();
        labyrinth.spreadPlayers(players);

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

    public boolean finished() {
        return labyrinth.haveAWinner();
    }

    public GameState getGameState() {
        return new GameState(
                labyrinth.toString(),
                players.toString(),
                monsters.toString(),
                currentPlayerIndex,
                finished(),
                log.toString());
    }

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

    private void configureLabyrinth() {
        for (int i = 0; i < numBlocks; i++) {
            int[] pos = labyrinth.randomEmptyPos();
            Orientation orientation = Dice.randomPos(2) == 0 ? Orientation.HORIZONTAL : Orientation.VERTICAL;
            labyrinth.addBlock(orientation, pos[0], pos[1], blockLength);
        }

        for (int i = 0; i < numMonsters; i++) {
            Monster monster = new Monster("Monstruo " + i, Dice.randomIntelligence(), Dice.randomStrength());
            monsters.add(monster);

            int[] pos = labyrinth.randomEmptyPos();
            labyrinth.addMonster(pos[0], pos[1], monster);
        }
    }

    private void nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        currentPlayer = players.get(currentPlayerIndex);
    }

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

    private Directions actualDirection(Directions preferredDirection) {
        int currentRow = currentPlayer.getRow();
        int currentCol = currentPlayer.getCol();
        ArrayList<Directions> validMoves = labyrinth.validMoves(currentRow, currentCol);
        return currentPlayer.move(preferredDirection, validMoves);
    }

    private GameCharacter combat(Monster monster) {
        int rounds = 0;

        while (rounds < MAX_ROUNDS) {
            rounds++;

            boolean monsterDead = monster.defend(currentPlayer.attack());
            if (monsterDead) {
                logRounds(rounds, MAX_ROUNDS);
                return GameCharacter.PLAYER;
            }

            boolean playerDead = currentPlayer.defend(monster.attack());
            if (playerDead) {
                logRounds(rounds, MAX_ROUNDS);
                return GameCharacter.MONSTER;
            }
        }

        logRounds(rounds, MAX_ROUNDS);

        if (currentPlayer.getHealth() >= monster.getHealth()) {
            return GameCharacter.PLAYER;
        }
        return GameCharacter.MONSTER;
    }

    private void manageResurrection() {
        boolean resurrect = Dice.resurrectPlayer();

        if (resurrect) {
            currentPlayer.resurrect();

            if (ResurrectedPlayer.FUZZY == Dice.fuzzyOrSuper()) {
                currentPlayer = new FuzzyPlayer(currentPlayer);
            } else {
                currentPlayer = new SuperPlayer(currentPlayer);
            }

            players.set(currentPlayerIndex, currentPlayer);
            labyrinth.actualicePlayer(currentPlayer);
            logResurrected();
        } else {
            logPlayerSkipTurn();
        }
    }

    private void manageReward(GameCharacter winner) {
        if (winner == GameCharacter.PLAYER) {
            currentPlayer.receiveReward();
            logPlayerWon();
        } else {
            logMonsterWon();
        }
    }

    public boolean nextStep(Directions preferredDirection) {
        log.setLength(0);

        if (currentPlayer.dead()) {
            manageResurrection();
            if (!finished()) {
                nextPlayer();
            }
            return finished();
        }

        Directions direction = actualDirection(preferredDirection);
        if (direction != preferredDirection) {
            logPlayerNoOrders();
        }

        int oldRow = currentPlayer.getRow();
        int oldCol = currentPlayer.getCol();

        Monster monster = labyrinth.putPlayer(direction, currentPlayer);

        if (currentPlayer.getRow() == oldRow && currentPlayer.getCol() == oldCol && monster == null) {
            logBlocked();
            return finished();
        }

        if (monster == null) {
            logNoMonster();
        } else {
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
