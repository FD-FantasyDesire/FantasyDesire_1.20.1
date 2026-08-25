# Designing a new core-shader project

How to go from "can Minecraft do X without a mod?" to a working pack.

---

## 1. Feasibility triage

Answer these in order. The first "no" ends the project.

1. **Does the effect need information the client already has on the GPU?**
   Yes → possible. No → you need a data channel (step 2) or it is impossible.
   *Impossible examples*: reading a block's identity at an arbitrary coordinate, knowing
   what the player is holding when it is not rendered, anything about entities outside the
   frustum, anything from the server that is not expressible as position/colour/texture/
   time/map/post-effect toggle.
2. **Can something be drawn where you need pixels?**
   Every pixel needs a carrier draw call (`geometry-hijacking.md § 10`). If the effect must
   appear when nothing is rendered there, you need a guaranteed carrier — a marker element
   on every block, an always-present item display, the HUD text pass.
3. **Does it need cross-frame memory?**
   Then it needs a post pipeline, which means **Fabulous graphics** on G1–G2, or a named
   post effect the server enables on G5. Fabulous is a real user-facing constraint; say so.
4. **Does it need an extra render of the scene?**
   Only feasible with server/datapack camera control, and it roughly halves frame rate per
   extra view. See `architectures.md § 6`.
5. **What is the target version?** If the answer is "several", multiply the work by the
   number of generations you span (`version-matrix.md § 7`).

---

## 2. Choosing a data channel

Match what you need to carry against `data-channels.md`.

