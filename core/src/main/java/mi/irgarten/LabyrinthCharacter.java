package mi.irgarten;

abstract class LabyrinthCharacter {
    private String name;
    private float intelligence;
    private float strength;
    private float health;
    private int row = -1;
    private int col = -1;

    public LabyrinthCharacter(String name, float intelligence, float strength, float health) {
        this.name = name;
        this.intelligence = intelligence;
        this.strength = strength;
        this.health = health;
    }

    public LabyrinthCharacter(LabyrinthCharacter other) {
        this.name = other.name;
        this.intelligence = other.intelligence;
        this.strength = other.strength;
        this.health = other.health;
        this.row = other.row;
        this.col = other.col;
    }

    public boolean dead() {
        return health <= 0;
    }

    public String getName() {
        return name;
    }

    public float getIntelligence() {
        return intelligence;
    }

    public float getStrength() {
        return strength;
    }

    public float getHealth() {
        return health;
    }

    protected void setHealth(float health) {
        this.health = health;
    }

    protected void sumaHealth(float health) {
        this.health += health;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void setPos(int row, int col) {
        if (row >= 0) {
            this.row = row;
        }
        if (col >= 0) {
            this.col = col;
        }
    }

    @Override
    public String toString() {
        return name
                + " Inteligencia: " + intelligence
                + ", Fuerza: " + strength
                + ", Vida: " + health
                + ", Posición(F,C): " + row + ", " + col + "\n";
    }

    protected void gotWounded() {
        health--;
    }

    public abstract float attack();

    public abstract boolean defend(float attack);
}