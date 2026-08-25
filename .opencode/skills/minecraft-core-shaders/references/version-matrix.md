# Version matrix — the syntax that changes under you

Facts here were established by inspecting shipping packs and vanilla shader sources for each
version, unless marked **(inferred)**. When a fact is load-bearing and you are unsure, get the
vanilla shader template for that exact version and check (`rendertype-inventory.md § 4`) —
that settles any question in this file definitively.

---

## 0. pack_format ↔ Minecraft version

| pack_format | Minecraft |
|---|---|
| 15 | 1.20.1–1.20.2 |
| 22 | 1.20.3–1.20.4 |
| 32 | 1.20.5–1.20.6 |
| 34 | 1.21–1.21.1 |
| 42 | 1.21.2–1.21.3 |
| 45 | 24w46a (pre-1.21.4) |
| 46 | 1.21.4 |
| 55 | 1.21.5 |
| 56–62 | 1.21.6 snapshots |
| 63 | 1.21.6 |
| 64 | 1.21.7–1.21.8 |
| 75 | 26.0/26.1 era |
| 93 | 26.3 |

Shipping packs confirm these boundaries in a useful way: multi-version packs declare overlay
ranges at exactly the points where the syntax changes, and those declared ranges are the best
available evidence for where a generation begins.

There are five **generations** with genuinely different shader syntax. Call them G1–G5.

| Gen | pack_format | Name used below |
|---|---|---|
| G1 | ≤ 41 | *classic* (1.20.x – 1.21.1) |
| G2 | 42 – 55 | *consolidated* (1.21.2 – 1.21.5) |
| G3 | 56 – 68 | *UBO* (1.21.6 – 1.21.8) |
| G4 | 69 – ~80 | *chunk-section* (1.21.9 – 26.1) |
| G5 | ~81 – 93+ | *new-backend* (26.2 – 26.3) |

G4 and G5 are G3 plus deltas; read G3 first.

---

## 1. G1 — classic (pack_format ≤ 41, MC 1.20.x – 1.21.1)

### File layout

```
assets/minecraft/
  shaders/
    core/rendertype_solid.{vsh,fsh,json}      one .json PER render type
    core/rendertype_cutout.{vsh,fsh,json}
    core/rendertype_entity_translucent_cull.{vsh,fsh,json}
    core/position_tex.{vsh,fsh,json}   ... etc (one file per pipeline)
    include/fog.glsl                          overrides the vanilla include
    include/light.glsl
    include/<your own>.glsl
    post/transparency.json                    POST PIPELINE (targets = ARRAY)
    program/<name>.{vsh,fsh,json}             POST PROGRAMS
pack.mcmeta
```

### Includes

Unnamespaced only: `#moj_import <fog.glsl>`, `#moj_import <light.glsl>`.
Resolution is `assets/minecraft/shaders/include/<name>`, and **a pack that ships its own
`include/fog.glsl` overrides the vanilla one for every shader in the game.** Packs use this
to reshape fog and the lightmap globally, and to smuggle in state sentinels.

Built-in `fog.glsl` provides:
```glsl
vec4  linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor);
float linear_fog_fade(float vertexDistance, float fogStart, float fogEnd);
float fog_distance(vec3 pos, int shape);                      // 1.20.3+
float fog_distance(mat4 modelViewMat, vec3 pos, int shape);   // older signature, still present in some versions
```
Built-in `light.glsl` provides:
```glsl
vec4 minecraft_mix_light(vec3 lightDir0, vec3 lightDir1, vec3 normal, vec4 color);
vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv);   // texture(), smooth
```

### Core pipeline JSON (required, one per render type)

