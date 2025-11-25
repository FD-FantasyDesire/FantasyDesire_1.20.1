package tennouboshiuzume.mods.FantasyDesire.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlowingLineParticle extends Particle {
    private final Vec3 start, end;
    private final float thickness;
    private final int color;
    private float alphaOverride = 0.75f;
    private boolean fade = false;

    public static final ParticleRenderType GLOWING_LINE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder bufferBuilder, TextureManager textureManager) {
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
        }

        @Override
        public String toString() {
            return "fantasydesire:glowing_line";
        }
    };


    public GlowingLineParticle(ClientLevel level, Vec3 start, Vec3 end, int color, float thickness, float alpha, boolean fade, int lifetime) {
        super(level, start.x, start.y, start.z);
        this.start = start;
        this.end = end;
        this.color = color;
        this.thickness = thickness;
        this.lifetime = lifetime;
        this.alphaOverride = alpha;
        this.fade = fade;
        this.age = 0;
        this.hasPhysics = false;
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();

        Vec3 from = start.subtract(camPos);
        Vec3 to = end.subtract(camPos);

        // 光束方向
        Vec3 dir = to.subtract(from).normalize();

        // 任意法线（与dir不平行）
        Vec3 up = Math.abs(dir.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = dir.cross(up).normalize();

        // 生命周期透明度
        float alpha;
        if (fade) {
            float lifeRatio = (float) age / lifetime;
            alpha = (1.0f - lifeRatio) * alphaOverride;
        } else {
            alpha = alphaOverride;
        }

        // 绕光束方向旋转生成圆柱
        int segments = 8; // 分段数量（更多 = 更圆）
        double angleStep = (Math.PI * 2.0) / segments;

        float r = ((this.color >> 16) & 0xFF) / 255.0F;
        float g = ((this.color >> 8) & 0xFF) / 255.0F;
        float b = (this.color & 0xFF) / 255.0F;

        for (int i = 0; i < segments; i++) {
            double a0 = i * angleStep;
            double a1 = (i + 1) * angleStep;

            // 旋转后的两个方向向量
            Vec3 offset0 = rotateAroundAxis(right, dir, a0).scale(thickness * 0.5);
            Vec3 offset1 = rotateAroundAxis(right, dir, a1).scale(thickness * 0.5);

            Vec3 v1 = from.add(offset0);
            Vec3 v2 = from.add(offset1);
            Vec3 v3 = to.add(offset1);
            Vec3 v4 = to.add(offset0);

            vertexConsumer.vertex(v1.x, v1.y, v1.z).color(r, g, b, alpha).endVertex();
            vertexConsumer.vertex(v2.x, v2.y, v2.z).color(r, g, b, alpha).endVertex();
            vertexConsumer.vertex(v3.x, v3.y, v3.z).color(r, g, b, alpha).endVertex();
            vertexConsumer.vertex(v4.x, v4.y, v4.z).color(r, g, b, alpha).endVertex();
        }
    }

    private static Vec3 rotateAroundAxis(Vec3 vec, Vec3 axis, double angle) {
        // Rodrigues 旋转公式
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dot = vec.dot(axis);
        Vec3 cross = axis.cross(vec);

        return new Vec3(
                vec.x * cos + cross.x * sin + axis.x * dot * (1 - cos),
                vec.y * cos + cross.y * sin + axis.y * dot * (1 - cos),
                vec.z * cos + cross.z * sin + axis.z * dot * (1 - cos)
        );
    }

    @Override
    public ParticleRenderType getRenderType() {
        return GLOWING_LINE;
    }

    @Override
    public void tick() {
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }
    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }
    @Override
    public boolean shouldCull() {
        return false;
    }

}