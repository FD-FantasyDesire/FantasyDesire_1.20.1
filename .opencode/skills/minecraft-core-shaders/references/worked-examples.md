# Worked examples — complete, minimal, compilable packs

Three known-good skeletons. Copy one, verify it works, **then** mutate it. Assembling a pack
from snippets fails far more often than editing a working one.

Each is written out in full: every file, every line. Where a binary asset is needed it is
described precisely enough to generate.

---

# Example A — minimal HUD (G3, pack_format 63, MC 1.21.6+)

An entity placed below `y = -1000` becomes a screen-anchored HUD element. No post pipeline,
so it works on every graphics setting.

**File tree**
```
pack.mcmeta
assets/minecraft/shaders/include/hud.glsl
assets/minecraft/shaders/core/rendertype_text.vsh
```

### `pack.mcmeta`
```json
{
  "pack": {
    "pack_format": 63,
    "supported_formats": [63, 99],
    "description": "Minimal HUD"
  }
}
```

### `assets/minecraft/shaders/include/hud.glsl`
```glsl
#version 150

// Anchoring bands, in world Y. A text display at y = -1500000 lands in the "centre" band.
#define HUD_GAP     1000000.0
#define HUD_REF_RES vec2(1920.0, 1080.0)
#define HUD_SCALE   vec2(100.0, 100.0)

bool is_hud(vec3 p) {
    return p.y < -1000.0;
}

// Requires: Position, ScreenSize (globals.glsl), and gl_Position to be assignable.
bool make_hud() {
    if (!is_hud(Position)) return false;

    vec3 pos = Position + vec3(0.0, 1.5 * HUD_GAP, 0.0);
    pos.x *= -1.0;

    float offset = 0.0;
    if (Position.y < -2.0 * HUD_GAP) {
        if (Position.y < -4.0 * HUD_GAP) {            // right-anchored
            pos.y += 2.0 * HUD_GAP;
            offset =  1.0 - (ScreenSize.y / 9.0 * 16.0) / ScreenSize.x;
        } else if (Position.y < -3.0 * HUD_GAP) {     // centre
            pos.y += 1.0 * HUD_GAP;
        } else {                                      // left-anchored
            offset = -1.0 + (ScreenSize.y / 9.0 * 16.0) / ScreenSize.x;
        }
        pos.y += 1.0 * HUD_GAP;
        pos.x *= (ScreenSize.y / 9.0 * 16.0) / ScreenSize.x;
    }

    pos.xy /= HUD_REF_RES * HUD_SCALE / 2.0;
    pos.x  += offset;
    pos.z  /= 1000000.0;                              // never z-fight with the world

    gl_Position = vec4(pos, 1.0);
    return true;
}
```

### `assets/minecraft/shaders/core/rendertype_text.vsh`
The vanilla G3 text shader, verbatim, plus two lines. **Extract the real file from the
client jar for your exact version** and apply the same two-line diff; this is the shape it
takes.
```glsl
#version 150

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

#moj_import <minecraft:globals.glsl>   // ScreenSize          -- ADDED
#moj_import <hud.glsl>                 //                     -- ADDED

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance   = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;

    if (make_hud()) {                  //                     -- ADDED
        vertexColor = Color;           // flat, unlit
        sphericalVertexDistance   = 0.0;
        cylindricalVertexDistance = 0.0;
        return;
    }
}
```

### Server side
```
summon text_display ~ -1500000 ~ {text:'{"text":"HELLO"}',billboard:"fixed",\
  shadow_radius:0f,view_range:1000f,\
  transformation:{translation:[0f,0f,0f],scale:[1f,1f,1f],\
                  left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]}}
```
Adjust the Y to pick the band. Confirm the NBT shape for your version.

### Verify
1. Load the pack, F3+T. Check `latest.log` for `Couldn't parse shader`.
2. Summon the entity. Text should appear pinned to the screen and stay put as you look
   around.
3. Resize the window and change GUI scale — the element must not drift.

---

# Example B — data marker + post pass (G2, pack_format 46, MC 1.21.4)

