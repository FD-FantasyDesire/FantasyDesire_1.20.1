package tennouboshiuzume.mods.FantasyDesire.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class VelocityUtils {

    /**
     * 获取两个实体之间的相对速度大小
     */
    public static double getRelativeSpeed(Entity e1, Entity e2) {
        Vec3 v1 = e1.getDeltaMovement();
        Vec3 v2 = e2.getDeltaMovement();
        return v1.subtract(v2).length();
    }

    /**
     * 获取实体自身的移动速度大小
     */
    public static double getSpeed(Entity entity) {
        return entity.getDeltaMovement().length();
    }
}
