package mi.irgarten;

/**
 * Closed set of the four possible movement directions inside the labyrinth.
 *
 * Modeled as an enum (not as int/String) for type safety: the compiler
 * prevents passing invalid values and allows exhaustive switches over the options.
 *
 * OOP concepts illustrated:
 *  - Strongly typed enumeration as a replacement for constants
 *  - Type safety: it is impossible to represent an "invalid" direction
 */
public enum Directions {
    LEFT, RIGHT, UP, DOWN
}
