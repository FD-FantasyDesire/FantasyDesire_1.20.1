# Mesh and model tricks

The vanilla model format gives you axis-aligned boxes with at most one rotation each. These
are the techniques that get past that — arbitrary meshes, raytraced geometry, and the
model-side plumbing every shader trick depends on.

---

## 1. Item model plumbing, per version

Version-critical. Getting this wrong means the shader never runs.

### Before 1.21.4 (pack_format ≤ 45) — `overrides` + `custom_model_data` predicate

`assets/minecraft/models/item/player_head.json`
```json
{
  "parent": "minecraft:item/template_skull",
  "overrides": [
    { "predicate": { "custom_model_data": 1 }, "model": "item/player_model" }
  ]
}
```
`assets/minecraft/models/item/player_model.json`
```json
{
  "parent": "builtin/entity",
  "gui_light": "side",
  "display": { "gui": { "translation": [0, 0, 0], "scale": [0, 0, 0] } }
}
```
Item side: give the item `CustomModelData: 1`.

### 1.21.4+ (pack_format ≥ 46) — `assets/<ns>/items/<id>.json` item definitions

The `overrides` mechanism is gone. Item appearance is a tree of model providers.

Simple:
```json
{ "model": { "type": "minecraft:model", "model": "minecraft:item/avatar" } }
```

Selecting on `custom_model_data`:
```json
{
  "model": {
    "type": "minecraft:select",
    "property": "minecraft:custom_model_data",
    "index": 0,
    "cases": [ { "when": "waypoint", "model": { "type": "minecraft:model", "model": "item/waypoint" } } ],
    "fallback": { "type": "minecraft:model", "model": "item/default" }
  }
}
```

A player head rendered through a custom base model:
```json
{
  "model": {
    "type": "minecraft:special",
    "base": "minecraft:item/avatar",
    "model": { "type": "minecraft:head", "kind": "player" }
  }
}
```
`minecraft:special` + `minecraft:head` is what puts the **player skin into `Sampler0`** while
letting you supply the geometry via `base`. That is the modern replacement for the
`builtin/entity` + `template_skull` trick.

Composites — a gun model plus two skinned hands:
```json
{
  "model": {
    "type": "minecraft:composite",
    "models": [
      { "type": "minecraft:model", "model": "minecraft:item/mosin_rifle" },
      { "type": "minecraft:special", "base": "minecraft:item/left_hand",
        "model": { "type": "minecraft:head", "kind": "player" } },
      { "type": "minecraft:special", "base": "minecraft:item/right_hand",
        "model": { "type": "minecraft:head", "kind": "player" } }
    ]
  }
}
```
The `base` models then use `display` to place themselves only in first person and collapse
everywhere else:
```json
{
  "textures": { "particle": "block/soul_sand" },
  "display": {
    "firstperson_righthand": { "rotation": [22.5, 180, 0], "translation": [-9, 1.84, -4.19], "scale": [0.471, 0.515, 1.515] },
    "gui":                   { "scale": [0, 0, 0] },
    "fixed":                 { "scale": [0, 0, 0] },
    "ground":                { "scale": [0, 0, 0] },
    "thirdperson_righthand": { "scale": [0, 0, 0] }
  }
}
```
`"scale": [0, 0, 0]` in a display context is the standard "exists but occupies no area"
device: geometry is still submitted (so your shader still runs and can rewrite
`gl_Position`) but contributes no visible pixels.

### Multi-version packs

Ship both under overlays: `models/item/*.json` with `overrides` in the `[22,45]` overlay,
`items/*.json` in the `[46,99]` overlay. Packs that must span the whole range end up with
half a dozen overlays whose ranges encode every item-format change Mojang made.

---

## 2. Atlases — getting a custom texture stitched

A texture the shader needs to recognise must be in the block/item atlas.

`assets/minecraft/atlases/blocks.json`
```json
{ "sources": [ { "type": "directory", "source": "custom", "prefix": "custom/" } ] }
```
puts everything in `textures/custom/` into the block atlas as `custom/<name>`.

Multiple sources:
```json
{ "sources": [
    { "type": "directory", "source": "voxel",  "prefix": "voxel/"  },
    { "type": "directory", "source": "custom", "prefix": "custom/" }
] }
```

Consequences to design around:
- **Sprite position is not stable.** Never hardcode atlas coordinates; find your data
  relative to the fragment's own UV (`data-channels.md § 4`).
- **Mipmapping blurs the edges of your sprite** into its neighbours. Read tags with
  `textureLod(Sampler0, UV0, -4)` or `texelFetch`, and pad data sprites with a border.
