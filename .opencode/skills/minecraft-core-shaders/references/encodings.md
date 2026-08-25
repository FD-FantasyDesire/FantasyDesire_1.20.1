# Encodings — packing data into 8-bit pixels

Every render target in the vanilla pipeline is RGBA8 (except depth). Everything you carry
between passes must survive a round trip through four unsigned bytes. This file is the
complete set of codecs this discipline needs.

---

## 1. Signed 24-bit fixed point in RGB (the workhorse)

The default choice for every data-marker value.

```glsl
// ---- encode (fragment shader, core) ----
vec4 encodeInt(int i) {
    int s = int(i < 0) * 128;
    i = abs(i);
    int r = i % 256; i /= 256;
    int g = i % 256; i /= 256;
    int b = i % 256;
    return vec4(float(r) / 255.0, float(g) / 255.0, float(b + s) / 255.0, 1.0);
}
vec4 encodeFloat(float v)      { return encodeInt(int(floor(v * 40000.0))); }
vec4 encodeFloat1024(float v)  { return encodeInt(int(floor(v * 1024.0))); }

// ---- decode (vertex shader, post) ----
int decodeInt(vec3 ivec) {
    ivec *= 255.0;
    int s = ivec.b >= 128.0 ? -1 : 1;
    return s * (int(ivec.r) + int(ivec.g) * 256 + (int(ivec.b) - 64 + s * 64) * 256 * 256);
}
float decodeFloat(vec3 ivec)      { return float(decodeInt(ivec)) / 40000.0; }
float decodeFloat1024(vec3 ivec)  { return float(decodeInt(ivec)) / 1024.0; }
```

Range and precision:

| Scale | Representable range | Resolution |
|---|---|---|
| 40000 | ±209.7 | 2.5 × 10⁻⁵ |
| 1024 | ±8192 | 9.8 × 10⁻⁴ |

Choose per field: matrix elements and normals want 40000; world offsets and fog distances
want 1024. **A value that overflows wraps silently**, which is why an unbounded quantity such
as `ProjMat[0][0]` should be stored as its arctangent (see `data-channels.md § 1`).

### The generalised form

```glsl
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

#define FP_PRECISION_UNIT   8388607.0
#define FP_PRECISION_HIGH   400000.0
#define FP_PRECISION_MEDIUM 10000.0
#define FP_PRECISION_LOW    1000.0

vec3  packFPtoF8x3(float x, float p)   { return packSI24toF8x3(int(round(x * p))); }
float unpackFPfromF8x3(vec3 v, float p){ return float(unpackSI24fromF8x3(v)) / p; }
```
Prefer this version for new work: it uses `round` rather than `floor` (halves the error) and
names the precision tiers.

Unsigned variant:
```glsl
vec3 packUI24toF8x3(uint u) { return vec3(u & 0xFFu, (u >> 8u) & 0xFFu, (u >> 16u) & 0xFFu) / 255.0; }
uint unpackUI24fromF8x3(vec3 v) { uvec3 d = uvec3(v * 255.0); return d.r | (d.g << 8u) | (d.b << 16u); }
```

---

## 2. Exact IEEE-754 float in RGBA (lossless, 32 bits)

Use when you need bit-exact round-tripping — depth values, accumulators, anything you will
compare for equality.

```glsl
vec4 packF32toF8x4(float f) {
    uint b = floatBitsToUint(f);
    return vec4(b >> 24u, (b >> 16u) & 0xFFu, (b >> 8u) & 0xFFu, b & 0xFFu) / 255.0;
}
float unpackF32fromF8x4(vec4 v) {
    uvec4 d = uvec4(v * 255.0);
    return uintBitsToFloat((d.r << 24u) | (d.g << 16u) | (d.b << 8u) | d.a);
}
```
Requires `#version 330` (`floatBitsToUint` / `uintBitsToFloat` are GLSL 3.30). This is how
packs carry a depth buffer through an RGBA8 target:
```glsl
float depth = texture(DiffuseDepthSampler, texCoord).r;
uint bits = floatBitsToUint(depth);
fragColor = vec4(bits >> 24, (bits >> 16) & 0xFFu, (bits >> 8) & 0xFFu, bits & 0xFFu) / 255.0;
```
Alpha is consumed, so a target holding a packed float cannot also hold an alpha tag.

Two-pixel variant when you must keep alpha free — split the four bytes across two pixels'
RG channels:
```glsl
vec4 data = packF32toF8x4(gameTime);
// pixel 21 → vec4(data[0], data[1], 0, 1);  pixel 22 → vec4(data[2], data[3], 0, 1)
```

