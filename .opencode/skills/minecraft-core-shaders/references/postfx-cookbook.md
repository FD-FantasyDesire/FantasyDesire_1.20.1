# Post-effect cookbook

Recipes for post-processing pipelines. Each section gives the target/pass graph and the
kernels. JSON is shown in G1 form (`targets` array) or G2+ form (`targets` object); translate
with `version-matrix.md`.

Read `§0` first; it applies to all of them.

---

## 0. Ground rules for every post pipeline

**Which pipeline to hijack.**

| File | Runs when | Notes |
|---|---|---|
| `post/transparency.json` (G1) / `post_effect/transparency.json` (G2+) | Graphics = **Fabulous!** only | The workhorse. Anything added here silently does nothing on Fancy/Fast. |
| `post/entity_outline.json` / `post_effect/entity_outline.json` | always, when something glows | Cheap, always-on hook. |
| `post/blur.json` | menu background blur | Menus only. |
| A named custom effect (26.2+) | when the server enables it | See `data-channels.md § 10`. |

**Rebuilding `transparency`.** If you replace the pipeline you must keep the stock
`transparency` pass and re-feed all of its aux samplers (list in `version-matrix.md § 1`), or
translucency compositing breaks. Usual shape:

```
minecraft:main ──► [your passes] ──► main' ──► transparency ──► swap ──► blit ──► minecraft:main
```

**Targets are not cleared.** Every declared target holds last frame's contents. A resource
(`§4`) and a hazard (validate before trusting).

**Order is literal.** Passes run top to bottom, exactly once. No loops — unroll them.

**You cannot read and write the same target in one pass.** Bounce through scratch.

**Reserve row 0** if a data marker lives there. Standard guard:
```glsl
if (int(floor(gl_FragCoord.y)) == 0) {
    fragColor = texelFetch(DiffuseSampler, ivec2(gl_FragCoord.xy), 0);
    return;
}
```
and clamp anything that samples an arbitrary UV:
```glsl
float markerCutoff = 1.5 / InSize.y;
sampleUV.y = max(sampleUV.y, markerCutoff);
```

---

## 1. Reconstructing world space from depth

Everything below needs this. The matrices arrive via a data marker
(`data-channels.md § 1`); decode them in the **vertex** shader and pass them `flat` — four
executions per pass instead of two million.

```glsl
// vertex
flat out mat4 viewProj;
flat out mat4 invViewProj;

void main() {
    mat4 proj = decodeMatrix(DiffuseSampler, 0);    // your marker layout
    mat4 view = decodeMatrix(DiffuseSampler, 18);
    viewProj    = proj * view;
    invViewProj = inverse(viewProj);
    gl_Position = screenquad[gl_VertexID];
    texCoord = gl_Position.xy * 0.5 + 0.5;
}

// fragment
vec3 positionFromDepth(vec2 uv, float depth) {
    vec4 ndc = vec4(uv, depth, 1.0) * 2.0 - 1.0;    // the *1.0 in w becomes 1.0 too
    vec4 p = invViewProj * ndc;
    return p.xyz / p.w;
}
```

Near/far and depth linearisation. Vanilla's near plane is 0.05; far is recoverable from the
projection matrix:
```glsl
vec2 depthPlanes(mat4 proj) {
    const float near = 0.05;
    float far = proj[3][2] * near / (proj[3][2] + 2.0 * near);
    return vec2(near, far);
}
float linearDepth(float ndcZ, vec2 planes) {        // ndcZ in [-1,1] -> [near,far]
    return 2.0 * planes.x * planes.y
         / (planes.y + planes.x - ndcZ * (planes.y - planes.x));
}
float ndcDepth(float linear, vec2 planes) {
    return (2.0 * planes.x * planes.y / linear - planes.y - planes.x)
         / (planes.x - planes.y);
}
```

### Normals from depth

There is no G-buffer, so derive normals from the depth derivative. Naive
`cross(dFdx, dFdy)` halos at silhouettes. Two better estimators:

**Closest-neighbour.** For each axis, use whichever neighbour's depth is closer to the
centre, and track the sign so the cross product keeps its orientation.
```glsl
vec3 normalFromDepth(sampler2D depthTex, vec2 uv, vec2 texel) {
    float dC = texture(depthTex, uv).r;
    if (dC == 1.0) return vec3(0.0);                       // sky

    vec2 uvR = uv + vec2( texel.x, 0.0), uvL = uv - vec2(texel.x, 0.0);
    vec2 uvU = uv + vec2(0.0, texel.y), uvD = uv - vec2(0.0, texel.y);
    float dR = texture(depthTex, uvR).r, dL = texture(depthTex, uvL).r;
    float dU = texture(depthTex, uvU).r, dD = texture(depthTex, uvD).r;

    vec3 pC = positionFromDepth(uv, dC);
    float sign = 1.0;

    vec3 pH;
    if (abs(dR - dC) < abs(dL - dC)) pH = positionFromDepth(uvR, dR);
    else                           { pH = positionFromDepth(uvL, dL); sign = -sign; }

    vec3 pV;
    if (abs(dU - dC) < abs(dD - dC)) { pV = positionFromDepth(uvU, dU); sign = -sign; }
    else                               pV = positionFromDepth(uvD, dD);

    return sign * normalize(cross(pV - pC, pH - pC));
}
```

**Second-derivative (more stable on slopes).** Sample at ±2 and ±4 texels; pick the side
whose linear extrapolation `2*d(±2) - d(±4)` best predicts the centre depth. Costs four extra
taps and removes most of the remaining slope artefacts.
```glsl
vec4 h = vec4(texture(depthTex, uv - vec2(2,0)*texel).r, texture(depthTex, uv + vec2(2,0)*texel).r,
              texture(depthTex, uv - vec2(4,0)*texel).r, texture(depthTex, uv + vec2(4,0)*texel).r);
vec2 hErr = abs((2.0 * h.xy - h.zw) - dC);
vec3 pH = (hErr.x < hErr.y) ? pC - positionFromDepth(uv - vec2(2,0)*texel, h.x)
                            : positionFromDepth(uv + vec2(2,0)*texel, h.y) - pC;
// same for vertical, then normalize(cross(pH, pV))
```

