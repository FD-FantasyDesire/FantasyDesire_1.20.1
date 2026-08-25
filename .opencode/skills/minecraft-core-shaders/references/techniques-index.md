# Techniques index — the complete idea catalog

The other reference files are organised by *task* ("I need to do X"). This one is organised
by *idea*, and is exhaustive. If a trick exists in this discipline, it should be here, even
when it fits no category.

Full treatments of the big systems live in `data-channels.md`, `geometry-hijacking.md`,
`postfx-cookbook.md`, `encodings.md`, `gpu-ui.md`, `mesh-tricks.md`. This file carries the
insight, the minimal code, and where it generalises.

---

# A. Getting information you "cannot" have

### A1. Data marker — core shader writes uniforms into pixels for post shaders
The founding trick. Full protocol in `data-channels.md § 1`.
**Generalises to:** any producer→consumer link between two shader stages that cannot share
uniforms.

### A2. Column markers, not just rows
Data can equally live down a **column** of an isolated target (say `ivec2(1, 40..72)`)
rather than along row 0. A column survives passes that only guard row 0, and lets several unrelated
producers own disjoint column ranges.

### A3. Floats packed across pixel boundaries
Store 16 exact float32s in 22 RGB pixels by letting each float straddle two pixels:
```glsl
decodeFloat(vec4(c0.xyz, c1.x)), decodeFloat(vec4(c1.yz, c2.xy)),
decodeFloat(vec4(c2.z, c3.xyz)), ...
```
Zero waste, bit-exact. Costs a hand-written index table.

### A4. Feature flags as magic rows
Instead of one "is the data valid" bit, give every subsystem its own magic row:
```glsl
hasData        = round(texelFetch(S, ivec2(1, 69), 0).rgb * 255.0) == vec3(191, 32, 1);
hasVolumetrics = round(texelFetch(S, ivec2(1, 72), 0).rgb * 255.0) == vec3(191, 32, 5);
hasSunColor    = round(texelFetch(S, ivec2(1, 71), 0).rgb * 255.0) == vec3(191, 32, 3);
```
Shared prefix `(191, 32)` = "this is a flag row"; third byte = which flag. The datapack turns
features on and off by placing or not placing one item.

### A5. Lightmap alpha as a 256-byte uniform bus
`data-channels.md § 2`. The only way to read MC's own lighting parameters
(`AmbientLightFactor`, `SkyFactor`, `NightVisionFactor`, `DarknessScale`, …).

### A6. Reading the sun out of the lightmap
No sun uniform exists. Infer sun intensity from the **hue deviation** of the lightmap's
brightest texel:
```glsl
#define NCOLOR normalize(vec3(1.0, 1.0, 1.0))
#define DCOLOR normalize(vec3(1.0, 1.0, 1.0))
float getSun(sampler2D lightMap) {
    vec3 sunlight = normalize(texture(lightMap, vec2(1.0, 1.0)).rgb);
    return clamp(pow(length(sunlight - NCOLOR) / length(DCOLOR - NCOLOR), 4.0), 0.0, 1.0);
}
```
and then **flattens the lightmap to grayscale** so its own deferred lighting can re-colour
the scene:
```glsl
vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    float sun = 1.0 - uv.y / 256.0 * getSun(lightMap);
    vec4 original = texture(lightMap, clamp(uv / 256.0, vec2(0.8/16.0), vec2(15.5/16.0)));
    float d = (original.r + original.b + original.g) / 3.0;
    return vec4(d, d, d, original.a);
}
```
**Generalises to:** treating any vanilla-computed texture as a *sensor* for the state that
produced it.

### A7. Fog uniforms as a pass selector
The datapack sets `FogStart`/`FogEnd`/`FogColor` to sentinel values before the pass it wants,
and the shader dispatches on them (`data-channels.md § 11`). This is how a pack forces a
wide-FOV portal projection or an orthographic shadow projection out of an unmodified client.

### A8. Overriding a vanilla include to change every shader at once
A pack that ships `shaders/include/fog.glsl` replaces it for the whole game — useful both to
reshape fog globally and to smuggle in a sentinel:
```glsl
if (abs(fogStart + 8.0) < 0.5) { fogEnd = 3.0; fogColor = vec4(0.156, 0.105, 0.070, 1.0); }
```
i.e. `FogStart == -8` means "dark corridor mode".

### A9. `GameTime` as a server→client scalar
`data-channels.md § 3`. Costs the day cycle.

### A10. Vertex `Color.a` as an animation clock
Use `Color.a` as a normalised 0–1 timeline the server advances each tick, and derive every
keyframe from it:
```glsl
float t = Color.a * 4.0;
vertexColor.a = clamp(min(8.0 * t, 4.0 - 4.0 * t), 0.0, 1.0);   // fade in/out envelope
float fallT = max((t - 0.75) / 0.25, 0.0);                       // sub-phase
float shakeT = clamp((t - 0.6) / 0.15, 0.0, 1.0);
```
Far better than `GameTime` for per-entity animation: no wrap problem, per-entity phase,
server-authoritative.

### A11. Vertex `Color.rgb` as birth-tick + duration
`Color.rg` = 16-bit birth tick, `Color.b` = duration in ticks. Lets the
shader run a timeline without any per-frame server traffic. Needs the midnight wrap guard.

### A12. Coordinate region bits
`ivec3(worldpos + 512.0) >> 10` splits a world position into "which behaviour" + "where"
(`data-channels.md § 7`).

### A13. Position threshold bands
`p.y < -1000`, then `-2e6`, `-3e6`, `-4e6` select HUD anchors. Simple, robust, used by four
projects.

### A14. Y-bitfield element ids
Shift an element id into the Y coordinate's bits
(`int(pos.y) >> HEIGHT_BIT`). Gives hundreds of distinct elements from one render type.

### A15. Texture magic pixels with corner pointers
Header at the sprite's top-left, and **every other pixel stores its offset back to it**, so
the shader can find the header regardless of where the atlas stitcher put the sprite
(`data-channels.md § 4`).

### A16. Texture alpha as an effect id
246–254 are visually opaque and free. Packs use the range to dispatch ported shader effects,
to carry emission strength, and to mark emissive or hidden variants — so check what else in
your pack already claims part of it. **Read it unmipped** (`textureLod(…, -4)`).

### A17. Sub-texel UV from RG bytes + interpolated fraction
An 8-bit-per-axis coarse grid in the texel, refined by the fraction of the interpolated UV:
```glsl
vec2 localUV = texture(Sampler0, texCoord0).rg;
vec2 uvStep  = 1.0 / vec2(textureSize(Sampler0, 0));
vec2 uvFine  = vec2(fract(texCoord0.x / max(uvStep.x, 1e-6)),
                    fract(texCoord0.y / max(uvStep.y, 1e-6)));
localUV = clamp((floor(localUV * 255.0) + uvFine) / 255.0, 0.0, 1.0);
```
Gives a full-precision surface parameterisation from an 8-bit texture..
### A18. Map items as a 16 KB frame buffer
`data-channels.md § 8`. Highest-bandwidth server→client channel that exists.

