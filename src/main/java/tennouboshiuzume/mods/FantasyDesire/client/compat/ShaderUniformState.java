package tennouboshiuzume.mods.FantasyDesire.client.compat;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.ShaderInstance;
import tennouboshiuzume.mods.FantasyDesire.mixin.client.ShaderInstanceAccessor;

import java.util.HashMap;
import java.util.Map;

/** 批次提交时复制数据，避免同一 shader 的下一次 set 覆盖已排队效果。 */
final class ShaderUniformState {
    private final Map<String, float[]> floats = new HashMap<>();
    private final Map<String, int[]> integers = new HashMap<>();
    private final Map<String, Object> samplers;

    ShaderUniformState(ShaderInstance shader) {
        ShaderInstanceAccessor access = (ShaderInstanceAccessor) shader;
        samplers = new HashMap<>(access.fantasydesire$getSamplerMap());
        access.fantasydesire$getUniformMap().forEach((name, uniform) -> {
            if (uniform.getType() <= 3) {
                int[] values = new int[uniform.getCount()];
                for (int i = 0; i < values.length; i++) values[i] = uniform.getIntBuffer().get(i);
                integers.put(name, values);
            } else {
                float[] values = new float[uniform.getCount()];
                for (int i = 0; i < values.length; i++) values[i] = uniform.getFloatBuffer().get(i);
                floats.put(name, values);
            }
        });
    }

    void apply(ShaderInstance shader) {
        floats.forEach((name, values) -> {
            Uniform uniform = shader.getUniform(name);
            if (uniform != null) uniform.set(values);
        });
        integers.forEach((name, values) -> {
            Uniform uniform = shader.getUniform(name);
            if (uniform == null) return;
            switch (values.length) {
                case 1 -> uniform.set(values[0]);
                case 2 -> uniform.set(values[0], values[1]);
                case 3 -> uniform.set(values[0], values[1], values[2]);
                case 4 -> uniform.set(values[0], values[1], values[2], values[3]);
                default -> throw new IllegalStateException("Unsupported integer uniform size: " + values.length);
            }
        });
        Map<String, Object> target = ((ShaderInstanceAccessor) shader).fantasydesire$getSamplerMap();
        target.clear();
        target.putAll(samplers);
        shader.markDirty();
    }
}
