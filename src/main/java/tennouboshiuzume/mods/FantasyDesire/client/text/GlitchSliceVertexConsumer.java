package tennouboshiuzume.mods.FantasyDesire.client.text;

import com.mojang.blaze3d.vertex.VertexConsumer;
import tennouboshiuzume.mods.FantasyDesire.textutils.TextEffectSpec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 将字体四边形切成水平扫描条，并对少量横条施加水平错位。 */
final class GlitchSliceVertexConsumer implements VertexConsumer {
    private static final int VERTICES_PER_QUAD = 4;
    private static final int MAX_SLICES = 32;

    private final VertexConsumer delegate;
    private final TextEffectSpec.Glitch spec;
    private final long frame;
    private final List<Vertex> vertices = new ArrayList<>(VERTICES_PER_QUAD);
    private Vertex current = new Vertex();

    GlitchSliceVertexConsumer(VertexConsumer delegate, TextEffectSpec.Glitch spec, double globalTicks) {
        this.delegate = delegate;
        this.spec = spec;
        this.frame = (long) Math.floor(globalTicks / spec.period());
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        current.x = (float) x;
        current.y = (float) y;
        current.z = (float) z;
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        current.red = red;
        current.green = green;
        current.blue = blue;
        current.alpha = alpha;
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        current.u = u;
        current.v = v;
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        current.overlayU = u;
        current.overlayV = v;
        current.hasOverlay = true;
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        current.lightU = u;
        current.lightV = v;
        current.hasLight = true;
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        current.normalX = x;
        current.normalY = y;
        current.normalZ = z;
        current.hasNormal = true;
        return this;
    }

    @Override
    public void endVertex() {
        vertices.add(current);
        current = new Vertex();
        if (vertices.size() == VERTICES_PER_QUAD) {
            emitSlicedQuad();
            vertices.clear();
        }
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
        delegate.defaultColor(red, green, blue, alpha);
    }

    @Override
    public void unsetDefaultColor() {
        delegate.unsetDefaultColor();
    }

    private void emitSlicedQuad() {
        List<Vertex> sorted = new ArrayList<>(vertices);
        sorted.sort(Comparator.comparingDouble(vertex -> vertex.y));
        List<Vertex> top = new ArrayList<>(sorted.subList(0, 2));
        List<Vertex> bottom = new ArrayList<>(sorted.subList(2, 4));
        top.sort(Comparator.comparingDouble(vertex -> vertex.x));
        bottom.sort(Comparator.comparingDouble(vertex -> vertex.x));

        Vertex topLeft = top.get(0);
        Vertex topRight = top.get(1);
        Vertex bottomLeft = bottom.get(0);
        Vertex bottomRight = bottom.get(1);
        float topY = (topLeft.y + topRight.y) * 0.5F;
        float bottomY = (bottomLeft.y + bottomRight.y) * 0.5F;
        float height = bottomY - topY;
        if (height <= 0.001F) {
            vertices.forEach(this::emit);
            return;
        }

        int sliceCount = Math.min(MAX_SLICES,
                Math.max(1, (int) Math.ceil(height / spec.sliceHeight())));
        for (int slice = 0; slice < sliceCount; slice++) {
            float t0 = slice / (float) sliceCount;
            float t1 = (slice + 1) / (float) sliceCount;
            float centerY = topY + height * (t0 + t1) * 0.5F;
            int scanline = (int) Math.floor(centerY / spec.sliceHeight());
            float offsetX = scanlineOffset(scanline);

            emit(interpolate(topLeft, bottomLeft, t0, offsetX));
            emit(interpolate(topLeft, bottomLeft, t1, offsetX));
            emit(interpolate(topRight, bottomRight, t1, offsetX));
            emit(interpolate(topRight, bottomRight, t0, offsetX));
        }
    }

    private float scanlineOffset(int scanline) {
        float trigger = normalizedNoise(spec.seed() ^ 0x13579BDF, scanline, frame);
        if (trigger >= spec.chance()) {
            return 0.0F;
        }
        return noise(spec.seed(), scanline, frame) * spec.amplitude();
    }

    private static Vertex interpolate(Vertex top, Vertex bottom, float amount, float offsetX) {
        Vertex result = new Vertex();
        result.x = lerp(top.x, bottom.x, amount) + offsetX;
        result.y = lerp(top.y, bottom.y, amount);
        result.z = lerp(top.z, bottom.z, amount);
        result.red = lerp(top.red, bottom.red, amount);
        result.green = lerp(top.green, bottom.green, amount);
        result.blue = lerp(top.blue, bottom.blue, amount);
        result.alpha = lerp(top.alpha, bottom.alpha, amount);
        result.u = lerp(top.u, bottom.u, amount);
        result.v = lerp(top.v, bottom.v, amount);
        result.overlayU = top.overlayU;
        result.overlayV = top.overlayV;
        result.hasOverlay = top.hasOverlay;
        result.lightU = top.lightU;
        result.lightV = top.lightV;
        result.hasLight = top.hasLight;
        result.normalX = lerp(top.normalX, bottom.normalX, amount);
        result.normalY = lerp(top.normalY, bottom.normalY, amount);
        result.normalZ = lerp(top.normalZ, bottom.normalZ, amount);
        result.hasNormal = top.hasNormal;
        return result;
    }

    private void emit(Vertex vertex) {
        delegate.vertex(vertex.x, vertex.y, vertex.z)
                .color(vertex.red, vertex.green, vertex.blue, vertex.alpha)
                .uv(vertex.u, vertex.v);
        if (vertex.hasOverlay) {
            delegate.overlayCoords(vertex.overlayU, vertex.overlayV);
        }
        if (vertex.hasLight) {
            delegate.uv2(vertex.lightU, vertex.lightV);
        }
        if (vertex.hasNormal) {
            delegate.normal(vertex.normalX, vertex.normalY, vertex.normalZ);
        }
        delegate.endVertex();
    }

    private static float normalizedNoise(int seed, int index, long frame) {
        return (noise(seed, index, frame) + 1.0F) * 0.5F;
    }

    private static float noise(int seed, int index, long frame) {
        long value = seed * 0x9E3779B9L ^ index * 0x85EBCA6BL ^ frame * 0xC2B2AE35L;
        value ^= value >>> 16;
        value *= 0x7FEB352DL;
        value ^= value >>> 15;
        return ((value & 0xFFFFL) / 32767.5F) - 1.0F;
    }

    private static float lerp(float first, float second, float amount) {
        return first + (second - first) * amount;
    }

    private static int lerp(int first, int second, float amount) {
        return Math.round(lerp((float) first, (float) second, amount));
    }

    private static final class Vertex {
        private float x;
        private float y;
        private float z;
        private int red = 255;
        private int green = 255;
        private int blue = 255;
        private int alpha = 255;
        private float u;
        private float v;
        private int overlayU;
        private int overlayV;
        private boolean hasOverlay;
        private int lightU;
        private int lightV;
        private boolean hasLight;
        private float normalX;
        private float normalY;
        private float normalZ = 1.0F;
        private boolean hasNormal;
    }
}