### A19. Non-black as a bit
```glsl
bool set = sign(length(texture(Sampler0, uv).xyz)) > 0.0;
```
Read config bits from a map this way instead of by exact comparison. It survives palette
quantisation, brightness shading, and filtering — everything that would break an exact test.
**Use whenever the carrier lossily transforms your data.**

### A20. Post-effect toggles as a bit bus (26.2+)
One tiny post effect per bit; the server enables the subset that is 1
(`data-channels.md § 10`). Lowest latency channel available.

### A21. Render targets as frame-to-frame memory, with a canary
Targets are never cleared. Add a fixed improbable pixel and validate it before trusting
anything (`data-channels.md § 9`). Without the canary you will silently decode garbage after
every resize or resource reload.

### A22. Scene-change detection by pixel diff
Decide "the scene changed, reset the timer" by comparing one pixel of `minecraft:main`
against last frame's copy. Crude, free, and enough for menus and transitions.

### A23. Conditional blit driven by a pixel
The pipeline is static, but *which* frame gets stored is data-driven:
```glsl
// vertex
save = int(round(texelFetch(ControlSampler, ivec2(PixelX, Offset), 0).rgb * 255.0) == Color);
// fragment
fragColor = (save == 1) ? texture(DiffuseSampler, texCoord) : texture(SavedSampler, texCoord);
```
This is what captures an off-camera frame for a portal or a shadow map.

### A24. Restoring a depth buffer into a real depth attachment
A depth buffer packed into RGBA8 can be written back to a *depth* attachment so later passes
get hardware depth compare:
```glsl
gl_FragDepth = decodeFloat(texture(DepthSampler, texCoord));
```
This is what makes a persistent shadow map possible.

### A25. Time of day from the sun quad's angle
The sky pass has no time uniform. The sun quad's world direction *is* the clock, so recover
it from the quad's own geometry:
```glsl
vec3 center = (pos1 + pos3) * 0.5;                                   // quad centre direction
float currentTime = 1.0 - fract(atan(center.x, center.y) / PI * 0.5 + 0.5);   // 0..1 per day
```
`pos1`/`pos3` come from the per-corner varying trick (B3), saved before the quad was
hijacked. Same family as A6: **rendered geometry used as a sensor for the state that
positioned it.**

### A26. Self-describing texture layout — no header at all
Derive the whole layout from `textureSize` plus a known aspect ratio, so nothing has to be
encoded:
```glsl
ivec2 texSize      = textureSize(Sampler0, 0);
ivec2 cubemapSize  = ivec2(texSize.x, texSize.x / 3 * 2);   // a 3x2 cubemap cross
int   cubemapCount = texSize.y / cubemapSize.y;
int   sunSize      = texSize.y - cubemapCount * (cubemapSize.y + 1);   // the leftover is the sun sprite
```
The pack author appends N cubemaps to `sun.png` and the shader works out N, their size, and
the size of the original sun sprite, from the image dimensions alone. Cheaper and more
robust than a magic header when the layout has a fixed shape.
Per-item metadata rides in a 1-pixel gap row before each block:
```glsl
float startTime = texelFetch(Sampler0, ivec2(0, sunSize + (1 + cubemapSize.y) * i), 0).r;
float endTime   = texelFetch(Sampler0, ivec2(1, sunSize + (1 + cubemapSize.y) * i), 0).r;
```

### A27. Magic alpha to identify *which texture* is bound
When several textures flow through one render type, tag the one you care about:
```glsl
isSun = (abs(texelFetch(Sampler0, ivec2(16), 0).a * 255.0 - 17.0) < 0.5) ? 1.0 : 0.0;
```
The vanilla skybox pack asks the pack author to set alpha 17 in the sun texture. Cheaper
than `textureSize` comparison and works when two textures share dimensions.

---

# B. Rasteriser and geometry abuse

### B1. Rewrite `gl_Position` to NDC
The base move. Corner tables, depth layering, and the "never draw" idioms are in
`geometry-hijacking.md § 2` and `§ 12`.

### B2. `gl_VertexID % 4` / `/ 4` as quad identity
Corner index and quad index. Everything else is built on this.

### B3. Per-corner varyings — recover a quad's geometry inside the fragment shader
Set exactly one of four varyings per corner, leave the rest zero; interpolation delivers the
corner data weighted by barycentrics, and dividing by the `.z` guard component recovers it.
```glsl
// vertex
vctfx_ipos1 = vctfx_ipos2 = vctfx_ipos3 = vctfx_ipos4 = vec3(0.0);
switch (gl_VertexID % 4) {
    case 0: vctfx_ipos1 = vec3(gl_Position.xy, 1.0); break;
    case 1: vctfx_ipos2 = vec3(gl_Position.xy, 1.0); break;
    case 2: vctfx_ipos3 = vec3(gl_Position.xy, 1.0); break;
    case 3: vctfx_ipos4 = vec3(gl_Position.xy, 1.0); break;
}
// fragment
vec2 ip1 = vctfx_ipos1.xy / vctfx_ipos1.z;   /* …and so on… */
vec2 innerMin = min(ip1, min(ip2, min(ip3, ip4)));
vec2 innerMax = max(ip1, max(ip2, max(ip3, ip4)));
```
Used for glyph screen and UV rects in text effects, face UV rects in shrink-wrapped armour,
quad tangent frames, and atlas bounds for manual filtering.
**This is the single most reusable structural trick after the data marker.**

### B4. `gl_PrimitiveID % 2` to know which triangle of the quad you are in
Needed when you want a *tangent frame*, since one triangle only sees three of the four
corners:
```glsl
vec3 pPos = gl_PrimitiveID % 2 == 0 ? pos1 : pos3;
vec3 tangent   = normalize(gl_PrimitiveID % 2 == 1 ? pos0 - pPos : pPos - pos2);
vec3 bitangent = normalize(gl_PrimitiveID % 2 == 0 ? pPos - pos0 : pos2 - pPos);
```

### B5. Expand the quad so an effect has room to draw outside the glyph
Set an `expand` flag in the effect; the vertex shader then inflates the quad and the fragment
shader clips with a UV bounds test:
```glsl
if (textData.shouldScale) { gl_Position.xy += corner * 0.5; vctfx_changedScale = 1.0; }
...
if (uvBoundsCheck(textData.uv, uvMin, uvMax)) textData.doTextureLookup = false;
```
Without it, outlines and shakes get clipped to the original glyph rectangle.

