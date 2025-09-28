package tennouboshiuzume.mods.FantasyDesire.TextUtils.Anim;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.ComponentUtils;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.ITextAnim;

import java.awt.*;

public class RainbowAnim implements ITextAnim {
    @Override
    public MutableComponent apply(MutableComponent component, long worldTime, long speed, TextColor[] colors) {
        return ComponentUtils.forEachChar(component, (c, baseStyle, index) -> {
            // 动态彩虹色
            float hue = ((index * 0.1f) + (worldTime / (float) speed) * 0.02f) % 1.0f;
            Color awtColor = Color.getHSBColor(hue, 1.0f, 1.0f);
            TextColor color = TextColor.fromRgb(awtColor.getRGB());

            // 保留原有样式，只额外叠加颜色
            return Component.literal(String.valueOf(c))
                    .setStyle(baseStyle.withColor(color));
        });
    }
}