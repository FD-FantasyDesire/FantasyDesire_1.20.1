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
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.joml.Vector3f;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDAttributes;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;

import javax.annotation.Nullable;

public class VoidStrikeEffect extends MobEffect {
    /** 施加、伤害结算与属性同步共用的层数上限；效果 amplifier 上限为此值减一。 */
    public static final int MAX_STACKS = 50;
    /** 粒子与音效的触发间隔，单位：tick。 */
    private static final int PRESENTATION_INTERVAL_TICKS = 20;
    public static final java.util.UUID STACK_MODIFIER_UUID = java.util.UUID.fromString(
            "b9b8b5ec-0a7f-4e9d-8f75-4f7c5d9d4a11");

    public VoidStrikeEffect() {
        super(MobEffectCategory.HARMFUL, 0x5500AA); // 紫黑色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide() && entity.level() instanceof ServerLevel sl) {
            syncStackAttribute(entity, amplifier);
            // 属性每 tick 校准，粒子和音效按原版效果计时与固定间隔触发。
            MobEffectInstance current = entity.getEffect(this);
            if (current == null) {
                return;
            }
            int duration = current.isInfiniteDuration() ? entity.tickCount : current.getDuration();
            if (duration % PRESENTATION_INTERVAL_TICKS != 0) {
                return;
            }
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
        if (stack != null && stack.getValue() > 0.0D) {
            return Math.min(MAX_STACKS, Math.max(0, (int) Math.round(stack.getValue())));
        }
        MobEffectInstance current = entity
                .getEffect(FDPotionEffects.VOID_STRIKE.get());
        if (current != null) {
            return Math.min(MAX_STACKS, current.getAmplifier() + 1);
        }
        return 0;
    }

    /** 服务端校准临时层数修改器，数值不变时不触发属性同步。 */
    public static void syncStackAttribute(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return;
        }
        AttributeInstance attribute = entity.getAttribute(FDAttributes.VOID_STRIKE_STACK.get());
        if (attribute == null) {
            return;
        }
        double stack = Math.min(MAX_STACKS,
                Math.max(0.0D, amplifier + 1.0D));
        AttributeModifier modifier = attribute.getModifier(STACK_MODIFIER_UUID);
        if (modifier == null || modifier.getAmount() != stack) {
            attribute.removeModifier(STACK_MODIFIER_UUID);
            attribute.addTransientModifier(new AttributeModifier(STACK_MODIFIER_UUID,
                    "fd_void_strike_stack", stack, AttributeModifier.Operation.ADDITION));
        }
    }

    /** 首次添加、叠层及隐藏效果恢复时立即同步，避免等待下次效果 tick。 */
    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);
        syncStackAttribute(entity, amplifier);
    }

    /** 与寒霜风暴一致，在实际移除或过期的生命周期中清理，取消移除时不会误清。 */
    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        if (entity.level().isClientSide()) {
            return;
        }
        AttributeInstance attribute = entity.getAttribute(FDAttributes.VOID_STRIKE_STACK.get());
        if (attribute != null) {
            attribute.removeModifier(STACK_MODIFIER_UUID);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    public static DustColorTransitionOptions dust = new DustColorTransitionOptions(new Vector3f(1.0f, 0f, 1.0f),
            new Vector3f().zero(), 1f);
}
