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
import tennouboshiuzume.mods.FantasyDesire.particle.AstraStarParticleOptions;

/** 匀速运动的单颗星芒；独立批次和 shader，不修改星座闪电的渲染路径。 */
public final class AstraStarParticle extends Particle {
    public static final ParticleRenderType ASTRA_STAR = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder buffer, TextureManager textures) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            // 每帧重新获取 shader，资源重载期间由 render 的加载检查跳过提交。
            RenderSystem.setShader(FDShaderHandler.isAstraStarShaderLoaded()
                    ? FDShaderHandler::getAstraStarShader : GameRenderer::getPositionColorTexShader);
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
            return "fantasydesire:astra_star";
        }
    };

    private final AstraStarParticleOptions options;
    private final double boundsPadding;
    private Vec3 direction = Vec3.ZERO;
    private double travelled, stepLength;

    public AstraStarParticle(ClientLevel level, double x, double y, double z,
            double vx, double vy, double vz, AstraStarParticleOptions options) {
        super(level, x, y, z);
        this.options = options;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.lifetime = options.lifetime;
        this.hasPhysics = false;
        this.rCol = ((options.color >> 16) & 255) / 255f;
        this.gCol = ((options.color >> 8) & 255) / 255f;
        this.bCol = (options.color & 255) / 255f;
        this.alpha = options.alpha;
        // 覆盖旋转后的光刺、中心光晕，以及最宽的拖尾边缘。
        this.boundsPadding = Math.sqrt(2) * Math.max(options.radius, options.width * 4.0);
        updateBounds();
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        double nextX = x + xd, nextY = y + yd, nextZ = z + zd;
        if (!validPosition(nextX, nextY, nextZ)) {
            remove();
            return;
        }
        // 直接使用原版传入的速度（格/tick），不加入随机速度、重力、阻力或碰撞。
        setPos(nextX, nextY, nextZ);
        Vec3 movement = new Vec3(x - xo, y - yo, z - zo);
        stepLength = movement.length();
        direction = stepLength > 1.0E-8 ? movement.scale(1.0 / stepLength) : Vec3.ZERO;
        travelled += stepLength;
        updateBounds();
    }

    private void updateBounds() {
        Vec3 current = new Vec3(x, y, z);
        Vec3 previous = new Vec3(xo, yo, zo);
        Vec3 trail = direction.scale(Math.min(travelled, options.trailLength));
        // 同时包住前后 tick 的头尾，避免插值期间拖尾跨出视锥包围盒。
        setBoundingBox(new AABB(current, previous).minmax(new AABB(current.subtract(trail), previous.subtract(trail)))
                .inflate(boundsPadding));
    }

    static boolean validPosition(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Math.abs(x) <= 3.0E7 && Math.abs(y) <= 3.0E7 && Math.abs(z) <= 3.0E7;
    }

    @Override
    public void render(VertexConsumer out, Camera camera, float partialTick) {
        if (!isAlive() || !FDShaderHandler.isAstraStarShaderLoaded()) {
            return;
        }
        float partial = Mth.clamp(partialTick, 0f, 1f);
        // 与位置插值对齐，时间单位为 tick；最后 30% 生命周期平滑收光，1 tick 粒子也能显示。
        float ticks = age == 0 ? 0f : age - 1 + partial;
        float fade = Mth.clamp((ticks / lifetime - 0.7f) / 0.3f, 0f, 1f);
        float opacity = alpha * (1f - fade * fade * (3f - 2f * fade));
        if (opacity <= 0f || options.color == 0) {
            return;
        }
        Vec3 center = new Vec3(Mth.lerp(partial, xo, x), Mth.lerp(partial, yo, y), Mth.lerp(partial, zo, z))
                .subtract(camera.getPosition());
        Vec3 cameraRight = new Vec3(new Vector3f(1, 0, 0).rotate(camera.rotation()));
        Vec3 cameraUp = new Vec3(new Vector3f(0, 1, 0).rotate(camera.rotation()));
        double trailLength = Math.min(options.trailLength, Math.max(0, travelled - stepLength * (1 - partial)));
        if (stepLength > 1.0E-8 && trailLength > 1.0E-8) {
            renderTrail(out, center, cameraRight, cameraUp, trailLength, opacity);
        }

        double angle = Math.toRadians(options.rotation);
        Vec3 right = cameraRight.scale(Math.cos(angle)).add(cameraUp.scale(Math.sin(angle)));
        Vec3 up = cameraUp.scale(Math.cos(angle)).subtract(cameraRight.scale(Math.sin(angle)));
        // 将原星芒拆成两组主光刺、两组弱对角光刺与中心光晕，尺寸通过网格传入，无逐粒子 uniform。
        quad(out, center, right.scale(options.radius * 0.84), up.scale(options.width * 4.0), 3, opacity);
        quad(out, center, up.scale(options.radius), right.scale(options.width * 4.0 * 0.84), 3, opacity);
        double diagonalScale = 1.0 / (1.35 * Math.sqrt(2));
        Vec3 diagonalRight = right.add(up).scale(1.0 / Math.sqrt(2));
        Vec3 diagonalUp = up.subtract(right).scale(1.0 / Math.sqrt(2));
        quad(out, center, diagonalRight.scale(options.radius * diagonalScale),
                diagonalUp.scale(options.width * 4.0 * diagonalScale), 9, opacity);
        quad(out, center, diagonalUp.scale(options.radius * diagonalScale),
                diagonalRight.scale(options.width * 4.0 * diagonalScale), 9, opacity);
        quad(out, center, right.scale(options.radius), up.scale(options.radius), 6, opacity);
    }

    private void renderTrail(VertexConsumer out, Vec3 head, Vec3 cameraRight, Vec3 cameraUp,
            double length, float opacity) {
        Vec3 tail = head.subtract(direction.scale(length));
        Vec3 side = direction.cross(head.add(tail).scale(-0.5));
        // 沿视线运动时仍保持合法侧向量；此时拖尾在画面上自然缩短。
        if (side.lengthSqr() < 1.0E-10) {
            side = direction.cross(cameraUp);
        }
        if (side.lengthSqr() < 1.0E-10) {
            side = direction.cross(cameraRight);
        }
        side = side.normalize().scale(options.width * 4.0);
        // 根部在星核覆盖范围内渐入；以世界长度定义过渡，长尾不会拉长接缝，极短尾自然减弱。
        // UV.y 的符号表示横向坐标，绝对值携带过渡长度占当前拖尾长度的比例。
        float joinFraction = (float) (Math.max(options.width * 1.5, options.radius * 0.08) / length);
        // 方片保持矩形，在片元阶段收尖，避免梯形 UV 插值沿三角剖分出现亮暗接缝。
        vertex(out, head.add(side), 0, joinFraction, opacity);
        vertex(out, head.subtract(side), 0, -joinFraction, opacity);
        vertex(out, tail.subtract(side), 1, -joinFraction, opacity);
        vertex(out, tail.add(side), 1, joinFraction, opacity);
    }

    private void quad(VertexConsumer out, Vec3 center, Vec3 right, Vec3 up, float shape, float opacity) {
        vertex(out, center.subtract(right).add(up), shape - 1, 1, opacity);
        vertex(out, center.subtract(right).subtract(up), shape - 1, -1, opacity);
        vertex(out, center.add(right).subtract(up), shape + 1, -1, opacity);
        vertex(out, center.add(right).add(up), shape + 1, 1, opacity);
    }

    private void vertex(VertexConsumer out, Vec3 point, float u, float v, float opacity) {
        out.vertex(point.x, point.y, point.z).color(rCol, gCol, bCol, opacity).uv(u, v).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ASTRA_STAR;
    }
}
