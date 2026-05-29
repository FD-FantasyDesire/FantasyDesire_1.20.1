package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ImmortalSoulEffect extends MobEffect {
    // 这是一个 “标识” 效果，用于提示玩家该效果已发动并且生效
    public ImmortalSoulEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);
    }
}
