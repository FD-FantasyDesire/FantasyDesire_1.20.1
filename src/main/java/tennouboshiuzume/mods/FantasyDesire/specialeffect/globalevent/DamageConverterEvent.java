package tennouboshiuzume.mods.FantasyDesire.specialeffect.globalevent;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageSource;
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
    static UUID ETERNITY_HEALTH_MODIFIER = UUID.fromString("a5b1b2f0-2f3c-4e3b-8a71-123456789abc");
//    根据伤害类型作出不同的效果追加
//    暴怒 对持盾敌人造成3倍，穿透护甲的伤害
//    色欲 使攻击者回复0.2生命值
//    暴食 使攻击者回复0.2饥饿值
//    忧郁 消耗敌人的氧气条
//    傲慢 对拥有护甲的敌人1.5x伤害
//    嫉妒 对生命值大于你的敌人造成3x伤害
//    次元 伤害的50%会转化为真实伤害
//    永劫 伤害的10%转化为生命值上限削减
//    吸收 使攻击者吸收同等生命值，溢出部分的10%转化为额外生命，最大100
//    决断 伤害的20%转化为真实伤害
//    回响 造成伤害的同时，使目标浸染虚空侵蚀
    
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
            System.out.println(fdDamageType);
            if (fdDamageType!=null&&!fdDamageType.equals("Null")){
                DamageSource fds = FDDamageSource.getEntityDamageSource(livingEntity.level(),FDDamageSource.fromString(fdDamageType),livingEntity);
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
        if (source.is(FDDamageSource.OMEGA)){
            if (target instanceof Player){

            }
        }
    }
//    伤害造成时事件处理
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void OnHurt(LivingHurtEvent event){
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();

        // 攻击者
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity)) return;
        LivingEntity attackerLiving = (LivingEntity) attacker;
        // 决断 (Resolution)
        if (source.is(FDDamageSource.RESOLUTION)) {
            float diff = target.getMaxHealth() - attackerLiving.getHealth();
            float extraDamage = diff * 0.25f;
            if (attackerLiving instanceof Player){
                target.hurt(attackerLiving.damageSources().playerAttack((Player) attackerLiving),extraDamage);
            }else {
                target.hurt(attackerLiving.damageSources().mobAttack(attackerLiving),extraDamage);
            }
        }
        // 暴怒 (Wrath)
        if (source.is(FDDamageSource.WRATH)) {
            if (target.isBlocking()) {
                event.setAmount(amount * 3f);
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
            event.setAmount((float)(amount * factor));
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
            target.setAirSupply(Math.max(0, oxygen - (int)(amount * 3)));
        }
        // 傲慢 (Pride)
        if (source.is(FDDamageSource.PRIDE)) {
            if (target.getArmorValue() > 0) {
                event.setAmount(amount * 1.5f);
            }
        }
        // 嫉妒 (Envy)
        if (source.is(FDDamageSource.ENVY)) {
            if (target.getHealth() > attackerLiving.getHealth()) {
                event.setAmount(amount * 3f);
            }
        }
    }
//    伤害最终结算（护甲后，实体更新前）事件处理
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void OnDamage(LivingDamageEvent event){
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();

        // 攻击者
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity)) return;
        LivingEntity attackerLiving = (LivingEntity) attacker;
        // 永劫 (Eternity)
        if (source.is(FDDamageSource.ETERNITY)) {
            // 附加效果：削减目标最大生命值
            float reduce = amount * 0.1f;
            AttributeInstance maxHealth = target.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.removeModifier(ETERNITY_HEALTH_MODIFIER); // 先清理旧的
                AttributeModifier mod = new AttributeModifier(
                        ETERNITY_HEALTH_MODIFIER,
                        "eternity_reduce",
                        -reduce, // 负数就是减血上限
                        AttributeModifier.Operation.ADDITION
                );
                maxHealth.addPermanentModifier(mod);
            }
        }
        //吸收（Absorb）
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
            float spreadDamage = amount * 3.0f; // 最终伤害300%
            List<LivingEntity> nearby = target.level().getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(5), e -> e != target && e != attackerLiving);
            for (LivingEntity e : nearby) {
                if (attackerLiving instanceof Player){
                    e.hurt(attackerLiving.damageSources().playerAttack((Player) attackerLiving),spreadDamage);
                }else {
                    e.hurt(attackerLiving.damageSources().mobAttack(attackerLiving),spreadDamage);
                }
            }
        }
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
}
