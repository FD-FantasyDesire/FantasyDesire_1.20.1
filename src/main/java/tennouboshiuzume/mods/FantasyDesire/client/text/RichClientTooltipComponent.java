package tennouboshiuzume.mods.FantasyDesire.client.text;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import tennouboshiuzume.mods.FantasyDesire.textutils.GlyphVisual;
import tennouboshiuzume.mods.FantasyDesire.textutils.RichTextDocument;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectEngine;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RichClientTooltipComponent implements ClientTooltipComponent {
    private static final int FULL_BRIGHT = 15728880;

    private final RichTooltipComponent data;
    private Font layoutFont;
    private List<PositionedGlyph> layout = List.of();
    private Map<Integer, ScopeBounds> gradientBounds = Map.of();
    private int textWidth;

    public RichClientTooltipComponent(RichTooltipComponent data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return 10;
    }

    @Override
    public int getWidth(Font font) {
        ensureLayout(font);
        return textWidth;
    }

    @Override
    public void renderText(Font font, int x, int y, Matrix4f pose,
            MultiBufferSource.BufferSource bufferSource) {
        ensureLayout(font);
        long now = Util.getMillis();
        double globalTicks = now / 50.0;
        double ageTicks = Math.max(0L, now - data.startedAtMillis()) / 50.0;

        for (PositionedGlyph positioned : layout) {
            RichTextDocument.Glyph glyph = positioned.glyph();
            GlyphVisual main = TextEffectEngine.evaluateMain(glyph, globalTicks, ageTicks);
            GlyphVisual shadow = TextEffectEngine.evaluateShadow(glyph, main, globalTicks, ageTicks);
            int shadowIndex = TextEffectEngine.shadowIndex(glyph);

            if (shadow != null) {
                RichTextDocument.EffectRef shadowGradient = activeGradient(
                        glyph, shadowIndex + 1, glyph.effects().size());
                drawLayer(font, positioned, shadow, shadowGradient, x, y, pose, bufferSource, globalTicks);
            }

            int mainEffectEnd = shadowIndex < 0 ? glyph.effects().size() : shadowIndex;
            RichTextDocument.EffectRef mainGradient = activeGradient(glyph, 0, mainEffectEnd);
            drawLayer(font, positioned, main, mainGradient, x, y, pose, bufferSource, globalTicks);
        }
    }

    private void drawLayer(Font font, PositionedGlyph positioned, GlyphVisual visual,
            RichTextDocument.EffectRef gradientRef, int x, int y, Matrix4f pose,
            MultiBufferSource.BufferSource bufferSource, double globalTicks) {
        if (!visual.visible() || visual.alpha() <= 0.01F) {
            return;
        }

        RichTextDocument.Glyph glyph = positioned.glyph();
        float drawX = x + positioned.x() + visual.offsetX();
        float drawY = y + visual.offsetY();
        Style style = visual.style().withColor(TextColor.fromRgb(visual.color()));
        ScopeBounds bounds = gradientRef == null ? null : gradientBounds.get(gradientRef.scopeId());
        if (gradientRef != null && bounds != null
                && gradientRef.spec() instanceof TextEffectSpec.Gradient gradient) {
            float gradientStartX = transformX(pose, x + bounds.start(), y);
            float gradientEndX = transformX(pose, x + bounds.end(), y);
            drawGradientGlyph(font, glyph, style, visual.argb(), drawX, drawY, pose, bufferSource,
                    gradient, gradientStartX, gradientEndX, globalTicks);
        } else {
            drawGlyph(font, glyph, style, visual.argb(), drawX, drawY, pose, bufferSource);
        }
    }

    private static void drawGlyph(Font font, RichTextDocument.Glyph glyph, Style style, int color,
            float x, float y, Matrix4f pose, MultiBufferSource.BufferSource bufferSource) {
        FormattedCharSequence sequence = sink -> sink.accept(glyph.index(), style, glyph.codePoint());
        Matrix4f glyphPose = new Matrix4f(pose).translate(x, y, 0.0F);
        font.drawInBatch(sequence, 0.0F, 0.0F, color, false, glyphPose, bufferSource,
                Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
    }

    private static void drawGradientGlyph(Font font, RichTextDocument.Glyph glyph, Style style, int color,
            float x, float y, Matrix4f pose, MultiBufferSource.BufferSource bufferSource,
            TextEffectSpec.Gradient gradient, float gradientStartX, float gradientEndX, double globalTicks) {
        FormattedCharSequence sequence = sink -> sink.accept(glyph.index(), style, glyph.codePoint());
        Matrix4f glyphPose = new Matrix4f(pose).translate(x, y, 0.0F);
        MultiBufferSource gradientSource = renderType -> new GradientVertexConsumer(
                bufferSource.getBuffer(renderType), gradient, gradientStartX, gradientEndX, globalTicks);
        font.drawInBatch(sequence, 0.0F, 0.0F, color, false, glyphPose, gradientSource,
                Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
    }

    private void ensureLayout(Font font) {
        if (layoutFont == font) {
            return;
        }
        layoutFont = font;
        List<PositionedGlyph> positioned = new ArrayList<>();
        Map<Integer, MutableBounds> boundsByScope = new HashMap<>();
        int x = 0;
        for (RichTextDocument.Glyph glyph : data.document().glyphs()) {
            Style widestStyle = hasBoldEffect(glyph) ? glyph.style().withBold(true) : glyph.style();
            FormattedCharSequence sequence = sink -> sink.accept(glyph.index(), widestStyle, glyph.codePoint());
            int advance = font.width(sequence);
            positioned.add(new PositionedGlyph(glyph, x, advance));
            for (RichTextDocument.EffectRef effect : glyph.effects()) {
                if (effect.spec() instanceof TextEffectSpec.Gradient) {
                    boundsByScope.computeIfAbsent(effect.scopeId(), ignored -> new MutableBounds())
                            .include(x, x + advance);
                }
            }
            x += advance;
        }
        layout = List.copyOf(positioned);
        Map<Integer, ScopeBounds> completedBounds = new HashMap<>();
        boundsByScope.forEach((scopeId, bounds) -> completedBounds.put(scopeId, bounds.build()));
        gradientBounds = Map.copyOf(completedBounds);
        textWidth = x;
    }

    private static boolean hasBoldEffect(RichTextDocument.Glyph glyph) {
        return glyph.effects().stream().anyMatch(ref -> ref.spec() instanceof TextEffectSpec.WaveBold
                || ref.spec() instanceof TextEffectSpec.WaveSlide);
    }

    private static RichTextDocument.EffectRef activeGradient(RichTextDocument.Glyph glyph, int start, int end) {
        RichTextDocument.EffectRef active = null;
        for (int i = start; i < end; i++) {
            RichTextDocument.EffectRef effect = glyph.effects().get(i);
            if (effect.spec() instanceof TextEffectSpec.Gradient) {
                active = effect;
            } else if (effect.spec() instanceof TextEffectSpec.Rainbow) {
                active = null;
            }
        }
        return active;
    }

    private static float transformX(Matrix4f pose, float x, float y) {
        Vector4f transformed = pose.transform(new Vector4f(x, y, 0.0F, 1.0F));
        return transformed.w() == 0.0F ? transformed.x() : transformed.x() / transformed.w();
    }

    private record PositionedGlyph(RichTextDocument.Glyph glyph, int x, int advance) {
    }

    private record ScopeBounds(float start, float end) {
    }

    private static final class MutableBounds {
        private float start = Float.POSITIVE_INFINITY;
        private float end = Float.NEGATIVE_INFINITY;

        private void include(float glyphStart, float glyphEnd) {
            start = Math.min(start, glyphStart);
            end = Math.max(end, glyphEnd);
        }

        private ScopeBounds build() {
            return new ScopeBounds(start, end);
        }
    }
}
