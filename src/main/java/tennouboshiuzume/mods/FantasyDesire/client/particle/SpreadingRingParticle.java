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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tennouboshiuzume.mods.FantasyDesire.particle.SpreadingRingParticleOptions;

@OnlyIn(Dist.CLIENT)
public class SpreadingRingParticle extends Particle {
    private final int color;
    private final float maxRadius;
    private final float thickness;

    public static final ParticleRenderType SPREADING_RING = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder bufferBuilder, TextureManager textureManager) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
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
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "fantasydesire:spreading_ring";
        }
    };

    public SpreadingRingParticle(ClientLevel level, double x, double y, double z, int color, float maxRadius,
            float thickness, int lifetime) {
        super(level, x, y, z);
        this.color = color;
        this.maxRadius = maxRadius;
        this.thickness = thickness;
        this.lifetime = lifetime;
        this.age = 0;
        this.hasPhysics = false;
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z());

        float lifeRatio = ((float) this.age + partialTicks) / this.lifetime;
        if (lifeRatio > 1.0f)
            lifeRatio = 1.0f;

        float currentRadius = this.maxRadius * lifeRatio;
        float alpha = (1.0f - lifeRatio);

        float r = ((this.color >> 16) & 0xFF) / 255.0F;
        float g = ((this.color >> 8) & 0xFF) / 255.0F;
        float b = (this.color & 0xFF) / 255.0F;

        int segments = 32;
        double angleStep = (Math.PI * 2.0) / segments;

        Quaternionf quaternion = new Quaternionf(camera.rotation());

        for (int i = 0; i < segments; i++) {
            double a0 = i * angleStep;
            double a1 = (i + 1) * angleStep;

            float cos0 = (float) Math.cos(a0);
            float sin0 = (float) Math.sin(a0);
            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);

            float innerR = Math.max(0, currentRadius - thickness);
            float outerR = currentRadius;

            Vector3f v1 = new Vector3f(cos0 * innerR, sin0 * innerR, 0).rotate(quaternion);
            Vector3f v2 = new Vector3f(cos1 * innerR, sin1 * innerR, 0).rotate(quaternion);
            Vector3f v3 = new Vector3f(cos1 * outerR, sin1 * outerR, 0).rotate(quaternion);
            Vector3f v4 = new Vector3f(cos0 * outerR, sin0 * outerR, 0).rotate(quaternion);

            vertexConsumer.vertex(x + v1.x(), y + v1.y(), z + v1.z()).color(r, g, b, alpha).endVertex();
            vertexConsumer.vertex(x + v2.x(), y + v2.y(), z + v2.z()).color(r, g, b, alpha).endVertex();
            vertexConsumer.vertex(x + v3.x(), y + v3.y(), z + v3.z()).color(r, g, b, alpha).endVertex();
            vertexConsumer.vertex(x + v4.x(), y + v4.y(), z + v4.z()).color(r, g, b, alpha).endVertex();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return SPREADING_RING;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
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
