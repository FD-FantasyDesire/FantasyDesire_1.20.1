package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;

import javax.annotation.Nullable;

public class TeleportBlockedEffect extends MobEffect {
    public TeleportBlockedEffect() {
        super(MobEffectCategory.HARMFUL, 0x80FF80);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {

    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
