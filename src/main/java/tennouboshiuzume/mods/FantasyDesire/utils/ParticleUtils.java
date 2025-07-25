package tennouboshiuzume.mods.FantasyDesire.utils;

import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

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


}