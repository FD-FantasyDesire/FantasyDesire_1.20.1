package tennouboshiuzume.mods.FantasyDesire.specialeffects.general;

import mods.flammpfeil.slashblade.entity.EntityDrive;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.init.FDAttributes;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDDriveEx;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MangEffect {
    public static final int MAX_STACKS = 3;
    private static final UUID STRENGTH_ID = UUID.fromString("e3db1bc2-8caf-4191-94ee-30e6632fb173");
    private static final UUID ATTACK_ID = UUID.fromString("4e10e8db-00aa-48fc-a3b0-c796723b3f92");
    private static final String COST_WINDOW = "fd_mang_cost_window";
    private static final String COST_SPENT = "fd_mang_cost_spent";

    private MangEffect() {
    }

    static void refresh(LivingEntity entity) {
        if (!(entity instanceof Player)) {
            return;
        }
        int points = GeneralRankEffects.tier(entity, FDSpecialEffectsRegistry.Mang) == 0
                ? 0 : Math.max(1, stacks(entity));
        GeneralRankEffects.modifier(entity.getAttribute(FDAttributes.FD_LC_MANG.get()), STRENGTH_ID,
                "fd_mang_strength", points, AttributeModifier.Operation.ADDITION);
        // 独立总乘区，只改变攻击力面板，不把评级、附魔等加值一并乘算。
        GeneralRankEffects.modifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID,
                "fd_mang_attack", Math.pow(FDConfig.MANG.multiplierBase.get(), points) - 1.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    static int stacks(LivingEntity entity) {
        return GeneralRankEffects.stacks(entity.getAttribute(FDAttributes.FD_LC_MANG.get()),
                STRENGTH_ID, MAX_STACKS);
    }

    static void tryStack(LivingEntity entity) {
        int tier = GeneralRankEffects.tier(entity, FDSpecialEffectsRegistry.Mang);
        if (!(entity instanceof Player) || tier == 0) {
            return;
        }
        double baseChance = GeneralRankEffects.stackChance(tier, FDConfig.MANG.stackChanceS.get(),
                FDConfig.MANG.stackChanceSs.get(), FDConfig.MANG.stackChanceSss.get());
        int current = stacks(entity);
        double chance = stackChance(current, baseChance, FDConfig.MANG.stackChanceDecay.get());
        int points = GeneralRankEffects.nextStacks(current, MAX_STACKS, chance,
                entity.getRandom().nextFloat());
        GeneralRankEffects.modifier(entity.getAttribute(FDAttributes.FD_LC_MANG.get()), STRENGTH_ID,
                "fd_mang_strength", points, AttributeModifier.Operation.ADDITION);
        refresh(entity);
    }

    /** 初始一层自动获得；之后每多一层，下一次叠层概率再乘衰减系数。 */
    static double stackChance(int currentStacks, double baseChance, double decay) {
        return currentStacks >= MAX_STACKS ? 0.0D
                : baseChance * Math.pow(decay, Math.max(0, currentStacks - 1));
    }

    static void clear(LivingEntity entity) {
        GeneralRankEffects.modifier(entity.getAttribute(FDAttributes.FD_LC_MANG.get()), STRENGTH_ID,
                "fd_mang_strength", 0, AttributeModifier.Operation.ADDITION);
        GeneralRankEffects.modifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID,
                "fd_mang_attack", 0, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    /** 每 20 tick 共用一个消耗预算；换刀不重置，避免范围攻击或来回换刀透支评分。 */
    static int budgetedCost(CompoundTag data, long time, int requested, int cap) {
        if (requested <= 0 || cap <= 0) {
            return 0;
        }
        long window = Math.floorDiv(time, 20L);
        int spent = data.contains(COST_WINDOW) && data.getLong(COST_WINDOW) == window
                ? Math.max(0, Math.min(cap, data.getInt(COST_SPENT))) : 0;
        int cost = Math.min(requested, cap - spent);
        data.putLong(COST_WINDOW, window);
        data.putInt(COST_SPENT, spent + cost);
        return cost;
    }

    /** 仅判定评分消耗，伤害增幅全部由攻击伤害属性修改器提供。 */
    static boolean isScoringHit(ResourceLocation type, boolean panelProjectile, boolean directPlayer) {
        return panelProjectile || directPlayer
                && (type.getNamespace().equals("minecraft") && type.getPath().equals("player_attack")
                        || type.getNamespace().equals(FantasyDesire.MODID));
    }

    private static boolean isScoringHit(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypes.THORNS)) {
            return false;
        }
        var direct = source.getDirectEntity();
        boolean panelProjectile = direct instanceof EntityFDDriveEx || direct instanceof EntityDrive;
        ResourceLocation type = source.typeHolder().unwrapKey().map(key -> key.location()).orElse(null);
        return type != null && isScoringHit(type, panelProjectile,
                direct == source.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBladeHit(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide() || !Float.isFinite(event.getAmount())
                || event.getAmount() <= 0 || !(event.getSource().getEntity() instanceof Player player)
                || event.getEntity() == player || !isScoringHit(event.getSource())) {
            return;
        }
        int stacks = stacks(player);
        if (stacks > 0 && GeneralRankEffects.tier(player, FDSpecialEffectsRegistry.Mang) > 0) {
            // 按本次攻击实际使用的档位扣分；先保留拔刀剑命中加分，再刷新后续命中的面板。
            // 完全被护盾吸收、免疫或取消的伤害不会走到正数的伤害结算。
            int cost = budgetedCost(player.getPersistentData(), player.level().getGameTime(),
                    stacks * FDConfig.MANG.rankCostPerStack.get(), FDConfig.MANG.rankCostPerSecondCap.get());
            if (cost > 0) {
                GeneralRankEffects.consume(player, cost);
            }
            GeneralRankEffects.refresh(player);
        }
    }
}
