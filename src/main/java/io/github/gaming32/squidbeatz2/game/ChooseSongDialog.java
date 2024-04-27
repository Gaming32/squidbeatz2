package io.github.gaming32.squidbeatz2.game;

import io.github.gaming32.squidbeatz2.game.assets.AssetManager;
import io.github.gaming32.squidbeatz2.game.assets.SongInfo;
import io.github.gaming32.squidbeatz2.game.assets.TranslationCategory;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

public class ChooseSongDialog extends JDialog {
    private final GameFrame gameFrame;

    public ChooseSongDialog(GameFrame gameFrame) {
        super(gameFrame, "Choose Song", true);
        this.gameFrame = gameFrame;

        initComponents();

        rootPane.registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
    }

    private void initComponents() {
        final Vector<String> songVector = new Vector<>();
        final JList<String> songList = new JList<>(songVector);
        for (final SongInfo song : AssetManager.getSongs()) {
            songVector.add(
                GameFrame.convertSongIndex(songVector.size()) + ". " +
                AssetManager.getTranslation(TranslationCategory.SONG, song.songId())
            );
        }
        songList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    gameFrame.songIndex = songList.getSelectedIndex();
                    dispose();
                }
            }
        });
        final JScrollPane songPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        songPane.setViewportView(songList);

        final JButton okButton = new JButton("Ok");
        okButton.addActionListener(e -> {
            gameFrame.songIndex = songList.getSelectedIndex();
            dispose();
        });
        okButton.setEnabled(false);
        songList.addListSelectionListener(e -> okButton.setEnabled(true));

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        final GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
            .addComponent(songPane)
            .addGroup(layout.createSequentialGroup()
                .addComponent(okButton)
                .addComponent(cancelButton)
            )
        );
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addComponent(songPane)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(okButton)
                .addComponent(cancelButton)
            )
        );
    }
}
