# GPU-side UI — text, digits and panels drawn entirely in GLSL

When the value you want to show only exists inside the shader (a decoded uniform, a
computed statistic, a debug readout), you cannot ask the game to draw text for you. Draw it
yourself.

---

## 1. A bitmap font packed into integer constants

A 5-wide by 6-tall glyph is 30 bits, so each character fits in one `uint`. Store the bitmap
MSB-first, row-major, and index it directly.

### Generating the table

Do not hand-write the constants. Draw the glyphs, then emit them:

```python
# font_gen.py -- emits GLSL #defines for a 5x6 bitmap font
GLYPHS = {
    "A": ["01110",
          "10001",
          "10001",
          "11111",
          "10001",
          "10001"],
    "B": ["11110",
          "10001",
          "11110",
          "10001",
          "10001",
          "11110"],
    # ... one entry per character you need
}

for name, rows in GLYPHS.items():
    assert len(rows) == 6 and all(len(r) == 5 for r in rows), name
    bits = int("".join(rows), 2)          # row 0 ends up in the high bits
    print("#define _%s %du" % (name, bits))
```

This keeps the font yours, lets you extend it (symbols, units, a different size) and removes
any dependency on a particular table. Adjust `GLYPH_W`/`GLYPH_H` below if you change the cell
size.

### Decoding one pixel

```glsl
const int GLYPH_W = 5;
const int GLYPH_H = 6;

bool glyphPixel(uint glyph, int x, int y) {
    int bit = (GLYPH_W - 1 - x) + (GLYPH_H - 1 - y) * GLYPH_W;
    return ((glyph >> bit) & 1u) == 1u;
}
```

### An immediate-mode emitter

A fragment shader runs per pixel, so "drawing" a string really means "asking, for this pixel,
whether any glyph in the string covers it". The whole string is re-emitted for every pixel.
That is fine at debug-overlay sizes and surprisingly cheap.

```glsl
const int TEXT_SCALE = 2;
const int ADVANCE_X  = GLYPH_W + 1;
const int ADVANCE_Y  = GLYPH_H + 1;

ivec2 tCursor;     // where the next glyph goes, in glyph-space pixels
ivec2 tPixel;      // this fragment, in glyph-space pixels, top-left origin
vec4  tInk;        // current text colour
vec4  tOut;        // accumulated result

void textBegin(ivec2 fragCoordTopLeft) {
    tCursor = ivec2(1, 1);
    tPixel  = fragCoordTopLeft / TEXT_SCALE;
    tInk    = vec4(1.0);
    tOut    = vec4(0.0);
}

void textColor(vec4 c) { tInk = c; }

void putGlyph(uint glyph) {
    ivec2 local = tPixel - tCursor;
    if (clamp(local, ivec2(0), ivec2(GLYPH_W - 1, GLYPH_H - 1)) == local)
        tOut = glyphPixel(glyph, local.x, local.y) ? tInk : vec4(0.0);
    tCursor.x += ADVANCE_X;
}

void putDigit(int d)  { putGlyph(DIGITS[clamp(d, 0, 9)]); }
void newline()        { tCursor.x = 1; tCursor.y += ADVANCE_Y; }
vec4 textResult()     { return tOut; }
```

### Printing a float

```glsl
const int FRACTION_DIGITS = 5;

void putFloat(float v) {
    if (v < 0.0) { v = -v; putGlyph(_DASH); }

    float whole = floor(v);
    if (whole == 0.0) {
        putDigit(0);
    } else {
        int digits = int(floor(log2(whole) / log2(10.0))) + 1;
        for (int i = 0; i < digits; i++) {
            float scale = pow(10.0, float(digits - 1 - i));
            float d = floor(whole / scale);
            putDigit(int(d));
            whole -= d * scale;
        }
    }

    putGlyph(_DOT);

    float frac = v - floor(v);
    for (int i = 0; i < FRACTION_DIGITS; i++) {
        frac *= 10.0;
        int d = int(floor(frac));
        putDigit(d);
        frac -= float(d);
        if (frac < 1e-6) break;                 // stop at an exact value
    }
}
```

### A colour swatch plus its components

```glsl
void putColor(vec3 c) {
    ivec2 local = tPixel - tCursor;
    if (clamp(local, ivec2(0), ivec2(GLYPH_H - 1)) == local) {
        bool border = clamp(local, ivec2(1), ivec2(GLYPH_H - 2)) != local;
        tOut = vec4(border ? vec3(0.0) : c, 1.0);
    }
    tCursor.x += GLYPH_H + 1;
    putGlyph(_PARENL);
    putFloat(c.r); putGlyph(_COMMA); putGlyph(_SPACE);
    putFloat(c.g); putGlyph(_COMMA); putGlyph(_SPACE);
    putFloat(c.b); putGlyph(_PARENR);
}
```

