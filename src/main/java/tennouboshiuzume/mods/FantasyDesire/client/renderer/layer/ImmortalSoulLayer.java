package tennouboshiuzume.mods.FantasyDesire.client.renderer.layer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;

import java.io.IOException;

public class ImmortalSoulLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static ResourceLocation POWER_LOCATION;

    public ImmortalSoulLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    private static ResourceLocation getPowerLocation() {
        if (POWER_LOCATION == null) {
            try {
                ResourceManager manager = Minecraft.getInstance().getResourceManager();
                Resource original = manager
                        .getResourceOrThrow(new ResourceLocation("textures/entity/creeper/creeper_armor.png"));
                NativeImage image = NativeImage.read(original.open());

                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int color = image.getPixelRGBA(x, y);
                        // ABGR format on Little Endian
                        int a = (color >> 24) & 0xFF;
                        int b = (color >> 16) & 0xFF;
                        int g = (color >> 8) & 0xFF;
                        int r = (color) & 0xFF;

                        // Greyscale luminance
                        int grey = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                        // Reconstruct with Grey RGB
                        int newColor = (a << 24) | (grey << 16) | (grey << 8) | grey;

                        image.setPixelRGBA(x, y, newColor);
                    }
                }

                DynamicTexture dynamicTexture = new DynamicTexture(image);
                POWER_LOCATION = Minecraft.getInstance().getTextureManager().register("dynamic_immortal_soul_armor",
                        dynamicTexture);
            } catch (IOException e) {
                e.printStackTrace();
                POWER_LOCATION = new ResourceLocation("textures/entity/creeper/creeper_armor.png");
            }
        }
        return POWER_LOCATION;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing,
            float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.hasEffect(FDPotionEffects.IMMORTAL_SOUL.get())) {
            float f = (float) entity.tickCount + partialTicks;
            EntityModel<T> model = this.getParentModel();
            model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
            this.getParentModel().copyPropertiesTo(model);

            // Use greyscale texture
            ResourceLocation texture = getPowerLocation();

            float xOffset = f * 0.01F;

            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.energySwirl(texture, xOffset, f * 0.01F));

            poseStack.pushPose();
            float scale = 1.05F;
            poseStack.scale(scale, scale, scale);

            // Gold color: 1.0, 0.84, 0.0
            model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 0.84F, 0.0F,
                    1.0F);

            poseStack.popPose();
        }
    }
}
