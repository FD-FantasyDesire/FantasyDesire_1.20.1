package tennouboshiuzume.mods.FantasyDesire.slasharts;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.init.FDAttributes;
import tennouboshiuzume.mods.FantasyDesire.potioneffect.FrostStormEffect;
import tennouboshiuzume.mods.FantasyDesire.specialeffects.effects.overcold.OverColdEffects;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

public class FreezeZero {
    // 数值来自 FDConfig（服务端同步配置），使用处实时读取
    private static final FDConfig.FreezeZero FREEZE_ZERO = FDConfig.FREEZE_ZERO;

    public static boolean AntiNTR(LivingEntity entity) {
        return CapabilityUtils.SEConditionMatcher.of(entity)
                .requireTranslation("item.fantasydesire.over_cold")
                .match() != null;
    }

    public static void FreezeZero(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }

        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(entity)
                .requireTranslation("item.fantasydesire.over_cold")
                .match();
        if (ctx == null)
            return;
        String specialType = ctx.fantasyState.getSpecialType();
        MobEffectInstance current = entity.getEffect(FDPotionEffects.FROST_STORM.get());
        int evolutionTier = OverColdEffects.getEvolutionTier(specialType);
        setStormStrengthBase(entity, evolutionTier);
        // 检查玩家是否已经处于风暴状态，至少一级进化之后，才能使其叠加增长
        if (current == null) {
            StartStorm(entity, evolutionTier);
        } else {
            StackStorm(entity, evolutionTier, current);
        }
    }

    // 进化等级提供施放时的强度加值
    private static void setStormStrengthBase(LivingEntity entity, int evolutionTier) {
        AttributeInstance attribute = entity.getAttribute(FDAttributes.FROST_STORM_STRENGTH.get());
        if (attribute == null) {
            return;
        }
        double minimumAmount = Math.max(0, evolutionTier);
        AttributeModifier current = attribute.getModifier(FrostStormEffect.STRENGTH_MODIFIER_UUID);
        double currentAmount = current == null ? 0.0D : current.getAmount();
        if (current != null && currentAmount >= minimumAmount) {
            return;
        }
        attribute.removeModifier(FrostStormEffect.STRENGTH_MODIFIER_UUID);
        attribute.addTransientModifier(new AttributeModifier(FrostStormEffect.STRENGTH_MODIFIER_UUID,
                "fd_frost_storm_strength", minimumAmount, AttributeModifier.Operation.ADDITION));
    }

    public static void StartStorm(LivingEntity entity, int evolutionTier) {
        entity.addEffect(new MobEffectInstance(FDPotionEffects.FROST_STORM.get(),
                (int) (20 * (FREEZE_ZERO.baseDurationSec()
                        + evolutionTier * FREEZE_ZERO.baseDurationPerTierSec())),
                evolutionTier));
    }

    public static void StackStorm(LivingEntity entity, int evolutionTier, MobEffectInstance current) {
        // 原版刷新效果会先移除再重加属性修改器，保存风暴自身的强度，避免累计成长被清零。
        AttributeInstance strengthAttribute = entity.getAttribute(FDAttributes.FROST_STORM_STRENGTH.get());
        AttributeModifier strengthModifier = strengthAttribute == null ? null
                : strengthAttribute.getModifier(FrostStormEffect.STRENGTH_MODIFIER_UUID);
        float stormStrength = 1.0F + (strengthModifier == null ? 0.0F : (float) strengthModifier.getAmount());

        int durationExtension = Math.max(
                (int) (20 * evolutionTier * FREEZE_ZERO.stackExtensionPerTierSec()),
                FREEZE_ZERO.stackExtensionMinTick());
        int newAmplifier = Math.min(current.getAmplifier() + 1, FREEZE_ZERO.ampCap());
        int newDuration = current.getDuration() + durationExtension;
        if (entity.addEffect(new MobEffectInstance(FDPotionEffects.FROST_STORM.get(), newDuration, newAmplifier, false,
                false, true))) {
            // 在本次施放内恢复属性，避免客户端收到半径为零的中间状态；等级以效果合并后的实际值为准。
            MobEffectInstance updated = entity.getEffect(FDPotionEffects.FROST_STORM.get());
            if (updated != null) {
                FrostStormEffect.syncStormAttributes(entity, updated.getAmplifier(), stormStrength);
            }
        }
    }
}
