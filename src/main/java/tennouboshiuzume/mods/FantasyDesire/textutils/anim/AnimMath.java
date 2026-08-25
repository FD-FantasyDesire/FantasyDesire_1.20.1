package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

final class AnimMath {
    private AnimMath() {
    }

    static float wrap(float value) {
        return value - (float) Math.floor(value);
    }

    static int floorMod(int value, int divisor) {
        return Math.floorMod(value, Math.max(1, divisor));
    }

    static float noise(int seed, int index, long frame) {
        long value = seed * 0x9E3779B9L ^ index * 0x85EBCA6BL ^ frame * 0xC2B2AE35L;
        value ^= value >>> 16;
        value *= 0x7FEB352DL;
        value ^= value >>> 15;
        return ((value & 0xFFFFL) / 32767.5F) - 1.0F;
    }

    static int lerpColor(int first, int second, float amount) {
        int r = (int) (((first >> 16) & 0xFF)
                + (((second >> 16) & 0xFF) - ((first >> 16) & 0xFF)) * amount);
        int g = (int) (((first >> 8) & 0xFF)
                + (((second >> 8) & 0xFF) - ((first >> 8) & 0xFF)) * amount);
        int b = (int) ((first & 0xFF) + ((second & 0xFF) - (first & 0xFF)) * amount);
        return (r << 16) | (g << 8) | b;
    }

    static int hsvToRgb(float hue, float saturation, float value) {
        float scaled = hue * 6.0F;
        int sector = (int) Math.floor(scaled);
        float fraction = scaled - sector;
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - fraction * saturation);
        float t = value * (1.0F - (1.0F - fraction) * saturation);
        float r;
        float g;
        float b;
        switch (floorMod(sector, 6)) {
            case 0 -> { r = value; g = t; b = p; }
            case 1 -> { r = q; g = value; b = p; }
            case 2 -> { r = p; g = value; b = t; }
            case 3 -> { r = p; g = q; b = value; }
            case 4 -> { r = t; g = p; b = value; }
            default -> { r = value; g = p; b = q; }
        }
        return ((int) (r * 255.0F) << 16) | ((int) (g * 255.0F) << 8) | (int) (b * 255.0F);
    }
}
