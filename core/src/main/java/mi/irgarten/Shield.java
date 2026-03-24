package mi.irgarten;

public class Shield extends CombatElement {

    public Shield(float p, int u) {
        super(p, u);
    }

    public float protect() {
        return produceEffect();
    }

    public String shortInfo() {
        return "[P:" + getEffect() + ",U:" + getUses() + "]";
    }

    @Override
    public String toString() {
        return "Shield: " + super.toString();
    }
}