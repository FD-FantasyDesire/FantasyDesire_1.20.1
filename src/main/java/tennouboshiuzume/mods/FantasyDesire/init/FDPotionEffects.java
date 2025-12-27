package tennouboshiuzume.mods.FantasyDesire.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.potioneffect.*;

public class FDPotionEffects {
        public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT,
                        FantasyDesire.MODID);

        public static final RegistryObject<MobEffect> VOID_STRIKE = MOB_EFFECTS.register("void_strike",
                        VoidStrikeEffect::new);
        public static final RegistryObject<MobEffect> RAINBOW_SEVEN_EDGE = MOB_EFFECTS.register("rainbow_seven_edge",
                        RainbowSevenEdgeEffect::new);
        public static final RegistryObject<MobEffect> TELEPORT_BLOCKED = MOB_EFFECTS.register("teleport_blocked",
                        TeleportBlocked::new);
        public static final RegistryObject<MobEffect> IMMORTAL_SOUL = MOB_EFFECTS.register("immortal_soul",
                        ImmortalSoulEffect::new);
        public static final RegistryObject<MobEffect> MISSILE_LOCKED = MOB_EFFECTS.register("missile_locked",
                        MissileLockedEffect::new);

        public static void register(IEventBus eventBus) {
                MOB_EFFECTS.register(eventBus);
        }
}
