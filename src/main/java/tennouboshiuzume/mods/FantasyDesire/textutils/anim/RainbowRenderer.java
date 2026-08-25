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
        float position = ref.localIndex() / Math.max(1.0F, spec.spread());
        float hue = AnimMath.wrap(position + (float) (context.globalTicks() / spec.period()));
        visual.setColor(AnimMath.hsvToRgb(hue, 1.0F, 1.0F));
    }
}