The foundational pattern: a terrain marker writes the projection matrix into a pixel strip,
and a post pass reconstructs world positions from depth and tints by height. Everything else
in `postfx-cookbook.md` is this plus a kernel.

**File tree**
```
pack.mcmeta
assets/minecraft/atlases/blocks.json
assets/minecraft/models/block/cube.json
assets/minecraft/textures/custom/marker.png          (1x1, RGB = 76,195,86, A = 255)
assets/minecraft/shaders/include/encodings.glsl
assets/minecraft/shaders/include/screenquad.glsl
assets/minecraft/shaders/core/terrain.vsh
assets/minecraft/shaders/core/terrain.fsh
assets/minecraft/shaders/core/rendertype_solid.json
assets/minecraft/shaders/core/rendertype_cutout.json
assets/minecraft/shaders/core/rendertype_cutout_mipped.json
assets/minecraft/post_effect/transparency.json
assets/minecraft/shaders/post/height_tint.vsh
assets/minecraft/shaders/post/height_tint.fsh
assets/minecraft/shaders/post/height_tint.json
```

### `pack.mcmeta`
```json
{ "pack": { "pack_format": 46, "description": "Data marker demo (Fabulous required)" } }
```

### `assets/minecraft/atlases/blocks.json`
```json
{ "sources": [ { "type": "directory", "source": "custom", "prefix": "custom/" } ] }
```

### `assets/minecraft/models/block/cube.json`
Overrides the parent of every cube-shaped block, adding a zero-volume marker face.
```json
{
    "parent": "block/block",
    "textures": { "marker": "custom/marker" },
    "elements": [
        {
            "from": [ 0, 0, 0 ],
            "to": [ 16, 16, 16 ],
            "faces": {
                "down":  { "texture": "#down",  "cullface": "down"  },
                "up":    { "texture": "#up",    "cullface": "up"    },
                "north": { "texture": "#north", "cullface": "north" },
                "south": { "texture": "#south", "cullface": "south" },
                "west":  { "texture": "#west",  "cullface": "west"  },
                "east":  { "texture": "#east",  "cullface": "east"  }
            }
        },
        {
            "from": [ 8, 8, 8 ],
            "to":   [ 8, 8, 8 ],
            "faces": { "up": { "uv": [ 8, 8, 8, 8 ], "texture": "#marker", "cullface": "up" } }
        }
    ]
}
```

### `assets/minecraft/textures/custom/marker.png`
1×1 pixel, RGBA `(76, 195, 86, 255)`. Generate with:
```python
from PIL import Image
Image.new("RGBA", (1, 1), (76, 195, 86, 255)).save("marker.png")
```

### `assets/minecraft/shaders/include/encodings.glsl`
```glsl
#version 330

#ifndef _ENCODINGS_GLSL
#define _ENCODINGS_GLSL

vec3 packSI24toF8x3(int i) {
    int sgn = int(i < 0);
    i = abs(i);
    return vec3(i & 0xFF, (i >> 8) & 0xFF, ((i >> 16) & 0x7F) | (sgn << 7)) / 255.0;
}
int unpackSI24fromF8x3(vec3 v) {
    ivec3 d = ivec3(v * 255.0);
    int sgn = d.b >> 7;
    int n = d.r | (d.g << 8) | ((d.b & 0x7F) << 16);
    return (sgn > 0 ? -1 : 1) * n;
}

#define FP_PRECISION_HIGH 400000.0
#define FP_PRECISION_LOW    1000.0

vec3  packFPtoF8x3(float x, float p)    { return packSI24toF8x3(int(round(x * p))); }
float unpackFPfromF8x3(vec3 v, float p) { return float(unpackSI24fromF8x3(v)) / p; }

#endif
```

### `assets/minecraft/shaders/include/screenquad.glsl`
```glsl
#version 150

#ifndef _SCREENQUAD_GLSL
#define _SCREENQUAD_GLSL

const vec4[] screenquad = vec4[](
    vec4(-1.0, -1.0, 0.0, 1.0),
    vec4( 1.0, -1.0, 0.0, 1.0),
    vec4( 1.0,  1.0, 0.0, 1.0),
    vec4(-1.0,  1.0, 0.0, 1.0)
);

#endif
```

