package tennouboshiuzume.mods.FantasyDesire.init;

import cn.mmf.slashblade_addon.specialattacks.RapidBlisteringSwords;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.specialattack.RainbowStar;
import tennouboshiuzume.mods.FantasyDesire.specialattack.WingToTheFuture;

public class FDCombo {
    public static final DeferredRegister<ComboState> COMBO_STATES = DeferredRegister.create(ComboState.REGISTRY_KEY,
            FantasyDesire.MODID);

    public static final RegistryObject<ComboState> WING_TO_THE_FUTURE = COMBO_STATES.register(
            "wing_to_the_future",
            ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                    .motionLoc(DefaultResources.ExMotionLocation)
                    .next(ComboState.TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
                    .nextOfTimeout(entity -> FantasyDesire.prefix("wing_to_the_future_end"))
                    .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                            .put(2, (entityIn) -> AttackManager.doSlash(entityIn, -30F, Vec3.ZERO, false, false, 0.1F))
                            .put(3, (entityIn) -> WingToTheFuture.WingToTheFutureEX(entityIn,entityIn.getMainHandItem())).build())
                    .addHitEffect(StunManager::setStun)::build);

    public static final RegistryObject<ComboState> WING_TO_THE_FUTURE_END = COMBO_STATES.register(
            "wing_to_the_future_end",
            ComboState.Builder.newInstance().startAndEnd(459, 488).priority(50)
                    .motionLoc(DefaultResources.ExMotionLocation).next(entity -> SlashBlade.prefix("none"))
                    .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                    .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                            .put(0, AttackManager::playQuickSheathSoundAction).build())
                    .releaseAction(ComboState::releaseActionQuickCharge)::build);

    public static final RegistryObject<ComboState> RAINBOW_STAR = COMBO_STATES.register(
            "rainbow_star",
            ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                    .motionLoc(DefaultResources.ExMotionLocation)
                    .next(ComboState.TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
                    .nextOfTimeout(entity -> FantasyDesire.prefix("rainbow_star_end"))
                    .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                            .put(2, (entityIn) -> AttackManager.doSlash(entityIn, -30F, Vec3.ZERO, false, false, 0.1F))
                            .put(3, (entityIn) -> RainbowStar.RainbowStar(entityIn,entityIn.getMainHandItem())).build())
                    .addHitEffect(StunManager::setStun)::build);

    public static final RegistryObject<ComboState> RAINBOW_STAR_END = COMBO_STATES.register(
            "rainbow_star_end",
            ComboState.Builder.newInstance().startAndEnd(459, 488).priority(50)
                    .motionLoc(DefaultResources.ExMotionLocation).next(entity -> SlashBlade.prefix("none"))
                    .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                    .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                            .put(0, AttackManager::playQuickSheathSoundAction).build())
                    .releaseAction(ComboState::releaseActionQuickCharge)::build);
}