---

## 3. LogLuv — HDR in RGBA8

Bloom and any lighting model need HDR intermediate targets, and MC gives you none. LogLuv
encodes an unbounded positive RGB in four bytes with good perceptual accuracy.

```glsl
// from https://therealmjp.github.io/posts/logluv-encoding-for-hdr/
const mat3 LOGLUV_M = mat3(
    0.2209, 0.1138, 0.0102,
    0.3390, 0.6780, 0.1130,
    0.4184, 0.7319, 0.2969);
const mat3 LOGLUV_INV_M = mat3(
     6.0013, -1.3320,  0.3007,
    -2.7000,  3.1029, -1.0880,
    -1.7995, -5.7720,  5.6268);

vec4 encodeLogLuv(vec3 rgb) {
    vec4 result;
    vec3 Xp_Y_XYZp = max(rgb * LOGLUV_M, vec3(1.0e-6));
    result.xy = Xp_Y_XYZp.xy / Xp_Y_XYZp.z;
    float Le = 2.0 * log2(Xp_Y_XYZp.y) + 127.0;
    result.w = fract(Le);
    result.z = (Le - (floor(result.w * 255.0)) / 255.0) / 255.0;
    return result;
}

vec3 decodeLogLuv(vec4 logLuv) {
    float Le = logLuv.z * 255.0 + logLuv.w;
    vec3 Xp_Y_XYZp;
    Xp_Y_XYZp.y = exp2((Le - 127.0) / 2.0);
    Xp_Y_XYZp.z = Xp_Y_XYZp.y / logLuv.y;
    Xp_Y_XYZp.x = logLuv.x * Xp_Y_XYZp.z;
    return max(Xp_Y_XYZp * LOGLUV_INV_M, 0.0);
}
```

**LogLuv is not linear**, so hardware bilinear filtering of a LogLuv target is wrong. Do the
filtering yourself after decoding:
```glsl
vec3 bilinear(sampler2D s, vec2 tex, ivec2 inRes) {
    tex = tex * inRes - 0.5;
    ivec2 c = ivec2(floor(tex));
    vec2 f = fract(tex);
    return mix(mix(decodeLogLuv(texelFetch(s, c + ivec2(0,0), 0)),
                   decodeLogLuv(texelFetch(s, c + ivec2(1,0), 0)), f.x),
               mix(decodeLogLuv(texelFetch(s, c + ivec2(0,1), 0)),
                   decodeLogLuv(texelFetch(s, c + ivec2(1,1), 0)), f.x), f.y);
}
```
Also note `encodeLogLuv(vec3(0.0))` is **not** `vec4(0)`; if you use "equals a specific
LogLuv value" as a sentinel, precompute it in the vertex shader and pass it as a varying
rather than comparing against `vec4(0)` or `vec4(1)` by accident.

---

## 4. YCoCg — chroma subsampling and bit stealing

Packing a colour plus a 4-bit auxiliary value into three bytes:

```glsl
const mat3 RGB2YCoCg = mat3(0.25, 0.5, -0.25, 0.5, 0.0, 0.5, 0.25, -0.5, -0.25);
vec3 rgb2YCoCg(vec3 rgb) { return RGB2YCoCg * rgb; }
vec3 YCoCg2rgb(vec3 y) { float t = y.r - y.b; return vec3(t + y.g, y.r + y.b, t - y.g); }

// 7 bits Y, 7 bits Co, 6 bits Cg, 4 spare bits
vec3 encodeYCoCg776(vec3 rgb, uint lowerBits) {
    vec3 c = rgb2YCoCg(clamp(rgb, 0.0, 1.0));
    c.yz += 0.5;
    uint bits = (lowerBits & 15u) << 4u;
    bits |= uint(c.x * 127.0) << 17u;
    bits |= uint(c.y * 127.0) << 10u;
    bits |= (uint(c.z * 63.0) >> 4u) << 8u;
    bits |= (uint(c.z * 63.0) & 15u);
    return vec3(bits >> 16u, (bits >> 8u) & 0xFFu, bits & 0xFFu) / 255.0;
}
vec3 decodeYCoCg776(vec3 v, out uint lowerBits) {
    uvec3 d = uvec3(v * 255.0);
    uint bits = (d.r << 16u) | (d.g << 8u) | d.b;
    lowerBits = (bits >> 4u) & 15u;
    float Y  = float(bits >> 17u) / 127.0;
    float Co = float((bits >> 10u) & 127u) / 127.0;
    float Cg = float((bits & 15u) | (((bits >> 8u) & 3u) << 4u)) / 63.0;
    return YCoCg2rgb(vec3(Y, Co - 0.5, Cg - 0.5));
}
```
Related packings worth having: 16-bit luma with 8-bit chroma each (good for temporal
accumulation) and a single byte of luma plus 4+4 chroma (for half-resolution chroma).

