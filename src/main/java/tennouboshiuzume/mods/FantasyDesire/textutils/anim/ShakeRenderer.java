package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

import tennouboshiuzume.mods.FantasyDesire.textutils.GlyphVisual;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

public final class ShakeRenderer extends TextEffectRenderer<TextEffectSpec.Shake> {
    public ShakeRenderer() {
        super(TextEffectSpec.Shake.class);
    }

    @Override
    protected void renderTyped(TextEffectSpec.Shake spec, RichTextDocument.EffectRef ref,
            GlyphVisual visual, TextEffectContext context) {
        long frame = (long) Math.floor(context.globalTicks() / spec.period());
        visual.addOffsetX(AnimMath.noise(spec.seed(), ref.localIndex(), frame) * spec.amplitudeX());
        visual.addOffsetY(AnimMath.noise(spec.seed() ^ 0x6D2B79F5, ref.localIndex(), frame)
                * spec.amplitudeY());
    }
}
