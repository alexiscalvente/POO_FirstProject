package mi.irgarten;

/**
 * Monster: enemy not controlled by the player. Concrete subclass of {@link LabyrinthCharacter}.
 *
 * Its logic is much simpler than a Player's: it does not accumulate weapons or shields,
 * its attack depends only on strength, and defense depends only on intelligence.
 * It dies when health reaches 0 (no "consecutive hits" mechanic like in Player).
 *
 * OOP concepts illustrated:
 *  - Concrete inheritance of an abstract class
 *  - Overriding (@Override) of the abstract methods attack and defend
 *  - Dynamic polymorphism: when Game calls monster.defend(...) THIS implementation runs
 */
public class Monster extends LabyrinthCharacter {
    // Fixed initial health for all monsters. Lower than the Player's so they can be defeated.
    private static final float INITIAL_HEALTH = 5f;

    /**
     * Builds a monster with the given name and attributes; health starts at INITIAL_HEALTH.
     */
    public Monster(String nombre, float inteligencia, float fuerza) {
        super(nombre, inteligencia, fuerza, INITIAL_HEALTH);
    }

    /**
     * Monster's attack: random intensity modulated by its strength.
     * Overriding of the inherited abstract.
     */
    @Override
    public float attack() {
        return Dice.intensity(getStrength());
    }

    /**
     * Monster's defense: if the received attack exceeds its intelligence (defensive energy),
     * it loses one health point and possibly dies.
     *
     * @param receivedAttack damage coming from the player
     * @return true if the monster has died in this defense
     */
    @Override
    public boolean defend(float receivedAttack) {
        boolean isDead = dead();
        if (!isDead) {
            float defensiveEnergy = getIntelligence();
            if (defensiveEnergy < receivedAttack) {
                gotWounded();           // loses one health point
                isDead = dead();        // re-checks whether it has died with the new health
            }
        }
        return isDead;
    }
}