Chroma-subsampled formats are the right choice when you need more luminance precision than
chroma precision — most notably in temporal accumulation buffers, where chroma error is
imperceptible but luma banding is obvious.

---

## 5. Map palette — 7-bit bytes in map colours

The vanilla map palette has 256 entries as 64 base colours × 4 brightness levels
(×180/255, ×220/255, ×255/255, ×135/255 — note the odd order). Entries 0–3 are transparent.

The palette is vanilla game data. Generate the table from the game's own values rather than
retyping it, and regenerate it whenever Mojang adds base colours.

```glsl
int decode7u(vec3 color) {
    ivec3 d = ivec3(color * 255.0);
    for (int i = 0; i < 128; i++) if (lookup[i] == d) return i;
    return 0;
}
```
A 128-iteration linear search per texel is acceptable at 128×128 but not at full screen. If
you need speed, precompute a lookup texture instead.

**24-bit colour in a 2×2 cell** (see `data-channels.md § 8` for the shader side). The C
encoder, server-side:
```c
uint8_t b1 = d & 0xFF,        msb1 = b1 >> 7;
uint8_t b2 = (d >> 8) & 0xFF, msb2 = b2 >> 7;
uint8_t b3 = (d >> 16) & 0xFF,msb3 = b3 >> 7;
uint8_t b4 = (msb3 << 2) | (msb2 << 1) | msb1;
b1 &= 0x7F; b2 &= 0x7F; b3 &= 0x7F;
out[y*2  ][x*2  ] = palette[b1];
out[y*2  ][x*2+1] = palette[b2];
out[y*2+1][x*2  ] = palette[b3];
out[y*2+1][x*2+1] = palette[b4];
```

**Bitstream form**: write 7 bits at a time, add 4 so no byte lands in the
transparent range.
```java
private static final int PAYLOAD_BITS = 7;
private static final int OFFSET = 4;

public void writeBits(int numBits, int value) {
    long mask = (1L << numBits) - 1;
    long v = value & mask;
    for (int i = numBits - 1; i >= 0; i--) {
        bitBuffer = (bitBuffer << 1) | (int)((v >> i) & 1);
        if (++bitCount == PAYLOAD_BITS) { target.write(bitBuffer + OFFSET); bitBuffer = 0; bitCount = 0; }
    }
}
```
Total capacity 128 × 128 = 16384 bytes → 114,688 bits per map.

**Brightness-preserving colour remap.** Because a map colour always appears at one of four
shades, a remap must handle all four. A macro keeps that readable:
```glsl
#define MAP(v, t) \
case ((int(v.r)<<16) + (int(v.g)<<8) + int(v.b)): color.rgb = t/255.; break; \
case ((int(v.r*220./255.)<<16) + (int(v.g*220./255.)<<8) + int(v.b*220./255.)): color.rgb = t/255.*220./255.; break; \
case ((int(v.r*180./255.)<<16) + (int(v.g*180./255.)<<8) + int(v.b*180./255.)): color.rgb = t/255.*180./255.; break; \
case ((int(v.r*135./255.)<<16) + (int(v.g*135./255.)<<8) + int(v.b*135./255.)): color.rgb = t/255.*135./255.; break;

void remapColor(inout vec4 color) {
    ivec3 i = ivec3(color.rgb * 255.5);
    switch ((i.r << 16) + (i.g << 8) + i.b) {
        MAP(vec3(127.,178.,56.),  vec3(94.,123.,57.))
        MAP(vec3(247.,233.,163.), vec3(248.,235.,186.))
        MAP(vec3(160.,160.,255.), vec3(132.,171.,244.))
        /* ... */
    }
}
```

---

## 6. Small helpers

