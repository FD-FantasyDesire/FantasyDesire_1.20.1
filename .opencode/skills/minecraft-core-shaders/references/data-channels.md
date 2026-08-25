# Data channels — how information reaches a core shader

A core shader can only read: its declared uniforms, its vertex attributes, its bound
textures, and (for post shaders) its input render targets. Every "custom" value in this
value arrives through one of the channels below. Pick by bandwidth, latency, and what
part of the frame you need it in.

| # | Channel | Direction | Bandwidth | Latency | Needs |
|---|---|---|---|---|---|
| 1 | Data marker / data quad | core → post | ~40 px × 24 bit | same frame | a marker texture on a drawn model |
| 2 | Lightmap alpha | client → core fsh | 16×16 bytes = 256 B | same frame | override `core/lightmap.fsh` |
| 3 | `GameTime` abuse (world age packet) | server → core | 1 value (~8 bit useful) | 1 packet | server control of time |
| 4 | Texture magic pixels | pack author → shader | unlimited (whole texture) | static | atlas control |
| 5 | Texture **alpha** tag | pack author → shader | 8 bit per texel | static | atlas control |
| 6 | Vertex `Color` | server → shader | 24–32 bit per quad | 1 tick | control of the entity's tint |
| 7 | Position / coordinate encoding | server → shader | ~30 bit | 1 tick | control of entity position |
| 8 | Map item pixels | server → shader | 16 KB per map | 1 packet | map items |
| 9 | Render-target persistence | frame N → N+1 | a whole target | 1 frame | a private post target |
| 10 | Post-effect toggles | server → post shader | 1 bit per effect | 1 packet | MC 26.2+ |
| 11 | `ProjMat` / `FogStart` / matrices | client state → shader | implicit | same frame | nothing |
| 12 | Item / entity render targets | core → post | full-screen RGBA | same frame | Fabulous pipeline |

---

## 1. Data marker (a.k.a. data quad, marker pixel strip)

**The single most important technique in this discipline.** Post shaders cannot see `ProjMat`,
`ModelViewMat`, `ChunkOffset`, `GameTime`, or anything else the core shader knows. So the
core shader *draws* those values into the bottom-left corner of the screen as a strip of
pixels, and the post shader reads them back with `texelFetch`.

### The carrier

Add a **zero-volume element** with a unique marker texture to a block or item model that is
guaranteed to be on screen. Zero volume means it produces geometry but no visible area.

```json
{
  "parent": "block/block",
  "textures": { "marker": "custom/marker" },
  "elements": [
    { "from": [0,0,0], "to": [16,16,16], "faces": { "down": {"texture":"#down","cullface":"down"}, "...": {} } },
    { "from": [8,8,8], "to": [8,8,8],
      "faces": { "up": { "uv": [8,8,8,8], "texture": "#marker", "cullface": "up" } } }
  ]
}
```
Register the marker texture in an atlas source so it gets stitched:
`assets/minecraft/atlases/blocks.json`
```json
{ "sources": [ { "type": "directory", "source": "custom", "prefix": "custom/" } ] }
```

Alternatives to a block model: an item-display / item-frame item with a
`custom_model_data` override, or a texture pixel on any always-visible entity.

### Detect it in the vertex shader

```glsl
ivec4 col = ivec4(round(texture(Sampler0, UV0) * 255.0));
dataQuad = col.rgb == ivec3(76, 195, 86) ? 1 : 0;   // pick a colour nothing else uses
```
Use `textureLod(Sampler0, UV0, -4)` instead of `texture` when mipmapping might blur the
marker.

### Move it to a fixed screen rectangle

```glsl
if (dataQuad > 0) {
    if (ChunkOffset == vec3(0.0)) {         // reject the block-in-inventory / GUI draw
        gl_Position = vec4(10.0, 10.0, 10.0, 1.0);   // off-screen, cheap discard
        return;
    }
    vec4 corner = vec4(-1.0, -1.0, -0.9, -0.995);   // xy = bottom-left, zw = top-right
    switch (gl_VertexID % 4) {
        case 0: gl_Position = vec4(corner.xw, -1.0, 1.0); break;
        case 1: gl_Position = vec4(corner.xy, -1.0, 1.0); break;
        case 2: gl_Position = vec4(corner.zy, -1.0, 1.0); break;
        case 3: gl_Position = vec4(corner.zw, -1.0, 1.0); break;
    }
}
```
`z = -1.0` puts it in front of everything. Sizing the rectangle in *pixels* rather than NDC
fractions is more robust:
```glsl
vec2 markerSize = vec2(39.0, 1.0) * (2.0 / ScreenSize);
vec2[] c = vec2[](vec2(0,1), vec2(0,0), vec2(1,0), vec2(1,1));
gl_Position = vec4(-1.0 + c[gl_VertexID % 4] * markerSize, 0.0, 1.0);
```