Store as `normal * 0.5 + 0.5` in RGB8.

---

## 2. SSAO with temporal accumulation

Pass graph:
```
normals    : main(+depth)                                        -> normals
ssao       : main(+depth, normals, prevAo, noise)                 -> ao
temporal   : ao(+prevAo, depth, prevDepth, normals, prevNormals)  -> temporal
apply_ao   : main(+depth, normals, temporal)                      -> main
blit       : temporal -> prevAo
blit       : normals  -> prevNormals
copy_depth : main(+depth) -> prevDepth      (packed lossless, see encodings.md § 2)
transparency ... blit -> minecraft:main
```

### The occlusion kernel

Hemisphere sampling oriented by a TBN built from the reconstructed normal and a per-pixel
random vector. Generate the sample set offline (points inside a unit hemisphere, biased
toward the origin so near-field occlusion dominates) and paste it as a `const` array:

```glsl
const int   AO_SAMPLES = 16;
const float AO_RADIUS  = 1.0;
const vec3  AO_KERNEL[AO_SAMPLES] = vec3[]( /* your generated hemisphere points */ );

float ssao(vec2 uv, float depth, vec3 normal, float frameIndex) {
    vec3 origin = positionFromDepth(uv, depth);

    vec3 rnd       = vec3(noise2(uv, frameIndex) * 2.0 - 1.0, 0.0);
    vec3 tangent   = normalize(rnd - normal * dot(rnd, normal));
    vec3 bitangent = cross(normal, tangent);
    mat3 tbn       = mat3(tangent, bitangent, normal);

    float occluded = 0.0;
    for (int i = 0; i < AO_SAMPLES; i++) {
        vec3 samplePos = origin + (tbn * AO_KERNEL[i]) * AO_RADIUS;

        vec4 clip = viewProj * vec4(samplePos, 1.0);
        vec3 proj = clip.xyz / clip.w * 0.5 + 0.5;
        proj.y = max(proj.y, 1.5 / InSize.y);              // stay off the marker row

        float sceneDepth = texture(DiffuseDepthSampler, proj.xy).r;
        if (sceneDepth == 1.0) continue;                    // sky occludes nothing

        float sceneZ = positionFromDepth(proj.xy, sceneDepth).z;
        // range check: geometry far behind the sample plane must not occlude it
        float falloff = smoothstep(0.0, 1.0, AO_RADIUS / abs(origin.z - sceneZ));
        occluded += (sceneZ >= samplePos.z + 0.05 ? 1.0 : 0.0) * falloff;
    }
    return 1.0 - occluded / float(AO_SAMPLES);
}
```

`frameIndex` is a 0–3 counter kept in one pixel of the previous AO target (`§4`), used to
scroll the noise lookup so successive frames sample different directions. Without it the
temporal pass averages the same pattern forever and never converges.

### Temporal reprojection

The generic pattern. Reuse it for any accumulation buffer.

```glsl
void main() {
    // 1. advance the frame counter in a dedicated pixel
    if (ivec2(gl_FragCoord.xy) == COUNTER_PIXEL) {
        float prev = texelFetch(PreviousDiffuseSampler, COUNTER_PIXEL, 0).r * 255.0;
        fragColor = vec4(mod(prev + 1.0, 4.0) / 255.0, 0.0, 0.0, 1.0);
        return;
    }
    // 2. preserve the marker row
    if (int(floor(gl_FragCoord.y)) == 0) {
        fragColor = texelFetch(DiffuseSampler, ivec2(gl_FragCoord.xy), 0);
        return;
    }

    vec3 current = texture(DiffuseSampler, texCoord).rgb;
    fragColor = vec4(current, 1.0);                 // default: reject history

    float depth = texture(DiffuseDepthSampler, texCoord).r;
    if (depth == 1.0) return;

    // 3. camera motion (see spaces-and-uniforms.md § 3)
    vec3 motion = mod(camPos - prevCamPos + 8.0, 16.0) - 8.0;

    // 4. reproject into last frame's screen space
    vec3 worldPos = positionFromDepth(texCoord, depth);
    vec4 prevClip = prevViewProj * vec4(worldPos - motion, 1.0);
    vec3 prevScreen = prevClip.xyz / prevClip.w * 0.5 + 0.5;
    if (clamp(prevScreen.xy, 1.0 / OutSize, 1.0 - 1.0 / OutSize) != prevScreen.xy) return;

    // 5. reject on surface mismatch
    vec3 n     = texture(NormalSampler,         texCoord).rgb * 2.0 - 1.0;
    vec3 prevN = texture(PreviousNormalSampler, prevScreen.xy).rgb * 2.0 - 1.0;
    if (dot(n, prevN) < 0.7) return;

    // 6. reject on depth mismatch (history depth stored losslessly)
    float prevDepth = unpackF32(texture(PreviousDepthSampler, prevScreen.xy));
    if (abs(prevScreen.z - prevDepth) > 0.001 * prevScreen.z) return;

    // 7. manual bilinear fetch — never trust hardware filtering across a reprojection
    vec2 t = prevScreen.xy * OutSize - 0.5;
    ivec2 c = ivec2(floor(t));
    vec2  f = t - vec2(c);
    vec3 history = mix(
        mix(texelFetch(PreviousDiffuseSampler, c,              0).rgb,
            texelFetch(PreviousDiffuseSampler, c + ivec2(1,0), 0).rgb, f.x),
        mix(texelFetch(PreviousDiffuseSampler, c + ivec2(0,1), 0).rgb,
            texelFetch(PreviousDiffuseSampler, c + ivec2(1,1), 0).rgb, f.x), f.y);

    fragColor = vec4(mix(history, current, 0.25), 1.0);    // ~4-frame exponential history
}
```

Every early `return` leaves the current frame's value — the correct behaviour on
disocclusion.

### Normal-aware blur, then apply

```glsl
float ao = texture(AmbientOcclusionSampler, texCoord).r;
float weight = 1.0;
vec3 nC = texture(NormalSampler, texCoord).rgb * 2.0 - 1.0;
for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
    if (x == 0 && y == 0) continue;
    ivec2 c = ivec2(gl_FragCoord.xy) + ivec2(x, y);
    if (dot(texelFetch(NormalSampler, c, 0).rgb * 2.0 - 1.0, nC) < 0.8) continue;  // keep edges
    ao += texelFetch(AmbientOcclusionSampler, c, 0).r;
    weight += 1.0;
}
ao /= weight;
```

