# 超新星汇聚 · Minecraft core shader

当前开发入口为 [MinecraftShaderLab](../../MinecraftShaderLab/README.md)，三件套位于 `assets/supernova/shaders/core/main.{json,vsh,fsh}`，使用桌面 GLSL 150。已从 `main.frag` 迁移粒子汇聚、连续旋转星核、冲击壳、外射碎片和紫青色体积云。历史 frag、浏览器文件及 review 图保留作为参考，后续只维护 core 版本。

## 启动与验证

从项目根目录直接执行：

```powershell
# 单实例；控制台可暂停、定位时间、调整参数。
.\shaderDev\MinecraftShaderLab\run.cmd -Shader .\shaderDev\Shaders\SuperNovaConvergence\main.preview.json

# 八个不同种子的同步实例。
.\shaderDev\MinecraftShaderLab\run.cmd -Shader .\shaderDev\Shaders\SuperNovaConvergence\stress.preview.json

# 原分辨率对照，算法与 50% 云团透明度均相同。
.\shaderDev\MinecraftShaderLab\run.cmd -Shader .\shaderDev\Shaders\SuperNovaConvergence\full.preview.json

# 自己的隐藏 GPU 会话，结束自动 shutdown；不控制桌面。
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\shaderDev\Shaders\SuperNovaConvergence\verify.ps1 -VisualOnly
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\shaderDev\Shaders\SuperNovaConvergence\verify.ps1 -Samples 7
```

`verify.ps1` 导出实际 PNG、独立场景颜色与 float32 深度、联系图及 JSON GPU 报告至工具 `build/supernova/<时间>/`；每组丢弃 3 帧预热，再采样 7 帧（可配置 3–50）。默认测 720p/1080p、分散/重叠、1/4/8/16 实例、32/56/80 步、汇聚/膨胀/云团/消散。仅测某阶段可传 `-BenchmarkPhases fade`。时间戳目录使用 UTC，产物不提交。

默认单实例和压测入口均为 `volumeScale: 0.5`：云团宽高减半，粒子、星核和冲击壳保持原分辨率。使用 `-CompareResolution` 可测同算法全分辨率/半分辨率；`-Counts`、`-Qualities`、`-BenchmarkPhases` 可缩小矩阵。数组参数在 PowerShell 内调用脚本，或使用 `powershell.exe -Command`。`-BaselinePath` 可指定本机保存的旧版本配置进行同场景对照，路径不由脚本自动生成。

## 输入契约

| 输入 | 单位与含义 |
| --- | --- |
| Position | `DefaultVertexFormat.POSITION`，NDC 全屏六顶点，XY 范围 [-1,1] |
| InverseViewProjection | 当前完整世界坐标投影视图矩阵的逆；与深度快照一致 |
| IViewRotMat | 当前视图旋转的逆，mat3；右/上/前方向均由实际相机取得 |
| CameraPosition / EffectCenter | 同一世界坐标系，单位格；默认爆心 (0,3.5,0) |
| ScreenSize / RenderSize | 最终输出 / 当前 pass 的像素尺寸，不用窗口逻辑尺寸 |
| ResolvePass / AuraSampler | -1 直接绘制、0 仅云团积分、1 重建云团与细节；采样独立低分辨率颜色 |
| CloudOpacity | 默认 0.5，各实例云团预乘颜色与 alpha 同时乘以 50%；不会修改积分密度、粒子或星核 |
| DepthSampler | 独立场景深度快照；预览绑定 `@scene_depth`，包含地形和实体 |
| EffectScale | 每一局部长度对应的格数，必须为正 |
| EffectAge | 该实例出生后的秒数；Java 使用 (ageTicks + partialTick) / 20 |
| ExplosionTime | 从出生到爆炸的秒数，0.5–12，默认 2.8 |
| Seed | 固定整数 0–9999，一次生命周期内不变化 |
| Exposure / ColorModulator | 自发光曝光与预乘颜色/透明度调制 |
| VolumeSteps / TrailSteps | 积分 16–80 步（默认 56）；每条尾迹 4–18 段（默认 18） |
| InstanceCount / InstanceColumns | 压测实例数量 1–32、布局列数；默认单实例 |
| InstanceSpacing | 压测布局在世界 X/Z 上的间距，格；0 表示重叠 |
| SeedStride / AgeStagger | 每槽种子增量 / 相邻槽出生时间差，后者为秒 |
| RepeatPreview | 默认 0，一次播放；预览配置为 1，循环周期 ExplosionTime + 3.6 秒 |

每槽年龄为 `EffectAge - slot * AgeStagger`，种子为 `Seed + slot * SeedStride`（钳制到合法范围）。每槽中心围绕 EffectCenter 排布；真实游戏中的任意独立位置/年龄需由 Java 每实例上传，不能把规则网格当作真实出生队列。

