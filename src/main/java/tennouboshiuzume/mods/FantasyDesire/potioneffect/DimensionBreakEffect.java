package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import java.util.UUID;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class DimensionBreakEffect extends MobEffect {
    private static final UUID DIMENSION_BREAK_MULTIPLY_UUID = UUID.fromString("9a1e3b5d-8f2c-4e7a-b1c4-6d9e0f3a5c2b");
    public DimensionBreakEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A2BE2); // Purple color
    }
    @Override
    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        AttributeInstance maxHealth = pLivingEntity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double currentMultiply = 0.0;
            AttributeModifier multiplyMod = maxHealth.getModifier(DIMENSION_BREAK_MULTIPLY_UUID);
            if (multiplyMod != null) {
                currentMultiply = multiplyMod.getAmount();
                maxHealth.removeModifier(DIMENSION_BREAK_MULTIPLY_UUID);
            }
//            每级每秒削减1%最大生命值，独立乘区，最大99%
            currentMultiply = Math.max(currentMultiply - 0.01, -0.99);
            maxHealth.addPermanentModifier(new AttributeModifier(DIMENSION_BREAK_MULTIPLY_UUID,
                    "Dimension Break Multiply", currentMultiply, AttributeModifier.Operation.MULTIPLY_TOTAL));
            if (pLivingEntity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                        pLivingEntity.getX(), pLivingEntity.getY() + pLivingEntity.getBbHeight() / 2,
                        pLivingEntity.getZ(),
                        2, 0.2, 0.2, 0.2, 0.2);
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        int level = pAmplifier + 1;
        int rate = 20 / level;
        rate = Math.max(rate, 1);
        return pDuration % rate == 0;
    }

    @Override
    public void removeAttributeModifiers(LivingEntity pLivingEntity,
            net.minecraft.world.entity.ai.attributes.AttributeMap pAttributeMap, int pAmplifier) {
        super.removeAttributeModifiers(pLivingEntity, pAttributeMap, pAmplifier);
        AttributeInstance maxHealth = pLivingEntity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(DIMENSION_BREAK_MULTIPLY_UUID);
        }
    }
}