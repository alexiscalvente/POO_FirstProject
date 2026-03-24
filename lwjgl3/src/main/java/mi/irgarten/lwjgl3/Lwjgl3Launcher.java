package mi.irgarten.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import mi.irgarten.MainGame;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        try {
            createApplication(getDefaultConfiguration());
        } catch (Throwable firstError) {
            System.err.println("[Launcher] OpenGL nativo no disponible. Reintentando con ANGLE (DirectX)...");
            firstError.printStackTrace();
            createApplication(getAngleFallbackConfiguration());
        }
    }

    private static Lwjgl3Application createApplication(Lwjgl3ApplicationConfiguration config) {
        return new Lwjgl3Application(new MainGame(), config);
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("IrgartenGrafico");
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL20, 2, 0);
        return config;
    }

    private static Lwjgl3ApplicationConfiguration getAngleFallbackConfiguration() {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("IrgartenGrafico (ANGLE)");
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        // ANGLE usa DirectX en Windows y evita depender del driver OpenGL nativo.
        config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.ANGLE_GLES20, 2, 0);
        return config;
    }
}
