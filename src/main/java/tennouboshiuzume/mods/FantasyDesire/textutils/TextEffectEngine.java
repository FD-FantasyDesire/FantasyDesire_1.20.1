package tennouboshiuzume.mods.FantasyDesire.textutils;

import tennouboshiuzume.mods.FantasyDesire.textutils.anim.TextEffectContext;
import tennouboshiuzume.mods.FantasyDesire.textutils.anim.TextEffectRenderers;

/** 只负责编排正文和下层副本，具体效果行为由 anim 包中的渲染器实现。 */
public final class TextEffectEngine {
    private TextEffectEngine() {
    }

    public static GlyphVisual evaluateMain(RichTextDocument.Glyph glyph, double globalTicks, double ageTicks) {
        GlyphVisual visual = new GlyphVisual(glyph.style());
        TextEffectContext context = new TextEffectContext(globalTicks, ageTicks);
        int end = firstLayerIndex(glyph);
        for (int i = 0; i < end; i++) {
            TextEffectRenderers.apply(glyph.effects().get(i), visual, context);
        }
        return visual;
    }

    public static GlyphVisual evaluateShadow(RichTextDocument.Glyph glyph, GlyphVisual main,
            double globalTicks, double ageTicks) {
        int shadowIndex = shadowIndex(glyph);
        if (shadowIndex < 0) {
            return null;
        }

        RichTextDocument.EffectRef shadowRef = glyph.effects().get(shadowIndex);
        TextEffectSpec.Shadow spec = (TextEffectSpec.Shadow) shadowRef.spec();
        GlyphVisual shadow = new GlyphVisual(main);
        TextEffectRenderers.shadow().configure(spec, shadow);

        TextEffectContext context = new TextEffectContext(globalTicks, ageTicks);
        for (int i = shadowIndex + 1; i < layerEndIndex(glyph, shadowIndex); i++) {
            TextEffectRenderers.apply(glyph.effects().get(i), shadow, context);
        }
        return shadow;
    }

    public static GlyphVisual evaluateOutline(RichTextDocument.Glyph glyph, GlyphVisual main,
            double globalTicks, double ageTicks) {
        int outlineIndex = outlineIndex(glyph);
        if (outlineIndex < 0) {
            return null;
        }

        RichTextDocument.EffectRef outlineRef = glyph.effects().get(outlineIndex);
        TextEffectSpec.Outline spec = (TextEffectSpec.Outline) outlineRef.spec();
        GlyphVisual outline = new GlyphVisual(main);
        TextEffectRenderers.outline().configure(spec, outline);

        TextEffectContext context = new TextEffectContext(globalTicks, ageTicks);
        for (int i = outlineIndex + 1; i < layerEndIndex(glyph, outlineIndex); i++) {
            TextEffectRenderers.apply(glyph.effects().get(i), outline, context);
        }
        return outline;
    }

    public static int shadowIndex(RichTextDocument.Glyph glyph) {
        for (int i = 0; i < glyph.effects().size(); i++) {
            if (glyph.effects().get(i).spec() instanceof TextEffectSpec.Shadow) {
                return i;
            }
        }
        return -1;
    }

    public static int outlineIndex(RichTextDocument.Glyph glyph) {
        for (int i = 0; i < glyph.effects().size(); i++) {
            if (glyph.effects().get(i).spec() instanceof TextEffectSpec.Outline) {
                return i;
            }
        }
        return -1;
    }

    public static int firstLayerIndex(RichTextDocument.Glyph glyph) {
        for (int i = 0; i < glyph.effects().size(); i++) {
            if (isLayerEffect(glyph.effects().get(i).spec())) {
                return i;
            }
        }
        return glyph.effects().size();
    }

    public static int layerEndIndex(RichTextDocument.Glyph glyph, int layerIndex) {
        for (int i = layerIndex + 1; i < glyph.effects().size(); i++) {
            if (isLayerEffect(glyph.effects().get(i).spec())) {
                return i;
            }
        }
        return glyph.effects().size();
    }

    private static boolean isLayerEffect(TextEffectSpec spec) {
        return spec instanceof TextEffectSpec.Shadow || spec instanceof TextEffectSpec.Outline;
    }
}
