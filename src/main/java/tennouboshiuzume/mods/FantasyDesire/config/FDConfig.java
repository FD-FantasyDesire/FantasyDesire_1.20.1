package tennouboshiuzume.mods.FantasyDesire.config;

import net.minecraftforge.common.ForgeConfigSpec;

import static tennouboshiuzume.mods.FantasyDesire.potioneffect.VoidStrikeEffect.MAX_STACKS;

/**
 * 模组数值配置(服务端同步 SERVER 型，服务器为准并下发客户端)。
 * 键名前缀与 lang/注册名一致：se/slash_art/effect/damagetype . fantasydesire . <注册id> .
 * <属性>。
 * 配置数值在使用处读取，不缓存为 static final；写入弹体或效果实例的值仅对后续创建生效。
 * SERVER 配置在登录时同步客户端，在线修改不等同于向所有客户端广播热更新。
 */
public final class FDConfig {
    public static final ForgeConfigSpec SPEC;

    // ---- se.fantasydesire.* ----
    public static final SoulShield SOUL_SHIELD;
    public static final ImmortalSoul IMMORTAL_SOUL;
    public static final TyrantStrike TYRANT_STRIKE;
    public static final BloodDrain BLOOD_DRAIN;
    public static final TripleBullet TRIPLE_BULLET;
    public static final EnergyBullet ENERGY_BULLET;
    public static final ExplosiveBullet EXPLOSIVE_BULLET;
    public static final ColdLeak COLD_LEAK;
    public static final RainbowFlux RAINBOW_FLUX;
    public static final ColorFlux COLOR_FLUX;
    public static final VoidStrikeSe VOID_STRIKE_SE;
    public static final EchoingStrike ECHOING_STRIKE;
    public static final Shin SHIN;
    public static final Mang MANG;

    // ---- slash_art.fantasydesire.* ----
    public static final WingToTheFuture WING_TO_THE_FUTURE;
    public static final CrimsonStrikeSa CRIMSON_STRIKE_SA;
    public static final RainbowStar RAINBOW_STAR;
    public static final TwinSystemL TWIN_SYSTEM_L;
    public static final TwinSystemR TWIN_SYSTEM_R;
    public static final EchoingVoidSa ECHOING_VOID_SA;
    public static final FreezeZero FREEZE_ZERO;
    public static final OverCharge OVER_CHARGE;

    // ---- effect.fantasydesire.* ----
    public static final VoidStrikeEffect VOID_STRIKE_EFFECT;
    public static final EchoTimer ECHO_TIMER;
    public static final FrostStorm FROST_STORM;
    public static final DimensionBreak DIMENSION_BREAK;
    public static final CometElytra COMET_ELYTRA;
    public static final RainbowSevenEdge RAINBOW_SEVEN_EDGE;

