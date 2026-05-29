package mi.irgarten;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Generic deck of combat elements. It is the project's key piece regarding
 * use of generics and the Template Method pattern.
 *
 * Why generic? To reuse the same deck logic for weapons and shields without
 * duplicating code. {@link WeaponCardDeck} extends {@code CardDeck<Weapon>}
 * and {@link ShieldCardDeck} extends {@code CardDeck<Shield>}.
 *
 * Why the {@code T extends CombatElement} bound? It is type safety: it
 * guarantees at compile time that only objects from the CombatElement family
 * can be shuffled. Without the bound, we could shuffle Strings or Players, which
 * would make no sense.
 *
 * Why Template Method? The superclass defines the algorithm's "skeleton"
 * in {@link #nextCard()} (check if empty, refill, shuffle, return the first one)
 * and delegates the varying details — specifically HOW cards are generated —
 * to the abstract method {@link #addCards()}. Each subclass implements it by
 * creating its own objects (Weapon or Shield).
 *
 * OOP concepts illustrated:
 *  - Generics with a bound (T extends CombatElement) → type safety + reuse
 *  - Abstract class as a reusable skeleton
 *  - Template Method pattern (nextCard fixed, addCards variable)
 *  - Encapsulation (the internal list is private final)
 */
abstract class CardDeck<T extends CombatElement> {

    // Fixed size of any deck in the game. protected static final = constant visible to subclasses.
    protected static final int TAMAÑO_BARAJA = 25;

    // final: the reference to the list does not change after construction (its content does).
    private final ArrayList<T> cardDeck;

    /** Creates an empty deck. Cards are generated lazily the first time one is requested. */
    public CardDeck() {
        cardDeck = new ArrayList<>();
    }

    /**
     * Adds a specific card to the deck. Protected: only addCards() of subclasses uses it.
     */
    protected void addCard(T card) {
        cardDeck.add(card);
    }

    /**
     * Template Method hook: each subclass decides how to populate the deck.
     * Abstract because each concrete type produces different cards (Weapon vs Shield).
     */
    protected abstract void addCards();

    /**
     * Returns the next card. If the deck is empty, it refills it by
     * calling the {@link #addCards()} hook and shuffles before serving.
     * The Template Method pattern is built on top of this method.
     *
     * @return the concrete card of the parameterized type T
     */
    public T nextCard() {
        if (cardDeck.isEmpty()) {
            addCards();                 // delegation to the abstract method (dynamic polymorphism)
            Collections.shuffle(cardDeck); // shuffle to randomize dealing
        }
        return cardDeck.remove(0);
    }
}
