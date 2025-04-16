package tennouboshiuzume.mods.FantasyDesire.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
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
import tennouboshiuzume.mods.FantasyDesire.utils.ColorUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.DamageUtils;

@Mod.EventBusSubscriber
public class idletest {
    public static final Capability<IFantasySlashBladeState> FDBLADESTATE = CapabilityManager.get(new CapabilityToken<IFantasySlashBladeState>() {
    });
    public static final Capability<ISlashBladeState> BLADESTATE = CapabilityManager.get(new CapabilityToken<ISlashBladeState>() {
    });

    @SubscribeEvent
    public static void fdupdateevent(SlashBladeEvent.UpdateEvent event) {
        ItemStack blade = event.getBlade();
        Entity player = event.getEntity();
        if (blade.getItem() instanceof ItemFantasySlashBlade) {
            blade.getCapability(BLADESTATE).ifPresent((s) -> {
                blade.getCapability(FDBLADESTATE).ifPresent((b) -> {
                    if (!s.getTranslationKey().equals("item.fantasydesire.pure_snow")) return;
                    int color = ColorUtils.getSmoothTransitionColor(player.level().getDayTime() % 126, 126, true);
                    s.setColorCode(color);
                    int timeStep = (int) ((player.level().getDayTime() + 8) % 126);
                    int damageTypeIndex = (timeStep / 18);
                    b.setSpecialAttackEffect(damageTypes[damageTypeIndex]);
                });
            });
        }
    }
    //      根据伤害类型作出不同的修正
//        暴怒 点燃敌人
//        色欲 使攻击者回复0.2生命值
//        暴食 使攻击者回复0.2饥饿值
//        忧郁 消耗敌人的氧气条
//        傲慢 对拥有护甲的敌人1.5x伤害
//        嫉妒 对生命值大于你的敌人造成3x伤害
//        次元 伤害的50%会转化为魔法伤害
//        永劫 伤害的10%转化为生命值上限削减
//        吸收 使攻击者吸收同等生命值，溢出部分的10%转化为额外生命，最大100
//        决断 伤害的20%转化为真实伤害

//    @SubscribeEvent
//    public static void OnHurt(LivingHurtEvent event) {
//        DamageSource source = event.getSource();
//        Holder<DamageType> holder = source.typeHolder();
//        holder.unwrapKey().ifPresent(key -> System.out.println("伤害类型ID: " + key.location()));
//        if (holder.unwrapKey().map(key ->
//                key.location().getNamespace().equals(FantasyDesire.MODID)
//        ).orElse(false)) {
//            return;
//        }
//        Entity attacker = source.getEntity(); // 攻击者
//        LivingEntity target = event.getEntity(); // 受击者
//        float original = event.getAmount();
//        if (attacker instanceof Player) {
//            ItemStack blade = ((Player) attacker).getMainHandItem();
//            if (blade.getItem() instanceof ItemFantasySlashBlade) {
//                blade.getCapability(FDBLADESTATE).ifPresent((s) -> {
//                    blade.getCapability(BLADESTATE).ifPresent((b) -> {
//                        if (s.getSpecialAttackEffect().equals("dimension") && b.getTranslationKey().equals("item.fantasydesire.chikeflare")) {
//                            event.setAmount(original * 0.5f);
//                            target.invulnerableTime = 0;
//                            target.hurtTime = 0;
//                            target.hurt(DamageUtils.createWithEntity(FDDamageTypes.DIMENSION, (LivingEntity) attacker, attacker.level().registryAccess()), original * 0.5f);
//                        }
//                    });
//                });
//            }
//        }
//    }

//    @SubscribeEvent
//    public static void OnDamage(LivingDamageEvent event) {
//        DamageSource source = event.getSource();
//        Entity attacker = source.getEntity();
//        LivingEntity target = event.getEntity();
//        Holder<DamageType> holder = source.typeHolder();
//        float original = event.getAmount();
////        if (attacker instanceof Player) {
////            ItemStack blade = ((Player) attacker).getMainHandItem();
////            if (blade.getItem() instanceof ItemFantasySlashBlade) {
////                blade.getCapability(FDBLADESTATE).ifPresent((s) -> {
////                    blade.getCapability(BLADESTATE).ifPresent((b) -> {
////
////                        if (s.getSpecialAttackEffect().equals("dimension") && b.getTranslationKey().equals("item.fantasydesire.chikeflare")){
////                            event.setAmount(original*0.5f);
////                        }
////
////                    });
////                });
////            }
////        }
////
////        if (holder.unwrapKey().filter(key -> key.equals(FDDamageTypes.ETERNITY)).isPresent() && target.getArmorValue()>0) {
////            event.setAmount(original * 1.5f);
////        }
////
////        if (holder.unwrapKey().filter(key -> key.equals(FDDamageTypes.PRIDE)).isPresent() && target.getArmorValue()>0) {
////            event.setAmount(original * 1.5f);
////        }

//    }


    public static String[] damageTypes = {"wrath", "lust", "sloth", "gluttony", "gloom", "pride", "envy"};

}
