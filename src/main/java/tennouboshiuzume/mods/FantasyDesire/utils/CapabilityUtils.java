package tennouboshiuzume.mods.FantasyDesire.utils;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;

import java.util.Optional;

public class CapabilityUtils {
    public static final Capability<IFantasySlashBladeState> FDBLADESTATE = CapabilityManager
            .get(new CapabilityToken<IFantasySlashBladeState>() {
            });
    public static final Capability<ISlashBladeState> BLADESTATE = CapabilityManager
            .get(new CapabilityToken<ISlashBladeState>() {
            });

    // ------------------ 基础获取方法 ------------------
    public static ISlashBladeState getBladeState(ItemStack blade) {
        Optional<ISlashBladeState> StateOpt = blade.getCapability(BLADESTATE).resolve();
        return StateOpt.orElse(null);
    }

    public static IFantasySlashBladeState getFantasyBladeState(ItemStack blade) {
        Optional<IFantasySlashBladeState> fdStateOpt = blade.getCapability(FDBLADESTATE).resolve();
        return fdStateOpt.orElse(null);
    }

    // ------------------ SE / 翻译键检测 ------------------
    public static boolean isSpecialEffectActive(ISlashBladeState state, RegistryObject<SpecialEffect> effect,
            LivingEntity entity) {
        int level = 0;
        if (entity instanceof Player player) {
            level = player.experienceLevel;
        } else if (entity instanceof LivingEntity) {
            level = effect.get().getRequestLevel();
        }
        return (SpecialEffect.isEffective(effect.getId(), level) && state.hasSpecialEffect(effect.getId()));
    }

    public static boolean isRightTranslationKey(ISlashBladeState state, String itemTranslationKey) {
        return state != null && state.getTranslationKey().equals(itemTranslationKey);
    }

    // 同时检定SE生效和翻译键
    public static boolean isSpecialEffectActiveForItem(ISlashBladeState state, RegistryObject<SpecialEffect> effect,
            LivingEntity entity, String itemTranslationKey) {
        return isSpecialEffectActive(state, effect, entity) && state.getTranslationKey().equals(itemTranslationKey);
    }

    // ------------------ BladeContext ------------------
    public static class BladeContext {
        public final ItemStack blade;
        public final ISlashBladeState state;
        public final IFantasySlashBladeState fantasyState;

        public BladeContext(ItemStack blade, ISlashBladeState state, IFantasySlashBladeState fantasyState) {
            this.blade = blade;
            this.state = state;
            this.fantasyState = fantasyState;
        }

    }

    // ------------------ 链式条件检查 ------------------
    public static class SEConditionMatcher {
        private final LivingEntity entity;

        // --- 定义检查模式枚举 ---
        private enum HandCheckMode {
            MAIN_HAND_ONLY, // 仅主手（默认）
            OFF_HAND_ONLY, // 仅副手
            BOTH_MAIN_PRIORITY, // 双手（主手优先）
            BOTH_OFF_PRIORITY // 双手（副手优先）
        }

        // 默认模式：仅主手
        private HandCheckMode checkMode = HandCheckMode.MAIN_HAND_ONLY;

        // 指定直接检查某个物品栈，而不是从实体手中寻找
        private ItemStack directStack = null;

        // --- 匹配条件配置 ---
        private String requireTranslationKey = null;
        private net.minecraft.resources.ResourceLocation requireModel = null;
        private RegistryObject<SpecialEffect> requireSE = null;
        private Object requireSA = null;

        private SEConditionMatcher(LivingEntity entity) {
            this.entity = entity;
        }

        private SEConditionMatcher(ItemStack stack, LivingEntity entity) {
            this.directStack = stack;
            this.entity = entity;
        }

        public static SEConditionMatcher of(LivingEntity entity) {
            return new SEConditionMatcher(entity);
        }

        public static SEConditionMatcher of(ItemStack stack, LivingEntity entity) {
            return new SEConditionMatcher(stack, entity);
        }

        // ================== 检查范围与优先级控制 ==================

        /**
         * 仅检查副手
         */
        public SEConditionMatcher onlyOffhand() {
            this.checkMode = HandCheckMode.OFF_HAND_ONLY;
            return this;
        }

        /**
         * 允许双手判定，优先检查主手（默认优先级）
         */
        public SEConditionMatcher allowBothHands() {
            this.checkMode = HandCheckMode.BOTH_MAIN_PRIORITY;
            return this;
        }

        /**
         * 允许双手判定，强制优先检查副手
         */
        public SEConditionMatcher allowBothHandsPrioritizeOffhand() {
            this.checkMode = HandCheckMode.BOTH_OFF_PRIORITY;
            return this;
        }

        // ================== 条件断言配置 ==================

        /**
         * 要求特定的翻译键
         */
        public SEConditionMatcher requireTranslation(String key) {
            this.requireTranslationKey = key;
            return this;
        }

        /**
         * 要求特定的模型
         */
        public SEConditionMatcher requireModel(net.minecraft.resources.ResourceLocation modelPath) {
            this.requireModel = modelPath;
            return this;
        }

        /**
         * 要求特定的SE
         */
        public SEConditionMatcher requireSE(RegistryObject<SpecialEffect> effect) {
            this.requireSE = effect;
            return this;
        }

        /**
         * 要求特定的SA
         */
        public SEConditionMatcher requireSA(Object slashArts) {
            this.requireSA = slashArts;
            return this;
        }

        // ================== 执行检查 ==================

        public BladeContext match() {
            if (entity == null)
                return null;

            // 根据当前的模式，决定执行哪个逻辑分支
            switch (checkMode) {
                case MAIN_HAND_ONLY:
                    return checkStack(entity.getMainHandItem());

                case OFF_HAND_ONLY:
                    return checkStack(entity.getOffhandItem());

                case BOTH_MAIN_PRIORITY: {
                    BladeContext mainCtx = checkStack(entity.getMainHandItem());
                    if (mainCtx != null)
                        return mainCtx;
                    return checkStack(entity.getOffhandItem());
                }

                case BOTH_OFF_PRIORITY: {
                    BladeContext offCtx = checkStack(entity.getOffhandItem());
                    if (offCtx != null)
                        return offCtx;
                    return checkStack(entity.getMainHandItem());
                }
            }
            return null; // 防御性返回
        }

        // 对单个物品栈进行核心逻辑判定
        private BladeContext checkStack(ItemStack stack) {
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemSlashBlade))
                return null;

            ISlashBladeState state = getBladeState(stack);
            IFantasySlashBladeState fdState = getFantasyBladeState(stack);
            if (state == null)
                return null;

            if (requireTranslationKey != null && !state.getTranslationKey().equals(requireTranslationKey))
                return null;
            if (requireModel != null && !state.getModel().equals(requireModel))
                return null;
            if (requireSE != null && !isSpecialEffectActive(state, requireSE, entity))
                return null;
            if (requireSA != null && !requireSA.equals(state.getSlashArts()))
                return null;

            return new BladeContext(stack, state, fdState);
        }
    }
}
