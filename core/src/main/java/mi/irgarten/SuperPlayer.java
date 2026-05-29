package mi.irgarten;

/**
 * "Super" player — variant in which a dead Player resurrects with a bonus (Super branch of the lottery).
 *
 * Its only change from the base Player is the attack: it multiplies the base attack
 * (strength + weapons) by intelligence, which can inflate it enormously. Defense
 * and movement are inherited as-is from Player.
 *
 * OOP concepts illustrated:
 *  - Inheritance + minimal overriding: only what differs is redefined
 *  - Dynamic polymorphism: in Game.combat, currentPlayer.attack() runs this version
 *    if the real instance is a SuperPlayer
 *  - Reuse of the base implementation with super.attack()
 *  - Inherited copy constructor for resurrection
 */
public class SuperPlayer extends Player {

    /**
     * Born from a dead Player, copying its state via the inherited copy constructor.
     */
    public SuperPlayer(Player other) {
        super(other);
    }

    /**
     * "Super" attack: reuses super.attack() (strength + weapons) and multiplies it by intelligence.
     */
    @Override
    public float attack() {
        return super.attack() * getIntelligence();
    }

    /** Prefixes "Soy Súper" to the inherited toString. */
    @Override
    public String toString() {
        return "Soy Súper " + super.toString();
    }
}
