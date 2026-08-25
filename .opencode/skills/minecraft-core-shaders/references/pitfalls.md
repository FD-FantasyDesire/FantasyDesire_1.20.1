# Pitfalls and debugging

Symptom-first. Most core-shader bugs in the wild are on this list.

**Before working through it by hand:** if you can run Fabric, inject RenderDoc with the
GFX-Debuggers mod and capture a frame (`instrumentation.md § 2`). Half of the symptoms below
— nothing renders, wrong target, wrong order, wrong uniform, geometry in the wrong place —
are answered by looking at the capture instead of reasoning about it.

---

## Nothing changed at all

- **Wrong generation.** `#moj_import <fog.glsl>` on G3+, or `#moj_import <minecraft:fog.glsl>`
  on G1 — the shader fails to compile and the game falls back. Check `latest.log`.
- **A core `.json` on G3+.** Pipeline JSONs in `shaders/core/` are ignored from 1.21.6; if
  your change lived in the JSON (a new uniform, a blend mode), it did nothing.
- **Missing `.json` on G1/G2.** A `.vsh`/`.fsh` with no matching pipeline JSON is not loaded
  at all on those versions.
- **A post effect in the wrong directory.** `shaders/post/x.json` (G1) vs
  `post_effect/x.json` (G2+). Both directories can exist; only one is read.
- **Sodium.** Sodium replaces terrain rendering and ignores `rendertype_solid` /
  `rendertype_cutout` / `rendertype_translucent` entirely unless the user has
  `sodiumcoreshadersupport` or equivalent. There is no pack-side fix.
- **Not Fabulous.** Post pipelines built on `transparency` only run on Graphics = Fabulous!
- **Iris.** Iris takes over the whole pipeline; core shaders are bypassed. There is no
  pack-side fix; packs drop Iris support rather than fight it.
- **Pack order.** A pack lower in the list is overridden by anything above it.
- **`no_` prefix.** Files named `no_rendertype_text_background.vsh` are disabled-by-rename;
  make sure you did not copy one.

## Black screen

- A shader compiled but writes garbage — usually a NaN. Sources: `acesInverse` without a
  clamp, `1.0 / dir.z` with `dir.z == 0`, `inverse()` of a matrix decoded from an
  uninitialised data strip, `log()` of zero in a float printer.
- `discard` in every branch of a fragment shader that also owns the vanilla path.
- A post pass reading and writing the same target (feedback loop).
- On G3+, redeclaring a UBO member as a loose `uniform` — some drivers link it and give you
  zeros rather than erroring.

## Flicker, or works only sometimes

- **Uninitialised persistent target.** Add a canary pixel and validate it
  (`data-channels.md § 9`).
- **The marker block left the frustum.** If the data strip's carrier is a block model, the
  strip vanishes the instant no such block is visible. Use a carrier you control, or fall
  back gracefully when the magic word is absent.
- **Two features claimed the same magic value.** Audit the allocation table.
- **Mipmap bleeding.** `texture()` on an atlas at a distance blends your tag with the
  neighbouring sprite. Use `textureLod(..., -4)` or `texelFetch`.
- **`GameTime` wrapped.** Any `GameTime`-based elapsed-time computation needs the wrap
  guard: `float e = mod(GameTime - birth + 1.0, 1.0); if (e > 0.5) e = 0.0;`
- **Fixed-point overflow.** A value exceeded `±(2^23 / scale)` and wrapped. Symptom: the
  effect is correct at some FOVs/positions and wildly wrong at others.

## The effect is offset, mirrored, or upside down

- **Corner order.** Vanilla quads are top-left, bottom-left, bottom-right, top-right for
  most render types, but not all. Print `gl_VertexID % 4` as a colour to check.
- **Y direction.** `gl_FragCoord.y` is bottom-up; texture V and UI layout are usually
  top-down. Convert explicitly:
  `ivec2 fragCoord = ivec2(gl_FragCoord.x, screenSize.y - 1 - floor(gl_FragCoord.y));`
  On G5 the NDC y direction depends on the backend — expose a flip uniform.
- **Half-texel.** `texelFetch` indexes texel centres; `texture` samples at UV. Converting
  between them needs the `-0.5` / `+0.5`:
  `vec2 uv = screenSpace * OutSize - 0.5; ivec2 c = ivec2(floor(uv)); vec2 f = uv - c;`
