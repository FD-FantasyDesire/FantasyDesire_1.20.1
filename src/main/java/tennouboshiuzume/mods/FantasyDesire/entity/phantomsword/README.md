# 幻影剑行为原型

本轮将待命绑定、飞行转向和命中规则从 `EntityFDPhantomSword.tick()` 提取为策略。生命周期仍复用既有待命、飞行、插入字段；完整的阶段枚举与统一迁移入口留待这一版手感验收后处理。

## 配置入口

```java
sword.setStandbyMode(EntityFDPhantomSword.StandbyMode.PLAYER);
sword.setTargetId(initialTarget.getId());
sword.setSpeed(1f);
sword.setInaccuracy(1f);
sword.setPiercingHoming(3, 16f);
```

`setPiercingHoming(总命中次数, 搜索半径)` 的次数包含首个目标，不使用父类 `Pierce + 1` 的规则。有效总次数为 0～128，搜索半径为 0～64 格。`MovingMode.PIERCING_SEEK` 是兼容枚举入口，默认配置为 3 次、16 格；自定义参数应使用 `setPiercingHoming()`。

`setInaccuracy()` 兼容父类 `shoot()` 的散布单位，0 表示无散布，不是角度。自动发射使用 `SwordLaunch`，以服务端生成并随出生包同步的实体 UUID 派生独立随机种子，保留父类高斯散布公式；相同朝向、速度和散布参数在两端得到相同结果，且不受其他随机调用影响。外部直接调用继承的 `shoot()` 仍使用父类行为。

客户端按 `DelayTicks` 立即预测发射，预测标记与服务端 `IT_FIRED` 分开，避免等待后续状态包期间继续绑定在玩家身上。服务端仍负责音效广播、发射事件、伤害、换目标与最终运动校正。发射当 tick 不执行制导，随后由 `SeekDelay` 控制制导起始年龄。原有零延迟和按序号错开发射的配置继续生效。

已有 `NONE / PLAYER / WORLD`、`NORMAL / SEEK / ADV_SEEK` 枚举 API 保留，旧 NBT 会转换为稳定行为 ID。新模式追加在枚举末尾，不改变旧 ordinal。

## 注册与执行

- `PhantomSwordBehaviors.BINDINGS` 保存绑定工厂：`world`、`owner`、`none`。
- `PhantomSwordBehaviors.FLIGHTS` 保存飞行工厂：`straight`、`homing`、`guided`、`piercing_homing`、`piercing_homing_chain`；最后一种用于命中后的强化连锁段。
- 每枚实体持有独立策略实例；只有类型变化时查表并创建。类型 ID 通过实体数据同步，两端应在开始游戏前注册相同类型。
- `SwordStandbyBinding.resolve()` 返回位置、基础旋转及待命速度；基类执行瞄准和发射。
- `SwordFlightBehavior.steer()` 返回期望速度或 null；基类执行移动、射线和表现。
- `SwordImpactPolicy` 负责命中资格、预留次数、插入/穿透以及结算后的换目标；不要在飞行策略中自行遍历旧射线或再次扣次数。
- `FlightType` 与 `FlightState` 保存行为 ID 和实例状态，`BindingType` 保存绑定类型。读取未知类型分别回退直线飞行与世界绑定。
- 当前穿刺次数和命中 UUID 集合仅在服务端维护并存档；客户端根据同步目标 ID 和运动状态预测，不负责选目标或扣次数。

新增行为示例：

```java
ResourceLocation custom = PhantomSwordBehaviors.FLIGHTS.register(
        ResourceLocation.tryParse("fantasydesire:custom_flight"), CustomFlight::new);
sword.setFlightBehavior(custom, settings);
```

工厂需返回新实例。`load()` 只恢复状态，不能播放发射效果、生成实体或造成伤害。行为应通过正常初始化阶段注册，不在实体 tick 中动态注册。

## 穿刺追踪规则