```glsl
// clamped 0..1 float in two bytes
vec2 packF01U16toF8x2(float f) {
    int bits = int(clamp(f, 0.0, 1.0) * 65535.0);
    return vec2(bits >> 8, bits & 0xFF) / 255.0;
}

// signed 32-bit int in RGBA
vec4 packSI32toF8x4(int i) { return vec4(i >> 24, (i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF) / 255.0; }
int unpackSI32fromF8x4(vec4 v) {
    ivec4 d = ivec4(v * 255.0);
    return (d.r << 24) | (d.g << 16) | (d.b << 8) | d.a;
}

// three bytes as a little-endian int (e.g. video metadata in a texture header)
int pixelToInt(ivec4 pixel) { return pixel.r | (pixel.g << 8) | (pixel.b << 16); }

// colour as a comparable 32-bit id
#define COLOR_ID_RGB(r, g, b) ((uint(r) << 24) | (uint(g) << 16) | (uint(b) << 8) | 255u)
uint colorId(vec4 c) {
    return (uint(round(c.r*255.0)) << 24) | (uint(round(c.g*255.0)) << 16)
         | (uint(round(c.b*255.0)) <<  8) |  uint(round(c.a*255.0));
}
```

---

## 7. Tone mapping and colour space

Minecraft's main target holds **gamma-encoded, tone-mapped sRGB**. To do any physically
meaningful work (bloom, colored lighting, HDR mixing) you must undo both, then redo them.

```glsl
vec3 acesFilm(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}
// analytic inverse of the above
vec3 acesInverse(vec3 x) {
    return (sqrt(-10127.0 * x * x + 13702.0 * x + 9.0) + 59.0 * x - 3.0) / (502.0 - 486.0 * x);
}
```
The canonical round trip:
```glsl
color.rgb = pow(color.rgb, vec3(2.2));       // sRGB -> linear
color.rgb = acesInverse(color.rgb);          // display -> scene-referred HDR
color.rgb = max(color.rgb, decodeLogLuv(emission));
color.rgb = mix(color.rgb, bloom, BLOOM_STRENGTH);
color.rgb = acesFilm(color.rgb);             // HDR -> display
color.rgb = pow(color.rgb, vec3(1.0 / 2.2)); // linear -> sRGB
```
A variant that clamps first to avoid the singularity at 1.0:
```glsl
vec3 reverseAces(vec3 color) {
    color = clamp(color, 0.01, 0.99);
    return (-sqrt(-0.0428*color*color + 0.0555*color) - 0.1214*color + 0.006) / (color - 1.0);
}
```
Always clamp before `acesInverse`; the square root goes imaginary near the endpoints and
produces NaNs that then propagate through blur passes and turn the screen black.

Other colour utilities worth having in your own `utils.glsl`: `hsvToRgb`, `rgbToHsv`,
`linearToLogC`/`logCToLinear`, and `luminance` (`dot(col, vec3(0.2126, 0.7152, 0.0722))`).

---

## 8. Dithering

Quantising to 8 bits after any accumulation produces banding. Add a sub-LSB dither.

Ordered 4×4 Bayer:
```glsl
float getDither(ivec2 pixel) {
    const float[] ditherMap = float[](
        0.0625, 0.5625, 0.25,  0.75,
        0.8125, 0.3125, 1.0,   0.5,
        0.1875, 0.6875, 0.125, 0.625,
        0.9375, 0.4375, 0.875, 0.375);
    return ditherMap[(pixel.x & 3) | ((pixel.y & 3) << 2)];
}
```
Blue noise from a texture — declare it as a
512×512 aux target/`location` and index it modulo 512:
```glsl
vec3 random(float v) {
    ivec2 c = ivec2(mod(gl_FragCoord.xy + vec2(0.75487762, 0.56984027) * 512.0 * v, 512.0));
    return texelFetch(NoiseSampler, c, 0).xyz;
}
```
Those two constants are the R2 low-discrepancy sequence offsets; scrolling the noise by
`frameIndex * R2` decorrelates successive frames, which is what makes temporal
accumulation converge instead of ghosting.

Apply as `fragColor.rgb += dither / 256.0;` before the final quantisation.

---

## 9. Choosing an encoding

| Need | Use |
|---|---|
| A matrix element, normal, or unit-ish scalar | `packFPtoF8x3` @ `FP_PRECISION_HIGH` |
| A world offset or fog distance | `packFPtoF8x3` @ `FP_PRECISION_LOW` |
| Depth, or anything compared for equality | `packF32toF8x4` (lossless) |
| HDR colour | LogLuv |
| Colour where luma matters more than chroma | YCoCg 1688 / 844 / 776 |
| A per-texel enum | texture alpha byte |
| Server → client bulk pixels | map palette 7-bit bytes |
| Cross-frame counter | one byte in a private 1×1 target + a canary pixel |

Always pair an encoding with a **validity check**: a magic value, a canary pixel, or a
range assertion. Uninitialised or stale target contents decode into plausible-looking
garbage otherwise, and the symptom (a scene that is subtly wrong once every few minutes) is
very hard to diagnose.
