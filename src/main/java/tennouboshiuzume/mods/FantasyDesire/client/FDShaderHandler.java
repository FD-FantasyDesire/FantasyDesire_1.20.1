package tennouboshiuzume.mods.FantasyDesire.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;

import java.io.IOException;

/**
 * 自定义核心着色器处理器
 * 注册和管理模组自定义的 GLSL 着色器程序（.vsh / .fsh / .json）
 */
@OnlyIn(Dist.CLIENT)
public class FDShaderHandler {

    /** 十字星型闪光的自定义核心着色器实例 */
    private static ShaderInstance crossFlashShader;

    /** 着色器是否成功加载 */
    private static boolean crossFlashShaderLoaded = false;

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(FDShaderHandler::onRegisterShaders);
        System.out.println("[FantasyDesire] Registered cross flash shader handler on MOD event bus.");
    }

    public static ShaderInstance getCrossFlashShader() {
        return crossFlashShader;
    }

    public static boolean isCrossFlashShaderLoaded() {
        return crossFlashShaderLoaded && crossFlashShader != null;
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            ShaderInstance shader = new ShaderInstance(
                    event.getResourceProvider(),
                    new ResourceLocation(FantasyDesire.MODID, "fd_cross_flash"),
                    DefaultVertexFormat.POSITION_COLOR_TEX);
            event.registerShader(shader, shaderInstance -> {
                crossFlashShader = shaderInstance;
                crossFlashShaderLoaded = true;
                System.out.println(
                        "[FantasyDesire] Cross flash shader loaded successfully: fantasydesire:fd_cross_flash");
            });
        } catch (IOException e) {
            System.err.println("[FantasyDesire] ===== CRITICAL: Failed to register cross flash shader! =====");
            System.err.println("[FantasyDesire] Error: " + e.getMessage());
            e.printStackTrace();
            crossFlashShader = null;
            crossFlashShaderLoaded = false;
        }
    }
}
