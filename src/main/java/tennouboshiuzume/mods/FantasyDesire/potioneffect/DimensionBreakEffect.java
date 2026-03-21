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
    private static final UUID DIMENSION_BREAK_ADDITION_UUID = UUID.fromString("1f3b5d9a-2c8f-7a4e-c4b1-e0f3a5c2b6d9");

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

            double currentAddition = 0.0;
            AttributeModifier additionMod = maxHealth.getModifier(DIMENSION_BREAK_ADDITION_UUID);
            if (additionMod != null) {
                currentAddition = additionMod.getAmount();
                maxHealth.removeModifier(DIMENSION_BREAK_ADDITION_UUID);
            }

            // Decrease 1% (0.01) each time, capped at -0.99 (99% reduction)
            currentMultiply -= 0.01;
            if (currentMultiply < -0.99) {
                currentMultiply = -0.99;
            }

            maxHealth.addPermanentModifier(new AttributeModifier(DIMENSION_BREAK_MULTIPLY_UUID,
                    "Dimension Break Multiply", currentMultiply, AttributeModifier.Operation.MULTIPLY_TOTAL));

            double oldHealth = maxHealth.getValue();

            // Decrease 1 flat health each time
            maxHealth.addPermanentModifier(new AttributeModifier(DIMENSION_BREAK_ADDITION_UUID,
                    "Dimension Break Addition", currentAddition - 1.0, AttributeModifier.Operation.ADDITION));

            double newHealth = maxHealth.getValue();

            // Prevent dropping below 1.0, and prevent "negative negative makes positive"
            // (health increasing)
            if (newHealth < 1.0 || newHealth > oldHealth) {
                maxHealth.removeModifier(DIMENSION_BREAK_ADDITION_UUID);
                maxHealth.addPermanentModifier(new AttributeModifier(DIMENSION_BREAK_ADDITION_UUID,
                        "Dimension Break Addition", currentAddition, AttributeModifier.Operation.ADDITION));
            }

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
        int rate = 20 / (pAmplifier + 1);
        if (rate > 0) {
            return pDuration % rate == 0;
        }
        return true;
    }

    @Override
    public void removeAttributeModifiers(LivingEntity pLivingEntity,
            net.minecraft.world.entity.ai.attributes.AttributeMap pAttributeMap, int pAmplifier) {
        super.removeAttributeModifiers(pLivingEntity, pAttributeMap, pAmplifier);
        AttributeInstance maxHealth = pLivingEntity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(DIMENSION_BREAK_MULTIPLY_UUID);
            maxHealth.removeModifier(DIMENSION_BREAK_ADDITION_UUID);
        }
    }
}