### Write the payload in the fragment shader

```glsl
if (dataQuad > 0) {
    vec2 pixel = floor(gl_FragCoord.xy);
    if (pixel.y >= 1.0 || pixel.x >= 38.0) discard;

    // layout:  0-15 projection matrix, 16 fogStart, 17 fogEnd,
    //          18-33 view matrix, 34-36 chunk offset, 37 game time
    if (pixel.x < 16) {
        int i = int(pixel.x);
        fragColor = encodeFloat(ProjMat[i / 4][i % 4]);
    } else if (pixel.x == 16) { fragColor = encodeFloat1024(FogStart);
    } else if (pixel.x == 17) { fragColor = encodeFloat1024(FogEnd);
    } else if (pixel.x < 34) {
        int i = int(pixel.x) - 18;
        fragColor = encodeFloat(ModelViewMat[i / 4][i % 4]);
    } else if (pixel.x < 37) {
        fragColor = encodeFloat1024(mod(ChunkOffset[int(pixel.x) - 34], 16.0));
    } else if (pixel.x == 37) {
        fragColor = encodeFloat(GameTime * 2400);
    }
    return;
}
```
Encoders are in `encodings.md`.

### Read it back in the post **vertex** shader

Decode once per vertex (4 times per frame), not once per fragment. Pass through `flat out`:

```glsl
flat out mat4 invProjMat;

void main() {
    mat4 projection;
    for (int i = 0; i < 16; i++) {
        vec3 c = texelFetch(DiffuseSampler, ivec2(i, 0), 0).rgb;
        projection[i / 4][i % 4] = decodeFloat(c);
    }
    invProjMat = inverse(projection);
    ...
}
```

### Hiding the strip

The strip is one row of visible pixels. Options:
- Put it under the HUD (row 0 is behind the hotbar at most resolutions) and accept it.
- Clamp every later sample away from row 0:
  `offset.y = max(offset.y, 1.5 / InSize.y);`.
- Copy row 1 over row 0 at the end, in a later pass.
- Average the 4-neighbourhood over marker pixels, or take the nearest-depth neighbour.
- Preserve it explicitly in intermediate passes so it survives to the passes that need it:
  ```glsl
  if (int(floor(gl_FragCoord.y)) == 0) {
      fragColor = texelFetch(DiffuseSampler, ivec2(gl_FragCoord.xy), 0);
      return;
  }
  ```

### Precision trap: the projection matrix

`ProjMat[0][0]` and `[1][1]` are `1/tan(fov/2)` and can exceed the ±(2^23 / 40000) range of
the fixed-point encoder at low FOV. Store `atan(value)` and apply `tan()` on decode:
```glsl
// encode
if (c == r && c < 2) value = atan(value);
// decode
projection[0][0] = tan(projection[0][0]);
projection[1][1] = tan(projection[1][1]);
```
Also zero `projection[3][0]` and `[3][1]` after decoding — the TAA jitter lives there and
makes reprojection unstable. The alternative is to store the un-jittered values in extra
slots and patch them back after decoding.

### Camera position without a position uniform

There is no camera-position uniform. `ChunkOffset` / `ModelOffset` mod 16 gives you the
camera's sub-chunk position; the difference between this frame and last frame, wrapped, is
the camera's **motion**, which is all reprojection needs:
```glsl
vec3 offset = mod(position - prevPosition + 8.0, 16.0) - 8.0;
```
This is exact for motion under 8 blocks/frame, which always holds.

---

## 2. Lightmap as a uniform bus

The lightmap is a 16×16 texture produced by `core/lightmap.fsh`, and that shader receives
uniforms the rest of the renderer never sees: `AmbientLightFactor`, `SkyFactor`,
`BlockFactor`, `UseBrightLightmap`, `SkyLightColor`, `NightVisionFactor`, `DarknessScale`,
`DarkenWorldFactor`, `BrightnessFactor`. The **alpha channel** of the lightmap is unused by
the renderer, so you can write arbitrary bytes into it and read them from any shader that
binds `Sampler2`. 256 bytes = 64 floats.

