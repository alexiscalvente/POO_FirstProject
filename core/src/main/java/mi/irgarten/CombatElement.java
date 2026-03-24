package mi.irgarten;

abstract class CombatElement {
    private float effect;
    private int uses;

    public CombatElement(float e, int u) {
        effect = e;
        uses = u;
    }

    protected float produceEffect() {
        if (uses > 0) {
            uses--;
            return effect;
        } else {
            return 0;
        }
    }

    public boolean discard() {
        return Dice.discardElement(uses);
    }

    public float getEffect() {
        return effect;
    }

    public int getUses() {
        return uses;
    }

    @Override
    public String toString() {
        return "[ " + effect + ", " + uses + " ]";
    }
}