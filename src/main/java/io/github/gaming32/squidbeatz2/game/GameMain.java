package io.github.gaming32.squidbeatz2.game;

import io.github.gaming32.squidbeatz2.Constants;
import io.github.gaming32.squidbeatz2.game.assets.AssetManager;
import io.github.gaming32.squidbeatz2.game.assets.FileGetter;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class GameMain {
    public static void main(String[] args) {
        final FileGetter<?> fileGetter;
        try {
            fileGetter = createFileGetter();
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), Constants.TITLE, JOptionPane.ERROR_MESSAGE);
            return;
        }

        System.out.println("Loading assets...");
        final long assetLoadTime = AssetManager.loadAssets(fileGetter);
        System.out.println("Loaded assets in " + Duration.ofNanos(assetLoadTime));

        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(AssetManager.getGameFont());
    }

    public static FileGetter<?> createFileGetter() {
        final String dataParentEnvName = Constants.IS_WINDOWS ? "APPDATA" : "XDG_DATA_HOME";
        final String dataParentEnv = System.getenv(dataParentEnvName);
        final Path dataParent = dataParentEnv != null
            ? Path.of(dataParentEnv)
            : Path.of(System.getProperty("user.home"), ".local", "share");
        Path suyuDir = dataParent.resolve("suyu");
        if (!Files.exists(suyuDir)) {
            final Path tryDir = dataParent.resolve("yuzu");
            if (!Files.exists(tryDir)) {
                throw new IllegalStateException("Suyu directory (" + suyuDir + ") not found. Please install Suyu.");
            }
            suyuDir = tryDir;
        }

        final Path romfsPath = suyuDir.resolve("dump/0100F8F0000A2000/romfs");
        if (!Files.exists(romfsPath)) {
            throw new IllegalStateException("Splatoon 2 dump not found. Did you forget to dump Splatoon 2?");
        }
        final Path octoExpansionPath = suyuDir.resolve("dump/0100F8F0000A3065/romfs");

        FileGetter<?> result = FileGetter.ofDirectory(romfsPath);
        if (Files.exists(octoExpansionPath)) {
            result = result.orElse(FileGetter.ofDirectory(octoExpansionPath));
        }
        return result;
    }
}
