package tennouboshiuzume.mods.FantasyDesire.TextUtils.Anim;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.ComponentUtils;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.ITextAnim;

public class StaticGradientAnim implements ITextAnim {
    @Override
    public MutableComponent apply(MutableComponent component, long worldTime, long speed, TextColor[] colors) {
        String text = component.getString();
        int length = text.length();
        if (length == 0 || colors == null || colors.length == 0) return component;

        return ComponentUtils.forEachChar(component, (c, baseStyle, index) -> {
            float t = (float) index / (float) Math.max(1, length - 1);

            // 计算在哪两个颜色之间插值
            int seg = (int) (t * (colors.length - 1));
            float localT = (t * (colors.length - 1)) - seg;

            TextColor c1 = colors[seg];
            TextColor c2 = colors[Math.min(seg + 1, colors.length - 1)];

            int blended = lerpColor(c1.getValue(), c2.getValue(), localT);

            return Component.literal(String.valueOf(c))
                    .setStyle(baseStyle.withColor(TextColor.fromRgb(blended)));
        });
    }

    // 线性插值两个颜色
    private int lerpColor(int rgb1, int rgb2, float t) {
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (r << 16) | (g << 8) | b;
    }
}