package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

import java.util.List;

import tennouboshiuzume.mods.FantasyDesire.textutils.GlyphVisual;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

public final class TextEffectRenderers {
    private static final GradientRenderer GRADIENT = new GradientRenderer();
    private static final ShadowRenderer SHADOW = new ShadowRenderer();
    private static final List<TextEffectRenderer<?>> ALL = List.of(
            GRADIENT,
            new RainbowRenderer(),
            new WaveBoldRenderer(),
            new WaveSlideRenderer(),
            SHADOW,
            new ShakeRenderer(),
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

    public static int gradientColor(TextEffectSpec.Gradient spec, float position, double globalTicks) {
        return GRADIENT.colorAt(spec, position, globalTicks);
    }

    public static ShadowRenderer shadow() {
        return SHADOW;
    }
}
