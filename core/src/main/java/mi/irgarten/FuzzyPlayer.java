package mi.irgarten;

import java.util.ArrayList;

public class FuzzyPlayer extends Player {

    public FuzzyPlayer(Player other) {
        super(other);
    }

    @Override
    public Directions move(Directions direction, ArrayList<Directions> validMoves) {
        Directions preferred = super.move(direction, validMoves);
        return Dice.nextStep(preferred, validMoves, getIntelligence());
    }

    @Override
    public float attack() {
        return sumWeapons() + Dice.intensity(getStrength());
    }

    @Override
    protected float defensiveEnergy() {
        return Dice.intensity(getIntelligence()) + sumShields();
    }

    @Override
    public String toString() {
        return "Fuzzy " + super.toString();
    }
}
