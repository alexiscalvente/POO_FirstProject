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

public class MainGame extends ApplicationAdapter {

    private enum ScreenState {
        START,
        SETTINGS,
        PLAYING,
        VICTORY
    }

    private static final int TOP_MARGIN = 26;
    private static final int BOTTOM_MARGIN = 22;
    private static final int LEFT_MARGIN = 26;
    private static final int MAX_RIGHT_PANEL_WIDTH = 430;
    private static final int MIN_RIGHT_PANEL_WIDTH = 320;
    private static final int PANEL_PADDING = 18;

    private static final float PLAYER_LERP = 0.20f;
    private static final float MOVE_PULSE_DURATION = 0.24f;
    private static final float COMBAT_FLASH_DURATION = 0.24f;
    private static final float SHAKE_DURATION = 0.16f;

    private Game miJuego;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout glyphLayout;

    private Texture texturaMuro;
    private Texture texturaSuelo;
    private Texture texturaSalida;
    private Texture texturaCombate;

    private final List<Texture> playerFrames = new ArrayList<>();
    private final List<Texture> monsterFrames = new ArrayList<>();
    private final List<Texture> ownedTextures = new ArrayList<>();

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

    private Sound sonidoMover;
    private Sound sonidoGolpe;
    private Sound sonidoVictoria;
    private Sound sonidoMuerteMonstruo;
    private Music musicaAmbiente;

    private float playerDrawX;
    private float playerDrawY;
    private float playerTargetX;
    private float playerTargetY;
    private boolean playerVisualInitialized = false;

    private float combatFlashTimer = 0f;
    private float movePulseTimer = 0f;
    private float shakeTimer = 0f;
    private float shakeX = 0f;
    private float shakeY = 0f;

    private float stateTime = 0f;
    private boolean victorySoundPlayed = false;

    private ScreenState screenState = ScreenState.START;

    private float closeButtonX;
    private float closeButtonY;
    private final float closeButtonW = 180;
    private final float closeButtonH = 48;

    private float uiScale = 1.0f;
    private float musicVolume = 0.30f;
    private float sfxVolume = 0.75f;
    private boolean fullscreenEnabled = true;
    private Game.GameDifficulty selectedDifficulty = Game.GameDifficulty.NORMAL;

    private int settingsIndex = 0;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        glyphLayout = new GlyphLayout();
        setFontScale(1.0f);

