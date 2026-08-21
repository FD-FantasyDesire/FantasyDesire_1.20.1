package tennouboshiuzume.mods.FantasyDesire.network;

import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FDPlayerVisualStateSync {
    public static final int TYRANT = 1;
    public static final int SHIELD = 1 << 1;
    public static final int IMMORTAL = 1 << 2;
    public static final int FROST_STORM = 1 << 3;
    public static final int COMET_ELYTRA = 1 << 4;

    private static final Map<UUID, Integer> LAST_STATES = new HashMap<>();

    private FDPlayerVisualStateSync() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 5 != 0) {
            return;
        }

        int state = getState(player);
        Integer previous = LAST_STATES.put(player.getUUID(), state);
        if (previous == null || previous != state) {
            FDNetwork.sendToTrackingAndSelf(player,
                    new FDNetwork.PlayerVisualStateMessage(player.getUUID(), state));
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer watcher) {
            FDNetwork.sendToPlayer(watcher,
                    new FDNetwork.PlayerVisualStateMessage(target.getUUID(), getState(target)));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_STATES.remove(event.getEntity().getUUID());
    }

    private static int getState(ServerPlayer player) {
        int state = 0;
        if (hasEffect(player, FDSpecialEffectsRegistry.TyrantStrike)) {
            state |= TYRANT;
        }
        if (hasEffect(player, FDSpecialEffectsRegistry.SoulShield)) {
            state |= SHIELD;
        }
        if (hasEffect(player, FDSpecialEffectsRegistry.ImmortalSoul)) {
            state |= IMMORTAL;
        }
        if (player.hasEffect(FDPotionEffects.FROST_STORM.get())) {
            state |= FROST_STORM;
        }
        if (player.hasEffect(FDPotionEffects.COMET_ELYTRA.get())) {
            state |= COMET_ELYTRA;
        }
        return state;
    }

    private static boolean hasEffect(ServerPlayer player, RegistryObject<SpecialEffect> effect) {
        return CapabilityUtils.SEConditionMatcher.of(player)
                .requireTranslation("item.fantasydesire.chikeflare")
                .requireSE(effect)
                .match() != null;
    }
}
