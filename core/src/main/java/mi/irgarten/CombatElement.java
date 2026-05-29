package mi.irgarten;

/**
 * Abstract superclass of all elements involved in a combat ({@link Weapon}, {@link Shield}).
 *
 * Models what is common: a numerical effect (damage or protection) and a limited number of uses.
 * It is abstract because "combat element" in the abstract does not exist: only weapons and
 * shields do.
 *
 * The coupling with {@link CardDeck} happens through this class: the generic
 * {@code CardDeck<T extends CombatElement>} bounds T to guarantee that only weapons and
 * shields can be shuffled — a Player is not a CombatElement, for example.
 *
 * OOP concepts illustrated:
 *  - Abstraction (abstract class)
 *  - Encapsulation (private attributes with accessors)
 *  - Reuse via inheritance: Weapon and Shield share effect/uses without duplicating code
 *  - Designed for polymorphism: discard() queries Dice uniformly
 */
abstract class CombatElement {
    private float effect; // base power (damage if it is a weapon, protection if a shield)
    private int uses;     // remaining uses; when reaching 0 the element stops providing effect

    /**
     * Constructor common to all subclasses.
     * @param e initial effect
     * @param u initial uses
     */
    public CombatElement(float e, int u) {
        effect = e;
        uses = u;
    }

    /**
     * Produces the element's effect if uses remain, decrementing one in the process.
     * It is protected: only subclasses use it (Weapon.attack and Shield.protect encapsulate it).
     * @return the effect if uses remained; 0 if it was already spent
     */
    protected float produceEffect() {
        if (uses > 0) {
            uses--;
            return effect;
        } else {
            return 0;
        }
    }

    /**
     * Decides whether this element should be discarded from the player's inventory.
     * Delegation: it does not implement the probability logic, it asks {@link Dice}.
     * @return true if the element must be removed
     */
    public boolean discard() {
        return Dice.discardElement(uses);
    }

    public float getEffect() {
        return effect;
    }

    public int getUses() {
        return uses;
    }

    /**
     * Common textual representation. Subclasses override it to add their prefix (Weapon/Shield).
     * Overriding of Object.toString (the @Override annotation verifies it at compile time).
     */
    @Override
    public String toString() {
        return "[ " + effect + ", " + uses + " ]";
    }
}
