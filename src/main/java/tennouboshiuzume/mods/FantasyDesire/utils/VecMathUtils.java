package tennouboshiuzume.mods.FantasyDesire.utils;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class VecMathUtils {

    /**
     * 绕任意轴旋转一个向量
     * @param vec 要旋转的向量
     * @param axis 旋转轴向量（需要是单位向量）
     * @param degrees 旋转角度（单位：度）
     * @return 旋转后的向量
     */
    public static Vec3 rotateAroundAxis(Vec3 vec, Vec3 axis, float degrees) {
        axis = axis.normalize();

        double radians = Math.toRadians(degrees);

        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double dot = vec.dot(axis);
        Vec3 cross = axis.cross(vec);

        return new Vec3(
                vec.x * cos + cross.x * sin + axis.x * dot * (1 - cos),
                vec.y * cos + cross.y * sin + axis.y * dot * (1 - cos),
                vec.z * cos + cross.z * sin + axis.z * dot * (1 - cos)
        );
    }
    public static Vec3 rotateAroundAxis(Vec3 vec, Vec3 axis, double angleRad,boolean isRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double dot = vec.dot(axis);
        return axis.scale(dot * (1.0 - cos))
                .add(vec.scale(cos))
                .add(axis.cross(vec).scale(sin));
    }
    /**
     * 从方向向量推算出Yaw和Pitch
     * @param vec 单位方向向量
     * @return 一个 float[2]，下标0是Yaw，下标1是Pitch
     */
    public static float[] getYawPitchFromVec(Vec3 vec) {
        double x = vec.x;
        double y = vec.y;
        double z = vec.z;
        float yaw = (float) (Math.atan2(-x, z) * (180F / Math.PI));
        float pitch = (float) (Math.asin(-y) * (180F / Math.PI));

        return new float[]{yaw, pitch};
    }

    public static Vec3 calculateDirectionVec(LivingEntity player, Entity target) {
        // 获取玩家和目标实体的位置差值
        double deltaX = target.getX() - player.getX();
        double deltaY = target.getY() - player.getY();
        double deltaZ = target.getZ() - player.getZ();

        // 计算水平距离
        double distanceXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // 计算 Pitch（水平方向的俯仰角度）和 Yaw（水平方向的偏航角度）
        double pitch = Math.toDegrees(Math.atan2(deltaY, distanceXZ)); // 水平距离与Y轴差值的反正切，得到俯仰角
        double yaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90; // 水平X与Z轴的反正切，得到偏航角

        // 保证 yaw 在 -180 到 180 之间
        if (yaw < -180) {
            yaw += 360;
        } else if (yaw > 180) {
            yaw -= 360;
        }

        // 将 pitch 和 yaw 转换为单位向量（方向向量）
        double pitchRadians = Math.toRadians(pitch);
        double yawRadians = Math.toRadians(yaw);

        // 计算方向向量的分量
        double x = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double y = -Math.sin(pitchRadians);
        double z = Math.cos(yawRadians) * Math.cos(pitchRadians);

        // 返回单位向量（方向向量）
        return new Vec3(x, y, z);
    }

    public static Vec3 getReversedDirection(float yaw, float pitch) {
        float reversedYaw = -yaw;
        float reversedPitch = -pitch;
        // 根据反转后的yaw、pitch计算方向向量
        double radYaw = Math.toRadians(reversedYaw);
        double radPitch = Math.toRadians(reversedPitch);

        double x = -Math.cos(radPitch) * Math.sin(radYaw);
        double y = -Math.sin(radPitch);
        double z = Math.cos(radPitch) * Math.cos(radYaw);

        return new Vec3(x, y, z);
    }

    public static Vec3 rotateTowards(Vec3 from, Vec3 to, float maxRadians) {
        Vec3 fromNorm = from.normalize();
        Vec3 toNorm = to.normalize();

        double dot = fromNorm.dot(toNorm);
        dot = Mth.clamp(dot, -1.0, 1.0);
        double angle = Math.acos(dot);

        if (angle < maxRadians) return toNorm; // 直接对齐
        double t = maxRadians / angle;

        // 球面插值 (slerp)
        return fromNorm.scale(1 - t).add(toNorm.scale(t)).normalize();
    }
}
