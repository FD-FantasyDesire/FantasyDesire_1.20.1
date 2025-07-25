package tennouboshiuzume.mods.FantasyDesire.utils;

import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
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
}
