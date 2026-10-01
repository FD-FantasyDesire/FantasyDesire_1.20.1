# Ember Sigil：漩涡球形瞬爆

依据下载的 Unity 球形爆炸源码与官方 VFX Graph 模型、节点参数重新制作的奇幻技能。单次效果由瞬间爆闪、快速扩张的球形空间、球内卷动的青金噪声和飞散碎光组成；主体 0.58 秒、碎光 0.64 秒内结束。没有地面火焰、焦痕或符环。

下载来源、固定提交、模型结构、实际节点参数与修改对应关系见 [REFERENCES.md](REFERENCES.md)。原型参考资料位于本机 `captures/reference-source/`；发布资源仅含一张 MIT 噪声贴图及其许可。

## 启动

从主项目根目录执行：

```powershell
# 单个效果，1.5 秒循环，完整亮度
.\shaderDev\MinecraftShaderLab\run.cmd -Shader .\shaderDev\EmberSigil\single.preview.json
# 半径 1.7 格内，在 3 秒内发生 100 次；4.5 秒循环
.\shaderDev\MinecraftShaderLab\run.cmd -Shader .\shaderDev\EmberSigil\main.preview.json
```

密集预览使用 `Intensity=0.42`，避免加法叠加后过亮；没有删减事件数。单个预览使用 `Intensity=1`，两者是同一套三件套。主预览最多约 20 个球同时存活，100 次爆炸的出生时间从 0 到 3 秒均匀分布。

## 资源与输入契约

- Minecraft core GLSL 150：`assets/embersigil/shaders/core/main.{vsh,fsh,json}`，顶点格式 `DefaultVertexFormat.POSITION_TEX`。
- `NoiseSampler`：共享 `assets/embersigil/textures/dissolve-pattern.png`，512×512，来自 MIT 工程；线性过滤、循环寻址，元数据为 `.png.mcmeta`。球面固定前后两层，各采样低频和五倍频噪声，共四次纹理采样，无逐步体积积分。
- 三个层各 100 个槽，连续六顶点面片；`mesh.copies=300`，1800 顶点，一次 draw。`gl_VertexID/6` 解码事件及层，未出生、过期和超出 `EventCount` 的槽移到裁剪空间外。
- `Position.xy` 和 UV0 都为 `[-1,1]`，不能用任意四顶点索引网格替代。预览 quad 模型平移 `(0,1.5,0)`，顶点 shader 减去同一值；正式接入单位模型矩阵时同步移除该减法。
- 球体通过视图射线与球面解析求交绘制。`InvProjMat`、`ScreenSize`、`ProjMat` 必须匹配当前 render target，`IViewRotMat` 是视图旋转的逆 mat3。球体写入前表面交点的 `gl_FragDepth`，辅助面片写入各自平面深度。
- 深度测试开、深度写入关、剔除关；预乘混合 `one / 1-srcalpha`，所有发光输出 alpha=0，等效 RGB 加法。透明交叠不会压暗其他球，但颜色仍会累积饱和；预览器没有 HDR/Bloom。

| Uniform | 单位 / 默认值 / 语义 |
| --- | --- |
| `EffectTime` | 秒；事件时间轴。不能直接替换为归一化 GameTime |
| `EventCount` | 整数 0–100，正式默认 1；0 完全停用 |
| `BurstWindow` | 秒，默认 3；0 表示同时出生 |
| `RepeatPreview` | 默认 0；仅预览循环，周期 `BurstWindow+1.5` 秒 |
| `EffectCenter` | 世界格，默认 `(0,0.95,-0.3)`，空中爆炸中心 |
| `ScatterRadius` | 世界格，默认 1.7；批次圆盘内固定随机分布，高度另有 0–0.4 格变化；单次不散布 |
| `Intensity` | 发光倍数，默认 1；密集配置 0.42 |
| `Twist` | 弧度/单位球高度，默认 2.8，控制空间扭转 |
| `FlowSpeed` | 弧度/秒，默认 7，控制卷动速度 |
| `DissolveWidth` | 无量纲噪声阈值宽度，默认 0.08 |

球体为透明前后表面组成的空间视觉近似，内部亮度使用厚度及噪声权重，并非真实体积流体。没有执行 Unity 节点引擎，也没有复刻原模型的顶点位移。

## 验证与性能

2026-10-02，NVIDIA RTX 3060 Laptop / 驱动 576.88 / OpenGL 3.2，1920×1080。每项预热 2 帧，再测 10 帧，报告自定义 draw 的 GPU 中位耗时；不含场景、深度复制、截图、HTTP、Java 事件管理成本。没有锁定 GPU 功耗与时钟，不用于跨设备承诺。

| 场景 | GPU 中位耗时 |
| --- | --- |
| EventCount=0 | 0.019 ms |
| 单次球体，相机距离 11 格 | 0.109 ms |
| 100 次 / 3 秒，中段 / 最后一次附近 | 0.817 / 0.793 ms |
| 100 次同一时刻，相机距离 11 格 | 3.656 ms |
| 100 次 / 3 秒，相机距离 4.5 格 | 4.089 ms |
| 100 次同一时刻，相机距离 4.5 格 | 16.849 ms |

透明覆盖面积是主要成本，不能承诺零性能影响。100 次／3 秒默认场景开销较低；贴近或同帧堆积需在正式渲染端增加细节档位、覆盖预算或分辨率方案。所有压力测试均保留完整事件数。

已检查单次 0–0.65 秒序列、批次 0–3.65 秒序列、反向/俯视/掠地/近距离视角和竖屏。截图中球体边界完整，地形与实体遮挡保留；GLSL 编译、绘制无错误/警告。冻结时间两次帧哈希一致，资源重载后帧哈希一致。单次 0.65 秒及批次 3.65 秒与场景快照逐像素相同，确认无残留。加法合成没有压暗场景像素。

本机产物：`captures/sourced-visual-verification.json`、`captures/sourced-performance.json`，以及单次/批次截图。公共工具的批量面片支持已经通过 `-Verify`、`-SmokeTest` 和 `tests/debug-api.ps1`；本次仅调整作品，未修改公共宿主。

## 正式 Minecraft 接入

当前作品仍独立于 mod，不自动由 Minecraft 加载。真实 Java 端应维护事件出生时间、中心、种子及生命周期，将固定压力测试调度替换为实际技能队列，批量构造同材质面片。统一对齐空间、UV、秒制时间、噪声绑定及混合/深度状态。

按项目 shader 兼容规范注册明确的渲染阶段、匹配顶点格式、每帧从 getter 获取 ShaderInstance，并在真实游戏验证 F3+T、光影、透明物体及并发。绘制后恢复渲染状态；工具中的场景和 GPU 测量不代替完整 Minecraft 世界验收。
