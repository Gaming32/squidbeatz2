package io.github.gaming32.squidbeatz2.game;

import io.github.gaming32.squidbeatz2.Constants;
import io.github.gaming32.squidbeatz2.game.assets.AssetManager;
import io.github.gaming32.squidbeatz2.game.assets.Dance;
import io.github.gaming32.squidbeatz2.game.assets.SongAudio;
import io.github.gaming32.squidbeatz2.game.assets.SongInfo;
import io.github.gaming32.squidbeatz2.game.assets.ThemeAssets;
import io.github.gaming32.squidbeatz2.game.assets.TranslationCategory;
import io.github.gaming32.squidbeatz2.game.config.KeybindConfig;
import io.github.gaming32.squidbeatz2.game.config.KeybindDialog;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public class GameFrame extends JFrame {
    public static final int FRAME_TIME_MILLIS = 1000 / 60;
    public static final int FRAME_TIME_NANOS = 1_000_000_000 / 60;
    public static final int DANCE_FRAME_TIME = 1_000_000_000 / 30; // Figure out what the actual frame rates are for each animation
    public static final int PLAY_BLINK_TIME = 1_500_000_000;

    private static final List<String> CAPTION_LABELS = List.of("0", "1", "2", "9");

    private final GamePanel gamePanel = new GamePanel();
    private final JMenuBar menuBar = new JMenuBar();
    private final Timer timer = new Timer(FRAME_TIME_MILLIS, this::updateScreen);
    private final Clip clip;
    private final Visualizer visualizer;

    int songIndex;
    private long songStartTime;
    private GameTheme theme = GameTheme.DEFAULT;

    public GameFrame() {
        super(Constants.TITLE);

        setContentPane(gamePanel);
        setJMenuBar(menuBar);
        setupMenu();

        addKeyListener(new KeyAdapter() {
            private final IntSet heldKeys = new IntOpenHashSet();

            @Override
            public void keyPressed(KeyEvent e) {
                if (!heldKeys.add(e.getKeyCode())) return;
                if (KeybindConfig.fullscreen.matches(e)) {
                    e.consume();
                    final int state = getExtendedState();
                    if ((state & MAXIMIZED_BOTH) != 0) {
                        dispose();
                        setUndecorated(false);
                        setJMenuBar(menuBar);
                        pack();
                        setExtendedState(state & ~MAXIMIZED_BOTH);
                        setVisible(true);
                    } else {
                        dispose();
                        setUndecorated(true);
                        setJMenuBar(null);
                        pack();
                        setExtendedState(state | MAXIMIZED_BOTH);
                        setVisible(true);
                    }
                } else if (KeybindConfig.changeTheme.matches(e)) {
                    e.consume();
                    theme = GameTheme.values()[(theme.ordinal() + 1) % GameTheme.values().length];
                } else if (KeybindConfig.startStop.matches(e)) {
                    e.consume();
                    if (songStartTime > 0) {
                        songStartTime = 0;
                        clip.stop();
                    } else {
                        playSong();
                    }
                } else if (KeybindConfig.prevSong.matches(e) || KeybindConfig.nextSong.matches(e)) {
                    e.consume();
                    final int shift = KeybindConfig.prevSong.matches(e) ? -1 : 1;
                    songIndex = Math.floorMod(songIndex + shift, AssetManager.getSongs().size());
                    if (songStartTime > 0) {
                        playSong();
                    }
                } else if (KeybindConfig.chooseSong.matches(e)) {
                    e.consume();
                    heldKeys.remove(KeybindConfig.chooseSong.key()); // Because the new dialog opens, we never get a release event
                    new ChooseSongDialog(GameFrame.this).setVisible(true);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                heldKeys.remove(e.getKeyCode());
            }
        });

        try {
            clip = AudioSystem.getClip();
        } catch (LineUnavailableException e) {
            throw new IllegalStateException(e);
        }
        visualizer = new Visualizer(() -> {
            int frame = clip.getFramePosition();
            final SongAudio audio = AssetManager.getSongAudio(AssetManager.getSongs().get(songIndex).songId());
            if (audio == null || !audio.loop()) {
                return frame;
            }
            final int loopStart = audio.loopStart();
            final int loopEnd = audio.loopEnd() - 1;
            while (frame >= loopEnd) {
                frame = frame - loopEnd + loopStart;
            }
            return frame;
        }, new short[0]);

        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
    }

    private void updateScreen(ActionEvent e) {
        gamePanel.repaint();
    }

    private void setupMenu() {
        setupFileMenu();
        setupOptionsMenu();
    }

    private void setupFileMenu() {
        final JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);

        final JMenuItem chooseSongItem = new JMenuItem("Choose Song");
        chooseSongItem.addActionListener(e -> new ChooseSongDialog(this).setVisible(true));
        fileMenu.add(chooseSongItem);

        fileMenu.addSeparator();

        final JMenuItem quitItem = new JMenuItem("Quit", KeyEvent.VK_Q);
        quitItem.addActionListener(e -> dispose());
        fileMenu.add(quitItem);
    }

    private void setupOptionsMenu() {
        final JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic(KeyEvent.VK_O);
        menuBar.add(optionsMenu);

        final JMenuItem editKeybindingsItem = new JMenuItem("Edit Keybindings");
        editKeybindingsItem.addActionListener(e -> new KeybindDialog(this).setVisible(true));
        optionsMenu.add(editKeybindingsItem);
    }

    private void playSong() {
        clip.close();
        final SongInfo song = AssetManager.getSongs().get(songIndex);
        final SongAudio audio = AssetManager.getSongAudio(song.songId());
        if (audio == null) return;
        try {
            final AudioInputStream ais = audio.getAudioInputStream();
            clip.open(ais);
            ((FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN)).setValue(20f * (float)Math.log10(Constants.VOLUME));
            if (audio.loop()) {
                clip.setLoopPoints(audio.loopStart(), audio.loopEnd() - 1);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }
            visualizer.setSamples(audio.monoSamples());
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        songStartTime = System.nanoTime();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void show() {
        super.show();
        timer.start();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void hide() {
        timer.stop();
        super.hide();
    }

    private class GamePanel extends JPanel {
        final long gameStart = System.nanoTime();
        int lastDanceFrame, dancePauseFrame;
        Dance lastDance;

        GamePanel() {
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(1280, 720));
        }

        @Override
        public void paint(Graphics g) {
            final Graphics2D g2d = (Graphics2D)g;
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.scale(getWidth() / 1920.0, getHeight() / 1080.0);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            draw(g2d);
            g2d.dispose();
        }

        private void draw(Graphics2D g) {
            final AffineTransform transform = g.getTransform();

            final long time = System.nanoTime() - gameStart;
            final long playTime = System.nanoTime() - songStartTime;
            final SongInfo songInfo = AssetManager.getSongs().get(songIndex);
            final ThemeAssets themeAssets = AssetManager.getThemeAssets(theme);

            if (theme.fullscreenManual) {
                g.drawImage(themeAssets.manual, 0, 0, null);
            } else {
                final int manualWidth = themeAssets.manual.getWidth();
                final int manualHeight = themeAssets.manual.getHeight();
                g.drawImage(
                    themeAssets.manual,
                    1920 - 150 - manualWidth + 7, 1080 - 47 - manualHeight, 1920 - 150, 1080 - 47,
                    0, 0, manualWidth - 7, manualHeight,
                    null
                );
            }

            final Point captionPos = switch (theme) {
                case DEFAULT -> new Point(1476, 91);
                case PEARL -> new Point(1562, 6);
                case MARINA -> new Point(1491 + 3, 105 + 22);
            };
            g.drawImage(themeAssets.caption, captionPos.x, captionPos.y, null);
            g.drawImage(themeAssets.captionStyleChange, captionPos.x, captionPos.y + themeAssets.caption.getHeight(), null);
            g.setColor(Color.WHITE);
            g.scale(0.95, 1);
            g.setFont(AssetManager.getGameFont().deriveFont(32f));
            final float textX = captionPos.x / 0.95f + 73f;
            float textY = captionPos.y + 40;
            for (final String label : CAPTION_LABELS) {
                final String translated = AssetManager.getTranslation(TranslationCategory.LABEL, label);
                g.drawString(translated, textX, textY);
                textY += 60f;
            }
            g.setTransform(transform);

            final BufferedImage playStatusImage;
            if (songStartTime == 0) {
                playStatusImage = themeAssets.stopIcon;
            } else if (time % (PLAY_BLINK_TIME * 2L) < PLAY_BLINK_TIME) {
                playStatusImage = themeAssets.playIcon;
            } else {
                playStatusImage = themeAssets.playIconDark;
            }
            g.drawImage(playStatusImage, 190, 82, null);

            final List<BufferedImage> danceFrames = AssetManager.getDance(songInfo.dance());
            final int danceFrame;
            if (songInfo.dance() != lastDance) {
                lastDance = songInfo.dance();
                lastDanceFrame = dancePauseFrame = 0;
            }
            if (clip.isRunning()) {
                lastDanceFrame = danceFrame = (int)((playTime / DANCE_FRAME_TIME + dancePauseFrame) % danceFrames.size());
            } else {
                danceFrame = dancePauseFrame = lastDanceFrame;
            }
            final BufferedImage danceImage = danceFrames.get(danceFrame);
            g.drawImage(danceImage, 1159, 177, danceImage.getWidth() * 2, danceImage.getHeight() * 2, null);

            g.drawImage(themeAssets.equalizerFrame, 153, 162, null);
            final FloatList points;
            if (clip.isRunning()) {
                visualizer.update();
                points = visualizer.getPoints();
            } else {
                points = FloatList.of();
            }
            for (int visualizerX = 0; visualizerX < 16; visualizerX++) {
                final int frameX = 188 + 57 * visualizerX;
                final int top;
                if (points.isEmpty()) {
                    top = -1;
                } else {
                    final int pointIndex = visualizerX + 121;
                    final float point = (points.getFloat(pointIndex - 1) + points.getFloat(pointIndex)) / 2f;
                    final float multiplier = (float)Math.cos((visualizerX - 1f) / 8 * Math.PI) * 5f + 17f;
                    top = (int)(-point / 150f * multiplier - 6);
                }
                for (int visualizerY = 0; visualizerY < 16; visualizerY++) {
                    final int frameY = 192 + 21 * visualizerY;
                    final int pointOrigin = 16 - visualizerY;
                    final BufferedImage image;
                    if (pointOrigin == top) {
                        image = themeAssets.blockHigh;
                    } else if (pointOrigin < top) {
                        image = themeAssets.blockNormal;
                    } else {
                        image = themeAssets.blockOff;
                    }
                    // Nintendo's is shifted right by half a pixel. There's no way to replicate this with Java 2D, however.
                    g.drawImage(image, frameX, frameY, null);
                }
            }

            g.drawImage(themeAssets.notationBars, 150, 667, null);

            g.setColor(new Color(0xE02E9D));
            g.scale(0.67, 1);
            g.setFont(AssetManager.getGameFont().deriveFont(64f));
            g.drawString(songInfo.getDisplayNumber(songIndex) + ".", 498, 128);
            g.drawString(AssetManager.getTranslation(TranslationCategory.SONG, songInfo.songId()), 600, 128);
            g.setTransform(transform);

            g.drawImage(themeAssets.mask, 0, 0, null);
        }
    }
}