```json
{
    "blend": { "func": "add", "srcrgb": "srcalpha", "dstrgb": "1-srcalpha" },
    "vertex": "rendertype_cutout",
    "fragment": "rendertype_cutout",
    "attributes": [ "Position", "Color", "UV0", "UV2", "Normal" ],
    "samplers": [ { "name": "Sampler0" }, { "name": "Sampler2" } ],
    "uniforms": [
        { "name": "ModelViewMat",  "type": "matrix4x4", "count": 16, "values": [ 1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1 ] },
        { "name": "ProjMat",       "type": "matrix4x4", "count": 16, "values": [ 1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1 ] },
        { "name": "ChunkOffset",   "type": "float", "count": 3, "values": [ 0,0,0 ] },
        { "name": "ColorModulator","type": "float", "count": 4, "values": [ 1,1,1,1 ] },
        { "name": "FogStart",      "type": "float", "count": 1, "values": [ 0 ] },
        { "name": "FogEnd",        "type": "float", "count": 1, "values": [ 1 ] },
        { "name": "FogColor",      "type": "float", "count": 4, "values": [ 0,0,0,0 ] },
        { "name": "FogShape",      "type": "int",   "count": 1, "values": [ 0 ] },
        { "name": "GameTime",      "type": "float", "count": 1, "values": [ 0 ] }
    ]
}
```

Rules:
- `"attributes"` lists the vertex attributes your `.vsh` declares. It exists only in G1 and
  is **optional** — several pf-34 packs omit it on core pipelines while others
  include it; post programs almost always declare
  `[ "Position" ]`. Include it when you are unsure; an attribute you use but did not declare
  can fail to bind on some drivers.
- **A uniform not listed here does not exist in the shader.** Adding
  `{"name":"GameTime",...}` or `{"name":"ScreenSize","type":"float","count":2,...}` is how
  you gain access to them — the single most common G1 edit.
- `"vertex"`/`"fragment"` are bare names (no `minecraft:` prefix, no `core/`).
- Other uniforms you can request: `IViewRotMat` (`matrix3x3`), `TextureMat`,
  `Light0_Direction`, `Light1_Direction`, `LineWidth`, `GlintAlpha`, `ScreenSize`,
  `GameTime`, `EndPortalLayers`.

### Vertex-shader boilerplate (terrain)

```glsl
#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ChunkOffset;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    vec3 pos = Position + ChunkOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    vertexDistance = fog_distance(pos, FogShape);
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
}
```

Fragment tail: `fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);`

### Post pipeline JSON — `shaders/post/<name>.json`

`targets` is an **array**; entries are bare strings or `{name,width,height}`.
Passes address programs by bare name; targets by name, with `:depth` for depth attachments.

```json
{
    "targets": [
        "translucent", "itemEntity", "particles", "clouds", "weather",
        "swap",
        { "name": "map", "width": 512, "height": 512 }
    ],
    "passes": [
        {
            "name": "ssao",
            "intarget": "minecraft:main",
            "outtarget": "ao",
            "auxtargets": [
                { "name": "DiffuseDepthSampler", "id": "minecraft:main:depth" },
                { "name": "NormalSampler",       "id": "normals" },
                { "name": "NoiseSampler", "id": "blue_noise", "width": 1, "height": 1, "bilinear": false }
            ],
            "uniforms": [ { "name": "Direction", "values": [ 0.0, 1.0 ] } ]
        },
        { "name": "blit", "intarget": "swap", "outtarget": "minecraft:main" }
    ]
}
```

- `intarget` is bound as `DiffuseSampler` (its depth as `DiffuseDepthSampler` if you aux it).
- An `auxtargets` entry whose `id` is **not** a declared target loads
  `assets/minecraft/textures/effect/<id>.png` — that is how `blue_noise`, `palette`,
  `cursor`, `transmittance` get in. `width`/`height` there are hints; `bilinear` toggles
  filtering.
- `"use_linear_filter": true` on a pass enables linear sampling of `intarget`.
- Built-in programs referenced by bare name: `blit`, `transparency`, `copy`, `sobel`,
  `blur`, `entity_outline_box_blur`, `phosphor`, `invert`.
- Overriding `shaders/post/transparency.json` is how packs inject work into the Fabulous
  transparency pipeline. **This only runs on Graphics = Fabulous!** Say so in the pack
  description. `shaders/post/blur.json` (menu blur) and `shaders/post/entity_outline.json`
  are the other two commonly hijacked pipelines.
