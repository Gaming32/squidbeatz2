package io.github.gaming32.squidbeatz2.game.assets;

import io.github.gaming32.squidbeatz2.bntx.Texture;
import io.github.gaming32.squidbeatz2.texture.TextureConverter;

import java.awt.image.BufferedImage;

public class ThemeAssets {
    public BufferedImage caption;
    public BufferedImage captionStyleChange;
    public BufferedImage manual;
    public BufferedImage mask;
    public BufferedImage playIcon;
    public BufferedImage playIconDark;
    public BufferedImage stopIcon;
    public BufferedImage equalizerFrame;
    public BufferedImage blockHigh;
    public BufferedImage blockNormal;
    public BufferedImage blockOff;
    public BufferedImage notationBars;

    public void load(Iterable<Texture> textures) {
        for (final Texture texture : textures) {
            final BufferedImage image = TextureConverter.toBufferedImage(texture);
            switch (texture.name) {
                case "Base_Caption" -> caption = image;
                case "Caption_StyleChange" -> captionStyleChange = image;
                case "Base_Manual" -> manual = image;
                case "Mask" -> mask = image;
                case "Playmark" -> playIcon = image;
                case "Playmark_Dark" -> playIconDark = image;
                case "StopMark" -> stopIcon = image;
                case "IcolizerFrame" -> equalizerFrame = image;
                case "Block_high" -> blockHigh = image;
                case "Block_normal" -> blockNormal = image;
                case "Block_off" -> blockOff = image;
                case "Base_notation" -> notationBars = image;
            }
        }
    }
}
