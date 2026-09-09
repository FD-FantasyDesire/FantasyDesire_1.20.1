package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

import tennouboshiuzume.mods.FantasyDesire.textutils.GlyphVisual;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

public final class RainbowRenderer extends TextEffectRenderer<TextEffectSpec.Rainbow> {
    public RainbowRenderer() {
        super(TextEffectSpec.Rainbow.class);
    }

    @Override
    protected void renderTyped(TextEffectSpec.Rainbow spec, RichTextDocument.EffectRef ref,
            GlyphVisual visual, TextEffectContext context) {
        float position = (ref.localIndex() + 0.5F) / Math.max(1.0F, ref.length());
        visual.setColor(colorAt(spec, position, ref.length(), context.globalTicks()));
    }

    public int colorAt(TextEffectSpec.Rainbow spec, float position, int scopeLength, double globalTicks) {
        float glyphPosition = position * Math.max(1, scopeLength);
        float hue = AnimMath.wrap(glyphPosition / Math.max(1.0F, spec.spread())
                + (float) (globalTicks / spec.period()));
        return AnimMath.hsvToRgb(hue, 1.0F, 1.0F);
    }
}
