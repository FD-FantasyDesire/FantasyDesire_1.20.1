package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

import tennouboshiuzume.mods.FantasyDesire.textutils.GlyphVisual;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

public final class WaveBoldRenderer extends TextEffectRenderer<TextEffectSpec.WaveBold> {
    public WaveBoldRenderer() {
        super(TextEffectSpec.WaveBold.class);
    }

    @Override
    protected void renderTyped(TextEffectSpec.WaveBold spec, RichTextDocument.EffectRef ref,
            GlyphVisual visual, TextEffectContext context) {
        int middle = ref.length() / 2;
        int radius = AnimMath.floorMod((int) Math.floor(context.globalTicks() / spec.stepTicks()), middle + 2);
        if (ref.localIndex() == middle - radius || ref.localIndex() == middle + radius) {
            visual.setStyle(visual.style().withBold(true));
        }
    }
}
