# Shader 原型开发与统一验证

此目录面向 **Minecraft Java 1.20.1 / Forge 47.4.0** 的视觉效果开发。后续统一使用 [MinecraftShaderLab](MinecraftShaderLab/README.md)：直接编译桌面 GLSL 150 的 `.vsh`、`.fsh` 和 Minecraft core shader JSON，无需先制作浏览器版本再转换。

新效果和继续开发的旧效果均接入这个公共工具。**不再为每个作品搭建 HTML/WebGL 沙盒、`preview.js` 或独立预览服务器。** 所需网格、纹理输入或 pass 超出当前能力时，扩展公共工具，再进行验证。已有 `.frag` 与浏览器入口保留作历史参考，不表示已经迁移，也不需要一次性改写所有旧作品。

## 统一入口

以下命令均从主项目根目录执行。工具自身的 [README](MinecraftShaderLab/README.md) 和 [DEBUG_API](MinecraftShaderLab/DEBUG_API.md) 使用工具目录作为工作目录。

```powershell
# 人工交互：也可以直接双击 shaderDev/MinecraftShaderLab/run.cmd。
.\shaderDev\MinecraftShaderLab\run.cmd

# 打开已有正式效果：这些配置直接读取 src/main/resources 中的三件套。
.\shaderDev\MinecraftShaderLab\run.cmd -Shader .\shaderDev\MinecraftShaderLab\examples\fantasy-frost.preview.json
.\shaderDev\MinecraftShaderLab\run.cmd -Shader .\shaderDev\MinecraftShaderLab\examples\fantasy-void.preview.json
.\shaderDev\MinecraftShaderLab\run.cmd -Shader .\shaderDev\MinecraftShaderLab\examples\fantasy-rift.preview.json

# 打开自己的作品，含空格的路径需要引号。
.\shaderDev\MinecraftShaderLab\run.cmd -Shader .\shaderDev\MyEffect\main.preview.json
```

工具使用 Java 17 与桌面 OpenGL，不参与 mod 的 Gradle 源码集或发布包。依赖缓存在工具自己的 `.deps/`；运行日志、验证报告和帧图像位于 `build/`、`captures/`，均被 Git 忽略。原版 include 和纹理可从本机 1.20.1 client.jar 读取；查找方式和手动指定方法见工具说明。

## 新作品最小结构

```text
shaderDev/MyEffect/
  README.md
  main.preview.json
  assets/myeffect/shaders/core/main.json
  assets/myeffect/shaders/core/main.vsh
  assets/myeffect/shaders/core/main.fsh
  assets/myeffect/shaders/include/      # 按需使用
  assets/myeffect/textures/            # 按需使用
```

core JSON 中的 `vertex` / `fragment` 使用 `myeffect:main`，按实际顶点格式声明 `attributes`，并完整声明 sampler 与 uniform。`.vsh` / `.fsh` 使用 `#version 150`、匹配的 `in/out`；矩阵默认值使用单位矩阵。可参考公共工具的 [surface 三件套](MinecraftShaderLab/examples/assets/shaderlab/shaders/core/surface.json)，不复制宿主实现。

下面是与上述目录对应的 `main.preview.json` 起点。`uniforms`、`bindings`、`textures` 按实际 shader 声明添加，不需要的字段省略：

```json
{
  "shader": "assets/myeffect/shaders/core/main.json",
  "target": "quad",
  "state": {"depthTest": true, "depthWrite": false, "cull": false},
  "mesh": {"width": 2, "height": 2, "uv": [0, 0, 1, 1], "billboard": true},
  "camera": {"yaw": 35, "pitch": 25, "distance": 9, "fov": 60}
}
```

所有文件路径相对于配置文件；标准 `assets/` 布局会自动确定资源根。预览配置保存实验场景和 Java 端需要提供的输入，正式 shader JSON 保持 Minecraft 格式。

## 试验场景与输入

| target | 用途 |
| --- | --- |
| `blocks` | 地面、台阶、立柱材质，检查顶点、法线和表面效果 |
| `sky` | 相机中心的天空立方体，检查方向空间和环视 |
| `entity` | 有肢体动画和 UV 岛的僵尸/苦力怕试件，检查表面附着和遮挡 |
| `quad` | 世界空间平面或 billboard，适合粒子、裂痕及光束承载网格 |
| `screen` | 读取地形/完整场景快照，检查深度重建与全屏合成 |

