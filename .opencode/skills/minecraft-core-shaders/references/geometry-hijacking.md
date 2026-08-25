# Geometry hijacking — drawing what the game wasn't going to draw

You cannot issue a draw call. Every pixel you want must ride on geometry the game already
submits. The pattern is always the same:

1. Pick a **carrier**: a draw the game reliably makes (a block face, an item display, a text
   glyph, the first-person hand, an entity quad).
2. **Recognise** it in the vertex shader (texture magic pixel, vertex colour, position band,
   projection shape).
3. **Overwrite `gl_Position`** with clip-space coordinates of your choosing.
4. Recompute varyings so the fragment shader knows it is in your mode.

---

## 1. The quad identity toolkit

Minecraft submits quads as 4 consecutive vertices, so:

```glsl
int corner = gl_VertexID % 4;   // 0,1,2,3 around the quad
int quadId = gl_VertexID / 4;   // which quad of the model
```
Vanilla's corner order is **top-left, bottom-left, bottom-right, top-right** for most
render types. The conventional corner tables:
```glsl
const vec2[] faceCoords = vec2[](vec2(-1,+1), vec2(-1,-1), vec2(+1,-1), vec2(+1,+1));
const vec2[] uvs        = vec2[](vec2( 1, 0), vec2( 0, 0), vec2( 0, 1), vec2( 1, 1));
```
Verify empirically for the render type you are hijacking — a mirrored quad is the classic
symptom of assuming the wrong order.

`gl_PrimitiveID % 2` tells you which triangle of the quad you are in, which
you need to recover a quad's tangent and bitangent from the three vertices a triangle
actually sees.

---

## 2. Screen-space quad from a world quad

The core move: replace world-space clip coordinates with fixed NDC.

```glsl
if (isMyThing) {
    vec2 bl = vec2(-1.0), tr = vec2(1.0, 0.1);
    switch (gl_VertexID % 4) {
        case 0: gl_Position = vec4(bl.x, tr.y, -1, 1); break;
        case 1: gl_Position = vec4(bl.x, bl.y, -1, 1); break;
        case 2: gl_Position = vec4(tr.x, bl.y, -1, 1); break;
        case 3: gl_Position = vec4(tr.x, tr.y, -1, 1); break;
    }
}
```
Notes:
- `z = -1.0, w = 1.0` → nearest depth, drawn over everything. Use `z` between −1 and 0 to
  layer several hijacked quads: `-1.0 + float(layerIndex) * 0.0005`.
- To *hide* a quad instead, send it far outside the frustum:
  `gl_Position = vec4(10.0, 10.0, 10.0, 1.0);` — cheaper than `discard` in the fragment
  shader because it is culled before rasterisation.
- Aspect-ratio-correct rectangles need `ScreenSize`:
  ```glsl
  float ratio = ScreenSize.x / ScreenSize.y;
  float vratio = ScreenSize.y / ScreenSize.x;
  if (vratio < ratio) ratio = 1.0; else vratio = 1.0;
  // now (0.04*vratio, 0.04*ratio) is a square margin in NDC
  ```

---

## 3. HUD anchoring by position band

The standard scheme: a text display or item display is placed at an absurd Y, and the shader
maps it to a screen anchor.
Full code in `data-channels.md § 7`. Design notes:

- A **reference resolution** (1920×1080) plus a uniform scale keeps the layout stable:
  ```glsl
  float scale = ScreenSize.y / refRes.y;          // uniform, height-driven
  vec2 ndcOffset = (pos * scale) / ScreenSize * 2.0;
  ```
- Integer scaling avoids shimmer on pixel art:
  ```glsl
  float s = clamp(floor(OutSize.y / 540.0), 1.0, 4.0);
  ```
- Anchors: encode which corner in the position band (left / centre / right) and offset by
  `±(1 - (ScreenSize.y/9*16)/ScreenSize.x)` to sit at the edge of a 16:9 safe area.
- Flatten lighting and fog for HUD elements or they will tint with the world:
  ```glsl
  vertexColor = Color;
  sphericalVertexDistance = 0.0;
  cylindricalVertexDistance = 0.0;
  ```
- Compress `z` so HUD elements never z-fight with the world: `pos.z /= 1000000.0;`

---

## 4. Off-screen waypoints with edge clamping

An item-display entity at the waypoint's world position becomes a screen marker that clamps
to the screen edge with a directional arrow when off-screen.

