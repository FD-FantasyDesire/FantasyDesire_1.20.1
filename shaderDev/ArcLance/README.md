# Arc Lance：雷光炮技能组合

参考 Gabriel Aguiar 的 [Unity VFX Graph — Stylized Laser Beam](https://www.gabrielaguiarprod.com/product-page/unity-vfx-graph-stylized-laser-beam) 的分层表现：束芯、电弧、起点能量团、命中闪光及粒子。这里独立编写 Minecraft core GLSL 150，没有复制或购买原作资源，不依赖 Unity。另加入贴地双震波与扩散球壳。

## 启动

```powershell
# 在主项目根目录执行。
.\shaderDev\MinecraftShaderLab\run.cmd -Shader .\shaderDev\ArcLance\main.preview.json
```

## 技能节奏

`SkillAge` 单位为秒，正式 JSON 默认单次播放；预览配置开启 `RepeatPreview=1`，每 4.6 秒自动循环。控制台可以暂停并逐帧观察。

| 年龄 | 表现 |
| --- | --- |
| 0–0.8 秒 | 能量球和三道断裂旋转环蓄力 |
| 0.83–1.07 秒 | 光束从 Origin 单向推进到 Target |
| 1.07–2.05 秒 | 白色束芯、青色外层、电弧、螺旋能流、命中爆闪与碎光 |
| 1.07–2.92 秒 | 命中球壳和双道贴地震波扩散、渐隐 |
| 3.2 秒之后 | 无效果残留，直接输出场景原图 |

## 渲染输入

- `Position` + `UV0`，对应 `DefaultVertexFormat.POSITION_TEX`。全屏 Position.xy 为 [-1,1] 裁剪坐标。单 pass 采样独立的场景快照，再合成发光。
- `Origin` / `Target` / `CameraPosition` 为世界坐标，格；光束长度由实际两端决定，重合端点直接跳过效果。电弧在两端收拢，不超出尚未展开的束头。
- `InverseViewProjection` 为完整投影与视图矩阵乘积的逆，深度重建和端点使用相同世界坐标系。`ScreenSize` 是当前输出像素尺寸。
- `SceneSampler` = 完整地形与实体颜色快照；`DepthSampler` = 相同尺寸、相同投影的完整非线性 OpenGL 深度 [0,1]。禁止采样正在写入的附件。深度截断体积和碎光，关闭硬件深度测试/写入/剔除，混合 one/zero。
- 震波贴在 y=0.08 的水平面，当前原型默认地面为 y=0。正式技能需增加实际命中地面高度与法线；当前不自动追踪任意 Minecraft 地形。
- 解析辉光在 shader 内生成，不依赖 HDR/Bloom。没有真实场景照明、烟尘、声音、相机震动或碰撞判定。
- `Steps` 默认 224，范围 64–320；`Intensity` 控制亮度。全屏体积积分成本需要实测，预览器单技能计时不能代表 Minecraft 并发性能。

## 接入与验收

通过公共工具[隐藏调试接口](../MinecraftShaderLab/DEBUG_API.md)检查阶段、环绕、深度遮挡、时间冻结、结束归零和重载。截图及测量输出在忽略的 `captures/`。

2026-10-02 验收：RTX 3060 Laptop / OpenGL 3.2 / NVIDIA 576.88，编译及绘制无 GPU 错误或警告。检查 0.62、0.94、1.35、2.15、2.85、3.25 秒，以及反向环绕和 720×1000 竖屏。1280×720 主镜头自定义 draw：蓄力约 2.6 ms、推进约 4 ms、持续发射约 13 ms、震波约 14 ms；不含场景、捕获和宿主开销，同机另有已打开的预览窗口，数字不是独占 GPU 基准或游戏帧率承诺。224 与 320 步主视角截图一致，冻结时间重绘一致，重载后截图一致。结束与重合端点两种情形的 PNG 哈希均与当帧完整场景快照相同，确认无效果残留。遮挡检查中实体躯干及立柱保持完整，体积仅积累场景深度之前的部分。公共工具未修改。

正式迁移需要 Java 端提供技能实例年龄与插值、实际起点/命中点和渲染阶段颜色/深度副本，按 `SHADER_COMPATIBILITY.md` 选择兼容阶段；ShaderInstance 注册须匹配顶点格式，每帧从 getter 获取并检查加载状态，最后恢复渲染状态。shaderDev 不自动接入 Minecraft，实际光影、资源重载和并发效果需要游戏内验证。