### Usage

```glsl
textBegin(fragCoord);
textColor(vec4(0.70, 0.70, 0.70, 1.0));
putGlyph(_S); putGlyph(_K); putGlyph(_Y); putGlyph(_SPACE);
textColor(vec4(0.50, 0.50, 0.50, 1.0));
putFloat(skyFactor);
newline();
/* more lines */
fragColor = textResult();
if (fragColor.a < 0.1) discard;
```

`fragCoord` must have a **top-left origin**, so flip it:
```glsl
ivec2 screenSize = ivec2(gl_FragCoord.xy / (glPos.xy / glPos.w * 0.5 + 0.5));
ivec2 fragCoord  = ivec2(gl_FragCoord.x, screenSize.y - 1 - floor(gl_FragCoord.y));
```
That first line is the general "recover screen size from a clip-space varying" trick — pass
`gl_Position` out of the vertex shader as `glPos` and divide.


## 2. Seven-segment digits

Cheaper and larger than the bitmap font — good for readouts. From.
```glsl
const float DW = 9.0;    // digit cell width
const float DH = 15.0;   // digit cell height
const float DT = 2.0;    // segment thickness

// segments: 0=a top, 1=b upper-right, 2=c lower-right, 3=d bottom,
//           4=e lower-left, 5=f upper-left, 6=g middle
int segMask(int d) {
    if (d == 0) return 63;
    if (d == 1) return 6;
    if (d == 2) return 91;
    if (d == 3) return 79;
    if (d == 4) return 102;
    if (d == 5) return 109;
    if (d == 6) return 125;
    if (d == 7) return 7;
    if (d == 8) return 127;
    return 111;                     // 9
}

bool inSeg(vec2 p, int s) {
    float mid = (DH - DT) * 0.5;
    if (s == 0) return p.y >= DH - DT && p.x >= 1.0 && p.x <= DW - 1.0;
    if (s == 6) return p.y >= mid && p.y <= mid + DT && p.x >= 1.0 && p.x <= DW - 1.0;
    if (s == 3) return p.y <= DT && p.x >= 1.0 && p.x <= DW - 1.0;
    if (s == 5) return p.x <= DT && p.y >= mid && p.y <= DH - 1.0;
    if (s == 1) return p.x >= DW - DT && p.y >= mid && p.y <= DH - 1.0;
    if (s == 4) return p.x <= DT && p.y >= 1.0 && p.y <= mid + DT;
    return p.x >= DW - DT && p.y >= 1.0 && p.y <= mid + DT;      // s == 2
}
```
Render lit and unlit ("ghost") segments separately for an authentic LCD look:
```glsl
bool lit = false, ghost = false;
for (int s = 0; s < 7; s++) {
    if (inSeg(dp, s)) { if ((mask & (1 << s)) != 0) lit = true; else ghost = true; }
}
if (lit)        col = vec3(0.05, 0.07, 0.05);
else if (ghost) col = mix(col, vec3(0.46, 0.53, 0.40), 0.75);
```

---

## 3. Laying out a panel in virtual pixels

Design the whole UI on a fixed virtual grid, then let the vertex shader map that grid to
however many real pixels the window gives you. Everything downstream is written once, in
integers.

```glsl
// hud.vsh — a region-limited triangle whose local coords are VIRTUAL pixels
layout(std140) uniform SamplerInfo { vec2 OutSize; };
layout(std140) uniform HudAnchor   { vec2 Anchor; };   // 0 = NDC -1 edge, 1 = +1 edge
layout(location = 0) out vec2 vPos;

const float PANEL_W = 138.0;
const float PANEL_H = 67.0;
const float MARGIN  = 8.0;

void main() {
    float s = clamp(floor(OutSize.y / 540.0), 1.0, 4.0);      // integer DPI scale
    vec2 panel  = vec2(PANEL_W, PANEL_H) * s;
    vec2 margin = vec2(MARGIN * s);
    vec2 corner = mix(margin, OutSize - panel - margin, Anchor);

    vec2 uv   = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);
    vec2 span = (vec2(PANEL_W, PANEL_H) + vec2(1.0)) * s;
    vec2 px   = corner + uv * span;

    gl_Position = vec4(px / OutSize * 2.0 - 1.0, 0.0, 1.0);
    vPos = uv * span / s;                                     // back to virtual pixels
}
```
Notes:
- `floor(OutSize.y / 540.0)` clamped to 1–4 gives crisp integer scaling, matching MC's own
  GUI scale behaviour.
