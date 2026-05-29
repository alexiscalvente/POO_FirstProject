package mi.irgarten;

/**
 * Concrete deck of weapons: instantiates the generic {@code CardDeck<T>} with T = Weapon.
 *
 * Implements the {@link #addCards()} hook of the Template Method defined in {@link CardDeck},
 * generating Weapons with random power and uses provided by {@link Dice}.
 *
 * OOP concepts illustrated:
 *  - Generic type instantiation (CardDeck&lt;Weapon&gt;)
 *  - Concrete implementation of the Template Method (addCards)
 *  - Delegation of randomness to Dice (utility class)
 */
public class WeaponCardDeck extends CardDeck<Weapon> {

    public WeaponCardDeck() {
        super();
    }

    /**
     * Fills the deck with TAMAÑO_BARAJA random weapons.
     * Overriding of the superclass's abstract method.
     */
    @Override
    protected void addCards() {
        for (int i = 0; i < TAMAÑO_BARAJA; i++) {
            addCard(new Weapon(Dice.weaponPower(), Dice.usesLeft()));
        }
    }
}