Fade AO into fog or distant terrain darkens implausibly:
```glsl
float aoWithFog(float ao, float dist, float fogStart, float fogEnd) {
    if (dist <= fogStart) return ao;
    float v = dist < fogEnd ? 1.0 - smoothstep(fogStart, fogEnd, dist) : 0.0;
    return mix(1.0, ao, v * v);
}
```

---

## 3. Bloom with a mip pyramid

### Tagging emissive pixels

Emission strength rides in texture alpha (`data-channels.md § 5`). The core fragment shader
must **preserve** the tag into `minecraft:main` instead of overwriting alpha with 1.0:

```glsl
vec4 albedo = texture(Sampler0, texCoord0);
vec4 unmipped = textureLod(Sampler0, texCoord0, -4.0);   // tag survives mip blending
int tag = int(round(unmipped.a * 255.0));

#ifndef TRANSLUCENT
if (tag >= EMISSIVE_MIN && tag <= EMISSIVE_MAX) {
    albedo.a = unmipped.a;                                // carry the tag through
} else {
    albedo *= vertexColor * ColorModulator;
    albedo.a = 1.0;
}
#else
albedo *= vertexColor * ColorModulator;
#endif
fragColor = linear_fog(albedo, vertexDistance, FogStart, FogEnd, FogColor);
#ifndef TRANSLUCENT
fragColor.a = albedo.a;
#endif
```

Extraction pass:
```glsl
fragColor = encodeLogLuv(vec3(0.0));
if (texture(DepthSampler, texCoord).r == 1.0) return;     // skip sky
vec4 c = texture(InSampler, texCoord);
int tag = int(round(c.a * 255.0));
if (tag >= EMISSIVE_MIN && tag <= EMISSIVE_MAX)
    fragColor = encodeLogLuv(pow(c.rgb, vec3(2.2)) * float(tag - EMISSIVE_MIN + 1));
```

### A pyramid without mip levels

Post targets are all full-screen with one mip. Emulate a pyramid by rendering into
progressively smaller **sub-rectangles** of full-size targets, clipping the screen quad:

```glsl
// vertex, shared by downsample and upsample
uniform vec2 OutSize;
uniform float Iteration;                 // 1-based level, supplied per pass
flat out ivec2 srcRes;
flat out ivec2 dstRes;

void main() {
    gl_Position = screenquad[gl_VertexID];

    ivec2 level = ivec2(OutSize) >> int(Iteration - 1.0);
    srcRes = level;          dstRes = level / 2;      // downsample
    // upsample swaps them: srcRes = level / 2; dstRes = level;

    vec2 corner = vec2(dstRes) / OutSize * 2.0 - 1.0;  // clip the quad to the sub-rect
    if (gl_Position.x > 0.0) gl_Position.x = corner.x;
    if (gl_Position.y > 0.0) gl_Position.y = corner.y;
}
```

Because the source is LogLuv-encoded, filtering must be manual:
```glsl
vec3 fetchBilinear(sampler2D tex, vec2 uv, ivec2 res) {
    vec2 t = uv * vec2(res) - 0.5;
    ivec2 c = ivec2(floor(t));
    vec2  f = fract(t);
    return mix(mix(decodeLogLuv(texelFetch(tex, c,              0)),
                   decodeLogLuv(texelFetch(tex, c + ivec2(1,0), 0)), f.x),
               mix(decodeLogLuv(texelFetch(tex, c + ivec2(0,1), 0)),
                   decodeLogLuv(texelFetch(tex, c + ivec2(1,1), 0)), f.x), f.y);
}
```

**Downsample** — the 13-tap filter: one centre tap, a 3×3 ring at 2-texel spacing, and a 2×2
ring at 1-texel spacing, weighted 0.125 / 0.0625 / 0.03125 / 0.125. It is stable against the
fireflies that a naive box filter amplifies.
```glsl
ivec2 px = ivec2(gl_FragCoord.xy);
if (any(greaterThanEqual(px, dstRes))) { fragColor = vec4(0.0); return; }
vec2 uv = gl_FragCoord.xy / vec2(dstRes);
vec2 s  = 1.0 / vec2(srcRes);

vec3 centre = fetchBilinear(InSampler, uv, srcRes);
vec3 ring2 = vec3(0.0);                      // corners of the 2-texel ring
ring2 += fetchBilinear(InSampler, uv + vec2(-2,-2) * s, srcRes);
ring2 += fetchBilinear(InSampler, uv + vec2( 2,-2) * s, srcRes);
ring2 += fetchBilinear(InSampler, uv + vec2(-2, 2) * s, srcRes);
ring2 += fetchBilinear(InSampler, uv + vec2( 2, 2) * s, srcRes);
vec3 edge2 = vec3(0.0);                      // edges of the 2-texel ring
edge2 += fetchBilinear(InSampler, uv + vec2( 0,-2) * s, srcRes);
edge2 += fetchBilinear(InSampler, uv + vec2( 0, 2) * s, srcRes);
edge2 += fetchBilinear(InSampler, uv + vec2(-2, 0) * s, srcRes);
edge2 += fetchBilinear(InSampler, uv + vec2( 2, 0) * s, srcRes);
vec3 inner = vec3(0.0);                      // the 2x2 ring at 1-texel spacing
inner += fetchBilinear(InSampler, uv + vec2(-1,-1) * s, srcRes);
inner += fetchBilinear(InSampler, uv + vec2( 1,-1) * s, srcRes);
inner += fetchBilinear(InSampler, uv + vec2(-1, 1) * s, srcRes);
inner += fetchBilinear(InSampler, uv + vec2( 1, 1) * s, srcRes);

fragColor = encodeLogLuv(centre * 0.125 + ring2 * 0.03125 + edge2 * 0.0625 + inner * 0.125);
```

