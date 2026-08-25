package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

import tennouboshiuzume.mods.FantasyDesire.textutils.GlyphVisual;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

public final class TypewriterRenderer extends TextEffectRenderer<TextEffectSpec.Typewriter> {
    public TypewriterRenderer() {
        super(TextEffectSpec.Typewriter.class);
    }

    @Override
    protected void renderTyped(TextEffectSpec.Typewriter spec, RichTextDocument.EffectRef ref,
            GlyphVisual visual, TextEffectContext context) {
        double revealTick = spec.delay() + ref.localIndex() * 20.0 / spec.charactersPerSecond();
        double elapsed = context.ageTicks() - revealTick;
        if (elapsed < 0.0) {
            visual.setVisible(false);
        } else if (spec.fade() > 0.0F) {
            visual.multiplyAlpha((float) Math.min(1.0, elapsed / spec.fade()));
        }
    }
}
