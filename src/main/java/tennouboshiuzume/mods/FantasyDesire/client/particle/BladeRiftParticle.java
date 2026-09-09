package tennouboshiuzume.mods.FantasyDesire.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.client.FDShaderHandler;
import tennouboshiuzume.mods.FantasyDesire.particle.BladeRiftParticleOptions;

/** A single world-space, camera-facing BladeRift ribbon. */
public class BladeRiftParticle extends Particle {
    private final Vec3 start, end;
    private final float riftLength, riftWidth;
    private final int coreColor, energyColor;
    private final ParticleRenderType renderType;

    public BladeRiftParticle(ClientLevel level, BladeRiftParticleOptions data) {
        super(level, (data.start.x + data.end.x) * .5, (data.start.y + data.end.y) * .5,
                (data.start.z + data.end.z) * .5);
        this.start = data.start;
        this.end = data.end;
        this.lifetime = data.lifetime;
        this.riftLength = data.riftLength;
        this.riftWidth = data.riftWidth;
        this.coreColor = data.coreColor;
        this.energyColor = data.energyColor;
        this.hasPhysics = false;
        this.renderType = createRenderType();
        // The mesh includes a generous SDF margin; keep that whole glow volume in the
        // particle bounds as well.
        double inflate = Math.max(.25, start.distanceTo(end) * riftWidth / riftLength * 28.0);
        setBoundingBox(new AABB(start, end).inflate(inflate));
    }

    private ParticleRenderType createRenderType() {
        return new ParticleRenderType() {
            @Override
            public void begin(BufferBuilder buffer, TextureManager textures) {
                ShaderInstance shader = FDShaderHandler.getBladeRiftShader();
                if (FDShaderHandler.isBladeRiftShaderLoaded() && shader != null) {
                    RenderSystem.disableCull();
                    RenderSystem.enableBlend();
                    RenderSystem.depthMask(false);
                    // This pass contains emission only. ONE/ONE allows the fragment shader's
                    // HDR halo to accumulate like bloom instead of attenuating it by alpha a
                    // second time (the former SRC_ALPHA/ONE path made the preview look dim).
                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE,
                            GlStateManager.DestFactor.ONE,
                            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                    RenderSystem.setShader(() -> shader);
                    float partial = Minecraft.getInstance().getFrameTime();
                    float progress = Math.min(1f, Math.max(0f, (age + partial) / lifetime));
                    set(shader, "Progress", progress);
                    set(shader, "FlowTime", (age + partial) * .05f);
                    set(shader, "RiftLength", riftLength);
                    set(shader, "RiftWidth", riftWidth);
                    setColor(shader, "CoreColor", coreColor);
                    setColor(shader, "EnergyColor", energyColor);
                }
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            }

            @Override
            public void end(Tesselator tesselator) {
                tesselator.end();
                RenderSystem.depthMask(true);
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
                RenderSystem.enableCull();
            }

            @Override
            public String toString() {
                return "fantasydesire:blade_rift/" + System.identityHashCode(this);
            }
        };
    }

    private static void set(ShaderInstance shader, String name, float value) {
        if (shader.getUniform(name) != null)
            shader.getUniform(name).set(value);
    }

    private static void setColor(ShaderInstance shader, String name, int color) {
        if (shader.getUniform(name) != null)
            shader.getUniform(name).set(
                    ((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f);
    }

    @Override
    public void render(VertexConsumer out, Camera camera, float partialTick) {
        if (!FDShaderHandler.isBladeRiftShaderLoaded())
            return;
        Vec3 axis = end.subtract(start);
        if (axis.lengthSqr() < 1.0E-8)
            return;
        double axisLength = axis.length();
        Vec3 forward = axis.scale(1.0 / axisLength);
        Vec3 center = start.add(end).scale(.5), view = camera.getPosition().subtract(center);
        Vec3 side = forward.cross(view.normalize());
        if (side.lengthSqr() < 1.0E-8)
            side = forward.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-8)
            side = forward.cross(new Vec3(1, 0, 0));

        // UV space is intentionally larger than the finite rift SDF. Both the
        // transverse halo and tip halo therefore reach zero before the quad edge.
        // The additive halo has a deliberately long tail. Keep over 20 widths of empty
        // SDF space on every side so that it reaches zero before rasterization ends.
        float axialMargin = Math.max(riftWidth * 24f, riftLength * .22f);
        float uvY = riftWidth * 28f;
        double worldPerLocalX = axisLength / (2.0 * riftLength);
        double worldPerLocalY = axisLength / riftLength;
        double halfWidth = Math.max(.08, worldPerLocalY * uvY);
        side = side.normalize().scale(halfWidth);
        Vec3 tipMargin = forward.scale(worldPerLocalX * axialMargin);
        Vec3 meshStart = start.subtract(tipMargin), meshEnd = end.add(tipMargin), cam = camera.getPosition();
        Vec3 a = meshStart.subtract(cam).add(side), b = meshStart.subtract(cam).subtract(side);
        Vec3 c = meshEnd.subtract(cam).subtract(side), d = meshEnd.subtract(cam).add(side);
        vertex(out, a, -riftLength - axialMargin, uvY);
        vertex(out, b, -riftLength - axialMargin, -uvY);
        vertex(out, c, riftLength + axialMargin, -uvY);
        vertex(out, d, riftLength + axialMargin, uvY);
    }

    private static void vertex(VertexConsumer out, Vec3 p, float u, float v) {
        out.vertex(p.x, p.y, p.z).color(1f, 1f, 1f, 1f).uv(u, v).endVertex();
    }

    @Override
    public void tick() {
        if (age++ >= lifetime)
            remove();
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return renderType;
    }
}
