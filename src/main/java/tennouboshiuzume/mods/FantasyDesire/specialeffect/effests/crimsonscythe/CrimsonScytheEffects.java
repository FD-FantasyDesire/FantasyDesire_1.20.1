package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.crimsonscythe;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDHuntSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.TargetUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.VecMathUtils;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CrimsonScytheEffects {
    // 狩魂 爪刃斩击
    @SubscribeEvent
    public static void onSlash(SlashBladeEvent.DoSlashEvent event) {
        ItemStack blade = event.getBlade();
        LivingEntity entity = event.getUser();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        // 获取主手Capability
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        // 检查主手效果激活
        boolean mainActive = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.CrimsonStrike, entity,
                "item.fantasydesire.crimson_scythe");
        int color = state.getColorCode();
        if (mainActive) {
            Vec3 forward = Vec3.directionFromRotation(0, entity.getYRot());
            Vec3 baseForwardUp = Vec3.directionFromRotation(-25, entity.getYRot());
            Vec3 baseForwardDown = Vec3.directionFromRotation(+25, entity.getYRot());
            Vec3 rotatedUp = VecMathUtils.rotateAroundAxis(baseForwardUp, forward, -event.getRoll());
            Vec3 rotatedDown = VecMathUtils.rotateAroundAxis(baseForwardDown, forward, -event.getRoll());
            float[] upYawPitch = VecMathUtils.getYawPitchFromVec(rotatedUp);
            float[] downYawPitch = VecMathUtils.getYawPitchFromVec(rotatedDown);
            double ratio = event.getDamage();
            AddonSlashUtils.doAddonSlash(entity, event.getRoll(), upYawPitch[0], upYawPitch[1], color, 0, Vec3.ZERO,
                    false, false, ratio, KnockBacks.cancel);
            AddonSlashUtils.doAddonSlash(entity, event.getRoll(), downYawPitch[0], downYawPitch[1], color, 0, Vec3.ZERO,
                    false, false, ratio, KnockBacks.cancel);
        }
        boolean offActive = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.BloodDrain,
                entity,
                "item.fantasydesire.crimson_scythe");
        if (offActive) {
            int sweepLevel = blade.getEnchantmentLevel(Enchantments.SWEEPING_EDGE);
            // 横扫之刃附魔增加锁定距离
            float lockDistance = 15 + sweepLevel * 10;
            int volleyCount = 3 + sweepLevel;
            float angleDeg = 30 + sweepLevel * 10;
            List<LivingEntity> targets = TargetUtils.getTargetsInSight(entity, lockDistance, angleDeg, true, null);
            // Sort descending by distance
            targets.sort((e1, e2) -> Double.compare(e2.distanceToSqr(entity), e1.distanceToSqr(entity)));
            Random random = new Random();
            for (int i = 0; i < volleyCount; i++) {
                EntityFDHuntSword ss = new EntityFDHuntSword(FDEntitys.FDHuntSword.get(), entity.level());
                ss.setIsCritical(false);
                ss.setOwner(entity);
                ss.setColor(state.getColorCode());
                ss.setRoll(random.nextInt(180));
                ss.setDamage(0.001);
                ss.setSpeed(1);
                ss.setStandbyMode("PLAYER");
                ss.setMovingMode("SEEK");
                ss.setDelay(120);
                ss.setDelayTicks(1);
                ss.setSeekDelay(5);
                ss.setSeekAngle(36);
                ss.setNoClip(true);
                ss.setMultipleHit(true);
                ss.setStandbyYawPitch(90, -90 + (360 / volleyCount) * i);
                ss.setFireSound(SoundEvents.CHAIN_BREAK, 1, 1.5f);
                ss.setHasTail(true);
                ss.setScale(0.5f);
                ss.setTailNodes(4);
                Entity finalTarget = null;
                if (state.getTargetEntity(entity.level()) != null) {
                    finalTarget = state.getTargetEntity(entity.level());
                } else if (!targets.isEmpty()) {
                    finalTarget = targets.get(i % targets.size());
                }
                if (finalTarget != null) {
                    ss.setTargetId(finalTarget.getId());
                } else {
                    return;
                }
                ss.setPos(entity.position().add(new Vec3(0, entity.getBbHeight() / 2, 0)));
                ss.setCenterOffset(new Vec3(0, entity.getEyeHeight(), 0));
                ss.setOffset(new Vec3(0, 0, -0.75f));
                entity.level().addFreshEntity(ss);
            }
        }
    }

    // 幻猎 幻影剑击中时拉近敌人
    @SubscribeEvent
    public static void onSummonedSwordHitEvent(SlashBladeEvent.SummonedSwordOnHitEntityEvent event) {
        if (event.getSummonedSword().getShooter() instanceof LivingEntity entity) {
            ItemStack blade = entity.getMainHandItem();
            if (!(blade.getItem() instanceof ItemFantasySlashBlade))
                return;
            ISlashBladeState state = CapabilityUtils.getBladeState(blade);
            boolean mainActive = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.BloodDrain,
                    entity,
                    "item.fantasydesire.crimson_scythe");
            if (mainActive) {
                if (event.getTarget() instanceof LivingEntity targetEntity) {
                    if (entity.distanceTo(targetEntity) > 5) {
                        Vec3 motion = entity.position().subtract(targetEntity.position()).normalize().scale(0.8);
                        targetEntity.setDeltaMovement(motion);
                        targetEntity.hurtMarked = true;
                    }
                }
            }
        }
    }
}
