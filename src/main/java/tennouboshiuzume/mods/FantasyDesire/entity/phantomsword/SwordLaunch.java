package tennouboshiuzume.mods.FantasyDesire.entity.phantomsword;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** 自动发射的确定性散布；UUID 随出生包同步并随实体存档，两端无需等待发射状态包。 */
public final class SwordLaunch {
    public static Vec3 velocity(UUID swordId, Vec3 direction, float speed, float inaccuracy) {
        Vec3 normalized = direction.normalize();
        if (inaccuracy == 0f)
            return normalized.scale(speed);

        // 独立随机源避免待命期间的粒子、音效等随机调用影响两端结果。
        RandomSource random = RandomSource.create(swordId.getMostSignificantBits()
                ^ Long.rotateLeft(swordId.getLeastSignificantBits(), 32));
        // 保持 SlashBlade shoot() 的高斯分布、0.0075F 单位和运算顺序，散布后不再次归一化。
        return normalized.add(random.nextGaussian() * (double) 0.0075F * (double) inaccuracy,
                random.nextGaussian() * (double) 0.0075F * (double) inaccuracy,
                random.nextGaussian() * (double) 0.0075F * (double) inaccuracy).scale(speed);
    }

    private SwordLaunch() {}
}
