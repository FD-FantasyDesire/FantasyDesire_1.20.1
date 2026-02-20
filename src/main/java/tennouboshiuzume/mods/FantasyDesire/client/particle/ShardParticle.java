package tennouboshiuzume.mods.FantasyDesire.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ShardParticle extends Particle {
    private final SpriteSet sprites;
    private static final ResourceLocation END_SKY_LOCATION = new ResourceLocation("textures/environment/end_sky.png");
    private static final ResourceLocation END_PORTAL_LOCATION = new ResourceLocation("textures/entity/end_portal.png");

    public ShardParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
            SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.lifetime = 20; // Default lifetime, can be adjusted
        this.gravity = 0; // No gravity initially
        this.hasPhysics = false; // No physics initially
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 vec3 = camera.getPosition();
        float f = (float) (this.x - vec3.x);
        float f1 = (float) (this.y - vec3.y);
        float f2 = (float) (this.z - vec3.z);
        Quaternionf quaternionf = new Quaternionf();
        // Calculate rotation based on movement direction
        if (this.xd != 0 || this.yd != 0 || this.zd != 0) {
            Vector3f direction = new Vector3f((float) this.xd, (float) this.yd, (float) this.zd);
            direction.normalize();
            // Create a rotation that aligns +Y with the direction vector
            quaternionf.rotationTo(new Vector3f(0, 1, 0), direction);
        } else {
            quaternionf = camera.rotation();
        }

        // Define the vertices for a sharp 3D prism/shard
        // This is a simplified representation of a "sharp prism"
        // Adjust vertices as needed for the desired "two-segment sharp stereoscopic
        // prism fragment" shape

        Vector3f[] vertices = new Vector3f[] {
                new Vector3f(0.0F, 0.5F, 0.0F), // Top tip
                new Vector3f(-0.1F, 0.0F, -0.1F), // Base corner 1
                new Vector3f(0.1F, 0.0F, -0.1F), // Base corner 2
                new Vector3f(0.1F, 0.0F, 0.1F), // Base corner 3
                new Vector3f(-0.1F, 0.0F, 0.1F), // Base corner 4
                new Vector3f(0.0F, -0.5F, 0.0F) // Bottom tip
        };

        // Transform vertices
        for (Vector3f vertex : vertices) {
            vertex.rotate(quaternionf);
            vertex.add(f, f1, f2);
        }

        // Front Face (Top)
        this.renderTriangle(consumer, vertices[0], vertices[1], vertices[2]);
        this.renderTriangle(consumer, vertices[0], vertices[2], vertices[3]);
        this.renderTriangle(consumer, vertices[0], vertices[3], vertices[4]);
        this.renderTriangle(consumer, vertices[0], vertices[4], vertices[1]);

        // Bottom Face (Bottom)
        this.renderTriangle(consumer, vertices[5], vertices[2], vertices[1]);
        this.renderTriangle(consumer, vertices[5], vertices[3], vertices[2]);
        this.renderTriangle(consumer, vertices[5], vertices[4], vertices[3]);
        this.renderTriangle(consumer, vertices[5], vertices[1], vertices[4]);

    }

    private void renderTriangle(VertexConsumer consumer, Vector3f v1, Vector3f v2, Vector3f v3) {
        // Simple white color, full alpha for now
        consumer.vertex(v1.x(), v1.y(), v1.z()).color(255, 255, 255, 255).endVertex();
        consumer.vertex(v2.x(), v2.y(), v2.z()).color(255, 255, 255, 255).endVertex();
        consumer.vertex(v3.x(), v3.y(), v3.z()).color(255, 255, 255, 255).endVertex();
        consumer.vertex(v3.x(), v3.y(), v3.z()).color(255, 255, 255, 255).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return RENDER_TYPE;
    }

    public static final ParticleRenderType RENDER_TYPE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder buffer, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getRendertypeEndPortalShader);
            RenderSystem.setShaderTexture(0, END_SKY_LOCATION);
            RenderSystem.setShaderTexture(1, END_PORTAL_LOCATION);
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
        }

        @Override
        public String toString() {
            return "fantasydesire:shard";
        }
    };

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            return new ShardParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
