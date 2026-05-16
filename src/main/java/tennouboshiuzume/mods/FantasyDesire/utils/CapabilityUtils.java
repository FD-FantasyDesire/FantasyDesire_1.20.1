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

import javax.annotation.Nullable;
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

    // ------------------ 耀魂扣除工具方法 ------------------

    /**
     * 尝试从 BladeState 中扣除指定数量的耀魂（ProudSoul）。
     * 创造模式玩家不消耗耀魂。
     *
     * @param state  BladeState，不能为 null
     * @param cost   要扣除的耀魂数量，必须 >= 0
     * @param entity 持有该刀的实体（用于判断创造模式），可为 null
     * @param blade  对应的 ItemStack（用于耀魂不足时消耗耐久降级），可为 null
     * @return 如果操作成功（扣除耀魂 / 创造模式 / 消耗耐久）返回 true；否则返回 false
     */
    public static boolean tryConsumeProudSoul(ISlashBladeState state, int cost, @Nullable LivingEntity entity,
            @Nullable ItemStack blade) {
        if (state == null || cost < 0) {
            return false;
        }
        if (cost == 0) {
            return true;
        }
        // 创造模式不消耗耀魂
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return true;
        }
        if (state.getProudSoulCount() < cost) {
            // 耀魂不足时，如果提供了 blade 则消耗耐久作为降级处理
            if (blade != null && entity != null) {
                blade.hurtAndBreak(1, entity, (e) -> {
                });
                return true;
            }
            return false;
        }
        state.setProudSoulCount(state.getProudSoulCount() - cost);
        return true;
    }

    /**
     * 尝试从 BladeState 中扣除指定数量的耀魂（ProudSoul）。
     * 重载版本：不传入实体和物品栈，保持原有行为（无创造模式豁免，无耐久降级）。
     *
     * @param state BladeState，不能为 null
     * @param cost  要扣除的耀魂数量，必须 >= 0
     * @return 如果耀魂足够且成功扣除返回 true；否则返回 false
     */
    public static boolean tryConsumeProudSoul(ISlashBladeState state, int cost) {
        return tryConsumeProudSoul(state, cost, null, null);
    }

    // ------------------ 特殊充能扣除工具方法 ------------------

    /**
     * 尝试从 FantasyBladeState 中扣除指定数量的特殊充能（SpecialCharge）。
     * 创造模式玩家不消耗特殊充能。
     *
     * @param fdState FantasyBladeState，不能为 null
     * @param cost    要扣除的特殊充能数量，必须 >= 0
     * @param entity  持有该刀的实体（用于判断创造模式），可为 null
     * @param blade   对应的 ItemStack（用于特殊充能不足时消耗耐久降级），可为 null
     * @return 如果操作成功（扣除特殊充能 / 创造模式 / 消耗耐久）返回 true；否则返回 false
     */
    public static boolean tryConsumeSpecialCharge(IFantasySlashBladeState fdState, int cost, @Nullable LivingEntity entity,
            @Nullable ItemStack blade) {
        if (fdState == null || cost < 0) {
            return false;
        }
        if (cost == 0) {
            return true;
        }
        // 创造模式不消耗特殊充能
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return true;
        }
        if (fdState.getSpecialCharge() < cost) {
            // 特殊充能不足时，如果提供了 blade 则消耗耐久作为降级处理
            if (blade != null && entity != null) {
                blade.hurtAndBreak(1, entity, (e) -> {
                });
                return true;
            }
            return false;
        }
        fdState.setSpecialCharge(fdState.getSpecialCharge() - cost);
        return true;
    }

    /**
     * 尝试从 FantasyBladeState 中扣除指定数量的特殊充能（SpecialCharge）。
     * 重载版本：不传入实体和物品栈，保持原有行为（无创造模式豁免，无耐久降级）。
     *
     * @param fdState FantasyBladeState，不能为 null
     * @param cost    要扣除的特殊充能数量，必须 >= 0
     * @return 如果特殊充能足够且成功扣除返回 true；否则返回 false
     */
    public static boolean tryConsumeSpecialCharge(IFantasySlashBladeState fdState, int cost) {
        return tryConsumeSpecialCharge(fdState, cost, null, null);
    }

    /**
     * 检查特殊充能是否足够
     *
     * @param fdState FantasyBladeState，不能为 null
     * @param cost    需要的特殊充能数量
     * @return 如果特殊充能足够返回 true；否则返回 false
     */
    public static boolean hasEnoughSpecialCharge(IFantasySlashBladeState fdState, int cost) {
        if (fdState == null || cost < 0) {
            return false;
        }
        return fdState.getSpecialCharge() >= cost;
    }

    /**
     * 添加特殊充能，不超过最大值
     *
     * @param fdState FantasyBladeState，不能为 null
     * @param amount  要添加的特殊充能数量
     * @return 实际添加的数量（可能因为达到上限而减少）
     */
    public static int addSpecialCharge(IFantasySlashBladeState fdState, int amount) {
        if (fdState == null || amount <= 0) {
            return 0;
        }
        int currentCharge = fdState.getSpecialCharge();
        int maxCharge = fdState.getMaxSpecialCharge();
        int spaceAvailable = maxCharge - currentCharge;
        int actualAdd = Math.min(amount, spaceAvailable);
        fdState.setSpecialCharge(currentCharge + actualAdd);
        return actualAdd;
    }

    /**
     * 设置特殊充能，确保在有效范围内
     *
     * @param fdState FantasyBladeState，不能为 null
     * @param amount  要设置的特殊充能数量
     */
    public static void setSpecialCharge(IFantasySlashBladeState fdState, int amount) {
        if (fdState == null) {
            return;
        }
        int maxCharge = fdState.getMaxSpecialCharge();
        int clampedAmount = Math.max(0, Math.min(amount, maxCharge));
        fdState.setSpecialCharge(clampedAmount);
    }

    /**
     * 获取特殊充能的百分比（0.0 到 1.0）
     *
     * @param fdState FantasyBladeState，不能为 null
     * @return 特殊充能的百分比，如果最大充能为0则返回0
     */
    public static float getSpecialChargePercentage(IFantasySlashBladeState fdState) {
        if (fdState == null || fdState.getMaxSpecialCharge() == 0) {
            return 0.0f;
        }
        return (float) fdState.getSpecialCharge() / (float) fdState.getMaxSpecialCharge();
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
            // 如果传入了 directStack，直接检查该物品栈（忽略 entity 是否为 null）
            if (directStack != null) {
                return checkStack(directStack);
            }

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