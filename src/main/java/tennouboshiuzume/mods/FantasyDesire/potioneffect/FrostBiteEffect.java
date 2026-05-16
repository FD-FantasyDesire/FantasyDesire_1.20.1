package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class FrostBiteEffect extends MobEffect {
    private static final UUID FROST_BITE_SPEED_UUID = UUID.fromString("b2c4d6e8-1a3f-4b5c-9d8e-2f4a6b8c0d2e");

    public FrostBiteEffect() {
        super(MobEffectCategory.HARMFUL, 0xADD8E6);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.setDeltaMovement(0, -0.02 * amplifier, 0);
        entity.level().addParticle(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY()+entity.getBbHeight()/2, entity.getZ(), 0, 0, 0);
        entity.hurtMarked = true;
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.addAttributeModifiers(entity, attributes, amplifier);
        AttributeInstance movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            AttributeModifier speedMod = movementSpeed.getModifier(FROST_BITE_SPEED_UUID);
            if (speedMod == null) {
                movementSpeed.addPermanentModifier(new AttributeModifier(FROST_BITE_SPEED_UUID,
                        "Frost Bite Speed", Math.max(-0.2 * amplifier,-1.0), AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
        entity.setNoGravity(true);
    }
    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap pAttributeMap, int pAmplifier) {
        super.removeAttributeModifiers(entity, pAttributeMap, pAmplifier);
        AttributeInstance movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(FROST_BITE_SPEED_UUID);
        }
        entity.setNoGravity(false);
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }
}