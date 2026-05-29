package mi.irgarten;

/**
 * Weapon: offensive combat element that a {@link Player} can carry.
 *
 * Extends {@link CombatElement} to inherit the effect + uses mechanic.
 * It only adds the {@link #attack()} operation (weapon-specific semantics)
 * and a more informative toString.
 *
 * OOP concepts illustrated:
 *  - Inheritance: reuses CombatElement's state and behavior
 *  - Specialization: adds attack() as a public view of the inherited produceEffect
 *  - toString overriding
 */
public class Weapon extends CombatElement {

    /**
     * @param p initial weapon power
     * @param u initial uses
     */
    public Weapon(float p, int u) {
        super(p, u);
    }

    /**
     * Returns the damage produced by the weapon in this attack (consumes one use).
     * Semantic facade over {@link CombatElement#produceEffect()}.
     */
    public float attack() {
        return produceEffect();
    }

    /** Compact version for the UI panel (not for exhaustive debugging). */
    public String shortInfo() {
        return "[P:" + getEffect() + ",U:" + getUses() + "]";
    }

    /** Overriding: prefixes "Weapon:" to the inherited toString. */
    @Override
    public String toString() {
        return "Weapon: " + super.toString();
    }
}