Writer (`core/lightmap.fsh`):
```glsl
float getFloat(int index) {
    switch (index) {
        case 0: return AmbientLightFactor;
        case 1: return SkyFactor;
        case 2: return BlockFactor;
        case 3: return UseBrightLightmap;
        case 4: return SkyLightColor[0];
        ...
    }
    return 0.0;
}

int getAlpha() {
    int coord = int(floor(gl_FragCoord.y) * 16 + floor(gl_FragCoord.x));
    int byte  = coord % 4;
    int index = coord / 4;
    return int((floatBitsToUint(getFloat(index)) >> (byte * 8)) & 0xFFu);
}

void main() {
    /* ... reproduce the vanilla lightmap colour exactly ... */
    fragColor = vec4(color, float(getAlpha()) / 255.0);
}
```

Reader (any shader with `Sampler2`):
```glsl
uint readPackedByte(sampler2D lightMap, ivec2 coord) {
    return uint(round(texelFetch(lightMap, coord, 0).a * 255.0));
}
float decodePackedFloat(sampler2D lightMap, int index) {
    int x = index % 4, y = index / 4;
    uint b0 = readPackedByte(lightMap, ivec2(x * 4 + 0, y));
    uint b1 = readPackedByte(lightMap, ivec2(x * 4 + 1, y));
    uint b2 = readPackedByte(lightMap, ivec2(x * 4 + 2, y));
    uint b3 = readPackedByte(lightMap, ivec2(x * 4 + 3, y));
    return uintBitsToFloat((b3 << 24u) | (b2 << 16u) | (b1 << 8u) | b0);
}
```

**Critical**: every shader that samples the lightmap must switch from
`minecraft_sample_lightmap` (bilinear `texture()`, which would blend your bytes) to
`minecraft_fetch_lightmap` (`texelFetch`, exact):
```glsl
vec4 minecraft_fetch_lightmap(sampler2D lightmap, ivec2 uv) {
    return vec4(texelFetch(lightmap, uv / 16, 0).rgb, 1.0);
}
```
You must override *every* core shader that uses the lightmap, or lighting will differ
between them. In practice that means `entity`, `particle`, `rendertype_entity_decal`,
`rendertype_item_entity_translucent_cull`, `rendertype_leash`, `rendertype_text*`,
`rendertype_translucent_moving_block` and `position_color_lightmap`.

Side effect: you lose smooth lightmap interpolation. Accept it or dither.

---

## 3. `GameTime` abuse — a server→client scalar

`GameTime` is `worldAge % 24000 / 24000.0`. A server that controls the world-age packet
therefore controls a float in every shader. Example, a server-controlled FOV:

```glsl
uniform float GameTime;
float getFov() { return floor(GameTime * 180.0F); }   // 0..180

mat4 changeFov(mat4 projection) {
    float fov = getFov();
    if (projection[2][3] != 0.0 && fov >= 1.0F) {      // skip orthographic (GUI)
        float invTanHF = 1.0 / tan(radians(fov * 0.5));
        float aspectInv = projection[0][0] / projection[1][1];
        projection[0][0] = invTanHF * aspectInv;
        projection[1][1] = invTanHF;
    }
    return projection;
}
```
Server side: send a time-update packet with
`worldAge = (long) Math.floor(((float) fov + 0.5F) / 180.0F * 24000.0F)`; `0` unlocks.

Trade-offs: you sacrifice the day/night cycle and every `GameTime`-driven animation
(clouds, water, portal, enchantment glint). Only worth it for one global scalar. Note that
`changeFov` must be applied in **every** vertex shader that uses `ProjMat`, otherwise
world and entities disagree — which is why a pack doing this ships ~60 `.vsh` files that
differ only in one line.

Also, guard the first-person hand, which uses a different projection:
```glsl
mat4 changeFov(float fogStart, mat4 projection) {
    if (fogStart > 3e38 && projection[2][3] != 0) return projection;  // hand: leave alone
    return changeFov(projection);
}
```

---

## 4. Texture magic pixels — a header inside the atlas

Put a signature and a payload into the texture itself, and read it with `texelFetch`. The
hard part is that a fragment only knows its own UV, so it must be able to *find* the header.
The solution is corner pointer pixels:

