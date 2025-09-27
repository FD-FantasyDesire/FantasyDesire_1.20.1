package tennouboshiuzume.mods.FantasyDesire.TextUtils.Anim;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.ITextAnimation;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.StyleData;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.TagTextParser;

import java.util.List;

public class StaticGradientAnimation implements ITextAnimation {
    @Override
    public MutableComponent apply(String text, StyleData style, long tick) {
        MutableComponent out = Component.literal("");
        int len = text.length();
        for (int i = 0; i < len; i++) {
            int rgb = style.colors != null && !style.colors.isEmpty() ?
                    interpolateColor(style.colors, i, len) : 0xFFFFFF;
            StyleData tmp = style.copy();
            tmp.colors = List.of(rgb);
            out.append(TagTextParser.applyStyleToText(text.substring(i, i + 1), tmp));
        }
        return out;
    }

    private int interpolateColor(List<Integer> colors, int index, int length) {
        if (colors.size() == 1) return colors.get(0);
        double f = (double) index / (length - 1);
        int seg = (int) Math.floor(f * (colors.size() - 1));
        double t = f * (colors.size() - 1) - seg;
        int c1 = colors.get(seg);
        int c2 = colors.get(Math.min(seg + 1, colors.size() - 1));
        return TagTextParser.lerpColor(c1, c2, t);
    }
}
