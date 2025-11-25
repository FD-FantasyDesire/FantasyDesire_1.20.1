package tennouboshiuzume.mods.FantasyDesire.utils;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.client.particle.GlowingLineParticleOptions;

import java.util.Random;

public class ParticleUtils {

    // 生成环形粒子
    public static void generateRingParticles(ParticleOptions particleType, Level level, double x, double y, double z, double radius, int numParticles) {
        if (!level.isClientSide()) {
            int randomAngle = new Random().nextInt(360 / numParticles);
            for (int i = 0; i < numParticles; i++) {
                double angle = 2 * Math.PI * i / numParticles + randomAngle;
                double offsetX = radius * Math.cos(angle);
                double offsetZ = radius * Math.sin(angle);
                ((ServerLevel) level).sendParticles(
                        particleType,    // 粒子类型
                        x,                        // 粒子位置 X
                        y,                        // 粒子位置 Y
                        z,                        // 粒子位置 Z
                        0,                        // 粒子数量
                        offsetX, 0, offsetZ, // 偏移量
                        0.1                       // 速度
                );
            }
        }
    }

    public static void LightBoltParticles(Level level, Vec3 start, Vec3 end, int color, float thickness, int lifetime, float alpha, boolean fade, double randomness, int maxSegments) {
        if (level instanceof ServerLevel serverLevel) {
            spawnSegment(serverLevel, start, end, color, thickness, lifetime, alpha, fade, randomness, maxSegments);
        }
    }

    private static void spawnSegment(ServerLevel serverLevel, Vec3 start, Vec3 end,
                                     int color, float thickness, int lifetime, float alpha,
                                     boolean fade, double randomness, int remainingPoints) {
        // 递归终止条件：只剩一个端点
        if (remainingPoints <= 1) {
            Vec3 midPoint = start.add(end).scale(0.5);
            GlowingLineParticleOptions opts = new GlowingLineParticleOptions(start, end, color, thickness, alpha, fade, lifetime);
            serverLevel.sendParticles(opts, midPoint.x, midPoint.y, midPoint.z, 1, 0, 0, 0, 0);
            return;
        }

        // 计算中点并加入随机偏移
        Vec3 mid = start.add(end).scale(0.5);
        double distance = start.distanceTo(end);
        double maxOffset = Math.min(randomness, distance * 0.5);
        Vec3 midWithRandom = mid.add(new Vec3(
                (Math.random() - 0.5) * maxOffset,
                (Math.random() - 0.5) * maxOffset,
                (Math.random() - 0.5) * maxOffset
        ));

        // 将剩余端点数分配给左右两条子线段
        int leftPoints = remainingPoints / 2;
        int rightPoints = remainingPoints - leftPoints;

        spawnSegment(serverLevel, start, midWithRandom, color, thickness, lifetime, alpha, fade, randomness, leftPoints);
        spawnSegment(serverLevel, midWithRandom, end, color, thickness, lifetime, alpha, fade, randomness, rightPoints);
    }
}