**Upsample** — a 3×3 tent (weights 1/2/1 per axis, i.e. 1/16 normalisation) plus the matching
downsample level, which is what makes it a progressive rather than a plain upscale:
```glsl
vec3 tent = vec3(0.0);
tent += fetchBilinear(InSampler, uv, srcRes) * 4.0;
tent += (fetchBilinear(InSampler, uv + vec2( 0, 1) * s, srcRes)
       + fetchBilinear(InSampler, uv + vec2( 0,-1) * s, srcRes)
       + fetchBilinear(InSampler, uv + vec2( 1, 0) * s, srcRes)
       + fetchBilinear(InSampler, uv + vec2(-1, 0) * s, srcRes)) * 2.0;
tent += fetchBilinear(InSampler, uv + vec2(-1,-1) * s, srcRes)
      + fetchBilinear(InSampler, uv + vec2( 1,-1) * s, srcRes)
      + fetchBilinear(InSampler, uv + vec2(-1, 1) * s, srcRes)
      + fetchBilinear(InSampler, uv + vec2( 1, 1) * s, srcRes);
tent /= 16.0;
tent += fetchBilinear(DownsampledSampler, uv * (vec2(dstRes) / vec2(srcRes)), dstRes);
fragColor = encodeLogLuv(tent);
```

Pipeline: 7 downsamples (`emissive → a → … → g`), then 6 upsamples back
(`g+f → f1`, `f1+e → e1`, …, `a1+emissive → bloom`), each pass overriding `Iteration`.

### Compositing

```glsl
const float BLOOM_STRENGTH = 0.05;
vec3 c = pow(texture(InSampler, texCoord).rgb, vec3(2.2));   // sRGB -> linear
c = acesInverse(c);                                          // display -> scene HDR
c = max(c, decodeLogLuv(texture(EmissiveSampler, texCoord)));
c = mix(c, decodeLogLuv(texture(BloomSampler, texCoord)), BLOOM_STRENGTH);
c = acesFilm(c);
fragColor = vec4(pow(c, vec3(1.0 / 2.2)), 1.0);
```

---

## 4. Cross-frame state and history buffers

Post targets are never cleared, so a private target is your only memory.

```json
"targets": [ "prevAo", "prevDepth", "prevNormals",
             { "name": "state", "width": 2, "height": 1 } ],
"passes": [
    { "name": "temporal", "intarget": "ao", "outtarget": "temporal",
      "auxtargets": [ { "name": "PreviousDiffuseSampler", "id": "prevAo" } ] },
    { "name": "blit", "intarget": "temporal", "outtarget": "prevAo" },
    { "name": "blit", "intarget": "normals",  "outtarget": "prevNormals" }
]
```

**Always validate with a canary.** Targets are reallocated on resize, resource reload and
world change; without a canary you decode uninitialised memory as state.
```glsl
const vec4 CANARY = vec4(1.0, 0.0, 1.0, 127.0 / 255.0);

if (ivec2(gl_FragCoord.xy) == ivec2(1, 0)) { fragColor = CANARY; return; }   // stamp it

bool stateValid = texelFetch(PrevStateSampler, ivec2(1, 0), 0) == CANARY;
float accumulated = stateValid ? unpackF32(texelFetch(PrevStateSampler, ivec2(0,0), 0)) : 0.0;
```

**A wrapping clock.** `Time` is a 0–1 fraction that wraps every second, so a delta needs the
wrap handled:
```glsl
float dt = Time;
if (dt < prevTime) dt += 1.0;
dt -= prevTime;
```

**Scene-change detection without a signal.** Compare one pixel of `minecraft:main` against
the previous frame's copy; reset the accumulator when it differs. Crude, free, and enough for
menus and transitions.

---

## 5. Motion blur

Needs this frame's and last frame's view-projection plus camera motion, and a merged depth
buffer so translucent geometry blurs too.

Merged depth:
```glsl
float depth = 1.0;
depth = min(depth, texture(DepthMain,       texCoord).r);
depth = min(depth, texture(DepthTranslucent,texCoord).r);
depth = min(depth, texture(DepthItemEntity, texCoord).r);
depth = min(depth, texture(DepthParticles,  texCoord).r);
depth = min(depth, texture(DepthClouds,     texCoord).r);
depth = min(depth, texture(DepthWeather,    texCoord).r);
fragColor = packF32(depth);                // RGBA8 can't hold a depth float natively
```

Velocity and accumulation:
```glsl
const int   SAMPLES  = 12;
const float STRENGTH = 0.03;

vec3 world  = positionFromDepth(texCoord, unpackF32(texture(DepthSampler, texCoord)));
vec4 pClip  = prevViewProj * vec4(world - cameraMotion, 1.0);
vec2 prevUV = pClip.xy / pClip.w * 0.5 + 0.5;

vec2 velocity = texCoord - prevUV;
velocity /= 1.0 + length(velocity);        // soft clamp: fast motion must not smear forever
velocity *= STRENGTH;

vec2 uv = texCoord - velocity * 0.5 * float(SAMPLES);
uv += velocity * blueNoise(gl_FragCoord.xy).x;      // jitter removes banding
vec3 sum = vec3(0.0);
for (int i = 0; i < SAMPLES; i++, uv += velocity)
    sum += texture(DiffuseSampler, clamp(uv, vec2(0.0, 1.5 / OutSize.y), vec2(1.0))).rgb;
fragColor = vec4(sum / float(SAMPLES), 1.0);
```

Two things that will bite you: the previous view matrix usually needs a `transpose` on decode
to match the encode side, and `ProjMat[3].xy` must be zeroed (TAA jitter) before either
matrix is used.

---

## 6. Screen re-projection — camera rotation with no input

An item display's world-space orientation supplies a tangent frame; a post pass re-projects
the screen through it, buying a few degrees of extra camera rotation.

Core side: on the marker quad, write the quad's world-space centre, tangent and bitangent
into the data strip. Recovering a tangent frame inside a fragment shader needs the per-corner
varying trick plus `gl_PrimitiveID` to know which triangle you are in
(`techniques-index.md § B3`, `§ B4`).