### B6. Depth as a free reduction
Encode a key in NDC z and the hardware depth test performs argmin/argmax across all draws.
"The highest block at this XZ" comes out for nothing:
```glsl
gl_Position.z = -float(blockPos.y + 386) / 1024.0;
```

### B7. Voxelization — one block, one pixel
2-D and 3-D addressing in `geometry-hijacking.md § 9`.

### B8. Checkerboard voxel placement
`p.x = p.x * 2 + (p.y & 1)` so neighbouring voxels are never neighbouring pixels — otherwise
blur and flood-fill passes leak between unrelated voxels.

### B9. Fixed-size targets to decouple data from the window
`{"width": 4000, "height": 3000}` keeps a voxel grid or map alive across resizes.

### B10. Projection substitution per pass
Replace `ProjMat` wholesale for a detected pass: portal FOV, shadow ortho, 2-D side world,
skybox far plane, fixed FOV. `geometry-hijacking.md § 7`, `§ 8`.

### B11. Far-plane rewrite for a skybox
```glsl
projMat[2][2] = -((zFar + zNear) / (zFar - zNear));
projMat[2][3] = -((2.0 * zFar * zNear) / (zFar - zNear));
```

### B12. Degenerate vs off-screen suppression
`gl_Position = vec4(0.0)` (w = 0, degenerate) and `vec4(10.0)` (outside frustum) both remove
geometry before rasterisation — cheaper than `discard`.

### B13. Thin depth slab for view models
`gl_Position.z = -0.1 + gl_Position.z * 0.0001` keeps a weapon in front of the world while
preserving its internal sorting.

### B14. GUI canvas from an inflated item quad
Collapse the item with `"scale": [0,0,0]`, inflate one quad in the shader, discard the other
23 (`geometry-hijacking.md § 11`).

### B15. Off-screen edge clamping with a direction sprite
The waypoint algorithm (`geometry-hijacking.md § 4`); the `min(t.x, t.y)` ray-to-rectangle
clamp plus `if (center.z >= 0.0) screenPos *= -1.0` for behind-camera markers.

### B16. Region-limited triangle (26.2+)
Rasterise only a pixel rectangle instead of the whole screen, exploiting the fact that post
passes do not clear their target (`version-matrix.md § 5`). Makes per-bit post effects cheap.

### B17. Oversized fullscreen triangle from a hijacked quad
A quad gives you 4 vertices; a fullscreen *triangle* has no diagonal seam and rasterises
fewer fragments. Emit the oversized triangle and make the second one degenerate:
```glsl
switch (gl_VertexID % 4) {
    case 0: gl_Position = vec4(-2.0, 10.0, 0.0, 1.0); break;
    case 1: gl_Position = vec4(-2.0, -2.0, 0.0, 1.0); break;
    case 2: gl_Position = vec4(10.0, -2.0, 0.0, 1.0); break;
    case 3: gl_Position = vec4(10.0, -2.0, 0.0, 1.0); break;   // duplicate of case 2
}
```
plus a belt-and-braces guard in the fragment shader:
```glsl
if (gl_PrimitiveID >= 1) discard;
```
Applied to the sun quad, this turns one small billboard into the entire sky.

### B18. Save the original geometry before you steal it
When you repurpose a quad but still need what it *was*, stash its corners in per-corner
varyings (B3) before overwriting `gl_Position`:
```glsl
switch (gl_VertexID % 4) {
    case 0: gl_Position = /* fullscreen */; vertex1 = vec4(Position, 1.0); break;
    case 1: gl_Position = /* fullscreen */; vertex2 = vec4(Position, 1.0); break;
    case 2: gl_Position = /* fullscreen */; vertex3 = vec4(Position, 1.0); break;
    case 3: gl_Position = /* fullscreen */;                                break;
}
```
Three corners fully determine the quad's plane, orientation and extent; the fourth is
`center + (center - pos1)`. This is what lets the skybox pack raytrace the sun back into the
frame after having taken its geometry.

### B19. `gl_Position = vec4(-1.0)` to delete a draw
`w = -1` is behind the eye and degenerate — the cleanest way to delete vanilla stars or any
other unwanted draw. Compare `vec4(0.0)` (w = 0, degenerate) and `vec4(10.0)` (outside the frustum).
All three are cheaper than fragment-stage `discard`.

---

# C. Preprocessor and language abuse

### C1. A declarative config file from a macro that opens a case label
GLSL has no tables of function pointers, so behaviour keyed by a magic value normally becomes
an unmaintainable `switch`. A macro that expands to `break;` **plus a case label** turns that
switch into something that reads as data:

```glsl
// each RULE(...) closes the previous block and opens the next case
#define RULE(r, g, b) break; case (((r) >> 2 << 16) | ((g) >> 2 << 8) | ((b) >> 2)):

bool applyRules(inout Style st, vec3 key) {
    ivec3 q = ivec3(round(key * 255.0)) >> 2;          // quantise: 64 levels per channel
    switch ((q.r << 16) | (q.g << 8) | q.b) {
        case -1:                                        // unreachable, opens the chain
        #moj_import <rules_config.glsl>
        break;
        default: return false;
    }
    return true;
}
```
and the config file is editable by someone who does not read GLSL:
```glsl
RULE(12, 12, 72) {
    setColor(rgb(235, 177, 53));
    shimmer(0.5, 0.5);
    setDepth(-0.1);
}
RULE(200, 200, 190) {
    setColor(rgb(255, 255, 255));
    removeShadow();
}
```
Two details make it work: the **quantisation** (`>> 2`) so the key tolerates colour rounding,
and the **unreachable opening label** so the first `RULE` has something to close.

**Generalises to:** any table of behaviours keyed by a magic value — HUD elements, block
types, entity variants, effect ids. Generate the config file from your server configuration
and the whole table stops being hand-maintained.

### C2. `#ifdef VSH` / `#ifdef FSH` in a shared include
One include serves both stages; the core shader `#define`s its stage before importing. Every
library pack does this.

### C3. `#define`-gated feature modules
```glsl
// per render type, list what this shader enables
#define FEATURE_WAYPOINT
#define FEATURE_HUD

bool applyFeatures() {
#ifdef FEATURE_SKYBOX
    if (make_skybox())   return true;
#endif
#ifdef FEATURE_WAYPOINT
    if (make_waypoint()) return true;
#endif
#ifdef FEATURE_HUD
    if (make_hud())      return true;
#endif
    return false;
}
```
Features compile out entirely when unused — important because GLSL has no dead-code
guarantee across a `switch`.

### C4. A settings namespace the user can edit
`assets/settings/shaders/include/settings.glsl` imported as `#moj_import <settings:settings.glsl>`,
containing only `#define`s. Keeps the user-facing knobs in one small file.

