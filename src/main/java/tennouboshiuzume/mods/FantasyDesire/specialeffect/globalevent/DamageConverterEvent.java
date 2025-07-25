package tennouboshiuzume.mods.FantasyDesire.specialeffect.globalevent;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
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
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.DamageUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.TargetUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber
public class DamageConverterEvent {
    public static final Capability<IFantasySlashBladeState> FDBLADESTATE = CapabilityManager.get(new CapabilityToken<IFantasySlashBladeState>() {
    });
    public static final Capability<ISlashBladeState> BLADESTATE = CapabilityManager.get(new CapabilityToken<ISlashBladeState>() {
    });
//    根据伤害类型作出不同的修正
//    暴怒 对持盾3倍，穿透
//    色欲 使攻击者回复0.2生命值
//    暴食 使攻击者回复0.2饥饿值
//    忧郁 消耗敌人的氧气条
//    傲慢 对拥有护甲的敌人1.5x伤害
//    嫉妒 对生命值大于你的敌人造成3x伤害
//    次元 伤害的50%会转化为真实伤害
//    永劫 伤害的10%转化为生命值上限削减
//    吸收 使攻击者吸收同等生命值，溢出部分的10%转化为额外生命，最大100
//    决断 伤害的20%转化为真实伤害
//    回响 造成伤害的300%传播至附近敌人