- `Anchor` as a uniform (not a `#define`) lets one shader serve all four corners, chosen
  from the pipeline JSON.
- The `+ vec2(1.0)` slack ensures the far edge of the panel is inside the triangle and is
  not dropped by the rasteriser fill rule.
- `gl_VertexIndex` here is G5; use `gl_VertexID` on G1–G4.

Fragment side — a bezel, an LCD background, then content, all in virtual pixels:
```glsl
if (vPos.x >= PANEL_W || vPos.y >= PANEL_H) discard;
vec2 p = vec2(vPos.x, RowFlip > 0.5 ? vPos.y : PANEL_H - vPos.y);   // top-left origin

vec3 col;
if (p.x < 1.0 || p.y < 1.0 || p.x >= PANEL_W - 1.0 || p.y >= PANEL_H - 1.0)
    col = vec3(0.05, 0.05, 0.06);                    // outer bezel
else if (p.x < 3.0 || p.y < 3.0 || p.x >= PANEL_W - 3.0 || p.y >= PANEL_H - 3.0)
    col = vec3(0.22, 0.24, 0.23);                    // inner bezel
else
    col = vec3(0.58, 0.66, 0.50);                    // LCD green

// rows/columns by division, no branching per element
float ry = p.y - PAD;
int   row = int(floor(ry / ROWADV));
float sy  = ry - float(row) * ROWADV;
if (row >= 0 && row < 3 && sy >= 0.0 && sy < DH) { /* draw row `row` */ }
```
The `floor(x / PITCH)` + `x - i*PITCH` idiom converts a linear coordinate into
(index, local offset) and replaces what would otherwise be a chain of `if` per element.

The `RowFlip` uniform exists because the y direction depends on the render backend; expose
it rather than hardcoding.

---

## 4. Signalling validity in the UI itself

If the panel is decoding a data channel, show whether the channel is live rather than
showing garbage:
```glsl
int magic = 0;
for (int i = 0; i < 8; i++) if (bitAt(i)) magic |= (1 << i);
bool ok = (magic == MAGIC);
...
int mask = ok ? segMask(digit) : 0;                       // all segments dark when invalid
col = border ? bezelColor : (ok ? swatch : vec3(0.34, 0.38, 0.31));
```
Users read "the readout went blank" correctly; they read "the readout says 173" incorrectly.

---

## 5. Rounded / circular widgets

For a minimap or a radial gauge, work in normalised device coordinates local to the widget
and test squared distance:
```glsl
vec2 uvn11 = local * 2.0 - 1.0;
float dist = squareWidget ? max(abs(uvn11.x), abs(uvn11.y)) : dot(uvn11, uvn11);
float innerBase = squareWidget ? 0.93 : 0.7;
float outer     = squareWidget ? 0.97 : 0.75;
float inner     = outer - (outer - innerBase) * 0.5;
float ringWidth = outer - inner;

if (dist < inner) { /* content */ }
else if (dist < outer) {                              // three concentric bezel rings
    fragColor = vec4(17.0 / 255.0);
    if (dist > inner + ringWidth * 0.3333 && dist < inner + ringWidth * 0.8333) {
        fragColor = vec4(40.0 / 255.0);
        if (dist > inner + ringWidth * 0.5) fragColor = vec4(70.0 / 255.0);
    }
    fragColor.a = 1.0;
} else discard;
```
Using `dot(uvn11, uvn11)` (squared) rather than `length` keeps the whole comparison
sqrt-free; just remember your thresholds are squared radii.

Rotation helper:
```glsl
mat2 mat2_rotate_z(float radians) {
    return mat2(cos(radians), -sin(radians), sin(radians), cos(radians));
}
```

---

## 6. Text-render-type HUDs

When you control the server, drawing HUD with real font glyphs is far cheaper than
procedural text: emit a bossbar or scoreboard string in a custom font whose glyphs are your
HUD art, and let the shader reposition it.

The protocol in outline:
- Element **identity** is encoded in the glyph's Y position as a bitfield
  (`HEIGHT_BIT 13`, `MAX_BIT 10`); see `data-channels.md § 7`.
- Element **timing** is encoded in the vertex colour: `Color.rg` = birth tick (big-endian
  16-bit), `Color.b` = duration in ticks.
