package tennouboshiuzume.mods.FantasyDesire.client;

import mods.flammpfeil.slashblade.client.renderer.LockonCircleRender;
import mods.flammpfeil.slashblade.client.renderer.gui.RankRenderer;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModel;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.BladeMotionManager;
import mods.flammpfeil.slashblade.compat.playerAnim.PlayerAnimationOverrider;
import mods.flammpfeil.slashblade.event.client.AdvancementsRecipeRenderer;
import mods.flammpfeil.slashblade.event.client.SneakingMotionCanceller;
import mods.flammpfeil.slashblade.event.client.UserPoseOverrider;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.util.LoaderUtil;
import org.jetbrains.annotations.Nullable;
import tennouboshiuzume.mods.FantasyDesire.init.FDItems;


@Mod.EventBusSubscriber(modid = "fantasydesire",value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class ClientHandler {
    @SubscribeEvent
    public static void doClientStuff(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(BladeModelManager.getInstance());
        MinecraftForge.EVENT_BUS.register(BladeMotionManager.getInstance());
        SneakingMotionCanceller.getInstance().register();
        if (LoaderUtil.isClassAvailable("dev.kosmx.playerAnim.api.layered.AnimationStack")) {
            PlayerAnimationOverrider.getInstance().register();
        } else {
            UserPoseOverrider.getInstance().register();
        }
        LockonCircleRender.getInstance().register();
        AdvancementsRecipeRenderer.getInstance().register();
        RankRenderer.getInstance().register();
        ItemProperties.register(FDItems.fantasyslashblade, new ResourceLocation("slashblade:user"),
                new ClampedItemPropertyFunction() {
                    @Override
                    public float unclampedCall(ItemStack p_174564_, @Nullable ClientLevel p_174565_,
                                               @Nullable LivingEntity p_174566_, int p_174567_) {
                        BladeModel.user = p_174566_;
                        return 0;
                    }
                });
    }
    @SubscribeEvent
    public static void Baked(final ModelEvent.ModifyBakingResult event) {
        bakeBlade(FDItems.fantasyslashblade, event);
    }

    public static void bakeBlade(Item blade, final ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation loc = new ModelResourceLocation(ForgeRegistries.ITEMS.getKey(blade), "inventory");
        BladeModel model = new BladeModel(event.getModels().get(loc), event.getModelBakery());
        event.getModels().put(loc, model);
    }
}