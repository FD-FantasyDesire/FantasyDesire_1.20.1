package tennouboshiuzume.mods.FantasyDesire.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tennouboshiuzume.mods.FantasyDesire.client.compat.FDShaderCompat;

@Mixin(GameRenderer.class)
public abstract class GameRendererShaderCompatMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void fantasydesire$beginShaderFrame(CallbackInfo ci) {
        FDShaderCompat.beginFrame();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void fantasydesire$finishShaderFrame(CallbackInfo ci) {
        FDShaderCompat.finishFrame();
    }
}
