package mi.irgarten;

import java.util.ArrayList;

public class Player extends LabyrinthCharacter {
    private static final int MAX_WEAPONS = 2;
    private static final int MAX_SHIELDS = 2;
    private static final int INITIAL_HEALTH = 10;
    private static final int HITS2LOSE = 3;

    private char number;
    private int consecutiveHits = 0;
    private ArrayList<Weapon> weapons;
    private ArrayList<Shield> shields;
    private ShieldCardDeck shieldCardDeck;
    private WeaponCardDeck weaponCardDeck;

    public Player(char number, float intelligence, float strength) {
        super("Player#" + number, intelligence, strength, INITIAL_HEALTH);
        this.number = number;
        this.weapons = new ArrayList<>(MAX_WEAPONS);
        this.shields = new ArrayList<>(MAX_SHIELDS);
        this.shieldCardDeck = new ShieldCardDeck();
        this.weaponCardDeck = new WeaponCardDeck();
    }

    public Player(Player other) {
        super(other);
        this.number = other.number;
        this.consecutiveHits = other.consecutiveHits;
        this.weapons = new ArrayList<>(other.weapons);
        this.shields = new ArrayList<>(other.shields);
        this.shieldCardDeck = new ShieldCardDeck();
        this.weaponCardDeck = new WeaponCardDeck();
    }

    public void resurrect() {
        setHealth(INITIAL_HEALTH);
        consecutiveHits = 0;
        weapons.clear();
        shields.clear();
    }

    @Override
    public float attack() {
        return getStrength() + sumWeapons();
    }

    @Override
    public boolean defend(float receivedAttack) {
        return manageHit(receivedAttack);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("Consecutive Hits: ").append(consecutiveHits).append("\n");

        if (weapons.isEmpty()) {
            sb.append("El jugador no tiene armas.\n");
        } else {
            for (int i = 0; i < weapons.size(); i++) {
                sb.append("Weapon ").append(i).append(": ").append(weapons.get(i)).append("\n");
            }
        }

        if (shields.isEmpty()) {
            sb.append("El jugador no tiene escudos.\n");
        } else {
            for (int i = 0; i < shields.size(); i++) {
                sb.append("Shield ").append(i).append(": ").append(shields.get(i)).append("\n");
            }
        }

        return sb.toString();
    }

    protected float defensiveEnergy() {
        return getIntelligence() + sumShields();
    }

    private void resetHits() {
        consecutiveHits = 0;
    }

    private void incConsecutiveHits() {
        consecutiveHits++;
    }

    protected float sumWeapons() {
        float ret = 0;
        for (Weapon weapon : weapons) {
            ret += weapon.attack();
        }
        return ret;
    }

    protected float sumShields() {
        float ret = 0;
        for (Shield shield : shields) {
            ret += shield.protect();
        }
        return ret;
    }

    public char getNumber() {
        return number;
    }

    public Directions move(Directions direction, ArrayList<Directions> validMoves) {
        if (validMoves.isEmpty()) {
            return direction;
        }

        if (validMoves.contains(direction)) {
            return direction;
        }

        return validMoves.get(0);
    }

    private void purgeUsedWeapons() {
        weapons.removeIf(Weapon::discard);
    }

    private void purgeUsedShields() {
        shields.removeIf(Shield::discard);
    }

    private void receiveWeapon(Weapon weapon) {
        purgeUsedWeapons();
        if (weapons.size() < MAX_WEAPONS) {
            weapons.add(weapon);
        }
    }

    private void receiveShield(Shield shield) {
        purgeUsedShields();
        if (shields.size() < MAX_SHIELDS) {
            shields.add(shield);
        }
    }

    public void receiveReward() {
        int wReward = Dice.weaponsReward();
        int sReward = Dice.shieldsReward();

        for (int i = 0; i < wReward; i++) {
            receiveWeapon(new Weapon(Dice.weaponPower(), Dice.usesLeft()));
        }

        for (int i = 0; i < sReward; i++) {
            receiveShield(new Shield(Dice.shieldPower(), Dice.usesLeft()));
        }

        sumaHealth(Dice.healthReward());
    }

    private boolean manageHit(float receivedAttack) {
        float defense = defensiveEnergy();

        if (defense < receivedAttack) {
            gotWounded();
            incConsecutiveHits();
        } else {
            resetHits();
        }

        if (consecutiveHits >= HITS2LOSE || dead()) {
            resetHits();
            return true;
        }

        return false;
    }

    public String weaponsInfo() {
        if (weapons.isEmpty()) {
            return "ninguna";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < weapons.size(); i++) {
            sb.append(weapons.get(i).shortInfo());
            if (i < weapons.size() - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public String shieldsInfo() {
        if (shields.isEmpty()) {
            return "ninguno";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < shields.size(); i++) {
            sb.append(shields.get(i).shortInfo());
            if (i < shields.size() - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}