```glsl
vec4 build_waypoint(inout vec2 texCoord, ivec2 texSize, vec2 size, vec3 position,
                    vec2 faceCoords, mat4 projMat, mat4 modelViewMat) {
    vec2 screenBounds = vec2(1.0) - size;

    ivec2 sprite = ivec2(0);
    vec4 center  = modelViewMat * vec4(position - vec3(faceCoords, 0.0), 1.0);
    vec4 clipPos = projMat * center;
    vec2 screenPos = clipPos.xy / clipPos.w;

    if (clipPos.z < 0.0 || abs(screenPos.x) > screenBounds.x || abs(screenPos.y) > screenBounds.y) {
        if (center.z >= 0.0) screenPos *= -1.0;          // behind camera → mirror
        vec2 t = screenBounds / abs(screenPos);
        screenPos *= min(t.x, t.y);                       // clamp to the rectangle border
        sprite = ivec2(ivec2(2, -2) * screenPos);          // choose an arrow sprite by direction
    }

    vec2 uv = faceCoords + 0.5;
    uv.y = 1.0 - uv.y;
    uv = 33.0 + 30.0 * uv + 32.0 * sprite;                 // index into a 3x3 sprite sheet
    texCoord += uv / vec2(texSize);

    return vec4(screenPos + size * faceCoords, -1.0, 1.0);
}
```
Detection is a magic texel. A useful variant is `rgb == 0 && alpha in [17,20]`, where the
alpha doubles as a layer index so several waypoints sort correctly against each other.

Distance-based scaling keeps far markers readable:
```glsl
vec2 scaleFactor = clamp(max(100.0 - length(Position), 0.0) / 100.0, 0.75, 1.0) * vec2(0.15);
gl_Position = build_waypoint(texCoord0, texSize,
                             scaleFactor / vec2(ScreenSize.x / ScreenSize.y, 1.0), ...);
```
The `/ vec2(aspect, 1.0)` keeps the marker square regardless of window shape.

---

## 5. First-person view models (FPS hands, weapons)

Three approaches in increasing order of control:

### 5a. Hijack the hand's own geometry
Repurpose one face of the first-person hand as a full-screen quad:
```glsl
renderInfo = 0.0;
if (FogStart > 3e38 && (mat3(ModelViewMat) * Normal).z < -0.5) {
    gl_Position = corners[gl_VertexID % 4];       // fullscreen NDC quad
    renderInfo = 1.0;
    texCoord0 = gl_Position.xy * 0.5 + 0.5;
    texCoord0.y = 1.0 - texCoord0.y;
}
```
`FogStart > 3e38` identifies the hand pass (G1/G2 only); the normal test picks the one face
that faces the camera. Zero cost, but you only get whatever faces the model has.

### 5b. Item model that adds hands
The item's model includes extra cubes UV-mapped into the player skin, and the shader
redirects `Sampler0` reads for those cubes to the skin texture.
Requires `rendertype_entity_translucent` to be pointed at a custom shader via `defines`.

### 5c. Billboard in a private position band
Put the weapon model far below the world; the shader maps it into a fixed-FOV screen-space
billboard so the weapon never clips into walls and never changes with the player's actual FOV.
```glsl
#define FOV 60.0
const float invTanHF = 1.0 / tan(radians(FOV / 2.0));

mat4 billboardChangeFov(mat4 projection) {
    if (projection[2][3] != 0.0) {
        float aspectInv = projection[0][0] / projection[1][1];
        projection[0][0] = invTanHF * aspectInv;
        projection[1][1] = invTanHF;
    }
    return projection;
}

vec2 changePos(vec2 pos) {                       // reference-resolution pixel -> NDC
    const vec2 refRes = vec2(1920.0, 1080.0);
    float scale = ScreenSize.y / refRes.y;
    vec2 ndcOffset = (pos * scale) / ScreenSize * 2.0;
    ndcOffset.y -= 1.0;
    return ndcOffset;
}

bool make_firstperson() {
    vec3 pos = Position;
    if (pos.y < -1000.0) {
        pos.y += 35000.0;
        mat4 projMat = billboardChangeFov(ProjMat);
        projMat[3][0] = 0.0; projMat[3][1] = 0.0;      // kill TAA jitter
        vec4 fixPos = vec4(pos, 1.0);
        fixPos.xz = -fixPos.xz;
        fixPos.xy = changePos(fixPos.xy);
        vec4 samplePos = projMat * fixPos;
        gl_Position = vec4(fixPos.xy, Position.z, samplePos.w);
        gl_Position.z = -0.1 + gl_Position.z * 0.0001;  // compress depth into a thin slab
        gl_Position.w *= 0.001;
        isBillboard = 1;
        return true;
    }
    return false;
}
```
A simpler variant:
```glsl
vec3 pos = Position / 1024.0;
if (pos.y < -1024.0) {
    mat4 projMat = changeFov(ProjMat);
    pos.y += 2048.0;
    pos += vec3(-0.1, -1.65, -1.4);      // eye offset
    gl_Position = projMat * vec4(pos, 1.0);
    gl_Position.z *= 0.001;
    vertexDistance = 0.0;
    return;
}
```
The `z *= 0.001` (or a thin remapped slab) is what keeps the view model in front of the
world without disabling depth entirely, so its own parts still sort correctly.