Post side:
```glsl
const float ZOOM = 0.8;              // lower = more rotation headroom, less FOV
vec2 ndc = texCoord * ZOOM;          // texCoord here is NDC, not 0..1

vec4 h = invProj * vec4(ndc, -1.0, 1.0);
vec3 ray = h.xyz / h.w;
ray = tbn * ray;                      // rotate the ray into the marker's frame
h = proj * vec4(ray, 1.0);

vec2 uv = h.xy / h.w * 0.5 + 0.5;
uv.y = max(uv.y, 1.0 / OutSize.y);
fragColor = texture(DiffuseSampler, uv);
```
Zero `proj[3].xy` before inverting. The zoom factor is the cost: you crop the frame so
re-projected rays still land inside it.

---

## 7. Portals and other off-camera views

The general architecture for rendering a view the camera is not at:

1. **The server or datapack moves the camera** to the other viewpoint for one frame, and
   sets a state sentinel the shader can recognise (`data-channels.md § 11`).
2. **Every core shader detects that pass** and substitutes an appropriate projection — a wide
   FOV for a portal, an orthographic one for a shadow map:
   ```glsl
   mat4 selectProjection() {
       if (isPortalPass()) return portalProjection();
       if (isShadowPass()) return shadowProjection();
       return ProjMat;
   }
   ```
   Build the substitutes explicitly rather than editing `ProjMat`:
   ```glsl
   mat4 perspective(float fovRadians, float near, float far) {
       float s = 1.0 / tan(fovRadians * 0.5);
       return mat4(  s, 0.0,                        0.0,  0.0,
                   0.0,   s,                        0.0,  0.0,
                   0.0, 0.0,     -far / (far - near), -1.0,
                   0.0, 0.0, -far * near / (far - near), 0.0);
   }
   mat4 orthographic(float l, float r, float b, float t, float n, float f) {
       return mat4(2.0/(r-l), 0.0, 0.0, 0.0,
                   0.0, 2.0/(t-b), 0.0, 0.0,
                   0.0, 0.0, -2.0/(f-n), 0.0,
                   -(r+l)/(r-l), -(t+b)/(t-b), -(f+n)/(f-n), 1.0);
   }
   ```
3. **A conditional blit saves that frame** into persistent colour and depth targets, gated by
   a control pixel so the static pipeline can decide at runtime which frame to keep
   (`techniques-index.md § A23`). Depth is written back through `gl_FragDepth` so later
   passes get hardware depth compare (`§ A24`). Two viewpoints × {colour, depth} ×
   double-buffering is eight targets.
4. **During the player's frame**, the portal surface is screen-space raymarched against the
   saved buffer.

### Screen-space raymarch

The reusable core, shared by portals and reflections:
```glsl
// march with a growing step, reject hits that skipped through thin geometry
vec2 traceScreenSpace(vec3 origin, vec3 dir, mat4 proj, sampler2D depthTex, vec2 planes) {
    float t = 0.05, tPrev = t, step = 0.05;
    for (int i = 0; i < 32; i++) {
        t += step;
        step *= 1.15;                                     // exponential growth

        vec3 p = origin + dir * t;
        vec4 clip = proj * vec4(p, 1.0);
        vec3 s = clip.xyz / clip.w * 0.5 + 0.5;
        if (clamp(s, 0.0, 1.0) != s) break;               // left the screen

        float sceneZ = linearDepth(texture(depthTex, s.xy).r * 2.0 - 1.0, planes);
        float rayZ   = linearDepth(s.z * 2.0 - 1.0, planes);
        // a hit is: behind the surface, but not so far behind that we tunnelled a wall
        if (rayZ > sceneZ && rayZ < sceneZ + step * 1.5)
            return refineScreenSpace(origin, dir, proj, depthTex, tPrev, t);
        tPrev = t;
    }
    return vec2(-1.0);                                    // miss
}

vec2 refineScreenSpace(vec3 origin, vec3 dir, mat4 proj, sampler2D depthTex,
                       float t0, float t1) {
    vec3 s;
    for (int i = 0; i < 5; i++) {                          // binary search
        float t = (t0 + t1) * 0.5;
        vec4 clip = proj * vec4(origin + dir * t, 1.0);
        s = clip.xyz / clip.w * 0.5 + 0.5;
        if (texture(depthTex, s.xy).r < s.z) t1 = t; else t0 = t;
    }
    return s.xy;
}
```
The `sceneZ + step * 1.5` thickness test is the part that matters: without it, a ray that
steps past a thin wall reports a hit on its back face and you get the classic SSR smear.

### Entering the portal plane

```glsl
vec3 samplePortal(vec3 rayOrigin, vec3 rayDir, vec3 portalPos, mat3 portalRot) {
    vec3 o = (rayOrigin - portalPos) * portalRot;      // into portal-local space
    vec3 d = rayDir * portalRot;
    float t = -o.z / d.z;                               // intersect the portal plane
    return traceSaved(o + d * t, d);                    // march in the saved view
}
```
Portal pixels are identified from a control-plane target, so one program can serve several
portals by varying a uniform.

**Cost.** Each extra viewpoint is a full extra scene render — that dominates every post pass
in the pipeline. Budget for roughly halved frame rate per view and say so up front.

---

## 8. Shadow mapping without a shadow pass

Same trick as `§7`: force an orthographic camera for one pass, save its depth, sample it.

```glsl
// branchless tangent frame (Duff et al.)
void orthonormalBasis(vec3 n, out vec3 b1, out vec3 b2) {
    if (n.z < -0.9999999) { b1 = vec3(0.0, -1.0, 0.0); b2 = vec3(-1.0, 0.0, 0.0); return; }
    float a = 1.0 / (1.0 + n.z);
    float b = -n.x * n.y * a;
    b1 = vec3(1.0 - n.x * n.x * a, b, -n.x);
    b2 = vec3(b, 1.0 - n.y * n.y * a, -n.y);
}

bool occluded(vec3 worldPos) {
    vec4 ls = shadowProj * vec4(worldPos + shadowOffset, 1.0);
    vec3 p = ls.xyz * 0.5 + 0.5;
    if (clamp(p, 0.0, 1.0) != p) return true;                 // outside the map
    return p.z - SHADOW_BIAS > texture(ShadowDepthSampler, p.xy).r;
}

// PCF with a penumbra stretched along the light direction
float softShadow(vec3 worldPos, vec3 normal, vec3 lightDir, inout uint rng) {
    vec3 b1, b2;
    orthonormalBasis(normal, b1, b2);
    vec2 stretch = (lightDir * mat3(b1, b2, normal)).xy;      // light dir in tangent space

    float sum = 0.0, n = 0.0;
    for (float y = -0.075; y < 0.075; y += 0.025)
    for (float x = -0.075; x < 0.075; x += 0.025) {
        vec2 o = 0.6 * (vec2(x, y) + rand2(rng) * 0.025);
        o += stretch * 0.2 * (rand1(rng) * 2.0 - 1.0);
        sum += occluded(worldPos + b1 * o.x + b2 * o.y) ? 1.0 : 0.0;
        n += 1.0;
    }
    return 1.0 - sum / n;
}
```

