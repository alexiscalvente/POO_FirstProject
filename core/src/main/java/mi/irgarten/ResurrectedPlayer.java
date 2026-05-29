package mi.irgarten;

/**
 * Labels the two types of player a dead Player can be resurrected into.
 *
 * Used as the return value of {@link Dice#fuzzyOrSuper()} so that
 * {@link Game#manageResurrection()} can decide which concrete subclass
 * (FuzzyPlayer or SuperPlayer) to replace the fallen player with. In other words,
 * this enum drives the dynamic polymorphism of the resurrection without coupling
 * Dice to the subclasses.
 *
 * OOP concepts illustrated:
 *  - Enumeration as a subclass-variant descriptor
 *  - Low coupling: Dice returns a label, it does not instantiate subclasses
 */
public enum ResurrectedPlayer {
    FUZZY, SUPER
}
