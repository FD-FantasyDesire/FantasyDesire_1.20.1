# Instrumentation — seeing what the GPU actually did

Two tiers of tooling:

- **Tier 1 — a real frame debugger.** RenderDoc or NVIDIA Nsight, injected into Minecraft by
  a Fabric mod. Gives you the complete ordered draw list, every render target's contents at
  every step, live uniform values, post-vertex-shader positions, and per-pixel history.
  **When you can use this, use it.** It turns most of this discipline's "mysteries" into
  something you simply look at.
- **Tier 2 — in-shader instrumentation.** Printing values to the screen, colour tagging,
  buffer visualisation. Always available, works on any loader and any version, and still the
  right tool for values you want to watch continuously while playing.

Much of the folklore in this field ("`FogStart > 1e6` means the hand") was originally
discovered with Tier 2. It is far faster to confirm with Tier 1.

---

## 1. The edit loop

1. Edit the file.
2. **F3+T** reloads resource packs, including shaders. No restart needed.
3. Read `.minecraft/logs/latest.log` — search for `ERROR`, `WARN`, `shader`, `Couldn't parse`.
4. Look at the screen.

Failure modes, in order of frequency:
- **Compile error** → logged; the game usually falls back to vanilla. A normal-looking screen
  is *not* evidence of success. Always check the log.
- **Link / sampler error** → logged as a warning; the shader runs with a missing binding and
  silently reads black. Varying-count overflow shows up here (`limits.md § 3b`).
- **Runtime NaN** → nothing logged; black screen or magenta speckle.
- **Wrong file or wrong directory** → nothing logged at all, because the game never looked
  for it.

**The "is my file even loaded?" test:** put `#error hello` at the top and reload. No error in
the log means wrong path, wrong generation, or a different render type than you think.

---

## 2. Frame capture — the real debugger

### 2a. Getting it attached

Minecraft runs OpenGL through LWJGL inside a JVM, so a graphics debugger must be injected at
process start. Two Fabric mods do this for you:

| Mod | Notes |
|---|---|
| **GFX-Debuggers** ("NSight and Renderdoc Injector") | Fabric only, MC 1.18.x–1.21.x plus 26.1.x/26.2 snapshots, Fabric Loader 0.14.0+, Java 17+, Windows and Linux. Shows a dialog at launch to pick a debugger; skip it with `-Ddebugger=renderdoc`, `-Ddebugger=nsight-frame`, or `-Ddebugger=skip`. Auto-detects install paths, overridable by system property or env var. |
| **NSight & Renderdoc Loader** | Same purpose. Its own docs warn that NSight enables the OpenGL debug context and overrides message filters, so **expect slowdowns and log spam** while it is active. |

Then attach RenderDoc (or Nsight) to the running process and capture — RenderDoc's default
capture key is **F12** / **PrintScreen**.

Caveats to state up front:
- **This requires Fabric**, so you are debugging under a mod loader, not pure vanilla. Keep
  Sodium and Iris out of the instance — they replace the pipeline you are trying to inspect.
- Captures are large. Capture **one** frame, with the scene set up so the thing you care
  about is on screen.
- Nsight's debug context changes timing; do not draw performance conclusions from a frame
  captured under it (use its GPU Trace mode for that instead — `§ 6`).
- For 26.x, confirm the backend is still one these tools hook before promising it will work.

### 2b. What each panel answers

| Panel | The question it settles |
|---|---|
| **Event Browser** (draw list) | *What is drawn, in what order.* The complete ordered list of every draw and every post pass in the frame — this is the ground truth behind `frame-anatomy.md`. Where the hand sits relative to post, whether entities precede particles, which of your passes actually ran. |
| **Texture Viewer** | *What is in every render target at every step.* Scrub the timeline and watch a target evolve. Find the exact draw that clobbered your data strip. |
| **Pipeline State** | *What state and which shader this draw used.* The bound program with its GLSL source, live uniform and UBO values, bound textures, blend/depth state, viewport. This is how you read `FogStart` in the hand pass without writing a single line of debug code. |
| **Mesh Viewer** | *Where your vertices actually went.* Input attributes and **post-vertex-shader output positions** for the selected draw. Your geometry hijack is either at the NDC coordinates you intended or it is not, and you can see which. |
| **Pixel History** | *Why is this pixel not what I expect.* Every draw that touched a chosen pixel, what it wrote, and whether it was discarded or failed the depth test. |
| **Shader Debugger** | Step a single vertex or fragment invocation with real values. |
| **Resource Inspector** | Every texture including the stitched atlas — verify your magic pixel survived stitching, and check what the mip chain did to it. |

### 2c. Diagnosis playbook

**"Nothing appears where my effect should be."**
1. Event Browser — is there a draw at all? If your carrier is missing, the problem is
   server-side or model-side, not shader-side.
2. Mesh Viewer on that draw — look at the **output** positions. Are they the NDC coordinates
   you wrote? If they are `(10,10,10)` or degenerate, a guard fired that you did not intend.
3. If positions are right, Pixel History on a pixel inside the intended rectangle — you will
   see whether the fragment was discarded, depth-rejected, or overwritten by a later draw.

