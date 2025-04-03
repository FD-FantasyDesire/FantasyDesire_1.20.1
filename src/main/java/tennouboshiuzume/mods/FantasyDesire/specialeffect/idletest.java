package tennouboshiuzume.mods.FantasyDesire.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
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
    public static void fdupdateevent(SlashBladeEvent.UpdateEvent event) {
        ItemStack blade = event.getBlade();
        Entity player = event.getEntity();
//        if (blade.getItem() instanceof ItemFantasySlashBlade) {
//            blade.getCapability(BLADESTATE).ifPresent((s) -> {
////                s.setModel(new ResourceLocation(FantasyDesire.MODID, "models/chikeflare.obj"));
////                s.setTexture(new ResourceLocation(FantasyDesire.MODID, "models/chikeflare.png"));
//                int color = ColorUtils.getSmoothTransitionColor(player.level().getDayTime() % 126, 126, true);
////                System.out.println(Integer.toHexString(color));
//                s.setColorCode(color);
//            });
//
//            blade.getCapability(FDBLADESTATE).ifPresent((s) -> {
//                int timeStep = (int) ((player.level().getDayTime() + 8) % 126);
//                int damageTypeIndex = (timeStep / 18) ;
//                s.setSpecialAttackEffect(damageTypes[damageTypeIndex]);
////                System.out.println(damageTypeIndex);
////                System.out.println(damageTypes[damageTypeIndex]);
//            });
//        }
    }
//    @SubscribeEvent
//    public static void damageevent(LivingDamageEvent event){
//        event.getSource()
//    }

    public static String[] damageTypes = {"Wrath", "Lust", "Sloth", "Gluttony", "Gloom", "Pride", "Envy"};

}
