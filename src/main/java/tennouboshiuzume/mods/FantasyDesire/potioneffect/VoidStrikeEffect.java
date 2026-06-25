package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;

import javax.annotation.Nullable;

public class VoidStrikeEffect extends MobEffect {
    public VoidStrikeEffect() {
        super(MobEffectCategory.HARMFUL, 0x5500AA); // 紫黑色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide() && entity.level() instanceof ServerLevel sl) {
            sl.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.AMBIENT, 0.5f,
                    1f);

            if (amplifier > 0) {
                // Generate Tomoe (spiral) particles based on amplifier
                net.minecraft.util.RandomSource random = entity.getRandom();
                float tiltX = (random.nextFloat() - 0.5f) * (float) Math.PI;
                float tiltY = random.nextFloat() * (float) Math.PI * 2f;
                float tiltZ = (random.nextFloat() - 0.5f) * (float) Math.PI;

                int arms = 3;
                int particlesPerArm = Math.min(15 + amplifier, 30);
                for (int i = 0; i < arms; i++) {
                    float armOffset = (float) (i * Math.PI * 2.0 / arms);
                    for (int j = 0; j < particlesPerArm; j++) {
                        float t = (float) j / particlesPerArm;
                        float angle = armOffset + t * (float) Math.PI * 2.0f;
                        float radius = 0.2f + t * 1.5f;

                        Vector3f pos = new Vector3f((float) (Math.cos(angle) * radius), 0f,
                                (float) (Math.sin(angle) * radius));
                        pos.rotateX(tiltX).rotateY(tiltY).rotateZ(tiltZ);

                        sl.sendParticles(dust, entity.position().x + pos.x(),
                                entity.position().y + entity.getBbHeight() / 2 + pos.y(), entity.position().z + pos.z(),
                                1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    public static int getVoidStrikeLayers(LivingEntity entity) {
        MobEffectInstance current = entity
                .getEffect(tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects.VOID_STRIKE.get());
        if (current != null) {
            return Math.min(50, current.getAmplifier() + 1);
        }
        return 0;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 每 20 tick 触发一次，同时播放粒子
        return duration % 20 == 0;
    }

    public @Nullable ResourceLocation getIcon() {
        // 这是 HUD 图标（左上角）显示使用的图标
        return new ResourceLocation(FantasyDesire.MODID, "textures/mob_effect/void_strike.png");
    }

    public static DustColorTransitionOptions dust = new DustColorTransitionOptions(new Vector3f(1.0f, 0f, 1.0f),
            new Vector3f().zero(), 1f);
}