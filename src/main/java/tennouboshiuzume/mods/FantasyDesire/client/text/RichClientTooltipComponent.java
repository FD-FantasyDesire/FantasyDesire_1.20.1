package tennouboshiuzume.mods.FantasyDesire.client.text;

import com.mojang.blaze3d.vertex.VertexConsumer;
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
    private Map<Integer, ScopeBounds> colorEffectBounds = Map.of();
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
            GlyphVisual outline = TextEffectEngine.evaluateOutline(glyph, main, globalTicks, ageTicks);
            int shadowIndex = TextEffectEngine.shadowIndex(glyph);
            int outlineIndex = TextEffectEngine.outlineIndex(glyph);

            if (shadow != null) {
                int shadowEnd = TextEffectEngine.layerEndIndex(glyph, shadowIndex);
                RichTextDocument.EffectRef shadowColorEffect = activeColorEffect(
                        glyph, shadowIndex + 1, shadowEnd);
                RichTextDocument.EffectRef shadowGlitchEffect = activeGlitchEffect(
                        glyph, shadowIndex + 1, shadowEnd);
                drawLayer(font, positioned, shadow, shadowColorEffect, shadowGlitchEffect,
                        x, y, pose, bufferSource, globalTicks);
            }

            if (outline != null) {
                RichTextDocument.EffectRef outlineRef = glyph.effects().get(outlineIndex);
                drawOutline(font, positioned, outline, outlineRef, x, y, pose, bufferSource, globalTicks);
            }

            RichTextDocument.EffectRef mainColorEffect = activeColorEffect(
                    glyph, 0, TextEffectEngine.firstLayerIndex(glyph));
            RichTextDocument.EffectRef mainGlitchEffect = activeGlitchEffect(
                    glyph, 0, TextEffectEngine.firstLayerIndex(glyph));
            drawLayer(font, positioned, main, mainColorEffect, mainGlitchEffect,
                    x, y, pose, bufferSource, globalTicks);
        }
    }

    private void drawOutline(Font font, PositionedGlyph positioned, GlyphVisual visual,
            RichTextDocument.EffectRef outlineRef, int x, int y, Matrix4f pose,
            MultiBufferSource.BufferSource bufferSource, double globalTicks) {
        if (!visual.visible() || visual.alpha() <= 0.01F) {
            return;
        }

        TextEffectSpec.Outline spec = (TextEffectSpec.Outline) outlineRef.spec();
        float radiusX = Math.abs(spec.offsetX());
        float radiusY = Math.abs(spec.offsetY());
        RichTextDocument.Glyph glyph = positioned.glyph();
        int outlineIndex = outlineRefIndex(glyph, outlineRef);
        int outlineEnd = TextEffectEngine.layerEndIndex(glyph, outlineIndex);
        RichTextDocument.EffectRef colorEffect = activeColorEffect(
                glyph, outlineIndex + 1, outlineEnd);
        RichTextDocument.EffectRef glitchEffect = activeGlitchEffect(
                glyph, outlineIndex + 1, outlineEnd);
        for (int xDirection = -1; xDirection <= 1; xDirection++) {
            for (int yDirection = -1; yDirection <= 1; yDirection++) {
                if (xDirection == 0 && yDirection == 0) {
                    continue;
                }
                GlyphVisual copy = new GlyphVisual(visual);
                copy.addOffsetX(xDirection * radiusX);
                copy.addOffsetY(yDirection * radiusY);
                drawLayer(font, positioned, copy, colorEffect, glitchEffect,
                        x, y, pose, bufferSource, globalTicks);
            }
        }
    }

    private void drawLayer(Font font, PositionedGlyph positioned, GlyphVisual visual,
            RichTextDocument.EffectRef colorEffectRef, RichTextDocument.EffectRef glitchEffectRef,
            int x, int y, Matrix4f pose,
            MultiBufferSource.BufferSource bufferSource, double globalTicks) {
        if (!visual.visible() || visual.alpha() <= 0.01F) {
            return;
        }

        RichTextDocument.Glyph glyph = positioned.glyph();
        float drawX = x + positioned.x() + visual.offsetX();
        float drawY = y + visual.offsetY();
        Style style = visual.style().withColor(TextColor.fromRgb(visual.color()));
        ScopeBounds bounds = colorEffectRef == null ? null : colorEffectBounds.get(colorEffectRef.scopeId());
        if ((colorEffectRef != null && bounds != null) || glitchEffectRef != null) {
            float effectStartX = bounds == null ? transformX(pose, x, y)
                    : transformX(pose, x + bounds.start(), y);
            float effectEndX = bounds == null ? transformX(pose, x + textWidth, y)
                    : transformX(pose, x + bounds.end(), y);
            drawEffectGlyph(font, glyph, style, visual.argb(), drawX, drawY, pose, bufferSource,
                    colorEffectRef, glitchEffectRef, effectStartX, effectEndX, globalTicks);
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

    private static void drawEffectGlyph(Font font, RichTextDocument.Glyph glyph, Style style, int color,
            float x, float y, Matrix4f pose, MultiBufferSource.BufferSource bufferSource,
            RichTextDocument.EffectRef colorEffectRef, RichTextDocument.EffectRef glitchEffectRef,
            float effectStartX, float effectEndX, double globalTicks) {
        FormattedCharSequence sequence = sink -> sink.accept(glyph.index(), style, glyph.codePoint());
        Matrix4f glyphPose = new Matrix4f(pose).translate(x, y, 0.0F);
        MultiBufferSource effectSource = renderType -> {
            VertexConsumer consumer = bufferSource.getBuffer(renderType);
            if (colorEffectRef != null) {
                consumer = new ContinuousColorVertexConsumer(consumer, colorEffectRef.spec(),
                        effectStartX, effectEndX, colorEffectRef.length(), globalTicks);
            }
            if (glitchEffectRef != null && glitchEffectRef.spec() instanceof TextEffectSpec.Glitch glitch) {
                consumer = new GlitchSliceVertexConsumer(consumer, glitch, globalTicks);
            }
            return consumer;
        };
        font.drawInBatch(sequence, 0.0F, 0.0F, color, false, glyphPose, effectSource,
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
        int padding = 0;
        for (RichTextDocument.Glyph glyph : data.document().glyphs()) {
            Style widestStyle = hasBoldEffect(glyph) ? glyph.style().withBold(true) : glyph.style();
            FormattedCharSequence sequence = sink -> sink.accept(glyph.index(), widestStyle, glyph.codePoint());
            int advance = font.width(sequence);
            positioned.add(new PositionedGlyph(glyph, x, advance));
            padding = Math.max(padding, outlinePadding(glyph));
            for (RichTextDocument.EffectRef effect : glyph.effects()) {
                if (isContinuousColorEffect(effect.spec())) {
                    boundsByScope.computeIfAbsent(effect.scopeId(), ignored -> new MutableBounds())
                            .include(x, x + advance);
                }
            }
            x += advance;
        }
        layout = List.copyOf(positioned);
        Map<Integer, ScopeBounds> completedBounds = new HashMap<>();
        boundsByScope.forEach((scopeId, bounds) -> completedBounds.put(scopeId, bounds.build()));
        colorEffectBounds = Map.copyOf(completedBounds);
        textWidth = x + padding;
    }

    private static boolean hasBoldEffect(RichTextDocument.Glyph glyph) {
        return glyph.effects().stream().anyMatch(ref -> ref.spec() instanceof TextEffectSpec.WaveBold
                || ref.spec() instanceof TextEffectSpec.WaveSlide);
    }

    private static RichTextDocument.EffectRef activeColorEffect(RichTextDocument.Glyph glyph, int start, int end) {
        RichTextDocument.EffectRef active = null;
        for (int i = start; i < end; i++) {
            RichTextDocument.EffectRef effect = glyph.effects().get(i);
            if (isContinuousColorEffect(effect.spec())) {
                active = effect;
            }
        }
        return active;
    }

    private static RichTextDocument.EffectRef activeGlitchEffect(RichTextDocument.Glyph glyph, int start, int end) {
        RichTextDocument.EffectRef active = null;
        for (int i = start; i < end; i++) {
            RichTextDocument.EffectRef effect = glyph.effects().get(i);
            if (effect.spec() instanceof TextEffectSpec.Glitch) {
                active = effect;
            }
        }
        return active;
    }

    private static int outlineRefIndex(RichTextDocument.Glyph glyph, RichTextDocument.EffectRef target) {
        for (int i = 0; i < glyph.effects().size(); i++) {
            if (glyph.effects().get(i) == target) {
                return i;
            }
        }
        return 0;
    }

    private static int outlinePadding(RichTextDocument.Glyph glyph) {
        int index = TextEffectEngine.outlineIndex(glyph);
        if (index < 0) {
            return 0;
        }
        TextEffectSpec.Outline outline = (TextEffectSpec.Outline) glyph.effects().get(index).spec();
        return (int) Math.ceil(Math.max(Math.abs(outline.offsetX()), Math.abs(outline.offsetY())));
    }

    private static boolean isContinuousColorEffect(TextEffectSpec spec) {
        return spec instanceof TextEffectSpec.Gradient || spec instanceof TextEffectSpec.Rainbow;
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