**Skip the expensive path where it cannot matter.** Test the centre normal *and its
neighbours* against the light before running 36 taps; this removes acne at silhouettes and
saves most of the cost:
```glsl
bool facingLight(vec2 uv, vec3 lightDir, float threshold, vec2 texel) {
    return dot(normalAt(uv),                        lightDir) >= threshold
        && dot(normalAt(uv + vec2( 2, 0) * texel),  lightDir) >= threshold
        && dot(normalAt(uv + vec2(-2, 0) * texel),  lightDir) >= threshold
        && dot(normalAt(uv + vec2( 0, 2) * texel),  lightDir) >= threshold
        && dot(normalAt(uv + vec2( 0,-2) * texel),  lightDir) >= threshold;
}
```

### Volumetrics from the same shadow map

```glsl
float henyeyGreenstein(float cosTheta, float g) {
    float gg = g * g;
    return (1.0 - gg) / (4.0 * PI * pow(1.0 + gg - 2.0 * g * cosTheta, 1.5));
}

vec3 volumetric(vec3 fragPos, vec3 lightDir, vec3 lightColor, inout uint rng) {
    const int   STEPS      = 16;
    const float SCATTERING = 0.015;
    float stepLen = length(fragPos) / float(STEPS + 1);
    vec3  dir     = normalize(fragPos);
    float phase   = henyeyGreenstein(dot(dir, lightDir), 0.08);

    float transmittance = 1.0, inScatter = 0.0;
    for (int i = 0; i < STEPS; i++) {
        float t = (float(i) + rand1(rng)) * stepLen;       // jitter kills banding
        transmittance *= exp(-SCATTERING * stepLen);
        if (!occluded(dir * t)) inScatter += transmittance * SCATTERING * stepLen * phase;
    }
    return lightColor * inScatter;                          // caller: color * transmittance + this
}
```

### Sun colour without an atmosphere model

Bake transmittance against solar elevation into a 1-D LUT and index it:
```glsl
float elevation = acos(clamp(lightDir.y, 0.0, 0.9999));
vec3 sunColor = texture(TransmittanceSampler, vec2(1.0 - degrees(elevation) / 90.0, 0.0)).rgb;
```

---

## 9. Screen-space reflections on water

Wave normals, Fresnel, then the raymarch from `§7`.

**Waves.** Sum directional waves whose position is advected by the previous octave's
derivative — that advection is what makes it read as water rather than sine ripples. The
`exp(sin(x) - 1)` profile gives sharp crests and flat troughs.
```glsl
vec2 waveOctave(vec2 pos, vec2 dir, float freq, float phase) {
    float x = dot(dir, pos) * freq + phase;
    float w = exp(sin(x) - 1.0);
    return vec2(w, -w * cos(x));            // value and derivative together
}

float waveHeight(vec2 pos, float time) {
    float phaseShift = length(pos) * 0.1;
    float freq = 1.0, weight = 1.0, sum = 0.0, total = 0.0;
    for (int i = 0; i < 6; i++) {
        vec2 dir = vec2(sin(float(i) * 1232.399963), cos(float(i) * 1232.399963));
        vec2 r = waveOctave(pos, dir, freq, time + phaseShift);
        pos   += dir * r.y * weight * 0.38;   // advect
        sum   += r.x * weight;
        total += weight;
        weight *= 0.8;
        freq   *= 1.18;
    }
    sum   += simplexNoise(pos * 0.2) + simplexNoise(pos * 0.1) * 2.0;
    total += 3.0;
    return sum / total * 0.15;
}

vec3 waveNormal(vec2 pos, float time) {
    const float e = 0.01;
    float h = waveHeight(pos, time);
    vec3 dx = vec3(e, waveHeight(pos + vec2(e, 0.0), time) - h, 0.0);
    vec3 dz = vec3(0.0, waveHeight(pos + vec2(0.0, e), time) - h, e);
    return normalize(cross(dz, dx));
}
```

**Fresnel, both polarisations** (not the Schlick approximation — water at grazing angles is
where the difference shows):
```glsl
float fresnelDielectric(float cosI, float n1, float n2) {
    float sin2T = (1.0 - cosI * cosI) * (n1 * n1) / (n2 * n2);
    if (sin2T >= 1.0) return 1.0;                       // total internal reflection
    float cosT = sqrt(1.0 - sin2T);
    float rS = (n1 * cosI - n2 * cosT) / (n1 * cosI + n2 * cosT);
    float rP = (n1 * cosT - n2 * cosI) / (n1 * cosT + n2 * cosI);
    return 0.5 * (rS * rS + rP * rP);
}
```

Then reflect the view ray about the wave normal, force it upward, trace, and mix by
reflectance.

---

## 10. Voxel coloured lighting

Stages:
```
copy_voxels : main(+history, data, prevData, depth) -> voxels
fill        : voxels -> swap
fill        : swap   -> voxels                  (two flood-fill iterations per frame)
copy        : voxels -> voxel_history           (fixed-size, survives window resize)
transparency: main -> swap
light       : swap(+data, voxels, depth, noise) -> swap2
composite   : swap2(+data, depth) -> minecraft:main
```

The core shader voxelizes: every light-emitting block writes its encoded colour and level to
its voxel pixel; every solid block writes an opaque marker
(`geometry-hijacking.md § 9` for the addressing).

