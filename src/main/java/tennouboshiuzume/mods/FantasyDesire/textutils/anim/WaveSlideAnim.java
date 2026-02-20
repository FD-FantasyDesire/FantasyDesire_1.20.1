package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import tennouboshiuzume.mods.FantasyDesire.textutils.ComponentUtils;
import tennouboshiuzume.mods.FantasyDesire.textutils.ITextAnim;

public class WaveSlideAnim implements ITextAnim {
//    滑动加粗
    @Override
    public MutableComponent apply(MutableComponent component, long worldTime, long speed, TextColor[] colors) {
        int length = component.getString().length();
        if (length == 0) return component;

        // 波峰的位置随时间移动
        int peak = (int) ((worldTime / speed) % length);

        return ComponentUtils.forEachChar(component, (c, baseStyle, index) -> {
            boolean bold = (index == peak || index == peak + 1); // 只加粗波峰的两个字

            return Component.literal(String.valueOf(c))
                    .setStyle(baseStyle.withBold(bold));
        });
    }
}