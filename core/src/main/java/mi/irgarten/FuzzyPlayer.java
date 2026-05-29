package mi.irgarten;

import java.util.ArrayList;

/**
 * "Fuzzy" player — variant in which a dead Player can resurrect (Fuzzy branch of the lottery).
 *
 * It changes three behaviors compared to the base Player:
 *  1. {@link #move(Directions, ArrayList)}: introduces randomness governed by intelligence
 *     (the less intelligence, the more movement mistakes).
 *  2. {@link #attack()}: stops summing strength directly and replaces it with a random intensity
 *     based on it, added to weapons damage.
 *  3. {@link #defensiveEnergy()}: intelligence is now modulated by chance, not fully contributed.
 *
 * OOP concepts illustrated:
 *  - Inheritance + selective overriding (only what differs changes)
 *  - Dynamic polymorphism: when Game.combat calls attack/defend on currentPlayer,
 *    if it is a FuzzyPlayer the JVM runs THESE methods, not Player's.
 *  - Reuse via super.move(...) and super.toString(): FuzzyPlayer relies on Player's
 *    behavior and "tweaks" it.
 *  - Inherited copy constructor: it is born from an existing Player.
 */
public class FuzzyPlayer extends Player {

    /**
     * Born from a dead Player, copying its state (via the inherited copy constructor).
     */
    public FuzzyPlayer(Player other) {
        super(other);
    }

    /**
     * "Erratic" movement: asks the base Player for a reasonable direction and then lets
     * {@link Dice#nextStep} perturb it depending on intelligence.
     * Overriding that reuses super.move and applies an additional layer.
     */
    @Override
    public Directions move(Directions direction, ArrayList<Directions> validMoves) {
        Directions preferred = super.move(direction, validMoves);
        return Dice.nextStep(preferred, validMoves, getIntelligence());
    }

    /**
     * Fuzzy attack: weapons (exact sum) + random intensity modulated by strength.
     * Overrides Player's attack(); in combat the JVM will pick this version by dynamic polymorphism.
     */
    @Override
    public float attack() {
        return sumWeapons() + Dice.intensity(getStrength());
    }

    /**
     * Fuzzy defense: instead of contributing full intelligence + shields, a random
     * intensity modulated by intelligence + shields is contributed.
     * Overriding of Player's protected method.
     */
    @Override
    protected float defensiveEnergy() {
        return Dice.intensity(getIntelligence()) + sumShields();
    }

    /** Prefixes "Fuzzy " to the inherited toString. */
    @Override
    public String toString() {
        return "Fuzzy " + super.toString();
    }
}
