package tennouboshiuzume.mods.FantasyDesire.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDAttributes;

import java.util.ArrayList;
import java.util.List;

/** 在本体刀身的模型坐标系中，按持有者的望属性追加金色光环。 */
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BladeMangRingRenderer {
    private static final int SEGMENTS = 64;
    // Chikeflare 与本体常规刀身沿 OBJ 的负 X 轴延伸，光环应位于局部 YZ 平面。
    // 半径、环宽和轴向距离均使用模型单位，顶点直接乘当前刀身 hardpointA 的动画矩阵。
    private static final float RADIUS = 16.0F;
    private static final float CORE_HALF_WIDTH = 0.65F;
    private static final float GLOW_HALF_WIDTH = 3.5F;
    private static final float[] COS = new float[SEGMENTS + 1];
    private static final float[] SIN = new float[SEGMENTS + 1];
    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    static {
        for (int i = 0; i < SEGMENTS; i++) {
            double angle = Math.PI * 2.0D * i / SEGMENTS;
            COS[i] = (float) Math.cos(angle);
            SIN[i] = (float) Math.sin(angle);
        }
        COS[SEGMENTS] = COS[0];
        SIN[SEGMENTS] = SIN[0];
    }

    private BladeMangRingRenderer() {
    }

    /** 仅在实际刀身渲染期间提供持有者；嵌套调用和异常都恢复上层上下文。 */
    public static void withContext(LivingEntity holder, ItemStack stack, Runnable render) {
        Context previous = CONTEXT.get();
        Context current = holder instanceof Player player ? new Context(player, stack, new ArrayList<>()) : null;
        try {
            CONTEXT.set(current);
            render.run();
            // 分件事件发生在本体绘制之前；等刀身、刀鞘和发光 pass 都提交后再追加光环。
            if (current != null) {
                for (RingDraw draw : current.pending()) {
                    draw.render();
                }
            }
        } finally {
            if (previous == null) {
                CONTEXT.remove();
            } else {
                CONTEXT.set(previous);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderBlade(RenderOverrideEvent event) {
        Context context = CONTEXT.get();
        if (context == null || event.getStack() != context.stack()) {
            return;
        }
        // 只追加到刀身基础 pass，发光层、刀鞘、蓄力效果和物品图标均不重复绘制。
        String part = event.getOriginalTarget();
        if (!"blade".equals(part) && !"blade_damaged".equals(part)) {
            return;
        }
        var attribute = context.player().getAttribute(FDAttributes.FD_LC_MANG.get());
        int count = attribute == null ? 0 : ringCount(attribute.getValue());
        if (count == 0) {
            return;
        }

        // 本体随后会 popPose 并复用矩阵；保存刀身动画矩阵副本，延迟绘制时仍使用此姿态。
        context.pending().add(new RingDraw(new Matrix4f(event.getPoseStack().last().pose()), event.getBuffer(),
                count, BladeMangRingProfiles.forModel(event.getModel()), event));
    }

    static int ringCount(double mang) {
        // 每完整一点对应一环；额外防御属性上限被其他模组放宽或出现非有限值。
        return Double.isFinite(mang) && mang >= 1.0D
                ? (int) Math.min(mang, FDAttributes.MAX_LC_POINTS) : 0;
    }

    private static void band(VertexConsumer vertices, Matrix4f pose, float x, float inner, float outer,
            float innerAlpha, float outerAlpha, float red, float green, float blue) {
        for (int i = 0; i < SEGMENTS; i++) {
            vertex(vertices, pose, i, inner, x, red, green, blue, innerAlpha);
            vertex(vertices, pose, i + 1, inner, x, red, green, blue, innerAlpha);
            vertex(vertices, pose, i + 1, outer, x, red, green, blue, outerAlpha);
            vertex(vertices, pose, i, outer, x, red, green, blue, outerAlpha);
        }
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose, int index, float radius, float x,
            float red, float green, float blue, float alpha) {
        vertices.vertex(pose, x, COS[index] * radius, SIN[index] * radius)
                .color(red, green, blue, alpha).endVertex();
    }

    private record Context(Player player, ItemStack stack, List<RingDraw> pending) {
    }

    private record RingDraw(Matrix4f pose, MultiBufferSource buffer, int count, BladeMangRingProfiles.Profile profile,
            RenderOverrideEvent event) {
        void render() {
            // 同优先级的后续监听器仍可能取消本次刀身绘制。
            if (event.isCanceled()) {
                return;
            }
            if (buffer instanceof MultiBufferSource.BufferSource source) {
                // 包括独立缓冲中的附魔光效，保证刀的颜色和深度均已完成提交。
                source.endBatch();
            }
            // 亮芯写深度：之后绘制的模型不能覆盖位于前方的实体环带。
            VertexConsumer core = buffer.getBuffer(RingRenderState.CORE);
            renderRings(core, pose, count, profile, true);
            // 柔光不写深度，透明边缘不会遮挡刀身或其他光环。
            VertexConsumer glow = buffer.getBuffer(RingRenderState.GLOW);
            renderRings(glow, pose, count, profile, false);
            if (buffer instanceof MultiBufferSource.BufferSource source) {
                source.endBatch(RingRenderState.GLOW);
            }
        }
    }

    static void renderRings(VertexConsumer vertices, Matrix4f pose, int count, BladeMangRingProfiles.Profile profile,
            boolean core) {
        for (int i = 0; i < count; i++) {
            float progress = profile.progress(i, count);
            float x = profile.xAt(progress);
            float scale = profile.scaleAt(progress);
            float radius = RADIUS * scale;
            float coreWidth = CORE_HALF_WIDTH * scale;
            float glowWidth = GLOW_HALF_WIDTH * scale;
            if (core) {
                band(vertices, pose, x, radius - coreWidth, radius + coreWidth,
                        0.95F, 0.95F, 1.0F, 0.84F, 0.30F);
            } else {
                band(vertices, pose, x, radius - glowWidth, radius - coreWidth,
                        0.0F, 0.55F, 1.0F, 0.65F, 0.08F);
                band(vertices, pose, x, radius + coreWidth, radius + glowWidth,
                        0.55F, 0.0F, 1.0F, 0.65F, 0.08F);
            }
        }
    }

    private static final class RingRenderState extends RenderStateShard {
        private static final RenderType CORE = create("fd_mang_ring_core", true);
        private static final RenderType GLOW = create("fd_mang_ring_glow", false);

        private static RenderType create(String name, boolean depthWrite) {
            return RenderType.create(name, DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
                    4096, false, true, RenderType.CompositeState.builder()
                            .setShaderState(POSITION_COLOR_SHADER)
                            // 与原版 position_color.json 的混合声明一致，避免 shader.apply 覆盖混合函数。
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(depthWrite ? COLOR_DEPTH_WRITE : COLOR_WRITE)
                            .setOutputState(ITEM_ENTITY_TARGET)
                            .createCompositeState(false));
        }

        private RingRenderState() {
            super("fd_mang_ring", () -> { }, () -> { });
        }
    }
}