**"It renders, but the colours are wrong."**
Texture Viewer, walk the target chain in pipeline order. The first target that looks wrong
identifies the pass; everything downstream is a symptom.

**"My data marker decodes to garbage."**
1. Texture Viewer on `minecraft:main`, zoom to the bottom-left row. Are the pixels non-black?
   Do they change as you turn?
2. If they are static, `ModelOffset` was zero — the inventory draw wrote the strip. Add the
   guard.
3. If they are black, the marker never rasterised — check the Event Browser for the terrain
   draw and the Mesh Viewer for where the marker quad went.
4. If they look right but decode wrong, Pipeline State on the *consuming* pass and read the
   uniform values directly; compare with the encoded ones.

**"Which pass detector identifies this draw?"**
Select the draw in the Event Browser, open Pipeline State, and read every uniform. Change one
thing in game (open the inventory, hold an item, go underwater), recapture, and diff. This
replaces the whole guess-and-check loop that produced the tables in
`spaces-and-uniforms.md § 5`, and it is how you should re-derive them for a version nobody
has documented.

**"Did the atlas destroy my magic pixel?"**
Resource Inspector → find the block atlas → zoom to your sprite. Then check mip level 1: if
the tag is gone there, that is why `texture()` fails at distance and `textureLod(..., -4)` or
`texelFetch` is required.

**"Is my post pass even running?"**
Event Browser. If the pass is absent, the pipeline JSON is not being loaded, the graphics
setting is wrong (Fabulous), or the file is in the wrong directory for the generation.

### 2d. Making your draws findable

Minecraft does not emit helpful debug group names for most draws, so a capture is a long flat
list. Two habits make it navigable:

- **Temporarily make the effect visually loud** — `fragColor = vec4(1,0,1,1);` — so you can
  find it by scrubbing the Texture Viewer instead of reading shader names.
- **Identify by shader source.** Pipeline State shows the bound program's GLSL. Put a unique
  comment at the top of each of your shaders (`// MYPACK terrain`) and you can confirm which
  draw is yours at a glance.

---

## 3. In-shader instrumentation (Tier 2)

Always available, and the right choice when you want to watch a value change continuously
rather than inspect one frozen frame.

### 3a. Printing a value to the screen

Requires the bitmap font from `gpu-ui.md § 1`, which exists precisely for this.

```glsl
#moj_import <minecraft:text.glsl>

// fragCoord must be TOP-LEFT origin
ivec2 screenSize = ivec2(gl_FragCoord.xy / (glPos.xy / glPos.w * 0.5 + 0.5));
ivec2 fragCoord  = ivec2(gl_FragCoord.x, screenSize.y - 1 - floor(gl_FragCoord.y));

resetText(fragCoord);
setTextColor(vec4(1.0));
c(_F);c(_O);c(_G);c(_S);c(_SPACE); f(FogStart);       nl();
c(_F);c(_O);c(_G);c(_E);c(_SPACE); f(FogEnd);         nl();
c(_P);c(_2);c(_3);c(_SPACE);       f(ProjMat[2][3]);  nl();
fragColor = getTextPixelColor();
if (fragColor.a < 0.1) discard;
```

Where to host it:
- **A hijacked fullscreen quad** — cleanest. One face of the first-person hand works well
  (`geometry-hijacking.md § 5a`).
- **A post pass** — easiest if the value already lives in a post shader.
- **A marker quad** — when you need it in a core shader with no obvious carrier.

To print a value that only exists in a *different* shader, route it through a data marker
first (`data-channels.md § 1`), then print it downstream.

### 3b. Colour tagging

```glsl
fragColor = vec4(1.0, 0.0, 1.0, 1.0);   // magenta = "this branch ran and won"
```
Enable one branch at a time. If magenta never appears, that code path is not executing.

### 3c. Cross-pass order probing

Each pass writes a unique byte into a dedicated pixel of a persistent target without
clearing; print the pixel from the last pass. The order of writes is the order of passes.
```glsl
if (ivec2(gl_FragCoord.xy) == ivec2(60, 0)) {
    fragColor = vec4(float(MY_PASS_ID) / 255.0, 0.0, 0.0, 1.0);
    return;
}
```
(With a frame capture available, the Event Browser answers this instantly instead.)

### 3d. Buffer visualisation

Temporarily blit the buffer you doubt straight to the screen:
```json
{ "program": "minecraft:post/blit", "inputs": [ { "sampler_name": "In", "target": "normals" } ],
  "output": "minecraft:main" }
```
```glsl
fragColor = vec4(normal * 0.5 + 0.5, 1.0);            // normals
fragColor = vec4(vec3(pow(depth, 64.0)), 1.0);        // depth, contrast-stretched
fragColor = vec4(fract(worldPos), 1.0);               // position as a repeating gradient
fragColor = vec4(vec3(float(count) / 16.0), 1.0);     // a loop counter
fragColor = vec4(abs(a - b) * 100.0, 1.0);            // a difference, amplified
```

### 3e. Bisecting a wrong result

