# AGENTS.md

Minecraft Forge 1.20.1 mod (`fantasydesire`), an addon for **SlashBlade Resharped**. Forge 47.4.0, Java 17, Mojang official mappings. Comments and design docs in this repo are written in **Chinese** — keep new comments/plans in Chinese to match.

## Build / run commands

Gradle wrapper is 8.8. `org.gradle.daemon=false` in `gradle.properties`, so every invocation is a cold JVM — builds are slow. CurseMaven dev deps (JEI, SlashBlade, etc.) need network on first resolve.

- `.\gradlew.bat runClient` — run the mod in dev workspace (working dir `run/`)
- `.\gradlew.bat runData` — run datagen, writes to `src/generated/resources`
- `.\gradlew.bat build` / `compileJava` — build / compile
- `gameTestServer` run config exists but **there are no tests, gametests, or CI**; don't hunt for them.

## Datagen output is committed

`src/generated/resources` (incl. `.cache/`) is tracked in git. After editing `data/DataGen.java`, `data/builtin/FantasySlashBladeBuiltInRegistry.java`, recipes, or damage types, rerun `gradlew runData` and commit the regenerated output. `FantasySlashBladeDefinition` is a **datapack registry** populated by datagen.

## Custom core shaders

Shaders live in `src/main/resources/assets/fantasydesire/shaders/core/fd_*.{json,vsh,fsh}` and are registered in `client/FDShaderHandler.onRegisterShaders` via `RegisterShadersEvent`. Each shader needs a `ShaderInstance` created with an explicit `DefaultVertexFormat` (must match the JSON `attributes`), a static getter, and an `isXxxShaderLoaded()` guard. Always fetch instances fresh through the getters each frame — `F3+T` reload creates new `ShaderInstance`s.

- 正式 `.fsh` 使用 `#version 150` 与 `in`/`out`，不使用 `gl_FragColor`。时间单位以实际上传端和 shader 换算为准：原版内建 `GameTime` 是按 24000 tick 周期归一化的值，乘以 24000 后才得到周期内 tick（如 `fd_cross_flash.fsh`），再除以 20 得到游戏秒；自定义时间 uniform 可以直接使用 tick、秒或生命周期进度，必须注明单位。
- The `.json` declares blend mode, samplers, and uniforms with default values; `matrix4x4` uniforms default to identity.
- 新增或修改正式特效 shader 时遵守 [`SHADER_COMPATIBILITY.md`](SHADER_COMPATIBILITY.md)。通过 `FDShaderCompat.registerShader` 显式选择 `DEFERRED_WORLD` 或 `POST_WORLD`，并按规格验证光影开启/关闭、资源重载和渲染状态恢复。

## shaderDev/ is NOT part of the Minecraft pipeline

`shaderDev/` 是独立的效果原型目录。后续开发统一使用 [`shaderDev/MinecraftShaderLab`](shaderDev/MinecraftShaderLab/README.md) 验证，调用与作品规范见 [`shaderDev/README.md`](shaderDev/README.md)，作用域约定见 [`shaderDev/AGENTS.md`](shaderDev/AGENTS.md)。

- 直接维护 Minecraft core `.fsh`、`.vsh`、`.json` 三件套（桌面 GLSL 150），在作品目录提供 `.preview.json` 描述场景、纹理、uniform、时间单位与渲染状态。主项目根目录启动：`.\shaderDev\MinecraftShaderLab\run.cmd -Shader .\shaderDev\<作品>\main.preview.json`。
- 不再为每个效果新建 HTML/WebGL 沙盒、`preview.js` 或 `serve.cjs`，GLSL Canvas 不再作为默认开发入口。已有浏览器/`.frag` 示例保留作历史参考；继续开发某个旧效果时，将该效果接入统一工具，无需批量迁移无关作品。
- agent 验收默认使用隐藏调试接口，按 [`DEBUG_API.md`](shaderDev/MinecraftShaderLab/DEBUG_API.md) 加载、定时绘制、导出画面和深度数据，不控制用户桌面。工具缺少所需网格、输入或 pass 时扩展公共工具，再验证效果。
- 工具回归命令：`.\shaderDev\MinecraftShaderLab\run.cmd -SmokeTest`、`-Verify`；API 回归：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\shaderDev\MinecraftShaderLab\tests\debug-api.ps1`。这些是独立工具检查，不需要 Forge/Gradle 构建。
- 循环应有可控上限、数组访问应合法；几何边界和生命周期按效果语义定义。避免除零、意外路径、可见网格截断与阶段残留，性能根据覆盖面积、并发数量和实际测量评估，不以“无纹理”或“单 pass”代替判断。

本目录不由 Minecraft 自动加载，也不自动接入 `src/main/resources`。MinecraftShaderLab 的 Java 代码、依赖与构建产物不加入 mod 的 Gradle 源码集或发布包。正式迁移时对齐坐标、时间单位、采样资源、shader JSON、顶点格式、混合与深度状态，并遵守上方的注册和资源重载约定。

## Reference sources

- `deplib/SlashBlade_Resharped-master/` — gitignored checkout of the required dependency source (`mods.flammpfeil.slashblade`). Consult it for SlashBlade API behavior; `mods.toml` declares only `forge`/`minecraft`/`slashblade` as mandatory deps.
- `plans/` — gitignored design docs (e.g. `energy-field-render-plan.md`, `CODE_REVIEW.md`). Read the relevant plan before implementing related features.

## Architecture notes

- Entry `FantasyDesire.java` wires all DeferredRegisters from `init/` (entities, particles, combo states, slash arts, special effects, potions, tab, recipes, items) and registers shaders client-side via `DistExecutor`.
- Client-only setup, entity renderers, particle providers, and player layers live in `client/ClientHandler`; shaders in `client/FDShaderHandler`.
- Blade special effects are per-directory under `specialeffects/effects/<name>/`.
- New entities/extras use custom NBT fields; many existing code paths lack null/range guards (see `plans/CODE_REVIEW.md` H/M issues) — validate inputs when adding new ones.
- Logging convention in rendering/shader code is `System.out/err.println` (not SLF4J).
