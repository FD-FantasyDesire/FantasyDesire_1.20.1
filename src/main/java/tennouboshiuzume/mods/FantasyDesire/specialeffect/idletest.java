package tennouboshiuzume.mods.FantasyDesire.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.ColorUtils;

@Mod.EventBusSubscriber
public class idletest {
    public static final Capability<IFantasySlashBladeState> FDBLADESTATE = CapabilityManager.get(new CapabilityToken<IFantasySlashBladeState>() {
    });
    public static final Capability<ISlashBladeState> BLADESTATE = CapabilityManager.get(new CapabilityToken<ISlashBladeState>() {
    });
    @SubscribeEvent
    public static void fdupdateevent (SlashBladeEvent.UpdateEvent event){
        ItemStack blade = event.getBlade();
        Entity player = event.getEntity();
        if (blade.getItem() instanceof ItemFantasySlashBlade){
            blade.getCapability(BLADESTATE).ifPresent((s) -> {
//                s.setModel(new ResourceLocation(FantasyDesire.MODID, "models/chikeflare.obj"));
//                s.setTexture(new ResourceLocation(FantasyDesire.MODID, "models/chikeflare.png"));
                s.setColorCode(ColorUtils.getSmoothTransitionColor(player.level().getDayTime()%120,120,true));
            });
//            blade.getCapability(FDBLADESTATE).ifPresent((s) -> {
//                s.setMaxSpecialCharge(100);
//                s.setSpecialCharge(13);
//            });
        }
    }
}
