package mi.irgarten;

public class WeaponCardDeck extends CardDeck<Weapon> {

    public WeaponCardDeck() {
        super();
    }

    @Override
    protected void addCards() {
        for (int i = 0; i < TAMAÑO_BARAJA; i++) {
            addCard(new Weapon(Dice.weaponPower(), Dice.usesLeft()));
        }
    }
}
