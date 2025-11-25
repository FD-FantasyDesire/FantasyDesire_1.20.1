package tennouboshiuzume.mods.FantasyDesire.utils;

import mods.flammpfeil.slashblade.entity.IShootable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.RayTraceHelper;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TargetUtils extends TargetSelector {
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

    public static List<LivingEntity> getLivingEntitiesInRadius(Level world, Vec3 pos, double range,
                                                               @Nullable List<Entity> exclude) {
        AABB aabb = new AABB(
                pos.x - range, pos.y - range, pos.z - range,
                pos.x + range, pos.y + range, pos.z + range
        );
        TargetSelector.AttackablePredicate predicate = new TargetSelector.AttackablePredicate();
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, aabb).stream()
                // 距离判定
                .filter(e -> e.distanceToSqr(pos) <= range * range)
                // isAttackable 标签过滤
                .filter(Entity::isAttackable)
                // 排除指定实体
                .filter(e -> exclude == null || !exclude.contains(e))
                .filter(predicate::test) //适用拔刀剑配置可攻击对象
                .collect(Collectors.toList());

        return entities;
    }


    public static List<LivingEntity> getTargetsInSight(Player player, double maxDistance, double radius, boolean requireVisible,@Nullable List<Entity> exclude) {
        Level level = player.level();

        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);

        Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));
        ClipContext context = new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult result = level.clip(context);

        // 使用 Vec3 位置，不必转 BlockPos
        Vec3 targetPos = result.getType() == HitResult.Type.BLOCK ? result.getLocation() : endPos;

        // 构造 AABB 进行范围搜索
        AABB searchBox = new AABB(targetPos.x - radius, targetPos.y - radius, targetPos.z - radius,
                targetPos.x + radius, targetPos.y + radius, targetPos.z + radius);
        TargetSelector.AttackablePredicate predicate = new TargetSelector.AttackablePredicate();
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player).stream()
                .filter(e -> e.distanceToSqr(targetPos) <= radius * radius)
                // isAttackable 标签过滤
                .filter(Entity::isAttackable)
                // 排除指定实体
                .filter(e -> exclude == null || !exclude.contains(e))
                .filter(predicate::test) //适用拔刀剑配置可攻击对象
                .filter(e-> !requireVisible || canSee(player, e))
                .collect(Collectors.toList());
        return entities;
    }
    @Nullable
    public static LivingEntity getNearestTargetInSight(Player player, double maxDistance, double radius, boolean requireVisible, @Nullable List<Entity> exclude) {
        Level level = player.level();

        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);

        // 计算视线终点
        Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));
        ClipContext context = new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
        BlockHitResult result = level.clip(context);

        // 落点（若视线没打到方块，则用最远点）
        Vec3 targetPos = result.getType() == HitResult.Type.BLOCK ? result.getLocation() : endPos;

        // 构造搜索范围
        AABB searchBox = new AABB(
                targetPos.x - radius, targetPos.y - radius, targetPos.z - radius,
                targetPos.x + radius, targetPos.y + radius, targetPos.z + radius
        );

        TargetSelector.AttackablePredicate predicate = new TargetSelector.AttackablePredicate();

        // 搜索并筛选符合条件的实体
        return level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player).stream()
                .filter(Entity::isAttackable)
                .filter(e -> exclude == null || !exclude.contains(e))
                .filter(predicate::test) // 拔刀剑可攻击判定
                .filter(e -> e.distanceToSqr(targetPos) <= radius * radius)
                .filter(e -> !requireVisible || canSee(player, e))
                // ✅ 选取距离“视线落点”最近的实体
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(targetPos)))
                .orElse(null);
    }
    // 视线检测判定，参考自Warframe
    public static boolean canSee(Entity looker, Entity entity) {
        Level level = looker.level();
        Vec3 eyePos = looker.getEyePosition(0f);
        AABB box = entity.getBoundingBox();
        // 碰撞箱底、中、顶三个点
        Vec3 bottom = entity.position().add(0,0.2,0);
        Vec3 middle = entity.position().add(0,entity.getBbHeight()/2,0);
        Vec3 top = entity.position().add(0,entity.getBbHeight(),0);
        Vec3[] points = { bottom, middle, top };

        for (Vec3 point : points) {
            ClipContext context = new ClipContext(eyePos, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, looker);
            BlockHitResult result = level.clip(context);
            if (result.getType() == HitResult.Type.MISS) {
                return true;
            }
        }

        return false;
    }


}