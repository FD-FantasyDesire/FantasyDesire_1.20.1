package tennouboshiuzume.mods.FantasyDesire.specialeffects.effects.crimsonscythe;

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
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;
import tennouboshiuzume.mods.FantasyDesire.utils.VecMathUtils;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CrimsonScytheEffects {
    private static final String TRANSLATION_KEY = "item.fantasydesire.crimson_scythe";

    @SubscribeEvent
    public static void onSlash(SlashBladeEvent.DoSlashEvent event) {
        ItemStack blade = event.getBlade();
        LivingEntity entity = event.getUser();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        // 获取主手Capability
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        int color = state.getColorCode();
        // 使用 SEConditionMatcher 检查 CrimsonStrike（狩魂 爪刃斩击）
        CapabilityUtils.BladeContext mainCtx = CapabilityUtils.SEConditionMatcher.of(entity)
                .requireTranslation(TRANSLATION_KEY)
                .requireSE(FDSpecialEffectsRegistry.CrimsonStrike)
                .match();
        if (mainCtx != null) {
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

        // 使用 SEConditionMatcher 检查 BloodDrain（幻猎 发射抓钩幻影剑，击中时拉近敌人）
        CapabilityUtils.BladeContext offCtx = CapabilityUtils.SEConditionMatcher.of(entity)
                .requireTranslation(TRANSLATION_KEY)
                .requireSE(FDSpecialEffectsRegistry.BloodDrain)
                .match();
        if (offCtx != null) {
            IFantasySlashBladeState fdState = offCtx.fantasyState;
            int sweepLevel = blade.getEnchantmentLevel(Enchantments.SWEEPING_EDGE);
            float lockDistance = 15 + sweepLevel * 10;
            int maxVolleyCount = 3 + sweepLevel;
            float angleDeg = 30 + sweepLevel * 10;
            List<LivingEntity> targets = FDTargetSelector.getTargetsInSight(entity, lockDistance, angleDeg, true, null);
            targets.sort((e1, e2) -> Double.compare(e2.distanceToSqr(entity), e1.distanceToSqr(entity)));

            int validTargetCount = targets.size();
            if (validTargetCount == 0 && state.getTargetEntity(entity.level()) != null) {
                validTargetCount = 1;
            }
            int volleyCount = Math.min(validTargetCount, maxVolleyCount);

            Random random = new Random();
            for (int i = 0; i < volleyCount; i++) {
                if (!CapabilityUtils.tryConsumeSpecialCharge(fdState, 1, entity, null)) {
                    break;
                }
                EntityFDHuntSword ss = new EntityFDHuntSword(FDEntitys.FDHuntSword.get(), entity.level());
                ss.setIsCritical(false);
                ss.setOwner(entity);
                ss.setColor(state.getColorCode());
                ss.setRoll(random.nextInt(180));
                ss.setDamage(0.001);
                ss.setSpeed(1.0f);
                ss.setStandbyMode(EntityFDPhantomSword.StandbyMode.PLAYER);
                ss.setMovingMode(EntityFDPhantomSword.MovingMode.SEEK);
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

    @SubscribeEvent
    public static void onHit(SlashBladeEvent.HitEvent event) {
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(event.getBlade(), event.getUser())
                .requireTranslation(TRANSLATION_KEY)
                .requireSE(FDSpecialEffectsRegistry.BloodDrain)
                .match();
        if (ctx != null) {
            IFantasySlashBladeState fdState = ctx.fantasyState;
            CapabilityUtils.addSpecialCharge(fdState, 1);
        }
    }
}