- **TAA jitter.** `ProjMat[3][0]` and `[3][1]` carry a sub-pixel jitter. Zero them before
  using the matrix for reprojection, or store the un-jittered values separately.
- **`transpose`.** `ModelViewMat`'s rotation is often stored transposed in a data marker;
  the decode side must match the encode side. Pick one convention and write it in the marker
  layout comment.

## Precision problems

- Float `Position` above ~2^24 loses integer resolution. Position-band HUD schemes that use
  10^6 gaps are near the edge; do not stack more than a few bands.
- `Color` is byte-quantised; you get 256 values per channel, and `round`, not `floor`, is
  the correct recovery: `floor(Color.r * 255.0 + 0.5)`.
- `UV2 / 16` is integer division; on some drivers `UV2` arrives already divided. Follow the
  convention of the version you target rather than assuming.
- Comparing decoded floats for equality only works with the lossless `packF32toF8x4` codec.

## Performance

- A `for` loop over a large constant in a fragment shader is fully unrolled by the driver.
  128-iteration palette searches are fine at 128×128 but not full screen.
- Every extra post pass is a full-screen read + write. Merge passes where you can; the
  packs routinely fold "preserve the marker row" and "do the work" into one pass.
- `gl_FragDepth` disables early-z for the entire shader. Only accept that when you are
  deliberately using depth as a data channel.
- The expensive part of a deferred renderer is the extra scene render, not the post passes.
  Budget accordingly.
- Fixed-size targets (`{"width": 4000, "height": 3000}`) cost that much VRAM regardless of
  window size.

## Compatibility

- **OptiFine** conflicts with core shaders; packs that must support both ship an
  `optifine/` fallback (`emissive.properties` and CIT definitions).
- **AMD drivers** are stricter about implicit conversions and about `switch` on non-constant
  expressions. Ambitious packs ship a simplified fallback pass chain and a separate
  "AMD compatibility" download. If you hit driver-specific breakage, simplify control flow
  before assuming a logic bug.
- **Integer division and modulo of negatives** differ from what you expect: `-1 / 16 == 0`,
  not `-1`. Use `floor()` on floats, or the `x -= int(x < 0)` correction
  (`voxelPos -= ivec3(lessThan(fragPos, vec3(0.0)));`).
- `#version 150` does not have `floatBitsToUint`, `uintBitsToFloat`, bitwise ops on `uint`,
  or `textureSize` on some samplers. Bump to `#version 330` when you need them — it is
  accepted in every generation.

## Process mistakes

- **Editing minified shaders in place.** Production packs are often minified to one statement
  per file. Reformat, edit, re-minify; do not try to patch a 3 KB line.
- **Hand-writing generated files.** If you find yourself editing the 400th block model,
  stop and write the generator.
- **Not committing the generator.** Any encoded asset — sprite sheets, palettes, mesh
  textures — needs its generator committed beside it so it can be regenerated.
- **Undocumented magic values.** Keep an allocation table in a header comment. The
  everything-packs are one collision away from a very confusing bug.
- **Silently dropping a version.** If a change only works on one generation, say so in the
  pack description and gate it behind an overlay.

---

## Reading `latest.log`

Shader compile errors look like:
```
[Render thread/ERROR]: Couldn't parse shader minecraft:core/rendertype_text
com.mojang.blaze3d.shaders.CompilationException: '' :  syntax error, unexpected ...
```
and pipeline errors like:
```
[Render thread/WARN]: Shader rendertype_cutout could not find sampler named Sampler2 ...
```
`Ctrl+F` for `ERROR` and `Shader` after every reload. The game will often keep running with
a broken shader, so an unchanged screen is not evidence of success.

---

## A quick self-audit for any shader you write

1. Does every code path assign `gl_Position` (vertex) or `fragColor` / `discard`
   (fragment)?
2. Are the vanilla varyings computed **before** the first early return?
3. Is every magic comparison exact, on integers, after `round`?
4. Is every decoded value validated before use?
5. Does anything read row 0 of a target that holds a data marker?
6. Are the includes the right generation's?
7. Would this still be correct at 4:3, at 21:9, and at 320×240?
8. Would this still be correct one frame after a window resize?
