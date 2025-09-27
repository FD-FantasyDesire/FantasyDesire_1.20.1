package tennouboshiuzume.mods.FantasyDesire.utils;
//用于颜色计算的工具类
public class ColorUtils {
    public static int getSmoothTransitionColor(float step, int totalSteps) {
        // 将渐变过程分成三个阶段，计算每个阶段的长度
//        step += (int) (totalSteps/6);
        float phaseLength = (float) totalSteps / 3;

        // 如果 step 大于 totalSteps，将其限制在 totalSteps 范围内
        if (step > totalSteps) {
            step = step%totalSteps;
        }

        int r = 0, g = 0, b = 0;

        // 第一阶段：从红色到绿色
        if (step <= phaseLength) {
            float progress = (float) step / phaseLength;
            r = (int) (255 * (1 - progress)); // 红色从255渐变到0
            g = (int) (255 * progress);       // 绿色从0渐变到255
            b = 0;                            // 蓝色保持0
        }
        // 第二阶段：从绿色到蓝色
        else if (step <= 2 * phaseLength) {
            float progress = (float) (step - phaseLength) / phaseLength;
            r = 0;                            // 红色保持0
            g = (int) (255 * (1 - progress)); // 绿色从255渐变到0
            b = (int) (255 * progress);       // 蓝色从0渐变到255
        }
        // 第三阶段：从蓝色回到红色
        else {
            float progress = (float) (step - 2 * phaseLength) / phaseLength;
            r = (int) (255 * progress);       // 红色从0渐变到255
            g = 0;                            // 绿色保持0
            b = (int) (255 * (1 - progress)); // 蓝色从255渐变到0
        }

        // 限制 r, g, b 的值在 0 到 255 之间，避免溢出
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        // 将 RGB 值合并为一个整数
        return (r << 16) | (g << 8) | b;
    }

    public static int getSmoothTransitionColor(float step, int totalSteps, boolean isHex) {
        // 限制 step 在 [0, totalSteps) 内
        if (step >= totalSteps) {
            step = step % totalSteps;
        }

        // Hue 从 0 到 360 度，走完整个色相环
        float hue = (step / totalSteps) * 360.0f;

        // HSV 参数
        float saturation = 1.0f; // 饱和度固定100%
        float value = 1.0f;      // 亮度固定100%

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