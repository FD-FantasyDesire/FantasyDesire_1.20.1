package tennouboshiuzume.mods.FantasyDesire.slasharts;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
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
        // 检查玩家是否已经处于风暴状态，至少一级进化之后，才能使其叠加增长
        if (current == null) {
            StartStorm(entity, evolutionTier);
        } else {
            StackStorm(entity, evolutionTier, current);
        }
    }

    public static void StartStorm(LivingEntity entity, int evolutionTier) {
        entity.addEffect(new MobEffectInstance(FDPotionEffects.FROST_STORM.get(),
                (int) (20 * (FREEZE_ZERO.baseDurationSec()
                        + evolutionTier * FREEZE_ZERO.baseDurationPerTierSec())),
                evolutionTier));
    }

    public static void StackStorm(LivingEntity entity, int evolutionTier, MobEffectInstance current) {
        int durationExtension = Math.max(
                (int) (20 * evolutionTier * FREEZE_ZERO.stackExtensionPerTierSec()),
                FREEZE_ZERO.stackExtensionMinTick());
        int newAmplifier = Math.min(current.getAmplifier() + 1, FREEZE_ZERO.ampCap());
        int newDuration = current.getDuration() + durationExtension;
        entity.addEffect(new MobEffectInstance(FDPotionEffects.FROST_STORM.get(), newDuration, newAmplifier, false,
                false, true));
    }
}
