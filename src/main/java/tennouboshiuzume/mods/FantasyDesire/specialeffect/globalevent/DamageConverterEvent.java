package tennouboshiuzume.mods.FantasyDesire.specialeffect.globalevent;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageSource;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.FDAttackManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber
public class DamageConverterEvent {
    public static final Capability<IFantasySlashBladeState> FDBLADESTATE = CapabilityManager.get(new CapabilityToken<IFantasySlashBladeState>() {
    });
    public static final Capability<ISlashBladeState> BLADESTATE = CapabilityManager.get(new CapabilityToken<ISlashBladeState>() {
    });
    public static UUID ETERNITY_HEALTH_MODIFIER = UUID.fromString("a5b1b2f0-2f3c-4e3b-8a71-123456789abc");
//    根据伤害类型作出不同的效果追加
//    暴怒 特殊火焰 对持盾敌人造成3x伤害
//    色欲 使攻击者回复0.2生命值
//    暴食 使攻击者回复0.2饥饿值
//    忧郁 特殊溺水 消耗敌人的氧气条
//    傲慢 对拥有护甲的敌人1.5x伤害，暴击强化
//    嫉妒 对生命值大于你的敌人造成3x伤害
//    次元 伤害的50%会转化为真实伤害
//    永劫 伤害的10%转化为生命值上限削减
//    吸收 使攻击者吸收同等生命值，溢出部分的10%转化为额外生命，最大20
//    决断 追加敌我生命值差值1/4的物理伤害
//    回响 对已叠加6x虚空强袭的敌人扩散伤害，并且移除虚空强袭

    //      伤害替换事件，用改进后的FDAttackManager处理特殊类型伤害
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void OnSlash(SlashBladeEvent.DoSlashEvent event) {
        if (event.getBlade().getItem() instanceof ItemFantasySlashBlade) {
            ItemStack blade = event.getBlade();
            LivingEntity livingEntity = event.getUser();
            Optional<ISlashBladeState> stateOpt = blade.getCapability(BLADESTATE).resolve();
            Optional<IFantasySlashBladeState> fdStateOpt = blade.getCapability(FDBLADESTATE).resolve();
            if (stateOpt.isEmpty() || fdStateOpt.isEmpty()) return;
            ISlashBladeState state = stateOpt.get();
            IFantasySlashBladeState fdState = fdStateOpt.get();
            String fdDamageType = fdState.getSpecialAttackEffect();
            if (fdDamageType != null && !fdDamageType.equals("Null")) {
                DamageSource fds = FDDamageSource.getEntityDamageSource(livingEntity.level(), FDDamageSource.fromString(fdDamageType), livingEntity);
                FDAttackManager.areaAttackWithSource(event.getUser(), KnockBacks.cancel.action, (float) event.getDamage(), true, true, false, null, fds);
                event.setDamage(0d);
            }
        }
    }

    //    重置无敌帧
    public static void resetInvulnerable(Entity target) {
        target.invulnerableTime = 0;
    }

