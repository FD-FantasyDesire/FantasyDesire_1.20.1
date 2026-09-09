package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

import java.util.List;

import tennouboshiuzume.mods.FantasyDesire.textutils.GlyphVisual;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

public final class TextEffectRenderers {
    private static final GradientRenderer GRADIENT = new GradientRenderer();
    private static final RainbowRenderer RAINBOW = new RainbowRenderer();
    private static final ShadowRenderer SHADOW = new ShadowRenderer();
    private static final OutlineRenderer OUTLINE = new OutlineRenderer();
    private static final List<TextEffectRenderer<?>> ALL = List.of(
            GRADIENT,
            RAINBOW,
            new WaveBoldRenderer(),
            new WaveSlideRenderer(),
            SHADOW,
            OUTLINE,
            new ShakeRenderer(),
            new GlitchRenderer(),
            new WaveRenderer(),
            new TypewriterRenderer());

    private TextEffectRenderers() {
    }

    public static void apply(RichTextDocument.EffectRef ref, GlyphVisual visual,
            TextEffectContext context) {
        for (TextEffectRenderer<?> renderer : ALL) {
            if (renderer.supports(ref.spec())) {
                renderer.apply(ref.spec(), ref, visual, context);
                return;
            }
        }
    }

    public static int continuousColor(TextEffectSpec spec, float position, int scopeLength, double globalTicks) {
        if (spec instanceof TextEffectSpec.Gradient gradient) {
            return GRADIENT.colorAt(gradient, position, globalTicks);
        }
        if (spec instanceof TextEffectSpec.Rainbow rainbow) {
            return RAINBOW.colorAt(rainbow, position, scopeLength, globalTicks);
        }
        throw new IllegalArgumentException("效果不支持连续颜色采样: " + spec.getClass().getSimpleName());
    }

    public static ShadowRenderer shadow() {
        return SHADOW;
    }

    public static OutlineRenderer outline() {
        return OUTLINE;
    }
}