### C5. Stub includes for cross-version compilation
A file containing only `#version 150`, named after a vanilla include that does not exist on
old versions (`version-matrix.md § 7b`).

### C6. `#define SHADER_VERSION n` per overlay
So runtime code can tell which variant loaded.

### C7. Disabling a file by renaming it
`no_rendertype_text_background.vsh` — keeps a variant in the tree without shipping it.

### C8. Early-out chain as the dispatch structure
```glsl
if (make_skybox())    return;
if (make_waypoint())  return;
if (make_hud())       return;
gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);   // vanilla fallthrough
```
Compute the vanilla varyings **before** the chain so every early return leaves valid outputs.

---

# D. Rendering methods

### D1. Normals from depth, closest-neighbour heuristic
`postfx-cookbook.md § 1`. Avoids silhouette halos.

### D2. Normals from depth, second-derivative heuristic (better)
Pick the axis direction whose *second difference* is smallest, using ±2 and ±4 taps:
```glsl
vec2 he = abs((2.0 * horizontal.xy - horizontal.zw) - depthCenter);
vec2 ve = abs((2.0 * vertical.xy   - vertical.zw)   - depthCenter);
vec3 horizontalDeriv = he.x < he.y ? left : right;
vec3 verticalDeriv   = ve.x < ve.y ? down : up;
return normalize(cross(horizontalDeriv, verticalDeriv));
```
Wider taps and a curvature test make it much more stable on sloped surfaces than the ±1
version.

### D3. Normals from screen-space derivatives of position
For custom meshes whose `Normal` attribute belongs to the dummy quad:
```glsl
vec3 normal = normalize(cross(dFdx(Pos), dFdy(Pos)));
if (isGUI == 1) normal.xz = -normal.xz;     // GUI renders mirrored
```.
### D4. SSAO with a fixed hemisphere kernel and blue-noise rotation
`postfx-cookbook.md § 2`.

### D5. Temporal reprojection with normal + depth rejection
`postfx-cookbook.md § 2`. The generic pattern for any accumulation buffer.

### D6. Camera motion from the wrapped chunk offset
```glsl
vec3 offset = mod(position - prevPosition + 8.0, 16.0) - 8.0;
```
There is no camera-position uniform; this recovers the *delta*, which is all reprojection
needs.

### D7. Bloom mip pyramid inside fixed-size targets
`postfx-cookbook.md § 3`. Render into shrinking sub-rectangles, clamp, do your own bilinear.

### D8. Screen-space reflections with exponential step growth and thickness rejection
```glsl
float tStep = 0.05 / max(0.001, cosTheta);
for (int i = 0; i < 32; i++) {
    t += tStep; tStep *= 1.15;
    /* … */
    if (viewPos.z < depth && viewPos.z > depth - tStep * 1.5) return binaryRefinement(...);
}
```
The `depth - tStep * 1.5` term rejects hits behind thin geometry (the classic SSR artefact
where a ray "hits" the back of a wall). Followed by 3–5 binary refinement steps.
See `postfx-cookbook.md § 7` and `§ 9`.
### D9. Physically-correct dielectric Fresnel
```glsl
float fresnelDielectric(float cosTheta0, float n0, float n1) {
    float sin2Theta0 = 1.0 - cosTheta0 * cosTheta0;
    float sin2Theta1 = sin2Theta0 * (n0 * n0) / (n1 * n1);
    if (sin2Theta1 >= 1.0) return 1.0;               // total internal reflection
    float cosTheta1 = sqrt(1.0 - sin2Theta1);
    float rs = (n0*cosTheta0 - n1*cosTheta1) / (n0*cosTheta0 + n1*cosTheta1);
    float rp = (n0*cosTheta1 - n1*cosTheta0) / (n0*cosTheta1 + n1*cosTheta0);
    return 0.5 * (rs * rs + rp * rp);
}
```
Both polarisations, not the Schlick approximation — the difference shows on water at grazing
angles.

### D10. Wave normals from summed directional waves plus simplex noise
```glsl
vec2 wavedx(vec2 position, vec2 direction, float frequency, float timeshift) {
    float x = dot(direction, position) * frequency + timeshift;
    float wave = exp(sin(x) - 1.0);          // sharp crests, flat troughs
    return vec2(wave, -wave * cos(x));       // value and derivative together
}
```
Six octaves with the position advected by the previous octave's derivative
(`position += p * res.y * weight * 0.38`) — that advection is what makes it look like water
rather than sine ripples.

### D11. Percentage-closer soft shadows with a directional penumbra
Stretch the PCF disc along the light direction projected into the surface tangent frame,
which is what a real penumbra does:
```glsl
vec2 directional = (sunDirection * mat3(b1, b2, normal)).xy;
penumbraOffset += directional * 0.2 * (random1() * 2.0 - 1.0);
```

### D12. Branchless orthonormal basis (Duff et al.)
```glsl
void buildOrthonormalBasis(vec3 n, out vec3 b1, out vec3 b2) {
    if (n.z < -0.9999999) { b1 = vec3(0,-1,0); b2 = vec3(-1,0,0); return; }
    float a = 1.0 / (1.0 + n.z);
    float b = -n.x * n.y * a;
    b1 = vec3(1.0 - n.x * n.x * a, b, -n.x);
    b2 = vec3(b, 1.0 - n.y * n.y * a, -n.y);
}
```

### D13. Neighbour pre-check to skip expensive shadowing
Only run the 36-tap PCF where the surface *and its four neighbours* face the sun:
```glsl
if (dot(normal, sunDirection) >= threshold &&
    dot(getNormal(texCoord + vec2(2,0)/InSize), sunDirection) >= threshold && /* …3 more… */)
    shadowing = getShadowing(position, normal);
```
Removes shadow acne at silhouettes and saves most of the cost.

### D14. Ray-marched volumetrics reusing the shadow map
```glsl
float phase = henyeyGreenstein(dot(direction, sunDirection), 0.08);
for (int i = 0; i < samples; i++) {
    float t = (float(i) + random1()) * tStep;      // jittered
    transmittance *= exp(-density * tStep);
    if (!isShadowed(direction * t, vec3(0.0))) volumetric += transmittance * scattering * tStep * phase;
}
fragColor.rgb = fragColor.rgb * transmittance + volumetric * lightColor;
```

### D15. Henyey-Greenstein phase function
```glsl
float henyeyGreenstein(float cosTheta, float g) {
    return 1.0 / (4.0 * PI) * (1.0 - g * g) / pow(1.0 + g * g - 2.0 * g * cosTheta, 1.5);
}
```

### D16. Atmospheric light colour from a transmittance LUT
```glsl
float sunElevation = acos(clamp(sunDirection.y, 0.0, 0.9999));
lightColor = texture(TransmittanceSampler, vec2(1.0 - degrees(sunElevation) / 90.0, 0.0)).rgb;
```
A 1280×1 texture indexed by elevation replaces a whole atmospheric scattering model.