- `textureSize(Sampler0, 0)` gives the atlas size, not the sprite size — useful as a cheap
  "which texture am I in" test when a texture is *not* atlased (`Sampler0` bound directly to
  a skin, a map, a GUI texture).

---

## 3. Arbitrary meshes encoded in a texture

The established solution is to store an OBJ mesh (positions, UVs, per-frame animation) inside
the model's own texture. The vertex shader reads vertex data with `texelFetch` and displaces
each of the model's dummy quads to the right place. The open-source **objmc** toolchain
implements this and defines the de-facto format; the layout below describes that format so a
shader can interoperate with it.

### Texture layout

```
row 0:  [magic(12,34,56,78)] [meta1] [meta2] [meta3] [meta4] [meta5] ...
rows 1..headerheight-1: vertex index table
rows headerheight..headerheight+size.y-1: the visible texture
then vph rows of positions, then vth rows of UVs
```
Header fields (each a pixel read as `ivec4 * 255`):

| Pixel | Contents |
|---|---|
| 0 | magic `(12, 34, 56, 78)` |
| 1 | flags: autorotate xy, noshadow, colorbehavior high bits |
| 2 | `size.x = r*256+g`, `size.y = b*256+a-128+bit` |
| 3 | `nvertices` (32-bit across rgba) |
| 4 | `nframes`, `ntextures`, `duration`, `autoplay`+`easing` |
| 5 | `vph` (position rows), `vth` (uv rows) |

Every *other* pixel of the sprite stores its own offset back to the top-left, so any
fragment/vertex can locate the header:
```glsl
ivec4 metauvoffset = ivec4(texelFetch(Sampler0, uv, 0) * 255);
ivec2 uvoffset = ivec2(metauvoffset.r * 256 + metauvoffset.g, metauvoffset.b + 1);
ivec2 topleft = uv - uvoffset;
if (ivec4(texelFetch(Sampler0, topleft, 0) * 255) == ivec4(12, 34, 56, 78)) { /* it's ours */ }
```

### Accessors

Everything is unsigned bytes, so each reader is "fetch texels, scale by 255, reassemble".
Write them once with named constants rather than inlining the arithmetic:

```glsl
const float BYTE = 255.0;

ivec4 fetchByte4(ivec2 origin, ivec2 offset) {
    return ivec4(texelFetch(Sampler0, origin + offset, 0) * BYTE);
}

// header cell N, one texel to the right of the magic
ivec4 readHeader(ivec2 origin, int index) {
    return fetchByte4(origin, ivec2(index, 0));
}

// map a linear element index onto the data region, which wraps at `width`
ivec2 dataTexel(ivec2 origin, int width, int rowBase, int index) {
    return origin + ivec2(index % width, rowBase + index / width);
}

// a position: three consecutive texels, each a 24-bit fixed-point value
// laid out as (high byte, middle byte, low byte) and biased by half the range
vec3 readPosition(ivec2 origin, int width, int rowBase, int index) {
    vec3 v;
    for (int axis = 0; axis < 3; axis++) {
        vec4 t = texelFetch(Sampler0, dataTexel(origin, width, rowBase, index * 3 + axis), 0);
        v[axis] = t.r * 256.0 + t.g + t.b / 256.0;
    }
    return v * (BYTE / 256.0) - vec3(128.0);        // undo the bias
}

// a UV: two consecutive texels, 16 bits each in the g and b channels
vec2 readUV(ivec2 origin, int width, int rowBase, int index) {
    vec2 uv;
    for (int axis = 0; axis < 2; axis++) {
        vec4 t = texelFetch(Sampler0, dataTexel(origin, width, rowBase, index * 2 + axis), 0);
        uv[axis] = (t.g * 65280.0 + t.b * BYTE) / 65535.0;
    }
    return uv;
}

// an index pair: two texels, each a 24-bit big-endian integer
ivec2 readIndexPair(ivec2 origin, int width, int rowBase, int index) {
    ivec2 r;
    for (int k = 0; k < 2; k++) {
        ivec4 t = fetchByte4(origin, dataTexel(ivec2(0), width, rowBase, index * 2 + k));
        r[k] = (t.r << 16) | (t.g << 8) | t.b;
    }
    return r;
}
```

Two conventions to note, because they are easy to get backwards:
- positions are **big-endian fixed point with a +128 bias**, so a value of zero encodes as the
  middle of the range and both signs are representable without a sign bit;
