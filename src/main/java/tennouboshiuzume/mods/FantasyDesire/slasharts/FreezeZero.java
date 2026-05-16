package tennouboshiuzume.mods.FantasyDesire.slasharts;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

public class FreezeZero {

    public static boolean AntiNTR(LivingEntity entity) {
        return CapabilityUtils.SEConditionMatcher.of(entity)
                .requireTranslation("item.fantasydesire.over_cold")
                .match() != null;
    }

    public static void FreezeZero(LivingEntity entity) {
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(entity)
                .requireTranslation("item.fantasydesire.over_cold")
                .match();
        if (ctx == null)
            return;

        String specialType = ctx.fantasyState.getSpecialType();
        MobEffectInstance current = entity.getEffect(FDPotionEffects.FROST_STORM.get());
        int evolutionTier = getEvolutionTier(specialType);
//        检查玩家是否已经处于冰冻风暴状态
        if (current == null) {
            StartStorm(entity, evolutionTier);
        } else {
            StackStorm(entity, evolutionTier, current);
        }
    }

    public static void StartStorm(LivingEntity entity, int evolutionTier) {

        entity.addEffect(new MobEffectInstance(FDPotionEffects.FROST_STORM.get(), 20 * (6 + evolutionTier * 3),evolutionTier));
    }

    public static void StackStorm(LivingEntity entity, int evolutionTier, MobEffectInstance current) {
        int durationExtension = 20 * evolutionTier * 3;
        int newAmplifier = Math.min(current.getAmplifier() + 1, 14);
        int newDuration = current.getDuration() + durationExtension;
        entity.addEffect(new MobEffectInstance(FDPotionEffects.FROST_STORM.get(), newDuration, newAmplifier,false,false,true));
    }

    private static int getEvolutionTier(String specialType) {
        return switch (specialType) {
            case "OverCold_1" -> 1;
            case "OverCold_2" -> 2;
            case "OverCold_3" -> 3;
            default -> 0;
        };
    }
}