Generator (Python, Pillow):
```python
frames.putpixel((0, 0),                  (149, 213, 75, 1))   # magic
frames.putpixel((1, 0),                  (width, height, 75, 1))
frames.putpixel((2, 0),                  (frame_dim, num_frames, 75, 1))
frames.putpixel((3, 0),                  (secs, frac255, 75, 1))
frames.putpixel((width - 1, 0),          (width - 1, 0, 75, 1))          # pointer back to (0,0)
frames.putpixel((0, height - 1),         (0, height - 1, 75, 1))
frames.putpixel((width - 1, height - 1), (width - 1, height - 1, 75, 1))
```
Shader:
```glsl
bool validateProperty2(vec4 d) { return ivec2(round(d.zw * 255.0)) == ivec2(75, 1); }

bool decodeProperties(vec2 uv, out ivec2 dim, out int frameDim, out int nframes,
                      out float time, out ivec2 size, out vec2 origin) {
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    ivec2 coord = ivec2(uv * texSize);
    if (decodeProperties0(coord, dim, frameDim, nframes, time)) { origin = uv; return true; }

    vec4 uvOffset = texelFetch(Sampler0, coord, 0);        // a corner pointer pixel
    if (!validateProperty2(uvOffset)) return false;
    ivec2 pointing = coord - ivec2(round(uvOffset.xy * 255.0));
    origin = vec2(pointing) / texSize;
    return decodeProperties0(pointing, dim, frameDim, nframes, time);
}
```
Because the atlas stitcher may move the sprite anywhere, the header must be *relative*: the
corner pixels store the offset back to the sprite's origin, so the shader recovers `origin`
regardless of where the sprite landed.

Two-level validation (magic word **and** a per-pixel tag `zw == (75,1)`) is essential; a
single magic colour will eventually collide with real art.

The same shape appears elsewhere: mesh-in-texture formats mark their header with a fixed
4-byte magic and store a per-face offset in every face pixel so any fragment can walk back to
it (`mesh-tricks.md`); a video player stores its magic split across four map pixels.

---

## 5. Texture alpha as a per-texel effect tag

The cheapest channel of all: the alpha value of a texel selects a code path. Alpha values
between ~246 and ~254 are visually indistinguishable from opaque, so they are free to
repurpose.

Dispatching several ported effects from one shader:
```glsl
ivec2 texSize = textureSize(Sampler0, 0);
vec4 rgb = texelFetch(Sampler0, ivec2(texCoord0 * vec2(texSize)), 0);
int a = int(rgb.a * 255.0 + 0.5);

if      (a == 254) fragColor = rgb * originColor * ColorModulator;   // pure vertex tint
else if (a == 248) fragColor = alpha248Outline(rgb, alpha248ScreenEdge(texCoord0), gl_FragCoord.xy);
else if (a == 249) fragColor = alpha249Outline(alpha249ScreenEdge(texCoord0));
else if (a == 247) fragColor = alpha247Volume(localUV);
else if (a == 250) fragColor = waterCausticSupersampled(localUV);
else if (a == 246) fragColor = alpha246Portal(localUV);
```
and recovers a **sub-texel UV** for the effect by combining the texel's own RG bytes (a
coarse 0–255 grid) with the fractional part of the interpolated UV:
```glsl
vec2 localUV = texture(Sampler0, texCoord0).rg;
vec2 uvStep  = 1.0 / vec2(textureSize(Sampler0, 0));
vec2 uvFine  = vec2(fract(texCoord0.x / max(uvStep.x, 1e-6)),
                    fract(texCoord0.y / max(uvStep.y, 1e-6)));
localUV = clamp((floor(localUV * 255.0) + uvFine) / 255.0, 0.0, 1.0);
```

A bloom pack uses a band such as 230-250 as **emission strength**:
```glsl
int alpha = int(round(col.a * 255.0));
if (alpha >= 230 && alpha <= 250) {
    fragColor = encodeLogLuv(pow(col.rgb, vec3(2.2)) * float(alpha - 229));
}
```
and reads the *unmipped* alpha with `textureLod(Sampler0, texCoord0, -4)` so mip blending
does not smear the tag across a sprite boundary.

A dynamic-emissive scheme remaps alphas so the tag survives the render type's own alpha use:
```glsl
float remap_alpha(int a) {
    if (a == 252) return 255.0;   // fully emissive, opaque
    if (a == 251) return 120.0;   // partially emissive, translucent (ice)
    if (a == 250) return 0.0;     // hide in 3D handheld
    if (a == 249) return 0.0;     // hide in 2D GUI
    if (a == 248) return 0.0;     // hide in 3D handheld + emissive
    return float(a);
}
```

A voxel-lighting pack packs a light **colour and level** into one texel: one exact RGBA
quadruple marks an opaque block, and an alpha band such as `(200, 216)` marks a light source
whose colour is the RGB and whose level is `alpha - 200`.

Constraints: alpha tags survive only if nothing else quantises them. Avoid alpha < 230 in
cutout render types (the `< 0.1` discard), and remember `ColorModulator` multiplies alpha
in most fragment shaders.

---

## 6. Vertex `Color` as a per-entity parameter