- UVs skip the red channel entirely, which leaves it free for the per-face offset that lets a
  fragment find the header.


### The vertex transform

```glsl
int corner = gl_VertexID % 4;
// relative vertex id, derived from this face's unique UV offset
int id = (((uvoffset.y - 1) * size.x) + uvoffset.x) * 4 + corner;
id += frame * nvertices;

int headerheight = 1 + int(ceil(float(nvertices) * 0.25 / float(size.x)));
int height = headerheight + size.y;

ivec2 index = getvert(topleft, size.x, height + vph + vth, id);
vec3 posoffset = getpos(topleft, size.x, height, index.x);
texCoord = getuv(topleft, size.x, height + vph, index.y) * vec2(size);

Pos += posoffset;
texCoord = (vec2(topleft.x, topleft.y + headerheight) + texCoord) / vec2(atlasSize)
         + vec2(onepixel.x * 0.0001 * corner, onepixel.y * 0.0001 * ((corner + 1) % 4));
```
That last epsilon term breaks ties so faces sharing identical UV bounds still rasterise.

### Animation and interpolation

```glsl
float time = GameTime * 24000.0;
time = autoplay ? time + float(nframes) * duration - mod(float(tcolor), float(nframes) * duration)
                : float(tcolor);
int frame = int(time / duration) % nframes;

if (nframes > 1 && easing > 0) {
    int nids = nframes * nvertices;
    id = (id + nvertices) % nids;
    vec3 p2 = getpos(topleft, size.x, height, getvert(topleft, size.x, height+vph+vth, id).x);
    transition = fract(time / duration);
    switch (easing) {
        case 1: posoffset = mix(posoffset, p2, transition); break;              // linear
        case 2: transition = transition < 0.5 ? 4.0*transition*transition*transition
                                              : 1.0 - pow(-2.0*transition + 2.0, 3.0)*0.5;
                posoffset = mix(posoffset, p2, transition); break;              // cubic in-out
        case 3: /* fetch p3, p4 and do a 4-point bezier */ break;
    }
}
```
```glsl
vec3 bezb(vec3 a, vec3 b, vec3 c, vec3 d, float t) {
    float t2 = t*t, t3 = t2*t;
    return (d-3*c+3*b-a)*t3 + (3*c-6*b+3*a)*t2 + (3*b-3*a)*t + a;
}
vec3 bezier(vec3 a, vec3 b, vec3 c, vec3 d, float t) {
    return bezb(b, b + (c-a)/6.0, c - (d-b)/6.0, c, t);
}
```

### `colorbehavior` — repurposing the three colour bytes

Each model declares, in its header, how `Color.rgb` maps onto rotation axes and/or the
animation tick:
```glsl
vec3 accuracy = vec3(255.0/256.0);
switch ((colorbehavior/64) % 4) {          // first colour byte
    case 0: rotation.x += Color.r*255.0; accuracy.r *= 256.0; break;
    case 1: rotation.y += Color.r*255.0; accuracy.g *= 256.0; break;
    case 2: rotation.z += Color.r*255.0; accuracy.b *= 256.0; break;
    case 3: tcolor = tcolor * 256 + int(Color.r*255.0);       break;
}
/* same for /16 %4 (green) and %16 (blue), where blue also has case 4 = hue tint */
rotation = rotation / accuracy * 2.0 * PI;
```
Two bytes on the same axis give 16-bit angular resolution (`accuracy *= 256` twice).

Auto-rotation toward the camera, derived from the normal:
```glsl
if (any(greaterThan(autorotate, vec2(0))) && isGUI == 0) {
    vec3 local = Normal;
    float yaw   = -atan(local.x, local.z);
    float pitch = -atan(local.y, length(local.xz));
    posoffset = rotate(vec3(vec2(pitch, yaw) * autorotate, 0) + rotation) * posoffset;
}
```

### Context detectors such a format needs

```glsl
bool isgui(mat4 ProjMat) { return ProjMat[3][2] == -2.0; }   // GUI item render
bool ishand(float FogStart) { return FogStart * 0.000001 > 1.0; }  // first person (G1/G2 only)
```

### When to use a mesh texture vs. hand-authored boxes

Use one when the mesh genuinely cannot be boxes (organic shapes, imported modelling work) or
when you need per-vertex animation with easing. It costs one `texelFetch` per vertex per
attribute and a much larger texture. For anything boxy, plain model JSON is cheaper and much
easier to debug.

---

## 4. Raytracing a player model inside a GUI slot

