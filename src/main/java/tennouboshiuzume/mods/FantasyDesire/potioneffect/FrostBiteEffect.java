package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

// 寒霜咬噬
// 降低移动速度，并且坠向地面
public class FrostBiteEffect extends MobEffect {
    /** 减速属性修改器 UUID（原版效果属性机制：效果附加/移除/升级时自动 add/remove/刷新） */
    private static final String FROST_BITE_SPEED_UUID = "b2c4d6e8-1a3f-4b5c-9d8e-2f4a6b8c0d2e";

    public FrostBiteEffect() {
        super(MobEffectCategory.HARMFUL, 0xADD8E6);
        // 注册减速属性修改器：实际数值在 getAttributeModifierValue 里按 amplifier 动态计算
        addAttributeModifier(Attributes.MOVEMENT_SPEED, FROST_BITE_SPEED_UUID, -0.2D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return Math.max(-0.2D * (amplifier + 1), -1.0D);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.setDeltaMovement(0, -0.2 * (amplifier + 1), 0);
        entity.level().addParticle(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY() + entity.getBbHeight() / 2,
                entity.getZ(), 0, 0, 0);
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.addAttributeModifiers(entity, attributes, amplifier);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap pAttributeMap, int pAmplifier) {
        super.removeAttributeModifiers(entity, pAttributeMap, pAmplifier);
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }
}
