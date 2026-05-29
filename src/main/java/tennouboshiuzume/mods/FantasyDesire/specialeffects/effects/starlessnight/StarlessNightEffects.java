package tennouboshiuzume.mods.FantasyDesire.specialeffects.effects.starlessnight;

import mods.flammpfeil.slashblade.event.BladeMotionEvent;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
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

        // 使用 SEConditionMatcher 统一检查翻译键
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireTranslation(TRANSLATION_KEY)
                .match();
        if (ctx == null)
            return;

        LivingEntity target = event.getTarget();

        // 检查 VoidStrike
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

        // 只处理回响伤害
        if (!source.is(FDDamageSource.ECHO)) {
            return;
        }

        // 获取攻击者
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player player)) {
            return;
        }

        // 检查玩家是否持有 EchoingStrike 效果的武器
        ItemStack blade = player.getMainHandItem();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade)) {
            return;
        }

        // 检查是否满足 EchoingStrike 效果条件
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireTranslation(TRANSLATION_KEY)
                .requireSE(FDSpecialEffectsRegistry.EchoingStrike)
                .match();
        if (ctx == null) {
            return;
        }

        // 处理回响打击的额外效果
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

        // 检查是否满足 EchoingVoid 效果条件
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireTranslation(TRANSLATION_KEY)
                .match();
        if (ctx == null) {
            return;
        }

        // 获取当前连段和新连段
        ResourceLocation currentCombo = CapabilityUtils.getBladeState(blade).getComboSeq();
        ResourceLocation newCombo = event.getCombo();

        // 检查新的连段是否不是 EchoingVoid 的白名单连段
        if (!isEchoingVoidCombo(newCombo)) {
            // 切换回原始模型
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
        int duration = 200; // max duration 10 seconds (200 ticks)
        int amplifier = stacks - 1;

        if (current != null) {
            amplifier = current.getAmplifier() + stacks;
        }
        amplifier = Math.min(amplifier, 49); // max 50 stacks

        entity.forceAddEffect(new MobEffectInstance(voidStrike, duration, amplifier), null);
    }

    // 重置无敌帧
    private static void resetInvulnerable(LivingEntity entity) {
        entity.invulnerableTime = 0;
    }

    // 回响打击效果
    private static void handleEchoingStrike(Player player, LivingEntity primaryTarget, float baseDamage) {
        // 25%概率触发
        if (player.getRandom().nextFloat() >= 0.25f) {
            return;
        }

        // 单次根据首个目标位置结算16格范围内的敌人
        List<LivingEntity> nearbyEnemies = FDTargetSelector.getLivingEntitiesInRadius(
                player,
                primaryTarget.position(),
                16.0,
                true,
                null);

        // 排除主要目标
        List<LivingEntity> potentialTargets = new ArrayList<>();
        for (LivingEntity enemy : nearbyEnemies) {
            if (enemy.isAlive() && enemy.getId() != primaryTarget.getId()) {
                potentialTargets.add(enemy);
            }
        }

        if (potentialTargets.isEmpty()) {
            return;
        }

        // 按照虚空强袭层数排序，优先选择层数最低的目标
        potentialTargets.sort((a, b) -> {
            int aLayers = getVoidStrikeLayers(a);
            int bLayers = getVoidStrikeLayers(b);
            return Integer.compare(aLayers, bLayers);
        });

        // 链式传递，最多连锁3次
        LivingEntity currentTarget = primaryTarget;
        Vec3 prevTargetPos = primaryTarget.position().add(0, primaryTarget.getBbHeight() / 2, 0);

        for (int chainCount = 0; chainCount < 3; chainCount++) {
            // 从剩余目标中选择层数最低的（排除当前目标）
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

            // 重置无敌帧并造成伤害
            resetInvulnerable(nextTarget);
            DamageSource damageSource = player.damageSources().magic();
            nextTarget.hurt(damageSource, baseDamage * 0.1f);
            resetInvulnerable(nextTarget);

            Vec3 nextTargetPos = nextTarget.position().add(0, nextTarget.getBbHeight() / 2, 0);

            // 连线和环形粒子效果
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                int lineColor = 0x8000ff;
                int baseLifetime = 20;
                tennouboshiuzume.mods.FantasyDesire.client.particle.GlowingLineParticleOptions lineOpts = new tennouboshiuzume.mods.FantasyDesire.client.particle.GlowingLineParticleOptions(
                        prevTargetPos, nextTargetPos, lineColor, 0.05f, 1.0f, true, baseLifetime);
                serverLevel.sendParticles(lineOpts, prevTargetPos.x, prevTargetPos.y, prevTargetPos.z, 1, 0, 0, 0, 0);

                double distance = prevTargetPos.distanceTo(nextTargetPos);
                int numRings = (int) (distance / 2.0);
                if (numRings > 0) {
                    net.minecraft.world.phys.Vec3 direction = nextTargetPos.subtract(prevTargetPos).normalize();
                    for (int i = 1; i <= numRings; i++) {
                        net.minecraft.world.phys.Vec3 ringPos = prevTargetPos.add(direction.scale(i * 2.0));
                        int ringLifetime = baseLifetime + i * 5;
                        tennouboshiuzume.mods.FantasyDesire.client.particle.SpreadingRingParticleOptions ringOpts = new tennouboshiuzume.mods.FantasyDesire.client.particle.SpreadingRingParticleOptions(
                                lineColor, 0.2f, 0.05f, ringLifetime);
                        serverLevel.sendParticles(ringOpts, ringPos.x, ringPos.y, ringPos.z, 1, 0, 0, 0, 0);
                    }
                }
            }

            stackVoidStrike(nextTarget);

            // 更新当前目标和上一个目标位置，用于下一次链式传递
            currentTarget = nextTarget;
            prevTargetPos = nextTargetPos;
        }
    }

    // 获取实体的虚空强袭层数
    private static int getVoidStrikeLayers(LivingEntity entity) {
        return tennouboshiuzume.mods.FantasyDesire.potioneffect.VoidStrikeEffect.getVoidStrikeLayers(entity);
    }
}