package tennouboshiuzume.mods.FantasyDesire.particle;

import net.minecraft.world.phys.Vec3;

import java.util.Random;

/** 仅在出生时生成确定的三维折线；节点相对起点保存，不受相机和动画时间影响。 */
public final class AstraLightningPath {
    private AstraLightningPath() {
    }

    public static Vec3[] create(AstraLightningParticleOptions options) {
        Vec3 axis = options.end.subtract(options.start);
        double length = axis.length();
        if (length < 1.0E-6) {
            return new Vec3[] { Vec3.ZERO };
        }
        double spacing = Math.max(0.75, options.starRadius * 3.0);
        int count = Math.min(options.maxEndpoints, Math.max(2, (int) Math.ceil(length / spacing) + 1));
        Vec3[] points = new Vec3[count];
        points[0] = Vec3.ZERO;
        points[count - 1] = axis;
        Vec3 forward = axis.scale(1.0 / length);
        Vec3 reference = Math.abs(forward.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 side = forward.cross(reference).normalize();
        Vec3 up = forward.cross(side).normalize();
        Random random = new Random(options.seed);
        double amplitude = Math.min(options.randomness, length / (count - 1) * 1.3);
        for (int i = 1; i < count - 1; i++) {
            double t = (i + (random.nextDouble() - 0.5) * 0.36) / (count - 1);
            double envelope = Math.sin(Math.PI * t);
            double radius = 0.45 + random.nextDouble() * 0.55;
            // 各折点独立采样轴线法平面内的方向，避免奇偶交替把第一折锁在同一侧。
            double angle = random.nextDouble() * Math.PI * 2.0;
            double lateral = Math.cos(angle) * radius;
            double depth = Math.sin(angle) * radius;
            points[i] = axis.scale(t).add(side.scale(lateral * amplitude * envelope))
                    .add(up.scale(depth * amplitude * envelope));
        }
        return points;
    }
}
