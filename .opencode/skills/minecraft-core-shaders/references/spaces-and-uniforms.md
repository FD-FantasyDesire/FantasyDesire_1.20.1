# Coordinate spaces and uniform semantics

The single largest source of silent bugs. Code compiles, screen is wrong. Read this before
writing any transform.

Evidence grades: **[V]** verified from inspected shipping packs, **[D]** derived, **[I]** inferred.

---

## 1. The spaces

```
Position (attribute)
   │  + ModelOffset / ChunkOffset      (terrain only)
   │  or + (ChunkPosition - CameraBlockPos) + CameraOffset   (G4+)
   ▼
camera-relative world space           ← "player space"; origin is the CAMERA
   │  × ModelViewMat
   ▼
view space                            ← camera at origin, looking down -Z
   │  × ProjMat
   ▼
clip space  (gl_Position)
   │  ÷ w
   ▼
NDC   [-1,1]³
   │  × 0.5 + 0.5
   ▼
screen [0,1]²  (× ScreenSize → pixels, which is gl_FragCoord.xy)
```

### The thing everyone gets wrong

**`ModelViewMat` contains no camera translation.** [V]

The world is submitted *already relative to the camera*. `ModelViewMat` is essentially a
rotation (plus whatever per-draw model transform applies), not a view matrix in the usual
sense. Consequences:

- `inverse(ModelViewMat) * viewPos` gives you a position **relative to the camera**, not a
  world position. There is no world position available anywhere.
- To undo the rotation you do not need `inverse()`; a rotation matrix's inverse is its
  transpose:
  ```glsl
  vec3 playerPos = viewPos * mat3(ModelViewMat);   // == transpose(mat3(ModelViewMat)) * viewPos
  ```
  GLSL's `vector * matrix` is `transpose(matrix) * vector`. This idiom is everywhere. [V]
- Camera **position** does not exist as a uniform. The only handle is the sub-chunk offset
  (`§ 3` below), which gives you camera *motion* between frames but never an absolute
  position.

### What `Position` means, per family

| Family | `Position` is | [grade] |
|---|---|---|
| terrain (`solid`, `cutout*`, `translucent`) | chunk-section relative; add the offset uniform | V |
| entity (`entity`, `rendertype_entity_*`) | model space, camera-relative once transformed | V |
| item entity | model space of the item display | V |
| text | GUI pixels when in HUD space, world units when a world text display | V |
| particle | camera-relative world space already | I |
| `position_tex_color` (sun/moon) | a unit-ish direction quad around the camera | V |
| GUI (`gui`, `position_tex`, `position_color`) | GUI pixels, origin top-left | D |

---

## 2. Uniform semantics

### Transform uniforms

| Uniform | Contents | Notes |
|---|---|---|
| `ProjMat` | perspective (world) or orthographic (GUI) | column-major: `ProjMat[col][row]` |
| `ModelViewMat` | rotation + per-draw model transform, **no camera translation** | see above |
| `ModelOffset` (G2+) / `ChunkOffset` (G1) | the chunk section's position relative to the camera | `vec3` |
| `ChunkPosition`, `CameraBlockPos`, `CameraOffset` (G4+) | replaces the above | `chunksection.glsl` |
| `TextureMat` | UV transform, active under `APPLY_TEXTURE_MATRIX` | |
| `IViewRotMat` (G1/G2) | inverse camera rotation, for billboards | `mat3` |

**Reading `ProjMat`:**
```glsl
ProjMat[0][0] =  1/tan(fovX/2)          // horizontal scale
ProjMat[1][1] =  1/tan(fovY/2)          // vertical scale
ProjMat[2][3] = -1  for perspective, 0 for orthographic     ← the GUI test
ProjMat[3][2] = -2*far*near/(far-near)  // used to derive far; near is 0.05
ProjMat[3][3] =  0  for perspective, 1 for orthographic
ProjMat[3][0], ProjMat[3][1] = sub-pixel offset — TAA jitter, or view bobbing in the sky pass
```
Derived quantities worth having:
```glsl
float aspect = ProjMat[1][1] / ProjMat[0][0];
vec2 getPlanes(mat4 proj) {                   // near is hardcoded 0.05 in vanilla
    const float near = 0.05;
    float far = proj[3][2] * near / (proj[3][2] + 2.0 * near);
    return vec2(near, far);
}
vec2 ui = ceil(2.0 / vec2(ProjMat[0][0], -ProjMat[1][1]));   // GUI size in "gui pixels"
```

