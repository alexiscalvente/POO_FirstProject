package mi.irgarten;

import java.util.ArrayList;

/**
 * Standard player. Concrete subclass of {@link LabyrinthCharacter} and, in turn,
 * superclass of the "resurrected" players {@link FuzzyPlayer} and {@link SuperPlayer}.
 *
 * Adds what a regular character does not have: inventory of weapons and shields,
 * own decks to refill equipment, "consecutive hits" logic and user-driven movement
 * (the Fuzzy version will distort it).
 *
 * The "resurrection" mechanic rests on its copy constructor: when a Player dies,
 * Game builds a FuzzyPlayer or SuperPlayer passing {@code this} to it to preserve
 * name/position/equipment and replace it in the list.
 *
 * OOP concepts illustrated:
 *  - Inheritance and specialization
 *  - Composition: the Player HAS weapons, shields and decks (it does not inherit from ArrayList)
 *  - Delegation: damage calculation is asked from each Weapon
 *  - Copy constructor (key piece for resurrection)
 *  - Encapsulation: equipment and counters are private; subclasses access with protected
 *  - Constructor overloading and method overriding
 */
public class Player extends LabyrinthCharacter {
    private static final int MAX_WEAPONS = 2;     // cap on weapons that can be carried at once
    private static final int MAX_SHIELDS = 2;     // cap on shields
    private static final int INITIAL_HEALTH = 10; // initial health; higher than the Monster's
    private static final int HITS2LOSE = 3;       // if you take N hits in a row without defending, you lose the combat

    private char number;                     // short identifier of the player ('0', '1', ...)
    private int consecutiveHits = 0;         // counter of consecutive hits received without defending well
    private ArrayList<Weapon> weapons;       // weapons inventory (composition)
    private ArrayList<Shield> shields;       // shields inventory (composition)
    private ShieldCardDeck shieldCardDeck;   // deck from which shields are refilled
    private WeaponCardDeck weaponCardDeck;   // deck from which weapons are refilled

    /**
     * Standard constructor: creates a player with its number and initial attributes.
     */
    public Player(char number, float intelligence, float strength) {
        super("Player#" + number, intelligence, strength, INITIAL_HEALTH);
        this.number = number;
        this.weapons = new ArrayList<>(MAX_WEAPONS);
        this.shields = new ArrayList<>(MAX_SHIELDS);
        this.shieldCardDeck = new ShieldCardDeck();
        this.weaponCardDeck = new WeaponCardDeck();
    }

    /**
     * Copy constructor. Replicates the original Player's state and creates new decks.
     *
     * Here is the basis of resurrection: FuzzyPlayer and SuperPlayer call this
     * constructor via {@code super(other)} to be born already with the dead one's data.
     * Important: the ArrayLists are copied with new ArrayList&lt;&gt;(other.weapons), creating
     * a new list but referencing the SAME Weapon/Shield objects. It is not a deep copy,
     * but the elements are immutable except for their internal uses counter.
     */
    public Player(Player other) {
        super(other);
        this.number = other.number;
        this.consecutiveHits = other.consecutiveHits;
        this.weapons = new ArrayList<>(other.weapons);
        this.shields = new ArrayList<>(other.shields);
        this.shieldCardDeck = new ShieldCardDeck();
        this.weaponCardDeck = new WeaponCardDeck();
    }

    /**
     * Reinitializes the player for resurrection: health to maximum, no pending
     * consecutive hits and no equipment (it will be reborn "naked" but as Fuzzy or Super).
     */
    public void resurrect() {
        setHealth(INITIAL_HEALTH);
        consecutiveHits = 0;
        weapons.clear();
        shields.clear();
    }

    /**
     * Player's base attack: strength + sum of damages from its weapons.
     * Overrides the abstract method from {@link LabyrinthCharacter}.
     */
    @Override
    public float attack() {
        return getStrength() + sumWeapons();
    }

    /**
     * Player's defense: delegates to {@link #manageHit(float)} which applies the
     * consecutive hits mechanic. Overrides the abstract method.
     */
    @Override
    public boolean defend(float receivedAttack) {
        return manageHit(receivedAttack);
    }

