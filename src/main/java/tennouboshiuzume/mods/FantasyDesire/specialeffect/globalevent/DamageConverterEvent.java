package tennouboshiuzume.mods.FantasyDesire.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageTypes;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;

import tennouboshiuzume.mods.FantasyDesire.utils.DamageUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.VelocityUtils;

@Mod.EventBusSubscriber
public class DamageConverterEvent {
    public static final Capability<IFantasySlashBladeState> FDBLADESTATE = CapabilityManager.get(new CapabilityToken<IFantasySlashBladeState>() {
    });
    public static final Capability<ISlashBladeState> BLADESTATE = CapabilityManager.get(new CapabilityToken<ISlashBladeState>() {
    });
//    根据伤害类型作出不同的修正
//    暴怒 点燃敌人
//    色欲 使攻击者回复0.2生命值
//    暴食 使攻击者回复0.2饥饿值
//    忧郁 消耗敌人的氧气条
//    傲慢 对拥有护甲的敌人1.5x伤害
//    嫉妒 对生命值大于你的敌人造成3x伤害
//    次元 伤害的50%会转化为真实伤害
//    永劫 伤害的10%转化为生命值上限削减
//    吸收 使攻击者吸收同等生命值，溢出部分的10%转化为额外生命，最大100
//    决断 伤害的20%转化为真实伤害

    @SubscribeEvent
    public static void OnHurt(LivingHurtEvent event) {
        System.out.println(event.getAmount());
        DamageSource source = event.getSource();
        LivingEntity player = (LivingEntity) source.getEntity(); // 攻击者
        LivingEntity target = event.getEntity(); // 受击者
        float original = event.getAmount();
        if (player instanceof Player) {
            Holder<DamageType> holder = source.typeHolder();
            holder.unwrapKey().ifPresent(key -> System.out.println("伤害类型ID: " + key.location()));
            if (holder.unwrapKey().map(key ->
                    key.location().getNamespace().equals(FantasyDesire.MODID)
            ).orElse(false)) {
                return;
            }
            ItemStack blade = ((Player) player).getMainHandItem();
            if (blade.getItem() instanceof ItemFantasySlashBlade) {
                blade.getCapability(FDBLADESTATE).ifPresent((s) -> {
                    blade.getCapability(BLADESTATE).ifPresent((b) -> {
                        if (s.getSpecialAttackEffect().equals("dimension") && b.getTranslationKey().equals("item.fantasydesire.chikeflare")) {
                            event.setAmount(original * 0.5f);
                            target.invulnerableTime = 0;
                            target.hurtTime = 0;
                            target.hurt(DamageUtils.createWithEntity(FDDamageTypes.DIMENSION, (LivingEntity) player, player.level().registryAccess()), original * 0.5f);
                        }
                        if (b.getTranslationKey().equals("item.fantasydesire.pure_snow")) {
                            String effect = s.getSpecialAttackEffect();
                            event.setAmount(0);
                            if ("wrath".equals(effect)) {
//                                暴怒，对格挡敌人造成三倍伤害
//                                算作火焰伤害
                                float multiple = target.isBlocking() ? 3 : 1;
                                target.invulnerableTime = 0;
                                target.hurtTime = 0;
                                target.hurt(DamageUtils.createWithEntity(FDDamageTypes.WRATH, (LivingEntity) player, player.level().registryAccess()), original * multiple);
                            } else if ("lust".equals(effect)) {
                                // 色欲，将伤害的一部分转换为生命值
                                target.invulnerableTime = 0;
                                target.hurtTime = 0;
                                target.hurt(DamageUtils.createWithEntity(FDDamageTypes.LUST, (LivingEntity) player, player.level().registryAccess()), original * 0.8f);
                                player.heal(original * 0.2f);
                            } else if ("sloth".equals(effect)) {
                                // 怠惰，玩家的移动速度越低，伤害倍率越高，算作摔落伤害
                                double attackerSpeed = player.getDeltaMovement().length();
                                double targetSpeed = target.getDeltaMovement().length();
                                double safeTargetSpeed = Math.max(targetSpeed, 0.05);
                                double speedRatio = Math.min(attackerSpeed / safeTargetSpeed, 3.0);
                                target.invulnerableTime = 0;
                                target.hurtTime = 0;
                                target.hurt(DamageUtils.createWithEntity(FDDamageTypes.SLOTH, (LivingEntity) player, player.level().registryAccess()), (float) speedRatio);

                            } else if ("gluttony".equals(effect)) {
                                // 暴食，伤害的一半转化为玩家的饱食度和饱和度
                                target.invulnerableTime = 0;
                                target.hurtTime = 0;
                                target.hurt(DamageUtils.createWithEntity(FDDamageTypes.GLUTTONY, (LivingEntity) player, player.level().registryAccess()), original * 0.5f);
                                ((Player) player).getFoodData().eat((int) (original * 0.3f), original * 0.2f);

                            } else if ("gloom".equals(effect)) {
                                // 忧郁，优先消耗敌人的氧气，如果没有氧气则造成两倍伤害
                                int currentAir = target.getAirSupply();
                                target.invulnerableTime = 0;
                                target.hurtTime = 0;
                                if (currentAir <= 0) {
                                    target.hurt(DamageUtils.createWithEntity(FDDamageTypes.GLOOM, (LivingEntity) player, player.level().registryAccess()), original * 2);
                                } else {
                                    target.setAirSupply((int) (currentAir - original));
                                }

                            } else if ("pride".equals(effect)) {
                                // 傲慢，对拥有护甲的敌人造成三倍伤害，同时具备两种属性会叠乘
                                float multiple = target.getArmorValue() > 0 ? 3 : 1;
                                target.invulnerableTime = 0;
                                target.hurtTime = 0;
                                target.hurt(DamageUtils.createWithEntity(FDDamageTypes.PRIDE, (LivingEntity) player, player.level().registryAccess()), original * multiple);

                            } else if ("envy".equals(effect)) {
                                // 嫉妒，对血量比你高的敌人造成1.5倍伤害，算作荆棘伤害
                                float multiple = target.getHealth() > player.getHealth() ? 1.5f : 1.0f;
                                target.invulnerableTime = 0;
                                target.hurtTime = 0;
                                target.hurt(DamageUtils.createWithEntity(FDDamageTypes.ENVY, (LivingEntity) player, player.level().registryAccess()), original * multiple);
                            }
//                            System.out.println(original);
                        }
                    });
                });
            }
        }
    }

    @SubscribeEvent
    public static void OnDamage(LivingDamageEvent event) {
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        LivingEntity target = event.getEntity();
        Holder<DamageType> holder = source.typeHolder();
        float original = event.getAmount();
    }


    public static String[] damageTypes = {"wrath", "lust", "sloth", "gluttony", "gloom", "pride", "envy"};

}
