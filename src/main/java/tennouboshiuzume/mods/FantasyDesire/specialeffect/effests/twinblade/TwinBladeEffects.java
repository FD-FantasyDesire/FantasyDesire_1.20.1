package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.twinblade;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.entity.IShootable;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.RayTraceHelper;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

import java.awt.*;
import java.util.Optional;
import java.util.Random;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TwinBladeEffects {
    //    双持共击
    @SubscribeEvent
    public static void onTwinSlash(SlashBladeEvent.DoSlashEvent event) {
        ItemStack blade = event.getBlade();
        ItemStack offblade = event.getUser().getOffhandItem();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade) || !(offblade.getItem() instanceof ItemFantasySlashBlade))
            return;
        if (!(event.getUser() instanceof Player player)) return;
//        获取主手Capability
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
//        获取副手Capability
        ISlashBladeState offstate = CapabilityUtils.getBladeState(offblade);
        IFantasySlashBladeState offfdState = CapabilityUtils.getFantasyBladeState(offblade);
//        检查主手效果激活
        boolean mainActive = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.TwinSet, player, "item.fantasydesire.twin_blade");
//        检查副手效果激活
        boolean offActive = CapabilityUtils.isSpecialEffectActiveForItem(offstate, FDSpecialEffects.TwinSet, player, "item.fantasydesire.twin_blade");
//        检查两把刀是否为不同形态
        if (fdState.getSpecialType().equals(offfdState.getSpecialType())) return;
        int color = state.getColorCode();
        int offcolor = offstate.getColorCode();
        if (mainActive && offActive) {
//            切斩
            double damage = event.getDamage();
            AddonSlashUtils.doAddonSlash(player, event.getRoll() - 180, player.getYRot(), 0, offcolor, Vec3.ZERO, false, false, damage, KnockBacks.cancel);
//            幻影共击
            if (getLockTarget(player).isPresent()){
                LivingEntity target = (LivingEntity) getLockTarget(player).get();
                Vec3 offset = target.position().subtract(player.position()).scale(2).add(player.position());
                summonSlash(player,event.getRoll(),invertColor(offcolor),offset,damage);
                summonSlash(player,event.getRoll() - 180,invertColor(color),offset,damage);
            }
        }
    }

    private static void summonSlash(LivingEntity player,float roll,int color,Vec3 pos,double damage){
        EntitySlashEffect jc = new EntitySlashEffect(SlashBlade.RegistryEvents.SlashEffect, player.level());
        jc.setPos(pos.x, pos.y+player.getBbHeight()/2, pos.z);
        jc.setOwner(player);
        jc.setRotationRoll(roll);
        jc.setYRot(player.getYRot() - 180);
        jc.setXRot(0);
        jc.setColor(color);
        jc.setMute(false);
        jc.setIsCritical(false);
        jc.setDamage(1);
        jc.setKnockBack(KnockBacks.cancel);
        if (player != null) {
            player.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent((rank) -> {
                jc.setRank(rank.getRankLevel(player.level().getGameTime()));
            });
        }
        player.level().addFreshEntity(jc);
    }
    private static Optional<Entity> getLockTarget(LivingEntity sender) {
        Level worldIn = sender.level();

        // 从Capability里获取目标
        Entity lockTarget = sender.getMainHandItem()
                .getCapability(ItemSlashBlade.BLADESTATE)
                .filter(state -> state.getTargetEntity(worldIn) != null)
                .map(state -> state.getTargetEntity(worldIn))
                .orElse(null);

        // 如果武器里有目标，直接返回
        if (lockTarget != null) {
            return Optional.of(lockTarget);
        }

        // 否则用 RayTraceHelper 尝试寻找
        return RayTraceHelper.rayTrace(
                        sender.level(),
                        sender,
                        sender.getEyePosition(1.0f),
                        sender.getLookAngle(),
                        4, 4,
                        (e) -> true
                )
                .filter(r -> r.getType() == HitResult.Type.ENTITY)
                .filter(r -> {
                    EntityHitResult er = (EntityHitResult) r;
                    Entity target = er.getEntity();

                    boolean isMatch = true;
                    if (target instanceof LivingEntity)
                        isMatch = TargetSelector.test.test(sender, (LivingEntity) target);

                    if (target instanceof IShootable)
                        isMatch = ((IShootable) target).getShooter() != sender;

                    return isMatch;
                })
                .map(r -> ((EntityHitResult) r).getEntity());
    }

    public static int invertColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int invR = 255 - r;
        int invG = 255 - g;
        int invB = 255 - b;

        return (invR << 16) | (invG << 8) | invB;
    }
}