**Always zero `ProjMat[3].xy` before using the matrix for reprojection or ray
reconstruction.** [V] It carries TAA jitter in the world passes and view bobbing in the sky
pass; both make results jitter by a pixel or wobble with the walk cycle.
```glsl
mat4 projMat = ProjMat;
projMat[3].xy = vec2(0.0);
```

### Colour and material uniforms

| Uniform | Contents | Trap |
|---|---|---|
| `ColorModulator` | per-draw tint, usually `vec4(1)` | multiplies **alpha** too — destroys alpha tags if you forget |
| `Color` (attribute) | per-vertex tint, byte-quantised | recover with `floor(Color.r * 255.0 + 0.5)`, never `floor(x*255)` |
| `overlayColor` (`Sampler1`, `UV1`) | hurt-flash / creeper-flash overlay | neutral is `a == 0`; blend as `mix(overlay.rgb, color.rgb, overlay.a)` |
| `lightMapColor` (`Sampler2`, `UV2`) | baked block+sky light | `UV2` is 0–240 in steps of 16; `texelFetch(Sampler2, UV2/16, 0)` |
| `GlintAlpha` | enchantment glint strength | |
| `LineWidth` | for `rendertype_lines` | |

`UV2` semantics: **x = block light, y = sky light**, each 0–15 scaled by 16. [V]
Two sampling styles, and they are not interchangeable:
```glsl
minecraft_sample_lightmap(Sampler2, UV2)   // texture(), bilinear — vanilla look
minecraft_fetch_lightmap(Sampler2, UV2)    // texelFetch, exact  — required if you pack data
```

### Fog uniforms

| G1 / G2 | G3+ |
|---|---|
| `FogStart`, `FogEnd`, `FogShape`, `FogColor` | `FogColor`, `FogEnvironmentalStart/End`, `FogRenderDistanceStart/End`, `FogSkyEnd`, `FogCloudsEnd` |

Sentinel values observed in G1/G2, used as pass detectors: [V]
- `FogStart > 1e6` (often written `> 3e38`) → first-person hand
- a specific negative `FogStart` → a datapack-set state sentinel
- `FogEnd` in a narrow band with `FogColor.rgb == 0` → a forced custom pass

These do **not** carry over to G3+. See `§ 5`.

### Globals (G3+, `globals.glsl`)

`ScreenSize`, `GameTime`, `GlintAlpha`, `MenuBlurRadius`.

`GameTime` is `(worldAge % 24000) / 24000.0` — a 0–1 value that wraps once per Minecraft
day. [V] Everything derived from it needs the wrap guard (`techniques-index.md § F10`).
Typical multipliers: `* 1200` (ticks-ish), `* 24000` (ticks), `* 2400`,
`* 600`.

---

## 3. Recovering things that "don't exist"

### Camera motion (no camera position uniform)
```glsl
vec3 offset = mod(position - prevPosition + 8.0, 16.0) - 8.0;
```
where `position` is `mod(ModelOffset, 16.0)` from this frame's data marker and
`prevPosition` from last frame's. Exact for motion under 8 blocks/frame. [V]

### Camera yaw
```glsl
vec3 local = transpose(mat3(ModelViewMat)) * vec3(1, 0, 0);
float yaw = -atan(local.x, local.z);
```

### A view ray, inside a **core** shader
Normally a post-shader operation, but the vanilla skybox pack does it in a fragment shader
with only `ScreenSize`, `ProjMat` and `ModelViewMat`: [V]
```glsl
mat4 projMat = ProjMat;
projMat[3].xy = vec2(0.0);                       // remove bobbing
vec4 ndcPos = vec4(gl_FragCoord.xy / ScreenSize * 2.0 - 1.0, 0.0, 1.0);
vec4 temp   = inverse(projMat) * ndcPos;
vec3 viewPos = temp.xyz / temp.w;
vec3 rayDir  = normalize(viewPos * mat3(ModelViewMat));
```

