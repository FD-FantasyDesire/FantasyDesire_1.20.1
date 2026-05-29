package tennouboshiuzume.mods.FantasyDesire.specialeffects.globalevent;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageSource;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.FDAttackManager;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber
public class DamageConverterEvent {
    public static final Capability<IFantasySlashBladeState> FDBLADESTATE = CapabilityManager
            .get(new CapabilityToken<IFantasySlashBladeState>() {
            });
    public static final Capability<ISlashBladeState> BLADESTATE = CapabilityManager
            .get(new CapabilityToken<ISlashBladeState>() {
            });
    public static UUID ETERNITY_HEALTH_MODIFIER = UUID.fromString("a5b1b2f0-2f3c-4e3b-8a71-123456789abc");

    // 伤害替换事件，用改进后的FDAttackManager处理特殊类型伤害
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void OnSlash(SlashBladeEvent.DoSlashEvent event) {
        if (event.getBlade().getItem() instanceof ItemFantasySlashBlade) {
            ItemStack blade = event.getBlade();
            LivingEntity livingEntity = event.getUser();
            Optional<ISlashBladeState> stateOpt = blade.getCapability(BLADESTATE).resolve();
            Optional<IFantasySlashBladeState> fdStateOpt = blade.getCapability(FDBLADESTATE).resolve();
            if (stateOpt.isEmpty() || fdStateOpt.isEmpty())
                return;
            ISlashBladeState state = stateOpt.get();
            IFantasySlashBladeState fdState = fdStateOpt.get();
            String fdDamageType = fdState.getSpecialAttackEffect();
            if (fdDamageType != null && !fdDamageType.equals("Null")) {
                DamageSource fds = FDDamageSource.getEntityDamageSource(livingEntity.level(),
                        FDDamageSource.fromString(fdDamageType), livingEntity);
                FDAttackManager.areaAttackWithSource(event.getUser(), KnockBacks.cancel.action,
                        (float) event.getDamage(), true, true, false, null, fds);
                event.setDamage(0d);
            }
        }
    }