- The stock `transparency` pass expects these aux samplers, and you must keep supplying
  them if you re-declare the pipeline: `TranslucentSampler`/`TranslucentDepthSampler`,
  `ItemEntitySampler`/`ItemEntityDepthSampler`, `ParticlesSampler`/`ParticlesDepthSampler`,
  `CloudsSampler`/`CloudsDepthSampler`, `WeatherSampler`/`WeatherDepthSampler`,
  `DiffuseDepthSampler`.

### Post program JSON — `shaders/program/<name>.json`

Same schema as a core pipeline JSON. Standard uniforms available to post programs:
`ProjMat` (an ortho matrix over the screen quad), `InSize`, `OutSize`, `Time`
(0..1 fractional, wraps each second), plus any you declare with constant `values` and
override per-pass with `"uniforms"` in the pipeline JSON.

Post vertex shaders receive `in vec4 Position` (a screen quad in pixel space). Two idioms:

```glsl
// (a) use the provided quad
vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
gl_Position = vec4(outPos.xy, 0.2, 1.0);
texCoord = outPos.xy * 0.5 + 0.5;

// (b) ignore it and emit NDC corners — required when you index by gl_VertexID
const vec4[] corners = vec4[](vec4(-1,-1,0,1), vec4(1,-1,0,1), vec4(1,1,0,1), vec4(-1,1,0,1));
gl_Position = corners[gl_VertexID];
texCoord = gl_Position.xy * 0.5 + 0.5;
```

---

## 2. G2 — consolidated (pack_format 42–55, MC 1.21.2 – 1.21.5)

Everything from G1 still applies **except** the deltas below.

### Delta 1 — namespaced includes

```glsl
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:light.glsl>
```
Your own includes may live in any namespace and are imported with it:
`assets/settings/shaders/include/settings.glsl` → `#moj_import <settings:settings.glsl>`.
This is the clean way to expose user-editable settings.
Unnamespaced (`#moj_import <fog.glsl>`) still resolves inside `minecraft:`.

### Delta 2 — shaders consolidated, driven by `defines`

Instead of `rendertype_solid.vsh` + `rendertype_cutout.vsh` + …, there are a few shared
sources — `core/terrain.{vsh,fsh}`, `core/entity.{vsh,fsh}`, `core/rendertype_text.{vsh,fsh}`,
`core/particle`, `core/position_tex`, … — and each `rendertype_*.json` selects one plus a
set of preprocessor defines:

```json
{
    "vertex": "minecraft:core/terrain",
    "fragment": "minecraft:core/terrain",
    "defines": {
        "flags":  [ "TRANSLUCENT" ],
        "values": { "ALPHA_CUTOUT": "0.1" }
    },
    "samplers": [ { "name": "Sampler0" }, { "name": "Sampler2" } ],
    "uniforms": [ /* as G1 */ ]
}
```

- `flags` → `#define TRANSLUCENT` ; `values` → `#define ALPHA_CUTOUT 0.1`.
- **`"attributes"` is gone** — attributes are inferred from the vertex format.
- `"vertex"`/`"fragment"` are full resource paths: `minecraft:core/terrain`, or your own
  file, e.g. `"minecraft:text/hand"` → `shaders/text/hand.{vsh,fsh}`. This lets you point
  a vanilla render type at a completely custom shader file.
- Common flags in vanilla sources: `ALPHA_CUTOUT`, `TRANSLUCENT`, `EMISSIVE`, `NO_OVERLAY`,
  `NO_CARDINAL_LIGHTING`, `APPLY_TEXTURE_MATRIX`, `LINEAR_FILTERING`.
- Write one source that serves every render type:
  ```glsl
  #ifdef ALPHA_CUTOUT
      if (color.a < ALPHA_CUTOUT) discard;
  #endif
  #ifndef NO_OVERLAY
      color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
  #endif
  #ifndef EMISSIVE
      color *= lightMapColor;
  #endif
  ```

### Delta 3 — `ChunkOffset` → `ModelOffset`

Terrain vertex offset uniform is renamed to `vec3 ModelOffset`, same meaning. Everything
that keyed off `ChunkOffset == vec3(0.0)` to detect a non-terrain pass must be updated.

