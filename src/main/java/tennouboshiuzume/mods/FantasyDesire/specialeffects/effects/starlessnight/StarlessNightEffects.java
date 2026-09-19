package tennouboshiuzume.mods.FantasyDesire.specialeffects.effects.starlessnight;

import mods.flammpfeil.slashblade.event.BladeMotionEvent;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.potioneffect.VoidStrikeEffect;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageSource;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;
import tennouboshiuzume.mods.FantasyDesire.utils.ItemUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StarlessNightEffects {
    // 数值来自 FDConfig（服务端同步配置），使用处实时读取
    private static final FDConfig.VoidStrikeSe VOID_STRIKE_SE = FDConfig.VOID_STRIKE_SE;
    private static final FDConfig.EchoingStrike ECHOING_STRIKE = FDConfig.ECHOING_STRIKE;
    private static final String TRANSLATION_KEY = "item.fantasydesire.starless_night";

    // 虚空强袭
    // 回响打击
    @SubscribeEvent
    public static void OnHit(SlashBladeEvent.HitEvent event) {
        ItemStack blade = event.getBlade();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        if (!(event.getUser() instanceof Player player))
            return;
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireTranslation(TRANSLATION_KEY)
                .match();
        if (ctx == null)
            return;
        LivingEntity target = event.getTarget();
        if (CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireTranslation(TRANSLATION_KEY)
                .requireSE(FDSpecialEffectsRegistry.VoidStrike)
                .match() != null) {
            stackVoidStrike(target);
        }
    }

    // 处理回响伤害的额外效果
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        if (!source.is(FDDamageSource.ECHO)) {
            return;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player player)) {
            return;
        }
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade)) {
            return;
        }
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireTranslation(TRANSLATION_KEY)
                .requireSE(FDSpecialEffectsRegistry.EchoingStrike)
                .match();
        if (ctx == null) {
            return;
        }
        handleEchoingStrike(player, target, amount);
    }

    // 处理连段切换事件，确保 EchoingVoid 连段被取消时自动切换回原始模型
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBladeMotion(BladeMotionEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade)) {
            return;
        }
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireTranslation(TRANSLATION_KEY)
                .match();
        if (ctx == null) {
            return;
        }
        ResourceLocation newCombo = event.getCombo();
        if (!isEchoingVoidCombo(newCombo)) {
            ItemUtils.ConvertModel(blade, "models/sn.obj");
        }
    }

    // 检查是否是 EchoingVoid 连段
    private static boolean isEchoingVoidCombo(ResourceLocation combo) {
        if (combo == null) {
            return false;
        }
        String comboPath = combo.getPath();
        return comboPath.equals("echoing_void") ||
                comboPath.equals("echoing_void_0") ||
                comboPath.equals("echoing_void_1") ||
                comboPath.equals("echoing_void_2") ||
                comboPath.equals("echoing_void_end");
    }

    private static void stackVoidStrike(LivingEntity entity) {
        stackVoidStrike(entity, 1);
    }

    public static void stackVoidStrike(LivingEntity entity, int stacks) {
        if (stacks <= 0)
            return;
        MobEffect voidStrike = FDPotionEffects.VOID_STRIKE.get();
        MobEffectInstance current = entity.getEffect(voidStrike);
        int duration = VOID_STRIKE_SE.duration();
        int amplifier = stacks - 1;
        if (current != null) {
            amplifier = current.getAmplifier() + stacks;
        }
        amplifier = Math.min(amplifier, VoidStrikeEffect.MAX_STACKS - 1);
        entity.forceAddEffect(new MobEffectInstance(voidStrike, duration, amplifier), null);
    }

    private static void resetInvulnerable(LivingEntity entity) {
        entity.invulnerableTime = 0;
    }

    // 回响打击效果
    private static void handleEchoingStrike(Player player, LivingEntity primaryTarget, float baseDamage) {
        if (player.getRandom().nextFloat() >= ECHOING_STRIKE.chainChance()) {
            return;
        }
        List<LivingEntity> nearbyEnemies = FDTargetSelector.getLivingEntitiesInRadius(
                player,
                primaryTarget.position(),
                ECHOING_STRIKE.chainRadius(),
                true,
                null);
        List<LivingEntity> potentialTargets = new ArrayList<>();
        for (LivingEntity enemy : nearbyEnemies) {
            if (enemy.isAlive() && enemy.getId() != primaryTarget.getId()) {
                potentialTargets.add(enemy);
            }
        }
        if (potentialTargets.isEmpty()) {
            return;
        }
        potentialTargets.sort((a, b) -> {
            int aLayers = getVoidStrikeLayers(a);
            int bLayers = getVoidStrikeLayers(b);
            return Integer.compare(aLayers, bLayers);
        });
        LivingEntity currentTarget = primaryTarget;
        Vec3 prevTargetPos = primaryTarget.position()
                .add(0, primaryTarget.getBbHeight() / 2, 0);

        for (int chainCount = 0; chainCount < ECHOING_STRIKE.chainCount(); chainCount++) {

            LivingEntity nextTarget = null;

            for (LivingEntity target : potentialTargets) {
                if (target.isAlive() && target.getId() != currentTarget.getId()) {
                    nextTarget = target;
                    break;
                }
            }

            if (nextTarget == null) {
                break;
            }

            // 关键：选中后立即移除，防止后续链回头
            potentialTargets.remove(nextTarget);

            resetInvulnerable(nextTarget);

            DamageSource damageSource = player.damageSources().magic();

            nextTarget.hurt(
                    damageSource,
                    baseDamage * ECHOING_STRIKE.chainDamageRatio());

            resetInvulnerable(nextTarget);

            Vec3 nextTargetPos = nextTarget.position()
                    .add(0, nextTarget.getBbHeight() / 2, 0);

            if (player.level() instanceof ServerLevel serverLevel) {
                int lineColor = 0x8000ff;
                int baseLifetime = 20;

                double distance = prevTargetPos.distanceTo(nextTargetPos);

                ParticleUtils.AstraLightningParticles(
                        player.level(),
                        prevTargetPos,
                        nextTargetPos,
                        lineColor,
                        0.05f,
                        baseLifetime,
                        1.0f,
                        true,
                        0.7d,
                        Math.max(2, (int) distance / 2 + 1),
                        0.5f,
                        serverLevel.random.nextLong());
            }

            stackVoidStrike(nextTarget);

            currentTarget = nextTarget;
            prevTargetPos = nextTargetPos;
        }
    }

    // 获取实体的虚空强袭层数
    private static int getVoidStrikeLayers(LivingEntity entity) {
        return VoidStrikeEffect.getVoidStrikeLayers(entity);
    }
}
