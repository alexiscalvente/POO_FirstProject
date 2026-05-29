package mi.irgarten;

/**
 * Abstract base class of ALL characters in the labyrinth: {@link Player} and {@link Monster}
 * (with their subclasses {@link FuzzyPlayer} and {@link SuperPlayer}).
 *
 * Centralizes the common state — name, intelligence, strength, health, position — and
 * declares {@link #attack()} and {@link #defend(float)} as abstract because each subclass
 * implements them differently. This is the basis of the dynamic polymorphism used by
 * {@link Game#combat(Monster)}: the line {@code monster.defend(currentPlayer.attack())}
 * works without Game knowing the concrete type of {@code currentPlayer}.
 *
 * It is {@code abstract} because a "character" in the abstract makes no sense: only
 * concrete instances exist. The compiler prevents {@code new LabyrinthCharacter(...)}.
 *
 * OOP concepts illustrated:
 *  - Abstraction (abstract class + abstract methods)
 *  - Tiered encapsulation (private for state, protected for controlled mutators)
 *  - Constructor overloading (regular + copy constructor)
 *  - Copy constructor: key piece for resurrection (cloning state while preserving it)
 */
abstract class LabyrinthCharacter {
    private String name;          // character identifier
    private float intelligence;   // determines Player's defense and Monster's attack
    private float strength;       // contributes to the base attack
    private float health;         // private: nobody modifies directly from outside the family
    private int row = -1;         // -1 = not yet placed in the labyrinth
    private int col = -1;         // -1 = not yet placed in the labyrinth

    /**
     * Regular constructor: sets the name and initial attributes.
     * Position is set later via {@link #setPos(int, int)} when the labyrinth distributes
     * characters across the board.
     */
    public LabyrinthCharacter(String name, float intelligence, float strength, float health) {
        this.name = name;
        this.intelligence = intelligence;
        this.strength = strength;
        this.health = health;
    }

    /**
     * Copy constructor: creates a new character with the same state as {@code other}.
     *
     * Key piece for the resurrection mechanic — it allows building a FuzzyPlayer
     * or SuperPlayer preserving the original Player's data. It is constructor
     * overloading: same conceptual operation, different signature.
     */
    public LabyrinthCharacter(LabyrinthCharacter other) {
        this.name = other.name;
        this.intelligence = other.intelligence;
        this.strength = other.strength;
        this.health = other.health;
        this.row = other.row;
        this.col = other.col;
    }

    /** True if health has reached 0 or below. */
    public boolean dead() {
        return health <= 0;
    }

    public String getName() {
        return name;
    }

    public float getIntelligence() {
        return intelligence;
    }

    public float getStrength() {
        return strength;
    }

    public float getHealth() {
        return health;
    }

    /**
     * Protected mutator: only subclasses can set health directly
     * (Player.resurrect uses it to restore it to maximum). Keeps encapsulation
     * towards the outside.
     */
    protected void setHealth(float health) {
        this.health = health;
    }

    /**
     * Increases health (rewards add to it). Protected: same reason as setHealth.
     */
    protected void sumaHealth(float health) {
        this.health += health;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    /**
     * Places the character on a cell of the labyrinth. Ignores negative coordinates
     * to allow updating only one dimension without touching the other.
     */
    public void setPos(int row, int col) {
        if (row >= 0) {
            this.row = row;
        }
        if (col >= 0) {
            this.col = col;
        }
    }

    /** Common textual representation; subclasses extend it with super.toString(). */
    @Override
    public String toString() {
        return name
                + " Inteligencia: " + intelligence
                + ", Fuerza: " + strength
                + ", Vida: " + health
                + ", Posición(F,C): " + row + ", " + col + "\n";
    }

    /**
     * Subtracts one unit of health. Protected because only the subclasses' internal
     * defense logic should be able to wound the character.
     */
    protected void gotWounded() {
        health--;
    }

    /**
     * Abstract method: each subclass decides its attack formula.
     * Enables dynamic polymorphism in {@link Game#combat(Monster)}: the JVM picks
     * Player.attack, FuzzyPlayer.attack or SuperPlayer.attack based on the actual runtime type.
     */
    public abstract float attack();

    /**
     * Abstract method: each subclass resolves how to defend.
     * Returns true if the defense ends with the defender's "death" (in the combat
     * sense: for Player it can be by accumulated consecutive hits).
     */
    public abstract boolean defend(float attack);
}
