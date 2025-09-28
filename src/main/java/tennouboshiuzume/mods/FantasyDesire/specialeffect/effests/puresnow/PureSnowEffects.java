package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.puresnow;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ColorUtils;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PureSnowEffects {
//    虹光通量
    @SubscribeEvent
    public static void updateEvent(SlashBladeEvent.UpdateEvent event) {
        ItemStack blade = event.getBlade();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        if (!(event.getEntity() instanceof Player player)) return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        if (!state.getTranslationKey().equals("item.fantasydesire.pure_snow")) return;
        boolean mainActive = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.RainbowFlux, player, "item.fantasydesire.pure_snow");
        if (mainActive) {
            int eachColorZone = 15;
            int totalSteps = eachColorZone*7;
            long tickCount = player.level().getGameTime();
            int timeStep = (int) (tickCount % totalSteps);
            int color = ColorUtils.getSmoothTransitionColor(timeStep, totalSteps, true);
            state.setColorCode(color);
            int stepsPerType = totalSteps / 7;
            int damageTypeIndex = ((timeStep + stepsPerType / 2) * 7 / totalSteps) % 7;
            fdState.setSpecialAttackEffect(damageTypes[damageTypeIndex]);
        }
    }
//    虹羽七刃剑
//    @SubscribeEvent
//    public static void onSlash(SlashBladeEvent.DoSlashEvent event) {
//        ItemStack blade = event.getBlade();
//        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
//            return;
//        if (!(event.getUser() instanceof Player player)) return;
//        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
//        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
//        if (!state.getTranslationKey().equals("item.fantasydesire.pure_snow")) return;
//        if (!CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.RainbowFlux, player, "item.fantasydesire.pure_snow")) return;
//        double damage = event.getDamage();
//        for (int i = 0; i < 7; i++) {
//            int cl = ColorUtils.getSmoothTransitionColor(i,7,true);
//            EntitySlashEffect jc = new EntitySlashEffect(SlashBlade.RegistryEvents.SlashEffect, player.level());
//            Vec3 pos = player.position().add(new Vec3(0,0,jc.getBbHeight()/2).yRot(-(float) Math.toRadians(360 /7*i+player.getYRot())));
//            jc.setPos(pos.x, pos.y+player.getBbHeight()/2, pos.z);
//            jc.setOwner(player);
//            jc.setRotationRoll(0);
//            jc.setYRot((float) 360 /7*i+player.getYRot());
//            jc.setXRot(0);
//            jc.setColor(cl);
//            jc.setRotationRoll(0f);
//            jc.setMute(true);
//            jc.setIsCritical(false);
//            jc.setDamage(damage);
//            jc.setKnockBack(KnockBacks.cancel);
//            if (player != null) {
//                player.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent((rank) -> {
//                    jc.setRank(rank.getRankLevel(player.level().getGameTime()));
//                });
//            }
//            player.level().addFreshEntity(jc);
//        }
//    }
    /**
     * Rodrigues 旋转公式：把向量 v 绕单位轴 axis 旋转 angle（弧度）
     */
    private static Vec3 rotateAroundAxis(Vec3 v, Vec3 axis, double angle) {
        Vec3 k = axis.normalize();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        Vec3 term1 = v.scale(cos);
        Vec3 term2 = k.cross(v).scale(sin); // k x v
        double kDotV = k.dot(v);
        Vec3 term3 = k.scale(kDotV * (1.0 - cos));
        return term1.add(term2).add(term3);
    }


    //    棱光通量
    public static String[] damageTypes = {"wrath", "lust", "sloth", "gluttony", "gloom", "pride", "envy"};

}
