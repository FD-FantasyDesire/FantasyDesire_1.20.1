package tennouboshiuzume.mods.FantasyDesire.utils;

import mods.flammpfeil.slashblade.entity.IShootable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.RayTraceHelper;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TargetUtils extends TargetSelector {
    public static Optional<Entity> getLockTarget(LivingEntity sender) {
        Level worldIn = sender.level();
        Entity lockTarget = sender.getMainHandItem()
                .getCapability(ItemSlashBlade.BLADESTATE)
                .filter(state -> state.getTargetEntity(worldIn) != null)
                .map(state -> state.getTargetEntity(worldIn))
                .orElse(null);
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
                (e) -> true)
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

    public static List<LivingEntity> getNearbyLivingEntities(Entity entity, double radius, boolean requireVisible,
            @Nullable List<Entity> exclude) {
        if (entity == null || entity.level().isClientSide)
            return List.of();

        Level level = entity.level();
        Vec3 eyePos = entity.getEyePosition(1.0f);
        TargetSelector.AttackablePredicate predicate = new TargetSelector.AttackablePredicate();

        AABB boundingBox = new AABB(
                eyePos.x - radius, eyePos.y - radius, eyePos.z - radius,
                eyePos.x + radius, eyePos.y + radius, eyePos.z + radius);
        double radiusSq = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, boundingBox)
                .stream()
                .filter(e -> e != entity)
                .filter(e -> exclude == null || !exclude.contains(e))
                .filter(Entity::isAttackable)
                .filter(predicate::test)
                .filter(e -> e.distanceToSqr(eyePos) <= radiusSq)
                .filter(e -> !requireVisible || canSee(entity, e))
                .collect(Collectors.toList());
    }

    public static List<LivingEntity> getLivingEntitiesInRadius(Entity entity, Vec3 pos, double range,
            boolean requireVisible, @Nullable List<Entity> exclude) {

        Level level = entity.level();
        TargetSelector.AttackablePredicate predicate = new TargetSelector.AttackablePredicate();

        AABB aabb = new AABB(
                pos.x - range, pos.y - range, pos.z - range,
                pos.x + range, pos.y + range, pos.z + range);

        double rangeSq = range * range;

        return level.getEntitiesOfClass(LivingEntity.class, aabb).stream()
                .filter(e -> e != entity)
                .filter(e -> exclude == null || !exclude.contains(e))
                .filter(Entity::isAttackable)
                .filter(predicate::test) // 拔刀剑配置判定
                .filter(e -> e.distanceToSqr(pos) <= rangeSq)
                .filter(e -> !requireVisible || canSee(entity, e))
                .collect(Collectors.toList());
    }

    public static List<LivingEntity> getTargetsInSight(Entity entity, double maxDistance, double maxAngleDeg,
            boolean requireVisible, @Nullable List<Entity> exclude) {
        Level level = entity.level();
        Vec3 eyePos = entity.getEyePosition(1.0f);
        TargetSelector.AttackablePredicate predicate = new TargetSelector.AttackablePredicate();

        AABB searchBox = new AABB(
                eyePos.x - maxDistance, eyePos.y - maxDistance, eyePos.z - maxDistance,
                eyePos.x + maxDistance, eyePos.y + maxDistance, eyePos.z + maxDistance);

        return level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != entity).stream()
                .filter(e -> exclude == null || !exclude.contains(e))
                .filter(Entity::isAttackable)
                .filter(predicate::test)
                .filter(e -> inVisionCone(entity, e, maxDistance, maxAngleDeg))
                .filter(e -> !requireVisible || canSee(entity, e))
                .collect(Collectors.toList());
    }

    @Nullable
    public static LivingEntity getNearestTargetInSight(Entity entity, double maxDistance, double maxAngleDeg,
            boolean requireVisible, @Nullable List<Entity> exclude) {

        Level level = entity.level();
        Vec3 eyePos = entity.getEyePosition(1.0f);
        TargetSelector.AttackablePredicate predicate = new TargetSelector.AttackablePredicate();

        AABB searchBox = new AABB(
                eyePos.x - maxDistance, eyePos.y - maxDistance, eyePos.z - maxDistance,
                eyePos.x + maxDistance, eyePos.y + maxDistance, eyePos.z + maxDistance);

        return level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != entity).stream()
                .filter(e -> exclude == null || !exclude.contains(e))
                .filter(Entity::isAttackable)
                .filter(predicate::test)
                .filter(e -> inVisionCone(entity, e, maxDistance, maxAngleDeg))
                .filter(e -> !requireVisible || canSee(entity, e))
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(eyePos)))
                .orElse(null);
    }

    private static boolean inVisionCone(Entity observer, LivingEntity target, double maxDistance,
            double halfAngleDeg) {
        if (observer == null || target == null || !target.isAlive())
            return false;

        Vec3 eyePos = observer.getEyePosition(1.0f);

        // 获取观察者朝向向量，若 getViewVector 返回零则使用 yaw/pitch 计算
        Vec3 lookVec = observer.getViewVector(1.0f);
        if (lookVec == null || lookVec.lengthSqr() < 1e-6) {
            double yawRad = Math.toRadians(observer.getYRot());
            double pitchRad = Math.toRadians(observer.getXRot());
            lookVec = new Vec3(-Math.sin(yawRad) * Math.cos(pitchRad),
                    -Math.sin(pitchRad),
                    Math.cos(yawRad) * Math.cos(pitchRad));
        }
        lookVec = lookVec.normalize();

        // 获取目标位置（优先眼位）
        Vec3 targetPos = target.getEyePosition(1.0f);
        if (targetPos == null) {
            AABB bb = target.getBoundingBox();
            targetPos = new Vec3((bb.minX + bb.maxX) * 0.5,
                    (bb.minY + bb.maxY) * 0.5,
                    (bb.minZ + bb.maxZ) * 0.5);
        }

        Vec3 toTarget = targetPos.subtract(eyePos);
        double distSq = toTarget.lengthSqr();
        if (distSq > maxDistance * maxDistance)
            return false;

        // 避免极近目标归一化零向量
        if (distSq < 1e-8)
            return true;

        Vec3 dirToTarget = toTarget.normalize();
        double dot = lookVec.dot(dirToTarget); // cos(theta)
        double cosLimit = Math.cos(Math.toRadians(halfAngleDeg));

        if (dot < cosLimit)
            return false;

        return true;
    }

    // 视线检测判定，参考自Warframe
    public static boolean canSee(Entity looker, Entity entity) {
        Level level = looker.level();
        Vec3 eyePos = looker.getEyePosition(0f);
        AABB box = entity.getBoundingBox();
        // 碰撞箱底、中、顶三个点
        Vec3 bottom = entity.position().add(0, 0.2, 0);
        Vec3 middle = entity.position().add(0, entity.getBbHeight() / 2, 0);
        Vec3 top = entity.position().add(0, entity.getBbHeight(), 0);
        Vec3[] points = { bottom, middle, top };

        for (Vec3 point : points) {
            ClipContext context = new ClipContext(eyePos, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    looker);
            BlockHitResult result = level.clip(context);
            if (result.getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

}