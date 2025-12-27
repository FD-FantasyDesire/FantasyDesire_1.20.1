package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class MissileLockedEffect extends MobEffect {
    public MissileLockedEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFF0000);
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.addAttributeModifiers(entity, attributes, amplifier);
        entity.setGlowingTag(true);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        entity.setGlowingTag(false);
    }

}