### `assets/minecraft/shaders/core/terrain.vsh`
```glsl
#version 330

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ModelOffset;
uniform int  FogShape;
uniform vec2 ScreenSize;

out float vertexDistance;
out vec4  vertexColor;
out vec2  texCoord0;
flat out int dataQuad;

// 20 pixels: 0-15 projection matrix, 16-18 model offset (mod 16), 19 reserved
#define MARKER_WIDTH 20.0

void main() {
    vec3 pos = Position + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    // read the marker unmipped so distance does not blur the tag away
    ivec4 col = ivec4(round(textureLod(Sampler0, UV0, -4.0) * 255.0));
    dataQuad = (col.rgb == ivec3(76, 195, 86)) ? 1 : 0;

    vertexDistance = fog_distance(pos, FogShape);
    vertexColor    = Color * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0      = UV0;

    if (dataQuad > 0) {
        if (ModelOffset == vec3(0.0)) {          // inventory / GUI draw: hide it
            gl_Position = vec4(10.0, 10.0, 10.0, 1.0);
            return;
        }
        vec2 markerSize = vec2(MARKER_WIDTH, 1.0) * (2.0 / ScreenSize);
        vec2 c[4] = vec2[](vec2(0.0, 1.0), vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0));
        gl_Position = vec4(-1.0 + c[gl_VertexID % 4] * markerSize, 0.0, 1.0);
    }
}
```

### `assets/minecraft/shaders/core/terrain.fsh`
```glsl
#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:encodings.glsl>

uniform sampler2D Sampler0;

uniform vec4  ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4  FogColor;
uniform mat4  ProjMat;
uniform vec3  ModelOffset;

in float vertexDistance;
in vec4  vertexColor;
in vec2  texCoord0;
flat in int dataQuad;

out vec4 fragColor;

void main() {
    if (dataQuad > 0) {
        ivec2 pixel = ivec2(floor(gl_FragCoord.xy));
        if (pixel.y >= 1 || pixel.x >= 20) discard;

        if (pixel.x < 16) {
            int i = pixel.x;
            fragColor = vec4(packFPtoF8x3(ProjMat[i / 4][i % 4], FP_PRECISION_HIGH), 1.0);
        } else if (pixel.x < 19) {
            fragColor = vec4(packFPtoF8x3(mod(ModelOffset[pixel.x - 16], 16.0) / 16.0,
                                          FP_PRECISION_HIGH), 1.0);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
        return;
    }

    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) discard;
#endif
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
#ifndef TRANSLUCENT
    fragColor.a = 1.0;
#endif
}
```

### `assets/minecraft/shaders/core/rendertype_solid.json`
```json
{
    "vertex": "minecraft:core/terrain",
    "fragment": "minecraft:core/terrain",
    "samplers": [ { "name": "Sampler0" }, { "name": "Sampler2" } ],
    "uniforms": [
        { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1 ] },
        { "name": "ProjMat",      "type": "matrix4x4", "count": 16, "values": [ 1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1 ] },
        { "name": "ModelOffset",  "type": "float", "count": 3, "values": [ 0.0, 0.0, 0.0 ] },
        { "name": "ColorModulator","type": "float", "count": 4, "values": [ 1.0, 1.0, 1.0, 1.0 ] },
        { "name": "FogStart",     "type": "float", "count": 1, "values": [ 0.0 ] },
        { "name": "FogEnd",       "type": "float", "count": 1, "values": [ 1.0 ] },
        { "name": "FogColor",     "type": "float", "count": 4, "values": [ 0.0, 0.0, 0.0, 0.0 ] },
        { "name": "FogShape",     "type": "int",   "count": 1, "values": [ 0 ] },
        { "name": "ScreenSize",   "type": "float", "count": 2, "values": [ 1.0, 1.0 ] }
    ]
}
```

### `rendertype_cutout.json`, `rendertype_cutout_mipped.json`
Identical to the above, plus:
```json
    "defines": { "values": { "ALPHA_CUTOUT": "0.1" } },
```
inserted after `"fragment"`. Both point at the same `core/terrain` source.

