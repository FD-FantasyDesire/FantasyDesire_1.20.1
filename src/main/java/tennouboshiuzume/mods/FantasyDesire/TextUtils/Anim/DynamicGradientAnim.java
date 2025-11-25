package tennouboshiuzume.mods.FantasyDesire.TextUtils.Anim;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.ComponentUtils;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.ITextAnim;

public class DynamicGradientAnim implements ITextAnim {
//    动态渐变
    @Override
    public MutableComponent apply(MutableComponent component, long worldTime, long speed, TextColor[] colors) {
        String text = component.getString();
        int length = text.length();
        if (length == 0 || colors == null || colors.length == 0) return component;

        return ComponentUtils.forEachChar(component, (c, baseStyle, index) -> {
            // t 表示当前字符在文本中的位置（0~1）
            float t = (float) index / Math.max(1, length - 1);

            // 颜色循环偏移
            float shift = (worldTime / (float) speed) * 0.02f;
            float pos = (t + shift) % 1.0f;

            // 根据 pos 找出当前位于哪两个颜色之间
            float scaled = pos * (colors.length - 1);
            int seg = (int) scaled;
            float localT = scaled - seg;

            TextColor c1 = colors[seg];
            TextColor c2 = colors[Math.min(seg + 1, colors.length - 1)];

            int blended = lerpColor(c1.getValue(), c2.getValue(), localT);

            return Component.literal(String.valueOf(c))
                    .setStyle(baseStyle.withColor(TextColor.fromRgb(blended)));
        });
    }

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
