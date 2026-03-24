package mi.irgarten;

import java.util.ArrayList;
import java.util.Random;

public class Dice {
    private static final int MAX_USES = 5;
    private static final float MAX_INTELLIGENCE = 10f;
    private static final float MAX_STRENGTH = 10f;
    private static final int WEAPONS_REWARD = 2;
    private static final int SHIELDS_REWARD = 3;
    private static final int HEALTH_REWARD = 5;
    private static final int MAX_ATTACK = 3;
    private static final int MAX_SHIELD = 2;

    private static final Random generator = new Random();

    private Dice() {
    }

    public static int randomPos(int max) {
        return generator.nextInt(max);
    }

    public static int whoStarts(int nplayers) {
        return generator.nextInt(nplayers);
    }

    public static float randomIntelligence() {
        return generator.nextFloat() * MAX_INTELLIGENCE;
    }

    public static float randomStrength() {
        return generator.nextFloat() * MAX_STRENGTH;
    }

    public static boolean resurrectPlayer() {
        return generator.nextBoolean();
    }

    public static int weaponsReward() {
        return generator.nextInt(WEAPONS_REWARD + 1);
    }

    public static int shieldsReward() {
        return generator.nextInt(SHIELDS_REWARD + 1);
    }

    public static int healthReward() {
        return generator.nextInt(HEALTH_REWARD + 1);
    }

    public static float weaponPower() {
        return generator.nextFloat() * MAX_ATTACK;
    }

    public static float shieldPower() {
        return generator.nextFloat() * MAX_SHIELD;
    }

    public static int usesLeft() {
        return generator.nextInt(MAX_USES + 1);
    }

    public static float intensity(float competence) {
        return generator.nextFloat() * competence;
    }

    public static boolean discardElement(int usesLeft) {
        if (usesLeft <= 0) {
            return true;
        }
        if (usesLeft >= MAX_USES) {
            return false;
        }
        float aleatorio = generator.nextFloat();
        float prob = (float) (MAX_USES - usesLeft) / MAX_USES;
        return prob > aleatorio;
    }

    public static Directions nextStep(Directions preference, ArrayList<Directions> validMoves, float intelligence) {
        if (validMoves == null || validMoves.isEmpty()) {
            return preference;
        }

        float porcentaje = intelligence / MAX_INTELLIGENCE;
        if (validMoves.contains(preference) && porcentaje >= generator.nextFloat()) {
            return preference;
        }

        return validMoves.get(generator.nextInt(validMoves.size()));
    }

    public static ResurrectedPlayer fuzzyOrSuper() {
        float aleatorio = intensity(1f);
        if (aleatorio < 0.8f) {
            return ResurrectedPlayer.SUPER;
        }
        return ResurrectedPlayer.FUZZY;
    }
}
