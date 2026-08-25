package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

import tennouboshiuzume.mods.FantasyDesire.textutils.GlyphVisual;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

public final class ShadowRenderer extends TextEffectRenderer<TextEffectSpec.Shadow> {
    public ShadowRenderer() {
        super(TextEffectSpec.Shadow.class);
    }

    @Override
    protected void renderTyped(TextEffectSpec.Shadow spec, RichTextDocument.EffectRef ref,
            GlyphVisual visual, TextEffectContext context) {
        configure(spec, visual);
    }

    public void configure(TextEffectSpec.Shadow spec, GlyphVisual visual) {
        visual.setColor(spec.color() & 0xFFFFFF);
        visual.multiplyAlpha(((spec.color() >>> 24) & 0xFF) / 255.0F);
        visual.addOffsetX(spec.offsetX());
        visual.addOffsetY(spec.offsetY());
    }
}
