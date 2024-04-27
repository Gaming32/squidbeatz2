package io.github.gaming32.squidbeatz2.game;

import io.github.gaming32.squidbeatz2.Constants;
import io.github.gaming32.squidbeatz2.game.assets.AssetManager;
import io.github.gaming32.squidbeatz2.game.assets.Dance;
import io.github.gaming32.squidbeatz2.game.assets.SongAudio;
import io.github.gaming32.squidbeatz2.game.assets.SongInfo;
import io.github.gaming32.squidbeatz2.game.assets.ThemeAssets;
import it.unimi.dsi.fastutil.floats.FloatList;

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

    private final GamePanel gamePanel = new GamePanel();
    private final Timer timer = new Timer(FRAME_TIME_MILLIS, this::updateScreen);
    private final Clip clip;
    private final Visualizer visualizer;

    private int songIndex;
    private long songStartTime;
    private GameTheme theme = GameTheme.DEFAULT;

    public GameFrame() {
        super(Constants.TITLE);

        setContentPane(gamePanel);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    final int state = getExtendedState();
                    if ((state & MAXIMIZED_BOTH) != 0) {
                        dispose();
                        setUndecorated(false);
                        pack();
                        setVisible(true);
                        setExtendedState(state & ~MAXIMIZED_BOTH);
                    } else {
                        dispose();
                        setUndecorated(true);
                        pack();
                        setVisible(true);
                        setExtendedState(state | MAXIMIZED_BOTH);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_T) { // TODO: These keybinds need to be remappable
                    theme = GameTheme.values()[(theme.ordinal() + 1) % GameTheme.values().length];
                } else if (e.getKeyCode() == KeyEvent.VK_K) {
                    if (songStartTime > 0) {
                        songStartTime = 0;
                        clip.stop();
                    } else {
                        playSong();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    final int shift = e.getKeyCode() == KeyEvent.VK_LEFT ? -1 : 1;
                    songIndex = Math.floorMod(songIndex + shift, AssetManager.getSongs().size());
                    if (songStartTime > 0) {
                        playSong();
                    }
                }
            }
        });

        try {
            clip = AudioSystem.getClip();
        } catch (LineUnavailableException e) {
            throw new IllegalStateException(e);
        }
        visualizer = new Visualizer(clip::getFramePosition, new short[0]);

        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
    }

    private void updateScreen(ActionEvent e) {
        gamePanel.repaint();
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
            final AffineTransform oldTransform = g.getTransform();
            g.scale(0.67, 1);
            g.setFont(AssetManager.getGameFont().deriveFont(64f));
            final int visualSongIndex = songIndex < 57 ? songIndex + 1 : songIndex - 57 + 80;
            g.drawString(visualSongIndex + ".", 498, 128);
            g.drawString(AssetManager.getSongName(songInfo.songId()), 600, 128);
            g.setTransform(oldTransform);

            g.drawImage(themeAssets.mask, 0, 0, null);
        }
    }
}