    @SubscribeEvent
    public static void OnHurt(LivingHurtEvent event) {
//        System.out.println(event.getAmount());
        DamageSource source = event.getSource();
        LivingEntity player = (LivingEntity) source.getEntity(); // 攻击者
        LivingEntity target = event.getEntity(); // 受击者
        float original = event.getAmount();
//        System.out.println(source.is(DamageTypeTags.BYPASSES_SHIELD));
        if (player instanceof Player) {
            Holder<DamageType> holder = source.typeHolder();
//            holder.unwrapKey().ifPresent(key -> System.out.println("伤害类型ID: " + key.location()));
            if (holder.unwrapKey().map(key ->
                    key.location().getNamespace().equals(FantasyDesire.MODID)
            ).orElse(false)) {
                return;
            }
            ItemStack blade = ((Player) player).getMainHandItem();
            if (blade.getItem() instanceof ItemFantasySlashBlade) {
                Optional<ISlashBladeState> stateOpt = blade.getCapability(BLADESTATE).resolve();
                Optional<IFantasySlashBladeState> fdStateOpt = blade.getCapability(FDBLADESTATE).resolve();
                if (stateOpt.isEmpty() || fdStateOpt.isEmpty()) return;
                ISlashBladeState state = stateOpt.get();
                IFantasySlashBladeState fdState = fdStateOpt.get();

                if (fdState.getSpecialAttackEffect().equals("dimension") && state.getTranslationKey().equals("item.fantasydesire.chikeflare")) {
                    event.setAmount(original * 0.5f);
                    resetInvulnerable(target);
                    target.hurt(DamageUtils.createWithEntity(FDDamageTypes.DIMENSION, (LivingEntity) player, player.level().registryAccess()), original * 0.5f);
                }
                if (state.getTranslationKey().equals("item.fantasydesire.pure_snow")) {
                    String effect = fdState.getSpecialAttackEffect();
                    float damageConvert = 0.8f;
                    event.setAmount(original * (1-damageConvert));
                    float damageRemain = original * damageConvert;
//                            剩余一半伤害转换为对应罪孽伤害
                    if ("wrath".equals(effect)) {
//                                暴怒，对格挡敌人造成三倍伤害
//                                算作火焰伤害
                        float multiple = target.isBlocking() ? 3 : 1;
                        resetInvulnerable(target);
                        target.hurt(DamageUtils.createWithEntity(FDDamageTypes.WRATH, (LivingEntity) player, player.level().registryAccess()), damageRemain * multiple);
                    } else if ("lust".equals(effect)) {
//                                色欲，将伤害的一部分转换为生命值
//                                算作凋零伤害
                        resetInvulnerable(target);
                        target.hurt(DamageUtils.createWithEntity(FDDamageTypes.LUST, (LivingEntity) player, player.level().registryAccess()), damageRemain * 0.8f);
                        player.heal(original * 0.2f);
                    } else if ("sloth".equals(effect)) {
//                                 怠惰，玩家的移动速度越低，伤害倍率越高
//                                 算作摔落伤害
                        double attackerSpeed = player.getDeltaMovement().length();
                        double targetSpeed = target.getDeltaMovement().length();
                        double safeTargetSpeed = Math.max(targetSpeed, 0.05);
                        double speedRatio = Math.min(attackerSpeed / safeTargetSpeed, 3.0);
                        resetInvulnerable(target);
                        target.hurt(DamageUtils.createWithEntity(FDDamageTypes.SLOTH, (LivingEntity) player, player.level().registryAccess()), (float) speedRatio * damageRemain);
                    } else if ("gluttony".equals(effect)) {
//                                暴食，伤害的一半转化为玩家的饱食度和饱和度
//                                算作饥饿伤害，无视药水伤害抗性
                        resetInvulnerable(target);
                        target.hurt(DamageUtils.createWithEntity(FDDamageTypes.GLUTTONY, (LivingEntity) player, player.level().registryAccess()), damageRemain * 0.5f);
                        ((Player) player).getFoodData().eat((int) (original * 0.3f), original * 0.2f);
                    } else if ("gloom".equals(effect)) {
//                                忧郁，优先消耗敌人的氧气，如果没有氧气则造成两倍伤害
//                                算作溺水伤害
                        int currentAir = target.getAirSupply();
                        resetInvulnerable(target);
                        if (currentAir <= 0) {
                            target.hurt(DamageUtils.createWithEntity(FDDamageTypes.GLOOM, (LivingEntity) player, player.level().registryAccess()), damageRemain * 2);
                        } else {
                            target.setAirSupply((int) (currentAir - original));
                        }
                    } else if ("pride".equals(effect)) {
//                                傲慢，对拥有护甲的敌人或生命上限是你五倍以上的敌人造成三倍伤害，同时具备两种属性会叠乘
//                                算作魔法伤害
                        float multiple = 1;
                        if (target.getArmorValue() > 0) multiple *= 3;
                        if (target.getMaxHealth() / player.getMaxHealth() >= 5) multiple *= 3;
                        resetInvulnerable(target);
                        target.hurt(DamageUtils.createWithEntity(FDDamageTypes.PRIDE, (LivingEntity) player, player.level().registryAccess()), damageRemain * multiple);
                    } else if ("envy".equals(effect)) {
//                                嫉妒，对当前血量比你高的敌人造成1.5倍伤害
//                                算作荆棘伤害
                        float multiple = target.getHealth() > player.getHealth() ? 1.5f : 1.0f;
                        resetInvulnerable(target);
                        target.hurt(DamageUtils.createWithEntity(FDDamageTypes.ENVY, (LivingEntity) player, player.level().registryAccess()), damageRemain * multiple);
                    }
                }
                if (fdState.getSpecialAttackEffect().equals("echo") && state.getTranslationKey().equals("item.fantasydesire.starless_night")) {
//                    回响伤害
                    int voidlevel = 0;
                    MobEffectInstance effect = player.getEffect(FDPotionEffects.VOID_STRIKE.get());
                    if (effect != null) {
                        voidlevel = effect.getAmplifier();
                        player.addEffect(new MobEffectInstance(FDPotionEffects.VOID_STRIKE.get(), 20 * 10, Math.min(5, voidlevel + 1)));
                    }else {
                        player.addEffect(new MobEffectInstance(FDPotionEffects.VOID_STRIKE.get(), 20 * 10, 0));
                    }
                    event.setAmount(original * (1 + voidlevel));
                    resetInvulnerable(target);
//                    TODO :转移至专门的SE效果
                    List<LivingEntity> targetList = TargetUtils.getNearbyLivingEntities(target, 5.0, Collections.singletonList(player));
                    ParticleUtils.generateRingParticles(ParticleTypes.END_ROD,player.level(),target.getX(),target.getY()+0.2f,target.getZ(),5,20);
                    player.level().playSound(null,target.getX(),target.getY(),target.getZ(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.get(), SoundSource.PLAYERS,0.5f,0.5f);
                    for (LivingEntity areatarget : targetList) {
                        areatarget.hurt(DamageUtils.createWithEntity(FDDamageTypes.ECHO, (LivingEntity) player, player.level().registryAccess()), event.getAmount() * 3f);
                    }
                }
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

    public static void resetInvulnerable(Entity target) {
        target.invulnerableTime = 0;
//        if (target instanceof LivingEntity) {
////            ((LivingEntity) target).hurtTime = 0;
//        }
    }




    public static String[] damageTypes = {"wrath", "lust", "sloth", "gluttony", "gloom", "pride", "envy"};

}
