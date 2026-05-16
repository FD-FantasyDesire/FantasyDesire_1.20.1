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

        // 检查是否从 EchoingVoid 连段切换到其他连段
        if (isEchoingVoidCombo(currentCombo) && !isEchoingVoidCombo(newCombo)) {
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
        int duration = 60;
        int amplifier = 0;
        MobEffect voidStrike = FDPotionEffects.VOID_STRIKE.get();
        // 如果已有这个效果，叠加等级
        MobEffectInstance current = entity.getEffect(voidStrike);
        if (current != null) {
            amplifier = Math.min(current.getAmplifier() + 1, 5);
            duration = current.getDuration();
        }
        entity.addEffect(new MobEffectInstance(voidStrike, duration, amplifier));
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

            // 从上一个目标生成闪电到当前目标（链式传递）
            ParticleUtils.LightBoltParticles(
                    player.level(),
                    prevTargetPos,
                    nextTargetPos,
                    0x8000ff, // 紫色
                    0.05f, // 粗细
                    5, // 存活时间
                    1.0f, // 透明度
                    true, // 渐隐
                    0.5, // 随机性
                    3 // 细分层级
            );

            // 在目标位置播放环形末地烛粒子效果，半径2，粒子数量8
            ParticleUtils.generateRingParticles(
                    ParticleTypes.END_ROD,
                    nextTarget.level(),
                    nextTarget.getX(),
                    nextTarget.getY() + nextTarget.getBbHeight() / 2,
                    nextTarget.getZ(),
                    2.0, // 半径
                    8 // 粒子数量
            );
            
            stackVoidStrike(nextTarget);
            
            // 更新当前目标和上一个目标位置，用于下一次链式传递
            currentTarget = nextTarget;
            prevTargetPos = nextTargetPos;
        }
    }

    // 获取实体的虚空强袭层数
    private static int getVoidStrikeLayers(LivingEntity entity) {
        MobEffect voidStrike = FDPotionEffects.VOID_STRIKE.get();
        MobEffectInstance effect = entity.getEffect(voidStrike);
        return effect != null ? effect.getAmplifier() + 1 : 0;
    }
}