### Screen size when `ScreenSize` is unavailable
Pass `gl_Position` out as a varying and divide: [V]
```glsl
ivec2 screenSize = ivec2(gl_FragCoord.xy / (glPos.xy / glPos.w * 0.5 + 0.5));
```

### World position of a terrain vertex
```glsl
// G1:      ivec3(floor(Position + floor(ChunkOffset)))
// G2:      ivec3(floor(Position + floor(ModelOffset)))
// G4+:     ivec3(floor(Position)) + ChunkPosition
```
All are **camera-relative**, not absolute world coordinates. [V]

### Sub-block camera position
`fract(ModelOffset).xz` — what a scrolling map needs to move smoothly between blocks. [V]

---

## 4. Y direction, handedness, winding

- View space looks down **−Z**.
- NDC `y` is **up**; `gl_FragCoord.y` is measured from the **bottom**.
- Texture `V` and virtually every UI layout are measured from the **top**.
- Convert explicitly, once, at the boundary:
  ```glsl
  ivec2 fragCoord = ivec2(gl_FragCoord.x, screenSize.y - 1 - floor(gl_FragCoord.y));
  ```
- Under the modern render backend the NDC y direction can differ; expose a flip uniform
  rather than hardcoding it. [V]
- Quad corner order is normally top-left, bottom-left, bottom-right, top-right, but verify
  per render type by colouring `gl_VertexID % 4`.

---

## 5. Pass detectors, by generation

| Detects | G1 / G2 | G3+ |
|---|---|---|
| GUI / orthographic | `ProjMat[2][3] == 0.0` | same |
| GUI item render (alt) | `ProjMat[3][2] == -2.0` | verify |
| HUD text space | `ProjMat[3].x == -1` | verify |
| First-person hand | `FogStart * 1e-6 > 1.0` | **gone** — use a marker texture on the item, or the hand's distinctive `ModelViewMat` translation, or `display.firstperson_*` scale tricks |
| Shadow / custom pass | fog sentinels | use `FogRenderDistanceEnd` / `FogEnvironmentalEnd` sentinels; must be re-measured |
| Which texture is bound | `textureSize(Sampler0, 0)` | same |

Everything in the G3+ column marked "verify" or "re-measure" is exactly what
`instrumentation.md` exists for. Do not guess these; measure them once and write the value
into your pack's comments.

**The fast way:** capture a frame (`instrumentation.md § 2`), select the draw you want to
identify in the Event Browser, and read every uniform straight off the Pipeline State panel.
Change one thing in game, recapture, diff. That is minutes of work and gives exact values,
where the in-shader approach is an afternoon of guessing.

---

## 6. A worked conversion, both directions

```glsl
// world (camera-relative) -> screen
vec4 clip = ProjMat * ModelViewMat * vec4(worldPos, 1.0);
vec2 screen = clip.xy / clip.w * 0.5 + 0.5;
bool behindCamera = clip.z < 0.0;         // or clip.w <= 0.0

// screen + depth -> world (camera-relative); needs invViewProj from a data marker
vec3 unproject(mat4 invViewProj, vec2 uv, float depth) {
    vec4 clip = vec4(uv, depth, 1.0) * 2.0 - 1.0;
    vec4 p = invViewProj * clip;
    return p.xyz / p.w;
}
```
Note `vec4(uv, depth, 1.0) * 2.0 - 1.0` multiplies the `w` too, giving `1.0` — correct, and
shorter than writing the components out. It appears in this exact form throughout the
shipping packs.

---

## 7. Checklist for any transform you write

1. Which space is the input in? Name it in a comment.
2. Did you add the terrain offset (right one for the generation)?
3. Did you zero `ProjMat[3].xy` if this feeds reprojection or ray reconstruction?
4. Are you using `inverse()` where a `transpose` would do (and be exact)?
5. Is your y direction consistent from `gl_FragCoord` through to the sample?
6. Are you assuming a camera *position* exists? It does not.
7. Does the result still hold at 4:3 and 21:9?