`Color` is 4 bytes the server sets per entity (item-display tint, text colour, particle
colour, glow colour). Two dozen bits, refreshed each tick. Uses:

- **Feature selector.** Gate whole subsystems on exact RGB triples:
  ```glsl
  if (Color.rgb == vec3(0xCA, 0xFE, 0xBA) / 0xFF) { /* skybox */ }
  if (Color.rgb == vec3(0x00, 0xBE, 0xEF) / 0xFF) { /* video player */ }
  isHighlighted = Color.rgb == vec3(0xAB,0xCD,0xEF)/0xFF ? 1 : 0;
  ```
- **Timing.** Pack the animation *birth tick* into `Color.rg` (big-endian 16-bit) and the
  *duration* into `Color.b`:
  ```glsl
  float birthTick = floor(Color.r*255.0+0.5) * 256.0 + floor(Color.g*255.0+0.5);
  float durationTicks = floor(Color.b * 255.0 + 0.5);
  float elapsed = mod(GameTime - birthTick/24000.0 + 1.0, 1.0);
  if (elapsed > 0.5) elapsed = 0.0;                       // wrap guard
  float progress = clamp(elapsed / (durationTicks/24000.0), 0.0, 1.0);
  ```
  That wrap guard matters: `GameTime` is modular, so an element born just before midnight
  would otherwise show a progress of ~1.0 forever.
- **Rotation and frame index.** A per-model behaviour byte can declare how the three colour
  bytes map onto {rotation X, rotation Y, rotation Z, animation tick}:
  ```glsl
  switch ((colorbehavior/64)%4) {
      case 0: rotation.x += Color.r*255; accuracy.r *= 256; break;
      case 1: rotation.y += Color.r*255; accuracy.g *= 256; break;
      case 2: rotation.z += Color.r*255; accuracy.b *= 256; break;
      case 3: tcolor = tcolor * 256 + int(Color.r*255); break;
  }
  ```
- **Screen position.** A radar puts each blip's x, y and rotation in `Color.rgb`.

Beware: `Color` is byte-quantised, `ColorModulator` multiplies it, and several render
types pre-multiply it by the lightmap. Capture the raw value early
(`out vec4 originColor = Color;`) if you need it un-lit.

---

## 7. Coordinate encoding — data in the entity's position

Entity positions are doubles on the server and floats in `Position`; the high bits are free
real estate if you keep the entity out of normal play space.

**Region bits**: shift 10 bits out of the world position.
```glsl
ivec3 extractCoordinateData(inout vec3 worldpos, bool ui) {
    ivec3 coordinateData = ui ? ivec3(0) : (ivec3(worldpos + 512.0) >> 10);
    worldpos = worldpos - vec3(coordinateData << 10);
    return coordinateData;
}
```
The recovered `worldpos` is the real render position; `coordinateData` is 3 small integers
selecting behaviour (`coordinateData.y < 0` → render this entity in the 2-D side world).

**Threshold bands**, the most common HUD scheme: put the entity
at an absurd Y and use the magnitude to select an anchor.
```glsl
#define GAP 1000000
bool is_hud(vec3 p) { return p.y < -1000.0; }

bool make_hud() {
    if (!is_hud(Position)) return false;
    vec3 pos = Position + vec3(0.0, 1.5 * GAP, 0.0);
    pos.x *= -1;
    float offset = 0.0;
    if (Position.y < -2.0 * GAP) {                    // anchored variants
        if (Position.y < -4.0 * GAP) {                // right
            pos.y += 2.0 * GAP;
            offset = 1.0 - (ScreenSize.y / 9 * 16) / ScreenSize.x;
        } else if (Position.y < -3.0 * GAP) {         // centre
            pos.y += 1.0 * GAP;
        } else {                                      // left
            offset = -1.0 + (ScreenSize.y / 9 * 16) / ScreenSize.x;
        }
        pos.y += 1.0 * GAP;
        pos.x *= (ScreenSize.y / 9 * 16) / ScreenSize.x;
    }
    pos.xy /= vec2(1920.0, 1080.0) * vec2(100, 100) / 2.0;   // reference-resolution scale
    pos.x += offset;
    pos.z /= 1000000.0;
    gl_Position = vec4(pos, 1);
    return true;
}
```

