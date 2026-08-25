package tennouboshiuzume.mods.FantasyDesire.client.text;

import com.mojang.blaze3d.vertex.VertexConsumer;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;
import tennouboshiuzume.mods.FantasyDesire.textutils.anim.TextEffectRenderers;

/** 在保留原版字体图集和 UV 的前提下，按顶点屏幕 X 坐标写入整段渐变颜色。 */
final class GradientVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final TextEffectSpec.Gradient gradient;
    private final float startX;
    private final float width;
    private final double globalTicks;
    private double vertexX;

    GradientVertexConsumer(VertexConsumer delegate, TextEffectSpec.Gradient gradient,
            float startX, float endX, double globalTicks) {
        this.delegate = delegate;
        this.gradient = gradient;
        this.startX = startX;
        this.width = Math.max(0.001F, endX - startX);
        this.globalTicks = globalTicks;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        vertexX = x;
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        float position = ((float) vertexX - startX) / width;
        int color = TextEffectRenderers.gradientColor(gradient, position, globalTicks);
        delegate.color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, alpha);
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        delegate.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        delegate.overlayCoords(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        delegate.uv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
        delegate.defaultColor(red, green, blue, alpha);
    }

    @Override
    public void unsetDefaultColor() {
        delegate.unsetDefaultColor();
    }
}
