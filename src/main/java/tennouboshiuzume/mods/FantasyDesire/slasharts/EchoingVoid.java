package tennouboshiuzume.mods.FantasyDesire.slasharts;

import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.VectorHelper;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityEnderSlashEffect;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

public class EchoingVoid {
    public static boolean AntiNTR(LivingEntity entity) {
        return CapabilityUtils.SEConditionMatcher.of(entity)
                .requireTranslation("item.fantasydesire.starless_night")
                .match() != null;
    }

    //
    public static EntitySlashEffect doEnderSlash(LivingEntity playerIn, float roll, float YRot, float XRot,
            int colorCode, float rotationOffset, Vec3 centerOffset, boolean mute, boolean critical, double damage,
            KnockBacks knockback, float scale, int lifetime) {
        if (playerIn.level().isClientSide()) {
            return null;
        } else {
            Vec3 pos = playerIn.position().add(0.0, (double) playerIn.getEyeHeight() * 0.75, 0.0)
                    .add(playerIn.getLookAngle().scale(0.30000001192092896));
            pos = pos.add(VectorHelper.getVectorForRotation(-90.0F, playerIn.getViewYRot(0.0F)).scale(centerOffset.y))
                    .add(VectorHelper.getVectorForRotation(0.0F, playerIn.getViewYRot(0.0F) + 90.0F)
                            .scale(centerOffset.z))
                    .add(playerIn.getLookAngle().scale(centerOffset.z));
            EntityEnderSlashEffect jc = new EntityEnderSlashEffect(FDEntitys.EnderSlashEffect.get(), playerIn.level());
            jc.setPos(pos.x, pos.y, pos.z);
            jc.setOwner(playerIn);
            jc.setLifetime(lifetime);
            jc.setRotationRoll(roll);
            jc.setYRot(YRot);
            jc.setXRot(XRot);
            jc.setRotationOffset(rotationOffset);
            jc.setColor(colorCode);
            jc.setMute(mute);
            jc.setIsCritical(critical);
            jc.setDamage(damage);
            jc.setKnockBack(knockback);
            jc.setScale(scale);
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