**Bitfield in Y**, which scales to hundreds of elements: the text element's Y encodes an
element id.
```glsl
#define HEIGHT_BIT 13
#define MAX_BIT 10
#define ADD_OFFSET 4095
#define DEFAULT_OFFSET 10

vec2 ui = ceil(2.0 / vec2(ProjMat[0][0], -ProjMat[1][1]));   // GUI size in "gui pixels"
if (pos.y >= ui.y && ProjMat[3].x == -1) {                   // HUD-space text only
    int bit = int(pos.y) >> HEIGHT_BIT;
    if (((bit >> MAX_BIT) & 1) == 1) {
        int id = bit - (1 << MAX_BIT);
        pos.x -= 0.5 * ui.x;
        pos.y -= (bit << HEIGHT_BIT) + ADD_OFFSET + DEFAULT_OFFSET;
        switch (id) { /* per-element layout, colour, animation flags */ }
    }
}
```
Float precision is the limit: above ~2^24 the mantissa loses integer resolution, so keep
`|Position|` under ~8·10^6 and never rely on the low bits of a large coordinate.

---

## 8. Map items — 16 KB of server-controlled pixels

A filled map is a 128×128 image of palette indices that the server can rewrite every tick
and that the client renders through `rendertype_text` (map contents are drawn as a textured
quad in the text render type). It is by far the highest-bandwidth server→client channel in
vanilla.

The vanilla palette has 256 entries of which the first 4 are transparent. Indices 4–255 are
usable, i.e. **7 clean bits per pixel** if you use one shade band, or a full byte if you
accept the map's own brightness quantisation.

Reverse-lookup in the shader:
```glsl
const ivec3 lookup[] = ivec3[128]( ivec3(89,125,39), ivec3(109,153,48), ... );

int decode7u(vec3 color) {
    ivec3 d = ivec3(color * 255.0);
    for (int i = 0; i < 128; i++) if (lookup[i] == d) return i;
    return 0;
}
```

**Full-RGB maps** — pack 3 bytes into a 2×2 block of 7-bit map colours:
```glsl
ivec2 coord = (ivec2(floor(texCoord0 * vec2(texSize))) / 2) * 2;
int b1 = decode7u(texelFetch(Sampler0, coord,               0).rgb);
int b2 = decode7u(texelFetch(Sampler0, coord + ivec2(1,0),  0).rgb);
int b3 = decode7u(texelFetch(Sampler0, coord + ivec2(0,1),  0).rgb);
int b4 = decode7u(texelFetch(Sampler0, coord + ivec2(1,1),  0).rgb);
b1 |= (b4 & 1) << 7;  b2 |= (b4 & 2) << 6;  b3 |= (b4 & 4) << 5;   // MSBs from the 4th cell
color = vec4(vec3(b3, b2, b1) / 255.0, 1.0);
```
Halves the resolution to 64x64 but gives 24-bit colour. Write the matching encoder
server-side and keep it next to the shader.

**Bitstream** — treat the map as a 7-bits-per-byte stream with an offset so no byte lands in
the transparent range:
```java
private static final int PAYLOAD_BITS = 7;
private static final int OFFSET = 4;          // skip transparent palette entries 0-3
// writeBits() accumulates 7 bits then emits (bits + 4)
```
Its payload is `MAGIC(32) | 47 palettes × 16 colours × 32 bits | 4096 tiles × (6-bit palette id + 4 × 4-bit index) | scale + position floats`,
i.e. a palettised 128×128 image compressed to fit 16384 bytes. Guard the whole thing with
a magic word (`0x53554E53`) so a stale or foreign map is rejected.

**Config bits** — read booleans and fixed-point values straight from map pixels:
```glsl
int decodeUnsigned(int offsetX, int offsetY) {
    float texel = 1.0 / 128.0;
    int power = 1, value = 0;
    for (int i = 0; i < 8; i++) {
        bool set = sign(length(texture(Sampler0, vec2(float(offsetX + i) * texel, float(offsetY) * texel)).xyz)) > 0;
        if (set) value += power;
        power *= 2;
    }
    return value;
}
float decodeFixedPoint(int x, int y) { return float(decodeUnsigned(x, y)) / 255.0; }
```
Note this uses "pixel is non-black" as the bit, which survives map palette quantisation.

Always detect the carrier by `textureSize(Sampler0, 0) == ivec2(128, 128)` before decoding.

---

## 9. Render-target persistence — cross-frame state

Post render targets are **not cleared between frames**. Declare a private target, `blit` into
it at the end of the pipeline, and read it at the start of the next frame. This is the only
memory a core-shader pack has.

```json
"targets": [ "prevAo", "prevDepth", "prevNormals" ],
"passes": [
    { "name": "temporal", "intarget": "ao", "outtarget": "temporal",
      "auxtargets": [ { "name": "PreviousDiffuseSampler", "id": "prevAo" }, ... ] },
    { "name": "blit", "intarget": "temporal", "outtarget": "prevAo" },
    { "name": "blit", "intarget": "normals",  "outtarget": "prevNormals" }
]
```