    //    伤害造成前事件处理
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        // 攻击者
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity)) return;
        LivingEntity attackerLiving = (LivingEntity) attacker;
        if (source.is(FDDamageSource.OMEGA)) {
            int duration = 100;
            int amplifier = 0;
            MobEffect effect = FDPotionEffects.TELEPORT_BLOCKED.get();
            target.addEffect(new MobEffectInstance(effect, duration, amplifier));
        }
    }

    //    伤害造成时事件处理
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void OnHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        // 攻击者
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity)) return;
        LivingEntity attackerLiving = (LivingEntity) attacker;

        if (source.is(FDDamageSource.OMEGA)) {
            int duration = 100;
            int amplifier = 0;
            MobEffect TPBlocked = FDPotionEffects.TELEPORT_BLOCKED.get();
            target.addEffect(new MobEffectInstance(TPBlocked, duration, amplifier));
        }
        // 决断 (Resolution)
        if (source.is(FDDamageSource.RESOLUTION)) {
            float extraDamage = attackerLiving.getHealth() * 0.25f;
            if (attackerLiving instanceof Player) {
                target.hurt(attackerLiving.damageSources().playerAttack((Player) attackerLiving), extraDamage);
            } else {
                target.hurt(attackerLiving.damageSources().mobAttack(attackerLiving), extraDamage);
            }
        }
        // 暴怒 (Wrath)
        if (source.is(FDDamageSource.WRATH)) {
            if (target.isBlocking()) {
                amount *= 3f;
            }
        }
        // 色欲 (Lust)
        if (source.is(FDDamageSource.LUST)) {
            attackerLiving.heal(0.2f);
        }
        // 怠惰 (Sloth)
        if (source.is(FDDamageSource.SLOTH)) {
            double moveSpeed = attackerLiving.getDeltaMovement().length();
            double factor = 1.0 + (0.2 / Math.max(0.1, moveSpeed)); // 越慢倍率越高
            amount *= (float) factor;
        }
        // 暴食 (Gluttony)
        if (source.is(FDDamageSource.GLUTTONY)) {
            if (attackerLiving instanceof Player player) {
                player.getFoodData().eat(1, 0.2f);
            }
        }
        // 忧郁 (Gloom)
        if (source.is(FDDamageSource.GLOOM)) {
            int oxygen = target.getAirSupply();
            target.setAirSupply(Math.max(0, oxygen - (int) (amount * 3)));
        }
        // 傲慢 (Pride)
        if (source.is(FDDamageSource.PRIDE)) {
            if (attackerLiving.getDeltaMovement().y < 0 && !attackerLiving.onGround()) {
                amount *= 2;
            }
            if (target.getArmorValue() > 0) {
                amount *= 1.5f;
            }
        }
        // 嫉妒 (Envy)
        if (source.is(FDDamageSource.ENVY)) {
            if (target.getHealth() > attackerLiving.getHealth()) {
                amount *= 3f;
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
        if (!(attacker instanceof LivingEntity)) return;
        LivingEntity attackerLiving = (LivingEntity) attacker;

        // 虚空强袭异常等级增伤
        if (target.hasEffect(FDPotionEffects.VOID_STRIKE.get())) {
            MobEffect voidStrike = FDPotionEffects.VOID_STRIKE.get();
            MobEffectInstance current = target.getEffect(voidStrike);
            if (current != null) {
                amount *= current.getAmplifier() + 1;
            }
        }

        // 永劫 (Eternity)
        if (source.is(FDDamageSource.ETERNITY)) {
            float reduce = amount * 0.1f;
            AttributeInstance maxHealth = target.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                // 先尝试获取旧的 modifier
                AttributeModifier old = maxHealth.getModifier(ETERNITY_HEALTH_MODIFIER);
                double totalReduce = -reduce;
                if (old != null) {
                    totalReduce += old.getAmount(); // 累加旧值
                    maxHealth.removeModifier(old);  // 移除旧的，避免重复
                }
                AttributeModifier mod = new AttributeModifier(
                        ETERNITY_HEALTH_MODIFIER,
                        "eternity_reduce",
                        totalReduce,
                        AttributeModifier.Operation.ADDITION
                );
                maxHealth.addPermanentModifier(mod);
//                System.out.println(target.getMaxHealth());
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
        // 回响 (Echo)
        if (source.is(FDDamageSource.ECHO)) {
            MobEffectInstance current = target.getEffect(FDPotionEffects.VOID_STRIKE.get());
            if (current != null && current.getAmplifier() == 5) {
                float spreadDamage = amount * 3.0f; // 最终伤害300%
                List<LivingEntity> nearby = target.level().getEntitiesOfClass(LivingEntity.class,
                        target.getBoundingBox().inflate(5), e -> e != target && e != attackerLiving);
                for (LivingEntity e : nearby) {
                    if (attackerLiving instanceof Player) {
                        e.hurt(attackerLiving.damageSources().playerAttack((Player) attackerLiving), spreadDamage);
                    } else {
                        e.hurt(attackerLiving.damageSources().mobAttack(attackerLiving), spreadDamage);
                    }

                    EntityPositionSource start = new EntityPositionSource(e, e.getBbHeight() / 2);
                    ((ServerLevel) attackerLiving.level()).sendParticles(new VibrationParticleOption(start, 10), target.position().x, e.position().y + target.getBbHeight() / 2, target.position().z, 1, 0, 0, 0, 0);
                }
                target.removeEffect(FDPotionEffects.VOID_STRIKE.get());
            }
        }

        event.setAmount(amount);
    }

    // 在玩家死亡时清理
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            clearEternity(player);
        }
    }

    // 在玩家上床时清理
    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        clearEternity(event.getEntity());
    }

    //用于清理永劫计数
    public static void clearEternity(LivingEntity entity) {
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && maxHealth.getModifier(ETERNITY_HEALTH_MODIFIER) != null) {
            maxHealth.removeModifier(ETERNITY_HEALTH_MODIFIER);
        }
    }

    //终焉伤害禁用传送
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
        //针对末影生物的禁用传送
        if (event.getEntityLiving().hasEffect(FDPotionEffects.TELEPORT_BLOCKED.get())) {
            event.setCanceled(true);
        }
    }
}
