# BladeRift

BladeRift 是一个独立的 GLSL Canvas 单 pass 空间裂痕斩击：暗色星空被长菱形/纺锤形切口劈开，两侧沿裂痕法线反向错位，切口内部呈现暗色异空间、流动星云、闪电状液态能量与中心细棱形。

## 运行环境

当前作品采用[基础预览约定](../README.md)，以下描述对应现有入口；需要场景纹理或其他能力时可按总规范扩展。

- 预览目标：VSCode **GLSL Canvas**（扩展 ID：`circledev.glsl-canvas`）。
- 打开 [`main.frag`](main.frag) 后运行 **Show GLSL Canvas**。
- 面向 WebGL 1 / GLSL ES 1.00：无 `#version`、`#include` 或纹理，使用 `gl_FragColor`。
- 默认无纹理环境可直接运行，不依赖 Minecraft 集成或宿主场景。

## Uniform

```glsl
uniform float u_time;       // 预览时间，秒
uniform vec2 u_resolution;  // 画布像素尺寸
```

坐标以画布短边归一化，支持非正方形分辨率；作品不依赖鼠标输入。

## 动画分段

默认循环为 `CYCLE = 1.00` 秒：

1. **Spawn（0%–13.5%）**：裂痕沿局部横轴从左端向右端单侧推进，前几帧短暂过曝，宽度和扭曲迅速建立。前沿到达中心时出现左端到中心的半条裂痕是本效果的有意设计，不是通用线段生成规则。
2. **Hold（13.5%–68%）**：保持长菱形轮廓，内部能量、FBM 扭曲与闪电持续流动。
3. **Fade（68%–100%）**：光和两侧错位平滑归零；周期末回到安静背景以隐藏接缝。

## 参数与视觉模块

顶部常量 [`CYCLE`](main.frag:12)、[`RIFT_LENGTH`](main.frag:13)、[`RIFT_WIDTH`](main.frag:14)、[`DISTORTION`](main.frag:15) 和 [`EXPOSURE`](main.frag:16) 分别控制循环、长度、宽度、错位幅度与预览曝光。

- `valueNoise` / `fbm`：固定 5 层内联噪声，驱动星云、内部流动和位移强度；不会进入有限距离或反转位移方向。
- `riftHalfWidth`：使用左右对称的轴向 taper；`finiteRiftDistance` 将横轴超出量与纵向边界超出量合成为有限二维距离，近/远辉光共享同一闭合端点与衰减规则，不会退化成贯穿屏幕的漏斗。
- `distortionMask` / `displacement`：独立控制背景错位；横轴两端、法线远处及 Spawn 前沿分别平滑归零。上侧始终沿 `+normal`、下侧始终沿 `-normal` 外移，FBM 只调制位移幅度而不反转方向。
- `chroma`：归属背景扰动带，仅在切口附近做克制的红蓝采样分离。
- `emissiveMask`、`core`、`nearGlow`、`farGlow`：与背景位移彻底解耦；单侧 reveal 仅作为 Spawn 阶段 opacity，Spawn 完成后 Hold/Fade 由对称的有限裂口/中心棱形距离和生命周期衰减驱动。近/远辉光可柔和越过两端，但通过有限 glow clip 在画布边缘前归零，只形成规则一致的有限圆滑尾辉。
- `distortionMask` / `splitFalloff`：仅作用于背景采样坐标和色差，在长轴端点、法线远处与 Spawn 前沿独立平滑归零；FBM 只调制位移强度，上下两侧方向固定为 `y > 0` 沿 `+normal`、`y < 0` 沿 `-normal`。
- 中心仅在核心两侧、远离抗锯齿边缘的位置保留低强度暗色断层；断层先于中心细棱形合成，不会以黑白条纹覆盖核心色。
- `nebula` / `vein`：暗色内部的液态星云与闪电能量。
- 中心细棱形使用独立的有限菱形 mask/SDF；仅 Spawn 时跟随外侧裂口单侧 reveal，Hold/Fade 显示完整长度，并保留锐利尖端而非无限细直线。预览 tone mapping 后以 `CORE_COLOR` 最终覆盖核心主体，避免外侧 `ENERGY_COLOR` 的加色与曝光把核心偏成白色或青色。
- 顶部 [`CORE_COLOR`](main.frag:17) 与 [`ENERGY_COLOR`](main.frag:18) 分别控制中心核心色和外侧能量/辉光色。

## 无场景纹理与宿主替换

当前 `spaceBackground` 用 FBM、hash 星点、暗色渐变和 vignette 自包含生成背景。迁移到宿主时，可将它替换为场景纹理采样，并把 `bgCoord` 转为屏幕 UV；保留 `chromaVec` 的两次偏移采样即可继续表现边缘色差。宿主 uniform、颜色空间和采样接口需按实际管线适配。

## 性能、兼容与 HDR

- FBM 为固定 5 层，循环边界为编译期常量；Spawn 使用单侧局部横轴 reveal。
- 无位运算、动态数组、导数、纹理函数或现代输出语法；分辨率及所有分母有安全下限。
- `GL_FRAGMENT_PRECISION_HIGH` 不可用时回退到 `mediump`；低精度设备可能损失细线与星点细节。
- 当前预览通过末尾指数 tone mapping 将颜色映射到 0–1 范围。HDR 保存能力取决于渲染目标格式；生产 HDR 合成需按实际管线移除预览 tone mapping，输出线性高亮到浮点目标，并另行实现所需 Bloom，不能仅靠输出大于 1 的颜色获得泛光。
