# Limits and budgets — what is actually possible

Feasibility answers. Grades: **[V]** verified from inspected shipping packs or vendor documentation, **[D]**
derived from how shipping packs behave, **[I]** inferred from general OpenGL practice — measure before
relying on it.

---

## 1. Hard "never" list

No amount of cleverness gets past these. Say so early rather than designing around them.

| Not available | Why it matters |
|---|---|
| Compute shaders, SSBOs, `imageStore`, atomics | no scatter writes; every algorithm must be expressible as "one output per rasterised fragment" |
| Geometry / tessellation shaders | you cannot create vertices; you can only move the ones the game submits |
| Readback to the CPU | the shader can never tell the game or the server anything |
| Custom uniforms pushed from a server | every "new" value must ride an existing channel |
| Reading the framebuffer in a core shader | core-to-core communication in one frame is impossible; use the texture, or last frame |
| Reading and writing the same target in one post pass | bounce through a scratch target |
| Loops in a post pipeline | unroll by writing the pass N times |
| Conditional passes | run always, let a pixel decide (`techniques-index.md § A23`) |
| Knowing what block is at an arbitrary coordinate | only what is currently being rasterised |
| Anything about entities outside the frustum | including their existence |
| Post effects reaching the hand or the GUI | they are drawn after post (`frame-anatomy.md § 2a`) |
| `#extension` beyond what the backend enables | a few are available (`GL_ARB_separate_shader_objects`, `GL_ARB_texture_query_LOD`); most are not |

---

## 2. Texture size

The one hard number the community has written down. From the vanilla skybox pack's README
[V]:

| Vendor | Max texture dimension |
|---|---|
| Intel integrated | 8192 |
| AMD | 16384 |
| NVIDIA | 32768 |

**Exceeding it does not degrade gracefully — the shader fails to load and the game may
crash.** Any scheme that grows a texture with content (a cubemap list, an objmc mesh, a
sprite sheet of frames) must state its ceiling and stay well under 8192 to be safe for all
users.

Corollary for atlases: the block/item atlas is stitched from everything you ship. Thousands
of tiny marker textures are fine; a few large ones are not. Ship one texture per distinct
*data value* rather than one per block.

---

## 3. Render targets and samplers

| Limit | Value | Grade |
|---|---|---|
| Post targets in one pipeline | shipping packs use up to **35** without issue | V |
| Samplers bound to one post pass | shipping packs use up to **9** | V |
| Samplers in a core shader | 3 in vanilla (`Sampler0/1/2`); more only on G1/G2 by declaring them | D |
| Uniforms declared in one G1/G2 pipeline JSON | shipping packs use ~12; no observed ceiling | D |
| UBO members (G3+) | you cannot add UBOs, only read the ones vanilla provides | V |

Each declared target costs a full-resolution RGBA8 allocation. At 1440p that is ~15 MB
each; 35 of them is ~500 MB of VRAM. That is why target-heavy packs come with an
optimisation recommendation.

**Fixed-size targets** cost their declared size regardless of window:
`{"width": 4000, "height": 3000}` is 48 MB, always [V].

---

## 3b. Varyings — the ceiling that breaks multi-feature packs

Interpolators between the vertex and fragment stages are a fixed hardware resource.
Practical limit ≈ **32 `vec4` slots** (`GL_MAX_VARYING_COMPONENTS` = 128 floats) [I —
measure on the target if you are close]. Exceeding it is a **link failure** logged as a
shader error, with no indication of which declaration pushed you over. Intel drivers expose
the smallest limits; if you can only test one vendor for this, test there.

Observed counts, in `out` / `flat out` declarations [V]:

| Shader | Declarations |
|---|---|
| an everything-pack item-entity shader | 17 |
| an everything-pack text shader | 15 |
| a deferred-renderer block shader | 12 |
| an FPS-pack entity shader | 11 |
| a text-effect subsystem on its own | 12 |

Slot cost: `float`/`vec2`/`vec3`/`vec4` = 1 slot each in practice; **`mat4` = 4 slots**,
`mat3` = 3. A post shader passing two `flat out mat4`s plus three vectors is already at 11
from five declarations.

This is the practical cap on how many features one render type can carry. Compression
techniques — bitfield packing, merged corner varyings, reconstruct-instead-of-interpolate,
`#ifdef` elimination — are in `composition.md § 3`.

---

## 4. Per-pass cost

Rough shape at 1080p. The unit that matters is **full-screen passes**.

| Work | Cost |
|---|---|
| A `blit` or `copy` | 1 pass |
| A 7-tap separable blur | 1 pass per direction |
| Bloom, 7-level pyramid | 14 passes, but each at decreasing effective resolution → ≈ 2.3 full-screen equivalents |
| SSAO 16 taps + 9-tap blur + temporal | ≈ 6 passes, and the AO pass itself costs ~16× a blit |
| Motion blur, 12 taps | ≈ 3 passes |
| Voxel flood fill | 1 pass per iteration, but only over the voxel region |
| Data marker | ~40 fragments. Free. |

