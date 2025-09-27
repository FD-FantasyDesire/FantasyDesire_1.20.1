package tennouboshiuzume.mods.FantasyDesire.TextUtils.Anim;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.ITextAnimation;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.StyleData;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.TagTextParser;

import java.util.List;

public class RainbowAnimation implements ITextAnimation {
    private final double speed;

    public RainbowAnimation(double speed) { this.speed = speed; }

    @Override
    public MutableComponent apply(String text, StyleData style, long tick) {
        MutableComponent out = Component.literal("");
        for (int i = 0; i < text.length(); i++) {
            float hue = (float)((i * 0.08 + tick * speed / 10.0) % 1.0);
            int rgb = TagTextParser.hsbToRgb(hue, 0.9f, 0.9f);
            StyleData tmp = style.copy();
            tmp.colors = List.of(rgb);
            out.append(TagTextParser.applyStyleToText(text.substring(i, i + 1), tmp));
        }
        return out;
    }
}