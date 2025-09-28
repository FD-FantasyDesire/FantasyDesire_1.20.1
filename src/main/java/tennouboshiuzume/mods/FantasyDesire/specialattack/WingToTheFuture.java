package tennouboshiuzume.mods.FantasyDesire.specialattack;


import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.data.builtin.FantasySlashBladeBuiltInRegistry;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ItemUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.TargetUtils;

public class WingToTheFuture {
    public static void WingToTheFuture(LivingEntity player, ItemStack blade){
        if (blade.getItem() instanceof ItemSlashBlade){
            ISlashBladeState state = CapabilityUtils.getBladeState(blade);
            if (state.getTranslationKey().equals("item.fantasydesire.chikeflare")){
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
                        Vec3 base = new Vec3(0,0,1);
                        Vec3 sec = base.yRot((float) Math.toRadians(front ?120 :-120)).normalize().scale(1.5 + 0.5*currentValue);
                        EntityFDPhantomSword ss = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(),player.level());
                        ss.setPos(player.position());
                        ss.setIsCritical(false);
                        ss.setOwner(player);
                        ss.setOffset(sec);
                        ss.setCenterOffset(new Vec3(0,player.getEyeHeight(),0));
                        ss.setColor(front ? 0xFFFF00 : 0x00FFFF);
                        ss.setRoll(front ? -45.0f : 45.0f);
                        ss.setStandbyMode("PLAYER");
                        ss.setSpeed(2.5f);
                        ss.setDamage(magicDamage);
                        ss.setDelayTicks(20+countdownValue);
                        ss.setDelay(200 + countdownValue);
                        ss.setScale(1.5f);
                        ss.setExpRadius(1);
                        ss.setParticleType(ParticleTypes.END_ROD);
                        if (state.getTargetEntity(player.level())!=null){
                            ss.setTargetId(state.getTargetEntityId());
                        }else if (TargetUtils.getLockTarget(player).isPresent()){
                            ss.setTargetId(TargetUtils.getLockTarget(player).get().getId());
                        }
                        player.level().addFreshEntity(ss);
                    }
                }
            }else {
                ItemStack newBlade = ItemUtils.dataBakeBlade(blade,FantasyDesire.getBladeAsRegistry(player.level(),FantasySlashBladeBuiltInRegistry.ChikeFlare));
                player.setItemInHand(InteractionHand.MAIN_HAND,newBlade);
            }

        }
    }

    public static void WingToTheFutureEX(LivingEntity player, ItemStack blade){
        if (blade.getItem() instanceof ItemSlashBlade){
            ISlashBladeState state = CapabilityUtils.getBladeState(blade);
            if (state.getTranslationKey().equals("item.fantasydesire.chikeflare")){
//                if (player.level().isClientSide()) return;
                if (!(player instanceof Player)) return;
                int wingCount = (int) Math.min(Math.max(Math.sqrt(Math.abs(((Player) player).experienceLevel)) - 5, 1), 3);
                float baseModif = state.getDamage();
                float magicDamage = 1.0f + (baseModif / 2.0f);
                int countdown = 1;
                int maxFeather = 32;
                for (int i = 0; i < wingCount; i++) {
                    int count = 1;
                    for (int j = 1; j <= maxFeather; j++) {
                        count++;
                        countdown++;
                        boolean front = (count % 2 == 0);
                        int currentValue = count / 2;
                        int countdownValue = countdown / 2;
                        float baseRadius = 2.5f; // 控制偏移距离
                        float progress = (float) j / (maxFeather - 1);
                        float xRotDeg = 60f - progress * 120f; // 上下展开角度，角度制
                        float yRotDeg = front ? 120f : -120f;  // 左右翼角度，角度制
                        Vec3 base = new Vec3(0,0,1);
                        Vec3 sec = base
                                .yRot((float)Math.toRadians(yRotDeg+i*(front ? -5f : 5f)))
                                .xRot((float)Math.toRadians(xRotDeg-i*(front ? 5f : -5f)))
                                .normalize()
                                .scale(baseRadius * (float)Math.pow(1.05, j)-i*0.25);
                        EntityFDPhantomSword ss = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(),player.level());
                        ss.setIsCritical(false);
                        ss.setOwner(player);
                        ss.setOffset(sec);
                        ss.setCenterOffset(new Vec3(0,player.getEyeHeight(),0));
                        ss.setColor(front ? 0xFFFF00 : 0x00FFFF);
                        ss.setRoll(front ? -45.0f : 45.0f);
                        ss.setStandbyMode("PLAYER");
                        ss.setMovingMode("SEEK");
                        ss.setSeekDelay(20+countdownValue+2);
                        ss.setSpeed(2.5f);
                        ss.setParticleType(ParticleTypes.END_ROD);
                        ss.setStandbyYawPitch(-yRotDeg,xRotDeg);
                        ss.setPos(player.position());
                        ss.setDamage(magicDamage);
                        ss.setDelayTicks(20+countdownValue);
                        ss.setDelay(300 + countdownValue);
                        ss.setScale(1.5f);
                        ss.setExpRadius(1);
                        if (state.getTargetEntity(player.level())!=null){
                            ss.setTargetId(state.getTargetEntityId());
                        }else if (TargetUtils.getLockTarget(player).isPresent()){
                            ss.setTargetId(TargetUtils.getLockTarget(player).get().getId());
                        }
                        player.level().addFreshEntity(ss);
                    }
                }
            }else {
                ItemStack newBlade = ItemUtils.dataBakeBlade(blade,FantasyDesire.getBladeAsRegistry(player.level(),FantasySlashBladeBuiltInRegistry.ChikeFlare));
                player.setItemInHand(InteractionHand.MAIN_HAND,newBlade);
            }

        }
    }
}