### Delta 4 — `post_effect/` replaces `shaders/post/*.json`

The **pipeline** moves to `assets/<ns>/post_effect/<name>.json` with an object-shaped
`targets` and a new pass schema; the **programs** move to
`assets/<ns>/shaders/post/<name>.{vsh,fsh,json}`.

```json
{
    "targets": {
        "emissive": {},
        "voxel_history": { "width": 4000, "height": 3000 },
        "data": { "width": 100, "height": 1 }
    },
    "passes": [
        {
            "program": "minecraft:post/emissive_pixels",
            "inputs": [
                { "sampler_name": "In",    "target": "minecraft:main" },
                { "sampler_name": "Depth", "target": "minecraft:main", "use_depth_buffer": true },
                { "sampler_name": "Noise", "location": "noise", "width": 512, "height": 512 },
                { "sampler_name": "Prev",  "target": "history", "bilinear": true }
            ],
            "uniforms": [ { "name": "Iteration", "values": [ 1.0 ] } ],
            "output": "emissive"
        }
    ]
}
```

Naming rule: `"sampler_name": "In"` → the GLSL uniform is **`InSampler`**. The suffix
`Sampler` is appended automatically. `"location"` loads a texture from
`assets/<ns>/textures/effect/<location>.png` instead of a target.

Built-in targets you can read: `minecraft:main`, `minecraft:translucent`,
`minecraft:item_entity`, `minecraft:particles`, `minecraft:clouds`, `minecraft:weather`,
`minecraft:entity_outline`. Built-in programs: `minecraft:post/transparency`,
`minecraft:post/blit`, `minecraft:post/blur`, `minecraft:post/entity_outline`,
`minecraft:post/box_blur`.

Post program JSON keeps its G1 shape but lives at `shaders/post/<n>.json` with namespaced
names:
```json
{
    "vertex": "minecraft:post/downsample",
    "fragment": "minecraft:post/downsample",
    "samplers": [ { "name": "InSampler" } ],
    "uniforms": [
        { "name": "OutSize",   "type": "float", "count": 2, "values": [ 1.0, 1.0 ] },
        { "name": "Iteration", "type": "float", "count": 1, "values": [ 1.0 ] }
    ]
}
```
Post vertex shaders in G2 typically declare **no `in` attributes at all** and build the quad
from `gl_VertexID`.

### Delta 5 — `ScreenSize` becomes routinely available

Declare `{ "name": "ScreenSize", "type": "float", "count": 2, "values": [1.0, 1.0] }` in the
pipeline JSON and `uniform vec2 ScreenSize;` in GLSL. Anything that sizes a data structure
from the framebuffer needs it.

---

## 3. G3 — UBO (pack_format 56–68, MC 1.21.6 – 1.21.8)

This is the biggest break. **Do not carry any G1/G2 boilerplate across this line.**

### Delta 1 — core pipeline `.json` files are gone from resource packs

`assets/minecraft/shaders/core/` contains **only `.vsh` and `.fsh`**. Pipelines are defined
in Java. You cannot add uniforms, change blend modes, or add samplers any more. You can only
replace GLSL source. This is directly observable in multi-version packs: their `[46,55]`
overlays contain `.json` files under `shaders/core/` and their `[56,99]` overlays do not.

Practical consequence: any G1/G2 trick that worked by *adding a uniform to the JSON* must be
redesigned for G3+ around the UBO contents that already exist.

### Delta 2 — uniforms move into uniform buffer objects, delivered by includes

```glsl
#moj_import <minecraft:dynamictransforms.glsl>   // ModelViewMat, ColorModulator, ModelOffset, TextureMat, LineWidth
#moj_import <minecraft:projection.glsl>          // ProjMat
#moj_import <minecraft:globals.glsl>             // ScreenSize, GlintAlpha, GameTime, MenuBlurRadius
#moj_import <minecraft:fog.glsl>                 // FogColor, FogEnvironmentalStart/End, FogRenderDistanceStart/End, ...
#moj_import <minecraft:light.glsl>               // Light0_Direction, Light1_Direction + mix/sample helpers
```
Never redeclare these as loose `uniform`s — that is a redefinition error. `sampler2D`
uniforms (`Sampler0`, `Sampler1`, `Sampler2`) are still declared normally.

