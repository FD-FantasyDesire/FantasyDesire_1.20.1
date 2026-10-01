package tennouboshiuzume.mods.FantasyDesire.specialeffects.general;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.init.FDAttributes;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShinEffect {
    public static final int MAX_STACKS = 18;
    private static final UUID STRENGTH_ID = UUID.fromString("8e2dcb19-a8ea-4a29-b3c8-a03f2dd8a517");

    private ShinEffect() {
    }

    static int refresh(LivingEntity entity) {
        int points = GeneralRankEffects.tier(entity, FDSpecialEffectsRegistry.Shin) == 0
                ? 0 : Math.max(1, stacks(entity));
        GeneralRankEffects.modifier(entity.getAttribute(FDAttributes.FD_LC_SHIN.get()), STRENGTH_ID,
                "fd_shin_strength", points, AttributeModifier.Operation.ADDITION);
        return points;
    }

    static int stacks(LivingEntity entity) {
        return GeneralRankEffects.stacks(entity.getAttribute(FDAttributes.FD_LC_SHIN.get()),
                STRENGTH_ID, MAX_STACKS);
    }

    static void tryStack(LivingEntity entity) {
        int tier = GeneralRankEffects.tier(entity, FDSpecialEffectsRegistry.Shin);
        if (tier == 0) {
            return;
        }
        double chance = GeneralRankEffects.stackChance(tier, FDConfig.SHIN.stackChanceS.get(),
                FDConfig.SHIN.stackChanceSs.get(), FDConfig.SHIN.stackChanceSss.get());
        int points = GeneralRankEffects.nextStacks(stacks(entity), MAX_STACKS, chance,
                entity.getRandom().nextFloat());
        GeneralRankEffects.modifier(entity.getAttribute(FDAttributes.FD_LC_SHIN.get()), STRENGTH_ID,
                "fd_shin_strength", points, AttributeModifier.Operation.ADDITION);
    }

    static void clear(LivingEntity entity) {
        GeneralRankEffects.modifier(entity.getAttribute(FDAttributes.FD_LC_SHIN.get()), STRENGTH_ID,
                "fd_shin_strength", 0, AttributeModifier.Operation.ADDITION);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !Float.isFinite(event.getAmount()) || event.getAmount() <= 0.0F) {
            return;
        }
        int points = refresh(entity);
        // 在拔刀剑受击扣评分之前取强度；不拦截虚空、/kill 等绕过无敌保护的伤害。
        if (points > 0 && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            double reduction = Math.min(FDConfig.SHIN.reductionCap.get(),
                    points * FDConfig.SHIN.reductionPerPoint.get());
            event.setAmount((float) (event.getAmount() * (1.0D - reduction)));
        }
    }
}
