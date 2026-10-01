package tennouboshiuzume.mods.FantasyDesire.client.renderer;

/** 仅平滑气场显示强度；时间单位为游戏 tick，不改变服务端减伤层数。 */
final class ShinStrengthTransition {
    private static final double DURATION_TICKS = 5.0D;
    private float from;
    private float target;
    private double startedAt;

    ShinStrengthTransition(float strength, double time) {
        from = target = strength;
        startedAt = time;
    }

    void update(float strength, double time) {
        if (target != strength) {
            // 同步在过渡中途抵达时，从当前显示值继续，避免回跳。
            from = value(time);
            target = strength;
            startedAt = time;
        }
    }

    float value(double time) {
        double progress = Math.max(0.0D, Math.min(1.0D, (time - startedAt) / DURATION_TICKS));
        double smooth = progress * progress * (3.0D - 2.0D * progress);
        return (float) (from + (target - from) * smooth);
    }
}
