package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.chikeflare;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.MathUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.VecMathUtils;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChikeFlareEffects {
//    灵魂之盾
    @SubscribeEvent
    public static void OnBypassAttack(LivingAttackEvent event){
        if (!(event.getEntity() instanceof Player player)) return;
        if(!(player.getMainHandItem().getItem() instanceof ItemFantasySlashBlade)) return;
        ItemStack blade = player.getMainHandItem();
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        RandomSource random = player.getRandom();
        if (CapabilityUtils.isSpecialEffectActiveForItem(state,FDSpecialEffects.SoulShield,player,"item.fantasydesire.chikeflare")){
            if (event.getSource().getEntity() instanceof LivingEntity attacker && MathUtils.RandomCheck(fdState.getSpecialCharge()*0.5f)) {
                Vec3 VecToAttacker = VecMathUtils.calculateDirectionVec(player, attacker);
                float[] YP = VecMathUtils.getYawPitchFromVec(VecToAttacker);
//                自动防反
                AddonSlashUtils.doAddonSlash(player, random.nextInt(180), YP[0], YP[1], 0x00FFFF,0, VecToAttacker, false, false, 0.2f, KnockBacks.cancel);
//                吸收成功格挡的伤害
                player.setAbsorptionAmount(Mth.clamp(player.getAbsorptionAmount()+event.getAmount(),0,20));
                event.setCanceled(true);
            }
        }
    }
    @SubscribeEvent
    public static void OnBypassAttack(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player.getMainHandItem().getItem() instanceof ItemFantasySlashBlade)) return;
        ItemStack blade = player.getMainHandItem();
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        RandomSource random = player.getRandom();
        if (CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.SoulShield, player, "item.fantasydesire.chikeflare")) {
//            自动防反失败，充能+1
            fdState.setSpecialCharge(Mth.clamp(fdState.getSpecialCharge()+1,0,fdState.getMaxSpecialCharge()));
//            减缓防反失败后的伤害
            event.setAmount(Math.min(event.getAmount(),5));
        }
    }
//    不屈之魂
    @SubscribeEvent
    public static void OnDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack validBlade = ItemStack.EMPTY;
        ISlashBladeState state = null;
        IFantasySlashBladeState fdState = null;
        // 主手检测
        if (main.getItem() instanceof ItemSlashBlade) {
            ISlashBladeState s = CapabilityUtils.getBladeState(main);
            if (CapabilityUtils.isSpecialEffectActiveForItem(s, FDSpecialEffects.ImmortalSoul, player, "item.fantasydesire.chikeflare")) {
                validBlade = main;
                state = s;
                fdState = CapabilityUtils.getFantasyBladeState(main);
            }
        }
        // 副手检测（只有当主手未通过时才检测）
        if (validBlade.isEmpty() && off.getItem() instanceof ItemSlashBlade) {
            ISlashBladeState s = CapabilityUtils.getBladeState(off);
            if (CapabilityUtils.isSpecialEffectActiveForItem(s, FDSpecialEffects.ImmortalSoul, player, "item.fantasydesire.chikeflare")) {
                validBlade = off;
                state = s;
                fdState = CapabilityUtils.getFantasyBladeState(off);
            }
        }
        // 如果两手都不满足条件则退出
        if (validBlade.isEmpty() || state == null || fdState == null) return;

        int soul = state.getProudSoulCount();
        float baseattack = state.getBaseAttackModifier();

        if (soul >= 1000) {
            // 扣除耀魂
            state.setProudSoulCount(soul - 1000);
            // 永久提升基础面板
            state.setBaseAttackModifier(baseattack + 0.67f);
            // 特殊充能 +10
            fdState.setSpecialCharge(Mth.clamp(fdState.getSpecialCharge() + 10, 0, fdState.getMaxSpecialCharge()));

            // 回复与抗性
            player.setHealth(player.getMaxHealth() / 2.0F);
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 6, 4));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 6, 4));

            // 阻止死亡事件
            event.setCanceled(true);

//            // 调试输出（可选）
//             player.sendSystemMessage(Component.literal("Immortal Soul triggered from " + (validBlade == main ? "Main Hand" : "Off Hand")));
        }
    }
//    暴君一击
    @SubscribeEvent
    public static void OnHit(SlashBladeEvent.HitEvent event){
        if (!(event.getUser() instanceof Player player)) return;
        if (!(player.getMainHandItem().getItem() instanceof ItemFantasySlashBlade)) return;
        ItemStack blade = player.getMainHandItem();
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        if (CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.TyrantStrike, player, "item.fantasydesire.chikeflare")) {
            LivingEntity target = event.getTarget();
            RandomSource random = target.getRandom();
            float yaw = (float) random.nextInt(360);
            float pitch = 90f+(float)(random.nextGaussian() * 5f);
            float roll = (float) (random.nextInt(360) - 180);
            Vec3 basePos = new Vec3(0, 0, 1);
            Vec3 spawnPos = target.position().add(0,target.getBbHeight()/2,0)
                    .add(basePos
                            .xRot((float) Math.toRadians(pitch))
                            .yRot((float) Math.toRadians(yaw))
                            .scale(30f));

            Vec3 lookVec = target.position().add(0, target.getBbHeight() / 2, 0).subtract(spawnPos).normalize();
            float lookYaw = (float) (Math.atan2(-lookVec.x, lookVec.z) * (180f / Math.PI));
            float lookPitch = (float) (Math.asin(-lookVec.y) * (180f / Math.PI));

            EntityFDPhantomSword ss = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(),player.level());
            ss.setIsCritical(false);
            ss.setOwner(player);
            ss.setColor(state.getColorCode());
            ss.setRoll(roll);
            ss.setDamage(target.getMaxHealth()/4);
            ss.setSpeed(5);
            ss.setStandbyMode("WORLD");
            ss.setMovingMode("NORMAL");
            ss.setDelay(200);
            ss.setParticleType(ParticleTypes.EXPLOSION);
            ss.setDelayTicks(40);
            ss.setNoClip(true);
            ss.setHasTail(true);
            ss.setFireSound(SoundEvents.WITHER_SHOOT,1,1.5f);
            ss.setScale(3f);
            ss.setTargetId(target.getId());
            ss.setStandbyYawPitch(lookYaw, lookPitch);
            ss.setPos(spawnPos);
            player.level().addFreshEntity(ss);
        }
    }
//TODO:修复初始朝向



}
