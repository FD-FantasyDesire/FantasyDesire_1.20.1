package tennouboshiuzume.mods.FantasyDesire.specialeffects.globalevent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.utils.EchoDamageHelper;

@Mod.EventBusSubscriber
public class EchoDeathEventHandler {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        EchoDamageHelper.detonateArea(entity, 5.0D);
    }
}
