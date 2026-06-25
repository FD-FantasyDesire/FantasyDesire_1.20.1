package tennouboshiuzume.mods.FantasyDesire.slasharts;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDHuntSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;

import java.util.List;

public class CrimsonStrike {
    public static boolean AntiNTR(LivingEntity entity) {
        return CapabilityUtils.SEConditionMatcher.of(entity)
                .requireTranslation("item.fantasydesire.crimson_scythe")
                .match() != null;
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
            sword.setStandbyMode(tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword.StandbyMode.PLAYER);
            sword.setMovingMode(tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword.MovingMode.SEEK);
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

    public static void Stage2(LivingEntity player) {
        if (player.level().isClientSide())
            return;
        // 乱舞斩击
        AddonSlashUtils.doAddonFDSlash(player, -90 + player.getRandom().nextFloat() * 180, player.getYRot(), 0,
                0xFF0000, 0, Vec3.ZERO, false, false, 2.0f, KnockBacks.cancel, 2f, 10);
        player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
    }

    public static void Stage3(LivingEntity player) {
        if (player.level().isClientSide())
            return;
        ItemStack blade = player.getMainHandItem();
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        if (state == null)
            return;

        // 终结爆发
        AddonSlashUtils.doAddonFDSlash(player, -90, player.getYRot(), 0, 0x8B0000, 0, Vec3.ZERO, true, true, 10.0f,
                KnockBacks.toss, 2f, 20);
        player.playSound(SoundEvents.GENERIC_EXPLODE, 1.0f, 1.0f);

        List<LivingEntity> targets = FDTargetSelector.getLivingEntitiesInRadius(player, player.position(), 15.0, false,
                null);
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().mobAttack(player), state.getDamage() * 5);
        }
    }
}
