package tennouboshiuzume.mods.FantasyDesire.utils;

import net.minecraft.util.RandomSource;

public class MathUtils {
    private static final RandomSource random = RandomSource.create();
    public static boolean RandomCheck(float chance){
        // 限制范围 [0, 100]
        if (chance <= 0) return false;
        if (chance >= 100) return true;
        // 生成 [0.0, 100.0) 的随机浮点数
        float roll = random.nextFloat() * 100.0f;
        return roll < chance;
    }
}
