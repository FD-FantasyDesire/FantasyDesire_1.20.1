package tennouboshiuzume.mods.FantasyDesire.specialeffects.general;

import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

import java.util.UUID;
import java.util.Map;
import java.util.WeakHashMap;

/** 通用评分效果：不限制刀的翻译键，强度由服务端维护并通过属性同步。 */
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneralRankEffects {
    private static final Map<LivingEntity, ItemStack> HELD_BLADES = new WeakHashMap<>();
    private GeneralRankEffects() {
    }

    static int tier(LivingEntity entity, RegistryObject<SpecialEffect> effect) {
        if (!entity.isAlive() || entity instanceof Player player && player.isSpectator()
                || CapabilityUtils.SEConditionMatcher.of(entity).requireSE(effect).match() == null) {
            return 0;
        }
        return entity.getCapability(CapabilityConcentrationRank.RANK_POINT).map(rank -> {
            if (rank.getUnitCapacity() <= 0) {
                return 0;
            }
            IConcentrationRank.ConcentrationRanks rating = rank.getRank(entity.level().getGameTime());
            return rating == null ? 0 : Math.max(0, Math.min(3,
                    rating.level - IConcentrationRank.ConcentrationRanks.S.level + 1));
        }).orElse(0);
    }

    static int stacks(AttributeInstance attribute, UUID id, int maxStacks) {
        AttributeModifier current = attribute == null ? null : attribute.getModifier(id);
        double amount = current == null ? 0 : current.getAmount();
        return Double.isFinite(amount) ? (int) Math.max(0, Math.min(maxStacks, amount)) : 0;
    }

    static int nextStacks(int current, int maxStacks, double chance, float roll) {
        int stacks = Math.max(1, Math.min(maxStacks, current));
        return stacks < maxStacks && roll < chance ? stacks + 1 : stacks;
    }

    static double stackChance(int tier, double s, double ss, double sss) {
        return switch (tier) {
            case 1 -> s;
            case 2 -> ss;
            case 3 -> sss;
            default -> 0.0D;
        };
    }

    static void consume(LivingEntity entity, long points) {
        entity.getCapability(CapabilityConcentrationRank.RANK_POINT)
                .ifPresent(rank -> rank.addRankPoint(entity, -points));
    }

    static void modifier(AttributeInstance attribute, UUID id, String name, double amount,
            AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }
        AttributeModifier current = attribute.getModifier(id);
        if (amount <= 0.0D || !Double.isFinite(amount)) {
            attribute.removeModifier(id);
        } else if (current == null || current.getAmount() != amount) {
            attribute.removeModifier(id);
            attribute.addTransientModifier(new AttributeModifier(id, name, amount, operation));
        }
    }

    static void refresh(LivingEntity entity) {
        ItemStack held = entity.getMainHandItem();
        ItemStack previous = HELD_BLADES.put(entity, held);
        // 比较实际物品栈引用，耐久、连段和 NBT 修改不会被当作换刀；弱键不保留卸载实体。
        if (previous != null && previous != held) {
            ShinEffect.clear(entity);
            MangEffect.clear(entity);
        }
        ShinEffect.refresh(entity);
        MangEffect.refresh(entity);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        refresh(entity);
        if (entity.tickCount % 5 == 0) {
            ShinEffect.tryStack(entity);
            MangEffect.tryStack(entity);
        }
        int shinStrength = ShinEffect.stacks(entity);
        // 时间单位为游戏 tick，每 20 tick 结算一次，避免逐 tick 发送评分同步包。
        if (shinStrength > 0 && entity.tickCount % 20 == 0) {
            consume(entity, (long) shinStrength * FDConfig.SHIN.rankCostPerStack.get());
        }
        refresh(entity);
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getSlot() == EquipmentSlot.MAINHAND) {
            refresh(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            // 拔刀剑近战也触发此事件；在读取面板前校准换刀、经验与评级变化。
            refresh(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            ShinEffect.clear(event.getEntity());
            MangEffect.clear(event.getEntity());
            HELD_BLADES.remove(event.getEntity());
        }
    }
}