        cargarSprites();
        texturaMuro = cargarTexturaObligatoria("muro.png");
        texturaSuelo = cargarTexturaObligatoria("suelo.png");

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
        iniciarNuevaPartida();
        fullscreenEnabled = Gdx.graphics.isFullscreen();
        screenState = ScreenState.START;
    }

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

        String[] variantes = { "monstruo1.png", "monstruo2.png", "monstruo3.png" };
        for (String nombre : variantes) {
            Texture frame = cargarTexturaOpcional(nombre);
            if (frame != null) {
                registrarTextura(frame);
                monsterFrames.add(frame);
            }
        }
    }

    private void registrarTextura(Texture texture) {
        if (texture != null && !ownedTextures.contains(texture)) {
            ownedTextures.add(texture);
        }
    }

    private void iniciarNuevaPartida() {
        miJuego = new Game(1, selectedDifficulty);
        playerVisualInitialized = false;
        victorySoundPlayed = false;
        combatFlashTimer = 0f;
        movePulseTimer = 0f;
        shakeTimer = 0f;
    }

    private void cargarSonidos() {
        sonidoMover = cargarSonidoSiExiste("move.mp3");
        sonidoGolpe = cargarSonidoSiExiste("hit.mp3");
        sonidoVictoria = cargarSonidoSiExiste("win.mp3");
        sonidoMuerteMonstruo = cargarSonidoSiExiste("monster_dead.mp3");

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

    private Texture crearTexturaColor(int width, int height, float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

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

        if (screenState == ScreenState.START) {
            dibujarPantallaInicio();
        } else if (screenState == ScreenState.SETTINGS) {
            dibujarPantallaOpciones();
            gestionarInputOpciones();
        } else if (screenState == ScreenState.PLAYING) {
            gestionarInputJuego();

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
                victorySoundPlayed = true;
            }
            dibujarPantallaVictoria();
        }

        batch.end();
    }

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
            float intensity = 6f * (shakeTimer / SHAKE_DURATION);
            shakeX = MathUtils.random(-intensity, intensity);
            shakeY = MathUtils.random(-intensity, intensity);
            if (shakeTimer <= 0f) {
                shakeX = 0f;
                shakeY = 0f;
            }
        }
    }

    private void gestionarInputGlobal() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
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

    private void gestionarInputJuego() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            iniciarNuevaPartida();
            return;
        }

        boolean moved = false;

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

        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            musicVolume = musicVolume > 0.01f ? 0f : 0.30f;
            aplicarVolumenMusica();
        }

        if (moved) {
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
    private void gestionarInputOpciones() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            settingsIndex = (settingsIndex + 4) % 5;
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (settingsIndex == 4) {
                screenState = ScreenState.START;
            }
        }
    }

    private void ajustarOpcionActual(int dir) {
        if (settingsIndex == 0) {
            uiScale = MathUtils.clamp(uiScale + dir * 0.05f, 0.80f, 1.35f);
        } else if (settingsIndex == 1) {
            musicVolume = MathUtils.clamp(musicVolume + dir * 0.05f, 0f, 1f);
            aplicarVolumenMusica();
        } else if (settingsIndex == 2) {
            sfxVolume = MathUtils.clamp(sfxVolume + dir * 0.05f, 0f, 1f);
        } else if (settingsIndex == 3) {
            int next = selectedDifficulty.ordinal() + dir;
            if (next < 0) {
                next = Game.GameDifficulty.values().length - 1;
            }
            if (next >= Game.GameDifficulty.values().length) {
                next = 0;
            }
            selectedDifficulty = Game.GameDifficulty.values()[next];
            iniciarNuevaPartida();
        }
    }

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

    private void dibujarPantallaInicio() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float centerX = w * 0.5f;
        float centerY = h * 0.5f;

        float cardW = Math.min(760f, w - 80f);
        float cardH = Math.min(520f, h - 70f);
        float cardX = centerX - cardW * 0.5f;
        float cardY = centerY - cardH * 0.5f;
        float compact = MathUtils.clamp(h / 900f, 0.78f, 1f);

        dibujarTarjetaCentro(cardX, cardY, cardW, cardH);
        dibujarBrillo(cardX + 20, cardY + cardH - 78, cardW - 40, 48, 0.08f);

        float y = cardY + cardH - 56f;

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

        float closeY = Math.max(cardY + 22f, y - 72f);
        dibujarBotonCerrar(centerX - closeButtonW * 0.5f, closeY, "Cerrar juego");

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            iniciarNuevaPartida();
            screenState = ScreenState.PLAYING;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            screenState = ScreenState.SETTINGS;
        }
    }

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

    private String dificultadTexto(Game.GameDifficulty difficulty) {
        if (difficulty == Game.GameDifficulty.EASY) {
            return "Facil";
        }
        if (difficulty == Game.GameDifficulty.HARD) {
            return "Dificil";
        }
        return "Normal";
    }

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

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            iniciarNuevaPartida();
            screenState = ScreenState.PLAYING;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            screenState = ScreenState.SETTINGS;
        }
    }
    private void dibujarLaberinto() {
        Labyrinth lab = miJuego.getLabyrinth();
        char[][] mapa = lab.getMatrix();

        int filas = lab.getNumRows();
        int cols = lab.getNumCols();

        int anchoVentana = Gdx.graphics.getWidth();
        int altoVentana = Gdx.graphics.getHeight();
        int panelWidth = getRightPanelWidth();

        int anchoDisponible = anchoVentana - panelWidth - LEFT_MARGIN * 2;
        int altoDisponible = altoVentana - TOP_MARGIN - BOTTOM_MARGIN;

        int tamCasilla = Math.min(anchoDisponible / cols, altoDisponible / filas);
        if (tamCasilla < 12) {
            tamCasilla = 12;
        }

        int anchoTablero = tamCasilla * cols;
        int altoTablero = tamCasilla * filas;

        int offsetX = LEFT_MARGIN + (anchoDisponible - anchoTablero) / 2 + Math.round(shakeX);
        int offsetY = BOTTOM_MARGIN + (altoDisponible - altoTablero) / 2 + Math.round(shakeY);

        int playerRow = -1;
        int playerCol = -1;

        dibujarMarcoTablero(offsetX, offsetY, anchoTablero, altoTablero);

        for (int fila = 0; fila < filas; fila++) {
            for (int col = 0; col < cols; col++) {
                int x = offsetX + col * tamCasilla;
                int y = offsetY + (filas - 1 - fila) * tamCasilla;

                char celda = mapa[fila][col];

                float checker = ((fila + col) % 2 == 0) ? 0.95f : 0.85f;
                float pulse = 0.04f * MathUtils.sin(stateTime * 1.7f + fila * 0.5f + col * 0.4f);
                batch.setColor(checker + pulse, checker + pulse, checker + pulse, 1f);
                batch.draw(texturaSuelo, x, y, tamCasilla, tamCasilla);
                batch.setColor(Color.WHITE);

                if (celda == 'X') {
                    dibujarSombra(x, y, tamCasilla, 0.18f);
                    batch.draw(texturaMuro, x, y, tamCasilla, tamCasilla);
                } else if (celda == 'E') {
                    float exitPulse = 0.68f + 0.30f * MathUtils.sin(stateTime * 3f);
                    batch.setColor(1f, 1f, 1f, exitPulse);
                    batch.draw(texturaSalida, x, y, tamCasilla, tamCasilla);
                    batch.setColor(1f, 1f, 1f, 0.55f + 0.2f * MathUtils.sin(stateTime * 4f));
                    batch.draw(brilloSalida, x - tamCasilla * 0.14f, y - tamCasilla * 0.14f, tamCasilla * 1.28f,
                            tamCasilla * 1.28f);
                    batch.setColor(Color.WHITE);
                } else if (celda == 'M') {
                    float bob = 2f * MathUtils.sin(stateTime * 4f + fila + col);
                    dibujarSombra(x, y, tamCasilla, 0.26f);
                    batch.draw(getMonsterFrame(), x, y + bob, tamCasilla, tamCasilla);
                } else if (celda == 'C') {
                    float bob = 1.5f * MathUtils.sin(stateTime * 5f + fila + col);
                    dibujarSombra(x, y, tamCasilla, 0.30f);
                    batch.draw(getMonsterFrame(), x, y + bob, tamCasilla, tamCasilla);
                    batch.draw(texturaCombate, x, y, tamCasilla, tamCasilla);
                    playerRow = fila;
                    playerCol = col;
                } else if (Character.isDigit(celda)) {
                    playerRow = fila;
                    playerCol = col;
                }
            }
        }

        if (playerRow != -1 && playerCol != -1) {
            float targetX = offsetX + playerCol * tamCasilla;
            float targetY = offsetY + (filas - 1 - playerRow) * tamCasilla;

            playerTargetX = targetX;
            playerTargetY = targetY;

            if (!playerVisualInitialized) {
                playerDrawX = playerTargetX;
                playerDrawY = playerTargetY;
                playerVisualInitialized = true;
            } else {
                playerDrawX += (playerTargetX - playerDrawX) * PLAYER_LERP;
                playerDrawY += (playerTargetY - playerDrawY) * PLAYER_LERP;
            }

            float pulse = movePulseTimer > 0f ? 1f + 0.20f * (movePulseTimer / MOVE_PULSE_DURATION) : 1f;
            float drawSize = tamCasilla * pulse;
            float shift = (drawSize - tamCasilla) * 0.5f;
            float bob = 1.5f * MathUtils.sin(stateTime * 8f);

            dibujarSombra(playerDrawX, playerDrawY, tamCasilla, 0.34f);
            batch.draw(getPlayerFrame(), playerDrawX - shift, playerDrawY - shift + bob, drawSize, drawSize);
        }
    }

    private Texture getPlayerFrame() {
        if (playerFrames.isEmpty()) {
            return texturaSuelo;
        }
        float speed = movePulseTimer > 0f ? 12f : 5f;
        int index = (int) (stateTime * speed) % playerFrames.size();
        return playerFrames.get(index);
    }

    private Texture getMonsterFrame() {
        if (monsterFrames.isEmpty()) {
            return texturaSuelo;
        }
        int index = (int) (stateTime * 6f) % monsterFrames.size();
        return monsterFrames.get(index);
    }

    private void dibujarFondoDinamico() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        batch.draw(fondoPantalla, 0, 0, w, h);

        batch.setColor(0.8f, 0.95f, 1f, 0.13f);
        batch.draw(fondoGradiente, 0, h * 0.36f, w, h * 0.64f);
        batch.setColor(Color.WHITE);

        float stripAlpha = 0.04f + 0.03f * MathUtils.sin(stateTime * 0.7f);
        dibujarBrillo(-120 + (stateTime * 28f) % (w + 240f), h * 0.62f, w * 0.52f, 80f, stripAlpha);
        dibujarBrillo(w - ((stateTime * 36f) % (w + 220f)), h * 0.22f, w * 0.44f, 66f, stripAlpha * 1.1f);
    }

    private void dibujarMarcoTablero(float x, float y, float w, float h) {
        batch.draw(marcoTablero, x - 16, y - 16, w + 32, h + 32);
        batch.setColor(0.65f, 0.80f, 1f, 0.12f);
        batch.draw(panelFondoClaro, x - 10, y + h + 6, w + 20, 4);
        batch.setColor(Color.WHITE);
    }

    private void dibujarTarjetaCentro(float x, float y, float w, float h) {
        batch.draw(panelFondo, x, y, w, h);
        batch.setColor(0.6f, 0.78f, 1f, 0.14f);
        batch.draw(panelFondoClaro, x + 10, y + h - 48, w - 20, 30);
        batch.setColor(Color.WHITE);
    }

    private void dibujarSombra(float x, float y, float tam, float alpha) {
        batch.setColor(0f, 0f, 0f, alpha);
        batch.draw(texPixel, x + tam * 0.18f, y + tam * 0.05f, tam * 0.64f, tam * 0.18f);
        batch.setColor(Color.WHITE);
    }

    private void dibujarBrillo(float x, float y, float w, float h, float alpha) {
        batch.setColor(0.72f, 0.90f, 1f, alpha);
        batch.draw(texPixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    private void dibujarFlashCombate() {
        float alpha = combatFlashTimer / COMBAT_FLASH_DURATION;
        batch.setColor(1f, 0.15f, 0.15f, 0.18f * alpha);
        batch.draw(texPixel, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setColor(Color.WHITE);
    }

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

    private void dibujarPanelDerecho() {
        int anchoVentana = Gdx.graphics.getWidth();
        int altoVentana = Gdx.graphics.getHeight();
        int panelWidth = getRightPanelWidth();

        int panelX = anchoVentana - panelWidth;
        batch.draw(panelFondo, panelX, 0, panelWidth, altoVentana);

        batch.setColor(0.60f, 0.75f, 1f, 0.12f);
        batch.draw(panelFondoClaro, panelX + 12, altoVentana - 74, panelWidth - 24, 36);
        batch.setColor(Color.WHITE);

        float x = panelX + PANEL_PADDING;
        float panelContentWidth = panelWidth - PANEL_PADDING * 2f;
        float y = altoVentana - 24;
        float line = 24f * uiScale;

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

        font.setColor(Color.GOLD);
        font.draw(batch, "ULTIMO EVENTO", x, y);
        y -= line;

        batch.setColor(1f, 1f, 1f, 0.08f);
        batch.draw(panelFondoClaro, x - 6, y - 80, panelContentWidth + 8, 94);
        batch.setColor(Color.WHITE);

        font.setColor(Color.WHITE);
        for (String s : dividirTextoPorAncho(miJuego.getLastLog(), panelContentWidth - 14f)) {
            if (y < 96f) {
                break;
            }
            font.draw(batch, s, x, y);
            y -= line;
        }

        y -= 8;

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

    private int getRightPanelWidth() {
        int desired = Math.round(Gdx.graphics.getWidth() * 0.33f);
        return MathUtils.clamp(desired, MIN_RIGHT_PANEL_WIDTH, MAX_RIGHT_PANEL_WIDTH);
    }

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

    private void dibujarTextoCentrado(String texto, float centerX, float y) {
        glyphLayout.setText(font, texto);
        font.draw(batch, texto, centerX - glyphLayout.width * 0.5f, y);
    }

    private ArrayList<String> dividirTextoPorAncho(String texto, float maxWidth) {
        ArrayList<String> lineas = new ArrayList<>();

        if (texto == null || texto.isEmpty()) {
            lineas.add("");
            return lineas;
        }

        String[] palabras = texto.split(" ");
        StringBuilder actual = new StringBuilder();

        for (String palabra : palabras) {
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

    private void setFontScale(float baseScale) {
        font.getData().setScale(baseScale * uiScale);
    }

    private boolean ratonDentro(float x, float y, float w, float h) {
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean clickEnBotonCerrar() {
        return Gdx.input.justTouched() && ratonDentro(closeButtonX, closeButtonY, closeButtonW, closeButtonH);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();

        for (Texture texture : ownedTextures) {
            texture.dispose();
        }

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
