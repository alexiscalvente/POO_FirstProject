package mi.irgarten;

/**
 * Shield: defensive combat element that a {@link Player} can carry.
 *
 * Analogous to {@link Weapon} but its effect is interpreted as protection, not damage.
 * Same structure: inherits effect/uses from {@link CombatElement} and exposes {@link #protect()}.
 *
 * OOP concepts illustrated:
 *  - Inheritance + specialization parallel to Weapon
 *  - Reuse: the use-spending mechanic lives once in the superclass
 */
public class Shield extends CombatElement {

    /**
     * @param p initial power (protection)
     * @param u initial uses
     */
    public Shield(float p, int u) {
        super(p, u);
    }

    /**
     * Returns the amount of protection the shield provides in this defense.
     * Semantic facade over {@link CombatElement#produceEffect()}.
     */
    public float protect() {
        return produceEffect();
    }

    /** Compact version for the UI panel. */
    public String shortInfo() {
        return "[P:" + getEffect() + ",U:" + getUses() + "]";
    }

    @Override
    public String toString() {
        return "Shield: " + super.toString();
    }
}
