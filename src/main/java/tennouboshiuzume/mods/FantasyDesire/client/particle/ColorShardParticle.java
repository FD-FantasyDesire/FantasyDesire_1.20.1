package tennouboshiuzume.mods.FantasyDesire.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ColorShardParticle extends Particle {
    private final int color;
    private final float scale;

    public ColorShardParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed,
            double zSpeed, int color, float scale) {
        super(level, x, y, z);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.color = color;
        this.scale = scale;
        this.lifetime = 20; // Default lifetime
        this.gravity = 0;
        this.hasPhysics = false;

        float r = ((this.color >> 16) & 0xFF) / 255.0F;
        float g = ((this.color >> 8) & 0xFF) / 255.0F;
        float b = (this.color & 0xFF) / 255.0F;
        this.setColor(r, g, b);
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
            float threshold = this.lifetime * 0.8F;
            if (this.age > threshold) {
                this.alpha = Math.max(0.0F, 1.0F - ((float) (this.age - threshold) / (this.lifetime * 0.2F)));
            }
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 vec3 = camera.getPosition();
        float f = (float) (net.minecraft.util.Mth.lerp(partialTicks, this.xo, this.x) - vec3.x);
        float f1 = (float) (net.minecraft.util.Mth.lerp(partialTicks, this.yo, this.y) - vec3.y);
        float f2 = (float) (net.minecraft.util.Mth.lerp(partialTicks, this.zo, this.z) - vec3.z);
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

        Vector3f[] vertices = new Vector3f[] {
                new Vector3f(0.0F, 0.5F * scale, 0.0F), // Top tip
                new Vector3f(-0.1F * scale, 0.0F, -0.1F * scale), // Base corner 1
                new Vector3f(0.1F * scale, 0.0F, -0.1F * scale), // Base corner 2
                new Vector3f(0.1F * scale, 0.0F, 0.1F * scale), // Base corner 3
                new Vector3f(-0.1F * scale, 0.0F, 0.1F * scale), // Base corner 4
                new Vector3f(0.0F, -0.5F * scale, 0.0F) // Bottom tip
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
        consumer.vertex(v1.x(), v1.y(), v1.z()).color(this.rCol, this.gCol, this.bCol, this.alpha).endVertex();
        consumer.vertex(v2.x(), v2.y(), v2.z()).color(this.rCol, this.gCol, this.bCol, this.alpha).endVertex();
        consumer.vertex(v3.x(), v3.y(), v3.z()).color(this.rCol, this.gCol, this.bCol, this.alpha).endVertex();
        consumer.vertex(v3.x(), v3.y(), v3.z()).color(this.rCol, this.gCol, this.bCol, this.alpha).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return GlowingLineParticle.GLOWING_LINE; // Uses the same RenderType as GlowingLineParticle
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<ColorShardParticleOptions> {
        public Provider() {
        }

        @Override
        public Particle createParticle(ColorShardParticleOptions type, ClientLevel level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            return new ColorShardParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, type.color, type.scale);
        }
    }
}