    /**
     * Extended textual representation: inherited health/attributes + inventory.
     * Subclasses (Fuzzy/Super) wrap it adding their prefix.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("Consecutive Hits: ").append(consecutiveHits).append("\n");

        if (weapons.isEmpty()) {
            sb.append("El jugador no tiene armas.\n");
        } else {
            for (int i = 0; i < weapons.size(); i++) {
                sb.append("Weapon ").append(i).append(": ").append(weapons.get(i)).append("\n");
            }
        }

        if (shields.isEmpty()) {
            sb.append("El jugador no tiene escudos.\n");
        } else {
            for (int i = 0; i < shields.size(); i++) {
                sb.append("Shield ").append(i).append(": ").append(shields.get(i)).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Player's defensive energy: intelligence + sum of protections from shields.
     * Protected: overridden by {@link FuzzyPlayer} to add randomness.
     */
    protected float defensiveEnergy() {
        return getIntelligence() + sumShields();
    }

    private void resetHits() {
        consecutiveHits = 0;
    }

    private void incConsecutiveHits() {
        consecutiveHits++;
    }

    /**
     * Sums damage from ALL weapons. Delegation: each Weapon is asked its attack();
     * Player does not compute damage directly, it aggregates it.
     */
    protected float sumWeapons() {
        float ret = 0;
        for (Weapon weapon : weapons) {
            ret += weapon.attack();
        }
        return ret;
    }

    /** Sum of protection from ALL shields. Same delegation idea as sumWeapons. */
    protected float sumShields() {
        float ret = 0;
        for (Shield shield : shields) {
            ret += shield.protect();
        }
        return ret;
    }

    public char getNumber() {
        return number;
    }

    /**
     * Standard Player's movement logic: if the preferred direction is valid,
     * it is respected; otherwise, the first available valid one is taken.
     *
     * {@link FuzzyPlayer} overrides it to introduce randomness governed
     * by intelligence (its movement "errs" with some probability).
     */
    public Directions move(Directions direction, ArrayList<Directions> validMoves) {
        if (validMoves.isEmpty()) {
            return direction;
        }

        if (validMoves.contains(direction)) {
            return direction;
        }

        return validMoves.get(0);
    }

    /**
     * Removes weapons marked for discard (Dice decides card by card).
     * Use of method reference and removeIf as a functional application on the collection.
     */
    private void purgeUsedWeapons() {
        weapons.removeIf(Weapon::discard);
    }

    private void purgeUsedShields() {
        shields.removeIf(Shield::discard);
    }

    /**
     * Adds a weapon if there is room (purging the spent ones beforehand).
     * Private: rewards ALWAYS go through {@link #receiveReward()}.
     */
    private void receiveWeapon(Weapon weapon) {
        purgeUsedWeapons();
        if (weapons.size() < MAX_WEAPONS) {
            weapons.add(weapon);
        }
    }

    private void receiveShield(Shield shield) {
        purgeUsedShields();
        if (shields.size() < MAX_SHIELDS) {
            shields.add(shield);
        }
    }

    /**
     * Reward after winning a combat: 0..N weapons, 0..M shields and a health bonus,
     * all determined by {@link Dice}.
     */
    public void receiveReward() {
        int wReward = Dice.weaponsReward();
        int sReward = Dice.shieldsReward();

        for (int i = 0; i < wReward; i++) {
            receiveWeapon(new Weapon(Dice.weaponPower(), Dice.usesLeft()));
        }

        for (int i = 0; i < sReward; i++) {
            receiveShield(new Shield(Dice.shieldPower(), Dice.usesLeft()));
        }

        sumaHealth(Dice.healthReward());
    }

    /**
     * Core of the defense mechanic: if defensive energy does not reach the received
     * attack, the player is wounded and adds a consecutive hit; if it resists,
     * the counter is reset. The player "loses" the combat if it accumulates HITS2LOSE
     * hits in a row or dies.
     *
     * @return true if the player is knocked out (lost this round)
     */
    private boolean manageHit(float receivedAttack) {
        float defense = defensiveEnergy();

        if (defense < receivedAttack) {
            gotWounded();
            incConsecutiveHits();
        } else {
            resetHits();    // a good defense breaks the streak of consecutive hits
        }

        if (consecutiveHits >= HITS2LOSE || dead()) {
            resetHits();
            return true;
        }

        return false;
    }

    /** Compact description of the weapons for the UI panel. */
    public String weaponsInfo() {
        if (weapons.isEmpty()) {
            return "ninguna";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < weapons.size(); i++) {
            sb.append(weapons.get(i).shortInfo());
            if (i < weapons.size() - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /** Compact description of the shields for the UI panel. */
    public String shieldsInfo() {
        if (shields.isEmpty()) {
            return "ninguno";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < shields.size(); i++) {
            sb.append(shields.get(i).shortInfo());
            if (i < shields.size() - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
