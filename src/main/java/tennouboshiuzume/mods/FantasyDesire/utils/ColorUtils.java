package tennouboshiuzume.mods.FantasyDesire.utils;

//用于颜色计算的工具类
public class ColorUtils {
    public static int getSmoothTransitionColor(float step, int totalSteps, boolean isHex) {
        // 限制 step 在 [0, totalSteps) 内
        if (step >= totalSteps) {
            step = step % totalSteps;
        }

        float hue = (step / totalSteps) * 360.0f;

        // HSV 参数
        float saturation = 1.0f; // 饱和度固定100%
        float value = 1.0f; // 亮度固定100%

        // 转换 HSV -> RGB
        int rgb = java.awt.Color.HSBtoRGB(hue / 360f, saturation, value);

        if (isHex) {
            // 去掉 alpha 通道，返回纯 RGB (0xRRGGBB)
            return rgb & 0xFFFFFF;
        } else {
            return rgb;
        }
    }

    public static int getSmoothTransitionColor(int colorStart, int colorEnd, float step, int totalSteps) {
        // 确保步数在0到2*totalSteps之间循环
        step = step % (2 * totalSteps);
        // 判断是前半段还是后半段
        if (step > totalSteps) {
            // 后半段则反向渐变
            step = 2 * totalSteps - step;
        }

        // 计算颜色比例
        float ratio = step / (float) totalSteps;

        // 提取颜色1的RGB分量
        int r1 = (colorStart >> 16) & 0xFF;
        int g1 = (colorStart >> 8) & 0xFF;
        int b1 = colorStart & 0xFF;

        // 提取颜色2的RGB分量
        int r2 = (colorEnd >> 16) & 0xFF;
        int g2 = (colorEnd >> 8) & 0xFF;
        int b2 = colorEnd & 0xFF;

        // 根据比例计算新的RGB分量
        int r = (int) Math.max(0, Math.min(255, r1 + (r2 - r1) * ratio));
        int g = (int) Math.max(0, Math.min(255, g1 + (g2 - g1) * ratio));
        int b = (int) Math.max(0, Math.min(255, b1 + (b2 - b1) * ratio));

        // 合并RGB为颜色
        return (r << 16) | (g << 8) | b;
    }

}