工具提供模型视图、投影、渲染尺寸、雾和光照相关的常规 uniform；自定义时间、种子、生命周期和实例参数通过配置显式绑定。完整字段见 [Uniform 与纹理契约](MinecraftShaderLab/README.md#uniform-与纹理契约)。

- `seconds` 为预览秒数；`ticks` 为秒数 × 20；`GameTime` 是 `(ticks % 24000) / 24000`。生命周期应使用独立年龄或进度，结合插值，避免全局时间回绕。不要把旧 `u_time` 只改名后当作游戏时间。
- `mesh.uv` 可指定纹理坐标以外的局部域。SDF、有限路径等效果必须与 Java 实际上传的 UV 范围一致；`[0,1]` 并非所有网格的默认语义。
- 明确模型局部、世界、视图与相机相对坐标。深度重建用完整视图矩阵的逆得到世界坐标，用旋转视图的逆得到相机相对坐标，输入中心必须与之配套。
- `gl_FragCoord` 与 `ScreenSize` 对应实际离屏目标像素尺寸。鼠标、实体轮廓或额外实例数据不会因为 shader 声明同名变量而自动产生；按实际需求扩展公共输入。
- 检查分辨率、除数、归一化向量和距离函数的退化情况，避免除零、NaN 和中心奇点。

## Agent 验证流程

使用自己启动的隐藏调试进程，不控制用户桌面，也不接管用户的 GUI。接口详情、CLI 客户端和结构化错误见 [DEBUG_API](MinecraftShaderLab/DEBUG_API.md)。

下面是可复用的 PowerShell 调用；设置时间后捕获实际 GPU 帧和两份深度，最后关闭自己的会话：

```powershell
$labRoot = (Resolve-Path .\shaderDev\MinecraftShaderLab).Path
$labSession = Join-Path $labRoot ('build/debug/agent-' + [Guid]::NewGuid().ToString('N') + '.json')
$labProcess = Start-Process -FilePath "$env:SystemRoot/System32/WindowsPowerShell/v1.0/powershell.exe" -WindowStyle Hidden -PassThru -ArgumentList @(
  '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"' + $labRoot + '\run.ps1"'),
  '-DebugServer', '-SessionFile', ('"' + $labSession + '"')
)
$labClient = Join-Path $labRoot 'debug-client.ps1'
try {
  $labDeadline = [DateTime]::UtcNow.AddSeconds(30)
  while (!(Test-Path -LiteralPath $labSession)) {
    if ($labProcess.HasExited -or [DateTime]::UtcNow -gt $labDeadline) { throw '调试进程未能就绪' }
    Start-Sleep -Milliseconds 100
  }
  & $labClient -Session $labSession -Command load -Body '{"path":"examples/fantasy-frost.preview.json"}'
  & $labClient -Session $labSession -Command configure -Body '{"time":2.75,"camera":{"yaw":45,"pitch":20}}'
  & $labClient -Session $labSession -Command render -Body '{"width":800,"height":600,"attachments":true}'
} finally {
  if (Test-Path -LiteralPath $labSession) {
    & $labClient -Session $labSession -Command shutdown
    $null = $labProcess.WaitForExit(10000)
  }
}
```

API 的相对加载路径以**工具目录**为基准；加载同级作品可以使用 `../MyEffect/main.preview.json`。复杂请求写入 JSON 文件并用 `-InputFile` 传给客户端。隐藏模式冻结时间、按请求绘制；源码保存后显式调用 `reload`，GUI 模式则自动重载。

改效果时，验证当前作品的开始、中点、阶段切换与结束，必要时检查不同视角、宽屏和遮挡，并读取实际截图。改公共加载器、渲染或启动代码时，再执行相关回归：

```powershell
.\shaderDev\MinecraftShaderLab\run.cmd -SmokeTest
.\shaderDev\MinecraftShaderLab\run.cmd -Verify
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\shaderDev\MinecraftShaderLab\tests\debug-api.ps1
```

`-SmokeTest` 输出隐藏控制台的布局图与控件 JSON，可检查界面而不操作桌面。工具回归本身不能代替新效果的视觉验收，只有编译/静态检查时须如实说明。

## 纹理、pass 与性能

允许纹理、导数、公共 `#moj_import` 和有明确上限的循环/数组访问。GLSL 150 支持整数位运算和动态索引；不默认依赖 compute shader、SSBO 等更高版本功能。

- 纹理用可复现的资源位置或相对路径；场景颜色/深度由公共宿主生成，作品 README 写明来源、坐标和有效范围。
- 多 pass 需说明输入、输出、顺序、目标格式与尺寸；不能在当前写入的纹理附件上采样。公共工具现有双深度流水线之外的能力需要先扩展，不能以另建 HTML 沙盒绕过。
- `fwidth`、`dFdx`、`dFdy` 应避免在非一致控制流中求值。循环有可控上限，数组访问合法。
- 无纹理、单 pass 或纯 GPU 几何都不自动保证性能。比较程序化运算与采样成本，关注屏幕覆盖面积、并发实例、透明叠加和中间目标数量。
- 根据成本暴露步数、噪声层数、范围或分辨率选项。记录测量环境，预览器帧率不能直接代表 Minecraft 多实例表现。

## 几何与动画质量

- 有限线段、曲线、光束和裂痕应有清楚的起点、端点、长度或范围。不得因缺失节点、无效分支或默认原点产生意外路径。
- 从实体表面发出的光束，其根部和头部要符合设计；真实遮挡不能代替正确的路径定义。
- 生命周期与空间形状分别检查。单侧展开、中心展开或移动窗口都可以，但尚未出生/已消失区域不应残留。
- 辉光应平滑衰减，不能暴露承载网格的矩形边界。按实际混合方式选择网格余量、衰减或合成方式。
- `BladeRift` 的 Spawn 从局部左端向右端推进，中途由起点延伸到中心符合设计。特殊语义应写入作品 README，避免在后续迭代中误修。

## 接入 Minecraft 与兼容边界

shaderDev 和公共验证器都不由 Minecraft 自动加载，不会自动写入 `src/main/resources`。使用正式格式消除了反复转译，但接入仍需：

1. 在实际 Java 渲染端对齐网格、UV、坐标、时间单位、sampler、uniform、混合和深度状态。
2. 明确场景颜色/深度副本的渲染阶段、分辨率、resize 和生命周期，避免纹理反馈。
3. 在 `FDShaderHandler` 注册 `ShaderInstance`，显式匹配 `DefaultVertexFormat`，提供 getter 与加载状态检查，每帧获取实例以兼容 `F3+T`。
4. 在真实世界验证地形、实体、透明物体、资源重载和并发效果。工具的盒模型、全亮 lightmap、固定光照与场景试件不等于完整 Minecraft 世界管线。

当前工具支持 core JSON 与 sampler2D，不自动支持 OptiFine/Iris shaderpack、任意后处理链、完整材质图集、AO、透明排序或 HDR/Bloom。输出超过 1 的颜色本身不会产生 Bloom；需要这些能力时先明确并实现公共渲染契约，参见工具 [兼容边界](MinecraftShaderLab/README.md#与游戏一致的部分与边界)。

## 现有作品

- [Shin](Shin/README.md)：属性 `fd_lc_shin` 驱动的三维金色流焰，体积积分结合实体/方块深度，已接入正式模组。
- [ShinRotatedBackup](ShinRotatedBackup/README.md)：噪声火焰与 yaw 跟随改版前的旋转焰片存档，自带冻结资源与独立预览。

下列作品的一部分 README 仍记录旧 Canvas/浏览器入口，属于历史说明。下一次继续开发时按本页补齐三件套与 `main.preview.json`，不再扩展独立浏览器宿主。

- [AstraLightning](AstraLightning/README.md)：固定种子的星座闪电和十字星光。
- [BladeRift](BladeRift/README.md)：单侧生成的有限空间裂痕；正式 shader 已有 [公共预设](MinecraftShaderLab/examples/fantasy-rift.preview.json)。
- [EchoTimer](EchoTimer/README.md)：附着实体轮廓的虚空侵蚀原型。
- [VoidFlame](VoidFlame/README.md)：虚空火焰与实体表面效果；正式 shader 已有 [公共预设](MinecraftShaderLab/examples/fantasy-void.preview.json)。
- [Starfield](Starfield/README.md)：银河、暗尘埃与分层恒星。
- [StarSea](StarSea/README.md)：双深度地形覆盖星海。
- [SuperNova](SuperNova/main.frag)、[SuperNova3D](SuperNova3D/README.md)、[SuperNovaConvergence](SuperNovaConvergence/README.md)：超新星系列历史原型。
- [VoidRifter](VoidRifter/main.frag)：交错菱形裂痕与中央传送门。