相机射线由场景深度和逆矩阵重建。云团积分、冲击壳弦长、粒子头与有限线段分别按场景深度裁剪；不再绘制原型相机、参考网格或背景。噪声绑定爆炸局部空间，视角改变不重新生成云形。固定 4.2 局部单位包围球覆盖当前路径、外射碎片与云团，网格没有矩形边界。

默认第 2.8 秒爆炸；爆发后约 0.4 秒完成主膨胀，3.1 秒后完全归零。正式单次模式无全局时间回绕。预览循环是独立开关，不能直接用归一化 GameTime 替代实例年龄。

## 混合与状态

默认执行两个 draw：pass 0 只积分云团并输出未调制的预乘透明颜色；pass 1 从独立颜色附件进行双线性重建，再绘制原分辨率细节。四邻域与当前像素线性深度不一致时，只重算当前云团，不降采样粒子。低分辨率使用自身 RenderSize 重建射线，以最终 ScreenSize 保持投影比例，支持奇数尺寸。

提前跳过确定为空的球形空腔、外轮廓和零密度采样；包围域测试先于实例种子旋转，无云团区域不做重建检查。空腔跳过保持原采样网格，未降低默认 56 步或噪声层数。CloudOpacity 在积分完成后作用，不通过减小密度改变原来的透射率提前退出条件。

星核使用预乘透明颜色，粒子和冲击壳的光使用不增加 alpha 的发光项。RGB 使用 `one / 1-srcalpha`，alpha 同样为 `one / 1-srcalpha`。关闭硬件深度测试、深度写入和剔除，以独立深度纹理进行遮挡；输出不采样当前颜色附件。

对效果自发光执行指数色调映射与显示域转换，保留深色云团和亮边；不对场景背景重复色调映射。与历史原型的不透明背景输出不同，因此不承诺逐像素相同。没有 HDR/Bloom 管线。多个云团按槽序合成，细节层在全部云团之后合成；相较原来按实例交错合成，跨实例遮光属于不同近似，没有相交体积的联合射线积分或动态深度排序。多个 50% 云团叠加后的总 alpha 可以超过 50%，不是将整个效果固定为 alpha=0.5。

## 已验证

2026-10-02，在 NVIDIA RTX 3060 Laptop / OpenGL 3.2 / NVIDIA 576.88 上通过实际编译、链接、绘制与 GL 错误检查：

- 0 / 1.7 / 2.783333 / 2.825 / 3.1 / 3.75 / 5.3 / 5.9 秒生命周期及侧视、俯视、相机进入云团、相机位于爆心。
- 1280×720 横屏、540×960 竖屏；种子 17 / 803 / 9999；倒计时 0.5 / 2.8 / 12 秒；32 / 56 / 80 步画面。
- 八实例同步和 0.25 秒错峰；并发性能矩阵见 [PERFORMANCE.md](PERFORMANCE.md)。
- 固定时间重复 PNG 哈希一致；重载后结束帧一致。
- 单次结束帧与同一帧无效果的 scene-color PNG 完全一致，避免把场景试件动画误判为效果残留。
- 将小尺度效果藏入实体后方的立柱，汇聚与云团帧均与无效果场景逐像素相同，未穿透不透明地形。
- 半分辨率生命周期、遮挡、相机位于爆心、奇数尺寸 537×311、原分辨率对照均通过；50% 云团颜色贡献与 0%/100% 对照偏差最大 1.5/255，0% 云团帧与场景完全一致。
- 公共工具 SmokeTest、Verify、debug-api.ps1 通过；API 新增 screen 降分辨率的实际纹理采样、清空、切换和奇数尺寸检查。

半分辨率和空区域优化已实施，完整更新对比见 [性能报告](PERFORMANCE.md)。汇聚尾迹仍在片元中求距，近距离、多实例重叠与地形边缘重算仍有成本，不能把此预览称作已通过 Minecraft 世界性能验收。

## 游戏内接入

本次文件仍位于 shaderDev，不由模组加载，未增加技能实体或 Java renderer。正式接入步骤：

1. 将三件套迁入 mod 资源命名空间，并同步 JSON 内资源名称；在 FDShaderHandler 中以 POSITION 注册、提供 getter 和 loaded guard，每帧获取新 ShaderInstance。
2. 按 [兼容规格](../../../plans/SHADER_COMPATIBILITY.md) 登记 POST_WORLD；自行安排 AFTER_LEVEL 调用，并用 ShaderRenderScope 恢复 framebuffer、viewport、纹理和其他状态。
3. 同步真实爆心、固定 seed、年龄、爆炸倒计时与正尺度；复制可用场景深度，保证相机矩阵、深度快照和输出尺寸同帧且同坐标系。
4. 实例中心/年龄任意变化时逐实例提交 uniform，处理透明排序；复现这里的独立低分辨率目标、ResolvePass、RenderSize 与深度感知合成，再将汇聚尾迹改为有限网格并重新测量。
5. 在实际 Minecraft 中检查无光影/有光影、Fabulous、透明物体、资源重载、resize、近裁面与渲染状态恢复。沙盒结果不代替这些验收。
