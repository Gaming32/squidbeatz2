package io.github.gaming32.squidbeatz2.game.config;

import com.google.common.collect.ImmutableMap;
import io.github.gaming32.squidbeatz2.game.GameMain;
import io.github.gaming32.squidbeatz2.util.TypedField;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

public class KeybindConfig {
    public static KeyBinding fullscreen = new KeyBinding(KeyEvent.VK_F11);
    public static KeyBinding changeTheme = KeyBinding.UNBOUND; // TODO: Update when themes actually work
    public static KeyBinding startStop = new KeyBinding(KeyEvent.VK_K);
    public static KeyBinding prevSong = new KeyBinding(KeyEvent.VK_LEFT);
    public static KeyBinding nextSong = new KeyBinding(KeyEvent.VK_RIGHT);
    public static KeyBinding chooseSong = new KeyBinding(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK);

    public static final Map<String, TypedField<KeyBinding>> KEYBINDINGS = createKeyBindingsMap();

    private static Map<String, TypedField<KeyBinding>> createKeyBindingsMap() {
        return Arrays.stream(KeybindConfig.class.getDeclaredFields())
            .filter(f -> f.getType() == KeyBinding.class)
            .collect(ImmutableMap.toImmutableMap(
                Field::getName,
                field -> new TypedField<>(field, KeyBinding.class)
            ));
    }

    public static void save() throws IOException {
        final Properties properties = new Properties(KEYBINDINGS.size() * 2);
        for (final var entry : KEYBINDINGS.entrySet()) {
            final KeyBinding keyBinding = entry.getValue().getStatic();
            properties.setProperty(entry.getKey() + ".key", Integer.toString(keyBinding.key));
            if (keyBinding.modifiers != 0) {
                properties.setProperty(entry.getKey() + ".modifiers", Integer.toString(keyBinding.modifiers));
            }
        }
        try (OutputStream os = Files.newOutputStream(getSaveFile())) {
            properties.store(os, null);
        }
    }

    public static void load() throws IOException {
        final Properties properties = new Properties(KEYBINDINGS.size() * 2);
        try (InputStream is = Files.newInputStream(getSaveFile())) {
            properties.load(is);
        } catch (NoSuchFileException e) {
            return;
        }
        for (final var entry : KEYBINDINGS.entrySet()) {
            final String keyString = properties.getProperty(entry.getKey() + ".key");
            if (keyString == null) continue;
            final int key;
            try {
                key = Integer.decode(keyString);
            } catch (NumberFormatException e) {
                System.err.println("Invalid number for key for " + entry.getKey() + ": " + keyString);
                continue;
            }
            int modifiers = 0;
            final String modifiersString = properties.getProperty(entry.getKey() + ".modifiers");
            if (modifiersString != null) {
                try {
                    modifiers = Integer.decode(modifiersString);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number for modifiers for " + entry.getKey() + ": " + keyString);
                }
            }
            entry.getValue().setStatic(new KeyBinding(key, modifiers));
        }
    }

    public static Path getSaveFile() {
        return GameMain.getDataDir().resolve("keybindings.properties");
    }

    public record KeyBinding(int key, int modifiers) {
        public static final KeyBinding UNBOUND = new KeyBinding(0);

        public KeyBinding(int key) {
            this(key, 0);
        }

        public boolean matches(KeyEvent event) {
            if (key == 0) {
                return false;
            }
            return key == event.getKeyCode() && modifiers == event.getModifiersEx();
        }

        @Override
        public String toString() {
            if (key == 0) {
                return "Unbound";
            }
            final String keyText = KeyEvent.getKeyText(key);
            if (modifiers == 0) {
                return keyText;
            }
            return KeyEvent.getModifiersExText(modifiers) + '+' + keyText;
        }
    }
}
