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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.init.FDAttributes;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;

import javax.annotation.Nullable;

public class VoidStrikeEffect extends MobEffect {
    // 数值来自 FDConfig（服务端同步配置），使用处实时读取
    private static final FDConfig.VoidStrikeEffect VOID_STRIKE_EFFECT = FDConfig.VOID_STRIKE_EFFECT;
    public static final java.util.UUID STACK_MODIFIER_UUID = java.util.UUID.fromString(
            "b9b8b5ec-0a7f-4e9d-8f75-4f7c5d9d4a11");

    public VoidStrikeEffect() {
        super(MobEffectCategory.HARMFUL, 0x5500AA); // 紫黑色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide() && entity.level() instanceof ServerLevel sl) {
            syncStackAttribute(entity, amplifier);
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
        AttributeInstance stack = entity.getAttribute(FDAttributes.VOID_STRIKE_STACK.get());
        int cap = VOID_STRIKE_EFFECT.stackCap();
        if (stack != null && stack.getValue() > 0.0D) {
            return Math.min(cap, Math.max(0, (int) Math.round(stack.getValue())));
        }
        MobEffectInstance current = entity
                .getEffect(FDPotionEffects.VOID_STRIKE.get());
        if (current != null) {
            return Math.min(cap, current.getAmplifier() + 1);
        }
        return 0;
    }

    public static void syncStackAttribute(LivingEntity entity, int amplifier) {
        double stack = Math.min(VOID_STRIKE_EFFECT.stackCap(),
                Math.max(0.0D, amplifier + 1.0D));
        FDAttributes.syncVoidStrikeStack(entity, stack, STACK_MODIFIER_UUID);
    }

    public static void clearStackAttribute(LivingEntity entity) {
        FDAttributes.clearVoidStrikeStack(entity, STACK_MODIFIER_UUID);
    }

    @Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class VoidStrikeEffectEvents {
        @SubscribeEvent
        public static void onEffectRemoved(MobEffectEvent.Remove event) {
            if (event.getEffect() != FDPotionEffects.VOID_STRIKE.get()) {
                return;
            }
            MobEffectInstance current = event.getEntity().getEffect(FDPotionEffects.VOID_STRIKE.get());
            if (current == null) {
                clearStackAttribute(event.getEntity());
            } else {
                syncStackAttribute(event.getEntity(), current.getAmplifier());
            }
        }

        @SubscribeEvent
        public static void onEffectExpired(MobEffectEvent.Expired event) {
            if (event.getEffectInstance().getEffect() == FDPotionEffects.VOID_STRIKE.get()) {
                clearStackAttribute(event.getEntity());
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 每 20 tick 触发一次，同时播放粒子
        return duration % VOID_STRIKE_EFFECT.tickInterval() == 0;
    }

    public @Nullable ResourceLocation getIcon() {
        // 这是 HUD 图标（左上角）显示使用的图标
        return new ResourceLocation(FantasyDesire.MODID, "textures/mob_effect/void_strike.png");
    }

    public static DustColorTransitionOptions dust = new DustColorTransitionOptions(new Vector3f(1.0f, 0f, 1.0f),
            new Vector3f().zero(), 1f);
}