---

## 6. Third-person / mirrored rendering

A magic texel marks geometry that should only exist in the world, never in the GUI, and
pushes it to the far plane so it renders behind everything:
```glsl
bool is_thirdperson(sampler2D tex, ivec2 pixel) {
    return ivec4(texelFetch(tex, pixel, 0) * 255.5) == ivec4(255, 0, 0, 1);
}
bool make_thirdperson() {
    ivec2 pixel = ivec2(UV0 * textureSize(Sampler0, 0));
    if (is_thirdperson(Sampler0, pixel)) {
        if (isgui(ProjMat)) { gl_Position = vec4(0.0); return true; }   // degenerate → invisible
        gl_Position.z = -1.0;
        gl_Position.w *= 2.0;
        vertexColor = vec4(1.0);
        isThirdPerson = 1;
        return true;
    }
    return false;
}
```
`gl_Position = vec4(0.0)` makes the quad degenerate (w = 0), which is the cheapest possible
"never draw this" — useful for suppressing an entity in one pass only.

---

## 7. Skybox — overriding the far plane

Rebuild the projection with a huge far plane so an entity can be drawn beyond the normal
render distance and behave as a skybox.
```glsl
if (Color.rgb == vec3(0xCA, 0xFE, 0xBA) / 0xFF) {
    mat4 projMat = ProjMat;
    float zFar = 10000.0, zNear = 0.5;
    projMat[2][2] = -((zFar + zNear) / (zFar - zNear));
    projMat[2][3] = -((2.0 * zFar * zNear) / (zFar - zNear));
    gl_Position = projMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = 0.0;    // no fog
    vertexColor = vec4(1.0); // no lighting
    return true;
}
```

---

## 8. Orthographic side-worlds

Replace the projection **and** the view matrix so a slice of the world renders as a flat
side-scroller.
```glsl
void createOrthographicMatrices(inout mat4 projMat, float orthographicSize) {
    float aspect = projMat[1][1] / projMat[0][0];
    projMat = mat4(
        2.0 / (aspect * orthographicSize), 0, 0, 0,
        0, 2.0 / orthographicSize, 0, 0,
        0, 0, -1.0 / 500.0, 0,
        0, 0, -1.0, 1.0
    );
}
void removeCameraRotation(inout mat4 viewMat) { viewMat = mat4(1.0); }

bool make_2d_world(mat4 projMat, mat4 viewMat, vec3 worldPos, int isGUI,
                   ivec3 coordinateData, inout int is2D) {
    if (coordinateData.y < 0) {
        is2D = 1;
        createOrthographicMatrices(projMat, 16.0);
        removeCameraRotation(viewMat);
        vertexColor = lightColor = faceLightColor = vec4(1.0);
        gl_Position = projMat * viewMat * vec4(worldPos, 1.0);
        gl_Position.z += 0.5;
        return true;
    }
    return false;
}
```
Gate is the coordinate-region encoding from `data-channels.md § 7`.

---

## 9. Voxelization — one block, one pixel

The audacious idea: send **every block in range** to a distinct screen pixel, so the
framebuffer becomes a spatial data structure the post pipeline can index.

### 2-D (minimap): world XZ → pixel XY, height in depth

