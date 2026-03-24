package mi.irgarten;

public class SuperPlayer extends Player {

    public SuperPlayer(Player other) {
        super(other);
    }

    @Override
    public float attack() {
        return super.attack() * getIntelligence();
    }

    @Override
    public String toString() {
        return "Soy Súper " + super.toString();
    }
}
