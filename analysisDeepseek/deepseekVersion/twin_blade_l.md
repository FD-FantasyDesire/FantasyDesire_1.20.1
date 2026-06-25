# 双子圣灵·左（TwinBladeL）深度分析文档

> **对应形态：** TWIN_SYSTEM_L（启动程式：MOOD 心境）
> **注册名：** `twin_blade_l`
> **特效色：** `0x00C8FF`（青蓝）
> **纹理：** [`twinbladeleft.png`](../../src/main/resources/assets/fantasydesire/models/twinbladeleft.png)

---

## 一、基础属性

| 属性                        | 值                                                                                                                                                                        |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **注册名**                  | `twin_blade_l`                                                                                                                                                            |
| **翻译键**                  | `item.fantasydesire.twin_blade`（与R共享）                                                                                                                                |
| **特效颜色**                | `0x00C8FF`（青蓝）                                                                                                                                                        |
| **纹理**                    | `twinbladeleft.png`                                                                                                                                                       |
| **模型**                    | [`twinblade.obj`](../../src/main/resources/assets/fantasydesire/models/twinblade.obj)（与R共享）                                                                          |
| **携带方式**                | `KATANA`                                                                                                                                                                  |
| **基础攻击修正**            | `2.5F`                                                                                                                                                                    |
| **最大伤害**                | `1024`                                                                                                                                                                    |
| **剑类型**                  | `BEWITCHED`                                                                                                                                                               |
| **特殊能量**                | 无（未设置 `maxSpecialCharge`）                                                                                                                                           |
| **特殊类型（specialType）** | `TwinBladeL`                                                                                                                                                              |
| **特殊攻击效果**            | [`resolution`](#resolution伤害类型详解)                                                                                                                                   |
| **SA（Slash Arts）**        | [`TWIN_SYSTEM_L`](#二sa技能分析twin_system_l--mood_slash心境连段) → [`FDCombo.MOOD_SLASH`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:232) |
| **SE（Special Effect）**    | [`TwinSet`](#三特殊效果机制分析)（双刀共鸣）                                                                                                                              |

### 附魔

| 附魔                           | 等级 |
| ------------------------------ | ---- |
| `FLAMING_ARROWS`（火焰箭）     | V    |
| `BLAST_PROTECTION`（爆炸保护） | III  |
| `FIRE_PROTECTION`（火焰保护）  | III  |
| `INFINITY_ARROWS`（无限）      | I    |

> **注册代码位置：** [`FantasySlashBladeBuiltInRegistry.java:178-206`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:178)

### 与 TwinBladeR 的差异对比

| 属性        | TwinBladeL（左）                                                                                                          | TwinBladeR（右）                                                                                                          |
| ----------- | ------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| 特效色      | `0x00C8FF` 青蓝                                                                                                           | `0xFF0089` 粉红                                                                                                           |
| 纹理        | `twinbladeleft.png`                                                                                                       | `twinbladeright.png`                                                                                                      |
| specialType | `TwinBladeL`                                                                                                              | `TwinBladeR`                                                                                                              |
| SA          | [`TWIN_SYSTEM_L` → `MOOD_SLASH`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:18) | [`TWIN_SYSTEM_R` → `DOOM_SLASH`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:20) |
| 连段风格    | **心境连段**（MOOD）：蓄力瞬步→跃升斩→双旋斩→重锤落+符文剑                                                                | **末日连段**（DOOM）：瞬移→预热乱舞→烧血循环→终结重击                                                                     |

---

## 二、SA技能分析：TWIN_SYSTEM_L → MOOD_SLASH（心境连段）

### 2.1 整体设计理念

`TWIN_SYSTEM_L` 注册于 [`FDSlashArtRegistry.java:18`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:18)，其连段入口为 [`FDCombo.MOOD_SLASH`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:232)。

> 代码注释原文："参考自黑兽·卯 奥义 云解显现。蓄力，瞬步跃升斩将敌人上斩滞空。回旋斩击2次后重锤落。并且召唤幻影剑追加攻击。"

L形态的MOOD连段是一套**高机动空战型combo**，核心思路为：**RippedStep瞬步突进 → 跃升斩浮空 → 双旋斩控场 → 重锤落地 + 幻影符文剑终结**。

### 2.2 连段结构详解

#### 阶段0：施放前检测 [`MOOD_SLASH`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:232)

```java
// 伪逻辑
if (TwinSlash.AntiNTR(entity))
    → mood_slash_0  // 双持验证通过，进入连段
else
    → twin_mode     // 未双持，进入形态切换模式
```

- **优先级：** 80
- **核心验证：** [`TwinSlash.AntiNTR()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/TwinSlash.java:181) 检查主副手是否均为 `twin_blade` 且 `specialType` 不同（L≠R）
- 若未满足双持条件，则回退到 [`TWIN_MODE`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:218)（形态切换）

#### 阶段1：滑步突击 [`MOOD_SLASH_0`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:243)

| 属性     | 值                         |
| -------- | -------------------------- |
| 帧范围   | 1-33                       |
| 优先级   | 80                         |
| 动画     | `testLocation`（默认姿势） |
| 超时跳转 | 第31帧→ `mood_slash_1`     |

**核心动作：**

- 第30帧（约1.5秒动画后）调用 [`TwinSlash.RippedStep()`](#rippedstep-裂空瞬步) 执行**裂空瞬步**
- 这是MOOD连段的核心位移机制

#### 阶段2：跃升斩 [`MOOD_SLASH_1`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:255)

| 属性   | 值                 |
| ------ | ------------------ |
| 帧范围 | 1700-1713          |
| 优先级 | 80                 |
| 动画   | `ExMotionLocation` |

**核心动作：**

- 第7帧：
  - `entityIn.setDeltaMovement(motion.x, 0.6f, motion.z)` — 将玩家向上推动 0.6（**跃升浮空**）
  - `AttackManager.doSlash(entityIn, -90+10, ...)` — 前方上斩
  - `AttackManager.doSlash(entityIn, -90-10, ...)` — 后方上斩
  - 击退效果：`KnockBacks.toss`（**抛掷**）

**击中效果：**

- 目标被向上推动 0.6（**滞空**）
- 目标获得 100 tick（5秒）`SLOW_FALLING`（缓慢坠落）效果
- 目标眩晕 15 tick

> **战术意义：** 将目标击飞至空中并给予缓慢坠落，为后续空中连段创造窗口

#### 阶段3：双旋斩 [`MOOD_SLASH_2`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:275)

| 属性     | 值                     |
| -------- | ---------------------- |
| 帧范围   | 725-743                |
| 优先级   | 80                     |
| 超时跳转 | 第12帧→ `mood_slash_3` |

**核心动作（第1-10 tick）：**

| Tick | 使用手   | 动作                                | 参数说明      |
| ---- | -------- | ----------------------------------- | ------------- |
| 1    | 主手     | `MoodSlash(roll=45°, offset=72)`    | 主手旋斩1     |
| 2    | 主手     | `MoodSlash(roll=45°, offset=0)`     | 主手旋斩2     |
| 3    | 主手     | `MoodSlash(roll=45°, offset=-72)`   | 主手旋斩3     |
| 4    | 主手     | `MoodSlash(roll=45°, offset=-144)`  | 主手旋斩4     |
| 5    | 主手     | `MoodSlash(roll=45°, offset=-216)`  | 主手旋斩5     |
| 6    | **副手** | `MoodSlash(roll=135°, offset=72)`   | **副手旋斩1** |
| 7    | **副手** | `MoodSlash(roll=135°, offset=0)`    | **副手旋斩2** |
| 8    | **副手** | `MoodSlash(roll=135°, offset=-72)`  | **副手旋斩3** |
| 9    | **副手** | `MoodSlash(roll=135°, offset=-144)` | **副手旋斩4** |
| 10   | **副手** | `MoodSlash(roll=135°, offset=-216)` | **副手旋斩5** |

> **关键观察：** 主手5次斩击（roll=45°）+ 副手5次斩击（roll=135°），形成**左右交替的360°回旋斩击**。副手斩击使用 `getOffhandItem()` 获取副手武器。

**击中效果：**

- 目标眩晕 40 tick（2秒）
- 触发 [`TwinSlash.HitEffect()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/TwinSlash.java:162) — 随机青蓝/粉红 `GlowingLineParticle` 粒子特效

**特殊机制：** `FallHandler::fallDecrease` — 减少下落距离，维持滞空

#### 阶段4：重锤落斩 + 幻影剑追击 [`MOOD_SLASH_3`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:307)

| 属性     | 值                   |
| -------- | -------------------- |
| 帧范围   | 500-576              |
| 优先级   | 80                   |
| 动画     | `ExMotionLocation`   |
| 超时跳转 | → `none`（连段结束） |

**核心动作（第8 tick）：**

1. `AttackManager.doSlash(entityIn, 90-15, false, false, 2.875f)` — **前方重锤**（伤害系数 2.875）
2. `AttackManager.doSlash(entityIn, 90+15, true, false, 2.875f)` — **后方重锤**
3. `entityIn.moveRelative(0.8f, new Vec3(0, -0.5, 1.25))` — **快速下坠突进**
4. **关键：** [`TwinSlash.ConvertForm()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/TwinSlash.java:202) 同时转换**主手和副手**的形态！
   - 主手 `TwinBladeL` → `TwinBladeR`（纹理、SA、颜色全部切换）
   - 副手 `TwinBladeR` → `TwinBladeL`

**击中效果（触发幻影符文剑）：**

- 目标眩晕 40 tick
- 调用 [`TwinSlash.MoodFinalRuneSword()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/TwinSlash.java:115) **2次**（主手+副手各一次）
  - 在目标周围 3格 随机位置生成 [`EntityFDPhantomSword`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityFDPhantomSword.java)
  - **伤害 = 玩家最大生命值的 25%**（`player.getMaxHealth() * 0.25`）
  - 飞行模式：`NORMAL`（直线穿透）
  - 粒子效果：`ENCHANT`
  - 发射音效：`TRIDENT_THUNDER`（三叉戟雷击声）
  - 40 tick 延迟后发射
  - 无碰撞（`setNoClip(true)`），穿透追踪

### 2.3 MOOD连段总览图

```
TWIN_SYSTEM_L (SA激活)
    │
    ├─ AntiNTR() 验证失败 → TWIN_MODE（形态切换）
    │
    └─ AntiNTR() 验证通过 → MOOD_SLASH_0 (帧1-33)
         │ 第30帧: RippedStep() 裂空瞬步
         ▼
         MOOD_SLASH_1 (帧1700-1713)
         │ 第7帧: 跃升斩 + 浮空效果
         ▼
         MOOD_SLASH_2 (帧725-743)
         │ 第1-10帧: 主副手交替回旋斩 (10连击)
         ▼
         MOOD_SLASH_3 (帧500-576)
         │ 第8帧: 重锤落地 + ConvertForm()形态转换
         │ 击中: MoodFinalRuneSword x2 (25%生命伤害)
         ▼
         none (连段结束)
```

---

## 三、特殊效果机制分析

### 3.1 TwinSet（双刀共鸣）SE

**注册位置：** [`FDSpecialEffectsRegistry.java:37`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:37)

**事件监听器：** [`TwinBladeEffects.onTwinSlash()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/twinblade/TwinBladeEffects.java:19)

这是双子圣灵双刀流玩法的**核心机制**。逻辑如下：

```java
@SubscribeEvent
public static void onTwinSlash(SlashBladeEvent.DoSlashEvent event) {
    // 1. 验证主手：必须是 twin_blade 且拥有 TwinSet SE
    // 2. 验证副手：必须是 twin_blade 且拥有 TwinSet SE
    // 3. 验证 specialType 不同（L ≠ R）
    // 4. 满足条件 → 副手同步追加斩击
    AddonSlashUtils.doAddonSlash(
        player,
        roll - 180,   // 副手攻击方向与主手相反（180°反向）
        player.getYRot(),
        0,
        offColor,     // 副手特效色（L青蓝 / R粉红）
        0,
        Vec3.ZERO,
        false, false,
        damage,       // 副手复制主手伤害
        KnockBacks.cancel
    );
}
```

**机制详解：**

| 条件     | 说明                                                |
| -------- | --------------------------------------------------- |
| 主手要求 | `item.fantasydesire.twin_blade` + 拥有 `TwinSet` SE |
| 副手要求 | 同上（仅限副手槽）                                  |
| 形态要求 | 主副手 `specialType` 必须不同（一个L一个R）         |
| 触发时机 | 每次 `DoSlashEvent`（每次拔刀剑斩击事件）           |
| 副手攻击 | `roll - 180`，即主手斩击的**反向180°**，形成交叉斩  |
| 伤害复制 | 副手继承主手 `event.getDamage()` 的完整伤害值       |
| 特效色   | 使用副手自身的 `colorCode`（L=青蓝，R=粉红）        |

> **双刀共鸣的战术价值：** 每次主手攻击，副手自动追加一次反向斩击，等同于**每次攻击造成双倍伤害**。这是双刀流 dps 的核心来源。

### 3.2 Resolution（决断）伤害类型

**注册位置：** [`FantasySlashBladeBuiltInRegistry.java:197`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:197)

**伤害处理：** [`DamageConverterEvent.OnSlash()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java:50) 和 [`DamageConverterEvent.OnHurt()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java:91)

**实现流程：**

1. **伤害替换（`DoSlashEvent` 阶段）：**
   - 检测到 `specialAttackEffect = "resolution"`
   - 将原始斩击伤害替换为 [`FDDamageSource.RESOLUTION`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/damagesource/FDDamageSource.java) 类型的伤害
   - 原始伤害设为 0（避免重复计算）

2. **伤害追加（`LivingHurtEvent` 阶段）：**
   ```java
   // Resolution: 追加本次伤害50%的魔法伤害
   if (source.is(FDDamageSource.RESOLUTION)) {
       float extraDamage = amount * 0.5f;
       resetInvulnerable(target);  // 重置无敌帧
       target.hurt(attackerLiving.damageSources().magic(), extraDamage);
       resetInvulnerable(target);  // 再次重置无敌帧
   }
   ```

**效果总结：**

- 所有伤害的 **50% 额外魔法伤害追加**
- **重置无敌帧 2 次**，确保追加伤害不被免疫
- 相当于所有攻击的 **总伤害倍率为 1.5x**（物理伤害 + 50%魔法伤害）
- 魔法伤害无视部分护甲，对高护甲目标效果更佳

### 3.3 VoidTransform（虚空转化）

**文件：** [`VoidTransform.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/twinblade/VoidTransform.java)

这是获得双子圣灵的**隐藏获取方式**：

| 条件     | 要求                                               |
| -------- | -------------------------------------------------- |
| 击杀数   | > 2000                                             |
| 重铸次数 | > 5                                                |
| 耀魂     | > 5000                                             |
| SE       | 拥有 `VoidTransform`                               |
| 触发     | 拔刀剑物品实体掉入虚空（低于 `世界最低高度 - 64`） |

**转化结果：**

- 随机（50%/50%）转化为 `TwinBladeL` 或 `TwinBladeR`
- 继承原剑的数据（附魔、击杀数等）
- 移除 `VoidTransform` SE（防止重复转化）
- 出现在世界底部 + 5 格高度
- 设置发光、无重力、着地状态

---

## 四、形态转换系统

### 4.1 ConvertForm 机制

**方法：** [`TwinSlash.ConvertForm()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/TwinSlash.java:202)

当调用时，执行以下切换：

| 属性        | L → R                                      | R → L                                      |
| ----------- | ------------------------------------------ | ------------------------------------------ |
| 纹理        | `twinbladeleft.png` → `twinbladeright.png` | `twinbladeright.png` → `twinbladeleft.png` |
| SA          | `TWIN_SYSTEM_L` → `TWIN_SYSTEM_R`          | `TWIN_SYSTEM_R` → `TWIN_SYSTEM_L`          |
| 颜色        | `0x00C8FF` → `0xFF0089`                    | `0xFF0089` → `0x00C8FF`                    |
| specialType | `TwinBladeL` → `TwinBladeR`                | `TwinBladeR` → `TwinBladeL`                |
| 音效        | `RESPAWN_ANCHOR_CHARGE` (重生锚充能声)     | 同上                                       |

**触发时机：**

- 通过 [`TWIN_MODE`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:218) ComboState 手动触发（优先级 80）
- **MOOD_SLASH_3** 重锤落斩后自动触发（主手+副手同步转换）
- **DOOM_SLASH_4** 末日终结后自动触发（主手+副手同步转换）

### 4.2 TWIN_MODE（形态切换模式）

```java
// 独立于SA之外的快速切换入口
public static final RegistryObject<ComboState> TWIN_MODE = FD_COMBO_STATES.register("twin_mode",
    ComboState.Builder.newInstance().startAndEnd(0, 1).priority(80)
        .clickAction(entity -> TwinSlash.ConvertForm(entity, entity.getMainHandItem()))
);
```

- 优先级 80，高于默认 SA 的判定优先级
- 作为 AntiNTR 验证失败的**兜底跳转目标**

---

## 五、双持验证系统（AntiNTR）

**方法：** [`TwinSlash.AntiNTR()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/TwinSlash.java:181)

```java
public static boolean AntiNTR(LivingEntity entity) {
    // 主手：必须是 twin_blade
    CapabilityUtils.BladeContext mainCtx = CapabilityUtils.SEConditionMatcher.of(player)
        .requireTranslation("item.fantasydesire.twin_blade").match();
    // 副手：必须是 twin_blade（仅限副手槽）
    CapabilityUtils.BladeContext offCtx = CapabilityUtils.SEConditionMatcher.of(player)
        .onlyOffhand()
        .requireTranslation("item.fantasydesire.twin_blade").match();
    // 两者都持有 twin_blade 且 specialType 不同
    if (mainCtx == null || offCtx == null) return false;
    if (mainCtx.fantasyState.getSpecialType().equals(offCtx.fantasyState.getSpecialType())) return false;
    return true;
}
```

> **设计意图："NTR" 是梗名，实际意思是防止玩家"出轨"——即只拿一把刀或者拿两把同型号的刀。双刀流的灵魂就是一左一右各一把，形态不同才能触发完整机制。**

---

## 六、RippedStep（裂空瞬步）与 DominateStep（支配瞬步）对比

### RippedStep（MOOD连段使用）

| 属性       | 值                                                                                                                                     |
| ---------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| 索敌范围   | 35 格                                                                                                                                  |
| 视野角度   | 25°                                                                                                                                    |
| 索敌方式   | [`getNearestTargetInSight()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/utils/FDTargetSelector.java:121)（最近可视目标） |
| 优先级     | 优先使用锁定目标                                                                                                                       |
| 传送位置   | 目标背后 2 格（`target.getLookAngle().scale(2.0)`）                                                                                    |
| 传送后朝向 | **不改变**面向方向                                                                                                                     |
| 粒子       | 8条闪电粒子，青蓝/粉红交替                                                                                                             |
| 烟雾       | 20个烟雾粒子                                                                                                                           |

### DominateStep（DOOM连段使用）

| 属性       | 值                                                                                                                                      |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| 索敌范围   | 15 格                                                                                                                                   |
| 索敌方式   | [`getNearbyLivingEntities()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/utils/FDTargetSelector.java:55)（范围内随机目标） |
| 优先级     | 优先使用锁定目标                                                                                                                        |
| 传送位置   | 目标背后 2 格                                                                                                                           |
| 传送后朝向 | **面向目标**（`lookAt(Anchor.EYES)`）                                                                                                   |
| 粒子       | 8条闪电粒子，青蓝/粉红交替                                                                                                              |
| 烟雾       | 20个烟雾粒子                                                                                                                            |

---

## 七、核心玩法流派总结

### 7.1 双刀流基础循环

```
1. 主手 TwinBladeL + 副手 TwinBladeR（或反之）
2. 每次攻击 → TwinSet 触发 → 主副手双斩
3. 使用 SA 激活连段
4. 连段结束时形态自动转换（L↔R）
5. 重新进入循环
```

### 7.2 L形态（MOOD心境）流派特点

| 维度         | 特点                                        |
| ------------ | ------------------------------------------- |
| **定位**     | 空战型 / 强控型                             |
| **核心位移** | `RippedStep`（35格超远距离瞬步至目标背后）  |
| **连段节奏** | 较慢但控制力强（浮空→滞空→落地）            |
| **控制力**   | 强（击飞浮空 + 缓慢坠落 + 眩晕）            |
| **爆发点**   | 重锤落地时触发的幻影符文剑（25%生命值伤害） |
| **形态转换** | 连段结束时自动触发                          |
| **适用场景** | 单目标精英/Boss战、需要强控制的战斗         |

### 7.3 双刀流战术要点

1. **走位策略：** 利用 `RippedStep` 的 35 格超远瞬步，可以实现"突袭→连段→脱离"的游击战术
2. **双持瞬步：** SA 激活时，主手瞬步 → 副手自动追击（TwinSet） → 两把刀同步打伤害
3. **形态切换战术：**
   - 每次连段结束自动切换形态
   - 主动使用 `TWIN_MODE` 切换形态以重置连段
   - 切换后 SA 会改变（MOOD ↔ DOOM），玩家可以交替使用两套连段
4. **Resolution 伤害：** 每次攻击额外 50% 魔法伤害，对抗高护甲目标效果突出

### 7.4 装备搭配建议

| 项目     | 建议                                                             |
| -------- | ---------------------------------------------------------------- |
| 主手     | TwinBladeL（启动 MOOD 连段）                                     |
| 副手     | TwinBladeR（提供 TwinSet 触发，启动 DOOM 连段）                  |
| 推荐附魔 | 锋利/亡灵杀手/节肢杀手（提升基础伤害→Resolution和TwinSet都受益） |
| 药水配合 | 跳跃提升（配合滞空连段）、力量（伤害加成）                       |

---

## 八、联动设计理念

### 8.1 L/R 双刀配对设计

双子圣灵的核心设计理念是**阴阳双生**：

- 两把刀共享模型 [`twinblade.obj`](../../src/main/resources/assets/fantasydesire/models/twinblade.obj) 但使用不同纹理
- 颜色配对：青蓝（左/L） ↔ 粉红（右/R）—— 互为补色
- SA 配对：MOOD（心境/柔） ↔ DOOM（末日/刚）—— 一文一武
- 连段配对：空战控制 ↔ 地面爆发
- 形态转换：连段结束后自动互换，体现"轮回"概念

### 8.2 TwinSet 同步攻击

- 主手攻击时，副手自动追加反向斩击
- 双刀同时攻击的视觉效果（交叉斩）
- 伤害完全复制，等同于 2x 输出

### 8.3 瞬步 + 终结技的 Combo 链

**MOOD 连段完整链：**

```
RippedStep（远距离突进）
    → 跃升斩（浮空控制）
    → 双旋斩（滞空输出，主副手交替10连击）
    → 重锤落（落地）
    → ConvertForm（形态切换）
    → MoodFinalRuneSword x2（幻影符文剑追击）
    → 此时主手变为 TwinBladeR，可继续 DOOM 连段
```

### 8.4 虚空转化彩蛋

`VoidTransform` 为双子圣灵提供了一条隐藏获取途径——将一把"功成名就"（高击杀、高重铸、高耀魂）的拔刀剑投入虚空，它会在虚空之力下转化为双子圣灵之一。这呼应了双子圣灵的"圣灵"之名——它们不是被"制造"出来的，而是从虚空中"觉醒"的。

---

> **分析文件：** [`analysis/deepseekVersion/twin_blade_l.md`](./twin_blade_l.md)
> **对应形态：** TwinBladeL（双子圣灵·左）— TWIN_SYSTEM_L
> **关联文件：** [`twin_blade_r.md`](./twin_blade_r.md)（双子圣灵·右分析）