**Flood fill** — one 6-neighbour average per pass, run twice per frame, with a small decay:
```glsl
const ivec3 NEIGHBOURS[6] = ivec3[](ivec3( 1,0,0), ivec3(-1,0,0),
                                    ivec3(0, 1,0), ivec3(0,-1,0),
                                    ivec3(0,0, 1), ivec3(0,0,-1));

vec4 src = texture(InSampler, texCoord);
if (src == OPAQUE_MARKER) { fragColor = src; return; }      // solid blocks stop light

vec3 light = decodeLogLuv(src);
ivec3 v = pixelToVoxel(ivec2(gl_FragCoord.xy), ivec2(InSize), voxelExtent);
for (int i = 0; i < 6; i++) {
    ivec3 n = v + NEIGHBOURS[i];
    if (any(greaterThanEqual(abs(n), ivec3(voxelExtent.x)))) continue;
    vec4 nd = texelFetch(InSampler, voxelToPixel(n, ivec2(InSize), voxelExtent), 0);
    if (nd != OPAQUE_MARKER) light += decodeLogLuv(nd);
}
fragColor = encodeLogLuv(light / 7.0 * 0.99);
```

**Lookup** with trilinear interpolation and a dither, added in inverse-tonemapped space so
bright lights do not clip:
```glsl
vec3 p = positionFromDepth(texCoord, depth);
vec3 dir = normalize(p);
p -= blockOffset + sign(dir) * 0.005;        // bias off the surface
p -= 1.0;                                     // centre the interpolation cell
p += dither - dir * 0.5;                      // hide the grid

ivec3 v = ivec3(p) - ivec3(lessThan(p, vec3(0.0)));   // floor, not truncate
vec3 f = fract(p);
vec3 light = trilinear(v, f);                          // 8 fetchLight() taps

color.rgb += dither / 256.0;
color.rgb  = acesInverse(color.rgb);
color.rgb *= 1.0 + sqrt(light / (1.0 + light));        // soft additive
color.rgb  = acesFilm(color.rgb);
```

**Hide the voxel pixels.** They sit in `minecraft:main`, so the composite pass must repaint
them. Taking the nearest-depth neighbour beats a 4-neighbour average on silhouettes:
```glsl
if (int(round(texture(DataSampler, texCoord).a * 255.0)) != VOXEL_TAG) return;
float best = 1.0;
for (int i = 0; i < 4; i++) {
    ivec2 c = ivec2(gl_FragCoord.xy) + OFFSETS[i];
    float d = texelFetch(DepthSampler, c, 0).r;
    if (d < best) { best = d; fragColor = texelFetch(InSampler, c, 0); }
}
```

**Expose settings** in a separate namespace so users can edit one small file:
```glsl
// assets/settings/shaders/include/settings.glsl
#define ENABLE_INTERPOLATION   1
#define ENABLE_LIGHT_DITHERING 1
#define VISUALIZE_LIGHT_COLOR  0
```
```glsl
#moj_import <settings:settings.glsl>
```

---

## 11. GPU-built world map

Distinct from a map-item minimap: this one *builds* the map from the world with no server
involvement.

```
transparency : main -> main'
hide_voxels  : main' -> final                        (repaint the voxel pixels)
copy_map     : main(+depth, prevMap, prevData) -> map     fixed 512x512, persistent
shade        : map(+palette, data) -> shadedMap
copy         : map  -> prevMap
copy         : main -> prevData
overlay      : final(+shadedMap, data, cursor) -> main
blit         : main -> minecraft:main
```

**Accumulate and scroll.** Each map texel stores `(paletteIndex, heightHi, heightLo, valid)`.
Scroll by the camera's integer block motion, then merge in this frame's voxels, keeping the
highest block seen:
```glsl
ivec2 px = ivec2(gl_FragCoord.xy);
ivec3 motion = ivec3(cameraBlockMotion);

fragColor = texelFetch(PreviousSampler, px - motion.xz, 0);
int storedHeight = (int(fragColor.g * 255.0) << 8 | int(fragColor.b * 255.0)) + motion.y;
fragColor.gb = vec2(float(storedHeight >> 8), float(storedHeight & 0xFF)) / 255.0;

vec4 voxel = fetchVoxelForColumn(px);
int height = heightFromVoxelDepth(px);
if (isVoxel(voxel) && (height >= storedHeight || fragColor.a < 1.0))
    fragColor = vec4(voxel.r, float(height >> 8) / 255.0, float(height & 0xFF) / 255.0, 1.0);
```

**Shade** from the height difference with the neighbour to the north, matching vanilla map
shading (three brightness bands), then remap to a nicer palette if you want.
```glsl
float slope = float(centre.height - north.height) * 0.8 - 0.2;
int band = slope > 0.6 ? 2 : (slope < -0.6 ? 0 : 1);
fragColor = texelFetch(PaletteSampler, ivec2(band, centre.index), 0);
```

**Draw the widget.** Rotate by camera yaw and offset by the sub-block position so it scrolls
smoothly rather than jumping a texel at a time:
```glsl
vec2 local = (texCoord - widgetMin) / (widgetMax - widgetMin);
if (clamp(local, 0.0, 1.0) != local) return;

vec2 n11 = local * 2.0 - 1.0;
float d = dot(n11, n11);                         // squared radius, no sqrt
if (d < INNER) {
    vec2 uv = ((local - 0.5) * rotate2(-yaw)).yx / ZOOM + 0.5;
    fragColor = texture(MapSampler, uv - subBlockOffset / MAP_SIZE);
} else if (d < OUTER) {
    fragColor = bezelRing(d);
}
```
Camera yaw comes from the decoded view matrix:
`vec3 f = viewMat * vec3(1,0,0); float yaw = -atan(f.x, f.z);`

---

## 12. Entity glow

Replace the outline pipeline with a two-pass separable blur, then subtract the original
silhouette so only the halo remains.

```glsl
layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
layout(std140) uniform BlurConfig  { vec2 BlurDir; };

const float RADIUS = 32.0;

void main() {
    float sigma = RADIUS * 0.5;
    vec2 step = BlurDir / InSize;
    vec4 sum = vec4(0.0);
    float wsum = 0.0;
    for (float i = -RADIUS; i <= RADIUS; i += 1.0) {
        float w = exp(-(i * i) / (2.0 * sigma * sigma));
        sum += texture(InSampler, texCoord + step * i) * w;
        wsum += w;
    }
    fragColor = sum / wsum;
}
```
Combine:
```glsl
vec4 blurred = texture(InSampler, texCoord);
float original = texture(OrigSampler, texCoord).a;

// un-premultiply so the glow keeps the entity's colour instead of fading to grey
vec3 tint = clamp(blurred.rgb / max(blurred.a, COLOR_FLOOR), 0.0, 1.0);
float coverage = max(blurred.a - original, 0.0);          // halo = blur minus silhouette
fragColor = vec4(tint * GLOW_TINT,
                 clamp(smoothstep(EDGE, SOFT, coverage) * INTENSITY, 0.0, 1.0));
```