### Delta 3 — fog API rewritten

| G1/G2 | G3+ |
|---|---|
| `float vertexDistance` | `float sphericalVertexDistance` **and** `float cylindricalVertexDistance` |
| `fog_distance(pos, FogShape)` | `fog_spherical_distance(pos)` / `fog_cylindrical_distance(pos)` |
| `linear_fog(color, d, FogStart, FogEnd, FogColor)` | `apply_fog(color, sphD, cylD, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor)` |
| `FogStart` / `FogEnd` / `FogShape` | `FogEnvironmentalStart/End`, `FogRenderDistanceStart/End`, `FogSkyEnd`, `FogCloudsEnd` |

Consequence: **`FogStart > 1e32` no longer works as a first-person-hand / GUI detector.**
See `geometry-hijacking.md` for G3+ replacements.

### G3 vertex boilerplate (entity)

```glsl
#version 150

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    sphericalVertexDistance   = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
#ifdef NO_CARDINAL_LIGHTING
    vertexColor = Color;
#else
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
#endif
#ifndef EMISSIVE
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
#endif
    overlayColor = texelFetch(Sampler1, UV1, 0);
    texCoord0 = UV0;
#ifdef APPLY_TEXTURE_MATRIX
    texCoord0 = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
#endif
}
```

### G3 fragment tail

```glsl
fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance,
                      FogEnvironmentalStart, FogEnvironmentalEnd,
                      FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
```

### Delta 4 — post-effect passes name shaders directly, uniforms become named UBOs

No more post-program `.json`. Each pass names a vertex and fragment shader and supplies
uniform-block contents inline:

```json
{
  "targets": { "swap": {} },
  "passes": [
    {
      "vertex_shader":   "minecraft:core/screenquad",
      "fragment_shader": "minecraft:post/entityglow",
      "inputs": [ { "sampler_name": "In", "target": "minecraft:entity_outline" } ],
      "output": "swap",
      "uniforms": {
        "BlurConfig": [ { "name": "BlurDir", "type": "vec2", "value": [ 1.0, 0.0 ] } ]
      }
    }
  ]
}
```
and in the fragment shader:
```glsl
#version 330
layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
layout(std140) uniform BlurConfig  { vec2 BlurDir; };
uniform sampler2D InSampler;
in vec2 texCoord;
out vec4 fragColor;
```
- `SamplerInfo` is provided automatically and carries `OutSize` (and `InSize` where a
  sampler is bound). Declare only the members you use, in order, matching std140 layout.
- Useful built-in vertex shaders: `minecraft:core/screenquad`, `minecraft:post/blit`.
- Useful built-in fragment shaders: `minecraft:post/blit` (with
  `BlitConfig { vec4 ColorModulate; }`), `minecraft:post/transparency`,
  `minecraft:post/rotate_180`, `minecraft:post/earthquake`
  (with `QuakeConfig { float Amplitude; float Frequency; }`).
- Uniform `type` strings seen in shipping packs: `"float"`, `"vec2"`, `"vec4"`. `"value"` is a
  scalar for `float`, an array otherwise.

---

## 4. G4 — chunk-section (pack_format ≈69–80, MC 1.21.9 – 26.1)

G3 plus:

### Delta 1 — terrain positioning uses a chunk-section UBO

```glsl
#moj_import <minecraft:chunksection.glsl>   // ChunkPosition, CameraBlockPos, CameraOffset

vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset;
```
`ModelOffset` no longer positions terrain. Getting the **world block position** of a terrain
vertex becomes `ivec3(floor(Position)) + ChunkPosition` instead of the G1/G2
`ivec3(floor(Position + floor(ModelOffset)))`. Any voxelization or minimap code must be
ported accordingly.

A G4 terrain shader need not import `minecraft:light.glsl` at all (terrain has no
`Light*_Direction`) and declares the lightmap sampler helper locally instead — a useful
pattern when you only need one helper from an include whose UBO you do not have:
```glsl
vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp((uv / 256.0) + 0.5 / 16.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}
```