### `assets/minecraft/post_effect/transparency.json`
```json
{
    "targets": { "swap": {} },
    "passes": [
        {
            "program": "minecraft:post/transparency",
            "inputs": [
                { "sampler_name": "Main",            "target": "minecraft:main" },
                { "sampler_name": "MainDepth",       "target": "minecraft:main",        "use_depth_buffer": true },
                { "sampler_name": "Translucent",     "target": "minecraft:translucent" },
                { "sampler_name": "TranslucentDepth","target": "minecraft:translucent", "use_depth_buffer": true },
                { "sampler_name": "ItemEntity",      "target": "minecraft:item_entity" },
                { "sampler_name": "ItemEntityDepth", "target": "minecraft:item_entity", "use_depth_buffer": true },
                { "sampler_name": "Particles",       "target": "minecraft:particles" },
                { "sampler_name": "ParticlesDepth",  "target": "minecraft:particles",   "use_depth_buffer": true },
                { "sampler_name": "Clouds",          "target": "minecraft:clouds" },
                { "sampler_name": "CloudsDepth",     "target": "minecraft:clouds",      "use_depth_buffer": true },
                { "sampler_name": "Weather",         "target": "minecraft:weather" },
                { "sampler_name": "WeatherDepth",    "target": "minecraft:weather",     "use_depth_buffer": true }
            ],
            "output": "swap"
        },
        {
            "program": "minecraft:post/height_tint",
            "inputs": [
                { "sampler_name": "In",    "target": "swap" },
                { "sampler_name": "Data",  "target": "minecraft:main" },
                { "sampler_name": "Depth", "target": "minecraft:main", "use_depth_buffer": true }
            ],
            "output": "minecraft:main"
        }
    ]
}
```
Note the marker strip lives in `minecraft:main`, which still holds the world at this point —
that is why `Data` reads `minecraft:main` while `In` reads the composited `swap`.

### `assets/minecraft/shaders/post/height_tint.json`
```json
{
    "vertex":   "minecraft:post/height_tint",
    "fragment": "minecraft:post/height_tint",
    "samplers": [ { "name": "InSampler" }, { "name": "DataSampler" }, { "name": "DepthSampler" } ],
    "uniforms": [ { "name": "OutSize", "type": "float", "count": 2, "values": [ 1.0, 1.0 ] } ]
}
```

### `assets/minecraft/shaders/post/height_tint.vsh`
```glsl
#version 330

#moj_import <minecraft:screenquad.glsl>
#moj_import <minecraft:encodings.glsl>

uniform sampler2D DataSampler;

out vec2 texCoord;
flat out mat4 invProj;
flat out vec3 modelOffset;
flat out int  hasData;

void main() {
    gl_Position = screenquad[gl_VertexID];
    texCoord = gl_Position.xy * 0.5 + 0.5;

    mat4 proj;
    for (int i = 0; i < 16; i++)
        proj[i / 4][i % 4] =
            unpackFPfromF8x3(texelFetch(DataSampler, ivec2(i, 0), 0).rgb, FP_PRECISION_HIGH);

    // validity: a real projection matrix always has a non-zero [1][1]
    hasData = abs(proj[1][1]) > 0.001 ? 1 : 0;
    invProj = inverse(proj);

    for (int i = 0; i < 3; i++)
        modelOffset[i] =
            unpackFPfromF8x3(texelFetch(DataSampler, ivec2(16 + i, 0), 0).rgb, FP_PRECISION_HIGH) * 16.0;
}
```

### `assets/minecraft/shaders/post/height_tint.fsh`
```glsl
#version 330

uniform sampler2D InSampler;
uniform sampler2D DataSampler;
uniform sampler2D DepthSampler;
uniform vec2 OutSize;

in vec2 texCoord;
flat in mat4 invProj;
flat in vec3 modelOffset;
flat in int  hasData;

out vec4 fragColor;

void main() {
    // keep the marker row intact for anything downstream
    if (int(floor(gl_FragCoord.y)) == 0) {
        fragColor = texelFetch(DataSampler, ivec2(gl_FragCoord.xy), 0);
        return;
    }

    fragColor = texture(InSampler, texCoord);
    if (hasData == 0) return;

    float depth = texture(DepthSampler, texCoord).r;
    if (depth == 1.0) return;                       // sky

    vec4 clip = vec4(texCoord, depth, 1.0) * 2.0 - 1.0;
    vec4 v = invProj * clip;
    vec3 viewPos = v.xyz / v.w;                     // camera-relative, view space

    float h = clamp(-viewPos.z / 64.0, 0.0, 1.0);   // distance, 0..64 blocks
    fragColor.rgb = mix(fragColor.rgb, vec3(1.0, 0.4, 0.2), h * 0.5);
}
```

