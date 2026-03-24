# Irgarten

Irgarten is a 2D labyrinth game built with [libGDX](https://libgdx.com/) and Java.  
You control a player through a grid-based maze, fight monsters, collect rewards, and try to reach the exit cell.

The project is also designed as an OOP-focused academic project: the game logic is organized around core object-oriented principles (encapsulation, abstraction, inheritance, and polymorphism).

## Project Description

The game is split into two main modules:

- `core`: game rules and domain model (`Game`, `Labyrinth`, `Player`, `Monster`, cards, combat, state).
- `lwjgl3`: desktop launcher and runtime entrypoint for playing on PC.

At runtime:

1. A maze is generated with obstacles, monsters, and one exit.
2. The player moves one step at a time.
3. Entering a monster cell triggers combat rounds.
4. If the player wins, rewards are granted (weapons/shields/health).
5. The game ends when the player reaches the exit.

## Gameplay

### Goal

Reach the exit (`E`) while surviving combat encounters.

### Controls

- `WASD` or arrow keys: move.
- `R`: restart current game.
- `M`: mute/unmute music.
- `F11`: toggle fullscreen.
- `ESC`: back to menu or exit depending on current screen.
- In menus: `O` opens settings, `ENTER` confirms/start.

### Game Systems

- **Difficulty presets**: `EASY`, `NORMAL`, `HARD` change maze and monster configuration.
- **Combat**: turn-based exchange up to a max number of rounds.
- **Rewards**: winning combat can grant weapon cards, shield cards, and health.
- **Resurrection mechanic**: defeated players may resurrect and evolve into specialized variants (`FuzzyPlayer` or `SuperPlayer`).

## OOP Concepts Used in This Project

### 1) Encapsulation

Classes keep internal state private and expose behavior via methods:

- `GameState` stores immutable snapshot data through private final fields and getters.
- `Game` encapsulates turn progression, combat resolution, and logging.
- `Labyrinth` hides board update rules (`canStepOn`, `updateOldPos`, movement validation).

This keeps rules centralized and prevents external classes from corrupting internal game state.

### 2) Abstraction

The project defines abstract bases that capture common behavior:

- `LabyrinthCharacter` abstracts shared character attributes and declares `attack()`/`defend(...)`.
- `CardDeck<T extends CombatElement>` abstracts deck behavior with a generic template and `addCards()` hook.

Concrete classes only implement specifics while reusing the common contract.

### 3) Inheritance

Concrete entities extend abstract/specialized parents:

- `Player` and `Monster` extend `LabyrinthCharacter`.
- `FuzzyPlayer` and `SuperPlayer` extend `Player`.
- `Weapon` and `Shield` extend `CombatElement`.
- `WeaponCardDeck` and `ShieldCardDeck` extend `CardDeck`.

Inheritance reduces duplication and models natural relationships in the game domain.

### 4) Polymorphism

Common method calls behave differently depending on runtime type:

- `attack()` and `defend(...)` are called through the shared `LabyrinthCharacter` contract but executed with class-specific logic.
- During resurrection, a base `Player` can be replaced by `FuzzyPlayer` or `SuperPlayer`, and gameplay continues through the same `Player` reference.
- `CardDeck.nextCard()` works uniformly while concrete deck subclasses define how cards are created.

This enables flexible gameplay behavior without changing high-level control flow.

## Build and Run

### Requirements

- Java JDK (recommended: 17+)
- Gradle wrapper (already included)

### Run (Windows)

```powershell
.\gradlew.bat lwjgl3:run
```

### Run (macOS/Linux)

```bash
./gradlew lwjgl3:run
```

### Build

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

## Assets

Game art/audio files are stored in the `assets/` folder (player, monsters, walls, floor, and sound effects/music).
