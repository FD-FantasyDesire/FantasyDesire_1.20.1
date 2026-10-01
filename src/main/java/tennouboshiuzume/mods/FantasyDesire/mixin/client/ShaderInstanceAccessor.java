package tennouboshiuzume.mods.FantasyDesire.mixin.client;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ShaderInstance.class)
public interface ShaderInstanceAccessor {
    @Accessor("uniformMap")
    Map<String, Uniform> fantasydesire$getUniformMap();

    @Accessor("samplerMap")
    Map<String, Object> fantasydesire$getSamplerMap();
}
