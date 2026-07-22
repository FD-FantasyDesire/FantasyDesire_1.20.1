package tennouboshiuzume.mods.FantasyDesire.utils;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

//用于颜色计算的工具类
public class ColorUtils {
    /**
     * Reads the particle command form 0xRRGGBB (the alpha channel is never
     * accepted).
     */
    public static int readRgb(StringReader reader) throws CommandSyntaxException {
        String value = reader.readString();
        try {
            return parseRgb(value);
        } catch (IllegalArgumentException exception) {
            throw new SimpleCommandExceptionType(Component.literal("Expected 0xRRGGBB color"))
                    .createWithContext(reader);
        }
    }

    public static int parseRgb(String value) {
        String text = value;
        if (text.startsWith("0x") || text.startsWith("0X"))
            text = text.substring(2);
        if (text.length() != 6 || !text.matches("[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("Expected 0xRRGGBB color");
        }
        return Integer.parseInt(text, 16) & 0xFFFFFF;
    }

    public static String formatRgb(int color) {
        return String.format(java.util.Locale.ROOT, "0x%06X", color & 0xFFFFFF);
    }

    public static int getSmoothTransitionColor(float step, int totalSteps, boolean isHex) {
        // 限制 step 在 [0, totalSteps) 内
        if (step >= totalSteps) {
            step = step % totalSteps;
        }
        float hue = (step / totalSteps) * 360.0f;
        float saturation = 1.0f;
        float value = 1.0f;
        int rgb = java.awt.Color.HSBtoRGB(hue / 360f, saturation, value);

        if (isHex) {
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
