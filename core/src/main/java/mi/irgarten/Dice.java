package mi.irgarten;

import java.util.ArrayList;
import java.util.Random;

/**
 * Utility class that centralizes ALL the game's randomness.
 *
 * It is the canonical example of the "utility class" pattern:
 *  - Private constructor: cannot be instantiated.
 *  - All methods are {@code static}: invoked as {@code Dice.randomPos(...)}.
 *  - Purely static state (a single shared {@link Random}).
 *
 * Why centralize? So no other model object depends on
 * {@link java.util.Random}: the rest of the code asks Dice and stays
 * decoupled from the concrete generator. This helps testing and consistency.
 *
 * OOP concepts illustrated:
 *  - Utility class (private constructor + static methods)
 *  - Encapsulation of a shared resource (the generator)
 *  - Abstraction: the rest of the model reasons in terms of "dice roll",
 *    not raw randomness.
 */
public class Dice {
    // Configuration constants: private static final = immutable and shared by the whole class.
    private static final int MAX_USES = 5;
    private static final float MAX_INTELLIGENCE = 10f;
    private static final float MAX_STRENGTH = 10f;
    private static final int WEAPONS_REWARD = 2;
    private static final int SHIELDS_REWARD = 3;
    private static final int HEALTH_REWARD = 5;
    private static final int MAX_ATTACK = 3;
    private static final int MAX_SHIELD = 2;

    // Single generator shared by the whole application. Encapsulated: nobody sees it nor replaces it.
    private static final Random generator = new Random();

    // Private constructor: utility class, not instantiable. Calling new Dice() is a design mistake.
    private Dice() {
    }

    /**
     * Random position in the range [0, max).
     * @param max exclusive upper bound
     * @return a random integer valid as an index
     */
    public static int randomPos(int max) {
        return generator.nextInt(max);
    }

    /**
     * Selects which player starts the game.
     * @param nplayers total number of players
     * @return index of the starting player
     */
    public static int whoStarts(int nplayers) {
        return generator.nextInt(nplayers);
    }

    /** Random intelligence in [0, MAX_INTELLIGENCE). Used when creating characters. */
    public static float randomIntelligence() {
        return generator.nextFloat() * MAX_INTELLIGENCE;
    }

    /** Random strength in [0, MAX_STRENGTH). */
    public static float randomStrength() {
        return generator.nextFloat() * MAX_STRENGTH;
    }

    /**
     * Decides whether a dead player resurrects or not (coin flip).
     * Called from {@link Game#manageResurrection()}.
     */
    public static boolean resurrectPlayer() {
        return generator.nextBoolean();
    }

    /** Amount of weapons given as a reward after winning a combat. */
    public static int weaponsReward() {
        return generator.nextInt(WEAPONS_REWARD + 1);
    }

    /** Amount of shields given as a reward. */
    public static int shieldsReward() {
        return generator.nextInt(SHIELDS_REWARD + 1);
    }

    /** Extra health given as a reward. */
    public static int healthReward() {
        return generator.nextInt(HEALTH_REWARD + 1);
    }

    /** Base power of a freshly generated weapon. */
    public static float weaponPower() {
        return generator.nextFloat() * MAX_ATTACK;
    }

    /** Base power of a freshly generated shield. */
    public static float shieldPower() {
        return generator.nextFloat() * MAX_SHIELD;
    }

    /** Remaining uses with which weapons and shields are initialized. */
    public static int usesLeft() {
        return generator.nextInt(MAX_USES + 1);
    }

    /**
     * Modulates a competence (strength or intelligence) by a random factor in [0,1).
     * Adds variance to attack and defense, avoiding fully deterministic combats.
     * @param competence upper bound — usually the character's strength or intelligence
     */
    public static float intensity(float competence) {
        return generator.nextFloat() * competence;
    }

    /**
     * Decides whether a combat element (weapon or shield) should be discarded.
     * Discard probability is proportional to wear (the fewer uses left, the more likely).
     * @param usesLeft remaining uses of the element
     * @return true if the element must disappear
     */
    public static boolean discardElement(int usesLeft) {
        if (usesLeft <= 0) {
            return true; // no uses left: always discarded
        }
        if (usesLeft >= MAX_USES) {
            return false; // freshly created at maximum: never discarded yet
        }
        float aleatorio = generator.nextFloat();
        // The fewer uses remaining, the higher the discard probability: prob grows linearly with wear.
        float prob = (float) (MAX_USES - usesLeft) / MAX_USES;
        return prob > aleatorio;
    }

    /**
     * Decides the actual direction a character will move in.
     * The higher the intelligence, the more likely the preferred direction is respected.
     * With low intelligence the character "makes mistakes" and picks a valid one at random.
     *
     * This logic is the basis of {@link FuzzyPlayer}'s erratic behavior.
     *
     * @param preference direction the player wanted to take
     * @param validMoves directions actually possible from its cell
     * @param intelligence character's intelligence (the higher the value, the more reliable)
     * @return direction that will finally be taken
     */
    public static Directions nextStep(Directions preference, ArrayList<Directions> validMoves, float intelligence) {
        if (validMoves == null || validMoves.isEmpty()) {
            return preference; // no valid alternatives, we return the preferred one (labyrinth logic will reject it)
        }

        float porcentaje = intelligence / MAX_INTELLIGENCE;
        // If the preferred direction is valid and the "reliability roll" succeeds, it is respected.
        if (validMoves.contains(preference) && porcentaje >= generator.nextFloat()) {
            return preference;
        }

        // Otherwise: pick at random among valid moves.
        return validMoves.get(generator.nextInt(validMoves.size()));
    }

    /**
     * Decides which concrete player type the dead one resurrects as: Fuzzy or Super.
     * SUPER is much more likely (80%) than FUZZY (20%).
     * The result drives the dynamic polymorphism of resurrection in Game.
     */
    public static ResurrectedPlayer fuzzyOrSuper() {
        float aleatorio = intensity(1f);
        if (aleatorio < 0.8f) {
            return ResurrectedPlayer.SUPER;
        }
        return ResurrectedPlayer.FUZZY;
    }
}