| You need | Channel | Cost |
|---|---|---|
| Matrices / camera state in a post shader | Data marker (#1) | 1 marker element per block model |
| MC's own lighting parameters | Lightmap alpha (#2) | must override every lightmap-sampling shader; loses lightmap smoothing |
| One server-controlled scalar | `GameTime` (#3) | destroys the day cycle and all time-based animation |
| Static per-texture config | Magic pixels (#4) / alpha tags (#5) | free |
| Per-entity parameters from a plugin | Vertex `Color` (#6) | 24–32 bits, 1 tick latency |
| Which of N behaviours for this entity | Position bands (#7) | entity must live outside play space |
| Bulk pixels from a plugin | Map items (#8) | 16 KB/map, needs map-item plumbing |
| Memory between frames | Persistent targets (#9) | post pipeline + canary validation |
| Live bits from a plugin, modern MC | Post-effect toggles (#10) | one JSON per bit; 26.2+ only |
| Nothing (client state only) | Implicit uniforms (#11) | free |

Pick the cheapest that carries enough. Combining several is normal — a deferred renderer
typically uses position bands, implicit uniforms, an isolated control-plane target and
persistent targets at once.

---

## 3. Project skeleton

```
mypack/
  pack.mcmeta
  pack.png
  assets/minecraft/
    atlases/blocks.json                 if you need a stitched marker texture
    models/block/cube.json              if you need a per-block marker
    textures/custom/marker.png
    shaders/
      include/
        <feature>.glsl                  one per feature, each exposing make_<feature>()
        encodings.glsl                  see encodings.md
        datamarker.glsl                 see data-channels.md, document the layout in a comment
        screenquad.glsl
      core/
        ...                             G1/G2: .vsh + .fsh + .json;  G3+: .vsh + .fsh only
      post/ or program/                 per generation
    post_effect/ or shaders/post/       per generation
  <overlay dirs>/                       one per generation you support
```

`pack.mcmeta` for a single-version pack:
```json
{ "pack": { "pack_format": 46, "description": "…" } }
```
for a range:
```json
{ "pack": { "pack_format": 64, "supported_formats": [34, 99], "description": "…" },
  "overlays": { "entries": [
    { "directory": "g2", "formats": [34, 55] },
    { "directory": "g3", "formats": [56, 99] } ] } }
```

### The feature-library convention

Each feature is one include exposing a boolean entry point:
```glsl
// include/myfeature.glsl
#version 150

bool is_myfeature(sampler2D tex, ivec2 pixel) {
    return ivec4(texelFetch(tex, pixel, 0) * 255.5) == ivec4(157, 211, 147, 99);
}

#ifdef VSH
bool make_myfeature() {
    ivec2 texSize = textureSize(Sampler0, 0);
    if (!is_myfeature(Sampler0, ivec2(UV0 * texSize))) return false;
    /* rewrite gl_Position and varyings */
    return true;
}
#endif

#ifdef FSH
bool make_myfeature_frag(inout vec4 color) { ... }
#endif
```
and the core shader is a chain of early-outs:
```glsl
#define VSH
#moj_import <myfeature.glsl>

void main() {
    /* standard varyings first, so early returns still have valid outputs */
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    ...
    if (make_skybox())    return;
    if (make_waypoint())  return;
    if (make_myfeature()) return;
}
```
Compute the vanilla path **before** the branches, not after, so every early return leaves
correct varyings behind. This is the single most common structural bug in new packs.

---

## 4. Picking magic values

- **Colours**: choose an RGB no vanilla texture uses and that survives `round(x*255)`.
  Values already claimed by packs in the wild, which you should avoid if yours might run
  alongside them: `(76,195,86)`, `(46,212,93)`, `(157,211,147,99)`, `(157,147,211,99)`,
  `(149,213,75,1)`, `(112,108,138)`, `(12,34,56,78)`, `0xCAFEBA`, `0x00BEEF`, `0xABCDEF`,
  `0xFEDCBA`, `0x00FEED`.
- **Alphas**: 246–254 are safe for tags (visually opaque). 230–250 is taken by the bloom
  convention; 249–252 by the emissive convention. Document your allocation in a comment
  block at the top of the shader, because collisions across features are the classic
  everything-pack bug.
- **Always compare exactly**: `ivec4(round(x * 255.0)) == ivec4(...)`, never `==` on floats.
- **Always validate two independent things** before acting on decoded data — a magic word
  plus a per-pixel tag, or a magic plus a range check.

---

## 5. Worked design: a server-driven compass bar

*Requirement*: a horizontal compass strip at the top of the screen showing cardinal
directions plus up to 8 server-placed markers. Target 1.21.8 (G3).

1. **Feasibility**: camera yaw is derivable from `ModelViewMat` (free). Marker bearings must
   come from the server. Pixels can ride on text-display glyphs. No cross-frame memory
   needed → no post pipeline → works on all graphics settings. Feasible.
2. **Channel**: markers are entities the server already places, so use position bands (#7)
   for "this is a compass marker" plus vertex `Color` (#6) for the marker's bearing and
   icon id. No data marker needed.
3. **Carrier**: one text display per marker, plus one for the bar itself.
4. **Shaders to override**: `core/rendertype_text.vsh` only.
5. **Implementation**:
   ```glsl
   #moj_import <minecraft:dynamictransforms.glsl>
   #moj_import <minecraft:projection.glsl>
   #moj_import <minecraft:globals.glsl>

   bool make_compass() {
       if (Position.y > -1000000.0) return false;
       vec3 local = transpose(mat3(ModelViewMat)) * vec3(1, 0, 0);
       float yaw = -atan(local.x, local.z);                 // radians, camera facing

       float bearing = Color.r * 255.0 / 255.0 * 6.28318;   // 8-bit bearing from the server
       float rel = mod(bearing - yaw + 3.14159, 6.28318) - 3.14159;
       if (abs(rel) > 0.6) { gl_Position = vec4(10.0); return true; }   // off the strip

       float x = rel / 0.6;                                  // -1..1 across the bar
       vec2 corner = vec2[](vec2(-1,1), vec2(-1,-1), vec2(1,-1), vec2(1,1))[gl_VertexID % 4];
       float s = clamp(floor(ScreenSize.y / 540.0), 1.0, 4.0);
       vec2 halfSize = vec2(8.0, 8.0) * s / ScreenSize * 2.0;
       gl_Position = vec4(x * 0.6 + corner.x * halfSize.x,
                          0.9 + corner.y * halfSize.y, -1.0, 1.0);
       vertexColor = vec4(1.0);
       sphericalVertexDistance = 0.0;
       cylindricalVertexDistance = 0.0;
       return true;
   }
   ```
6. **Test plan** (`§7`).

---

## 6. Worked design: a health bar over every mob

*Requirement*: a bar above each mob, coloured by health fraction. Target 1.21.4 (G2).

- Carrier: a text display riding on the mob, so the bar already tracks the entity — no
  billboarding needed beyond what MC does.
- Channel: health fraction in `Color.r` (8 bits is plenty), team colour in `Color.gb`.
- Shader: `core/rendertype_text.{vsh,fsh}` with a `.json` adding `GameTime` and `ScreenSize`.
- Recognition: a magic texel in the bar's own texture, so ordinary nameplates are untouched.
- Fragment: `texCoord0.x < health ? filledColor : emptyColor`, with a 1-px border by
  testing `texCoord0` against the texel size.
- Gotcha: the entity's `Color` is also used by the nameplate render; capture it as
  `originColor` before the lightmap multiply.

---

## 7. Testing without a debugger

There is no shader debugger, no `printf`, and compile errors go only to `latest.log`.

1. **Compile first.** Errors appear in `.minecraft/logs/latest.log` when the pack loads.
   Reload with F3+T. A shader that fails to compile falls back to vanilla silently in some
   versions and turns the screen black in others.
2. **Visualise every intermediate.** Add a debug pass that blits the buffer you are
   suspicious of to `minecraft:main`, or temporarily `fragColor = vec4(myValue, 0, 0, 1)`.
3. **Visualise the data strip.** Before doubting the decoder, zoom a screenshot into the
   bottom-left row and check the pixels are non-black and changing.
4. **Use the GLSL text overlay** (`gpu-ui.md § 1`) to print decoded floats on screen. This
   is the closest thing to `printf` this discipline has. See `instrumentation.md § 3a`.
5. **Bisect with magic colours.** Replace a suspicious branch with
   `fragColor = vec4(1,0,1,1); return;` and see whether magenta appears.
6. **Test the matrix round trip** by re-projecting a known point and checking it lands
   where it should:
   `fragColor = vec4(abs(reconstructPosition(texCoord, depth) - expected) * 10.0, 1.0);`
7. **Test at several window sizes**, including a non-16:9 one and a very small one. Most
   layout bugs are aspect-ratio bugs, and voxel/mip addressing derived from `ScreenSize`
   changes behaviour at small sizes.
8. **Test in a GUI, in an inventory, in the first-person hand, in an item frame, on the
   ground, and in the third person.** A shader that only checks `ProjMat[2][3]` will
   surprise you in at least two of those.
9. **Test at midnight** (`/time set 18000`, then let it wrap) if you use `GameTime`.
10. **Test after a resource reload and a window resize** if you use persistent targets.

---

## 8. Estimating effort

Roughly:

| Scope | Files | Notes |
|---|---|---|
| One uniform override across all shaders | ~60 `.vsh` + `.json` | mechanical; script it |
| One self-contained effect on one render type | 3–8 | a day |
| A post pipeline with 3–5 passes and a data marker | 15–25 | `architectures.md § 4` |
| A feature library for a server pack | 20–40 shader files + models | `architectures.md § 3` |
| Per-block marker models | 300–2000 generated JSON | script it; never hand-write |
| Multi-generation support | × number of generations | plus stub includes and overlay wiring |
| An extra scene render (portals, shadows) | + datapack + a modpack recommendation | `architectures.md § 6`, months |

---

## 9. Novel directions

Ideas that follow from combining existing techniques; none of these are known to exist yet.

- **Post-effect bit bus + GPU UI (G5).** A server can push arbitrary bits this way. Combine with `gpu-ui.md` panels to get a fully server-driven HUD with **zero
  entities** — no text displays, no armour stands, no map items, one-packet latency. Scale
  the bus by adding effects; 256 bits costs 256 tiny JSON files, all generated.
- **Voxel + temporal + LogLuv = coloured GI.** Existing voxel lighting does a couple of
  flood-fill iterations per frame with no reuse beyond a raw history copy. Adding proper
  temporal reprojection to the light buffer would let it converge over many frames.
- **Mesh-in-texture + raytracing.** GUI raytracers use a hardcoded box hierarchy, while
  mesh-in-texture formats already store arbitrary geometry a shader can read. A raytracer over
  that format would render arbitrary meshes in a GUI slot.
- **Map bitstream + post pipeline.** Map-decoded screens render onto a quad. Feeding the
  decoded image into a post target instead would allow full-screen server-driven UI with
  post-processing behind it (blur, drop shadows, transitions).
- **Data marker carrying a scene description.** The marker currently carries camera state.
  Nothing stops it carrying a small list of lights, portals, or shapes written by the core
  shader from entity positions — a general "GPU scene graph" for post shaders.
- **Depth-as-reduction for other queries.** The depth test can act as a max-reduction over
  any key you encode in NDC z, not just block height. The same trick generalises: encode any key in NDC z and the hardware
  performs argmin/argmax over all draws for free.

When proposing one of these, state the version it requires and the graphics-settings
constraint up front.

---

## 10. Checklist before shipping

- [ ] `pack.mcmeta` declares the right `pack_format` and, if applicable, `supported_formats`
      / `min_format` / `max_format` and overlays.
- [ ] Every shader compiles on the target version — check `latest.log`, not the screen.
- [ ] The vanilla path is unchanged for everything you did not intend to touch.
- [ ] Every magic value is documented in a header comment with its allocation.
- [ ] Every decoded value is validated (magic word, canary pixel, or range check).
- [ ] Data-marker pixels are hidden or clamped away from in every pass that samples freely.
- [ ] Tested in GUI / hand / item frame / ground / third person, and at two aspect ratios.
- [ ] If it needs Fabulous, the pack description says so.
- [ ] If it modifies terrain shaders, `sodium.ignored_shaders` is present and the Sodium
      limitation is documented.
- [ ] Generated files (models, overlays) have their generator committed alongside.
- [ ] Upstream attribution comments are preserved.