Practical ceilings observed in shipping packs: a deferred renderer running ~20 passes needs
render distance 3 and an optimisation modpack; voxel lighting and SSAO at ~8 passes each are
comfortable. **Above roughly 10 full-screen-equivalent passes you are in "requires a good
GPU" territory.**

The expensive thing is almost never the post passes. It is:

| Real cost driver | Multiplier |
|---|---|
| An extra full scene render (portal view, shadow map) | ×2 per view — dominates everything else |
| A marker quad on every block | one extra quad per visible block; noticeable at high render distance |
| Voxelization | one extra quad per visible block, plus a fill pass |
| Thousands of translucent item displays | translucency sorting is CPU-side and superlinear |
| Large `for` loops in a fragment shader | fully unrolled by the driver; a 128-iteration palette search is fine at 128×128 and fatal full-screen |

---

## 5. Per-vertex cost

Vertex shaders run millions of times per frame on terrain, but only a handful of times for
sky, sun and markers. Budget accordingly.

- Mesh-in-texture formats perform 6–10 `texelFetch` calls **per vertex** and ship in
  production packs — vertex-stage texture reads are affordable.
- Decode data markers in the **vertex** stage and pass `flat`: 4 executions per pass instead
  of 2 million. Every serious pack does this.
- A `switch` over hundreds of cases in a vertex shader (a HUD element table) is fine
  [V] — the branch is uniform across the draw.
- `inverse()` of a `mat4` in a vertex shader, once per pass: fine. Per fragment: do not.

---

## 6. Data channel bandwidth

| Channel | Payload | Refresh |
|---|---|---|
| Data marker | ~40 px × 24 bits ≈ 120 bytes | every frame |
| Lightmap alpha | 256 bytes | every frame |
| Vertex `Color` | 4 bytes per entity | 1 tick |
| `brightness` → `UV2` | 1 byte per entity | 1 tick |
| Position encoding | ~30 usable bits per entity | 1 tick |
| `GameTime` | ~8 useful bits, globally | 1 packet |
| One map item | 16 384 bytes (114 688 bits at 7 bits/byte) | per packet |
| Post-effect toggles | 1 bit per declared effect | 1 packet |
| Persistent target | a whole target | 1 frame |

Position float precision caps the position channel: keep `|Position|` under ~8·10⁶ or the
low bits stop resolving [D].

---

## 7. Scaling rules of thumb

- **Voxel grids** size themselves from the framebuffer:
  `⌊((width/2) × (height−1))^(1/3)⌋` blocks per side, halved for the radius. At 1080p that is
  about ±50 blocks [V]. Wanting more means a fixed-size target and more
  VRAM, not a bigger window.
- **Map-based screens** are 128×128 per map. Larger screens are grids of maps, and each is a
  separate entity and a separate packet.
- **Marker models** scale with block variety, not world size — generate them.
- **Post targets** scale with window area. Half-resolution intermediates are the standard
  saving; a bloom mip pyramid is the extreme version.

---

## 8. Compatibility ceilings

| Environment | Status |
|---|---|
| Vanilla, Fabulous | full support |
| Vanilla, Fancy / Fast | **no post pipelines**; core shaders still work |
| Sodium | replaces terrain rendering; terrain core shaders are ignored without a helper mod. Declare `sodium.ignored_shaders` and be honest [V] |
| Iris | takes over the pipeline; core shaders bypassed. Packs generally drop Iris support rather than fight it |
| OptiFine | conflicts; ship an `optifine/` fallback (CIT, `emissive.properties`) if you must support it [V] |
| AMD drivers | stricter about implicit conversions and non-constant `switch`; ambitious packs ship an alternate, simpler pass chain as a fallback |
| Intel integrated | 8192 texture limit is the binding constraint |

---

## 9. Feasibility triage, by cost class

| Class | Examples | Verdict |
|---|---|---|
| Free | data marker, alpha tags, position bands, magic pixels, pass detectors | always yes |
| Cheap | HUDs, waypoints, text effects, sprite animation, item model tricks | yes |
| Moderate | SSAO, bloom, motion blur, colour grading, minimaps, GUI raytracing | yes, with Fabulous |
| Expensive | voxel lighting, persistent world maps, per-block markers at high render distance | yes, state the cost |
| Very expensive | portals, shadow maps, anything needing an extra scene render | only with server camera control; halves frame rate per view |
| Impossible | see `§ 1` | say so immediately |

When a request lands in "very expensive", be concrete about what shipping one has historically
required: Fabulous graphics, render distance 3, entity distance 500 %, an optimisation modpack,
and years of work.
