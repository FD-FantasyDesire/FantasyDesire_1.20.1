package tennouboshiuzume.mods.FantasyDesire.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank.ConcentrationRanks;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Face;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.client.renderer.util.MSAutoCloser;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityEnderSlashEffect;

@OnlyIn(Dist.CLIENT)
public class EnderSlashEffectRender<T extends EntityEnderSlashEffect> extends EntityRenderer<T> {
    private static final ResourceLocation modelLocation = new ResourceLocation("slashblade", "model/util/slash.obj");
    private static final ResourceLocation textureLocation = new ResourceLocation("slashblade", "model/util/slash.png");

    public @NotNull ResourceLocation getTextureLocation(@NotNull T entity) {
        return textureLocation;
    }

    public EnderSlashEffectRender(EntityRendererProvider.Context context) {
        super(context);
    }

    public void render(T entity, float entityYaw, float partialTicks, PoseStack matrixStackIn,
            @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        MSAutoCloser msac = MSAutoCloser.pushMatrix(matrixStackIn);

        try {
            matrixStackIn
                    .mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
            matrixStackIn.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
            matrixStackIn.mulPose(Axis.XP.rotationDegrees(entity.getRotationRoll()));
            WavefrontObject model = BladeModelManager.getInstance().getModel(modelLocation);
            int lifetime = entity.getLifetime();
            float progress = Math.min((float) lifetime, (float) entity.tickCount + partialTicks) / (float) lifetime;
            double deathTime = (double) lifetime;
            double baseAlpha = Math.min(deathTime,
                    (double) Math.max(0.0F, (float) (lifetime - entity.tickCount) - partialTicks)) / deathTime;
            baseAlpha = -Math.pow(baseAlpha - 1.0, 4.0) + 1.0;
            matrixStackIn.mulPose(Axis.YP.rotationDegrees(entity.getRotationOffset() - 135.0F * progress));
            matrixStackIn.scale(1.0F, 0.25F, 1.0F);
            float baseScale = 1.2F * entity.getScale();
            matrixStackIn.scale(baseScale, baseScale, baseScale);
            float yscale = 0.03F;
            float scale = entity.getBaseSize() * Mth.lerp(progress, 0.03F, 0.035F);
            int color = entity.getColor() & 16777215;
            IConcentrationRank.ConcentrationRanks rank = entity.getRankCode();
            if (rank.level < ConcentrationRanks.C.level) {
                color = 5592405;
            }

            ResourceLocation rl = this.getTextureLocation(entity);
            int alpha = (255 & (int) (255.0 * baseAlpha)) << 24;
            MSAutoCloser msacb;
            float windscale;
            if (ConcentrationRanks.S.level <= rank.level) {
                msacb = MSAutoCloser.pushMatrix(matrixStackIn);

                try {
                    windscale = entity.getBaseSize() * Mth.lerp(progress, 0.035F, 0.03F);
                    matrixStackIn.scale(windscale, yscale, windscale);
                    Face.setAlphaOverride(Face.alphaOverrideYZZ);
                    Face.setUvOperator(1.0F, 1.0F, 0.0F, -0.8F + progress * 0.3F);
                    BladeRenderState.setCol(2236962 | alpha);
                    BladeRenderState.renderOverridedColorWrite(ItemStack.EMPTY, model, "base", rl, matrixStackIn,
                            bufferIn, packedLightIn);
                } catch (Throwable var30) {
                    if (msacb != null) {
                        try {
                            msacb.close();
                        } catch (Throwable var29) {
                            var30.addSuppressed(var29);
                        }
                    }

                    throw var30;
                }

                if (msacb != null) {
                    msacb.close();
                }
            }

            if (ConcentrationRanks.D.level <= rank.level) {
                msacb = MSAutoCloser.pushMatrix(matrixStackIn);

                try {
                    matrixStackIn.scale(scale, yscale, scale);
                    Face.setAlphaOverride(Face.alphaOverrideYZZ);
                    Face.setUvOperator(1.0F, 1.0F, 0.0F, -0.35F + progress * -0.15F);
                    BladeRenderState.setCol(color | alpha);
                    BladeRenderState.renderOverridedColorWrite(ItemStack.EMPTY, model, "base", rl, matrixStackIn,
                            bufferIn, packedLightIn);
                } catch (Throwable var33) {
                    if (msacb != null) {
                        try {
                            msacb.close();
                        } catch (Throwable var28) {
                            var33.addSuppressed(var28);
                        }
                    }

                    throw var33;
                }

                if (msacb != null) {
                    msacb.close();
                }
            }

            if (ConcentrationRanks.B.level <= rank.level) {
                msacb = MSAutoCloser.pushMatrix(matrixStackIn);

                try {
                    windscale = entity.getBaseSize() * Mth.lerp(progress, 0.03F, 0.0375F);
                    matrixStackIn.scale(windscale, yscale, windscale);
                    Face.setAlphaOverride(Face.alphaOverrideYZZ);
                    Face.setUvOperator(1.0F, 1.0F, 0.0F, -0.5F + progress * -0.2F);
                    BladeRenderState.setCol(4210752 | alpha);
                    BladeRenderState.renderOverridedLuminous(ItemStack.EMPTY, model, "base", rl, matrixStackIn,
                            bufferIn, packedLightIn);
                } catch (Throwable var31) {
                    if (msacb != null) {
                        try {
                            msacb.close();
                        } catch (Throwable var26) {
                            var31.addSuppressed(var26);
                        }
                    }

                    throw var31;
                }

                if (msacb != null) {
                    msacb.close();
                }
            }

            msacb = MSAutoCloser.pushMatrix(matrixStackIn);

            try {
                matrixStackIn.scale(scale, yscale, scale);
                Face.setAlphaOverride(Face.alphaOverrideYZZ);
                Face.setUvOperator(1.0F, 1.0F, 0.0F, -0.35F + progress * -0.15F);
                BladeRenderState.setCol(color | alpha);
                BladeRenderState.renderOverridedLuminous(ItemStack.EMPTY, model, "base", rl, matrixStackIn, bufferIn,
                        packedLightIn);
            } catch (Throwable var32) {
                if (msacb != null) {
                    try {
                        msacb.close();
                    } catch (Throwable var27) {
                        var32.addSuppressed(var27);
                    }
                }

                throw var32;
            }

            if (msacb != null) {
                msacb.close();
            }
        } catch (Throwable var34) {
            if (msac != null) {
                try {
                    msac.close();
                } catch (Throwable var25) {
                    var34.addSuppressed(var25);
                }
            }

            throw var34;
        }

        if (msac != null) {
            msac.close();
        }

    }
}