### Verify
1. Graphics = **Fabulous!**
2. Look at any block. Distant geometry should tint orange.
3. Look at the bottom-left corner: a 20×1 strip of coloured pixels should be visible. It
   should *change* as you turn — that is the projection matrix updating.
4. If everything is uniformly untinted, `hasData` is 0: the marker is not reaching the strip.
   Check that the block you are looking at inherits `block/block` via `block/cube`.

---

# Example C — a server-toggleable post effect (G5, pack_format 93, MC 26.3)

The smallest useful modern post effect: a coloured square in a screen corner that the server
can switch on and off by name.

**File tree**
```
pack.mcmeta
assets/demo/post_effect/badge.json
assets/demo/shaders/post/region.vsh
assets/demo/shaders/post/solid.fsh
```

### `pack.mcmeta`
```json
{ "pack": { "pack_format": 93, "description": "Toggleable badge" } }
```

### `assets/demo/post_effect/badge.json`
```json
{
  "targets": {},
  "passes": [
    {
      "vertex_shader": "demo:post/region",
      "fragment_shader": "demo:post/solid",
      "output": "minecraft:main",
      "uniforms": {
        "RegionConfig": [
          { "name": "Rect", "type": "vec4", "value": [ 8.0, 8.0, 24.0, 24.0 ] }
        ],
        "SolidConfig": [
          { "name": "Color", "type": "vec4", "value": [ 1.0, 0.35, 0.1, 1.0 ] }
        ]
      }
    }
  ]
}
```

### `assets/demo/shaders/post/region.vsh`
```glsl
#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform SamplerInfo  { vec2 OutSize; };
layout(std140) uniform RegionConfig { vec4 Rect; };   // x, y, w, h in output pixels

layout(location = 0) out vec2 localPos;

void main() {
    vec2 uv = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);

    // +2px slack so the far corner is strictly inside the triangle
    vec2 span = Rect.zw + vec2(2.0);
    vec2 px   = Rect.xy + uv * span;

    gl_Position = vec4(px / OutSize * 2.0 - 1.0, 0.0, 1.0);
    localPos = uv * span;
}
```

### `assets/demo/shaders/post/solid.fsh`
```glsl
#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform RegionConfig { vec4 Rect; };
layout(std140) uniform SolidConfig  { vec4 Color; };

layout(location = 0) in vec2 localPos;
layout(location = 0) out vec4 fragColor;

void main() {
    if (localPos.x >= Rect.z || localPos.y >= Rect.w) discard;
    fragColor = Color;
}
```

### Why this is the template for a data bus
The pass rasterises ~26 fragments and **does not clear its output**, so N of these can be
stacked with no measurable cost. Duplicate `badge.json` N times with different `Rect.x`
values and you have a set of per-bit writers (`data-channels.md § 10`). Add a `header`
effect that blanks the strip and stamps a magic pattern, and a `hud` effect that grabs,
patches and decodes, and you have a complete server→shader channel.

### Verify
1. Enable the effect by name from the server.
2. An orange 24×24 square appears 8 px from the bottom-left.
3. Disable it — the square disappears with no residue. If a smear remains, a later pass is
   not repainting that region; that is the no-clear behaviour, and it is why the `header`
   pass exists in the real design.

---

## Using these

- Get one running **before** writing anything of your own. A working baseline turns every
  later failure into a bisect instead of a mystery.
- Keep the marker strip visible during development; hide it last.
- When you port an example to another generation, change one thing at a time and reload
  after each: includes, then uniforms, then the pipeline JSON, then the logic.
