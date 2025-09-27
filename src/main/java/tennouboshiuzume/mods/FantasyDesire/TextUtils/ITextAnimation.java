package tennouboshiuzume.mods.FantasyDesire.TextUtils;

import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public interface ITextAnimation {
    /**
     * 对指定字符/文本组件应用动画
     * @param text 当前字符或字符串
     * @param style 当前继承的样式
     * @param tick 当前游戏 tick
     * @return 返回带动画效果的 MutableComponent
     */
    MutableComponent apply(String text, StyleData style, long tick);
}