### Delta 2 — per-face lighting

New flag `PER_FACE_LIGHTING` and helpers:
```glsl
#ifdef PER_FACE_LIGHTING
    vec2 light = minecraft_compute_light(Light0_Direction, Light1_Direction, Normal);
    vertexPerFaceColorBack  = minecraft_mix_light_separate(-light, Color);
    vertexPerFaceColorFront = minecraft_mix_light_separate( light, Color);
#else
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
#endif
```
Your entity shader must emit **both** `vertexPerFaceColorBack` and
`vertexPerFaceColorFront` when the flag is set, or the pipeline will not link.

### Delta 3 — `#version 330` is the norm for core shaders

`gl_VertexID` still exists here.

### Delta 4 — `pack.mcmeta` gains explicit format bounds

```json
{"pack": {"pack_format": 75, "min_format": 75, "max_format": 75, "description": "..."}}
```
Some tooling emits `"max_format": [75, 0]`; both forms are seen in the wild.

---

## 5. G5 — new backend (pack_format ≈81–93+, MC 26.2 – 26.3)

G3/G4 plus a Vulkan-flavoured GLSL dialect for **post-effect** shaders. Verified against
pack_format 93 / MC 26.3.

```glsl
#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform SamplerInfo   { vec2 OutSize; };
layout(std140) uniform RegionConfig  { vec4 Rect; };

layout(location = 0) in  vec2 localPos;
layout(location = 0) out vec4 fragColor;

void main() {
    vec2 uv = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);   // NOT gl_VertexID
    ...
}
```

Deltas:
- **`gl_VertexIndex` replaces `gl_VertexID`** in post shaders.
- `layout(location = N)` is required on varyings (`GL_ARB_separate_shader_objects`).
- Custom namespaces work throughout: `"vertex_shader": "cs:post/region"` →
  `assets/cs/shaders/post/region.vsh`.
- Post passes **do not clear their output target** — `PostPass` uses an empty clear colour,
  so a pass that rasterises only a small rectangle leaves the rest untouched. This makes
  "one post effect per pixel of payload" affordable.
- A single triangle covering a pixel rectangle of the output target:
  ```glsl
  vec2 uv   = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);
  vec2 span = Rect.zw + vec2(2.0);          // +2px slack so the far corner is inside
  vec2 px   = Rect.xy + uv * span;
  gl_Position = vec4(px / OutSize * 2.0 - 1.0, 0.0, 1.0);
  localPos = uv * span;                      // pixel-space coords for the fragment shader
  ```
- Under the OpenGL backend NDC +y is up, so anchoring `(0, 1)` is the top-left. Do not
  hardcode a y direction — take it from a UBO value so the pipeline JSON can flip it.
- **Post effects became individually addressable by name**
  (`assets/<ns>/post_effect/<id>.json`), so a server can enable/disable them at runtime.
  This is the basis of the modern server→shader data bus: N one-pixel post effects, each
  writing white to one pixel, give N bits the server can set by name. See `data-channels.md`.

---

## 6. Cross-generation invariants

Stable across G1–G5, and the bedrock of the whole discipline.

**Vertex attributes** (names fixed by the vertex format, not by you):

| Attribute | Type | Meaning |
|---|---|---|
| `Position` | `vec3` | model / chunk-section space |
| `Color` | `vec4` | vertex tint, 0–1, quantised from bytes (so 256 distinct values per channel) |
| `UV0` | `vec2` | texture coords into `Sampler0` |
| `UV1` | `ivec2` | overlay (hurt / flash) coords → `texelFetch(Sampler1, UV1, 0)` |
| `UV2` | `ivec2` | lightmap coords 0–240 step 16 → `texelFetch(Sampler2, UV2/16, 0)` |
| `Normal` | `vec3` | normal |

**Samplers**: `Sampler0` = albedo / atlas, `Sampler1` = overlay, `Sampler2` = lightmap
(a 16×16 texture). Post shaders get whatever the pipeline binds.

