package tennouboshiuzume.mods.FantasyDesire.slasharts;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.data.builtin.FantasySlashBladeBuiltInRegistry;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDSoulPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.utils.*;

import java.util.List;

public class WingToTheFuture {
    private static final String CHIKEFLARE_KEY = "item.fantasydesire.chikeflare";

    public static boolean AntiNTR(LivingEntity entity) {
        return CapabilityUtils.SEConditionMatcher.of(entity)
                .requireTranslation(CHIKEFLARE_KEY)
                .match() != null;
    }

    // 重制羽翼幻影剑展开
    public static void WingToTheFuture(LivingEntity player, ItemStack blade) {
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireTranslation(CHIKEFLARE_KEY)
                .match();
        if (ctx == null)
            return;
        if (!(player instanceof Player))
            return;
        ISlashBladeState state = ctx.state;
        int wingCount = Mth.clamp((int) (Math.sqrt(Math.abs(((Player) player).experienceLevel)) - 5), 1, 3);
        float baseModif = state.getDamage();
        float magicDamage = 1.0f + (baseModif / 2.0f);
        int countdown = 1;
        int maxFeather = 32;
        List<LivingEntity> targets = FDTargetSelector.getTargetsInSight((Player) player, 35, 20, true, null);
        for (int i = 0; i < wingCount; i++) {
            int count = 1;
            for (int j = 1; j <= maxFeather; j++) {
                count++;
                countdown++;
                boolean front = (count % 2 == 0);
                int countdownValue = countdown / 2;
                float baseRadius = 2.5f;
                float progress = (float) j / (maxFeather - 1);
                float xRotDeg = 60f - progress * 120f;
                float yRotDeg = front ? 120f : -120f;
                Vec3 base = new Vec3(0, 0, 1);
                Vec3 sec = base
                        .yRot((float) Math.toRadians(yRotDeg + i * (front ? 5f : -5f)))
                        .xRot((float) Math.toRadians(xRotDeg - i * (5f)))
                        .normalize()
                        .scale(baseRadius * (float) Math.pow(1.05, j) - i * 0.25);
                EntityFDSoulPhantomSword ss = new EntityFDSoulPhantomSword(FDEntitys.FDSoulPhantomSword.get(),
                        player.level());
                ss.setIsCritical(false);
                ss.setOwner(player);
                ss.setOffset(sec);
                ss.setCenterOffset(new Vec3(0, player.getEyeHeight(), 0));
                ss.setColor(front ? 0xFFFF00 : 0x00FFFF);
                ss.setRoll(front ? -45.0f : 45.0f);
                ss.setStandbyMode(EntityFDPhantomSword.StandbyMode.PLAYER);
                ss.setMovingMode(EntityFDPhantomSword.MovingMode.ADV_SEEK);
                ss.setSpeed(2.5f);
                ss.setStandbyYawPitch(-yRotDeg, xRotDeg);
                ss.setPos(player.position());
                ss.setDamage(magicDamage);
                ss.setSeekDelay(20 + countdownValue + 5);
                ss.setDelayTicks(20 + countdownValue);
                ss.setDelay(100 + countdownValue);
                ss.setScale(1.5f);
                ss.setExpRadius(3f);
                ss.setHasTail(true);
                ss.setNoClip(true);
                if (state.getTargetEntity(player.level()) != null) {
                    ss.setTargetId(state.getTargetEntityId());
                } else if (!targets.isEmpty()) {
                    ss.setTargetId(targets.get(countdownValue % targets.size()).getId());
                }
                ss.tryInit();
                player.level().addFreshEntity(ss);
            }
        }
    }

    // 不是对应的刀，转化
    public static void ConvertChikeFlare(LivingEntity player, ItemStack blade) {
        ItemStack newBlade = ItemUtils.dataBakeBlade(blade,
                FantasyDesire.getBladeAsRegistry(player.level(), FantasySlashBladeBuiltInRegistry.ChikeFlare));
        player.setItemInHand(InteractionHand.MAIN_HAND, newBlade);
        player.playSound(SoundEvents.TRIDENT_THUNDER, 1, 0.5f);
        ParticleUtils.LightBoltParticles(player.level(), player.position(), player.position().add(new Vec3(0, 32, 0)),
                0xFFFFFF, 0.2f, 20, 0.75f, false, 4, 16);
        for (int i = 0; i < 16; i++) {
            Vec3 end = new Vec3(0, 0, 16);
            end = end.yRot((float) Math.toRadians(Math.random() * 360f))
                    .xRot((float) Math.toRadians(Math.random() * 360f)).add(player.position());
            ParticleUtils.LightBoltParticles(player.level(), player.position(), end, 0xFFFF00, 0.1f, 40, 0.75f, false,
                    2, 32);
            ParticleUtils.LightBoltParticles(player.level(), player.position(), end, 0x00FFFF, 0.1f, 40, 0.75f, false,
                    2, 32);
        }
    }
}
