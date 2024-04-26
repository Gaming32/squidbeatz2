package io.github.gaming32.squidbeatz2.game;

import io.github.gaming32.squidbeatz2.Constants;
import io.github.gaming32.squidbeatz2.game.assets.AssetManager;
import io.github.gaming32.squidbeatz2.game.assets.SongInfo;
import io.github.gaming32.squidbeatz2.game.assets.ThemeAssets;

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
import java.awt.image.BufferedImage;
import java.util.List;

public class GameFrame extends JFrame {
    public static final int FRAME_TIME_MILLIS = 1000 / 60;
    public static final int FRAME_TIME_NANOS = 1_000_000_000 / 60;
    public static final int DANCE_FRAME_TIME = 1_000_000_000 / 30;

    private final GamePanel gamePanel = new GamePanel();
    private final Timer timer = new Timer(FRAME_TIME_MILLIS, this::updateScreen);

    private int songIndex;
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
                } else if (e.getKeyCode() == KeyEvent.VK_T) { // TODO: Temp testing code
                    theme = GameTheme.values()[(theme.ordinal() + 1) % GameTheme.values().length];
                }
            }
        });

        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
    }

    private void updateScreen(ActionEvent e) {
        gamePanel.repaint();
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

        GamePanel() {
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(1280, 720));
        }

        @Override
        public void paint(Graphics g) {
            final Graphics2D g2d = (Graphics2D)g;
            g2d.scale(getWidth() / 1920.0, getHeight() / 1080.0);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            draw(g2d);
            g2d.dispose();
        }

        private void draw(Graphics2D g) {
            final long time = System.nanoTime() - gameStart;
            final SongInfo songInfo = AssetManager.getSongs().get(songIndex);
            final List<BufferedImage> dance = AssetManager.getDance(songInfo.dance());
            final BufferedImage danceImage = dance.get((int)(time / DANCE_FRAME_TIME % dance.size()));

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, 1920, 1080);

            g.setFont(AssetManager.getGameFont().deriveFont(32f));
            g.setColor(Color.WHITE);
            g.drawString(Long.toString(time), 160, 100);

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

            g.drawImage(danceImage, 1159, 177, danceImage.getWidth() * 2, danceImage.getHeight() * 2, null);

            g.drawImage(themeAssets.mask, 0, 0, null);
        }
    }
}