    // ---- damagetype.fantasydesire.* ----
    public static final Resolution RESOLUTION;
    public static final Eternity ETERNITY;
    public static final Absorb ABSORB;
    public static final Lust LUST;
    public static final Gluttony GLUTTONY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SOUL_SHIELD = new SoulShield(builder);
        IMMORTAL_SOUL = new ImmortalSoul(builder);
        TYRANT_STRIKE = new TyrantStrike(builder);
        BLOOD_DRAIN = new BloodDrain(builder);
        TRIPLE_BULLET = new TripleBullet(builder);
        ENERGY_BULLET = new EnergyBullet(builder);
        EXPLOSIVE_BULLET = new ExplosiveBullet(builder);
        COLD_LEAK = new ColdLeak(builder);
        RAINBOW_FLUX = new RainbowFlux(builder);
        COLOR_FLUX = new ColorFlux(builder);
        VOID_STRIKE_SE = new VoidStrikeSe(builder);
        ECHOING_STRIKE = new EchoingStrike(builder);
        SHIN = new Shin(builder);
        MANG = new Mang(builder);
        WING_TO_THE_FUTURE = new WingToTheFuture(builder);
        CRIMSON_STRIKE_SA = new CrimsonStrikeSa(builder);
        RAINBOW_STAR = new RainbowStar(builder);
        TWIN_SYSTEM_L = new TwinSystemL(builder);
        TWIN_SYSTEM_R = new TwinSystemR(builder);
        ECHOING_VOID_SA = new EchoingVoidSa(builder);
        FREEZE_ZERO = new FreezeZero(builder);
        OVER_CHARGE = new OverCharge(builder);
        VOID_STRIKE_EFFECT = new VoidStrikeEffect(builder);
        ECHO_TIMER = new EchoTimer(builder);
        FROST_STORM = new FrostStorm(builder);
        DIMENSION_BREAK = new DimensionBreak(builder);
        COMET_ELYTRA = new CometElytra(builder);
        RAINBOW_SEVEN_EDGE = new RainbowSevenEdge(builder);
        RESOLUTION = new Resolution(builder);
        ETERNITY = new Eternity(builder);
        ABSORB = new Absorb(builder);
        LUST = new Lust(builder);
        GLUTTONY = new Gluttony(builder);
        SPEC = builder.build();
    }

    private FDConfig() {
    }

    /** 心：每 5 tick 按评级概率叠层，上限十八档，每档减伤 5%。 */
    public static class Shin {
        public final ForgeConfigSpec.ConfigValue<Double> reductionPerPoint;
        public final ForgeConfigSpec.ConfigValue<Double> reductionCap;
        public final ForgeConfigSpec.ConfigValue<Integer> rankCostPerStack;
        public final ForgeConfigSpec.ConfigValue<Double> stackChanceS;
        public final ForgeConfigSpec.ConfigValue<Double> stackChanceSs;
        public final ForgeConfigSpec.ConfigValue<Double> stackChanceSss;

        private Shin(ForgeConfigSpec.Builder builder) {
            push(builder, "se", "lc_shin", "心：初始1层，每5 tick按评级概率增加1层，上限18层，低于S或换刀清空。每秒按强度额外扣分，保留自然衰减与受击扣分。");
            reductionPerPoint = builder.defineInRange("reduction_per_point", 0.05D, 0.0D, 1.0D);
            reductionCap = builder.defineInRange("reduction_cap", 0.9D, 0.0D, 0.95D);
            rankCostPerStack = builder.defineInRange("rank_cost_per_stack", 1, 1, 300);
            stackChanceS = builder.defineInRange("stack_chance_s", 0.05D, 0.0D, 1.0D);
            stackChanceSs = builder.defineInRange("stack_chance_ss", 0.15D, 0.0D, 1.0D);
            stackChanceSss = builder.defineInRange("stack_chance_sss", 0.25D, 0.0D, 1.0D);
            builder.pop(3);
        }
    }

    /** 望：攻击伤害属性独立总乘区为 multiplierBase 的层数次方，有效命中共用扣分预算。 */
    public static class Mang {
        public final ForgeConfigSpec.ConfigValue<Double> multiplierBase;
        public final ForgeConfigSpec.ConfigValue<Integer> rankCostPerStack;
        public final ForgeConfigSpec.ConfigValue<Integer> rankCostPerSecondCap;
        public final ForgeConfigSpec.ConfigValue<Double> stackChanceS;
        public final ForgeConfigSpec.ConfigValue<Double> stackChanceSs;
        public final ForgeConfigSpec.ConfigValue<Double> stackChanceSss;
        public final ForgeConfigSpec.ConfigValue<Double> stackChanceDecay;

        private Mang(ForgeConfigSpec.Builder builder) {
            push(builder, "se", "lc_mang",
                    "望：初始1层，每5 tick按评级概率增加1层，每多一层叠层概率再乘衰减系数，上限3层，攻击伤害属性独立总乘区为倍率底数的层数次方。面板型攻击命中按层数扣分，每20 tick共用预算，不额外增强幻影剑。");
            multiplierBase = builder.defineInRange("multiplier_base", 1.25D, 1.0D, 1.5D);
            rankCostPerStack = builder.defineInRange("rank_cost_per_stack", 1, 1, 300);
            rankCostPerSecondCap = builder.defineInRange("rank_cost_per_second_cap", 8, 1, 300);
            stackChanceS = builder.defineInRange("stack_chance_s", 0.10D, 0.0D, 1.0D);
            stackChanceSs = builder.defineInRange("stack_chance_ss", 0.15D, 0.0D, 1.0D);
            stackChanceSss = builder.defineInRange("stack_chance_sss", 0.20D, 0.0D, 1.0D);
            stackChanceDecay = builder.comment("叠层概率 = 评级基础概率 × 此系数^(当前层数-1)，默认第二层升第三层的概率减半。")
                    .defineInRange("stack_chance_decay", 0.5D, 0.0D, 1.0D);
            builder.pop(3);
        }
    }

    /** 进入 <root>.fantasydesire.<id> 配置节(根前缀与 lang 键一致) */
    private static void push(ForgeConfigSpec.Builder builder, String root, String id, String comment) {
        builder.push(root);
        builder.push("fantasydesire");
        builder.comment(comment).push(id);
    }

    /** 灵魂之盾(SoulShield)：受击充能、按充能程度概率反击、失败伤害封顶并转化充能 */
    public static class SoulShield {
        public final ForgeConfigSpec.ConfigValue<Double> counterChanceMin;
        public final ForgeConfigSpec.ConfigValue<Double> counterChanceMax;
        public final ForgeConfigSpec.ConfigValue<Double> counterChanceCapRatio;
        public final ForgeConfigSpec.ConfigValue<Integer> counterFixedCharge;
        public final ForgeConfigSpec.ConfigValue<Double> counterSuccessDamageRatio;
        public final ForgeConfigSpec.ConfigValue<Integer> counterSuccessChargeCap;
        public final ForgeConfigSpec.ConfigValue<Double> counterFailDamageCap;
        public final ForgeConfigSpec.ConfigValue<Integer> counterFailChargeCap;
        public final ForgeConfigSpec.ConfigValue<Double> counterFailOverflowToCharge;

        private SoulShield(ForgeConfigSpec.Builder builder) {
            push(builder, "se", "soul_shield",
                    "灵魂之盾(SoulShield)。受击时获得灵魂碎片充能，按充能程度概率自动反击；反击失败时最终伤害封顶，溢出伤害转化为充能。");
            counterChanceMin = builder.defineInRange("counter_chance_min", 5.0D, 0.0D, 100.0D);
            counterChanceMax = builder.defineInRange("counter_chance_max", 95.0D, 0.0D, 100.0D);
            counterChanceCapRatio = builder.defineInRange("counter_chance_cap_ratio", 0.75D, 0.0D, 1.0D);
            counterFixedCharge = builder.defineInRange("counter_fixed_charge", 6, 0, Integer.MAX_VALUE);
            counterSuccessDamageRatio = builder.defineInRange("counter_success_damage_ratio", 0.33D, 0.0D, 10.0D);
            counterSuccessChargeCap = builder.defineInRange("counter_success_charge_cap", 60, 0, Integer.MAX_VALUE);
            counterFailDamageCap = builder.defineInRange("counter_fail_damage_cap", 5.0D, 0.0D, 10000.0D);
            counterFailChargeCap = builder.defineInRange("counter_fail_charge_cap", 60, 0, Integer.MAX_VALUE);
            counterFailOverflowToCharge = builder.defineInRange("counter_fail_overflow_to_charge", 1.0D, 0.0D, 10.0D);
            builder.pop(3);
        }

        public float counterChanceMin() {
            return counterChanceMin.get().floatValue();
        }

        public float counterChanceMax() {
            return counterChanceMax.get().floatValue();
        }

        public float counterChanceCapRatio() {
            return counterChanceCapRatio.get().floatValue();
        }

        public int counterFixedCharge() {
            return counterFixedCharge.get();
        }

        public float counterSuccessDamageRatio() {
            return counterSuccessDamageRatio.get().floatValue();
        }

        public int counterSuccessChargeCap() {
            return counterSuccessChargeCap.get();
        }

        public float counterFailDamageCap() {
            return counterFailDamageCap.get().floatValue();
        }

        public int counterFailChargeCap() {
            return counterFailChargeCap.get();
        }

        public float counterFailOverflowToCharge() {
            return counterFailOverflowToCharge.get().floatValue();
        }
    }

    /** 不屈之魂(ImmortalSoul)：消耗耀魂免死，永久成长面板并给予恢复效果 */
    public static class ImmortalSoul {
        public final ForgeConfigSpec.ConfigValue<Integer> reviveSoulCost;
        public final ForgeConfigSpec.ConfigValue<Double> reviveChargeRatio;
        public final ForgeConfigSpec.ConfigValue<Double> baseAttackBonus;
        public final ForgeConfigSpec.ConfigValue<Integer> maxDamageBonus;
        public final ForgeConfigSpec.ConfigValue<Double> healthRatio;
        public final ForgeConfigSpec.ConfigValue<Integer> regenDuration;
        public final ForgeConfigSpec.ConfigValue<Integer> regenAmplifier;
        public final ForgeConfigSpec.ConfigValue<Integer> resistDuration;
        public final ForgeConfigSpec.ConfigValue<Integer> resistAmplifier;

        private ImmortalSoul(ForgeConfigSpec.Builder builder) {
            push(builder, "se", "immortal_soul",
                    "不屈之魂(ImmortalSoul)。遭受致命伤害时消耗耀魂免于死亡：永久提升面板攻击与最大耐久、回复血量、清除效果并给予再生与抗性。");
            reviveSoulCost = builder.defineInRange("revive_soul_cost", 1000, 0, Integer.MAX_VALUE);
            reviveChargeRatio = builder.defineInRange("revive_charge_ratio", 0.20D, 0.0D, 1.0D);
            baseAttackBonus = builder.defineInRange("base_attack_bonus", 0.67D, 0.0D, 1000.0D);
            maxDamageBonus = builder.defineInRange("max_damage_bonus", 9, 0, Integer.MAX_VALUE);
            healthRatio = builder.defineInRange("health_ratio", 0.50D, 0.0D, 1.0D);
            regenDuration = builder.defineInRange("regen_duration", 120, 0, Integer.MAX_VALUE);
            regenAmplifier = builder.defineInRange("regen_amplifier", 4, 0, 255);
            resistDuration = builder.defineInRange("resist_duration", 120, 0, Integer.MAX_VALUE);
            resistAmplifier = builder.defineInRange("resist_amplifier", 4, 0, 255);
            builder.pop(3);
        }

        public int reviveSoulCost() {
            return reviveSoulCost.get();
        }

        public float reviveChargeRatio() {
            return reviveChargeRatio.get().floatValue();
        }

        public float baseAttackBonus() {
            return baseAttackBonus.get().floatValue();
        }

        public int maxDamageBonus() {
            return maxDamageBonus.get();
        }

        public float healthRatio() {
            return healthRatio.get().floatValue();
        }

        public int regenDuration() {
            return regenDuration.get();
        }

        public int regenAmplifier() {
            return regenAmplifier.get();
        }

        public int resistDuration() {
            return resistDuration.get();
        }

        public int resistAmplifier() {
            return resistAmplifier.get();
        }
    }

    /** 暴君一击(TyrantStrike)：消耗充能追加目标最大生命百分比的次元伤害 */
    public static class TyrantStrike {
        public final ForgeConfigSpec.ConfigValue<Double> triggerRatio;
        public final ForgeConfigSpec.ConfigValue<Double> consumeRatio;
        public final ForgeConfigSpec.ConfigValue<Double> healthPercent;
        public final ForgeConfigSpec.ConfigValue<Double> chargeDamageScale;
        public final ForgeConfigSpec.ConfigValue<Double> phantomDamage;

        private TyrantStrike(ForgeConfigSpec.Builder builder) {
            push(builder, "se", "tyrant_strike",
                    "暴君一击(TyrantStrike)。灵魂碎片达到比例时，每次命中消耗最大充能比例，追加目标最大生命百分比 + 消耗量×系数 的次元伤害并召唤幻影长枪。");
            triggerRatio = builder.defineInRange("trigger_ratio", 0.95D, 0.0D, 1.0D);
            consumeRatio = builder.defineInRange("consume_ratio", 0.20D, 0.0D, 1.0D);
            healthPercent = builder.defineInRange("health_percent", 0.08D, 0.0D, 1.0D);
            chargeDamageScale = builder.defineInRange("charge_damage_scale", 0.25D, 0.0D, 100.0D);
            phantomDamage = builder.defineInRange("phantom_damage", 1.0D, 0.0D, 1.0E9D);
            builder.pop(3);
        }

        public float triggerRatio() {
            return triggerRatio.get().floatValue();
        }

        public float consumeRatio() {
            return consumeRatio.get().floatValue();
        }

        public float healthPercent() {
            return healthPercent.get().floatValue();
        }

        public float chargeDamageScale() {
            return chargeDamageScale.get().floatValue();
        }

        public double phantomDamage() {
            return phantomDamage.get();
        }
    }

    /** 幻猎(BloodDrain)：发射抓钩幻影剑，附魔横扫之刃扩展锁距与视野角 */
    public static class BloodDrain {
        public final ForgeConfigSpec.ConfigValue<Double> lockBase;
        public final ForgeConfigSpec.ConfigValue<Double> lockPerSweep;
        public final ForgeConfigSpec.ConfigValue<Double> angleBase;
        public final ForgeConfigSpec.ConfigValue<Double> anglePerSweep;
        public final ForgeConfigSpec.ConfigValue<Integer> hitCharge;

        private BloodDrain(ForgeConfigSpec.Builder builder) {
            push(builder, "se", "blood_drain",
                    "幻猎(BloodDrain)。斩击时对扇形视野内的敌人发射追踪抓钩；锁距与视野角随横扫之刃附魔等级成长，普通命中回复血肉钩。");
            lockBase = builder.defineInRange("lock_base", 15.0D, 1.0D, 512.0D);
            lockPerSweep = builder.defineInRange("lock_per_sweep", 10.0D, 0.0D, 100.0D);
            angleBase = builder.defineInRange("angle_base", 30.0D, 1.0D, 180.0D);
            anglePerSweep = builder.defineInRange("angle_per_sweep", 10.0D, 0.0D, 45.0D);
            hitCharge = builder.defineInRange("hit_charge", 1, 0, 100);
            builder.pop(3);
        }

        public float lockBase() {
            return lockBase.get().floatValue();
        }

        public float lockPerSweep() {
            return lockPerSweep.get().floatValue();
        }

        public float angleBase() {
            return angleBase.get().floatValue();
        }

        public float anglePerSweep() {
            return anglePerSweep.get().floatValue();
        }

        public int hitCharge() {
            return hitCharge.get();
        }
    }

    /** WG-Zero 智能弹药系统(TripleBullet)：3 连发智能追踪弹 */
    public static class TripleBullet {
        public final ForgeConfigSpec.ConfigValue<Integer> ammoCost;
        public final ForgeConfigSpec.ConfigValue<Double> refineLinear;
        public final ForgeConfigSpec.ConfigValue<Double> refineSqrt;
        public final ForgeConfigSpec.ConfigValue<Double> enchantMultPerLevel;
        public final ForgeConfigSpec.ConfigValue<Integer> volleyCount;
        public final ForgeConfigSpec.ConfigValue<Double> seekAngle;
        public final ForgeConfigSpec.ConfigValue<Double> sweepRangeMult;
        public final ForgeConfigSpec.ConfigValue<Double> speed;
        public final ForgeConfigSpec.ConfigValue<Integer> delay;
        public final ForgeConfigSpec.ConfigValue<Double> lockBase;
        public final ForgeConfigSpec.ConfigValue<Integer> reloadSoulCost;
        public final ForgeConfigSpec.ConfigValue<Integer> reloadCooldown;

        private TripleBullet(ForgeConfigSpec.Builder builder) {
            push(builder, "se", "triple_bullet",
                    "WG-Zero 智能弹药系统(TripleBullet)。普通射击模式的弹道与伤害参数；伤害 = (基础攻击+重铸奖励)×(1+力量附魔×系数)，锁距 = 基础 + 横扫之刃×增量。装填与重铸奖励系数为枪刃两模式共用。");
            ammoCost = builder.defineInRange("ammo_cost", 1, 0, 64);
            refineLinear = builder.defineInRange("refine_linear", 0.1D, 0.0D, 10.0D);
            refineSqrt = builder.defineInRange("refine_sqrt", 1.5D, 0.0D, 100.0D);
            enchantMultPerLevel = builder.defineInRange("enchant_mult_per_level", 0.15D, 0.0D, 5.0D);
            volleyCount = builder.defineInRange("volley_count", 3, 1, 64);
            seekAngle = builder.defineInRange("seek_angle", 18.0D, 0.0D, 90.0D);
            builder.comment("每级横扫之刃增加的锁距(格)，普通射击与充能齐发共用。");
            sweepRangeMult = builder.defineInRange("sweep_range_mult", 5.0D, 0.0D, 100.0D);
            speed = builder.defineInRange("speed", 1.0D, 0.05D, 10.0D);
            delay = builder.defineInRange("delay", 100, 0, 1000);
            builder.comment("基础锁距(格)，普通射击与充能齐发共用。");
            lockBase = builder.defineInRange("lock_base", 15.0D, 1.0D, 512.0D);
            reloadSoulCost = builder.defineInRange("reload_soul_cost", 36, 0, Integer.MAX_VALUE);
            reloadCooldown = builder.defineInRange("reload_cooldown", 60, 0, 1000);
            builder.pop(3);
        }

        public int ammoCost() {
            return ammoCost.get();
        }

        public float refineLinear() {
            return refineLinear.get().floatValue();
        }

        public float refineSqrt() {
            return refineSqrt.get().floatValue();
        }

        public float enchantMultPerLevel() {
            return enchantMultPerLevel.get().floatValue();
        }

        public int volleyCount() {
            return volleyCount.get();
        }

        public float seekAngle() {
            return seekAngle.get().floatValue();
        }

        public float sweepRangeMult() {
            return sweepRangeMult.get().floatValue();
        }

        public float speed() {
            return speed.get().floatValue();
        }

        public int delay() {
            return delay.get();
        }

        public float lockBase() {
            return lockBase.get().floatValue();
        }

        public int reloadSoulCost() {
            return reloadSoulCost.get();
        }

        public int reloadCooldown() {
            return reloadCooldown.get();
        }
    }

    /** WG-Omega 冲击波发射器(EnergyBullet)：多弹片穿透冲击波 */
    public static class EnergyBullet {
        public final ForgeConfigSpec.ConfigValue<Integer> ammoCost;
        public final ForgeConfigSpec.ConfigValue<Double> refineLinear;
        public final ForgeConfigSpec.ConfigValue<Double> refineSqrt;
        public final ForgeConfigSpec.ConfigValue<Double> enchantMultPerLevel;
        public final ForgeConfigSpec.ConfigValue<Integer> pelletCount;
        public final ForgeConfigSpec.ConfigValue<Double> speed;
        public final ForgeConfigSpec.ConfigValue<Integer> pierce;
        public final ForgeConfigSpec.ConfigValue<Double> thunderExpRadius;
        public final ForgeConfigSpec.ConfigValue<Integer> cooldown;

        private EnergyBullet(ForgeConfigSpec.Builder builder) {
            push(builder, "se", "energy_bullet",
                    "WG-Omega 冲击波发射器(EnergyBullet)。发射多枚高速穿透能量弹；伤害 = (基础攻击+重铸奖励)×(1+力量附魔×系数)。重铸奖励系数为枪刃两模式共用。");
            ammoCost = builder.defineInRange("ammo_cost", 6, 0, 64);
            refineLinear = builder.defineInRange("refine_linear", 0.2D, 0.0D, 10.0D);
            refineSqrt = builder.defineInRange("refine_sqrt", 1.5D, 0.0D, 100.0D);
            enchantMultPerLevel = builder.defineInRange("enchant_mult_per_level", 0.15D, 0.0D, 5.0D);
            pelletCount = builder.defineInRange("pellet_count", 8, 1, 64);
            speed = builder.defineInRange("speed", 3.0D, 0.05D, 10.0D);
            pierce = builder.defineInRange("pierce", 3, 0, 16);
            thunderExpRadius = builder.defineInRange("thunder_exp_radius", 6.0D, 0.0D, 64.0D);
            cooldown = builder.defineInRange("cooldown", 10, 0, 100);
            builder.pop(3);
        }

        public int ammoCost() {
            return ammoCost.get();
        }

        public float refineLinear() {
            return refineLinear.get().floatValue();
        }

        public float refineSqrt() {
            return refineSqrt.get().floatValue();
        }

        public float enchantMultPerLevel() {
            return enchantMultPerLevel.get().floatValue();
        }

        public int pelletCount() {
            return pelletCount.get();
        }

        public float speed() {
            return speed.get().floatValue();
        }

        public int pierce() {
            return pierce.get();
        }

        public float thunderExpRadius() {
            return thunderExpRadius.get().floatValue();
        }

        public int cooldown() {
            return cooldown.get();
        }
    }

    /** WG-HyperBlast 绝肃爆裂弹头(ExplosiveBullet)：爆裂模式替换智能弹道的各项参数 */
    public static class ExplosiveBullet {
        public final ForgeConfigSpec.ConfigValue<Integer> volleyCount;
        public final ForgeConfigSpec.ConfigValue<Double> seekAngle;
        public final ForgeConfigSpec.ConfigValue<Double> sweepRangeMult;
        public final ForgeConfigSpec.ConfigValue<Double> speed;
        public final ForgeConfigSpec.ConfigValue<Integer> delay;
        public final ForgeConfigSpec.ConfigValue<Double> lockBase;
        public final ForgeConfigSpec.ConfigValue<Double> damageMult;
        public final ForgeConfigSpec.ConfigValue<Double> expRadiusBase;

        private ExplosiveBullet(ForgeConfigSpec.Builder builder) {
            push(builder, "se", "explosive_bullet",
                    "WG-HyperBlast 绝肃爆裂弹头(ExplosiveBullet)。装备时替换智能弹药系统的弹道参数(发射数量/追踪角/锁距/弹速/延迟)，伤害 ×系数，爆炸半径 = 基础 + 力量附魔等级。");
            volleyCount = builder.defineInRange("volley_count", 1, 1, 64);
            seekAngle = builder.defineInRange("seek_angle", 6.0D, 0.0D, 90.0D);
            builder.comment("每级横扫之刃增加的锁距(格)，爆裂弹普通射击与充能齐发共用。");
            sweepRangeMult = builder.defineInRange("sweep_range_mult", 15.0D, 0.0D, 100.0D);
            speed = builder.defineInRange("speed", 0.33D, 0.05D, 10.0D);
            delay = builder.defineInRange("delay", 300, 0, 1000);
            builder.comment("基础锁距(格)，爆裂弹普通射击与充能齐发共用。");
            lockBase = builder.defineInRange("lock_base", 35.0D, 1.0D, 512.0D);
            builder.comment("爆裂弹伤害倍率，普通射击与充能齐发共用。");
            damageMult = builder.defineInRange("damage_mult", 5.0D, 0.0D, 100.0D);
            builder.comment("基础爆炸半径(格)，再加力量附魔等级；普通射击与充能齐发共用。");
            expRadiusBase = builder.defineInRange("exp_radius_base", 2.0D, 0.0D, 64.0D);
            builder.pop(3);
        }

        public int volleyCount() {
            return volleyCount.get();
        }

        public float seekAngle() {
            return seekAngle.get().floatValue();
        }

        public float sweepRangeMult() {
            return sweepRangeMult.get().floatValue();
        }

        public float speed() {
            return speed.get().floatValue();
        }

        public int delay() {
            return delay.get();
        }

        public float lockBase() {
            return lockBase.get().floatValue();
        }

        public float damageMult() {
            return damageMult.get().floatValue();
        }

        public float expRadiusBase() {
            return expRadiusBase.get().floatValue();
        }
    }

    /** 寒流外溢(ColdLeak)：命中施加寒霜咬噬，tier3 额外回复进化点 */
    public static class ColdLeak {
        public final ForgeConfigSpec.ConfigValue<Integer> biteDuration;
        public final ForgeConfigSpec.ConfigValue<Integer> tier3Charge;

        private ColdLeak(ForgeConfigSpec.Builder builder) {
            push(builder, "se", "cold_leak",
                    "寒流外溢(ColdLeak)。命中时施加寒霜咬噬，等级始终跟随进化等级。tier3 命中额外获得进化点。");
            biteDuration = builder.defineInRange("bite_duration", 120, 0, Integer.MAX_VALUE);
            tier3Charge = builder.defineInRange("tier3_charge", 2, 0, 100);
            builder.pop(3);
        }

        public int biteDuration() {
            return biteDuration.get();
        }

        public int tier3Charge() {
            return tier3Charge.get();
        }
    }

    /** 虹光通量(RainbowFlux)：虹羽七刃剑状态下的七倍范围攻击 */
    public static class RainbowFlux {
        public final ForgeConfigSpec.ConfigValue<Double> areaDamageMult;

        private RainbowFlux(ForgeConfigSpec.Builder builder) {
            push(builder, "se", "rainbow_flux",
                    "虹光通量(RainbowFlux)。持有「虹羽七刃剑」效果时，将斩击替换为当前罪属性的范围攻击，伤害 = 本次斩击 × 系数。");
            areaDamageMult = builder.defineInRange("area_damage_mult", 7.0D, 0.0D, 100.0D);
            builder.pop(3);
        }

        public float areaDamageMult() {
            return areaDamageMult.get().floatValue();
        }
    }

    /** 全色汇流(ColorFlux)：满 7 层召唤七色幻影剑 */
    public static class ColorFlux {
        public final ForgeConfigSpec.ConfigValue<Double> swordDamageBase;
        public final ForgeConfigSpec.ConfigValue<Double> swordDamageAttackRatio;

        private ColorFlux(ForgeConfigSpec.Builder builder) {
            push(builder, "se", "color_flux",
                    "全色汇流(ColorFlux)FinalDMG = Base + BladeStat * Ratio * Hardcode Extra Buff");
            swordDamageBase = builder.defineInRange("sword_damage_base", 1.0D, 0.0D, 100.0D);
            swordDamageAttackRatio = builder.defineInRange("sword_damage_attack_ratio", 0.5D, 0.0D, 10.0D);
            builder.pop(3);
        }

        public float swordDamageBase() {
            return swordDamageBase.get().floatValue();
        }

        public float swordDamageAttackRatio() {
            return swordDamageAttackRatio.get().floatValue();
        }
    }

    /** 虚空强袭(SE VoidStrike)：命中叠加易伤层数 */
    public static class VoidStrikeSe {
        public final ForgeConfigSpec.ConfigValue<Integer> duration;

        private VoidStrikeSe(ForgeConfigSpec.Builder builder) {
            push(builder, "se", "void_strike",
                    "虚空强袭(VoidStrike)");
            duration = builder.defineInRange("duration", 200, 0, Integer.MAX_VALUE);
            builder.pop(3);
        }

        public int duration() {
            return duration.get();
        }
    }

    /** 回响打击(EchoingStrike)：回响伤害概率连锁 */
    public static class EchoingStrike {
        public final ForgeConfigSpec.ConfigValue<Double> chainChance;
        public final ForgeConfigSpec.ConfigValue<Double> chainRadius;
        public final ForgeConfigSpec.ConfigValue<Integer> chainCount;
        public final ForgeConfigSpec.ConfigValue<Double> chainDamageRatio;

        private EchoingStrike(ForgeConfigSpec.Builder builder) {
            push(builder, "se", "echoing_strike",
                    "回响打击(Echoing Strike)");
            chainChance = builder.defineInRange("chain_chance", 0.25D, 0.0D, 1.0D);
            chainRadius = builder.defineInRange("chain_radius", 16.0D, 0.0D, 256.0D);
            chainCount = builder.defineInRange("chain_count", 3, 1, 16);
            chainDamageRatio = builder.defineInRange("chain_damage_ratio", 0.1D, 0.0D, 10.0D);
            builder.pop(3);
        }

        public float chainChance() {
            return chainChance.get().floatValue();
        }

        public float chainRadius() {
            return chainRadius.get().floatValue();
        }

        public int chainCount() {
            return chainCount.get();
        }

        public float chainDamageRatio() {
            return chainDamageRatio.get().floatValue();
        }
    }

    /** 翱向未来之翼(WingToTheFuture)：羽翼幻影剑轰炸 */
    public static class WingToTheFuture {
        public final ForgeConfigSpec.ConfigValue<Double> swordDamageBase;
        public final ForgeConfigSpec.ConfigValue<Double> swordDamageAttackRatio;
        public final ForgeConfigSpec.ConfigValue<Double> targetRange;
        public final ForgeConfigSpec.ConfigValue<Double> targetAngle;
        public final ForgeConfigSpec.ConfigValue<Double> expRadius;
        public final ForgeConfigSpec.ConfigValue<Double> speed;

        private WingToTheFuture(ForgeConfigSpec.Builder builder) {
            push(builder, "slash_art", "wing_to_the_future",
                    "SA 翱向未来之翼 (Wing To The Future) FinalDMG = Base + BladeStat * Ratio * Hardcode Extra Buff + Vanilla Explosive Damage");
            swordDamageBase = builder.defineInRange("sword_damage_base", 1.0D, 0.0D, 100.0D);
            swordDamageAttackRatio = builder.defineInRange("sword_damage_attack_ratio", 0.5D, 0.0D, 10.0D);
            targetRange = builder.defineInRange("target_range", 35.0D, 1.0D, 512.0D);
            targetAngle = builder.defineInRange("target_angle", 20.0D, 1.0D, 180.0D);
            expRadius = builder.defineInRange("exp_radius", 3.0D, 0.0D, 64.0D);
            speed = builder.defineInRange("speed", 2.5D, 0.05D, 10.0D);
            builder.pop(3);
        }

        public float swordDamageBase() {
            return swordDamageBase.get().floatValue();
        }

        public float swordDamageAttackRatio() {
            return swordDamageAttackRatio.get().floatValue();
        }

        public float targetRange() {
            return targetRange.get().floatValue();
        }

        public float targetAngle() {
            return targetAngle.get().floatValue();
        }

        public float expRadius() {
            return expRadius.get().floatValue();
        }

        public float speed() {
            return speed.get().floatValue();
        }
    }

    public static class CrimsonStrikeSa {
        public final ForgeConfigSpec.ConfigValue<Double> huntTargetRange;
        public final ForgeConfigSpec.ConfigValue<Double> huntSeekAngle;
        public final ForgeConfigSpec.ConfigValue<Double> clawDamage;
        public final ForgeConfigSpec.ConfigValue<Double> clawScale;

        private CrimsonStrikeSa(ForgeConfigSpec.Builder builder) {
            push(builder, "slash_art", "crimson_strike",
                    "SA 深红强袭 (CrimsonStrike) FinalDMG = Base + BladeStat * Ratio * Hardcode Extra Buff");
            huntTargetRange = builder.defineInRange("hunt_target_range", 40.0D, 1.0D, 512.0D);
            huntSeekAngle = builder.defineInRange("hunt_seek_angle", 36.0D, 0.0D, 90.0D);
            clawDamage = builder.defineInRange("claw_damage", 4.0D, 0.0D, 1000.0D);
            clawScale = builder.defineInRange("claw_scale", 3.0D, 0.0D, 100.0D);
            builder.pop(3);
        }

        public float huntTargetRange() {
            return huntTargetRange.get().floatValue();
        }

        public float huntSeekAngle() {
            return huntSeekAngle.get().floatValue();
        }

        public float clawDamage() {
            return clawDamage.get().floatValue();
        }

        public float clawScale() {
            return clawScale.get().floatValue();
        }
    }

    /** Imagenation 虹光星雨(RainbowStar)：彩虹幻影剑雨 */
    public static class RainbowStar {
        public final ForgeConfigSpec.ConfigValue<Double> swordDamageBase;
        public final ForgeConfigSpec.ConfigValue<Double> swordDamageAttackRatio;
        public final ForgeConfigSpec.ConfigValue<Double> speed;

        private RainbowStar(ForgeConfigSpec.Builder builder) {
            push(builder, "slash_art", "rainbow_star",
                    "Imagenation 虹光星雨(RainbowStar)。FinalDMG = Base + BladeStat * Ratio * Hardcode Extra Buff");
            swordDamageBase = builder.defineInRange("sword_damage_base", 1.0D, 0.0D, 100.0D);
            swordDamageAttackRatio = builder.defineInRange("sword_damage_attack_ratio", 0.5D, 0.0D, 10.0D);
            speed = builder.defineInRange("speed", 5.0D, 0.05D, 10.0D);
            builder.pop(3);
        }

        public float swordDamageBase() {
            return swordDamageBase.get().floatValue();
        }

        public float swordDamageAttackRatio() {
            return swordDamageAttackRatio.get().floatValue();
        }

        public float speed() {
            return speed.get().floatValue();
        }
    }

    /** 启动程式：MOOD(TwinSystemL) */
    public static class TwinSystemL {
        public final ForgeConfigSpec.ConfigValue<Double> rippedRange;
        public final ForgeConfigSpec.ConfigValue<Double> rippedAngle;

        private TwinSystemL(ForgeConfigSpec.Builder builder) {
            push(builder, "slash_art", "twin_system_l",
                    "启动程式：MOOD (TwinSystemL)");
            rippedRange = builder.defineInRange("ripped_range", 35.0D, 1.0D, 512.0D);
            rippedAngle = builder.defineInRange("ripped_angle", 25.0D, 1.0D, 180.0D);
            builder.pop(3);
        }

        public float rippedRange() {
            return rippedRange.get().floatValue();
        }

        public float rippedAngle() {
            return rippedAngle.get().floatValue();
        }

    }

    /** 终结程式：DOOM(TwinSystemR) */
    public static class TwinSystemR {
        public final ForgeConfigSpec.ConfigValue<Double> dominateRadius;
        public final ForgeConfigSpec.ConfigValue<Double> doomSelfDamage;

        private TwinSystemR(ForgeConfigSpec.Builder builder) {
            push(builder, "slash_art", "twin_system_r",
                    "终结程式：DOOM (TwinSystemR)");
            dominateRadius = builder.defineInRange("dominate_radius", 15.0D, 1.0D, 512.0D);
            doomSelfDamage = builder.defineInRange("doom_self_damage", 2.0D, 0.0D, 100.0D);
            builder.pop(3);
        }

        public float dominateRadius() {
            return dominateRadius.get().floatValue();
        }

        public float doomSelfDamage() {
            return doomSelfDamage.get().floatValue();
        }

    }

    /** 宙极崩毁(EchoingVoid)：叠层 + 多段范围回响斩 */
    public static class EchoingVoidSa {
        public final ForgeConfigSpec.ConfigValue<Integer> voidStrikeStacks;

        private EchoingVoidSa(ForgeConfigSpec.Builder builder) {
            push(builder, "slash_art", "echoing_void",
                    "宙极崩毁(EchoingVoid)");
            voidStrikeStacks = builder.defineInRange("void_strike_stacks", 10, 1, MAX_STACKS);
            builder.pop(3);
        }

        public int voidStrikeStacks() {
            return voidStrikeStacks.get();
        }
    }

    /** 永冻：零世代(FreezeZero)：风暴展开与叠加 */
    public static class FreezeZero {
        public final ForgeConfigSpec.ConfigValue<Double> baseDurationSec;
        public final ForgeConfigSpec.ConfigValue<Double> baseDurationPerTierSec;
        public final ForgeConfigSpec.ConfigValue<Double> stackExtensionPerTierSec;
        public final ForgeConfigSpec.ConfigValue<Integer> stackExtensionMinTick;
        public final ForgeConfigSpec.ConfigValue<Integer> ampCap;

        private FreezeZero(ForgeConfigSpec.Builder builder) {
            push(builder, "slash_art", "freeze_zero",
                    "永冻：零世代(FreezeZero)");
            baseDurationSec = builder.defineInRange("base_duration_sec", 6.0D, 0.0D, 3600.0D);
            baseDurationPerTierSec = builder.defineInRange("base_duration_per_tier_sec", 3.0D, 0.0D, 3600.0D);
            stackExtensionPerTierSec = builder.defineInRange("stack_extension_per_tier_sec", 3.0D, 0.0D, 3600.0D);
            stackExtensionMinTick = builder.defineInRange("stack_extension_min_tick", 30, 0, 1000);
            ampCap = builder.defineInRange("amp_cap", 14, 0, 255);
            builder.pop(3);
        }

        public float baseDurationSec() {
            return baseDurationSec.get().floatValue();
        }

        public float baseDurationPerTierSec() {
            return baseDurationPerTierSec.get().floatValue();
        }

        public float stackExtensionPerTierSec() {
            return stackExtensionPerTierSec.get().floatValue();
        }

        public int stackExtensionMinTick() {
            return stackExtensionMinTick.get();
        }

        public int ampCap() {
            return ampCap.get();
        }
    }

    /** 超载充能(OverCharge)：BFG 巨型能量球 */
    public static class OverCharge {
        public final ForgeConfigSpec.ConfigValue<Double> bfgExpRadius;

        private OverCharge(ForgeConfigSpec.Builder builder) {
            push(builder, "slash_art", "over_charge",
                    "超载充能(OverCharge)");
            bfgExpRadius = builder.defineInRange("bfg_exp_radius", 25.0D, 0.0D, 128.0D);
            builder.pop(3);
        }

        public float bfgExpRadius() {
            return bfgExpRadius.get().floatValue();
        }
    }

    /** 虚空强袭(药水效果 VoidStrike)：易伤乘区 */
    public static class VoidStrikeEffect {
        public final ForgeConfigSpec.ConfigValue<Double> damagePerLayer;

        private VoidStrikeEffect(ForgeConfigSpec.Builder builder) {
            push(builder, "effect", "void_strike",
                    "虚空强袭(VoidStrike)");
            damagePerLayer = builder.defineInRange("damage_per_layer", 0.10D, 0.0D, 5.0D);
            builder.pop(3);
        }

        public float damagePerLayer() {
            return damagePerLayer.get().floatValue();
        }
    }

    /** 回响计时(EchoTimer)：回响伤害引爆倒计时 */
    public static class EchoTimer {
        public final ForgeConfigSpec.ConfigValue<Integer> duration;

        private EchoTimer(ForgeConfigSpec.Builder builder) {
            push(builder, "effect", "echo_timer",
                    "回响计时(EchoTimer)");
            duration = builder.defineInRange("duration", 60, 0, Integer.MAX_VALUE);
            builder.pop(3);
        }

        public int duration() {
            return duration.get();
        }
    }

    /** 寒霜风暴(FrostStorm)：领域、咬噬与持续强度成长 */
    public static class FrostStorm {
        public final ForgeConfigSpec.ConfigValue<Double> radiusBase;
        public final ForgeConfigSpec.ConfigValue<Double> radiusPerAmp;
        public final ForgeConfigSpec.ConfigValue<Double> radiusCap;
        public final ForgeConfigSpec.ConfigValue<Integer> biteDuration;
        public final ForgeConfigSpec.ConfigValue<Double> strengthGrowthPerSecond;
        public final ForgeConfigSpec.ConfigValue<Double> strengthCap;

        private FrostStorm(ForgeConfigSpec.Builder builder) {
            push(builder, "effect", "frost_storm",
                    "寒霜风暴(FrostStorm)");
            radiusBase = builder.defineInRange("radius_base", 4.0D, 0.0D, 64.0D);
            radiusPerAmp = builder.defineInRange("radius_per_amp", 3.0D, 0.0D, 64.0D);
            radiusCap = builder.defineInRange("radius_cap", 16.0D, 0.0D, 64.0D);
            biteDuration = builder.defineInRange("bite_duration", 100, 0, Integer.MAX_VALUE);
            strengthGrowthPerSecond = builder.defineInRange("strength_growth_per_second", 0.05D, 0.0D, 10.0D);
            strengthCap = builder.defineInRange("strength_cap", 10.0D, 1.0D, 10.0D);
            builder.pop(3);
        }

        public float radiusBase() {
            return radiusBase.get().floatValue();
        }

        public float radiusPerAmp() {
            return radiusPerAmp.get().floatValue();
        }

        public float radiusCap() {
            return radiusCap.get().floatValue();
        }

        public int biteDuration() {
            return biteDuration.get();
        }

        public float strengthGrowthPerSecond() {
            return strengthGrowthPerSecond.get().floatValue();
        }

        public float strengthCap() {
            return strengthCap.get().floatValue();
        }
    }

    /** 次元崩解(DimensionBreak)：持续削减最大生命 */
    public static class DimensionBreak {
        public final ForgeConfigSpec.ConfigValue<Double> reducePerSec;
        public final ForgeConfigSpec.ConfigValue<Double> reduceCap;

        private DimensionBreak(ForgeConfigSpec.Builder builder) {
            push(builder, "effect", "dimension_break",
                    "次元崩解(DimensionBreak)");
            reducePerSec = builder.defineInRange("reduce_per_sec", 0.01D, 0.0D, 1.0D);
            reduceCap = builder.defineInRange("reduce_cap", -0.99D, -1.0D, 0.0D);
            builder.pop(3);
        }

        public float reducePerSec() {
            return reducePerSec.get().floatValue();
        }

        public float reduceCap() {
            return reduceCap.get().floatValue();
        }
    }

    /** 彗星羽翼(CometElytra)：飞行增益 + 撞墙/坠落猛击联动 */
    public static class CometElytra {
        public final ForgeConfigSpec.ConfigValue<Integer> baseDuration;
        public final ForgeConfigSpec.ConfigValue<Double> clashWeaponDamageMult;
        public final ForgeConfigSpec.ConfigValue<Double> clashExplosionRadius;
        public final ForgeConfigSpec.ConfigValue<Double> clashHealthPercent;

        private CometElytra(ForgeConfigSpec.Builder builder) {
            push(builder, "effect", "comet_elytra",
                    "彗星羽翼(CometElytra)");
            baseDuration = builder.defineInRange("base_duration", 1200, 0, Integer.MAX_VALUE);
            clashWeaponDamageMult = builder.defineInRange("clash_weapon_damage_mult", 10.0D, 0.0D, 1000.0D);
            clashExplosionRadius = builder.defineInRange("clash_explosion_radius", 15.0D, 0.0D, 128.0D);
            clashHealthPercent = builder.defineInRange("clash_health_percent", 0.1D, 0.0D, 1.0D);
            builder.pop(3);
        }

        public int baseDuration() {
            return baseDuration.get();
        }

        public float clashWeaponDamageMult() {
            return clashWeaponDamageMult.get().floatValue();
        }

        public float clashExplosionRadius() {
            return clashExplosionRadius.get().floatValue();
        }

        public float clashHealthPercent() {
            return clashHealthPercent.get().floatValue();
        }
    }

    /** 虹羽七刃剑(RainbowSevenEdge)：由 SA 施加的时长 */
    public static class RainbowSevenEdge {
        public final ForgeConfigSpec.ConfigValue<Integer> castDuration;

        private RainbowSevenEdge(ForgeConfigSpec.Builder builder) {
            push(builder, "effect", "rainbow_seven_edge",
                    "虹羽七刃剑(RainbowSevenEdge)");
            castDuration = builder.defineInRange("cast_duration", 280, 0, Integer.MAX_VALUE);
            builder.pop(3);
        }

        public int castDuration() {
            return castDuration.get();
        }
    }

    /** 决断(Resolution)：追加一半魔法伤害 */
    public static class Resolution {
        public final ForgeConfigSpec.ConfigValue<Double> extraRatio;

        private Resolution(ForgeConfigSpec.Builder builder) {
            push(builder, "damagetype", "resolution",
                    "决断(Resolution)");
            extraRatio = builder.defineInRange("extra_ratio", 0.5D, 0.0D, 10.0D);
            builder.pop(3);
        }

        public float extraRatio() {
            return extraRatio.get().floatValue();
        }
    }

    /** 永劫(Eternity)：每次削最大生命上限 */
    public static class Eternity {
        public final ForgeConfigSpec.ConfigValue<Double> reduceRatio;

        private Eternity(ForgeConfigSpec.Builder builder) {
            push(builder, "damagetype", "eternity",
                    "永劫(Eternity)");
            reduceRatio = builder.defineInRange("reduce_ratio", 0.1D, 0.0D, 1.0D);
            builder.pop(3);
        }

        public float reduceRatio() {
            return reduceRatio.get().floatValue();
        }
    }

    /** 吸收(Absorb)：全额治疗攻击者 */
    public static class Absorb {
        public final ForgeConfigSpec.ConfigValue<Double> overflowRatio;
        public final ForgeConfigSpec.ConfigValue<Double> absorbCap;

        private Absorb(ForgeConfigSpec.Builder builder) {
            push(builder, "damagetype", "absorb",
                    "吸收(Absorb)");
            overflowRatio = builder.defineInRange("overflow_ratio", 0.1D, 0.0D, 1.0D);
            absorbCap = builder.defineInRange("absorb_cap", 20.0D, 0.0D, 100.0D);
            builder.pop(3);
        }

        public float overflowRatio() {
            return overflowRatio.get().floatValue();
        }

        public float absorbCap() {
            return absorbCap.get().floatValue();
        }
    }

    /** 色欲(Lust)：生命虹吸 */
    public static class Lust {
        public final ForgeConfigSpec.ConfigValue<Double> healRatio;

        private Lust(ForgeConfigSpec.Builder builder) {
            push(builder, "damagetype", "lust",
                    "色欲(Lust)");
            healRatio = builder.defineInRange("heal_ratio", 0.05D, 0.0D, 1.0D);
            builder.pop(3);
        }

        public float healRatio() {
            return healRatio.get().floatValue();
        }
    }

    /** 暴食(Gluttony)：血肉转化 */
    public static class Gluttony {
        public final ForgeConfigSpec.ConfigValue<Double> foodRatio;
        public final ForgeConfigSpec.ConfigValue<Double> absorbRatio;
        public final ForgeConfigSpec.ConfigValue<Double> absorbCap;

        private Gluttony(ForgeConfigSpec.Builder builder) {
            push(builder, "damagetype", "gluttony",
                    "暴食(Gluttony)");
            foodRatio = builder.defineInRange("food_ratio", 0.1D, 0.0D, 1.0D);
            absorbRatio = builder.defineInRange("absorb_ratio", 0.5D, 0.0D, 1.0D);
            absorbCap = builder.defineInRange("absorb_cap", 10.0D, 0.0D, 100.0D);
            builder.pop(3);
        }

        public float foodRatio() {
            return foodRatio.get().floatValue();
        }

        public float absorbRatio() {
            return absorbRatio.get().floatValue();
        }

        public float absorbCap() {
            return absorbCap.get().floatValue();
        }
    }
}
