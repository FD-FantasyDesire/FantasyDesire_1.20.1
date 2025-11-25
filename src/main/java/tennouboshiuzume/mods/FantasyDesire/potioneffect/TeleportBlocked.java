package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;

import javax.annotation.Nullable;

public class TeleportBlocked extends MobEffect {
    public TeleportBlocked() {
        super(MobEffectCategory.HARMFUL, 0x80FF80);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {

    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    public @Nullable ResourceLocation getIcon() {
        // 这是 HUD 图标（左上角）显示使用的图标
        return new ResourceLocation(FantasyDesire.MODID, "textures/mob_effect/void_strike.png");
    }

}
