package mi.irgarten;

public class ShieldCardDeck extends CardDeck<Shield> {

    public ShieldCardDeck() {
        super();
    }

    @Override
    protected void addCards() {
        for (int i = 0; i < TAMAÑO_BARAJA; i++) {
            addCard(new Shield(Dice.shieldPower(), Dice.usesLeft()));
        }
    }
}
