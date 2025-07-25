package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.chikeflare;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.MathUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.VecMathUtils;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChikeFlareEffects {
    //    @SubscribeEvent
//    public static void OnBypassAttack(LivingAttackEvent event){
//        if (!(event.getEntity() instanceof Player player)) return;
//        if(!(player.getMainHandItem().getItem() instanceof ItemFantasySlashBlade)) return;
//        ItemStack blade = player.getMainHandItem();
//        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
//        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
//        RandomSource random = player.getRandom();
//        if (CapabilityUtils.isSpecialEffectActiveForItem(state,FDSpecialEffects.SoulShield,player,"item.fantasydesire.chikeflare")){
//            if (event.getSource().getEntity()instanceof LivingEntity attacker){
//                if (attacker.distanceTo(player)>2.5) return;
//                Vec3 VecToAttacker =  VecMathUtils.calculateDirectionVec(player,attacker);
//                float[] YP = VecMathUtils.getYawPitchFromVec(VecToAttacker);
//                AddonSlashUtils.doAddonSlash(player,random.nextInt(180),YP[0],0,0x00FFFF,VecToAttacker,false,false,0.2f, KnockBacks.cancel);
//                event.setCanceled(true);
//            }
//        }
//    }
//    灵魂之盾
    @SubscribeEvent
    public static void OnBypassAttack(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player.getMainHandItem().getItem() instanceof ItemFantasySlashBlade)) return;
        ItemStack blade = player.getMainHandItem();
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        RandomSource random = player.getRandom();
        if (CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.SoulShield, player, "item.fantasydesire.chikeflare")) {
//            基于充能提升触发率（最大50%）
            if (event.getSource().getEntity() instanceof LivingEntity attacker && MathUtils.RandomCheck(fdState.getSpecialCharge()*0.5f)) {
                Vec3 VecToAttacker = VecMathUtils.calculateDirectionVec(player, attacker);
                float[] YP = VecMathUtils.getYawPitchFromVec(VecToAttacker);
//                自动防反
                AddonSlashUtils.doAddonSlash(player, random.nextInt(180), YP[0], YP[1], 0x00FFFF, VecToAttacker, false, false, 0.2f, KnockBacks.cancel);
//                吸收成功格挡的伤害
                player.setAbsorptionAmount(Mth.clamp(player.getAbsorptionAmount()+event.getAmount(),0,20));
                event.setCanceled(true);
            }
//            自动防反失败，充能+1
            fdState.setSpecialCharge(Mth.clamp(fdState.getSpecialCharge()+1,0,fdState.getMaxSpecialCharge()));
//            减缓防反失败后的伤害
            event.setAmount(Math.min(event.getAmount(),10));
        }
    }
//    不屈之魂
    @SubscribeEvent
    public static void OnDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player.getMainHandItem().getItem() instanceof ItemFantasySlashBlade)) return;
        ItemStack blade = player.getMainHandItem();
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        System.out.println(event.getSource().getEntity());
        if (CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.ImmortalSoul, player, "item.fantasydesire.chikeflare")) {
            int soul = state.getProudSoulCount();
            float baseattack = state.getBaseAttackModifier();
            if (soul>=1000){
//                死亡时扣除1000耀魂
                state.setProudSoulCount(soul-1000);
//                ！永久！提升基础面板
                state.setBaseAttackModifier(baseattack+2.0f);
//                充能+10
                fdState.setSpecialCharge(Mth.clamp(fdState.getSpecialCharge()+10,0,fdState.getMaxSpecialCharge()));
//                给予回复5和抗性5
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,20*6,4));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,20*6,4));
                event.setCanceled(true);
            }
        }
    }

//



}
