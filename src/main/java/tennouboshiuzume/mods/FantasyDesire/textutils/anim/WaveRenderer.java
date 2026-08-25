package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

import tennouboshiuzume.mods.FantasyDesire.textutils.GlyphVisual;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

public final class WaveRenderer extends TextEffectRenderer<TextEffectSpec.Wave> {
    private static final float TAU = (float) (Math.PI * 2.0);

    public WaveRenderer() {
        super(TextEffectSpec.Wave.class);
    }

    @Override
    protected void renderTyped(TextEffectSpec.Wave spec, RichTextDocument.EffectRef ref,
            GlyphVisual visual, TextEffectContext context) {
        float spatial = ref.localIndex() / spec.wavelength();
        float temporal = (float) (context.globalTicks() / spec.period());
        visual.addOffsetY((float) Math.sin((spatial - temporal) * TAU) * spec.amplitude());
    }
}
