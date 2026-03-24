package mi.irgarten;

public class Monster extends LabyrinthCharacter {
    private static final float INITIAL_HEALTH = 5f;

    public Monster(String nombre, float inteligencia, float fuerza) {
        super(nombre, inteligencia, fuerza, INITIAL_HEALTH);
    }

    @Override
    public float attack() {
        return Dice.intensity(getStrength());
    }

    @Override
    public boolean defend(float receivedAttack) {
        boolean isDead = dead();
        if (!isDead) {
            float defensiveEnergy = getIntelligence();
            if (defensiveEnergy < receivedAttack) {
                gotWounded();
                isDead = dead();
            }
        }
        return isDead;
    }
}
