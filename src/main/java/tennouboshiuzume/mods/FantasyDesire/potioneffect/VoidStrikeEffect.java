package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;

import javax.annotation.Nullable;

public class VoidStrikeEffect extends MobEffect {
    public VoidStrikeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x5500AA); // 紫黑色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            ServerLevel sl = (ServerLevel) entity.level();
            sl.sendParticles(dust, entity.position().x, entity.position().y+entity.getBbHeight()/2, entity.position().z, 2*amplifier, entity.getBbWidth() / 2, entity.getBbHeight() / 2, entity.getBbWidth() / 2, 0.05);
            sl.playSound(null, entity.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.AMBIENT, 0.5f, 0.5f);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    public @Nullable ResourceLocation getIcon() {
        // 这是 HUD 图标（左上角）显示使用的图标
        return new ResourceLocation(FantasyDesire.MODID, "textures/mob_effect/void_strike.png");
    }

    public static DustColorTransitionOptions dust = new DustColorTransitionOptions(new Vector3f(1.0f, 0f, 1.0f), new Vector3f().zero(), 3f);
}