```glsl
#define X_RESOLUTION 512
#define Y_RESOLUTION 512

if (minimapQuad > 0.0) {
    ivec3 blockPos = ivec3(floor(Position + floor(ChunkOffset)));
    int cX = X_RESOLUTION / 2, cY = Y_RESOLUTION / 2;
    if (blockPos.x < -cX || blockPos.x >= cX || blockPos.z < -cY || blockPos.z >= cY) {
        gl_Position = vec4(10.0); return;                  // out of range
    }

    vec2 screenSize = vec2(X_RESOLUTION * 2, Y_RESOLUTION + 1);
    ivec2 screenPos = blockPos.xz + ivec2(cX, cY + 1);
    screenPos.x = screenPos.x * 2 + screenPos.y % 2;        // checkerboard: avoids
                                                            // adjacent-pixel bleeding
    vec2 bl = (screenPos - 0.0) / screenSize * 2.0 - 1.0;
    vec2 tr = (screenPos + 1.0) / screenSize * 2.0 - 1.0;
    // ... emit the 1-pixel quad, with
    //     z = -float(blockPos.y + 386) / 1024
    // so the depth test keeps the HIGHEST block automatically
    voxelCoord = vec2(screenPos) / screenSize;
}
```
The Y coordinate rides in `gl_Position.z`, so the hardware depth test performs the
"topmost visible block" reduction for free. The fragment shader then writes exactly one
pixel and discards the rest of the quad:
```glsl
ivec2 screenSize = ivec2(round(gl_FragCoord.xy / (glPos.xy / glPos.w * 0.5 + 0.5)));
ivec2 coord = ivec2(round(voxelCoord * screenSize));
if (ivec2(gl_FragCoord.xy) == coord) fragColor = vec4(minimapData, 249.0 / 255.0);
else discard;
```
`249/255` in alpha is the "this is a voxel pixel" tag used later by `hide_voxels`.

### 3-D: a linearised voxel grid in the framebuffer

```glsl
ivec2 getVoxelizationRadius(ivec2 screenSize) {
    int pixels = (screenSize.x >> 1) * (screenSize.y - 1);
    int voxelizeDist = int(pow(float(pixels), 1.0 / 3.0));
    int voxelizeExt  = voxelizeDist / 2;
    return ivec2(voxelizeExt, voxelizeExt * 2);
}

ivec2 voxelToPixel(ivec3 blockPos, ivec2 screenSize, ivec2 extDist) {
    screenSize.x >>= 1;
    blockPos += extDist.x;
    int linearIndex = blockPos.y * extDist.y * extDist.y + blockPos.z * extDist.y + blockPos.x;
    ivec2 p = ivec2(linearIndex % screenSize.x, linearIndex / screenSize.x);
    p.x = p.x * 2 + (p.y & 1);        // checkerboard again
    p.y += 1;                         // row 0 reserved for the data marker
    return p;
}

ivec3 pixelToVoxel(ivec2 p, ivec2 screenSize, ivec2 extDist) {
    screenSize.x >>= 1; p.x >>= 1; p.y -= 1;
    int i = p.y * screenSize.x + p.x;
    return ivec3(i % extDist.y, (i / extDist.y) % extDist.y, (i / extDist.y) / extDist.y) - extDist.x;
}

vec4 placeVoxel(ivec3 blockPos, ivec2 screenSize, int vertexId) {
    ivec2 ext = getVoxelizationRadius(screenSize);
    if (any(greaterThanEqual(abs(blockPos), ivec3(ext.x)))) return vec4(-10.0, -10.0, -10.0, 1.0);
    ivec2 p = voxelToPixel(blockPos, screenSize, ext);
    vec2 lo = vec2(p) / vec2(screenSize);
    vec2 hi = lo + 1.0 / vec2(screenSize);
    vec2 v;
    switch (vertexId % 4) {
        case 0: v = vec2(lo.x, hi.y); break;
        case 1: v = vec2(lo.x, lo.y); break;
        case 2: v = vec2(hi.x, lo.y); break;
        case 3: v = vec2(hi.x, hi.y); break;
    }
    return vec4(v * 2.0 - 1.0, -1.0, 1.0);
}
```
At 1920×1080 this yields a cube of about ⌊(960·1079)^(1/3)⌋ ≈ 100 blocks on a side, halved
to ±50 around the camera. The **checkerboard** (`p.x = p.x*2 + (p.y & 1)`) exists because
neighbouring voxels must not be neighbouring pixels — otherwise the later blur/flood-fill
passes would leak between unrelated voxels.

Because the voxel grid lives in a window-sized target, copy it into a fixed-size history
target between frames so resizing the window does not destroy the data.

---

## 10. Marker models — how to guarantee a draw happens

