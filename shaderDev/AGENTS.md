# Shader 开发与验证约定

本目录后续统一使用 [MinecraftShaderLab](MinecraftShaderLab/README.md)。执行细节见 [README](README.md)，隐藏调试协议见 [DEBUG_API](MinecraftShaderLab/DEBUG_API.md)。

- 新效果直接制作桌面 GLSL 150 的 `.vsh` / `.fsh` 和 Minecraft core `.json`，配套一个作品内的 `main.preview.json`。优先使用标准 `assets/<namespace>/shaders/{core,include}` 布局；JSON 默认值和预览实验参数分开。
- 不再为单个效果搭建 HTML、WebGL、GLSL Canvas 或复制预览服务器。当前工具缺少必需的几何、纹理输入或 pass 时，扩展 `MinecraftShaderLab` 的公共能力，并记录输入契约和兼容边界。
- 历史 `.frag` 和 HTML 文件不代表新流程；修改旧效果时为当前作品补齐统一工具入口，不批量改写无关效果。
- 从主项目根目录启动：`.\shaderDev\MinecraftShaderLab\run.cmd -Shader .\shaderDev\<作品>\main.preview.json`。实际路径包含空格时用引号。
- agent 默认运行自己的隐藏 `-DebugServer` 会话，使用 API 调整时间、相机和参数，读取截图、深度及 GPU 错误；不控制用户桌面，不接管用户的 GUI 实例。结束时调用 `shutdown`。
- 只改效果时加载其配置并检查实际画面、关键生命周期、视角和遮挡；改公共工具时执行所需的 `-SmokeTest`、`-Verify`、`tests/debug-api.ps1` 回归。不能以仅编译通过代替视觉验收。
- 每个作品 README 写明可直接复制的启动命令、资源、时间单位、坐标、纹理绑定、混合与深度状态、已验证场景及游戏内最终接入步骤。公共约定链接本目录文档，避免复制一套宿主实现。
- `.deps/`、`build/`、`captures/` 是本机产物，不提交；Windows PowerShell `.ps1` 必须保持 UTF-8 BOM。
- 预览工具不由 mod 加载。正式接入仍需 Java 数据绑定、shader 注册、资源重载与真实 Minecraft 世界验收。