**Tiny state targets.** A float accumulator fits in 1x1 and 2x1 targets:
```json
{ "name": "prevTime",  "width": 1, "height": 1 },
{ "name": "prevAccum", "width": 2, "height": 1 }
```
```glsl
float prevTime = decodeFloat(DiffuseSampler);      // 32-bit float in RGBA8
float deltaTime = Time;
if (deltaTime < prevTime) deltaTime += 1.0;        // Time wraps 0..1
deltaTime -= prevTime;

float accumTime = decodeFloat(PrevAccumSampler) + deltaTime;
if (texelFetch(PrevMainSampler, ivec2(0,0), 0).rgb != texelFetch(MainSampler, ivec2(0,0), 0).rgb)
    accumTime = 0.0;                               // scene changed → reset
if (texelFetch(PrevAccumSampler, ivec2(1,0), 0) != vec4(1, 0, 1, 127.0/255.0))
    accumTime = 0.0;                               // canary pixel gone → target was reset
```
The **canary pixel** is essential. Targets are reallocated on resize, resource reload, and
world change; without a canary you will decode uninitialised memory as state. Write a fixed
improbable colour to one pixel every frame and validate it before trusting anything else.

**Frame counters** for jittered sampling: increment a byte modulo N in one pixel.
```glsl
if (ivec2(gl_FragCoord.xy) == ivec2(37, 0)) {
    int x = int(texelFetch(PreviousDiffuseSampler, ivec2(37,0), 0).r * 255.0 + 1.0) % 4;
    fragColor = vec4(float(x) / 255.0, 0.0, 0.0, 1.0);
    return;
}
```

**Huge history targets.** Declaring a fixed-size target keeps a voxel grid from shrinking
when the window does. Fixed-size targets decouple your
data structure from the window; use them whenever the window size would otherwise change
your addressing.

---

## 10. Post-effect toggles (MC 26.2+) — a real server→shader bus

From 26.x a server can enable and disable individual named post effects from a resource
pack at runtime. That makes a general data bus possible: **one post effect per bit**.

`assets/cs/post_effect/b17.json` — writes white to pixel (25, 0):
```json
{
  "targets": {},
  "passes": [{
    "vertex_shader": "cs:post/region",
    "fragment_shader": "cs:post/solid",
    "output": "minecraft:main",
    "uniforms": {
      "RegionConfig": [ { "name": "Rect", "type": "vec4", "value": [25.0, 0.0, 1.0, 1.0] } ],
      "SolidConfig": [ { "name": "Color", "type": "vec4", "value": [1.0, 1.0, 1.0, 1.0] } ]
    }
  }]
}
```

Three roles are needed:
1. **`cs:header`, always applied first.** Blanks the payload pixels (because "no writer ran"
   is indistinguishable from "bit is 0" if the world is still under them) and stamps an
   8-pixel magic pattern so the decoder can tell a live signal from screen garbage.
   ```glsl
   const int MAGIC = 0xB4;
   int x = int(localPos.x);
   float v = (x < 8) ? float((MAGIC >> x) & 1) : 0.0;
   fragColor = vec4(v, v, v, 1.0);
   ```
2. **`cs:bN`, one per bit**, each rasterising ~18 fragments.
3. **`cs:hud`, always applied last.** Grabs the strip into a private target (you cannot read
   and write `minecraft:main` in one pass — that is a feedback loop), patches the strip out
   of the visible image, and renders the decoded UI.
   ```json
   "targets": { "data": { "width": 80, "height": 2 } },
   "passes": [
     { "vertex_shader": "minecraft:core/screenquad", "fragment_shader": "cs:post/grab",
       "inputs": [{"sampler_name": "In", "target": "minecraft:main"}], "output": "data" },
     { "vertex_shader": "cs:post/region", "fragment_shader": "cs:post/patch",
       "inputs": [{"sampler_name": "In", "target": "data"}], "output": "minecraft:main",
       "uniforms": {"RegionConfig": [{"name":"Rect","type":"vec4","value":[0,0,80,1]}]} },
     { "vertex_shader": "cs:post/hud", "fragment_shader": "cs:post/hud",
       "inputs": [{"sampler_name": "In", "target": "data"}], "output": "minecraft:main" }
   ]
   ```
   `grab` copies rows 0 (payload) and 1 (untouched world) of the bottom-left 80×2 corner;
   `patch` overwrites row 0 with row 1 so the strip is invisible.