**Builtins**: `gl_VertexID` (G1–G4; `gl_VertexIndex` in G5 post), `gl_FragCoord`,
`gl_PrimitiveID`, `gl_FragDepth`, `discard`, `dFdx`/`dFdy`/`fwidth`, `textureSize`,
`texelFetch`, `textureLod`. Quads are 4 consecutive vertices, so `gl_VertexID % 4`
identifies the corner and `gl_VertexID / 4` the quad index — the foundation of every
geometry hijack.

**Not available, ever**: compute shaders, SSBOs, image load/store, geometry/tessellation
stages, readback to the CPU, custom uniforms pushed from a server, and any per-frame CPU
logic. `#version 330` (and `#version 150` for older targets) is the ceiling; `#extension`
beyond what the backend already enables will not link.

---

## 7. Shipping one pack for many versions

Two mechanisms, used together by every serious pack in the shipping packs.

### 7a. `overlays` in `pack.mcmeta`

```json
{
  "pack": {
    "pack_format": 64,
    "supported_formats": [34, 99],
    "description": "..."
  },
  "overlays": {
    "entries": [
      { "directory": "shader_legacy", "formats": [46, 55] },
      { "directory": "shader_modern", "formats": [56, 99] },
      { "directory": "bettermodel_legacy", "formats": [22, 45], "min_format": 22, "max_format": 45 },
      { "directory": "bettermodel_modern", "formats": [46, 99], "min_format": 46, "max_format": 99 }
    ]
  }
}
```
Each overlay directory holds a full `assets/` tree layered on top of the base when the
client's pack format falls in range. `formats` may be `[min,max]` or a single int;
`min_format`/`max_format` are the newer spelling — emit both for maximum compatibility
(shipping multi-version packs emit both).

Put version-sensitive shader sources in overlays; keep textures/models/fonts in the base.
The natural split is `[46,55]` / `[56,99]` — the G2/G3 line.

### 7b. Stub includes

`#moj_import <minecraft:globals.glsl>` fails to resolve on versions where the include does
not exist, killing the shader. Ship a stub in the overlay for the old versions:

```
betterhud_1_21_2/assets/minecraft/shaders/include/globals.glsl           →  "#version 150"
betterhud_1_21_2/assets/minecraft/shaders/include/dynamictransforms.glsl →  "#version 150"
```
…and declare the same names as ordinary uniforms in that version's pipeline `.json`
(`ModelViewMat`, `ProjMat`, `ScreenSize`, `GameTime`). One shader source then compiles on
both sides of the line.

Corollary: **an empty stub named after a vanilla include silently disables that include on
versions where it does exist.** Only put stubs in version-scoped overlays, never in the base.

### 7c. Version-stamped source

Put `#define SHADER_VERSION 0|1|2|3` at the top of each overlay's copy of the same shader so
runtime code can tell which variant loaded. Cheap; worth doing.

### 7d. Runtime feature detection inside GLSL

When overlays are not available (a single pack that must work everywhere), branch on
observable differences instead:
- `ProjMat[2][3] == 0.0` → orthographic (GUI) in every generation.
- `textureSize(Sampler0, 0)` → tells you which atlas/texture is bound.
- A `#ifdef` on a define you set yourself per overlay is always preferable to guessing.

---

## 8. Sodium / Iris compatibility declaration

Sodium replaces terrain rendering and ignores core shaders unless told otherwise. Packs
declare which of their shader files Sodium should not warn about, in `pack.mcmeta`:

```json
{
  "pack": { "pack_format": 34, "description": "..." },
  "sodium": {
    "ignored_shaders": [
      "rendertype_solid.json", "rendertype_solid.fsh", "rendertype_solid.vsh",
      "rendertype_cutout.json", "rendertype_cutout_mipped.json",
      "rendertype_translucent.json", "light.glsl", "fog.glsl"
    ]
  }
}
```
Packs that must work on Sodium ship a compatibility mod alongside. If a pack modifies terrain
shaders and must work on Sodium without a helper mod, it cannot — say so up front rather than
shipping something that silently does nothing.
