package mi.irgarten;

enum Orientation {
    VERTICAL, HORIZONTAL
}

enum GameCharacter {
    PLAYER, MONSTER
}

public class Weapon extends CombatElement {

    public Weapon(float p, int u) {
        super(p, u);
    }

    public float attack() {
        return produceEffect();
    }

    public String shortInfo() {
        return "[P:" + getEffect() + ",U:" + getUses() + "]";
    }

    @Override
    public String toString() {
        return "Weapon: " + super.toString();
    }
}