# Event Horizon：引力透镜黑洞

独立的 Minecraft 1.20.1 core shader 原型。参考 Eric Bruneton 的 [A Real-time High-quality Black Hole Shader](https://ebruneton.github.io/black_hole_shader/)（2020）及其 [吸积盘模型说明](https://ebruneton.github.io/black_hole_shader/black_hole/model.glsl.html)。本作品独立编写，未复制原作代码、贴图或预计算数据。

采用有界速度 Verlet 光线积分，呈现吸积盘的直接像、绕行像、黑洞阴影、背景星光弯曲、差动旋转和多普勒明暗变化。原作使用预计算表、光束追踪和专门的恒星滤波；这里使用等效中心力、程序化背景和艺术化盘面辐射，不能作为科学精确的相对论模拟。解析散射提供盘面辉光，不依赖预览器未提供的 HDR/Bloom。

## 启动

从主项目根目录运行：

```powershell
.\shaderDev\MinecraftShaderLab\run.cmd -Shader .\shaderDev\EventHorizon\main.preview.json
```

## 输入契约

- GLSL 150、`Position` + `UV0`，Minecraft 顶点格式为 `DefaultVertexFormat.POSITION_TEX`。全屏网格 Position.xy 使用裁剪空间 [-1,1]；不经过模型矩阵。
- `EffectTime` 单位为秒，只控制吸积盘差动旋转。效果持续存在，无出生/消失生命周期。
- `CameraPosition`、`HoleCenter` 为世界坐标；`InverseViewProjection` 是完整 `(ProjMat * ModelViewMat)` 的逆矩阵。默认黑洞中心为 `(0,1.5,0)`，Schwarzschild 半径为 1 格，盘面在世界 XZ 平面。
- `ScreenSize` 为当前绘制目标的实际像素尺寸。程序不采样纹理，无外部图片或 Minecraft jar 依赖。
- 单次全屏覆盖，混合 `one/zero`；关闭深度测试、深度写入和剔除。独立宇宙场景，不与 Minecraft 地形合成。
- `Steps` 默认 280、合法范围 64–420；达到预算后不显示尚未逃逸的背景。过低预算会丢失高阶绕行像。`LensingStrength` 默认 1，可在控制台置 0 比较直线追踪；仅 1 对应上述中心力模型。
- `Exposure` 控制曝光，`DiscBrightness` 控制盘亮度。时间只作用于连续周期旋转，可暂停观察。

## 验证与接入

使用公共工具的[隐藏 GPU 调试接口](../MinecraftShaderLab/DEBUG_API.md)验收；截图、计时和相机验证记录见 `captures/verification.json`（本机产物）。

2026-10-02 验证：NVIDIA RTX 3060 Laptop、驱动 576.88、OpenGL 3.2。GLSL 编译、链接和实际绘制无错误或警告；1280×720 主视角预热后自定义 draw 约 2.7–2.8 ms，不含 PNG、场景绘制与宿主开销。280 与 420 步主视角截图 SHA-256 完全相同，冻结重绘一致，0 秒与 12 秒图像不同。检查了 yaw=65° / pitch=32° 环绕、pitch=1° 掠视、pitch=70° 俯视、600×900 竖屏和关闭透镜的对照。两份深度附件均为全背景，确认未写入深度。公共工具未修改。

正式接入需要单独提供全屏渲染阶段与全部自定义 uniform，在 `FDShaderHandler` 创建匹配顶点格式的 `ShaderInstance`，通过 getter 每帧获取重载后的实例并使用加载保护；依照项目 `SHADER_COMPATIBILITY.md` 注册渲染阶段，恢复原渲染状态，并在 Minecraft 中检查光影和 F3+T。这里没有自动接入模组，也没有验证真实游戏管线。要制作世界内黑洞，需另做场景颜色/深度采样和有限空间契约，不能直接把全屏背景当作实体特效。
