package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

import tennouboshiuzume.mods.FantasyDesire.textutils.GlyphVisual;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

public abstract class TextEffectRenderer<S extends TextEffectSpec> {
    private final Class<S> specType;

    protected TextEffectRenderer(Class<S> specType) {
        this.specType = specType;
    }

    public final boolean supports(TextEffectSpec spec) {
        return specType.isInstance(spec);
    }

    public final void apply(TextEffectSpec spec, RichTextDocument.EffectRef ref,
            GlyphVisual visual, TextEffectContext context) {
        renderTyped(specType.cast(spec), ref, visual, context);
    }

    protected abstract void renderTyped(S spec, RichTextDocument.EffectRef ref,
            GlyphVisual visual, TextEffectContext context);
}
