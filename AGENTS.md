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

## shaderDev/ is NOT part of the Minecraft pipeline

`shaderDev/` 是独立的效果原型目录，具体要求见 [`shaderDev/README.md`](shaderDev/README.md)。按效果需要选择以下方式，无需把所有作品限制在同一种预览能力内：

- **基础预览**：默认使用 VSCode GLSL Canvas（`circledev.glsl-canvas`）与自包含的 `main.frag`，按 WebGL 1 / GLSL ES 1.00 编写。优先零纹理依赖，便于打开即用；这是预览默认选择，不是 Minecraft 的能力限制。
- **Minecraft 原型**：需要纹理、片元导数、自定义 uniform、顶点阶段、公共 GLSL 函数或多 pass 时可以使用，并按需提供独立预览宿主。在作品 README 中说明运行方式、资源与输入数据，以及到正式管线的映射。正式能力基线为桌面 GLSL 150；浏览器宿主使用其实际支持的 GLSL ES 语法，不能直接加载 `#version 150`。
- 循环应有可控上限、数组访问应合法；几何边界和生命周期按效果语义定义。避免除零、意外路径、可见网格截断与阶段残留，性能根据覆盖面积、并发数量和实际测量评估，不以“无纹理”或“单 pass”代替判断。

本目录不由 Minecraft 自动加载，也不自动接入 `src/main/resources`。正式迁移时适配坐标、时间单位、采样资源、shader JSON、顶点格式、混合与深度状态，并遵守上方的注册和资源重载约定。

## Reference sources

- `deplib/SlashBlade_Resharped-master/` — gitignored checkout of the required dependency source (`mods.flammpfeil.slashblade`). Consult it for SlashBlade API behavior; `mods.toml` declares only `forge`/`minecraft`/`slashblade` as mandatory deps.
- `plans/` — gitignored design docs (e.g. `energy-field-render-plan.md`, `CODE_REVIEW.md`). Read the relevant plan before implementing related features.

## Architecture notes

- Entry `FantasyDesire.java` wires all DeferredRegisters from `init/` (entities, particles, combo states, slash arts, special effects, potions, tab, recipes, items) and registers shaders client-side via `DistExecutor`.
- Client-only setup, entity renderers, particle providers, and player layers live in `client/ClientHandler`; shaders in `client/FDShaderHandler`.
- Blade special effects are per-directory under `specialeffects/effects/<name>/`.
- New entities/extras use custom NBT fields; many existing code paths lack null/range guards (see `plans/CODE_REVIEW.md` H/M issues) — validate inputs when adding new ones.
- Logging convention in rendering/shader code is `System.out/err.println` (not SLF4J).
