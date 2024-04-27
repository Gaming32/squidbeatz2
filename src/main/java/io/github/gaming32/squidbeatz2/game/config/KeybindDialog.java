package io.github.gaming32.squidbeatz2.game.config;

import io.github.gaming32.squidbeatz2.Constants;
import io.github.gaming32.squidbeatz2.game.GameFrame;
import io.github.gaming32.squidbeatz2.game.assets.AssetManager;
import io.github.gaming32.squidbeatz2.util.TypedField;
import it.unimi.dsi.fastutil.ints.IntSet;

import javax.swing.*;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class KeybindDialog extends JDialog {
    private ActiveKeybinding activeKeybinding;
    private final BindListener bindListener = new BindListener();

    public KeybindDialog(GameFrame gameFrame) {
        super(gameFrame, "Change Keybindings", true);

        initComponents();

        rootPane.registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        addKeyListener(bindListener);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
    }

    private void initComponents() {
        final JPanel optionsPanel = createOptions();

        final JButton okButton = new JButton("Ok");
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        okButton.addActionListener(e -> dispose());
        okButton.addKeyListener(bindListener);

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        add(optionsPanel);
        add(okButton);
    }

    private JPanel createOptions() {
        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayout(0, 2, 5, 5));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        for (final var entry : KeybindConfig.KEYBINDINGS.entrySet()) {
            final var keybinding = entry.getValue();

            final JLabel label = new JLabel(AssetManager.getResourceBundle().getString("keybind." + entry.getKey()));
            optionsPanel.add(label);

            final JTextField textField = new JTextField(keybinding.getStatic().toString());
            textField.setEditable(false);
            textField.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        e.consume();
                        activeKeybinding = new ActiveKeybinding(keybinding, textField);
                        textField.setEditable(true);
                    }
                }
            });
            textField.addKeyListener(bindListener);
            optionsPanel.add(textField);
        }
        return optionsPanel;
    }

    private record ActiveKeybinding(TypedField<KeybindConfig.KeyBinding> keybinding, JTextField textField) {
    }

    private class BindListener extends KeyAdapter {
        private static final IntSet MODIFIER_KEYS = IntSet.of(
            KeyEvent.VK_META, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_SHIFT, KeyEvent.VK_ALT_GRAPH
        );

        @Override
        public void keyPressed(KeyEvent e) {
            if (activeKeybinding == null || MODIFIER_KEYS.contains(e.getKeyCode())) return;
            e.consume();
            final var newBinding = new KeybindConfig.KeyBinding(e.getKeyCode(), e.getModifiersEx());
            activeKeybinding.keybinding.setStatic(newBinding);
            activeKeybinding.textField.setEditable(false);
            activeKeybinding.textField.setText(newBinding.toString());
            activeKeybinding = null;
            try {
                KeybindConfig.save();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                    KeybindDialog.this,
                    "Failed to save keybindings: " + ex,
                    Constants.TITLE, JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}