- Element **behaviour** is a `property` bitfield chosen by a `switch (id)` inside the
  shader — so the pack ships a compiled table of element definitions.

Property bits observed:

| Bit | Meaning |
|---|---|
| 0 | sine wave bob: `pos.y += 4.0 * sin((GameTime*1200.0 + pos.x/ui.x) * 3.1415 * 2.0)` |
| 1 | rainbow hash colour |
| 2 | colour jitter |
| 3–6 | slide direction mask (left / right / down / up) |
| 7–12 | easing selector |
| 13 | fade in with the ease |
| 14 | fade out with the ease |
| 15–16 | ping-pong mode |

Easing implementations (all take `progress` in 0..1):
```glsl
if      ((easeType & 1)  > 0) eased = progress;                                   // linear
else if ((easeType & 2)  > 0) eased = progress * progress;                        // quad in
else if ((easeType & 4)  > 0) eased = 1.0 - (1.0 - progress) * (1.0 - progress);  // quad out
else if ((easeType & 16) > 0) { float t = progress;                               // elastic
    eased = (t == 1.0) ? 1.0 : 1.0 - cos(t * 3.14159 * 2.5) * pow(1.0 - t, 2.0); }
else if ((easeType & 32) > 0) { float t = progress;                               // back
    eased = 1.0 + 2.7 * pow(t - 1.0, 3.0) + 1.7 * pow(t - 1.0, 2.0); }
else eased = progress * progress * (3.0 - 2.0 * progress);                        // smoothstep
```
Ping-pong:
```glsl
float t = mod(GameTime, halfCycleGT * 2.0) / halfCycleGT;
float progress = t < 1.0 ? t : 2.0 - t;
if ((pingpong & 2) > 0) progress = progress * progress * (3.0 - 2.0 * progress);
```

Two guards you must not omit:
1. Only touch HUD-space text: `if (pos.y >= ui.y && ProjMat[3].x == -1)`.
2. Handle `GameTime` wrapping at midnight:
   ```glsl
   float elapsed = mod(GameTime - birthGT + 1.0, 1.0);
   if (elapsed > 0.5) elapsed = 0.0;
   ```

`ui` is the GUI extent in "gui pixels", derived from the orthographic projection:
```glsl
vec2 ui = ceil(2.0 / vec2(ProjMat[0][0], -ProjMat[1][1]));
```

---

## 7. Animated glyphs

Turn a single font character into a GIF by packing frames into a sprite sheet and remapping
UVs per frame in the vertex shader. The header lives in the texture itself
(`data-channels.md § 4`).

```glsl
if (decodeProperties(UV0, dim, frame_dim, nframes, loop_time, size, origin)) {
    isAnimated = 1.0;
    float time = fract(GameTime * 1200.0 / loop_time);
    int frame = int(time * float(nframes));
    int uframes = (dim.x - 2) / frame_dim;
    int u = frame % uframes;
    int v = frame / uframes;
    uv = vec2((uv.x - origin.x) / float(dim.x) * float(frame_dim) + origin.x,
              (uv.y - origin.y) / float(dim.y) * float(frame_dim) + origin.y);
    uv += (vec2(u, v) * vec2(frame_dim) + 1.0) / vec2(size);
}
```
Fragment shader picks between the original and remapped UV:
```glsl
vec4 color = texture(Sampler0, isAnimated > 0.5 ? texCoord1 : texCoord0) * vertexColor * ColorModulator;
```
Note `texCoord0` is still passed through unchanged — the header lookup in the vertex shader
needs the raw UV, and keeping both lets the same shader serve normal text.

The sheet generator (`generator.py`, Pillow) is quoted in `architectures.md`.

The 1-pixel border around each frame (`+1` in the UV maths, `width = frames_per_line *
frame_dim + 2`) prevents bilinear bleeding between frames. Always pad sprite sheets you
sample with anything other than `texelFetch`.

---

## 8. Rules of thumb

- **Prefer `texelFetch` over `texture`** anywhere pixel identity matters. Bilinear filtering
  silently destroys packed data, magic pixels, and pixel-art edges.
- **Snap to integers early.** `ivec2 p = ivec2(floor(gl_FragCoord.xy))`, then do all layout
  in `int`. Float layout drifts and produces off-by-one seams at some resolutions.
- **Reference resolution + uniform scale** beats per-axis stretching for anything with text.
- **Discard outside the widget**, do not draw transparent black — you would still pay the
  blend and might disturb a data channel underneath.
- **Every panel that decodes something should be able to say "no signal".**