| Carrier | Trigger | Guarantees | Used by |
|---|---|---|---|
| Zero-volume element on `block/cube` | every solid block on screen | always present while any block is visible | data markers, voxelization |
| Item model override on a common item | `custom_model_data` | server-controlled, single instance | camera controllers, config carriers |
| `builtin/entity` player head with `scale: [0,0,0]` | rendering a skull in a GUI | GUI-only, gives you the skin in `Sampler0` | GUI avatars, model raytracing |
| `minecraft:special` head item model (1.21.4+) | any item slot | modern replacement for the above | GUI avatars |
| Bossbar / scoreboard text glyph | HUD text | free, no entity needed | HUD frameworks |
| Item display entity | server places it | full control of position + tint | waypoints, control planes, view models |
| Text display entity | server places it | high glyph throughput | HUDs, world labels |

Zero-volume marker element (goes in **every** block model you care about):
```json
{ "from": [8,8,8], "to": [8,8,8],
  "faces": { "up": { "uv": [8,8,8,8], "texture": "#marker", "cullface": "up" } } }
```
Use all six faces so the marker survives any cullface state:
```json
{ "from": [8,8,8], "to": [8,8,8],
  "faces": {
    "down":  {"uv":[8,8,8,8],"texture":"#voxel","cullface":"down"},
    "up":    {"uv":[8,8,8,8],"texture":"#voxel","cullface":"up"},
    "north": {"uv":[8,8,8,8],"texture":"#voxel","cullface":"north"},
    "south": {"uv":[8,8,8,8],"texture":"#voxel","cullface":"south"},
    "west":  {"uv":[8,8,8,8],"texture":"#voxel","cullface":"west"},
    "east":  {"uv":[8,8,8,8],"texture":"#voxel","cullface":"east"}
  } }
```
Costs: you must ship an overridden model for every block that participates — 1900+ of them,
each just
`{"parent":"minecraft:block/cube_all","textures":{"all":"...","minimap":"block/minimap_15"}}`
where the referenced texture encodes that block's per-block data value. Generate these with a
script; never hand-write them.

---

## 11. GUI detection and GUI-only rendering

```glsl
// orthographic → GUI, in every generation
bool isGui = ProjMat[2][3] == 0.0;

// gui_player_models_base combines it with an axis-aligned normal to find the head cube
renderModel = int(ProjMat[2][3] == 0.0
              && max(abs(Normal.x), max(abs(Normal.y), abs(Normal.z))) == 1.0);
```
To turn a GUI item into a full-cell canvas, inflate its quad and rewrite its UVs:
```glsl
const vec2 uvs[] = vec2[](vec2(1,0), vec2(0,0), vec2(0,1), vec2(1,1));
if (renderModel > 0) {
    const float inflate = 27.0;
    texCoord0 = uvs[gl_VertexID % 4];
    position.xy += (texCoord0 * 2.0 - 1.0) * inflate;
}
```
then in the fragment shader use only one of the cube's quads and discard the rest:
```glsl
if (renderModel > 0) {
    if (quadId != 3) discard;      // keep exactly one face; the others would overdraw
    ...
}
```
Scale asymmetrically so the canvas grows the right way for each facing:
```glsl
const float scale = 37.0;
if (renderAvatar > 0)
    pos.xy += (uvs[gl_VertexID % 4] * scale - vec2(scale * 0.5, 0.0)) * vec2(-Normal.z, 1.0);
```

The item model must collapse to nothing so the vanilla render contributes no pixels:
```json
{ "parent": "builtin/entity", "gui_light": "front",
  "display": { "gui": { "translation": [0, 15, 0], "scale": [0, 0, 0] } } }
```

---

## 12. Depth control cheat sheet

| Goal | Technique |
|---|---|
| Draw over everything | `gl_Position.z = -1.0` (with `w = 1.0`) |
| Draw behind everything | `gl_Position.z = 1.0`, or increase `w` |
| Layer N hijacked quads | `z = -1.0 + float(layer) * 0.0005` |
| Never draw | `gl_Position = vec4(10.0)` (out of frustum) or `vec4(0.0)` (degenerate, w = 0) |
| Keep world sorting but compress range | `gl_Position.z *= 0.001` |
| Store data in depth | write NDC z yourself; the depth test becomes a min/max reduction |
| Force a fragment's depth | `gl_FragDepth = ...` (costs early-z for the whole shader) |

A voxelizing pack uses the "store data in depth" trick twice: block height in the voxel pass,
and `gl_FragDepth = 0.0` on the data-marker quad so it always wins.
