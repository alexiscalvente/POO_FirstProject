package mi.irgarten;

/**
 * Concrete deck of shields: instantiates the generic {@code CardDeck<T>} with T = Shield.
 *
 * Symmetric counterpart of {@link WeaponCardDeck}. Its separate existence (rather than
 * a single deck with mixed elements) is a design decision: weapons and shields are
 * dealt in different amounts and must be kept in independent piles.
 *
 * OOP concepts illustrated:
 *  - Generic type instantiation (CardDeck&lt;Shield&gt;)
 *  - Concrete implementation of the Template Method (addCards)
 */
public class ShieldCardDeck extends CardDeck<Shield> {

    public ShieldCardDeck() {
        super();
    }

    /** Fills the deck with TAMAÑO_BARAJA random shields. */
    @Override
    protected void addCards() {
        for (int i = 0; i < TAMAÑO_BARAJA; i++) {
            addCard(new Shield(Dice.shieldPower(), Dice.usesLeft()));
        }
    }
}
