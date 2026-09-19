package tennouboshiuzume.mods.FantasyDesire.utils;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.particle.BladeRiftParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.particle.GlowingLineParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.particle.AstraLightningParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.particle.AstraStarParticleOptions;

import java.util.Random;

public class ParticleUtils {
    /** 生成一颗有精确初速度的星芒，速度单位为格/tick；调用方选择服务端或客户端其中一侧。 */
    public static void spawnAstraStar(Level level, Vec3 position, Vec3 velocity, AstraStarParticleOptions options,
            float speed) {
        if (position == null || velocity == null
                || !Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)
                || Math.abs(position.x) > 3.0E7 || Math.abs(position.y) > 3.0E7 || Math.abs(position.z) > 3.0E7
                || !Float.isFinite((float) velocity.x) || !Float.isFinite((float) velocity.y)
                || !Float.isFinite((float) velocity.z)) {
            throw new IllegalArgumentException("星芒位置必须在世界范围内，速度必须可表示为有限浮点数");
        }
        if (level instanceof ServerLevel serverLevel) {
            // 原版 count=0 表示生成一颗粒子，delta * speed 是速度，不能改成 count=1 的随机散布。
            sendForceParticles(serverLevel, options, position.x, position.y, position.z, 0,
                    velocity.x, velocity.y, velocity.z, speed, 64.0);
        }
    }

    // 强制向范围内玩家发送结构化粒子数据包，避免客户端限流破坏分段特效。
    public static <T extends ParticleOptions> int sendForceParticles(ServerLevel level, T particle, double x, double y,
            double z, int count, double deltaX, double deltaY, double deltaZ, double speed, double radius) {
        net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket packet = new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket(
                particle, true, x, y, z, (float) deltaX, (float) deltaY, (float) deltaZ, (float) speed, count);
        int j = 0;
        for (net.minecraft.server.level.ServerPlayer serverplayer : level.players()) {
            if (serverplayer.distanceToSqr(x, y, z) <= radius * radius) {
                serverplayer.connection.send(packet);
                ++j;
            }
        }
        return j;
    }

    // 生成环形运动的粒子 （我想已经用不上了）
    public static void generateRingParticles(ParticleOptions particleType, Level level, double x, double y, double z,
            double radius, int numParticles) {
        if (!level.isClientSide() && numParticles > 0) {
            double randomAngle = new Random().nextDouble() * Mth.TWO_PI;
            for (int i = 0; i < numParticles; i++) {
                double angle = 2 * Mth.PI * i / numParticles + randomAngle;
                double offsetX = radius * Math.cos(angle);
                double offsetZ = radius * Math.sin(angle);
                ((ServerLevel) level).sendParticles(
                        particleType,
                        x,
                        y,
                        z,
                        0,
                        offsetX, 0, offsetZ,
                        0.1);
            }
        }
    }

    public static void LightBoltParticles(Level level, Vec3 start, Vec3 end, int color, float thickness, int lifetime,
            float alpha, boolean fade, double randomness, int maxSegments) {
        if (level instanceof ServerLevel serverLevel) {
            spawnSegment(serverLevel, start, end, color, thickness, lifetime, alpha, fade, randomness,
                    Mth.clamp(maxSegments, 1, 32));
        }
    }

    private static void spawnSegment(ServerLevel serverLevel, Vec3 start, Vec3 end,
            int color, float thickness, int lifetime, float alpha,
            boolean fade, double randomness, int remainingPoints) {
        // 递归终止条件：只剩一个端点
        if (remainingPoints <= 1) {
            Vec3 midPoint = start.add(end).scale(0.5);
            GlowingLineParticleOptions opts = new GlowingLineParticleOptions(start, end, color, thickness, alpha, fade,
                    lifetime);
            sendForceParticles(serverLevel, opts, midPoint.x, midPoint.y, midPoint.z, 1, 0, 0, 0, 0, 64.0);
            return;
        }
        // 计算中点并加入随机偏移
        Vec3 mid = start.add(end).scale(0.5);
        double distance = start.distanceTo(end);
        double maxOffset = Math.min(randomness, distance * 0.5);
        Vec3 midWithRandom = mid.add(new Vec3(
                (Math.random() - 0.5) * maxOffset,
                (Math.random() - 0.5) * maxOffset,
                (Math.random() - 0.5) * maxOffset));
        // 将剩余端点数分配给左右两条子线段
        int leftPoints = remainingPoints / 2;
        int rightPoints = remainingPoints - leftPoints;
        spawnSegment(serverLevel, start, midWithRandom, color, thickness, lifetime, alpha, fade, randomness,
                leftPoints);
        spawnSegment(serverLevel, midWithRandom, end, color, thickness, lifetime, alpha, fade, randomness, rightPoints);
    }

    /** 兼容旧调用：旧线段数换成含首尾的节点数，旧圆环半径改为星芒尺寸。 */
    public static void AstraLightningParticles(Level level, Vec3 start, Vec3 end, int color, float thickness,
            int lifetime, float alpha, boolean fade, double randomness, int maxSegments, float ringRadius) {
        AstraLightningParticles(level, start, end, color, thickness, lifetime, alpha, fade, randomness,
                Mth.clamp(maxSegments, 1, 32) + 1, Math.abs(ringRadius), level.random.nextLong());
    }

    /** 服务端出生；显式指定种子时，每个客户端生成完全相同的整条折线。 */
    public static void AstraLightningParticles(Level level, Vec3 start, Vec3 end, int color, float thickness,
            int lifetime, float alpha, boolean fade, double randomness, int maxEndpoints, float starRadius, long seed) {
        if (level instanceof ServerLevel) {
            spawnAstraLightning(level, new AstraLightningParticleOptions(start, end, color, thickness, lifetime,
                    alpha, fade, randomness, maxEndpoints, starRadius, seed));
        }
    }

    /** 在起点生成一个粒子；服务端只发送一份参数，客户端也可直接调用。 */
    public static void spawnAstraLightning(Level level, AstraLightningParticleOptions options) {
        Vec3 start = options.start;
        if (level instanceof ServerLevel serverLevel) {
            // 以起点发送，额外覆盖整条闪电的距离，避免原来的中点发送约定遗漏远端观察者。
            double radius = 64.0 + start.distanceTo(options.end);
            sendForceParticles(serverLevel, options, start.x, start.y, start.z, 1, 0, 0, 0, 0, radius);
        } else if (level.isClientSide()) {
            level.addParticle(options, true, start.x, start.y, start.z, 0, 0, 0);
        }
    }

    // 剑痕粒子
    public static void spwanBladeRiftParticles(Level level, Vec3 start, Vec3 end, int lifetime, float length,
            float width,
            int innerColor, int outerColor) {
        if (level instanceof ServerLevel serverLevel) {
            Vec3 midPoint = start.add(end).scale(0.5);
            BladeRiftParticleOptions opts = new BladeRiftParticleOptions(start, end, lifetime, length, width,
                    innerColor, outerColor);
            sendForceParticles(serverLevel, opts, midPoint.x, midPoint.y, midPoint.z, 1, 0, 0, 0, 0, 64.0);
        }
    }
}
