package mi.irgarten;

import java.util.ArrayList;
import java.util.Collections;

abstract class CardDeck<T extends CombatElement> {

    protected static final int TAMAÑO_BARAJA = 25;

    private final ArrayList<T> cardDeck;

    public CardDeck() {
        cardDeck = new ArrayList<>();
    }

    protected void addCard(T card) {
        cardDeck.add(card);
    }

    protected abstract void addCards();

    public T nextCard() {
        if (cardDeck.isEmpty()) {
            addCards();
            Collections.shuffle(cardDeck);
        }
        return cardDeck.remove(0);
    }
}