1. 初始目标在发射前指定，只碰撞当前锁定目标，其他实体不会抢走次数。
2. Forge impact 被取消时不消耗次数；接受的碰撞预留一次次数并记录主体 UUID，避免命中回调重入。即使 `hurt()` 拒绝伤害，该碰撞也消耗一次并触发换目标。
3. 接受命中后停在真实接触点。有剩余次数时，从此点向周围 360°、配置半径内搜索最近的有效可见活体；次数耗尽或无下一目标时，插入刚命中的存活目标，沿用普通剑约 5 秒的插入寿命。刚命中的目标已死亡则碎裂。
4. 有效性依据射手的敌我关系与 SlashBlade 攻击配置；可见性由剑弹对候选中心、眼睛、脚部射线检测，任一点无遮挡即可。排除射手和本剑已命中的主体，多部位按主体 UUID 去重。
5. 新目标从下一个 tick 开始制导，不在本 tick 扫描旧线段继续命中。首次换目标切换为 `piercing_homing_chain`，原有剩余次数、搜索半径和命中集合随实例转移，行为 ID 同步使客户端也启用强化追踪；此后不再等待首段 `SeekDelay`。
6. 尚未命中时，当前目标提前失效仍会碎裂。存档恢复的目标 UUID 尚待加载时最多宽限 20 个制导 tick。
7. 超时、方块命中和爆炸仍服从基类规则；配置爆炸可能直接结束连锁。

首段转向取 `min(36°, SeekAngle × 当前速度)`，按真实夹角限制，并处理方向相反、零角度和重合位置。连锁段取 `min(90°, SeekAngle × 3 × max(1, 当前速度))`；追赶速度取 `max(基础速度 × 1.5, 目标速度 + 基础速度 × 0.5)`。大角度转弯时按朝向对齐程度减速，最低保留 20%，且步长不超过目标距离，减少小范围绕圈与越过目标。

所有 FD 幻影剑插入后仅随目标平移，保留命中朝向，不使用目标朝向或父类随机 `OffsetYaw` 渲染。插入时清除残余速度，停止记录飞行拖尾；普通剑、魂剑、枪剑及幻猎的模型与剑尖特效采用同一朝向规则。

## 临时游戏入口

`SmartPistolMode.dumpAmmo()` 的普通幻影剑使用新模式。参数集中在 `DUMP_AMMO_PIERCE_HITS=3`、`DUMP_AMMO_RETARGET_RADIUS=16`、`DUMP_AMMO_INACCURACY=1`。爆裂弹头仍生成原有智能导弹。

保留原本球面发射、10 tick 追踪延迟、基础速度和穿墙配置。换目标时仍检测遮挡，穿墙运动开关不会让墙后的候选自动合格。多枚剑各自计数，因此不同剑可以命中同一个目标。

建议低弹药量，放置至少 4 个高血量、符合当前 SlashBlade 攻击配置的实体，其中一个用墙遮住；检查顺序、第三次命中后插入、墙后目标排除，再测试只有一个目标时立即插入、近距离侧面/身后目标与突然死亡的目标。让插入目标移动和转身，确认剑只平移、朝向固定且没有新的飞行拖尾。大量齐发可能在其余剑到达前杀死初始目标，剩余剑会按目标失效规则碎裂。

## 自动检查

```powershell
.\gradlew.bat -I scripts/verify-phantom-sword.gradle verifyPhantomSword
```

检查总次数、重复命中、每剑独立状态、UUID/NBT 往返、阶段切换状态保留、非法半径、注册冲突、转向限制和反向/零向量边界，以及发射散布的两端一致性、独立性、零散布和分布量级。逐 tick 模拟检查连锁对身后、近距离侧面、横向移动与快速远离目标的接触效果。不启动世界，不覆盖实际碰撞、插入渲染、玩家绑定、可见性与网络表现。游戏中还需验证 `GunBladeEffects` 的零延迟能量弹、按序号发射的智能弹，以及带散布的 `dumpAmmo()`，观察是否仍有发射前停顿或轨迹跳变。

若本机 JDK 17 再遇到 `Unable to establish loopback connection`，可在当前 PowerShell 进程指定项目中的 socket 临时目录后运行；无需修改系统环境：

```powershell
New-Item -ItemType Directory -Force build/socket-tmp | Out-Null
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir="H:/FantasyDesire 1.20.1/build/socket-tmp"'
```
