package tennouboshiuzume.mods.FantasyDesire.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tennouboshiuzume.mods.FantasyDesire.client.compat.FDShaderCompat;

/** 在渲染线程接管已登记的特效；原版和其他模组的批次仍按原流程提交。 */
@Mixin(BufferUploader.class)
public abstract class BufferUploaderMixin {
    @Inject(method = "_drawWithShader", at = @At("HEAD"), cancellable = true)
    private static void fantasydesire$deferWorldEffect(BufferBuilder.RenderedBuffer buffer, CallbackInfo ci) {
        if (FDShaderCompat.capture(buffer)) ci.cancel();
    }
}