A full 3-D player can be rendered from a player-head item by raytracing the skin's box
hierarchy in the fragment shader. This is the template for "render arbitrary 3-D content into
a 2-D UI cell".

### Setup

The item collapses to nothing (`scale: [0,0,0]`); the vertex shader detects the GUI head
render and inflates one quad into a canvas (`geometry-hijacking.md § 11`). The fragment
shader keeps exactly one quad (`if (quadId != 3) discard;`) and raytraces.

### Ray setup

```glsl
mat3 rotateY(float rad) {
    float c = cos(rad), s = sin(rad);
    return mat3(c, 0, s, 0, 1, 0, -s, 0, c);
}

mat3 cameraRot = rotateY(GameTime * rotationSpeed);
vec3 direction = cameraRot * normalize(vec3(-1.0));
vec3 side = normalize(cross(vec3(0, 1, 0), direction));
vec3 up   = cross(direction, side);

vec2 clip = texCoord0 * 2.0 - 1.0;
vec3 origin = cameraRot * vec3(1.0, 1.75, 1.0) * 20.0 + 10.0 * (side * clip.x - up * clip.y);
```
Orthographic (rays are parallel, origin varies with the pixel) — correct for a GUI icon and
cheaper than perspective.

### Slab box intersection

```glsl
struct intersection {
    float t, t2;
    bool inside;
    vec3 position;
    vec3 normal, normal2;
    vec2 uv;
    vec4 albedo;
};

bool boxIntersect(inout intersection it, vec3 origin, vec3 direction, vec3 position, vec3 size) {
    vec3 invDir = 1.0 / direction;
    vec3 ext  = abs(invDir) * size;
    vec3 tMin = -invDir * (origin - position) - ext;
    vec3 tMax = tMin + ext * 2.0;
    float near = max(max(tMin.x, tMin.y), tMin.z);
    float far  = min(min(tMax.x, tMax.y), tMax.z);
    if (near > far || far < 0.0 || near > it.t) return false;
    it.inside   = near <= 0.0;
    it.t        = it.inside ? far : near;
    it.t2       = far;
    it.normal   = (it.inside ? step(tMax, vec3(far)) : step(vec3(near), tMin)) * -sign(direction);
    it.normal2  = step(tMax, vec3(far)) * -sign(direction);
    it.position = direction * it.t + origin;
    return true;
}
```

### Mapping a hit to skin UVs

The six faces of each body part have fixed rectangles in the 64×64 skin. Encode them as
`vec4(u0, v0, u1, v1)` per face, twelve per part (six base + six overlay):
```glsl
void boxTexCoord(inout intersection it, vec3 origin, vec3 size, const vec4 uvs[12], int offset) {
    vec3 t = it.position - origin;
    vec3 mask = abs(it.normal);
    vec2 uv  = mask.x * t.zy + mask.y * t.xz + mask.z * t.xy;
    vec2 dim = mask.x * size.zy + mask.y * size.xz + mask.z * size.xy;
    uv = mod(uv / (dim * 2.0) + 0.5, 1.0);

    vec4 uvmap;
    vec3 normal = it.normal * (it.t == it.t2 ? -1.0 : 1.0);
    if      (normal.x ==  1.0) uvmap = uvs[offset];
    else if (normal.x == -1.0) uvmap = uvs[offset + 1];
    else if (normal.y ==  1.0) uvmap = uvs[offset + 2];
    else if (normal.y == -1.0) uvmap = uvs[offset + 3];
    else if (normal.z ==  1.0) uvmap = uvs[offset + 4];
    else if (normal.z == -1.0) uvmap = uvs[offset + 5];
    it.uv = floor(mix(uvmap.xy, uvmap.zw, uv));
}
```
The head table, for reference (base layer then hat layer offset by `+vec4(32,0,32,0)`):
```glsl
const vec4 headUV[12] = vec4[](
    vec4( 0, 16,  8,  8), vec4(24, 16, 16,  8), vec4(16,  0,  8,  8),
    vec4(24,  0, 16,  8), vec4(16, 16,  8,  8), vec4(24, 16, 32,  8),
    vec4(24, 16, 16,  8) + vec4(32, 0, 32, 0),
    vec4( 0, 16,  8,  8) + vec4(32, 0, 32, 0),
    vec4(16,  0,  8,  8) + vec4(32, 0, 32, 0),
    vec4(24,  0, 16,  8) + vec4(32, 0, 32, 0),
    vec4(16, 16,  8,  8) + vec4(32, 0, 32, 0),
    vec4(24, 16, 32,  8) + vec4(32, 0, 32, 0));
```
Body, arms and legs follow the same shape; the full tables are in.
### Transparent-texel fall-through

