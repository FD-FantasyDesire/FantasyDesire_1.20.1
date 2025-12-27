package tennouboshiuzume.mods.FantasyDesire.utils;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.VectorHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityEnderSlashEffect;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDSlashEffect;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;

public class AddonSlashUtils extends AttackManager {
    public static EntitySlashEffect doAddonSlash(LivingEntity playerIn, float roll, float YRot, float XRot,
            int colorCode, float rotationOffset, Vec3 centerOffset, boolean mute, boolean critical, double damage,
            KnockBacks knockback) {
        if (playerIn.level().isClientSide()) {
            return null;
        } else {
            Vec3 pos = playerIn.position().add(0.0, (double) playerIn.getEyeHeight() * 0.75, 0.0)
                    .add(playerIn.getLookAngle().scale(0.30000001192092896));
            pos = pos.add(VectorHelper.getVectorForRotation(-90.0F, playerIn.getViewYRot(0.0F)).scale(centerOffset.y))
                    .add(VectorHelper.getVectorForRotation(0.0F, playerIn.getViewYRot(0.0F) + 90.0F)
                            .scale(centerOffset.z))
                    .add(playerIn.getLookAngle().scale(centerOffset.z));
            EntitySlashEffect jc = new EntitySlashEffect(SlashBlade.RegistryEvents.SlashEffect, playerIn.level());
            jc.setPos(pos.x, pos.y, pos.z);
            jc.setOwner(playerIn);
            jc.setRotationRoll(roll);
            jc.setYRot(YRot);
            jc.setXRot(XRot);
            jc.setRotationOffset(rotationOffset);
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

    public static EntitySlashEffect doAddonFDSlash(LivingEntity playerIn, float roll, float YRot, float XRot,
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
            EntityFDSlashEffect jc = new EntityFDSlashEffect(FDEntitys.FDSlashEffect.get(), playerIn.level());
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

    public static EntitySlashEffect doAddonEnderSlash(LivingEntity playerIn, float roll, float YRot, float XRot,
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

    public static EntitySlashEffect doAddonSlashWithEvent(LivingEntity playerIn, float roll, float YRot, float XRot,
            int colorCode, float rotationOffset, Vec3 centerOffset, boolean mute, boolean critical, double comboRatio,
            KnockBacks knockback) {
        if (playerIn.level().isClientSide()) {
            return null;
        } else {
            ItemStack blade = playerIn.getMainHandItem();
            if (!blade.getCapability(ItemSlashBlade.BLADESTATE).isPresent()) {
                return null;
            } else {
                SlashBladeEvent.DoSlashEvent event = new SlashBladeEvent.DoSlashEvent(blade,
                        (ISlashBladeState) blade.getCapability(ItemSlashBlade.BLADESTATE)
                                .orElseThrow(NullPointerException::new),
                        playerIn, roll, critical, comboRatio, knockback);
                if (MinecraftForge.EVENT_BUS.post(event)) {
                    return null;
                } else {
                    Vec3 pos = playerIn.position().add(0.0, (double) playerIn.getEyeHeight() * 0.75, 0.0)
                            .add(playerIn.getLookAngle().scale(0.30000001192092896));
                    pos = pos
                            .add(VectorHelper.getVectorForRotation(-90.0F, playerIn.getViewYRot(0.0F))
                                    .scale(centerOffset.y))
                            .add(VectorHelper.getVectorForRotation(0.0F, playerIn.getViewYRot(0.0F) + 90.0F)
                                    .scale(centerOffset.z))
                            .add(playerIn.getLookAngle().scale(centerOffset.z));
                    EntitySlashEffect jc = new EntitySlashEffect(SlashBlade.RegistryEvents.SlashEffect,
                            playerIn.level());
                    jc.setPos(pos.x, pos.y, pos.z);
                    jc.setOwner(event.getUser());
                    jc.setRotationRoll(event.getRoll());
                    jc.setYRot(YRot);
                    jc.setXRot(XRot);
                    jc.setColor(colorCode);
                    jc.setMute(mute);
                    jc.setIsCritical(event.isCritical());
                    jc.setDamage(event.getDamage());
                    jc.setKnockBack(event.getKnockback());
                    playerIn.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent((rank) -> {
                        jc.setRank(rank.getRankLevel(playerIn.level().getGameTime()));
                    });
                    playerIn.level().addFreshEntity(jc);
                    return jc;
                }
            }
        }
    }
}
