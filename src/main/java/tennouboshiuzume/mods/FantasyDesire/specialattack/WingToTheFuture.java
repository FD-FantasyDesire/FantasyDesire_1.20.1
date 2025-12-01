package tennouboshiuzume.mods.FantasyDesire.specialattack;


import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.data.builtin.FantasySlashBladeBuiltInRegistry;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.utils.*;

import java.util.List;

public class WingToTheFuture {
    public static void WingToTheFuture(LivingEntity player, ItemStack blade) {
        if (blade.getItem() instanceof ItemSlashBlade) {
            ISlashBladeState state = CapabilityUtils.getBladeState(blade);
            if (state.getTranslationKey().equals("item.fantasydesire.chikeflare")) {
                if (player.level().isClientSide()) return;
                if (!(player instanceof Player)) return;
                int wingCount = (int) Math.min(Math.max(Math.sqrt(Math.abs(((Player) player).experienceLevel)) - 5, 1), 3);
                float baseModif = state.getDamage();
                float magicDamage = 1.0f + (baseModif / 2.0f);
                int countdown = 1;
                for (int i = 0; i < wingCount; i++) {
                    int count = 1;
                    for (int j = 1; j <= 32; j++) {
                        count++;
                        countdown++;
                        boolean front = (count % 2 == 0);
                        int currentValue = count / 2;
                        int countdownValue = countdown / 2;
                        Vec3 base = new Vec3(0, 0, 1);
                        Vec3 sec = base.yRot((float) Math.toRadians(front ? 120 : -120)).normalize().scale(1.5 + 0.5 * currentValue);
                        EntityFDPhantomSword ss = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(), player.level());
                        ss.setPos(player.position());
                        ss.setIsCritical(false);
                        ss.setOwner(player);
                        ss.setOffset(sec);
                        ss.setCenterOffset(new Vec3(0, player.getEyeHeight(), 0));
                        ss.setColor(front ? 0xFFFF00 : 0x00FFFF);
                        ss.setRoll(front ? -45.0f : 45.0f);
                        ss.setStandbyMode("PLAYER");
                        ss.setSpeed(2.5f);
                        ss.setDamage(magicDamage);
                        ss.setDelayTicks(20 + countdownValue);
                        ss.setDelay(200 + countdownValue);
                        ss.setScale(1.5f);
                        ss.setExpRadius(1);
                        ss.setParticleType(ParticleTypes.END_ROD);
                        if (state.getTargetEntity(player.level()) != null) {
                            ss.setTargetId(state.getTargetEntityId());
                        } else if (TargetUtils.getLockTarget(player).isPresent()) {
                            ss.setTargetId(TargetUtils.getLockTarget(player).get().getId());
                        }
                        player.level().addFreshEntity(ss);
                    }
                }
            } else {
                ItemStack newBlade = ItemUtils.dataBakeBlade(blade, FantasyDesire.getBladeAsRegistry(player.level(), FantasySlashBladeBuiltInRegistry.ChikeFlare));
                player.setItemInHand(InteractionHand.MAIN_HAND, newBlade);
            }

        }
    }

    public static void WingToTheFutureEX(LivingEntity player, ItemStack blade) {
        if (blade.getItem() instanceof ItemSlashBlade) {
            ISlashBladeState state = CapabilityUtils.getBladeState(blade);
            if (state.getTranslationKey().equals("item.fantasydesire.chikeflare")) {
//                if (player.level().isClientSide()) return;
                if (!(player instanceof Player)) return;
                int wingCount = (int) Math.min(Math.max(Math.sqrt(Math.abs(((Player) player).experienceLevel)) - 5, 1), 3);
                float baseModif = state.getDamage();
                float magicDamage = 1.0f + (baseModif / 2.0f);
                int countdown = 1;
                int maxFeather = 32;
                List<LivingEntity> targets = TargetUtils.getTargetsInSight((Player) player, 35, 20, true, null);
                for (int i = 0; i < wingCount; i++) {
                    int count = 1;
                    for (int j = 1; j <= maxFeather; j++) {
                        count++;
                        countdown++;
                        boolean front = (count % 2 == 0);
                        int countdownValue = countdown / 2;
                        float baseRadius = 2.5f; // 控制偏移距离
                        float progress = (float) j / (maxFeather - 1);
                        float xRotDeg = 60f - progress * 120f; // 上下展开角度，角度制
                        float yRotDeg = front ? 120f : -120f;  // 左右翼角度，角度制
                        Vec3 base = new Vec3(0, 0, 1);
                        Vec3 sec = base
                                .yRot((float) Math.toRadians(yRotDeg + i * (front ? -5f : 5f)))
                                .xRot((float) Math.toRadians(xRotDeg - i * (front ? 5f : -5f)))
                                .normalize()
                                .scale(baseRadius * (float) Math.pow(1.05, j) - i * 0.25);
                        EntityFDPhantomSword ss = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(), player.level());
                        ss.setIsCritical(false);
                        ss.setOwner(player);
                        ss.setOffset(sec);
                        ss.setCenterOffset(new Vec3(0, player.getEyeHeight(), 0));
                        ss.setColor(front ? 0xFFFF00 : 0x00FFFF);
                        ss.setRoll(front ? -45.0f : 45.0f);
                        ss.setStandbyMode("PLAYER");
                        ss.setMovingMode("SEEK");
                        ss.setSpeed(2.5f);
                        ss.setParticleType(ParticleTypes.END_ROD);
                        ss.setStandbyYawPitch(-yRotDeg, xRotDeg);
                        ss.setPos(player.position());
                        ss.setDamage(magicDamage);
                        ss.setSeekDelay(20 + countdownValue + 5);
                        ss.setDelayTicks(20 + countdownValue);
                        ss.setDelay(300 + countdownValue);
                        ss.setScale(1.5f);
                        ss.setExpRadius(4f);
                        ss.setHasTail(true);
                        ss.setNoClip(true);
                        if (state.getTargetEntity(player.level()) != null) {
                            ss.setTargetId(state.getTargetEntityId());
                        } else if (!targets.isEmpty()) {
                            ss.setTargetId(targets.get(j % targets.size()).getId());
                        }
                        player.level().addFreshEntity(ss);
                    }
                }
            } else {
                ConvertChikeFlare(player, blade);
            }
        }
    }


    public static void WingToTheFutureElytra(LivingEntity player, ItemStack blade) {
        if (blade.getItem() instanceof ItemSlashBlade) {
            ISlashBladeState state = CapabilityUtils.getBladeState(blade);
            if (state.getTranslationKey().equals("item.fantasydesire.chikeflare")) {
                int wingCount = (int) Math.min(Math.max(Math.sqrt(Math.abs(((Player) player).experienceLevel)) - 5, 1), 3);
                int tailcount = 6;
                for (int i = 0; i < 2; i++) {

                    boolean is_right = i % 2 == 0;
//                    wing
                    for (int j = 0; j < 2; j++) {

                    }
//                    tails

                    for (int j = 0; j < tailcount; j++) {
                        Vec3 baseVec = new Vec3(0, 0, -1);
                        Vec3 sec = baseVec.xRot((float) Math.toRadians(30)).zRot((float) Math.toRadians((360/tailcount) * j)).scale(3f);
                        float[] YP = VecMathUtils.getYawPitchFromVec(sec);
                        Vec3 finaLocation = sec.add(0,0,1.5f);
                        SummonFeather(state, player, finaLocation, new Vec3(0,0,0), state.getColorCode(), 0, 0, (360/tailcount) * j, YP[0], YP[1], 1, 200, 200, 200, 1.5f);
                    }
                    player.moveRelative(0.5f,new Vec3(0,0,1));
                }
            }
        }
    }

    public static void ConvertChikeFlare(LivingEntity player, ItemStack blade) {
        ItemStack newBlade = ItemUtils.dataBakeBlade(blade, FantasyDesire.getBladeAsRegistry(player.level(), FantasySlashBladeBuiltInRegistry.ChikeFlare));
        player.setItemInHand(InteractionHand.MAIN_HAND, newBlade);
        player.playSound(SoundEvents.TRIDENT_THUNDER, 1, 0.5f);
        ParticleUtils.LightBoltParticles(player.level(), player.position(), player.position().add(new Vec3(0, 32, 0)), 0xFFFFFF, 0.2f, 20, 0.75f, false, 4, 16);
        for (int i = 0; i < 16; i++) {
            Vec3 end = new Vec3(0, 0, 16);
            end = end.yRot((float) Math.toRadians(Math.random() * 360f)).xRot((float) Math.toRadians(Math.random() * 360f)).add(player.position());
            ParticleUtils.LightBoltParticles(player.level(), player.position(), end, 0xFFFF00, 0.1f, 40, 0.75f, false, 2, 32);
            ParticleUtils.LightBoltParticles(player.level(), player.position(), end, 0x00FFFF, 0.1f, 40, 0.75f, false, 2, 32);
        }
    }

    private static void SummonFeather(ISlashBladeState state, LivingEntity player, Vec3 offset, Vec3 centerOffset, int color, float yaw, float pitch, float roll, float sYaw, float sPitch, float magicDamage, int seekDelay, int delay, int lifeTime, float scale) {
        EntityFDPhantomSword ss = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(), player.level());
        ss.setIsCritical(false);
        ss.setOwner(player);
        ss.setOffset(offset);
        ss.setCenterOffset(centerOffset);
        ss.setColor(color);
        ss.setRoll(roll);
        ss.setStandbyMode("PLAYER");
        ss.setMovingMode("SEEK");
        ss.setSpeed(2.5f);
        ss.setParticleType(ParticleTypes.END_ROD);
        ss.setStandbyYawPitch(sYaw, sPitch);
        ss.setPos(player.position());
        ss.setDamage(magicDamage);
        ss.setSeekDelay(seekDelay);
        ss.setDelayTicks(delay);
        ss.setDelay(lifeTime);
        ss.setScale(scale);
        ss.setExpRadius(4f);
        ss.setHasTail(true);
        ss.setNoClip(true);
        ss.setTargetId(state.getTargetEntityId());
        player.level().addFreshEntity(ss);
    }
}