Skin overlays are mostly transparent. When the front face is transparent, continue to the
back face of the same box before giving up:
```glsl
void box(inout intersection it, vec3 origin, vec3 direction, mat3 transform,
         vec3 position, vec3 size, const vec4 uvs[12], int layer) {
    intersection temp = it;
    vec3 originT = transform * origin, directionT = transform * direction;
    if (!boxIntersect(temp, originT, directionT, position, size)) return;
    boxTexCoord(temp, position, size, uvs, layer * 6);
    temp.albedo = texelFetch(Sampler0, ivec2(temp.uv), 0);
    if (temp.albedo.a < 0.1) {
        if (temp.t == temp.t2 || temp.t2 >= FAR) return;
        temp.t = temp.t2; temp.normal = temp.normal2;
        temp.position = directionT * temp.t + originT;
        boxTexCoord(temp, position, size, uvs, layer * 6);
        temp.albedo = texelFetch(Sampler0, ivec2(temp.uv), 0);
        if (temp.albedo.a < 0.1) return;
    }
    temp.normal   = temp.normal * transform;         // back to world space
    temp.position = direction * temp.t + origin;
    it = temp;
}
```

### The scene

Overlay layer first (it is larger and would otherwise be occluded), base layer second; the
`it.t` running minimum sorts them:
```glsl
intersection it = intersection(far, far, false, vec3(0.0), vec3(0.0), vec3(0.0), vec2(0.0), vec4(1,1,1,0));
// overlay (inflated)
box(it, origin, direction, mat3(1.0),  vec3(0, 6, 0),      vec3(4, 6, 2) + 0.25,  bodyUV,     1);
box(it, origin, direction, leftArmT,   vec3(-6.5, 5.5, 0), vec3(1.5, 6, 2) + 0.25, leftArmUV,  1);
box(it, origin, direction, rightArmT,  vec3( 6.5, 5.5, 0), vec3(1.5, 6, 2) + 0.25, rightArmUV, 1);
box(it, origin, direction, mat3(1.0),  vec3(0, 16, 0),     vec3(4, 4, 4) + 0.5,   headUV,     1);
// base
box(it, origin, direction, mat3(1.0),  vec3(0, 6, 0),      vec3(4, 6, 2),  bodyUV,     0);
/* ... */
```
Arms are tilted 5° via a constant rotation matrix, which is what makes the pose read as a
character rather than a mannequin:
```glsl
const mat3 rightArmT = mat3(cos(radians(5.0)), -sin(radians(5.0)), 0,
                            sin(radians(5.0)),  cos(radians(5.0)), 0, 0, 0, 1);
```

### Shading and translucency

```glsl
vec3 lightDir = cameraRot * normalize(vec3(0.0, 1.0, 0.5));
float directional = dot(it.normal, lightDir) * 0.4 + 0.6;
fragColor = vec4(it.albedo.rgb * directional, it.albedo.a);

for (int i = 0; i < 4 && fragColor.a < 1.0; i++) {      // up to 4 translucent layers
    it = rayTrace(it.position + direction * 0.01, direction, FAR);
    if (it.t < FAR) {
        directional = dot(it.normal, lightDir) * 0.4 + 0.6;
        fragColor = vec4(blend(it.albedo.rgb * directional, fragColor), it.albedo.a);
    }
}
```
The quality version adds ambient occlusion, antialiasing (supersample the clip coordinate)
and specular reflections — same skeleton, more rays.

---

## 5. Sprite-based fake 3-D characters

Much cheaper than raytracing when the result is small: composite a 16x16 character sprite out
of skin regions, with a hand-authored idle animation.
```glsl
vec4 sampleSkinQuad(sampler2D skin, ivec2 pixel, ivec2 position, ivec2 size,
                    ivec2 baseCoords, ivec2 hatCoords, ivec2 skinSize) {
    if (pixel.x < position.x || pixel.y < position.y
     || pixel.x >= position.x + size.x || pixel.y >= position.y + size.y) return vec4(0.0);
    ivec2 rel = pixel - position;
    rel = ivec2(vec2(rel * skinSize) / vec2(size));
    vec4 base = texelFetch(skin, baseCoords + rel, 0);
    vec4 hat  = texelFetch(skin, hatCoords  + rel, 0);
    return vec4(mix(base.rgb, hat.rgb, hat.a), 1.0 - (1.0 - base.a) * (1.0 - hat.a));
}
```
with body parts at fixed sprite positions (`head (4,1)`, `body (4,9)`, `lArm (3,10)`,
`rArm (12,10)`, `lLeg (4,14)`, `rLeg (9,14)`) offset per animation frame at 10 fps.

