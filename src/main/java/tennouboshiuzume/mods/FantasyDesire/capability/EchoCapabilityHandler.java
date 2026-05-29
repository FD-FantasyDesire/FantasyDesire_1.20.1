package tennouboshiuzume.mods.FantasyDesire.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class EchoCapabilityHandler {
    public static final ResourceLocation ECHO_CAP = new ResourceLocation("fantasydesire", "echo_damage");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity) {
            event.addCapability(ECHO_CAP, new EchoDamageProvider());
        }
    }
}