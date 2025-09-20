package tennouboshiuzume.mods.FantasyDesire.utils;

import mods.flammpfeil.slashblade.entity.IShootable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.RayTraceHelper;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TargetUtils {
    public static List<LivingEntity> getNearbyLivingEntities(Entity center, double radius, List<LivingEntity> excludeEntitys) {
        if (center == null || center.level().isClientSide) return List.of();

        Level world = center.level();
        AABB boundingBox = center.getBoundingBox().inflate(radius);

        double radiusSquared = radius * radius;

        TargetSelector.AttackablePredicate predicate = new TargetSelector.AttackablePredicate();

        return world.getEntitiesOfClass(LivingEntity.class, boundingBox)
                .stream()
                .filter(entity -> !excludeEntitys.contains(entity))
                .filter(entity -> entity.distanceToSqr(center) <= radiusSquared) // 欧几里得距离过滤
                .filter(predicate::test) //适用拔刀剑配置可攻击对象
                .collect(Collectors.toList());
    }

    /**
     * 尝试获取玩家的锁定目标
     * @param sender 发射者（通常是玩家）
     * @return 可选的目标实体
     */

    public static Optional<Entity> getLockTarget(LivingEntity sender) {
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
                        35, 35,
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
}