A 1-pixel outline, drawn by dilating the alpha:
```glsl
if (color.a != 1.0)
    for (int x = -1; x <= 1; ++x) for (int y = -1; y <= 1; ++y)
        if (samplePlayerSprite(Sampler0, texCoord0 + 0.5 * vec2(x,y) / vec2(textureSize(Sampler0,0)),
                               spriteId, GameTime).a == 1.0)
            color = vec4(0.0, 0.0, 0.0, 1.0);
```

### 2-D GUI head icons

Build an isometric-ish head and body avatar by copying rectangles of the skin with per-region
scaling and a 0.6 side-face darkening:
```glsl
bool rect(ivec2 c, int x1, int x2, int y1, int y2) {
    return c.x >= x1 && c.x < x2 && c.y >= y1 && c.y < y2;
}
if (rect(pixel, 0, 16, 0, 18)) {                    // overlay front, 2x scaled with 2 seams
    if (pixel.y > 1)  pixel.y--;
    if (pixel.y > 15) pixel.y--;
    fragColor = texelFetch(Sampler0, pixel / 2 + ivec2(40, 8), 0);
} else if (rect(pixel, 16, 24, 0, 18)) {            // overlay right side
    pixel.x -= 16;
    if (pixel.y > 0)  pixel.y--;
    if (pixel.y > 15) pixel.y--;
    fragColor = texelFetch(Sampler0, pixel / ivec2(1, 2) + ivec2(48, 8), 0);
    fragColor.rgb *= 0.6;
}
```
The `if (pixel.y > N) pixel.y--` lines are deliberate seam removals: a 2× upscale of a 16 px
face into 18 px needs two duplicated rows dropped, and doing it by index avoids any
filtering.

---

## 6. Tight armor / shrink-wrapping

Shrink armour geometry toward the body so it does not z-fight with the player model, and
compensate the UVs so the texture is not stretched.
```glsl
const float lostwarArmorTightening = 0.02;

vec3 applyLostwarTightArmor(vec3 position, vec3 normal) {
    float offset = lostwarArmorTightening;
    if (length(vec3(ProjMat[0][3], ProjMat[1][3], ProjMat[2][3])) < 0.1) offset *= 32.0;  // GUI
    return position - normalize(normal) * offset;
}

vec2 lostwarArmorUvCutoff(vec2 faceUvCenterPixels, vec2 faceUvSizePixels,
                          vec2 samplerSize, bool leggingsTexture) {
    vec2 uvSize = max(faceUvSizePixels, vec2(1.0));
    float expansion = lostwarArmorExpansionPixels(faceUvCenterPixels, samplerSize, leggingsTexture);
    vec2 geometrySize = uvSize + vec2(expansion * 2.0);
    return lostwarArmorTightening * 16.0 * uvSize / geometrySize + vec2(0.015);
}
```
It recovers each face's UV rectangle from three corner varyings, because a fragment shader
only sees interpolated UV:
```glsl
if (gl_VertexID % 4 == 0) armorCornerTex1 = vec3(UV0, 1.0);
if (gl_VertexID % 4 == 2) armorCornerTex2 = vec3(UV0, 1.0);
if (gl_VertexID % 2 == 1) armorCornerTex3 = vec3(UV0, 1.0);   // flat out
```
Interpolation of a varying that is non-zero at exactly one corner delivers that corner's
value scaled by the barycentric weight, so dividing by the `.z` component recovers it. This
"per-corner varying" pattern is the same one used to recover quad bounds elsewhere
(`techniques-index.md § B3`).
---

## 7. Generating thousands of models

A per-block marker scheme means 1900+ block model overrides. Hand-authoring is not viable.
Generate them.

```python
import json, pathlib
OUT = pathlib.Path("assets/minecraft/models/block")
for block, map_color_index in blocks.items():
    OUT.joinpath(f"{block}.json").write_text(json.dumps({
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"minecraft:block/{block}", "minimap": f"block/minimap_{map_color_index}"}
    }))
```
Rules that save pain later:
- Derive the list from the vanilla jar's own model files, not from a hand-typed block list.
- Keep the generator in the repo next to the pack so regeneration for a new MC version is one
  command.
- One texture per distinct data value, not one per block, so the atlas stays small.
