package tennouboshiuzume.mods.FantasyDesire.utils;

import net.minecraft.util.RandomSource;

public class MathUtils {
    private static final RandomSource random = RandomSource.create();

    public static boolean RandomCheck(float chance) {
        // 限制范围 [0, 100]
        if (chance <= 0)
            return false;
        if (chance >= 100)
            return true;
        // 生成 [0.0, 100.0) 的随机浮点数
        float roll = random.nextFloat() * 100.0f;
        return roll < chance;
    }

    /**
     * 将值限制在 [min, max] 范围内
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 将值限制在 [min, max] 范围内
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 将值限制在 [min, max] 范围内
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
