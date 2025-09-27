package tennouboshiuzume.mods.FantasyDesire.utils;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.VectorHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static tennouboshiuzume.mods.FantasyDesire.utils.VecMathUtils.rotateAroundAxis;

public class AddonSlashUtils {
    public static EntitySlashEffect doAddonSlash(LivingEntity playerIn, float roll, float YRot, float XRot, int colorCode, Vec3 centerOffset, boolean mute, boolean critical, double damage, KnockBacks knockback) {
        if (playerIn.level().isClientSide()) {
            return null;
        } else {
            Vec3 pos = playerIn.position().add(0.0, (double) playerIn.getEyeHeight() * 0.75, 0.0).add(playerIn.getLookAngle().scale(0.30000001192092896));
            pos = pos.add(VectorHelper.getVectorForRotation(-90.0F, playerIn.getViewYRot(0.0F)).scale(centerOffset.y)).add(VectorHelper.getVectorForRotation(0.0F, playerIn.getViewYRot(0.0F) + 90.0F).scale(centerOffset.z)).add(playerIn.getLookAngle().scale(centerOffset.z));
            EntitySlashEffect jc = new EntitySlashEffect(SlashBlade.RegistryEvents.SlashEffect, playerIn.level());
            jc.setPos(pos.x, pos.y, pos.z);
            jc.setOwner(playerIn);
            jc.setRotationRoll(roll);
            jc.setYRot(YRot);
            jc.setXRot(XRot);
            jc.setColor(colorCode);
            jc.setMute(mute);
            jc.setIsCritical(critical);
            jc.setDamage(damage);
            jc.setKnockBack(knockback);
            if (playerIn != null) {
                playerIn.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent((rank) -> {
                    jc.setRank(rank.getRankLevel(playerIn.level().getGameTime()));
                });
            }
            playerIn.level().addFreshEntity(jc);
            return jc;
        }
    }
}
