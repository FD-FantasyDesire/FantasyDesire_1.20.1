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

- `.fsh` files use `#version 150`, `in`/`out` (no `gl_FragColor`), and `GameTime` uniform is **in ticks** (multiplied by 24000 in code, e.g. `fd_cross_flash.fsh`).
- The `.json` declares blend mode, samplers, and uniforms with default values; `matrix4x4` uniforms default to identity.

## shaderDev/ is NOT part of the Minecraft pipeline

`shaderDev/` is a **GLSL Canvas (VSCode extension `circledev.glsl-canvas`) prototyping scaffold** for effect ideas. Each effect dir has a self-contained `main.frag` written for **WebGL 1 / GLSL ES 1.00** (`precision highp float`, `gl_FragColor`, uniforms `u_time`/`u_resolution`/`u_mouse`), no textures allowed. It does not load into Minecraft. Migrating to a real shader requires manual adaptation: add `#version 150`, switch to `in/out`, replace `u_time`→`GameTime` (ticks), `gl_FragCoord`→UV/screen coords, and resolve uniform names per the target JSON. Keep this scaffold independent — don't wire it into `src/main/resources`.

## Reference sources

- `deplib/SlashBlade_Resharped-master/` — gitignored checkout of the required dependency source (`mods.flammpfeil.slashblade`). Consult it for SlashBlade API behavior; `mods.toml` declares only `forge`/`minecraft`/`slashblade` as mandatory deps.
- `plans/` — gitignored design docs (e.g. `energy-field-render-plan.md`, `CODE_REVIEW.md`). Read the relevant plan before implementing related features.

## Architecture notes

- Entry `FantasyDesire.java` wires all DeferredRegisters from `init/` (entities, particles, combo states, slash arts, special effects, potions, tab, recipes, items) and registers shaders client-side via `DistExecutor`.
- Client-only setup, entity renderers, particle providers, and player layers live in `client/ClientHandler`; shaders in `client/FDShaderHandler`.
- Blade special effects are per-directory under `specialeffects/effects/<name>/`.
- New entities/extras use custom NBT fields; many existing code paths lack null/range guards (see `plans/CODE_REVIEW.md` H/M issues) — validate inputs when adding new ones.
- Logging convention in rendering/shader code is `System.out/err.println` (not SLF4J).
