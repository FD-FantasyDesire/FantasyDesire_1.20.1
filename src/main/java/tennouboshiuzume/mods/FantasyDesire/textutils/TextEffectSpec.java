package tennouboshiuzume.mods.FantasyDesire.textutils;

import java.util.List;

/**
 * 富文本效果的强类型数据规格。时间统一使用 1/20 秒为一 tick，位移统一使用 GUI 像素。
 */
public sealed interface TextEffectSpec permits TextEffectSpec.Gradient, TextEffectSpec.Rainbow,
        TextEffectSpec.WaveBold, TextEffectSpec.WaveSlide, TextEffectSpec.Shadow,
        TextEffectSpec.Outline, TextEffectSpec.Shake, TextEffectSpec.Glitch,
        TextEffectSpec.Wave, TextEffectSpec.Typewriter {

    record Gradient(List<Integer> colors, float period, boolean animated) implements TextEffectSpec {
        public Gradient {
            colors = List.copyOf(colors);
        }
    }

    record Rainbow(float period, float spread) implements TextEffectSpec {
    }

    record WaveBold(float stepTicks) implements TextEffectSpec {
    }

    record WaveSlide(float stepTicks, int width) implements TextEffectSpec {
    }

    record Shadow(int color, float offsetX, float offsetY) implements TextEffectSpec {
    }

    record Outline(int color, float offsetX, float offsetY) implements TextEffectSpec {
    }

    record Shake(float amplitudeX, float amplitudeY, float period, int seed) implements TextEffectSpec {
    }

    record Glitch(float amplitude, float sliceHeight, float period, float chance, int seed)
            implements TextEffectSpec {
    }

    record Wave(float amplitude, float wavelength, float period) implements TextEffectSpec {
    }

    record Typewriter(float charactersPerSecond, float delay, float fade, boolean reserve) implements TextEffectSpec {
    }
}
