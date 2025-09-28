package tennouboshiuzume.mods.FantasyDesire.TextUtils.Anim;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.ComponentUtils;
import tennouboshiuzume.mods.FantasyDesire.TextUtils.ITextAnim;

public class WaveBoldAnim implements ITextAnim {
    @Override
    public MutableComponent apply(MutableComponent component, long worldTime, long speed, TextColor[] colors) {
        int length = component.getString().length();
        if (length == 0) return component;

        int mid = length / 2;

        // 根据时间扩散半径
        int radius = (int) ((worldTime / speed) % (mid + 2));

        return ComponentUtils.forEachChar(component, (c, baseStyle, index) -> {
            // 只有在波峰位置（mid ± radius）时加粗
            boolean bold = (index == mid - radius || index == mid + radius);

            return Component.literal(String.valueOf(c))
                    .setStyle(baseStyle.withBold(bold));
        });
    }
}