    // 伤害造成前事件处理
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        // 攻击者
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity))
            return;
        LivingEntity attackerLiving = (LivingEntity) attacker;
        if (source.is(FDDamageSource.OMEGA)) {
            if (target.getHealth() <= attackerLiving.getMaxHealth()) {
                target.kill();
            }
        }
    }

    // 伤害造成时事件处理（此时 target.getHealth() 为受伤前的生命值）
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void OnHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        // 攻击者
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity))
            return;
        LivingEntity attackerLiving = (LivingEntity) attacker;
        // 决断 (Resolution)
        // 追加本次伤害50%的魔法伤害
        if (source.is(FDDamageSource.RESOLUTION)) {
            float extraDamage = amount * 0.5f;
            resetInvulnerable(target);
            target.hurt(attackerLiving.damageSources().magic(), extraDamage);
            resetInvulnerable(target);
        }
        // 暴怒 (Wrath)
        if (source.is(FDDamageSource.WRATH)) {
            float missingHealthPercent = (target.getMaxHealth() - target.getHealth()) / target.getMaxHealth();
            amount *= (1.0f + missingHealthPercent * 2.0f);
        }
        // 怠惰 (Sloth)
        if (source.is(FDDamageSource.SLOTH)) {
            if (target.level().random.nextFloat() < 0.5f) {
                target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
                target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 20, 1));
            }
        }
        // 忧郁 (Gloom)
        if (source.is(FDDamageSource.GLOOM)) {
            int maxAir = target.getMaxAirSupply();
            int currentAir = target.getAirSupply();
            if (maxAir > 0) {
                float airPercent = (float) currentAir / maxAir;
                amount *= (1.0f + airPercent);
                target.setAirSupply(0);
            } else {
                amount *= 1.5f;
            }
        }
        // 傲慢 (Pride)
        // 根据玩家当前生命值缩放
        if (source.is(FDDamageSource.PRIDE)) {
            float healthPercent = attackerLiving.getHealth() / attackerLiving.getMaxHealth();
            float factor = 3.4375f * healthPercent - 0.4375f;
            amount *= Math.max(0f, factor);
        }
        // 嫉妒 (Envy)
        // 根据双方护甲差，每1点+5%
        if (source.is(FDDamageSource.ENVY)) {
            int targetArmor = target.getArmorValue();
            int attackerArmor = attackerLiving.getArmorValue();
            if (targetArmor > attackerArmor) {
                int diff = targetArmor - attackerArmor;
                amount *= (1.0f + diff * 0.05f);
            }
        }
        // 回响（Echo）
        if (source.is(FDDamageSource.ECHO)) {
            target.forceAddEffect(new MobEffectInstance(FDPotionEffects.ECHO_TIMER.get(), 60, 0, false, false, false),
                    attacker);
            if (amount > 0.1f) {
                float storeAmount = amount - 0.1f;
                target.getCapability(tennouboshiuzume.mods.FantasyDesire.capability.EchoDamageProvider.ECHO_DAMAGE)
                        .ifPresent(cap -> {
                            cap.addDamage(attacker.getUUID(), storeAmount);
                        });
                amount = 0.1f;
            }
        }
        event.setAmount(amount);
    }

    // 伤害最终结算（护甲后，实体更新前）事件处理
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void OnDamage(LivingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        // 攻击者
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity))
            return;
        LivingEntity attackerLiving = (LivingEntity) attacker;
        // 虚空强袭叠加对回响伤害增伤
        int voidStrikeLayers = tennouboshiuzume.mods.FantasyDesire.potioneffect.VoidStrikeEffect
                .getVoidStrikeLayers(target);
        if (voidStrikeLayers > 0) {
            amount *= 1.0f + voidStrikeLayers * 0.1f;
        }
        // 永劫
        if (source.is(FDDamageSource.ETERNITY)) {
            float reduce = amount * 0.1f;
            AttributeInstance maxHealth = target.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                // 先尝试获取旧的 modifier
                AttributeModifier old = maxHealth.getModifier(ETERNITY_HEALTH_MODIFIER);
                double totalReduce = -reduce;
                if (old != null) {
                    totalReduce += old.getAmount(); // 累加旧值
                    maxHealth.removeModifier(old); // 移除旧的，避免重复
                }
                AttributeModifier mod = new AttributeModifier(
                        ETERNITY_HEALTH_MODIFIER,
                        "eternity_reduce",
                        totalReduce,
                        AttributeModifier.Operation.ADDITION);
                maxHealth.addPermanentModifier(mod);
            }
        }
        // 吸收（Absorb）
        if (source.is(FDDamageSource.ABSORB)) {
            float heal = amount;
            float missing = attackerLiving.getMaxHealth() - attackerLiving.getHealth();
            float overflow = Math.max(0, heal - missing);
            attackerLiving.heal(heal);
            if (overflow > 0) {
                float bonus = overflow * 0.1f;
                float newAbsorb = Math.min(20f, attackerLiving.getAbsorptionAmount() + bonus);
                attackerLiving.setAbsorptionAmount(newAbsorb);
            }
        }
        // 色欲 (Lust)
        if (source.is(FDDamageSource.LUST)) {
            attackerLiving.heal(amount * 0.05f);
        }
        // 暴食 (Gluttony)
        if (source.is(FDDamageSource.GLUTTONY)) {
            if (attackerLiving instanceof Player player) {
                float healFood = amount * 0.1f;
                int foodNeeded = 20 - player.getFoodData().getFoodLevel();
                if (healFood > foodNeeded) {
                    player.getFoodData().setFoodLevel(20);
                    float excess = healFood - foodNeeded;
                    float bonusAbsorb = excess * 0.5f;
                    float newAbsorb = Math.min(10f, attackerLiving.getAbsorptionAmount() + bonusAbsorb);
                    attackerLiving.setAbsorptionAmount(newAbsorb);
                } else {
                    int newFood = Math.min(20, player.getFoodData().getFoodLevel() + (int) Math.max(1, healFood));
                    player.getFoodData().setFoodLevel(newFood);
                }
            }
        }
        event.setAmount(amount);
    }

    // 在玩家死亡时清理
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        clearEternity(entity);
    }

    // 在实体进入世界时清理
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity livingEntity) {
            clearEternity(livingEntity);
        }
    }

    // 在玩家上床时清理
    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        clearEternity(event.getEntity());
    }

    // 终焉伤害禁用传送
    @SubscribeEvent
    public void onEntityTeleport(EntityTeleportEvent.ChorusFruit event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            // 如果玩家处于禁传送状态，就阻止
            if (player.hasEffect(FDPotionEffects.TELEPORT_BLOCKED.get())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onEntityTeleport(EntityTeleportEvent.EnderPearl event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            // 如果玩家处于禁传送状态，就阻止
            if (player.hasEffect(FDPotionEffects.TELEPORT_BLOCKED.get())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onEntityTeleport(EntityTeleportEvent.EnderEntity event) {
        // 针对末影生物的禁用传送
        if (event.getEntityLiving().hasEffect(FDPotionEffects.TELEPORT_BLOCKED.get())) {
            event.setCanceled(true);
        }
    }

    // 用于清理永劫计数
    public static void clearEternity(LivingEntity entity) {
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && maxHealth.getModifier(ETERNITY_HEALTH_MODIFIER) != null) {
            maxHealth.removeModifier(ETERNITY_HEALTH_MODIFIER);
        }
    }

    // 重置无敌帧
    public static void resetInvulnerable(Entity target) {
        target.invulnerableTime = 0;
    }

    // 允许特定药水效果被强制赋予
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        MobEffect effect = event.getEffectInstance().getEffect();
        if (effect == FDPotionEffects.ECHO_TIMER.get() ||
                effect == FDPotionEffects.VOID_STRIKE.get() ||
                effect == FDPotionEffects.FROST_BITE.get() ||
                effect == FDPotionEffects.FROST_STORM.get() ||
                effect == FDPotionEffects.TELEPORT_BLOCKED.get() ||
                effect == FDPotionEffects.DIMENSION_BREAK.get() ||
                effect == FDPotionEffects.IMMORTAL_SOUL.get() ||
                effect == FDPotionEffects.MISSILE_LOCKED.get() ||
                effect == FDPotionEffects.RAINBOW_SEVEN_EDGE.get() ||
                effect == FDPotionEffects.COMET_ELYTRA.get()) {
            event.setResult(Result.ALLOW);
        }
    }

}