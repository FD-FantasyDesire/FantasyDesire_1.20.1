package tennouboshiuzume.mods.FantasyDesire.TextUtils;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public interface ITextAnim {
    MutableComponent apply(MutableComponent input, long worldTime, long speed, TextColor[] colors);
}