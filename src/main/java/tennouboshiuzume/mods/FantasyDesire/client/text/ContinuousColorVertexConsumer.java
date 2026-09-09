package tennouboshiuzume.mods.FantasyDesire.client.text;

import com.mojang.blaze3d.vertex.VertexConsumer;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;
import tennouboshiuzume.mods.FantasyDesire.textutils.anim.TextEffectRenderers;

/** 按顶点屏幕 X 坐标写入整段连续颜色，同时保留原版字体图集和 UV。 */
final class ContinuousColorVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final TextEffectSpec effect;
    private final float startX;
    private final float width;
    private final int scopeLength;
    private final double globalTicks;
    private double vertexX;

    ContinuousColorVertexConsumer(VertexConsumer delegate, TextEffectSpec effect,
            float startX, float endX, int scopeLength, double globalTicks) {
        this.delegate = delegate;
        this.effect = effect;
        this.startX = startX;
        this.width = Math.max(0.001F, endX - startX);
        this.scopeLength = Math.max(1, scopeLength);
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
        int color = TextEffectRenderers.continuousColor(effect, position, scopeLength, globalTicks);
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