Decoder:
```glsl
bool bitAt(int i) { return texelFetch(InSampler, ivec2(i, 0), 0).r > 0.5; }
int chanValue(int color, int ch) {
    int base = 8 + color * 24 + ch * 8, v = 0;
    for (int k = 0; k < 8; k++) if (bitAt(base + k)) v |= (1 << k);
    return v;
}
```
Latency is one packet; bandwidth is one bit per declared effect, and adding more is just more
generated JSON. Cost is trivial because each writer touches a handful of fragments.

---

## 11. Implicit client state — free signals in existing uniforms

No plumbing required; just read them.

| Signal | Test | Generations |
|---|---|---|
| GUI / orthographic projection | `ProjMat[2][3] == 0.0` | all |
| GUI item render (alt) | `ProjMat[3][2] == -2.0` | G1-G2 |
| HUD text space | `ProjMat[3].x == -1` | G1–G4 |
| First-person hand | `FogStart * 0.000001 > 1`, i.e. `FogStart > 1e6`; often written `FogStart > 3e38` | G1–G2 only |
| A forced custom pass | a fog sentinel the server sets, e.g. `FogEnd` in a narrow band with `FogColor.rgb == vec3(0)` | G1–G2 |
| Terrain vs. inventory block | `ChunkOffset == vec3(0.0)` → not terrain | G1–G2 |
| GUI size in "gui pixels" | `ceil(2.0 / vec2(ProjMat[0][0], -ProjMat[1][1]))` | all |
| Which texture is bound | `textureSize(Sampler0, 0)` | all |
| Camera yaw | `vec3 local = transpose(mat3(ModelViewMat)) * vec3(1,0,0); yaw = -atan(local.x, local.z);` | all |
| Screen size from a clip position | `ivec2(gl_FragCoord.xy / (glPos.xy / glPos.w * 0.5 + 0.5))` | all (when `ScreenSize` is unavailable) |

Compose several of these into a projection dispatcher: one function returning a different
projection depending on which pass the game is currently in.
```glsl
bool isShadowProj()  { return FogEnd > 4.0 && FogEnd < 32.0 && FogColor.rgb == vec3(0.0)
                           && ProjMat[2][3] != 0.0 && FogStart < 1000000.0; }
bool isPortalProj()  { return (abs(FogEnd - FogStart * 10.0) < 0.5
                           || (abs(FogEnd - 96.0) < 0.5 && abs(FogStart - 86.4) > 0.5))
                           && abs(ProjMat[3][3]) < 0.5; }
mat4 getProjMat() {
    if (isPortalProj()) return getPortalProjMat();
    if (isShadowProj()) return getShadowProjMat();
    return ProjMat;
}
```
The datapack sets fog distances to those exact sentinel values before the pass it wants,
which turns a vanilla fog setting into a pass selector. This is how a pack renders a
shadow map or an off-camera view with no client mod — see `architectures.md § 6`.

**G3+ replacements** for the removed `FogStart` tricks: use `ProjMat[2][3] == 0.0` for GUI,
distinguish the hand by its distinctive `ModelViewMat` translation or by a marker texture on
the item, and detect the shadow pass by `FogRenderDistanceEnd` sentinels instead of
`FogEnd`. Verify empirically — the fog uniforms carry different values than in G1/G2.

---

## 12. Item-entity / translucent targets as scratch RAM (Fabulous only)

In the Fabulous pipeline, `itemEntity`, `particles`, `clouds` and `weather` are separate
full-screen targets composited by `transparency`. Anything drawn in those render types
lands in a target the post pipeline can read *separately from the world*. That makes them
ideal data planes.

A control plane on `itemEntity`: a datapack places item displays whose textures are single
specific colours, the core shader routes them to known screen pixels, and post passes read
that target to obtain, for example:
- 22 rows of packed floats forming the inverse view-projection matrix,
- 7 rows per portal giving yaw, pitch and position,
- a 1-pixel "which frame is this" control colour that gates the frame-saving passes:
  ```glsl
  vec3 col = round(texelFetch(ParticlesSampler, ivec2(PixelX, Offset), 0).rgb * 255.0);
  save = int(col == Color);      // Color comes from the pass's uniform override
  ```
  ```glsl
  void main() { fragColor = (save == 1) ? texture(DiffuseSampler, texCoord)
                                        : texture(SavedSampler, texCoord); }
  ```
  This is a conditional blit: the pipeline is static, but which frame actually gets stored
  is decided by a pixel. This is what captures an off-camera frame for a portal or shadow map.

Cost: you must re-supply the target to the `transparency` pass, and anything genuinely
rendered in those layers will collide with your data. Packs doing this often re-point unused
samplers to the same target to keep the layout consistent.
