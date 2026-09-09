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

    /** BFG 日冕电浆能量球的自定义核心着色器实例 */
    private static ShaderInstance bfgCoronaShader;

    /** BFG 敌人连接桥的自定义核心着色器实例 */
    private static ShaderInstance bfgBridgeShader;
    private static ShaderInstance bladeRiftShader;
    private static ShaderInstance frostFieldShader;
    private static ShaderInstance voidFlameMaskShader;
    private static ShaderInstance voidFlameOutlineShader;

    /** 着色器是否成功加载 */
    private static boolean crossFlashShaderLoaded = false;

    /** BFG 日冕电浆能量球着色器是否成功加载 */
    private static boolean bfgCoronaShaderLoaded = false;

    /** BFG 敌人连接桥着色器是否成功加载 */
    private static boolean bfgBridgeShaderLoaded = false;
    private static boolean bladeRiftShaderLoaded = false;

    /** 寒霜风暴球形能量场着色器是否成功加载 */
    private static boolean frostFieldShaderLoaded = false;
    private static boolean voidFlameMaskShaderLoaded = false;
    private static boolean voidFlameOutlineShaderLoaded = false;

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(FDShaderHandler::onRegisterShaders);
        System.out.println("[FantasyDesire] Registered cross flash shader handler on MOD event bus.");
    }

    public static ShaderInstance getCrossFlashShader() {
        return crossFlashShader;
    }

    public static ShaderInstance getBfgCoronaShader() {
        return bfgCoronaShader;
    }

    public static ShaderInstance getBfgBridgeShader() {
        return bfgBridgeShader;
    }

    public static ShaderInstance getBladeRiftShader() {
        return bladeRiftShader;
    }

    public static ShaderInstance getFrostFieldShader() {
        return frostFieldShader;
    }

    public static ShaderInstance getVoidFlameMaskShader() {
        return voidFlameMaskShader;
    }

    public static ShaderInstance getVoidFlameOutlineShader() {
        return voidFlameOutlineShader;
    }

    public static boolean isCrossFlashShaderLoaded() {
        return crossFlashShaderLoaded && crossFlashShader != null;
    }

    public static boolean isBfgCoronaShaderLoaded() {
        return bfgCoronaShaderLoaded && bfgCoronaShader != null;
    }

    public static boolean isBfgBridgeShaderLoaded() {
        return bfgBridgeShaderLoaded && bfgBridgeShader != null;
    }

    public static boolean isBladeRiftShaderLoaded() {
        return bladeRiftShaderLoaded && bladeRiftShader != null;
    }

    public static boolean isFrostFieldShaderLoaded() {
        return frostFieldShaderLoaded && frostFieldShader != null;
    }

    public static boolean isVoidFlameMaskShaderLoaded() {
        return voidFlameMaskShaderLoaded && voidFlameMaskShader != null;
    }

    public static boolean isVoidFlameOutlineShaderLoaded() {
        return voidFlameOutlineShaderLoaded && voidFlameOutlineShader != null;
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

        try {
            ShaderInstance shader = new ShaderInstance(
                    event.getResourceProvider(),
                    new ResourceLocation(FantasyDesire.MODID, "fd_bfg_corona"),
                    DefaultVertexFormat.POSITION_COLOR_TEX);
            event.registerShader(shader, shaderInstance -> {
                bfgCoronaShader = shaderInstance;
                bfgCoronaShaderLoaded = true;
                System.out.println(
                        "[FantasyDesire] BFG corona plasma shader loaded successfully: fantasydesire:fd_bfg_corona");
            });
        } catch (IOException e) {
            System.err.println("[FantasyDesire] ===== CRITICAL: Failed to register BFG corona plasma shader! =====");
            System.err.println("[FantasyDesire] Error: " + e.getMessage());
            e.printStackTrace();
            bfgCoronaShader = null;
            bfgCoronaShaderLoaded = false;
        }

        try {
            ShaderInstance shader = new ShaderInstance(
                    event.getResourceProvider(),
                    new ResourceLocation(FantasyDesire.MODID, "fd_bfg_bridge"),
                    DefaultVertexFormat.POSITION_COLOR_TEX);
            event.registerShader(shader, shaderInstance -> {
                bfgBridgeShader = shaderInstance;
                bfgBridgeShaderLoaded = true;
                System.out.println(
                        "[FantasyDesire] BFG plasma bridge shader loaded successfully: fantasydesire:fd_bfg_bridge");
            });
        } catch (IOException e) {
            System.err.println("[FantasyDesire] ===== CRITICAL: Failed to register BFG plasma bridge shader! =====");
            System.err.println("[FantasyDesire] Error: " + e.getMessage());
            e.printStackTrace();
            bfgBridgeShader = null;
            bfgBridgeShaderLoaded = false;
        }

        try {
            ShaderInstance shader = new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation(FantasyDesire.MODID, "fd_blade_rift"), DefaultVertexFormat.POSITION_COLOR_TEX);
            event.registerShader(shader, loaded -> {
                bladeRiftShader = loaded;
                bladeRiftShaderLoaded = true;
            });
        } catch (IOException e) {
            System.err.println("[FantasyDesire] Failed to register BladeRift shader: " + e.getMessage());
            bladeRiftShader = null;
            bladeRiftShaderLoaded = false;
        }

        try {
            ShaderInstance shader = new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation(FantasyDesire.MODID, "fd_frost_field"), DefaultVertexFormat.POSITION_TEX);
            event.registerShader(shader, loaded -> {
                frostFieldShader = loaded;
                frostFieldShaderLoaded = true;
                System.out.println(
                        "[FantasyDesire] Frost storm energy field shader loaded successfully: fantasydesire:fd_frost_field");
            });
        } catch (IOException e) {
            System.err.println("[FantasyDesire] Failed to register frost storm energy field shader: " + e.getMessage());
            frostFieldShader = null;
            frostFieldShaderLoaded = false;
        }

        try {
            ShaderInstance shader = new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation(FantasyDesire.MODID, "fd_void_flame_mask"), DefaultVertexFormat.NEW_ENTITY);
            event.registerShader(shader, loaded -> {
                voidFlameMaskShader = loaded;
                voidFlameMaskShaderLoaded = true;
                System.out.println("[FantasyDesire] Void flame silhouette mask shader loaded successfully.");
            });
        } catch (IOException e) {
            System.err.println("[FantasyDesire] Failed to register void flame silhouette mask shader: " + e.getMessage());
            voidFlameMaskShader = null;
            voidFlameMaskShaderLoaded = false;
        }

        try {
            ShaderInstance shader = new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation(FantasyDesire.MODID, "fd_void_flame_outline"), DefaultVertexFormat.POSITION_TEX);
            event.registerShader(shader, loaded -> {
                voidFlameOutlineShader = loaded;
                voidFlameOutlineShaderLoaded = true;
                System.out.println("[FantasyDesire] Void flame outline shader loaded successfully.");
            });
        } catch (IOException e) {
            System.err.println("[FantasyDesire] Failed to register void flame outline shader: " + e.getMessage());
            voidFlameOutlineShader = null;
            voidFlameOutlineShaderLoaded = false;
        }
    }
}
