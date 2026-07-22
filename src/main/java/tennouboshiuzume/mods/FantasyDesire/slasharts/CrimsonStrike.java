package tennouboshiuzume.mods.FantasyDesire.slasharts;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageSource;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDHuntSword;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;

import java.util.List;
import java.util.Random;

public class CrimsonStrike {
    public static boolean AntiNTR(LivingEntity entity) {
        return CapabilityUtils.SEConditionMatcher.of(entity)
                .requireTranslation("item.fantasydesire.crimson_scythe")
                .match() != null;
    }

    public static void HitEffect(Entity entity, float radiusMult) {
        Random rd = new Random();
        Boolean tColor = Math.random() < 0.5;
        float halfLength = (entity.getBbHeight() + entity.getBbWidth()) / 3 * radiusMult * (tColor ? 0.5f : 1f);
        Vec3 base = new Vec3(0, 0, halfLength)
                .yRot((float) Math.toRadians(rd.nextInt(360))).xRot((float) Math.toRadians(rd.nextInt(360)));
        Vec3 start = entity.position().add(base).add(0, entity.getBbHeight() / 2, 0);
        Vec3 end = entity.position().add(base.scale(-1)).add(0, entity.getBbHeight() / 2, 0);
        ParticleUtils.spwanBladeRiftParticles(entity.level(), start, end, 20, 1.5f, 0.03f,
                tColor ? 0xFFFFFF : 0x000000,
                0xFF0000);
    }

    public static void ShootHunterSword(LivingEntity player) {
        if (player.level().isClientSide())
            return;
        ItemStack blade = player.getMainHandItem();
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        if (state == null)
            return;
        // 发射 EntityFDHuntSword 聚怪
        int count = 24;
        double radius = 1.5;
        List<LivingEntity> targets = FDTargetSelector.getLivingEntitiesInRadius(player, player.position(), 40.0, true,
                null);
        double phi = Math.PI * (3.0 - Math.sqrt(5.0)); // 黄金角
        for (int i = 0; i < count; i++) {
            EntityFDHuntSword sword = new EntityFDHuntSword(FDEntitys.FDHuntSword.get(), player.level());
            sword.setOwner(player);
            sword.setDamage(state.getDamage());
            sword.setColor(state.getColorCode());
            // 斐波那契球面算法计算相对坐标
            double y = 1 - (i / (double) (count - 1)) * 2; // y 取值从 1 到 -1
            double radiusAtY = Math.sqrt(1 - y * y); // 半径在此高度的大小
            double theta = phi * i; // 黄金角乘以索引
            double x = Math.cos(theta) * radiusAtY;
            double z = Math.sin(theta) * radiusAtY;
            // 计算出的坐标方向向量，作为剑的方向
            Vec3 dir = new Vec3(x, y, z).normalize();
            // 设置剑的绝对坐标，以玩家为中心加上偏移
            Vec3 spawnPos = player.position().add(0, player.getBbHeight() / 2.0, 0).add(dir.scale(radius));
            sword.setOffset(dir.scale(radius));
            sword.setCenterOffset(new Vec3(0, player.getBbHeight() / 2, 0));
            sword.setPos(spawnPos.x(), spawnPos.y(), spawnPos.z());
            // 设置剑的朝向（向外扩散）
            float yaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90;
            float pitch = (float) Math.toDegrees(Math.asin(-dir.y));
            sword.setStandbyYawPitch(yaw, pitch);
            // 设置待机和移动模式
            sword.setStandbyMode(EntityFDPhantomSword.StandbyMode.PLAYER);
            sword.setMovingMode(EntityFDPhantomSword.MovingMode.SEEK);
            sword.setDelay(100);
            sword.setDelayTicks(0);
            sword.setSeekDelay(10);
            sword.setSeekAngle(36);
            sword.setNoClip(true);
            sword.setHasTail(true);
            sword.setScale(0.5f);
            // 均匀分配目标
            if (!targets.isEmpty()) {
                LivingEntity target = targets.get(i % targets.size());
                sword.setTargetId(target.getId());
            }

            player.level().addFreshEntity(sword);
        }
        player.playSound(SoundEvents.TRIDENT_THROW, 1.0f, 1.0f);
    }

    public static void doTripleAddonFDSlash(LivingEntity playerIn, float roll, float YRot, float XRot,
            int colorCode, float rotationOffset, Vec3 centerOffset, boolean mute, boolean critical, double damage,
            KnockBacks knockback, int lifetime) {
        if (playerIn.level().isClientSide()) {
            return;
        }

        AddonSlashUtils.doAddonFDSlash(playerIn, roll, YRot, XRot, colorCode, rotationOffset, centerOffset, mute,
                critical, damage, knockback, 3.0f, lifetime, FDDamageSource.ABSORB.location().toString());

        float distance = 1.5f;
        float rad = (float) Math.toRadians(-roll + 90.0f);
        float dy = (float) Math.sin(rad) * distance;
        float dz = (float) Math.cos(rad) * distance;

        Vec3 offset1 = centerOffset.add(0, dy, dz);
        Vec3 offset2 = centerOffset.add(0, -dy, -dz);

        AddonSlashUtils.doAddonFDSlash(playerIn, roll, YRot, XRot, colorCode, rotationOffset, offset1, mute, critical,
                damage, knockback, 3.0f, lifetime, FDDamageSource.ABSORB.location().toString());
        AddonSlashUtils.doAddonFDSlash(playerIn, roll, YRot, XRot, colorCode, rotationOffset, offset2, mute, critical,
                damage, knockback, 3.0f, lifetime, FDDamageSource.ABSORB.location().toString());
    }

}
