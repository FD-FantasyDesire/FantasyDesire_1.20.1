package tennouboshiuzume.mods.FantasyDesire.textutils.anim;

import java.util.List;

import tennouboshiuzume.mods.FantasyDesire.textutils.GlyphVisual;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

public final class GradientRenderer extends TextEffectRenderer<TextEffectSpec.Gradient> {
    public GradientRenderer() {
        super(TextEffectSpec.Gradient.class);
    }

    @Override
    protected void renderTyped(TextEffectSpec.Gradient spec, RichTextDocument.EffectRef ref,
            GlyphVisual visual, TextEffectContext context) {
        float position = (ref.localIndex() + 0.5F) / Math.max(1.0F, ref.length());
        visual.setColor(colorAt(spec, position, context.globalTicks()));
    }

    public int colorAt(TextEffectSpec.Gradient spec, float position, double globalTicks) {
        List<Integer> colors = spec.colors();
        if (colors.size() == 1) {
            return colors.get(0);
        }

        if (spec.animated()) {
            float animatedPosition = AnimMath.wrap(position + (float) (globalTicks / spec.period()));
            float scaled = animatedPosition * colors.size();
            int first = (int) Math.floor(scaled) % colors.size();
            int second = (first + 1) % colors.size();
            return AnimMath.lerpColor(colors.get(first), colors.get(second),
                    scaled - (float) Math.floor(scaled));
        }

        float clampedPosition = Math.max(0.0F, Math.min(1.0F, position));
        float scaled = clampedPosition * (colors.size() - 1);
        int first = Math.min(colors.size() - 1, (int) Math.floor(scaled));
        int second = Math.min(colors.size() - 1, first + 1);
        return AnimMath.lerpColor(colors.get(first), colors.get(second), scaled - first);
    }
}
