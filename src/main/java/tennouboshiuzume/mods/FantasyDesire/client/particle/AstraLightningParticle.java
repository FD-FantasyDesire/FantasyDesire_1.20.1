package tennouboshiuzume.mods.FantasyDesire.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import tennouboshiuzume.mods.FantasyDesire.client.FDShaderHandler;
import tennouboshiuzume.mods.FantasyDesire.particle.AstraLightningParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.particle.AstraLightningPath;

/** 一个粒子负责整条星座；所有实例共用渲染批次，颜色和生命期亮度由顶点传递。 */
public final class AstraLightningParticle extends Particle {
    public static final ParticleRenderType ASTRA_LIGHTNING = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder buffer, TextureManager textures) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            // 每帧重新取得实例，兼容 F3+T；尚未加载时 render 不会提交顶点。
            RenderSystem.setShader(FDShaderHandler.isAstraLightningShaderLoaded()
                    ? FDShaderHandler::getAstraLightningShader : GameRenderer::getPositionColorTexShader);
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
            return "fantasydesire:astra_lightning";
        }
    };

    private final AstraLightningParticleOptions options;
    private final Vec3[] points;
    private final float[] starScales, phases, pathProgress;
    private final double beamHalfWidth, starSize;

    public AstraLightningParticle(ClientLevel level, AstraLightningParticleOptions options) {
        super(level, options.start.x, options.start.y, options.start.z);
        this.options = options;
        this.points = AstraLightningPath.create(options);
        this.lifetime = options.lifetime;
        this.hasPhysics = false;
        this.rCol = ((options.color >> 16) & 255) / 255f;
        this.gCol = ((options.color >> 8) & 255) / 255f;
        this.bCol = (options.color & 255) / 255f;
        this.beamHalfWidth = options.thickness * 4.0;
        this.starSize = Math.max(options.starRadius, options.thickness * 2.0);
        this.starScales = new float[points.length];
        this.phases = new float[points.length];
        this.pathProgress = new float[points.length];
        java.util.Random random = new java.util.Random(options.seed ^ 0x6A09E667F3BCC909L);
        AABB bounds = new AABB(options.start, options.end);
        double totalLength = 0;
        for (int i = 0; i < points.length; i++) {
            boolean endpoint = i == 0 || i == points.length - 1;
            starScales[i] = endpoint ? 1.25f : 0.85f + random.nextFloat() * 0.25f;
            phases[i] = random.nextFloat() * Mth.TWO_PI;
            Vec3 world = options.start.add(points[i]);
            bounds = bounds.minmax(new AABB(world, world));
            if (i > 0) {
                totalLength += points[i].distanceTo(points[i - 1]);
            }
            pathProgress[i] = (float) totalLength;
        }
        for (int i = 0; i < points.length; i++) {
            pathProgress[i] /= (float) Math.max(totalLength, 1.0E-6);
        }
        // 星芒方片的对角线也必须在裁剪盒内，覆盖闪烁时的最大尺寸。
        setBoundingBox(bounds.inflate(Math.max(beamHalfWidth, starSize * 4.0)));
    }

    @Override
    public void render(VertexConsumer out, Camera camera, float partialTick) {
        if (!FDShaderHandler.isAstraLightningShaderLoaded()) {
            return;
        }
        // 时间为本粒子的 tick，不依赖周期归一化的 GameTime。
        float ticks = age + partialTick;
        float progress = Mth.clamp(ticks / lifetime, 0f, 1f);
        float envelope = smooth(0f, Math.min(1.5f, lifetime * 0.12f), ticks);
        if (options.fade) {
            envelope *= 1f - smooth(0.22f, 1f, progress);
        } else {
            envelope *= 1f - smooth(Math.max(0f, lifetime - 2f), lifetime, ticks);
        }
        float opacity = options.alpha * envelope;
        if (opacity <= 0f) {
            return;
        }
        Vec3 origin = options.start.subtract(camera.getPosition());
        Vec3 cameraRight = new Vec3(new Vector3f(1, 0, 0).rotate(camera.rotation()));
        Vec3 cameraUp = new Vec3(new Vector3f(0, 1, 0).rotate(camera.rotation()));
        for (int i = 1; i < points.length; i++) {
            Vec3 a = origin.add(points[i - 1]);
            Vec3 b = origin.add(points[i]);
            Vec3 direction = b.subtract(a).normalize();
            Vec3 side = direction.cross(a.add(b).scale(-0.5));
            if (side.lengthSqr() < 1.0E-10) {
                side = direction.cross(cameraUp);
            }
            if (side.lengthSqr() < 1.0E-10) {
                side = direction.cross(cameraRight);
            }
            side = side.normalize().scale(beamHalfWidth);
            float head = opacity * pulse(pathProgress[i - 1], ticks);
            float tail = opacity * pulse(pathProgress[i], ticks);
            vertex(out, a.add(side), 0, 1, head);
            vertex(out, a.subtract(side), 0, -1, head);
            vertex(out, b.subtract(side), 1, -1, tail);
            vertex(out, b.add(side), 1, 1, tail);
        }
        for (int i = 0; i < points.length; i++) {
            float twinkle = 0.91f + 0.09f * Mth.sin(ticks * 0.65f + phases[i]);
            double size = starSize * 1.8 * starScales[i] * (0.94 + 0.06 * twinkle);
            Vec3 right = cameraRight.scale(size);
            Vec3 up = cameraUp.scale(size);
            Vec3 center = origin.add(points[i]);
            float brightness = opacity * twinkle * pulse(pathProgress[i], ticks);
            // 星芒 UV.x 使用 [2,4]，与线段 [0,1] 分离，避免逐实例 uniform 和渲染类型。
            vertex(out, center.subtract(right).add(up), 2, 1, brightness);
            vertex(out, center.subtract(right).subtract(up), 2, -1, brightness);
            vertex(out, center.add(right).subtract(up), 4, -1, brightness);
            vertex(out, center.add(right).add(up), 4, 1, brightness);
        }
    }

    private static float pulse(float distance, float ticks) {
        float wave = (distance - ticks * 0.12f) * 5f;
        float initialFlash = (float) Math.exp(-Math.pow((ticks - 2.0) / 1.3, 2));
        return Math.min(1f, 0.72f + 0.20f * (float) Math.exp(-wave * wave) + 0.08f * initialFlash);
    }

    private static float smooth(float from, float to, float value) {
        float t = Mth.clamp((value - from) / Math.max(1.0E-6f, to - from), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private void vertex(VertexConsumer out, Vec3 point, float u, float v, float opacity) {
        out.vertex(point.x, point.y, point.z).color(rCol, gCol, bCol, opacity).uv(u, v).endVertex();
    }

    @Override
    public void tick() {
        if (++age >= lifetime) {
            remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ASTRA_LIGHTNING;
    }
}
