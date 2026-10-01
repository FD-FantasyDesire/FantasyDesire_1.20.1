package tennouboshiuzume.mods.FantasyDesire.entity.phantomsword;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.utils.VecMathUtils;

public final class PhantomSwordBehaviors {
    public static final BehaviorRegistry<SwordStandbyBinding> BINDINGS = new BehaviorRegistry<>();
    public static final BehaviorRegistry<SwordFlightBehavior> FLIGHTS = new BehaviorRegistry<>();

    public static final ResourceLocation WORLD = BINDINGS.register(id("world"), () -> sword -> {
        float[] angles = sword.getStandbyYawPitch();
        return new SwordStandbyBinding.Pose(sword.position(), Vec3.ZERO, -angles[0], -angles[1], false);
    });
    public static final ResourceLocation OWNER = BINDINGS.register(id("owner"), () -> sword -> {
        Entity owner = sword.getShooter();
        if (owner == null)
            return null;
        Vec3 offset = sword.getOffset().xRot((float) Math.toRadians(-owner.getXRot()))
                .yRot((float) Math.toRadians(-owner.getYRot()));
        float[] angles = sword.getStandbyYawPitch();
        Vec3 base = new Vec3(0, 0, 1).xRot((float) Math.toRadians(angles[1]))
                .yRot((float) Math.toRadians(angles[0]));
        Vec3 axis = new Vec3(1, 0, 0).yRot((float) Math.toRadians(owner.getYRot()));
        float[] rotation = VecMathUtils.getYawPitchFromVec(VecMathUtils.rotateAroundAxis(
                base.yRot((float) Math.toRadians(owner.getYRot())), axis, -owner.getXRot()));
        return new SwordStandbyBinding.Pose(owner.position().add(sword.getCenterOffset()).add(offset),
                owner.getDeltaMovement(), rotation[0], rotation[1], true);
    });
    public static final ResourceLocation NONE = BINDINGS.register(id("none"), () -> new SwordStandbyBinding() {
        @Override
        public Pose resolve(EntityFDPhantomSword sword) {
            return null;
        }

        @Override
        public boolean autoLaunch() {
            return false;
        }
    });

    public static final ResourceLocation STRAIGHT = FLIGHTS.register(id("straight"), () -> sword -> null);
    public static final ResourceLocation HOMING = FLIGHTS.register(id("homing"), () -> new HomingFlight(false));
    public static final ResourceLocation GUIDED = FLIGHTS.register(id("guided"), () -> new HomingFlight(true));
    public static final ResourceLocation PIERCING_HOMING = FLIGHTS.register(id("piercing_homing"),
            PiercingHomingFlight::new);
    // 首次命中后切换类型，复用已有行为 ID 同步机制，让客户端同时启用强化转向。
    public static final ResourceLocation PIERCING_CHAIN = FLIGHTS.register(id("piercing_homing_chain"),
            () -> new PiercingHomingFlight(true));

    private static ResourceLocation id(String path) {
        return ResourceLocation.tryParse("fantasydesire:" + path);
    }

    private PhantomSwordBehaviors() {}
}