1. **Is the shader loaded?** `#error` test (`§ 1`).
2. **Is the branch taken?** Colour tag it.
3. **Is the input sane?** Print or visualise it.
4. **Is the transform right?** Feed a known point through:
   ```glsl
   vec4 t = ProjMat * ModelViewMat * vec4(0.0, 0.0, -1.0, 1.0);  // 1 block ahead
   fragColor = vec4(abs(t.xy / t.w), 0.0, 1.0);                  // ≈0 at screen centre
   ```
5. **Precision problem?** Multiply the error by 100 and look at it.
6. **Driver problem?** Simplify control flow — `switch` → `if`, remove integer bit ops, add
   explicit casts. If it starts working you found a vendor quirk, not a logic bug.
7. **Feature interaction?** Disable features one at a time (`composition.md § 5`).

---

## 4. Reading a pack you have never seen

When the user supplies reference packs, this is how to extract value quickly.

**Step 1 — establish the generation.** `cat pack.mcmeta`. `pack_format` → generation
(`version-matrix.md § 0`). `overlays` entries are first-hand evidence of where the author
believed the syntax boundaries are — better than any table.

**Step 2 — inventory the shaders.** `find . -path "*shaders*" -type f | sort`
- `.json` under `shaders/core/`? → G1/G2. Absent → G3+.
- `shaders/post/*.json` → G1 post format; `post_effect/*.json` → G2+.
- Which render types are overridden → what the pack hooks.
- `shaders/include/` → read first; it is the vocabulary.

**Step 3 — find the pipeline graph.** The `targets` list plus each pass's inputs and output
*is* the dataflow diagram. Sketch it before reading any GLSL.

**Step 4 — find the channels.**
```bash
grep -rn "texelFetch(Sampler0" --include=*.vsh --include=*.fsh .   # magic pixels
grep -rnE "== *i?vec[34]\(" --include=*.vsh --include=*.fsh .      # magic colours
grep -rn "flat out\|flat in" --include=*.vsh --include=*.fsh .     # decoded-in-vertex values
grep -rn "gl_VertexID\|gl_PrimitiveID" --include=*.vsh --include=*.fsh .
grep -rn "Position.y <\|ProjMat\[" --include=*.vsh .               # bands, pass detectors
grep -rn "\.a \* 255\|round(.*255" --include=*.fsh .               # alpha tags
```
`flat out` in a post vertex shader is the strongest single signal: whatever it declares is
what the pack decodes from its data marker.

**Step 5 — find the carrier.** `atlases/` with a custom directory source, a `cube.json` with
a zero-volume element, thousands of one-line block models (generated per-block data).

**Step 6 — deminify if needed.** Some packs ship one statement per file. Reformat first.

**Step 7 — record what you learned.** Generation, channel, carrier, and any *new* trick. New
tricks belong in `techniques-index.md`.

---

## 5. Measuring a new Minecraft version

The checklist that keeps this skill current. A **client-jar diff plus one frame capture**
answers almost all of it.

- [ ] `pack_format` of the release and of the first snapshot that changed shaders.
- [ ] Does `shaders/core/` still accept `.json` from packs?
- [ ] Extract `assets/minecraft/shaders/` from the jar (or fetch a template pack) and diff
      against the previous version. **The diff is the migration guide.**
- [ ] Which includes exist, and what does each declare?
- [ ] Fog function signatures and uniform names.
- [ ] Terrain positioning uniform (`ChunkOffset` / `ModelOffset` / `chunksection.glsl`).
- [ ] Which `defines` each render type sets.
- [ ] Post-effect JSON schema: `program` vs `vertex_shader`/`fragment_shader`; uniform shape.
- [ ] `gl_VertexID` vs `gl_VertexIndex`; whether `layout(location=)` is required.
- [ ] Re-measure the pass detectors (`spaces-and-uniforms.md § 5`) — **Pipeline State on the
      relevant draws is the fast way**.
- [ ] Confirm the frame order in `frame-anatomy.md` from the Event Browser.
- [ ] Whether post targets are still uncleared between frames.
- [ ] Item model / `custom_model_data` syntax on the server side.

---

## 6. Performance attribution

Budgets are in `limits.md`; this is how to find out where the time actually went.

**In-game, first pass.** F3 opens the debug overlay; shift+F3 shows the frame-time pie chart
and its breakdown. It attributes cost to broad phases, which is enough to tell "my post
pipeline" from "chunk rebuilds".

**Bisection.** Comment out passes from the pipeline JSON one at a time and compare frame
times. Crude but decisive, and it needs no tooling.

**Nsight GPU Trace** (via the same injector mods) gives real per-draw GPU timings and
occupancy. This is the tool for "which of my 14 bloom passes is actually expensive". Do not
mix it with a frame-debug capture — the debug context distorts timing.

Things worth measuring before optimising:
- Full-screen passes are the usual suspect, but an extra **scene render** dwarfs them.
- A marker quad on every block costs one extra quad per visible block; it shows up as
  vertex/chunk cost, not post cost.
- Large `for` loops in a fragment shader are fully unrolled; a 128-iteration palette search
  is fine at 128×128 and fatal full-screen.
- Translucent item displays cost CPU-side sorting that scales badly.
