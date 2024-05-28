package io.github.gaming32.squidbeatz2.game;

import io.github.gaming32.squidbeatz2.Constants;
import io.github.gaming32.squidbeatz2.game.assets.AssetManager;
import io.github.gaming32.squidbeatz2.game.assets.FileGetter;
import io.github.gaming32.squidbeatz2.game.config.KeybindConfig;
import it.unimi.dsi.fastutil.floats.FloatConsumer;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.SplashScreen;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class GameMain {
    private static Path dataDir;

    public static void main(String[] args) {
        final Path dataParent = getDataParent();
        final FileGetter<?> fileGetter;
        try {
            fileGetter = createFileGetter(dataParent);
            dataDir = dataParent.resolve("squidbeatz2");
            Files.createDirectories(dataDir);
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(
                null,
                e.getLocalizedMessage(),
                Constants.TITLE, JOptionPane.ERROR_MESSAGE
            );
            return;
        } catch (FileAlreadyExistsException e) {
            JOptionPane.showMessageDialog(
                null,
                "Data folder (" + dataDir + ") is already a file",
                Constants.TITLE, JOptionPane.ERROR_MESSAGE
            );
            return;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                null,
                "Failed to create data folder: " + e,
                Constants.TITLE, JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        System.out.println("Loading assets...");
        final long assetLoadTime = AssetManager.loadAssets(fileGetter, createProgressConsumer());
        System.out.println("Loaded assets in " + Duration.ofNanos(assetLoadTime));

        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(AssetManager.getGameFont());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Failed to load system LaF");
            e.printStackTrace();
        }

        try {
            KeybindConfig.load();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                null,
                "Failed to load keybindings: " + e,
                Constants.TITLE, JOptionPane.ERROR_MESSAGE
            );
        }

        try {
            KeybindConfig.save();
        } catch (IOException e) {
            System.err.println("Failed to save keybindings");
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new GameFrame().setVisible(true));
    }

    public static Path getDataDir() {
        return dataDir;
    }

    public static FileGetter<?> createFileGetter(Path dataParent) {
        Path suyuDir = dataParent.resolve("suyu");
        if (!Files.exists(suyuDir)) {
            final Path tryDir = dataParent.resolve("yuzu");
            if (!Files.exists(tryDir)) {
                throw new IllegalStateException("Suyu directory (" + suyuDir + ") not found. Please install Suyu.");
            }
            suyuDir = tryDir;
        }

        final Path romfsPath = findRomfsPath(suyuDir, Constants.SPLATOON_2_TITLES);
        if (romfsPath == null) {
            throw new IllegalStateException("Splatoon 2 dump not found. Did you forget to dump Splatoon 2?");
        }
        final Path octoExpansionPath = findRomfsPath(suyuDir, Constants.OCTO_EXPANSION_TITLES);

        FileGetter<?> result = FileGetter.ofDirectory(romfsPath);
        if (octoExpansionPath != null) {
            result = result.orElse(FileGetter.ofDirectory(octoExpansionPath));
        }
        return result;
    }

    private static Path findRomfsPath(Path suyuDir, List<String> titleList) {
        final Path dumpDir = suyuDir.resolve("dump");
        for (final String title : titleList) {
            final Path result = dumpDir.resolve(title).resolve("romfs");
            if (Files.isDirectory(result)) {
                return result;
            }
        }
        return null;
    }

    public static Path getDataParent() {
        final String dataParentEnvName = Constants.IS_WINDOWS ? "APPDATA" : "XDG_DATA_HOME";
        final String dataParentEnv = System.getenv(dataParentEnvName);
        return dataParentEnv != null
            ? Path.of(dataParentEnv)
            : Path.of(System.getProperty("user.home"), ".local", "share");
    }

    private static FloatConsumer createProgressConsumer() {
        final SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash == null) {
            return p -> {};
        }
        return progress -> {
            synchronized (splash) {
                final Graphics2D g = splash.createGraphics();
                final Dimension bounds = splash.getSize();
                g.setColor(Color.GREEN);
                g.fillRect(0, bounds.height - 10, (int)(bounds.width * progress), 10);
                g.dispose();
                splash.update();
            }
        };
    }
}
