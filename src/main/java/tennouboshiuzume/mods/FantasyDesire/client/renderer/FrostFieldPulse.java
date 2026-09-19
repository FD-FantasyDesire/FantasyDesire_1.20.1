package tennouboshiuzume.mods.FantasyDesire.client.renderer;

/** 每个领域只维护一个波峰；尾部离开当前边界后才能开始下一轮。 */
final class FrostFieldPulse {
    // 单位：格/tick；尾宽与两个冰霜片元着色器中的 1.2 格衰减范围一致。
    private static final double SPEED = 3.2 / 20.0;
    private static final double TAIL_WIDTH = 1.2;
    private double previousTick = Double.NaN;
    private double radius;

    float advance(double tick, float fieldRadius) {
        if (!Double.isFinite(tick) || !Float.isFinite(fieldRadius) || fieldRadius <= 0.0F) {
            return 0.0F;
        }
        if (Double.isNaN(previousTick) || tick < previousTick) {
            previousTick = tick;
            radius = 0.0;
        }
        radius += (tick - previousTick) * SPEED;
        previousTick = tick;
        // 半径扩张时让当前波继续前进，不依据新的周期反算进度；同一帧重复采集也不会加速。
        if (radius >= fieldRadius + TAIL_WIDTH) {
            radius = 0.0;
        }
        return (float) radius;
    }
}
