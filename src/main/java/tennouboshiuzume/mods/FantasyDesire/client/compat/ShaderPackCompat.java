package tennouboshiuzume.mods.FantasyDesire.client.compat;

import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/** 仅通过公开 API 探测光影，避免将 Oculus/Iris 变成必需依赖。 */
public final class ShaderPackCompat {
    private static boolean resolved;
    private static Object api;
    private static Method shaderPackInUse;
    private static Method renderingShadowPass;
    private static boolean failureLogged;

    private ShaderPackCompat() {
    }

    public static boolean isShaderPackInUse() {
        resolve();
        return query(shaderPackInUse);
    }

    public static boolean isRenderingShadowPass() {
        resolve();
        return query(renderingShadowPass);
    }

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        if (!ModList.get().isLoaded("oculus") && !ModList.get().isLoaded("iris")) return;
        for (String name : new String[]{"net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.api.v0.IrisApi"}) {
            try {
                Class<?> type = Class.forName(name);
                Object instance = type.getMethod("getInstance").invoke(null);
                Method inUse = type.getMethod("isShaderPackInUse");
                Method shadowPass = type.getMethod("isRenderingShadowPass");
                api = instance;
                shaderPackInUse = inUse;
                renderingShadowPass = shadowPass;
                return;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // 新旧包名逐个探测；两者都失败时保留原版路径并给出一次诊断。
            }
        }
        logFailure("Iris API was not found; custom effects will use the vanilla rendering path.");
    }

    private static boolean query(Method method) {
        if (api == null || method == null) return false;
        try {
            return Boolean.TRUE.equals(method.invoke(api));
        } catch (ReflectiveOperationException | LinkageError e) {
            logFailure("Iris API query failed: " + e.getClass().getSimpleName());
            return false;
        }
    }

    private static void logFailure(String message) {
        if (!failureLogged) {
            failureLogged = true;
            System.err.println("[FantasyDesire] " + message);
        }
    }
}
