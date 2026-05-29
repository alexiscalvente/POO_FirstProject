package mi.irgarten;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * PRESENTATION layer: bridge between the game logic ({@link Game}, {@link GameState})
 * and the libGDX graphics API. NO game rule is decided here — we just draw what
 * the model says and translate the user's keystrokes into calls to Game.nextStep.
 *
 * EXPLICIT INHERITANCE from an external library:
 *   {@code extends ApplicationAdapter} from libGDX.
 * ApplicationAdapter is an abstract class from libGDX that implements the lifecycle
 * methods (create, render, dispose, resize, pause, resume) as empty by default.
 * We override the ones we care about — "Template Method" pattern brought into the
 * framework: libGDX calls our methods at the appropriate time.
 *
 * core/lwjgl3 SEPARATION — key for defending the architecture:
 *   - The {@code core} module contains the pure logic (Game, Player, Labyrinth…) and this
 *     presentation class, without knowing ANYTHING about the operating system.
 *   - The {@code lwjgl3} module is just the desktop launcher. It launches MainGame and nothing else.
 *   - MainGame consumes Game's PUBLIC API (nextStep, getGameState, finished,
 *     getCurrentPlayer, getMonstersDebugLines) and never touches internal attributes.
 *   - This is the clean "model / presentation / platform" boundary.
 *
 * COMPOSITION over inheritance:
 *   MainGame HAS a {@link Game} ({@code miJuego}). It does NOT extend Game. This avoids coupling
 *   the presentation with the game logic and allows changing the UI without touching the model.
 *
 * Implicit State pattern:
 *   The {@link ScreenState} enumeration (START, SETTINGS, PLAYING, VICTORY) acts as the
 *   UI state; the {@link #render()} method delegates to different drawing and input
 *   routines based on the state, which in practice is a simplified version of the
 *   State pattern (no class per state, just a switch in the loop).
 *
 * OOP concepts illustrated:
 *  - Inheritance from a library abstract class (ApplicationAdapter)
 *  - Composition (MainGame has a Game)
 *  - Lifecycle method overriding
 *  - Encapsulation of graphic resources (all private)
 *  - Enumeration as UI finite state (implicit State pattern)
 *  - Clear model/view separation
 */
public class MainGame extends ApplicationAdapter {

    /**
     * UI screen states. The render() of the main loop picks what to draw and what input
     * to process based on the current value. It is the basis of the implicit State pattern.
     */
    private enum ScreenState {
        START,      // welcome screen
        SETTINGS,   // options screen
        PLAYING,    // game in progress
        VICTORY     // game won
    }

    // ---- Layout constants. private static final = immutable and shared by the class. ----
    private static final int TOP_MARGIN = 26;
    private static final int BOTTOM_MARGIN = 22;
    private static final int LEFT_MARGIN = 26;
    private static final int MAX_RIGHT_PANEL_WIDTH = 430;
    private static final int MIN_RIGHT_PANEL_WIDTH = 320;
    private static final int PANEL_PADDING = 18;

    // Animation constants: movement smoothing factor and effect durations.
    private static final float PLAYER_LERP = 0.20f;          // 0..1 — how much it approaches the target each frame
    private static final float MOVE_PULSE_DURATION = 0.24f;  // seconds of the visual pulse on move
    private static final float COMBAT_FLASH_DURATION = 0.24f;// seconds of the red flash in combat
    private static final float SHAKE_DURATION = 0.16f;       // seconds of camera "shake"

    // ---- COMPOSITION: MainGame HAS a Game. It does not inherit from it. ----
    private Game miJuego;                // the game model
    private SpriteBatch batch;           // libGDX: groups draw calls for performance
    private BitmapFont font;             // typography
    private GlyphLayout glyphLayout;     // measures text width to center/split

    // Mandatory textures (loaded from assets/)
    private Texture texturaMuro;
    private Texture texturaSuelo;
    private Texture texturaSalida;
    private Texture texturaCombate;

    // Animations per frame. final List → the list is not reassigned, but its content is filled in create().
    private final List<Texture> playerFrames = new ArrayList<>();
    private final List<Texture> monsterFrames = new ArrayList<>();
    private final List<Texture> ownedTextures = new ArrayList<>(); // all textures to release in dispose()

    // Textures generated in code (solid color rectangles).
    private Texture texPixel;
    private Texture fondoGradiente;
    private Texture panelFondo;
    private Texture panelFondoClaro;
    private Texture marcoTablero;
    private Texture brilloSalida;
    private Texture barraFondo;
    private Texture barraVidaJugador;
    private Texture botonNormal;
    private Texture botonHover;
    private Texture fondoPantalla;

    // Audio resources. They can be null if the files do not exist (optional loading).
    private Sound sonidoMover;
    private Sound sonidoGolpe;
    private Sound sonidoVictoria;
    private Sound sonidoMuerteMonstruo;
    private Music musicaAmbiente;

    // Player avatar animation state: current interpolated position vs. logical target.
    private float playerDrawX;
    private float playerDrawY;
    private float playerTargetX;
    private float playerTargetY;
    private boolean playerVisualInitialized = false; // false until we know its first cell

    // Visual effect timers: they count down to 0 every frame.
    private float combatFlashTimer = 0f;
    private float movePulseTimer = 0f;
    private float shakeTimer = 0f;
    private float shakeX = 0f;
    private float shakeY = 0f;

    private float stateTime = 0f;             // accumulated time for cyclic animations (sin/cos)
    private boolean victorySoundPlayed = false; // guarantees the victory sound plays only once

    // Initial UI state. It will change to PLAYING when the user presses Enter.
    private ScreenState screenState = ScreenState.START;

    // Geometry of the "Close game" button (reused across screens).
    private float closeButtonX;
    private float closeButtonY;
    private final float closeButtonW = 180;
    private final float closeButtonH = 48;

    // Options configurable from the SETTINGS menu.
    private float uiScale = 1.0f;
    private float musicVolume = 0.30f;
    private float sfxVolume = 0.75f;
    private boolean fullscreenEnabled = true;
    private Game.GameDifficulty selectedDifficulty = Game.GameDifficulty.NORMAL;

    private int settingsIndex = 0; // which menu option is selected

    // ============================================================================
    //  libGDX LIFECYCLE (ApplicationAdapter overrides)
    // ============================================================================

    /**
     * libGDX lifecycle initialization hook. The framework calls it ONCE after
     * creating the window and before the first render. Here we load all resources
     * (textures, fonts, sounds) and start a game.
     *
     * Overriding: ApplicationAdapter.create() is empty by default; we fill it in.
     */
    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        glyphLayout = new GlyphLayout();
        setFontScale(1.0f);

        // Loading of sprites from classpath files
        cargarSprites();
        texturaMuro = cargarTexturaObligatoria("muro.png");
        texturaSuelo = cargarTexturaObligatoria("suelo.png");

        // Flat textures generated in memory (lighter than loading PNGs for solid colors)
        texPixel = crearTexturaColor(2, 2, 1f, 1f, 1f, 1f);
        texturaSalida = crearTexturaColor(32, 32, 0.2f, 0.9f, 0.22f, 1f);
        texturaCombate = crearTexturaColor(32, 32, 1f, 0.18f, 0.18f, 0.6f);
        fondoGradiente = crearTexturaColor(32, 32, 0.03f, 0.06f, 0.10f, 1f);
        panelFondo = crearTexturaColor(32, 32, 0.09f, 0.11f, 0.16f, 0.95f);
        panelFondoClaro = crearTexturaColor(32, 32, 0.15f, 0.18f, 0.24f, 0.65f);
        marcoTablero = crearTexturaColor(32, 32, 0.08f, 0.10f, 0.13f, 1f);
        brilloSalida = crearTexturaColor(16, 16, 0.45f, 0.95f, 0.50f, 0.45f);

        barraFondo = crearTexturaColor(8, 8, 0.22f, 0.22f, 0.25f, 1f);
        barraVidaJugador = crearTexturaColor(8, 8, 0.15f, 0.85f, 0.20f, 1f);

        botonNormal = crearTexturaColor(16, 16, 0.24f, 0.27f, 0.36f, 1f);
        botonHover = crearTexturaColor(16, 16, 0.50f, 0.28f, 0.22f, 1f);
        fondoPantalla = crearTexturaColor(16, 16, 0.05f, 0.07f, 0.11f, 1f);

        cargarSonidos();
        iniciarNuevaPartida();                       // builds the first Game
        fullscreenEnabled = Gdx.graphics.isFullscreen();
        screenState = ScreenState.START;
    }

    /**
     * Loads the player's main sprite (mandatory) and the optional variants to
     * animate it. Same pattern for monsters: a base texture and several variants.
     */
    private void cargarSprites() {
        Texture jugador = cargarTexturaObligatoria("jugador.png");
        registrarTextura(jugador);
        playerFrames.add(jugador);

        Texture jugadorAlt = cargarTexturaOpcional("jugadort.png");
        if (jugadorAlt != null) {
            registrarTextura(jugadorAlt);
            playerFrames.add(jugadorAlt);
        }

        Texture monstruoBase = cargarTexturaObligatoria("monstruo.png");
        registrarTextura(monstruoBase);
        monsterFrames.add(monstruoBase);

        // Variants loop: each extra PNG adds an animation frame if it exists.
        String[] variantes = { "monstruo1.png", "monstruo2.png", "monstruo3.png" };
        for (String nombre : variantes) {
            Texture frame = cargarTexturaOpcional(nombre);
            if (frame != null) {
                registrarTextura(frame);
                monsterFrames.add(frame);
            }
        }
    }

    /** Registers a texture to release it in dispose(); avoids double registrations. */
    private void registrarTextura(Texture texture) {
        if (texture != null && !ownedTextures.contains(texture)) {
            ownedTextures.add(texture);
        }
    }

    /**
     * Builds a NEW Game (the only way to restart the match).
     * Also resets the visual timers and the victory sound flag.
     *
     * Composition in action: this is where the model is instantiated. If MainGame inherited
     * from Game, this would be impossible — we could not change the game without changing the UI.
     */
    private void iniciarNuevaPartida() {
        miJuego = new Game(1, selectedDifficulty);
        playerVisualInitialized = false;
        victorySoundPlayed = false;
        combatFlashTimer = 0f;
        movePulseTimer = 0f;
        shakeTimer = 0f;
    }

    // ============================================================================
    //  AUDIO RESOURCE LOADING (optional — the app works without sound)
    // ============================================================================

    private void cargarSonidos() {
        sonidoMover = cargarSonidoSiExiste("move.mp3");
        sonidoGolpe = cargarSonidoSiExiste("hit.mp3");
        sonidoVictoria = cargarSonidoSiExiste("win.mp3");
        sonidoMuerteMonstruo = cargarSonidoSiExiste("monster_dead.mp3");

        // Accepts several alternative names for the ambient music.
        musicaAmbiente = cargarMusicaSiExiste("ambient.mp3", "ambiente.mp3", "music.mp3");
        aplicarVolumenMusica();
    }

    private Sound cargarSonidoSiExiste(String nombre) {
        FileHandle file = Gdx.files.internal(nombre);
        if (!file.exists()) {
            return null;
        }
        return Gdx.audio.newSound(file);
    }

    /**
     * Loads the FIRST music found among several candidate names.
     * Use of varargs ({@code String...}) to keep the call flexible.
     */
    private Music cargarMusicaSiExiste(String... nombres) {
        for (String nombre : nombres) {
            FileHandle file = Gdx.files.internal(nombre);
            if (file.exists()) {
                Music music = Gdx.audio.newMusic(file);
                music.setLooping(true);
                music.play();
                return music;
            }
        }
        return null;
    }

    private void aplicarVolumenMusica() {
        if (musicaAmbiente != null) {
            musicaAmbiente.setVolume(MathUtils.clamp(musicVolume, 0f, 1f));
            if (!musicaAmbiente.isPlaying()) {
                musicaAmbiente.play();
            }
        }
    }

    /** Helper to play effects respecting the global SFX volume. */
    private void reproducir(Sound sonido, float volumen, float pitch) {
        if (sonido != null) {
            sonido.play(volumen * sfxVolume, pitch, 0f);
        }
    }

    private Texture cargarTexturaObligatoria(String nombre) {
        FileHandle file = Gdx.files.internal(nombre);
        if (!file.exists()) {
            throw new RuntimeException("Falta el asset obligatorio: " + nombre);
        }
        return new Texture(file);
    }

    private Texture cargarTexturaOpcional(String nombre) {
        FileHandle file = Gdx.files.internal(nombre);
        if (!file.exists()) {
            return null;
        }
        return new Texture(file);
    }

    /**
     * Creates a solid one-color texture using a Pixmap. Useful for buttons and panels.
     * Important: the Pixmap is released after uploading data to GPU; we only keep the Texture.
     */
    private Texture crearTexturaColor(int width, int height, float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    // ============================================================================
    //  MAIN LOOP — render() (called by libGDX ~60 times/second)
    // ============================================================================

    /**
     * Main game loop. libGDX invokes it every frame.
     *
     * Structure:
     *  1. Updates time and effect timers.
     *  2. Processes GLOBAL input (ESC, F11, close click) — valid in any state.
     *  3. Clears the screen.
     *  4. Opens the batch (libGDX groups draw calls).
     *  5. Draws the background and delegates to the corresponding screen based on screenState.
     *     This switch embodies the "implicit State pattern": each state has its own
     *     drawing and its own input processing.
     *  6. Closes the batch.
     *
     * Overriding of ApplicationAdapter's empty render().
     */
    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        stateTime += delta;
        actualizarTimers(delta);
        gestionarInputGlobal();

        Gdx.gl.glClearColor(0.03f, 0.05f, 0.09f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        dibujarFondoDinamico();

        // Dispatch by state: this is where the "implicit State pattern" makes sense.
        if (screenState == ScreenState.START) {
            dibujarPantallaInicio();
        } else if (screenState == ScreenState.SETTINGS) {
            dibujarPantallaOpciones();
            gestionarInputOpciones();
        } else if (screenState == ScreenState.PLAYING) {
            gestionarInputJuego();

            // Model polling: if the game has ended, state transition.
            if (miJuego.finished()) {
                screenState = ScreenState.VICTORY;
            }

            dibujarLaberinto();
            dibujarPanelDerecho();
            if (combatFlashTimer > 0f) {
                dibujarFlashCombate();
            }
        } else if (screenState == ScreenState.VICTORY) {
            if (!victorySoundPlayed) {
                reproducir(sonidoVictoria, 0.85f, 1f);
                victorySoundPlayed = true; // guarantees the jingle plays ONCE per victory
            }
            dibujarPantallaVictoria();
        }

        batch.end();
    }

    /**
     * Decrements all visual effect timers (flash, pulse, shake) by delta.
     * While the shake is active, generates random offsets to simulate the rattle.
     */
    private void actualizarTimers(float delta) {
        if (combatFlashTimer > 0f) {
            combatFlashTimer -= delta;
            if (combatFlashTimer < 0f) {
                combatFlashTimer = 0f;
            }
        }

        if (movePulseTimer > 0f) {
            movePulseTimer -= delta;
            if (movePulseTimer < 0f) {
                movePulseTimer = 0f;
            }
        }

        if (shakeTimer > 0f) {
            shakeTimer -= delta;
            // Shake intensity decays linearly with the remaining time.
            float intensity = 6f * (shakeTimer / SHAKE_DURATION);
            shakeX = MathUtils.random(-intensity, intensity);
            shakeY = MathUtils.random(-intensity, intensity);
            if (shakeTimer <= 0f) {
                shakeX = 0f;
                shakeY = 0f;
            }
        }
    }

    // ============================================================================
    //  INPUT HANDLING (keyboard and mouse)
    // ============================================================================

    /**
     * GLOBAL input (valid in any screen): ESC navigates back or exits,
     * F11 toggles fullscreen, click on the close button terminates the app.
     */
    private void gestionarInputGlobal() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            // ESC depends on context: from SETTINGS go back to the menu; from START or VICTORY exit; from PLAYING go back to the menu.
            if (screenState == ScreenState.SETTINGS) {
                screenState = ScreenState.START;
            } else if (screenState == ScreenState.START || screenState == ScreenState.VICTORY) {
                Gdx.app.exit();
            } else {
                screenState = ScreenState.START;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            alternarPantallaCompleta();
        }

        if (clickEnBotonCerrar()) {
            Gdx.app.exit();
        }
    }

    /**
     * Input during the match. Translates directional keys into calls to {@link Game#nextStep}.
     * HERE is the UI → model boundary: the only information the UI passes to the model is
     * a {@link Directions}. The whole decision about what happens with that direction is made by Game.
     *
     * After a movement, fires visual/audio effects depending on the substrings that appear
     * in the log. It is coupling by convention (log substring searches are fragile), but
     * MainGame does not decide any outcome: it only reacts.
     */
    private void gestionarInputJuego() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            iniciarNuevaPartida();
            return;
        }

        boolean moved = false;

        // Each directional or WASD key is translated into a nextStep call with the direction.
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            miJuego.nextStep(Directions.UP);
            moved = true;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            miJuego.nextStep(Directions.DOWN);
            moved = true;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            miJuego.nextStep(Directions.LEFT);
            moved = true;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            miJuego.nextStep(Directions.RIGHT);
            moved = true;
        }

        // M key: quickly mutes/activates the music.
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            musicVolume = musicVolume > 0.01f ? 0f : 0.30f;
            aplicarVolumenMusica();
        }

        if (moved) {
            // Inspects the log returned by the model to decide which effects to fire.
            // The model is the "source of truth"; the UI only reacts to its messages.
            String log = miJuego.getLastLog();
            movePulseTimer = MOVE_PULSE_DURATION;

            if (log.contains("combate") || log.contains("monstruo") || log.contains("Has ganado")) {
                combatFlashTimer = COMBAT_FLASH_DURATION;
                shakeTimer = SHAKE_DURATION;
                reproducir(sonidoGolpe, 0.75f, MathUtils.random(0.96f, 1.04f));

                if (log.contains("Has ganado el combate")) {
                    reproducir(sonidoMuerteMonstruo, 0.85f, MathUtils.random(0.92f, 1.02f));
                }
            } else if (log.contains("bloqueado")) {
                reproducir(sonidoMover, 0.35f, 0.85f);
            } else {
                reproducir(sonidoMover, 0.62f, MathUtils.random(0.96f, 1.05f));
            }
        }
    }

    /**
     * Input specific to the SETTINGS screen: navigates between options with arrows/WASD
     * and adjusts values with left/right. It is an elementary "vertical menu".
     */
    private void gestionarInputOpciones() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            settingsIndex = (settingsIndex + 4) % 5; // up (with wrap-around modulo 5)
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            settingsIndex = (settingsIndex + 1) % 5;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            ajustarOpcionActual(-1);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            ajustarOpcionActual(1);
        }

        // ENTER on the "Back" option returns to the main menu.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (settingsIndex == 4) {
                screenState = ScreenState.START;
            }
        }
    }

    /**
     * Applies a +/-1 adjustment to the selected option. Ranges are constrained with clamp.
     * Changing difficulty restarts the match (Game is rebuilt with the new parameters).
     */
    private void ajustarOpcionActual(int dir) {
        if (settingsIndex == 0) {
            uiScale = MathUtils.clamp(uiScale + dir * 0.05f, 0.80f, 1.35f);
        } else if (settingsIndex == 1) {
            musicVolume = MathUtils.clamp(musicVolume + dir * 0.05f, 0f, 1f);
            aplicarVolumenMusica();
        } else if (settingsIndex == 2) {
            sfxVolume = MathUtils.clamp(sfxVolume + dir * 0.05f, 0f, 1f);
        } else if (settingsIndex == 3) {
            // Circular traversal of the GameDifficulty enum
            int next = selectedDifficulty.ordinal() + dir;
            if (next < 0) {
                next = Game.GameDifficulty.values().length - 1;
            }
            if (next >= Game.GameDifficulty.values().length) {
                next = 0;
            }
            selectedDifficulty = Game.GameDifficulty.values()[next];
            iniciarNuevaPartida(); // changing difficulty => new Game
        }
    }

    /**
     * Toggles between fullscreen and windowed. If it switches to windowed, it leaves a
     * reasonable margin against the display so the close is visible.
     */
    private void alternarPantallaCompleta() {
        Graphics.DisplayMode mode = Gdx.graphics.getDisplayMode();
        if (!fullscreenEnabled) {
            Gdx.graphics.setFullscreenMode(mode);
            fullscreenEnabled = true;
        } else {
            int w = Math.max(1280, mode.width - 320);
            int h = Math.max(720, mode.height - 180);
            Gdx.graphics.setWindowedMode(w, h);
            fullscreenEnabled = false;
        }
    }

    // ============================================================================
    //  UI SCREENS: START, SETTINGS, VICTORY
    //  (Each draws a "center card" with texts and buttons; they share helpers
    //   dibujarTarjetaCentro/dibujarTextoCentrado/dibujarBotonCerrar.)
    // ============================================================================

    /**
     * Initial screen: animated title, instructions and shortcuts. ENTER starts the match,
     * O opens options, ESC exits. It detects those keys at the end of the method.
     */
    private void dibujarPantallaInicio() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float centerX = w * 0.5f;
        float centerY = h * 0.5f;

        // Computes the center card size responsively.
        float cardW = Math.min(760f, w - 80f);
        float cardH = Math.min(520f, h - 70f);
        float cardX = centerX - cardW * 0.5f;
        float cardY = centerY - cardH * 0.5f;
        float compact = MathUtils.clamp(h / 900f, 0.78f, 1f);

        dibujarTarjetaCentro(cardX, cardY, cardW, cardH);
        dibujarBrillo(cardX + 20, cardY + cardH - 78, cardW - 40, 48, 0.08f);

        float y = cardY + cardH - 56f;

        // "IRGARTEN" title with oscillating scale (heartbeat) using sin(t).
        font.setColor(Color.GOLD);
        setFontScale((1.85f + 0.08f * MathUtils.sin(stateTime * 2.2f)) * compact);
        dibujarTextoCentrado("IRGARTEN", centerX, y);
        y -= 78f * compact;

        font.setColor(Color.WHITE);
        setFontScale(1.05f * compact);
        dibujarTextoCentrado("Explora el laberinto, derrota monstruos", centerX, y);
        y -= 33f * compact;
        dibujarTextoCentrado("y alcanza la salida.", centerX, y);
        y -= 52f * compact;

        font.setColor(Color.SKY);
        dibujarTextoCentrado("ENTER -> Empezar partida", centerX, y);
        y -= 30f * compact;
        dibujarTextoCentrado("O -> Opciones", centerX, y);
        y -= 30f * compact;
        dibujarTextoCentrado("ESC -> Salir", centerX, y);
        y -= 56f * compact;

        font.setColor(Color.LIGHT_GRAY);
        dibujarTextoCentrado("Movimiento: Flechas o WASD", centerX, y);
        y -= 28f * compact;
        dibujarTextoCentrado("F11: pantalla completa | M: mutear musica", centerX, y);

        setFontScale(1.0f);

        // The "Close game" button is anchored relative to the card.
        float closeY = Math.max(cardY + 22f, y - 72f);
        dibujarBotonCerrar(centerX - closeButtonW * 0.5f, closeY, "Cerrar juego");

        // Transitions from the initial screen.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            iniciarNuevaPartida();
            screenState = ScreenState.PLAYING;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            screenState = ScreenState.SETTINGS;
        }
    }

    /**
     * OPTIONS screen. Draws a vertical menu with labels and values; highlights the selected
     * row. The actual changes are applied by {@link #gestionarInputOpciones()} →
     * {@link #ajustarOpcionActual(int)}.
     */
    private void dibujarPantallaOpciones() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float centerX = w * 0.5f;

        float cardW = Math.min(840f, w - 70f);
        float cardH = Math.min(560f, h - 60f);
        float cardX = centerX - cardW * 0.5f;
        float cardY = (h - cardH) * 0.5f;

        dibujarTarjetaCentro(cardX, cardY, cardW, cardH);
        dibujarBrillo(cardX + 20, cardY + cardH - 78, cardW - 40, 52, 0.10f);

        float y = cardY + cardH - 56f;
        setFontScale(1.45f);
        font.setColor(Color.GOLD);
        dibujarTextoCentrado("OPCIONES", centerX, y);

        y -= 70f;
        setFontScale(1.05f);

        // Parallel listing of labels and values. The index marks which one is selected.
        String[] labels = {
                "Escala UI",
                "Volumen musica",
                "Volumen efectos",
                "Dificultad",
                "Volver"
        };

        String[] values = {
                Math.round(uiScale * 100f) + "%",
                Math.round(musicVolume * 100f) + "%",
                Math.round(sfxVolume * 100f) + "%",
                dificultadTexto(selectedDifficulty),
                "ENTER"
        };

        // Each row: if selected, a background highlight is drawn and the label in SKY.
        for (int i = 0; i < labels.length; i++) {
            boolean selected = i == settingsIndex;
            if (selected) {
                batch.setColor(1f, 1f, 1f, 0.14f);
                batch.draw(panelFondoClaro, cardX + 70, y - 26, cardW - 140, 40);
                batch.setColor(Color.WHITE);
            }

            font.setColor(selected ? Color.SKY : Color.WHITE);
            font.draw(batch, labels[i], cardX + 90, y);

            glyphLayout.setText(font, values[i]);
            font.draw(batch, values[i], cardX + cardW - 90 - glyphLayout.width, y);
            y -= 56f;
        }

        font.setColor(Color.LIGHT_GRAY);
        setFontScale(0.90f);
        dibujarTextoCentrado("Flechas/WASD para moverte. Izq/Der para ajustar. ESC para volver.", centerX, cardY + 72f);
        dibujarBotonCerrar(centerX - closeButtonW * 0.5f, cardY + 18f, "Cerrar juego");
    }

    /** Translates the {@link Game.GameDifficulty} enum into a friendly label for the menu. */
    private String dificultadTexto(Game.GameDifficulty difficulty) {
        if (difficulty == Game.GameDifficulty.EASY) {
            return "Facil";
        }
        if (difficulty == Game.GameDifficulty.HARD) {
            return "Dificil";
        }
        return "Normal";
    }

    /**
     * VICTORY screen. Shows player summary (final health, weapons, shields) and
     * shortcuts to restart/open options/exit. Reads the player state through
     * Game's public API ({@link Game#getCurrentPlayer()}).
     */
    private void dibujarPantallaVictoria() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float centerX = w * 0.5f;

        float cardW = Math.min(900f, w - 70f);
        float cardH = Math.min(610f, h - 60f);
        float cardX = centerX - cardW * 0.5f;
        float cardY = (h - cardH) * 0.5f;
        float infoWidth = cardW - 90f;

        dibujarTarjetaCentro(cardX, cardY, cardW, cardH);
        dibujarBrillo(cardX + 20, cardY + cardH - 78, cardW - 40, 52, 0.10f);

        float y = cardY + cardH - 50f;

        font.setColor(Color.GREEN);
        setFontScale(1.55f);
        dibujarTextoCentrado("HAS ESCAPADO DEL LABERINTO", centerX, y);
        y -= 60f;

        font.setColor(Color.WHITE);
        setFontScale(1.05f);
        dibujarTextoCentrado("Has llegado a la salida y completado la partida.", centerX, y);
        y -= 52f;

        // Player summary: polymorphism on display — getName() / getHealth() / weaponsInfo()
        // work the same whether the real instance is Player, FuzzyPlayer or SuperPlayer.
        Player jugador = miJuego.getCurrentPlayer();
        if (jugador != null) {
            font.setColor(Color.CYAN);
            dibujarTextoCentrado("Jugador: " + jugador.getName() + "  |  Vida final: " + jugador.getHealth(), centerX, y);
            y -= 42f;

            font.setColor(Color.WHITE);
            for (String s : dividirTextoPorAncho("Armas: " + jugador.weaponsInfo(), infoWidth)) {
                dibujarTextoCentrado(s, centerX, y);
                y -= 30f;
            }
            y -= 10f;
            for (String s : dividirTextoPorAncho("Escudos: " + jugador.shieldsInfo(), infoWidth)) {
                dibujarTextoCentrado(s, centerX, y);
                y -= 30f;
            }
        }

        y = Math.max(y, cardY + 120f);

        font.setColor(Color.GOLD);
        dibujarTextoCentrado("ENTER -> Nueva partida", centerX, y);
        y -= 30f;
        dibujarTextoCentrado("O -> Opciones", centerX, y);
        y -= 30f;
        dibujarTextoCentrado("ESC -> Salir", centerX, y);

        setFontScale(1.0f);
        dibujarBotonCerrar(centerX - closeButtonW * 0.5f, cardY + 22f, "Cerrar juego");

        // Transitions from the victory screen.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            iniciarNuevaPartida();
            screenState = ScreenState.PLAYING;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            screenState = ScreenState.SETTINGS;
        }
    }

    // ============================================================================
    //  LABYRINTH DRAWING DURING A MATCH
    // ============================================================================

    /**
     * Translates the model's {@code char[][]} into pixels: traverses the matrix and for each
     * cell draws floor, wall, exit, monster or combat. The player is NOT painted on the grid
     * directly but via an interpolated sprite (playerDrawX/Y) to animate the movement.
     *
     * Concepts to highlight:
     *  - The UI consumes the model's snapshot (matrix of characters) and does NOT know the
     *    Player/Monster classes: it only sees the character codes. This demonstrates the decoupling.
     *  - Linear interpolation (lerp): each frame, the visible position approaches the logical
     *    position by PLAYER_LERP percent. It gives the feel of smooth movement.
     */
    private void dibujarLaberinto() {
        Labyrinth lab = miJuego.getLabyrinth();
        char[][] mapa = lab.getMatrix();

        int filas = lab.getNumRows();
        int cols = lab.getNumCols();

        int anchoVentana = Gdx.graphics.getWidth();
        int altoVentana = Gdx.graphics.getHeight();
        int panelWidth = getRightPanelWidth();

        // Reserves space for the side panel and margins; computes the cell size that fits.
        int anchoDisponible = anchoVentana - panelWidth - LEFT_MARGIN * 2;
        int altoDisponible = altoVentana - TOP_MARGIN - BOTTOM_MARGIN;

        int tamCasilla = Math.min(anchoDisponible / cols, altoDisponible / filas);
        if (tamCasilla < 12) {
            tamCasilla = 12; // minimum size so something is still visible
        }

        int anchoTablero = tamCasilla * cols;
        int altoTablero = tamCasilla * filas;

        // We apply the "shake" offset only to the board, not the panel.
        int offsetX = LEFT_MARGIN + (anchoDisponible - anchoTablero) / 2 + Math.round(shakeX);
        int offsetY = BOTTOM_MARGIN + (altoDisponible - altoTablero) / 2 + Math.round(shakeY);

        int playerRow = -1;
        int playerCol = -1;

        dibujarMarcoTablero(offsetX, offsetY, anchoTablero, altoTablero);

        // Double loop: for each cell decides which texture to paint based on its character.
        for (int fila = 0; fila < filas; fila++) {
            for (int col = 0; col < cols; col++) {
                int x = offsetX + col * tamCasilla;
                // libGDX uses origin at lower-left corner, so we invert the row.
                int y = offsetY + (filas - 1 - fila) * tamCasilla;

                char celda = mapa[fila][col];

                // Checker pattern with sinusoidal pulse to give the floor some life.
                float checker = ((fila + col) % 2 == 0) ? 0.95f : 0.85f;
                float pulse = 0.04f * MathUtils.sin(stateTime * 1.7f + fila * 0.5f + col * 0.4f);
                batch.setColor(checker + pulse, checker + pulse, checker + pulse, 1f);
                batch.draw(texturaSuelo, x, y, tamCasilla, tamCasilla);
                batch.setColor(Color.WHITE);

                // Dispatch by cell character: equivalent to a mini visual switch.
                if (celda == 'X') {
                    dibujarSombra(x, y, tamCasilla, 0.18f);
                    batch.draw(texturaMuro, x, y, tamCasilla, tamCasilla);
                } else if (celda == 'E') {
                    // Exit with pulsing glow.
                    float exitPulse = 0.68f + 0.30f * MathUtils.sin(stateTime * 3f);
                    batch.setColor(1f, 1f, 1f, exitPulse);
                    batch.draw(texturaSalida, x, y, tamCasilla, tamCasilla);
                    batch.setColor(1f, 1f, 1f, 0.55f + 0.2f * MathUtils.sin(stateTime * 4f));
                    batch.draw(brilloSalida, x - tamCasilla * 0.14f, y - tamCasilla * 0.14f, tamCasilla * 1.28f,
                            tamCasilla * 1.28f);
                    batch.setColor(Color.WHITE);
                } else if (celda == 'M') {
                    // Monster with slight vertical "bobbing" (sinusoid).
                    float bob = 2f * MathUtils.sin(stateTime * 4f + fila + col);
                    dibujarSombra(x, y, tamCasilla, 0.26f);
                    batch.draw(getMonsterFrame(), x, y + bob, tamCasilla, tamCasilla);
                } else if (celda == 'C') {
                    // Combat: monster + red tint. We note this cell as the player's position.
                    float bob = 1.5f * MathUtils.sin(stateTime * 5f + fila + col);
                    dibujarSombra(x, y, tamCasilla, 0.30f);
                    batch.draw(getMonsterFrame(), x, y + bob, tamCasilla, tamCasilla);
                    batch.draw(texturaCombate, x, y, tamCasilla, tamCasilla);
                    playerRow = fila;
                    playerCol = col;
                } else if (Character.isDigit(celda)) {
                    // Cell with digit = player position. We note it to draw it separately (interpolated).
                    playerRow = fila;
                    playerCol = col;
                }
            }
        }

        // Player sprite: logical position → target pixel position → interpolation.
        if (playerRow != -1 && playerCol != -1) {
            float targetX = offsetX + playerCol * tamCasilla;
            float targetY = offsetY + (filas - 1 - playerRow) * tamCasilla;

            playerTargetX = targetX;
            playerTargetY = targetY;

            if (!playerVisualInitialized) {
                // First time we know where it is: we plant it there without interpolating.
                playerDrawX = playerTargetX;
                playerDrawY = playerTargetY;
                playerVisualInitialized = true;
            } else {
                // Smoothed lerp: each frame we get PLAYER_LERP closer to the target.
                playerDrawX += (playerTargetX - playerDrawX) * PLAYER_LERP;
                playerDrawY += (playerTargetY - playerDrawY) * PLAYER_LERP;
            }

            // Scale pulse on movement + constant sinusoidal bobbing.
            float pulse = movePulseTimer > 0f ? 1f + 0.20f * (movePulseTimer / MOVE_PULSE_DURATION) : 1f;
            float drawSize = tamCasilla * pulse;
            float shift = (drawSize - tamCasilla) * 0.5f;
            float bob = 1.5f * MathUtils.sin(stateTime * 8f);

            dibujarSombra(playerDrawX, playerDrawY, tamCasilla, 0.34f);
            batch.draw(getPlayerFrame(), playerDrawX - shift, playerDrawY - shift + bob, drawSize, drawSize);
        }
    }

    /**
     * Returns the player frame for this time instant. While moving, the animation speed
     * accelerates (more sense of marching).
     */
    private Texture getPlayerFrame() {
        if (playerFrames.isEmpty()) {
            return texturaSuelo; // fallback: should not occur if assets are right
        }
        float speed = movePulseTimer > 0f ? 12f : 5f;
        int index = (int) (stateTime * speed) % playerFrames.size();
        return playerFrames.get(index);
    }

    /** Returns the monster frame for this time instant (constant animation). */
    private Texture getMonsterFrame() {
        if (monsterFrames.isEmpty()) {
            return texturaSuelo;
        }
        int index = (int) (stateTime * 6f) % monsterFrames.size();
        return monsterFrames.get(index);
    }

    // ============================================================================
    //  REUSABLE DRAWING HELPERS (background, frames, glows, bars, flash)
    //  They are "pure presentation" auxiliary methods: they do not touch the model.
    // ============================================================================

    /** Screen background with strips that scroll creating a depth feel. */
    private void dibujarFondoDinamico() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        batch.draw(fondoPantalla, 0, 0, w, h);

        batch.setColor(0.8f, 0.95f, 1f, 0.13f);
        batch.draw(fondoGradiente, 0, h * 0.36f, w, h * 0.64f);
        batch.setColor(Color.WHITE);

        // Bright strips that scroll laterally with stateTime → cyclic animation.
        float stripAlpha = 0.04f + 0.03f * MathUtils.sin(stateTime * 0.7f);
        dibujarBrillo(-120 + (stateTime * 28f) % (w + 240f), h * 0.62f, w * 0.52f, 80f, stripAlpha);
        dibujarBrillo(w - ((stateTime * 36f) % (w + 220f)), h * 0.22f, w * 0.44f, 66f, stripAlpha * 1.1f);
    }

    /** Dark frame behind the board with a subtle light line on top. */
    private void dibujarMarcoTablero(float x, float y, float w, float h) {
        batch.draw(marcoTablero, x - 16, y - 16, w + 32, h + 32);
        batch.setColor(0.65f, 0.80f, 1f, 0.12f);
        batch.draw(panelFondoClaro, x - 10, y + h + 6, w + 20, 4);
        batch.setColor(Color.WHITE);
    }

    /** Center card used by START/SETTINGS/VICTORY screens: dark background + top bar. */
    private void dibujarTarjetaCentro(float x, float y, float w, float h) {
        batch.draw(panelFondo, x, y, w, h);
        batch.setColor(0.6f, 0.78f, 1f, 0.14f);
        batch.draw(panelFondoClaro, x + 10, y + h - 48, w - 20, 30);
        batch.setColor(Color.WHITE);
    }

    /** Flat elliptical shadow under characters/walls to give visual weight. */
    private void dibujarSombra(float x, float y, float tam, float alpha) {
        batch.setColor(0f, 0f, 0f, alpha);
        batch.draw(texPixel, x + tam * 0.18f, y + tam * 0.05f, tam * 0.64f, tam * 0.18f);
        batch.setColor(Color.WHITE);
    }

    /** Semi-transparent bluish band, reused in headers and background. */
    private void dibujarBrillo(float x, float y, float w, float h, float alpha) {
        batch.setColor(0.72f, 0.90f, 1f, alpha);
        batch.draw(texPixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    /** Full-screen red flash during a combat; opacity decays with the timer. */
    private void dibujarFlashCombate() {
        float alpha = combatFlashTimer / COMBAT_FLASH_DURATION;
        batch.setColor(1f, 0.15f, 0.15f, 0.18f * alpha);
        batch.draw(texPixel, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setColor(Color.WHITE);
    }

    /** Generic health bar: background + fill proportional to current/max ratio. */
    private void dibujarBarraVida(float x, float y, float width, float height, float current, float max,
            Texture barTexture) {
        batch.draw(barraFondo, x, y, width, height);

        float ratio = current / max;
        if (ratio < 0f) {
            ratio = 0f;
        }
        if (ratio > 1f) {
            ratio = 1f;
        }

        batch.draw(barTexture, x, y, width * ratio, height);
    }

    // ============================================================================
    //  RIGHT SIDE PANEL (information about the match in progress)
    // ============================================================================

    /**
     * Side panel with instructions, last log, current player data and the list
     * of alive monsters. It is the most visible face of the UI ↔ model boundary: each text
     * block is obtained by ASKING the model (getLastLog, getCurrentPlayer, getMonstersDebugLines).
     *
     * Implements "skip if it does not fit": if the Y cursor falls below a threshold,
     * the drawing is interrupted so we do not overflow the panel.
     */
    private void dibujarPanelDerecho() {
        int anchoVentana = Gdx.graphics.getWidth();
        int altoVentana = Gdx.graphics.getHeight();
        int panelWidth = getRightPanelWidth();

        int panelX = anchoVentana - panelWidth;
        batch.draw(panelFondo, panelX, 0, panelWidth, altoVentana);

        // Decorative top band.
        batch.setColor(0.60f, 0.75f, 1f, 0.12f);
        batch.draw(panelFondoClaro, panelX + 12, altoVentana - 74, panelWidth - 24, 36);
        batch.setColor(Color.WHITE);

        float x = panelX + PANEL_PADDING;
        float panelContentWidth = panelWidth - PANEL_PADDING * 2f;
        float y = altoVentana - 24;
        float line = 24f * uiScale;

        // Header and keyboard shortcuts.
        font.setColor(Color.WHITE);
        setFontScale(1.12f);
        font.draw(batch, "IRGARTEN // ESTADO", x, y);
        y -= line;
        setFontScale(0.94f);

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Mover: Flechas / WASD", x, y);
        y -= line;
        font.draw(batch, "Reiniciar: R", x, y);
        y -= line;
        font.draw(batch, "F11: Pantalla completa", x, y);
        y -= line;
        font.draw(batch, "M: Mutear musica", x, y);
        y -= line * 1.25f;

        // --- "Last event" section: shows the model's log split by width. ---
        font.setColor(Color.GOLD);
        font.draw(batch, "ULTIMO EVENTO", x, y);
        y -= line;

        batch.setColor(1f, 1f, 1f, 0.08f);
        batch.draw(panelFondoClaro, x - 6, y - 80, panelContentWidth + 8, 94);
        batch.setColor(Color.WHITE);

        font.setColor(Color.WHITE);
        for (String s : dividirTextoPorAncho(miJuego.getLastLog(), panelContentWidth - 14f)) {
            if (y < 96f) {
                break; // protects against vertical overflow
            }
            font.draw(batch, s, x, y);
            y -= line;
        }

        y -= 8;

        // --- "Current player" section: polymorphism in practice. ---
        // jugador can be Player, FuzzyPlayer or SuperPlayer: all respond to the same getters.
        Player jugador = miJuego.getCurrentPlayer();
        if (jugador != null && y > 170f) {
            font.setColor(Color.CYAN);
            font.draw(batch, "JUGADOR ACTUAL", x, y);
            y -= line;

            font.setColor(Color.WHITE);
            font.draw(batch, "Nombre: " + jugador.getName(), x, y);
            y -= line;

            font.draw(batch, "Vida: " + jugador.getHealth() + " / 30", x, y);
            y -= 8;
            float barWidth = panelContentWidth - 12f;
            float barY = y - 10f;
            dibujarBarraVida(x, barY, barWidth, 12f, jugador.getHealth(), 30f, barraVidaJugador);
            y = barY - 16f;

            font.draw(batch, "Inteligencia: " + jugador.getIntelligence(), x, y);
            y -= line;
            font.draw(batch, "Fuerza: " + jugador.getStrength(), x, y);
            y -= line;
            font.draw(batch, "Posicion: (" + jugador.getRow() + ", " + jugador.getCol() + ")", x, y);
            y -= line;

            for (String s : dividirTextoPorAncho("Armas: " + jugador.weaponsInfo(), panelContentWidth - 14f)) {
                if (y < 120f) {
                    break;
                }
                font.draw(batch, s, x, y);
                y -= line;
            }

            for (String s : dividirTextoPorAncho("Escudos: " + jugador.shieldsInfo(), panelContentWidth - 14f)) {
                if (y < 120f) {
                    break;
                }
                font.draw(batch, s, x, y);
                y -= line;
            }

            y -= 8f;
        }

        // --- "Monsters" section: asks the model for the pre-formatted list. ---
        if (y > 120f) {
            font.setColor(Color.SALMON);
            font.draw(batch, "MONSTRUOS", x, y);
            y -= line;

            font.setColor(Color.WHITE);
            ArrayList<String> monstruosInfo = miJuego.getMonstersDebugLines();
            for (String info : monstruosInfo) {
                if (y < 92f) {
                    break;
                }

                for (String s : dividirTextoPorAncho(info, panelContentWidth - 14f)) {
                    if (y < 92f) {
                        break;
                    }
                    font.draw(batch, s, x, y);
                    y -= line;
                }
                y -= 4f;
            }
        }

        setFontScale(1.0f);
        dibujarBotonCerrar(panelX + (panelWidth - closeButtonW) * 0.5f, 25, "Cerrar juego");
    }

    /** Width of the side panel: ~33% of the window, bounded between MIN and MAX. */
    private int getRightPanelWidth() {
        int desired = Math.round(Gdx.graphics.getWidth() * 0.33f);
        return MathUtils.clamp(desired, MIN_RIGHT_PANEL_WIDTH, MAX_RIGHT_PANEL_WIDTH);
    }

    /**
     * Reusable "Close game" button. Memorizes its position in {@code closeButtonX/Y} so
     * that {@link #clickEnBotonCerrar()} can detect the click. It is a simple way of
     * "shared state across frames" without a separate Button class.
     */
    private void dibujarBotonCerrar(float x, float y, String texto) {
        closeButtonX = x;
        closeButtonY = y;

        boolean hover = ratonDentro(closeButtonX, closeButtonY, closeButtonW, closeButtonH);
        batch.draw(hover ? botonHover : botonNormal, closeButtonX, closeButtonY, closeButtonW, closeButtonH);

        if (hover) {
            batch.setColor(1f, 1f, 1f, 0.17f + 0.08f * MathUtils.sin(stateTime * 11f));
            batch.draw(texPixel, closeButtonX, closeButtonY, closeButtonW, closeButtonH);
            batch.setColor(Color.WHITE);
        }

        font.setColor(Color.WHITE);
        glyphLayout.setText(font, texto);
        font.draw(batch, texto, closeButtonX + (closeButtonW - glyphLayout.width) * 0.5f, closeButtonY + 31f);
    }

    /** Prints a text centered in X. Uses GlyphLayout to measure the actual width. */
    private void dibujarTextoCentrado(String texto, float centerX, float y) {
        glyphLayout.setText(font, texto);
        font.draw(batch, texto, centerX - glyphLayout.width * 0.5f, y);
    }

    /**
     * Own "word wrap" algorithm: splits the text into lines that do not exceed {@code maxWidth}
     * pixels, splitting on spaces and, as a last resort, splitting inside a word too long.
     * Returns the list of lines, ready to be printed one by one.
     *
     * It is a good example of string handling with StringBuilder + GlyphLayout for measurement.
     */
    private ArrayList<String> dividirTextoPorAncho(String texto, float maxWidth) {
        ArrayList<String> lineas = new ArrayList<>();

        if (texto == null || texto.isEmpty()) {
            lineas.add("");
            return lineas;
        }

        String[] palabras = texto.split(" ");
        StringBuilder actual = new StringBuilder();

        for (String palabra : palabras) {
            // We try adding the next word; if it exceeds the width, we close the line.
            String candidato = actual.length() == 0 ? palabra : actual + " " + palabra;
            glyphLayout.setText(font, candidato);

            if (glyphLayout.width <= maxWidth) {
                actual.setLength(0);
                actual.append(candidato);
            } else {
                if (actual.length() > 0) {
                    lineas.add(actual.toString());
                    actual.setLength(0);
                }

                glyphLayout.setText(font, palabra);
                if (glyphLayout.width <= maxWidth) {
                    actual.append(palabra);
                } else {
                    // The word alone is wider than the panel: we chop it character by character.
                    String fragmento = "";
                    for (int i = 0; i < palabra.length(); i++) {
                        String siguiente = fragmento + palabra.charAt(i);
                        glyphLayout.setText(font, siguiente);
                        if (glyphLayout.width > maxWidth && !fragmento.isEmpty()) {
                            lineas.add(fragmento);
                            fragmento = String.valueOf(palabra.charAt(i));
                        } else {
                            fragmento = siguiente;
                        }
                    }
                    if (!fragmento.isEmpty()) {
                        actual.append(fragmento);
                    }
                }
            }
        }

        if (actual.length() > 0) {
            lineas.add(actual.toString());
        }

        return lineas;
    }

    /** Multiplies a base scale by the global UI scale configured in options. */
    private void setFontScale(float baseScale) {
        font.getData().setScale(baseScale * uiScale);
    }

    /**
     * Checks whether the cursor is inside a rectangle. Important: libGDX gives the mouse Y
     * from the TOP; we invert it to align with our coordinates (origin at the bottom).
     */
    private boolean ratonDentro(float x, float y, float w, float h) {
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    /** True if right in this frame a click has been made on the "Close game" button. */
    private boolean clickEnBotonCerrar() {
        return Gdx.input.justTouched() && ratonDentro(closeButtonX, closeButtonY, closeButtonW, closeButtonH);
    }

    // ============================================================================
    //  SHUTDOWN — dispose() releases ALL native resources
    // ============================================================================

    /**
     * Lifecycle hook called by libGDX when the app closes. We manually release
     * textures, fonts and sounds because they are NATIVE resources (GPU memory, audio buffers)
     * that the JVM garbage collector does not touch.
     *
     * Overriding of ApplicationAdapter's empty dispose().
     */
    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();

        // Textures registered in cargarSprites (the ones loaded from PNG).
        for (Texture texture : ownedTextures) {
            texture.dispose();
        }

        // Mandatory textures and those generated in code.
        texturaMuro.dispose();
        texturaSuelo.dispose();
        texturaSalida.dispose();
        texturaCombate.dispose();

        texPixel.dispose();
        fondoGradiente.dispose();
        panelFondo.dispose();
        panelFondoClaro.dispose();
        marcoTablero.dispose();
        brilloSalida.dispose();
        barraFondo.dispose();
        barraVidaJugador.dispose();
        botonNormal.dispose();
        botonHover.dispose();
        fondoPantalla.dispose();

        // Audio (can be null if assets did not exist): we check before releasing.
        if (sonidoMover != null) {
            sonidoMover.dispose();
        }
        if (sonidoGolpe != null) {
            sonidoGolpe.dispose();
        }
        if (sonidoVictoria != null) {
            sonidoVictoria.dispose();
        }
        if (sonidoMuerteMonstruo != null) {
            sonidoMuerteMonstruo.dispose();
        }
        if (musicaAmbiente != null) {
            musicaAmbiente.dispose();
        }
    }
}
