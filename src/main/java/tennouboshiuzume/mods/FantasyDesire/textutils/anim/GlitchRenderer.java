package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

import tennouboshiuzume.mods.FantasyDesire.textutils.GlyphVisual;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

public final class GlitchRenderer extends TextEffectRenderer<TextEffectSpec.Glitch> {
    public GlitchRenderer() {
        super(TextEffectSpec.Glitch.class);
    }

    @Override
    protected void renderTyped(TextEffectSpec.Glitch spec, RichTextDocument.EffectRef ref,
            GlyphVisual visual, TextEffectContext context) {
        // Glitch 需要切分字形四边形，由客户端顶点阶段处理。
    }
}