One shader serves both directions; it can tell which pass it is in from `BlurDir.y != 0.0`.

---

## 13. Blur chains and transitions

**Doubling radii.** Eight cheap 7-tap separable passes at radii 1, 1, 2, 2, 4, 4, 8, 8
approximate a very wide Gaussian:
`Direction` = (0,1), (1,0), (0,2), (2,0), (0,4), (4,0), (0,8), (8,0).

**Tent weights plus progressive desaturation** — as the blur grows, drain the colour. Cheap
and very effective for pause menus:
```glsl
for (float r = -radius; r <= radius; r += 1.0) {
    float w = radius - abs(r);
    sum += texture(InSampler, texCoord + texel * r * BlurDir) * w;
    wsum += w;
}
vec4 c = sum / wsum;
float luma = pow(dot(pow(c.rgb, vec3(2.2)), vec3(0.2126, 0.7152, 0.0722)), 1.0 / 2.2);
fragColor = mix(c, vec4(vec3(luma), 1.0), radius * 0.25);
```

**Drive the radius from a cross-frame timer** (`§4`) to get an eased transition with no
server involvement.

---

## 14. Colour grading with a 3-D LUT

Two ways to pack a 3-D LUT into a 2-D texture.

**16×16 tiles of 256×256 — exact, no interpolation needed:**
```glsl
ivec3 c = ivec3(color.rgb * 255.0);
fragColor.rgb = texelFetch(LutSampler,
    ivec2((c.b % 16) * 256 + c.r, (c.b / 16) * 256 + c.g), 0).rgb;
```

**32-slice strip — smaller, needs manual slice blending:**
```glsl
float x  = color.g * (31.0 / 1024.0) + 0.5 / 1024.0;
float y  = color.b * (31.0 /   32.0) + 0.5 /   32.0;
float z1 = floor(color.r * 31.0) / 32.0;
float z2 = ceil (color.r * 31.0) / 32.0;
float f  = (z2 == z1) ? 0.0 : (color.r * 31.0 / 32.0 - z1) / (z2 - z1);
fragColor.rgb = mix(texture(LutSampler, vec2(x + z1, y)).rgb,
                    texture(LutSampler, vec2(x + z2, y)).rgb, f);
```

Grade in linear, inverse-tonemapped space and re-apply the tone map afterwards, or the LUT
fights the display transform.

---

## 15. Cheap screen effects

```glsl
// saturation
float l = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
color.rgb = mix(vec3(l), color.rgb, saturation);

// vignette
vec2 d = texCoord - 0.5;
color.rgb *= 1.0 - dot(d, d) * vignetteStrength;

// chromatic aberration
color.r = texture(InSampler, texCoord + d * aberration).r;
color.b = texture(InSampler, texCoord - d * aberration).b;
```

A status-effect overlay: a radial tint plus procedural particles, all from `GameTime` and a
hash — no textures, no state.
```glsl
float t = max(dot(uv, uv) - 0.3, 0.0);
vec4 background = tint * vec4(1.0, 1.0, 1.0, t);
float glow = 0.0;
for (int i = 0; i < 64; ++i) {
    float aspect = ScreenSize.x / ScreenSize.y;
    vec2 p = vec2((hash1(float(i) + 1.0) * 2.0 - 1.0) * aspect,
                   hash1(float(i) + 2.0) * 2.0 - 1.0);
    float size = hash1(float(i) + 3.0) * 0.002 + 0.0002;
    float life = fract(GameTime * SPEED + hash1(float(i) + 4.0));
    p *= 1.0 + drift * life;
    float fade = -cos(life * TAU) * 0.5 + 0.5;
    vec2 delta = p - uv * vec2(aspect, 1.0);
    if (dot(delta, delta) < size) glow += 0.2 * t * t * fade;
}
```

---

## 16. Reproducing vanilla transparency compositing

If you rewrite the `transparency` pass you must reproduce its layer sort: six layers
(main, translucent, item entity, particles, clouds, weather), insertion-sorted by depth
back-to-front, skipping fully transparent ones.

```glsl
const int LAYERS = 6;
vec4  layerColor[LAYERS];
float layerDepth[LAYERS];
int   layerCount = 0;

void insertLayer(vec4 c, float d) {
    if (c.a == 0.0) return;
    layerColor[layerCount] = c;
    layerDepth[layerCount] = d;
    int j = layerCount++;
    for (int i = j - 1; j > 0 && layerDepth[j] > layerDepth[i]; j = i--) {
        float td = layerDepth[i]; layerDepth[i] = layerDepth[j]; layerDepth[j] = td;
        vec4  tc = layerColor[i]; layerColor[i] = layerColor[j]; layerColor[j] = tc;
    }
}
```
Then blend front-to-back from the sorted array. Get this wrong and translucency ordering
breaks everywhere in the game, not just in your effect.

---

## 17. Performance budget

Rough costs at 1080p; the unit is full-screen passes.

| Feature | Extra passes | Notes |
|---|---|---|
| Data marker | 0 | ~40 fragments |
| Bloom, 7-level pyramid | 14 | each at decreasing resolution → ≈2.3 full-screen equivalents |
| SSAO + temporal | 6 | plus 16 taps in the AO pass |
| Motion blur | 3 | 12 taps |
| Voxel coloured lighting | 6 | plus a full extra voxel rasterisation of terrain |
| GPU-built map | 6 | plus one marker quad per visible block |
| Portal | 4 | **plus a whole extra scene render per portal** |
| Shadow map | 2 | **plus a whole extra scene render** |

The expensive items are the extra scene renders, not the post passes. Anything in that class
needs a stated requirement: Fabulous graphics, low render distance, and an honest warning.
See `limits.md`.