### D17. Voxel flood-fill coloured lighting
`postfx-cookbook.md § 8`. Two 6-neighbour average iterations per frame, 1 % decay, trilinear
lookup, added in inverse-tonemapped space.

### D18. Light intensity from colour and level
```glsl
vec3 lightColor = decodeYCoCg776(color.rgb, lightLevel);
lightColor *= lightColor * (float(lightLevel * lightLevel) + 0.5);
```
Squaring both terms gives a perceptually right falloff without a physical model.

### D19. Box raytracing with transparent fall-through
`mesh-tricks.md § 4`. Slab intersection, skin UV tables, continue to the back face when the
front texel is transparent.

### D20. Supersampling from the UV derivative
```glsl
vec2 pixel = vec2(dFdx(uv.x), dFdy(uv.y));
sample(uv + pixel * vec2(-0.33, -0.33), …);
sample(uv + pixel * vec2( 0.33, -0.33), …);
sample(uv + pixel * vec2( 0.33,  0.33), …);
sample(uv + pixel * vec2(-0.33,  0.33), …);
```
Gives resolution-independent 4x AA in a fragment shader that has no idea how big it is on
screen.

### D21. Inline blue noise as a `const` array
When you cannot bind a noise texture (core shaders can't add samplers on G3+):
```glsl
const vec3 blueNoise[] = vec3[]( /* 256 entries */ );
vec3 random(inout float v) {
    ivec2 c = ivec2(mod(gl_FragCoord.xy + vec2(cos(v*2.399963), sin(v*2.399963)) * (v += 1.0), 16.0));
    return mod(blueNoise[(c.y * 16 + c.x) % 256], 1.0);
}
```
`2.399963` is the golden angle — rotating the lookup by it decorrelates successive samples.

### D22. Cosine-weighted hemisphere sampling
```glsl
vec3 cosineSampleHemisphere(vec3 n, inout float seed) {
    vec2 u = random(seed).xy;
    float r = sqrt(u.x), theta = TAU * u.y;
    vec3 b = normalize(cross(n, vec3(0.0, 1.0, 1.0)));
    vec3 t = cross(b, n);
    return normalize(r * sin(theta) * b + sqrt(1.0 - u.x) * n + r * cos(theta) * t);
}
```

### D23. Hash-based PRNG with IEEE bit construction
```glsl
uint hash(uint x) { x += x<<10u; x ^= x>>6u; x += x<<3u; x ^= x>>11u; x += x<<15u; return x; }
float floatConstruct(uint m) {
    m &= 0x007FFFFFu; m |= 0x3F800000u;      // mantissa of 1.0..2.0
    return fract(uintBitsToFloat(m) - 1.0);
}
```
Stateful wrapper (`advancePRNG`) so a fragment can draw a stream of numbers.

### D24. 3-D colour LUT in a 2-D texture, two layouts
```glsl
// (a) 16x16 tiles of 256x256 — exact, texelFetch, no interpolation needed
ivec3 rgb = ivec3(color.rgb * 255.0);
color.rgb = texelFetch(Lut1Sampler, ivec2((rgb.b % 16) * 256 + rgb.r,
                                          (rgb.b / 16) * 256 + rgb.g), 0).rgb;

// (b) 32-slice strip — smaller, needs manual slice interpolation
float x = color.g * (31.0/1024.0) + 0.5/1024.0;
float y = color.b * (31.0/32.0)   + 0.5/32.0;
float z1 = floor(color.r * 31.0) / 32.0, z2 = ceil(color.r * 31.0) / 32.0;
color.rgb = mix(texture(Lut2Sampler, vec2(x + z1, y)).rgb,
                texture(Lut2Sampler, vec2(x + z2, y)).rgb,
                (z2 == z1) ? 0.0 : (color.r * 31.0/32.0 - z1) / (z2 - z1));
```
Colour grading with no maths in the shader at all.

### D25. ACES, two ways
Curve fit (cheap, invertible) and the fitted RRT+ODT matrices (accurate). Both in
`encodings.md § 7`; the matrix form is in.
### D26. Reinhard with an inverse guard
```glsl
vec3 TMO(vec3 x)        { return x / (1.0 + x); }
vec3 InverseTMO(vec3 x) { return x / (1.00001 - x); }
```
The `1.00001` is what stops the inverse exploding at white.

### D27. Outer glow = blur minus original silhouette
```glsl
float cov = max(blur.a - origA, 0.0);
float glowA = clamp(smoothstep(GLOW_EDGE, GLOW_SOFT, cov) * GLOW_INTENSITY, 0.0, 1.0);
vec3 color = clamp(blur.rgb / max(blur.a, COLOR_FLOOR), 0.0, 1.0) * GLOW_TINT;
```
Dividing by `blur.a` un-premultiplies, so the glow keeps the entity's colour instead of
fading to grey..
### D28. Separable blur with the radius as a per-pass uniform
One shader, two directions, and the shader can tell which pass it is in:
```glsl
bool isFinal = sampleStep.y != 0.0;      // vertical = second pass
```

### D29. Doubling-radius blur chain
`Direction` = (0,1),(1,0),(0,2),(2,0),(0,4),(4,0),(0,8),(8,0) approximates a very wide
Gaussian in 8 cheap 7-tap passes..
### D30. Tent-weighted blur with progressive desaturation
```glsl
for (float r = -radius; r <= radius; r += 1.0) { float weight = radius - abs(r); … }
fragColor = mix(color, vec4(vec3(grayscale(color.rgb)), 1.0), radius * 0.25);
```
For a pause menu: as the blur grows, the world desaturates. Cheap, very effective.

### D31. Screen-space edge detection from UV derivatives
Resolution- and distance-independent outlines:
```glsl
vec2 sx = dFdx(uv), sy = dFdy(uv);
if (!isTagged(uv + sx * 2.0)) edge = max(edge, 1.0);
if (!isTagged(uv + (sx + sy) * 2.0)) edge = max(edge, 0.72);   // diagonals weigh less
```
Contrast with texture-space edge detection (fixed texel offsets), which changes thickness
with distance.

### D32. Signed distance field from a 3×3 texel neighbourhood
```glsl
vec3 textSdf() {
    vec3 value = vec3(0.0, 0.0, 1.0);
    for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) {
        vec2 uv = textData.uv + vec2(x, y) / 256.0;
        if (uvBoundsCheck(uv, textData.uvMin, textData.uvMax)) continue;
        if (texture(Sampler0, uv).a >= 0.1) {
            vec3 v = vec3(fract(uv * 256.0), 0.0);
            if (x == 0) v.x = 0.0;  if (y == 0) v.y = 0.0;
            if (x > 0)  v.x = 1.0 - v.x;  if (y > 0) v.y = 1.0 - v.y;
            v.z = length(v.xy);
            if (v.z < value.z) value = v;
        }
    }
    return value;
}
```
Turns a 1-bit bitmap font into a distance field at runtime, for outlines and glows.

### D33. Manual bilinear with atlas bounds clamping and mip blending
The correct way to filter an atlased sprite without bleeding into its neighbour:
```glsl
float mipLevel = textureQueryLOD(samp, texCoord).x;
if (mipLevel >= 1.0) return textureLod(samp, texCoord, mipLevel);
texCoord = clamp(texCoord * texSize - 0.5,
                 min(b1, min(b2, b3)) * texSize,
                 max(b1, max(b2, b3)) * texSize - 1.0);
/* 4 texelFetch + mix */
if (mipLevel > 0.0) color = mix(color, textureLod(samp, originalTexCoord, 1.0), mipLevel);
```
Bounds come from the per-corner varying trick (B3)..
### D34. Vanilla's transparency compositing (insertion-sorted layers)
If you rewrite the `transparency` pass you must reproduce this:
```glsl
void try_insert(vec4 color, float depth) {
    if (color.a == 0.0) return;
    color_layers[active_layers] = color;
    depth_layers[active_layers] = depth;
    int jj = active_layers++, ii = jj - 1;
    while (jj > 0 && depth_layers[jj] > depth_layers[ii]) { /* swap ii, jj */ jj = ii--; }
}
```
Six layers: main, translucent, itemEntity, particles, clouds, weather. Back-to-front blend
afterwards.

### D35. Merged depth across all transparency layers
```glsl
float depth = 1.0;
depth = min(depth, texture(DepthSampler0, texCoord).r);   /* …through DepthSampler5… */
```
Anything that needs "the depth of what the player actually sees" needs this.

### D36. Render only where the sky is
```glsl
fragColor = (texture(DepthSampler, texCoord).r == 1.0 && !isShadowmap)
          ? texture(DiffuseSampler, texCoord) : texture(SavedSampler, texCoord);
```
Keeps a saved frame everywhere except the sky.

### D37. Voxel-pixel repair by nearest-depth neighbour
Data pixels written into `minecraft:main` are replaced by whichever neighbour is closest to
the camera:
```glsl
if (int(round(texture(DataSampler, texCoord).a * 255.0)) != 100) return;
/* compare left/up/down depths, take the smallest */
```
Better than a 4-neighbour average when the marker sits on a silhouette..
### D38. Sample offset to dodge your own data
When a pass would sample a pixel it wrote data into, nudge one texel:
```glsl
if (int(round(data.a * 255.0)) == 100) texCoord0.x += 1.0 / DataSize.x;
if (int(gl_FragCoord.y) == 0)          texCoord0.y += 1.0 / DataSize.y;
```
Pairs with the checkerboard (B8), which guarantees the neighbour is not also data.

### D39. View ray reconstruction inside a **core** shader
Normally a post-shader operation. With only `ScreenSize`, `ProjMat` and `ModelViewMat`:
```glsl
mat4 projMat = ProjMat;
projMat[3].xy = vec2(0.0);                    // remove view bobbing / TAA jitter
vec4 ndcPos  = vec4(gl_FragCoord.xy / ScreenSize * 2.0 - 1.0, 0.0, 1.0);
vec4 temp    = inverse(projMat) * ndcPos;
vec3 viewPos = temp.xyz / temp.w;
vec3 rayDir  = normalize(viewPos * mat3(ModelViewMat));   // transpose = inverse rotation
```
Unlocks raytracing from any hijacked fullscreen quad, with no post pipeline and therefore no
Fabulous requirement.

### D40. Cubemap sampling from a 3×2 cross layout
Tedious to get right; here it is complete.
```glsl
vec2 convertToCubemapUV(vec3 direction) {
    float l = max(max(abs(direction.x), abs(direction.y)), abs(direction.z));
    vec3 dir = direction / l;
    vec3 absDir = abs(dir);

    if (absDir.x >= absDir.y && absDir.x > absDir.z) {
        if (dir.x < 0) return vec2(0, 0.5)       + ( dir.zy * vec2(-1, -1) + 1) / 2 / vec2(3, 2);
        else           return vec2(2.0 / 3, 0.5) + (-dir.zy * vec2(-1,  1) + 1) / 2 / vec2(3, 2);
    } else if (absDir.y >= absDir.z) {
        if (dir.y > 0) return vec2(1.0 / 3, 0)   + ( dir.xz * vec2( 1, -1) + 1) / 2 / vec2(3, 2);
        else           return vec2(0, 0)         + (-dir.xz * vec2(-1, -1) + 1) / 2 / vec2(3, 2);
    } else {
        if (dir.z < 0) return vec2(1.0 / 3, 0.5) + (-dir.xy * vec2(-1,  1) + 1) / 2 / vec2(3, 2);
        else           return vec2(2.0 / 3, 0)   + ( dir.xy * vec2(-1, -1) + 1) / 2 / vec2(3, 2);
    }
}
```
Matches the OptiFine custom-sky layout, so existing skybox art works unmodified. Sample with
`texelFetch(base + ivec2(cubemapSize * uv), 0)` to avoid seam bleeding.

### D41. Ray–plane intersection to redraw geometry you hijacked
```glsl
float rayPlane(vec3 rayOrigin, vec3 rayDir, vec3 point, vec3 normal) {
    return dot(point - rayOrigin, normal) / dot(rayDir, normal);
}
```
and recovering the quad's UV at the hit by projecting onto its two edge vectors:
```glsl
vec3 hitPos = rayDir * t;
vec3 sideX = pos3 - pos2, sideY = pos1 - pos2;
vec2 uv = vec2(dot(hitPos - pos2, sideX) / dot(sideX, sideX),
               dot(hitPos - pos2, sideY) / dot(sideY, sideY));
if (clamp(uv, 0.0, 1.0) == uv) { /* inside the original quad */ }
```
The moon is the same plane mirrored: `normal *= -1; center *= -1;` and negate the edge
vectors. This is how a pack can steal the sun's geometry for a fullscreen effect and still
draw a correct sun and moon.

### D42. Cyclic keyframe interpolation with wrap-around
The general pattern for "a list of timestamped states on a looping clock":
```glsl
int currentIndex = -1;
float startTime;
for (int i = count - 1; i >= 0; i--) {
    startTime = keyTime(i);
    if (currentTime > startTime) { currentIndex = i; break; }
}
float endTime;
if (currentIndex == -1) {                 // before the first entry: use the last, shifted back
    currentIndex = count - 1;
    startTime = keyTime(currentIndex);
    endTime   = keyEnd(currentIndex);
    if (endTime > startTime) endTime -= 1.0;
    startTime -= 1.0;
} else {
    endTime = keyEnd(currentIndex);
}
float f = clamp((currentTime - startTime) / (endTime - startTime), 0.0, 1.0);
int previousIndex = (currentIndex - 1 + count) % count;
result = mix(value(previousIndex), value(currentIndex), f);
```
The `-= 1.0` shift is the part people get wrong: it moves the last entry's window into
negative time so the interpolation across midnight is continuous.

### D43. Faux ambient by averaging the environment
A solid object drawn over a skybox looks like a sticker unless it picks up surrounding
light. Average the environment around the hit point:
```glsl
vec3 ambientColor = vec3(0.0);
for (int x = -5; x <= 5; x++) for (int y = -5; y <= 5; y++) {
    vec3 p = hitPos + sideX * x * 0.3 / 5.0 + sideY * y * 0.3 / 5.0;
    ambientColor += sampleEnvironment(normalize(p));
}
ambientColor /= 11.0 * 11.0;
```
121 taps, but only over the moon's few hundred pixels. Expose it as a config `const bool` so
users can switch to flat black.

---

# E. Text and UI

### E1. 5×6 bitmap font packed into `uint` constants
`gpu-ui.md § 1`, with an immediate-mode cursor API (`c()`, `d()`, `f()`, `nl()`).

### E2. Seven-segment digits with lit and ghost segments
`gpu-ui.md § 2`.

### E3. Virtual-pixel panel layout with integer DPI scaling
`gpu-ui.md § 3`. `floor(OutSize.y / 540.0)` clamped 1–4.

### E4. `floor(x / PITCH)` + `x - i * PITCH` for rows and columns
Replaces a chain of `if` per element with two arithmetic ops.

### E5. Screen size recovered from a clip-space varying
```glsl
ivec2 screenSize = ivec2(gl_FragCoord.xy / (glPos.xy / glPos.w * 0.5 + 0.5));
```
Works when `ScreenSize` is unavailable.

### E6. GUI extent in "gui pixels" from the ortho matrix
```glsl
vec2 ui = ceil(2.0 / vec2(ProjMat[0][0], -ProjMat[1][1]));
```

### E7. Text shadow detection by fractional Z
```glsl
bool isShadow = mod(Position.z, 1.0) < 0.01;
```
Vanilla draws the shadow copy at a fractional z offset. Lets you style or remove shadows
per effect.

### E8. Text effects as UV manipulation before the sample
The whole category — waves, shakes, outlines, gradients, shimmer, aberration — reduces to one
idea: **move or duplicate the UV before sampling the glyph atlas, and keep three colour slots
for the result.** Once the glyph's screen rect and UV rect are recovered (B3), every effect is
a few lines.

Coordinates each effect needs:
- `charPos` — the glyph's centre in text-space pixels. Drives per-character phase (waves).
- `localPos` — position within the glyph, 0..1. Drives within-glyph gradients and shimmer.
- `uv`, `uvMin`, `uvMax` — the atlas rect, for bounds-checked neighbour sampling.

Representative implementations:
```glsl
// per-character vertical wave: phase from the glyph's own x position
void waveEffect(inout Style st, float speed, float freq) {
    st.uv.y += sin(st.charPos.x * 0.1 * freq - GameTime * 7500.0 * speed) / ATLAS_SIZE;
    st.expand = true;                       // needs room outside the glyph
}

// a moving highlight band across the glyph
void shimmerEffect(inout Style st, float speed, float intensity) {
    if (st.isShadow) return;
    float band = st.localPos.x + st.localPos.y - GameTime * 6400.0 * speed;
    if (mod(band, 5.0) < 0.75) st.over = vec4(1.0, 1.0, 1.0, intensity);
}

// row-banded shading, so a flat glyph reads as embossed metal
void metallicEffect(inout Style st, vec3 base) {
    int row = int(floor((st.uv.y - st.uvMin.y) * ATLAS_SIZE));
    st.color.rgb = row  > 3 ? base * 0.7
                 : row == 3 ? base + 0.25
                            : base;
}

// outline: if any neighbouring texel is opaque, paint behind
void outlineEffect(inout Style st, vec3 color) {
    st.expand = true;
    vec2 texel = 1.0 / vec2(ATLAS_SIZE);
    for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
        if (x == 0 && y == 0) continue;
        vec2 uv = st.uv + vec2(x, y) * texel;
        if (outsideRect(uv, st.uvMin, st.uvMax)) continue;
        if (texture(Sampler0, uv).a >= 0.1) { st.behind = vec4(color, 1.0); return; }
    }
}
```
Every effect that moves the UV outside the glyph must set `expand`, so the vertex shader
inflates the quad and the fragment shader clips with a bounds test (B5). Without that, the
effect is silently cropped to the original glyph rectangle.

### E9. Three colour slots per glyph
`color` (the glyph itself), `behind` (outlines, aberration, shadows) and `over` (shimmer,
highlights). Compositing order matters:
```glsl
fragColor = mix(vec4(st.behind.rgb, st.behind.a * st.color.a),
                glyphSample * st.color, glyphSample.a);
fragColor.rgb = mix(fragColor.rgb, st.over.rgb, st.over.a);
```
Two slots are not enough: an outline must go behind the glyph and a highlight in front, and
both can be active at once.

### E10. Animated glyphs from a sprite sheet
`gpu-ui.md § 7`, with a 1-pixel border per frame to stop bilinear bleeding.

### E11. Damped-oscillation entrance
```glsl
scale *= 1.0 - (pow(0.03, entranceT) * cos(TAU * 2.0 * entranceT));
```
Reads as a spring; two constants, no state.

### E12. Envelope from a single 0–1 clock
```glsl
alpha = clamp(min(8.0 * t, 4.0 - 4.0 * t), 0.0, 1.0);
alpha = 1.0 - (1.0 - alpha) * (1.0 - alpha);      // ease-out
```
Fade in over the first 1/8, hold, fade out over the last quarter — from one parameter.

### E13. Sub-phase extraction
```glsl
float fallT  = max((t - 0.75) / 0.25, 0.0);
float shakeT = clamp((t - 0.6) / 0.15, 0.0, 1.0);
```
Slicing one timeline into overlapping named phases, each remapped to 0–1.

### E14. Easing library
Linear, quad in/out, smoothstep, cubic in-out, elastic, back — all in `gpu-ui.md § 6`, plus
`easeElastic`, `springShake`, `easeInOutCirc`, `easeInOutSine` in.
### E15. Rounded/circular widgets with squared distance
`gpu-ui.md § 5`.

### E16. "No signal" states
A decoded UI must be able to say it has no data (blank the digits, grey the swatch) instead
of showing garbage.

---

# F. Numerics and robustness

### F1. Fixed-point in 24 bits, with a precision tier per field
`encodings.md § 1`. HIGH (400000) for matrix elements, LOW (1000) for world offsets.

### F2. `atan`/`tan` to squeeze an unbounded value into range
`ProjMat[0][0]` can exceed the fixed-point range at low FOV; store its arctangent.

### F3. Zero the TAA jitter before reprojecting
`projection[3][0] = projection[3][1] = 0.0;` — or store the un-jittered values separately
and patch them back (`decodeUnjitteredProjection`).

### F4. Exact float round trip for anything compared for equality
`packF32toF8x4`. Fixed point will fail an `==`.

### F5. LogLuv for HDR in RGBA8, with manual filtering
`encodings.md § 3`. Never let the hardware filter a LogLuv target.

### F6. Precompute your sentinel, don't guess it
`encodeLogLuv(vec3(0.0))` is not `vec4(0)`. Compute the sentinel in the vertex shader, pass
it as a varying, and compare against that.

### F7. YCoCg for bit stealing
7+7+6 bits of colour plus 4 spare bits in three bytes (`encodings.md § 4`).

### F8. Byte offset to dodge reserved palette entries
Write 7 bits per byte and add 4 so no byte lands in the transparent map-colour range.

### F9. Floor, not truncate, for negative voxel coordinates
```glsl
ivec3 voxelPos = ivec3(fragPos);
voxelPos -= ivec3(lessThan(fragPos, vec3(0.0)));
```
`int(-0.5) == 0`, which puts two adjacent voxels in the same cell across the origin.

### F10. Modular-time wrap guard
```glsl
float elapsed = mod(GameTime - birthGT + 1.0, 1.0);
if (elapsed > 0.5) elapsed = 0.0;
```
Without it, anything born near midnight shows a stuck progress of 1.0.

### F11. Wrapped delta for a counter that resets
```glsl
if (deltaTime < prevTime) deltaTime += 1.0;    // Time wraps 0..1 each second
deltaTime -= prevTime;
```

### F12. Clamp before an inverse tone map
`acesInverse` goes imaginary near 0 and 1; the NaN then propagates through every blur pass
and blackens the screen.

### F13. Dither before quantising
Bayer 4x4 or blue noise, `+= dither / 256.0`. Adding a dither to the *vertex colour*
(`color *= vertexColor + dither`) also kills banding in flat-lit faces.

### F14. Exact integer comparison for every magic value
`ivec4(round(x * 255.0)) == ivec4(...)`. Never `==` on floats.

### F15. Two independent validations before trusting decoded data
A magic word *and* a per-pixel tag, or a magic *and* a range check.

### F16. Epsilon UV nudge to break coincident-face ties
```glsl
texCoord += vec2(onepixel.x * 0.0001 * corner, onepixel.y * 0.0001 * ((corner + 1) % 4));
```
Needed for faces that share identical UV bounds.

### F17. Rasteriser fill-rule slack
```glsl
vec2 span = Rect.zw + vec2(2.0);   // +2px so the far corner is strictly inside
```
Without it the last row/column of a pixel-exact rectangle is dropped.

### F18. Half-texel discipline
`texelFetch` indexes texel centres, `texture` samples at UV. Converting needs the `-0.5`:
```glsl
vec2 uv = screenSpace * OutSize - 0.5;
ivec2 c = ivec2(floor(uv));
vec2 f = uv - c;
```

---

# G. Structure and process

### G1. Feature library with `make_<feature>()` entry points
`new-projects.md § 3`.

### G2. Generate bulk assets, commit the generator
Sprite sheets, palettes, mesh textures and per-block models all need their generator committed
beside them. 1900 block models are not hand-written.

### G3. Version overlays plus stub includes
`version-matrix.md § 7`.

### G4. Point one render type at a custom shader file instead of overriding a shared one
`"vertex": "minecraft:text/hand"` (G1/G2 only). Isolates your change to one render type.

### G5. Document the magic-value allocation in a header comment
The everything-packs are one collision away from a very confusing bug.

### G6. A legacy fallback path for hostile drivers
Ambitious packs ship an entire simplified pass chain and a separate vendor-specific build.
Sometimes the answer to a driver bug is a second, simpler implementation.

### G7. Declare `sodium.ignored_shaders`, and be honest when Sodium wins
`version-matrix.md § 8`.

### G8. State the graphics-settings requirement in the pack description
Fabulous is not optional for post pipelines on G1–G2.

### G9. Deletion as a feature — one file per removal
When your effect requires suppressing vanilla behaviour, ship each suppression as its own
file and document that **deleting the file reverts it**:
```
core/stars.vsh          -> delete to restore stars
core/position_color.*   -> delete to restore the sunset horizon band
include/fog.glsl        -> delete to restore fog
include/config.glsl     -> const bool switches for the rest
```
This turns a monolithic pack into something users can compose. Costs nothing; enormously
improves adoption.

### G10. A `config.glsl` of `const bool` switches
```glsl
// Set to "false" if you want the moon to not use the average sky colour behind it
const bool AVERAGE_MOON_LIGHTING = true;
```
`const bool` (not `#define`) reads better for end users and still folds away at compile time.
Pair with a `settings:` namespace (C4) when there are many.

### G11. State the licence terms in the README
Say what people may do with your pack, and check the terms of anything you incorporate.
Techniques are free to learn from and reimplement; a block of someone else's GLSL is not.


---

# H. Ideas that follow from the above but are not in shipping packs

Composition candidates, with what they would need.

- **Post-effect bit bus (A20) + GPU UI (E1–E3)** → a fully server-driven HUD with zero
  entities and one-packet latency. Needs 26.2+.
- **Temporal reprojection (D5) applied to the voxel light buffer (D17)** → coloured GI that
  converges over many frames instead of two flood-fill iterations.
- **objmc (mesh-tricks § 3) + box raytracing (D19)** → arbitrary meshes raytraced in a GUI
  slot, by pointing the raytracer at objmc's texture-encoded vertex data.
- **The case-label macro DSL (C1) applied to block or entity behaviour tables** → an
  editable config file instead of a generated `switch`.
- **Data marker (A1) carrying a small scene description** — a list of lights, portals or
  shapes assembled by the core shader from entity positions. A general GPU scene graph for
  post shaders.
- **Depth-as-reduction (B6) for other queries** — nearest entity, brightest light, closest
  waypoint: encode the key in NDC z and let the depth test do the argmin.
- **Map bitstream (A18) decoded into a post target** rather than a quad → full-screen
  server-driven UI that can then be blurred, shadowed, and transitioned by later passes.
- **A6's "vanilla texture as sensor" applied elsewhere** — the sky texture, the moon phase
  texture, the enchantment glint, each carries client state a shader could read back.
