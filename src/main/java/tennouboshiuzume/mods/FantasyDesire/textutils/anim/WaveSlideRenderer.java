package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

import tennouboshiuzume.mods.FantasyDesire.textutils.GlyphVisual;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

public final class WaveSlideRenderer extends TextEffectRenderer<TextEffectSpec.WaveSlide> {
    public WaveSlideRenderer() {
        super(TextEffectSpec.WaveSlide.class);
    }

    @Override
    protected void renderTyped(TextEffectSpec.WaveSlide spec, RichTextDocument.EffectRef ref,
            GlyphVisual visual, TextEffectContext context) {
        int length = Math.max(1, ref.length());
        int peak = AnimMath.floorMod((int) Math.floor(context.globalTicks() / spec.stepTicks()), length);
        int distance = AnimMath.floorMod(ref.localIndex() - peak, length);
        if (distance < spec.width()) {
            visual.setStyle(visual.style().withBold(true));
        }
    }
}
