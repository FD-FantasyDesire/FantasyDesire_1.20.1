package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

import tennouboshiuzume.mods.FantasyDesire.textutils.GlyphVisual;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

public final class OutlineRenderer extends TextEffectRenderer<TextEffectSpec.Outline> {
    public OutlineRenderer() {
        super(TextEffectSpec.Outline.class);
    }

    @Override
    protected void renderTyped(TextEffectSpec.Outline spec, RichTextDocument.EffectRef ref,
            GlyphVisual visual, TextEffectContext context) {
        configure(spec, visual);
    }

    public void configure(TextEffectSpec.Outline spec, GlyphVisual visual) {
        visual.setColor(spec.color() & 0xFFFFFF);
        visual.multiplyAlpha(((spec.color() >>> 24) & 0xFF) / 255.0F);
    }
}
