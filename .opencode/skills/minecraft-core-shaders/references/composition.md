# Composition — putting many features in one pack

A single-feature pack is easy. A production server pack has ten or twenty features sharing
three or four shader files, and the hard part stops being any individual trick and becomes
**keeping them from destroying each other**.

This is the discipline every large shipping pack runs on. Adopt it from the first feature,
not the fifth — retrofitting is far more expensive.

---

## 1. The feature module contract

Every feature is one include exposing a boolean entry point per stage. The entry point
returns `true` if it **claimed** this vertex/fragment.

```glsl
// include/lib/myfeature.glsl
#version 150

#ifndef LOSTWAR_MYFEATURE_INCLUDED
#define LOSTWAR_MYFEATURE_INCLUDED

// ---- recognition: cheap, exact, and unique to this feature ----
bool is_myfeature(sampler2D tex, ivec2 pixel) {
    return ivec4(texelFetch(tex, pixel, 0) * 255.5) == ivec4(157, 211, 147, 99);
}

#ifdef VSH
bool make_myfeature() {
    if (!is_myfeature(Sampler0, ivec2(UV0 * textureSize(Sampler0, 0)))) return false;
    // rewrite gl_Position and any varyings this feature owns
    return true;
}
#endif

#ifdef FSH
bool shade_myfeature(inout vec4 color) {
    if (myfeature_flag < 0.5) return false;
    // ...
    return true;
}
#endif

#endif
```

Rules:
- **One recognition predicate**, separated from the action, so it can be tested and reused.
- **Never write a varying you do not own.** Ownership is per feature; shared varyings
  (`vertexColor`, `texCoord0`) are written by the vanilla path only, then optionally
  *overwritten* by the claiming feature.
- **No side effects before the guard.** A feature that modifies state and then returns
  `false` is the worst bug class in this discipline.
- Guard the file with an include sentinel; includes get pulled in more than once as the
  dependency graph grows.

---

## 2. The dispatch chain, and its ordering rules

```glsl
#define VSH
#moj_import <lib/skybox.glsl>
#moj_import <lib/waypoint.glsl>
#moj_import <lib/hud.glsl>
#moj_import <lib/2d.glsl>

void main() {
    // 1. vanilla path FIRST, in full
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = fog_distance(Position, FogShape);
    vertexColor    = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color)
                   * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
    // 2. every feature's varyings reset to a neutral value
    isMyFeature = 0; isVideo = 0; is2D = 0;

    // 3. the chain
    if (make_skybox())   return;
    if (make_waypoint()) return;
    if (make_hud())      return;
    if (make_2d())       return;
}
```

**Ordering rules, in priority order:**

1. **Vanilla path and all varying resets come before the chain.** Every early `return` must
   leave a completely valid varying set behind. This is the single most common structural
   bug in new packs.
2. **More specific recognition first.** A feature keyed on an exact 4-byte texel outranks
   one keyed on a position band, which outranks one keyed on a coordinate range.
3. **Screen-space escapes before world transforms.** Anything that abandons the world
   (HUD, waypoint, fullscreen canvas) should claim before anything that merely modifies the
   world transform (FOV override, 2-D projection), because the latter is wasted work.
4. **Cheap recognition before expensive recognition.** A `Color.rgb ==` compare costs
   nothing; a `textureSize` + `texelFetch` costs a sample. Order the chain so the common
   case exits early.
5. **Mutually exclusive features must be provably mutually exclusive.** If two can both
   return `true`, you have a latent bug that will appear the day someone combines them
   in-game. Document the exclusion or make the recognition disjoint.

Fragment side is the same shape, but `inout`-style rather than `return`-style, because
several features can legitimately contribute to one pixel (emissive + outline + tint):

```glsl
void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.1) discard;

    make_emissive(color, pureColor, tintColor);   // modifies
    make_2d_tint(color, tintColor);               // modifies
    if (shade_waypoint(color)) { fragColor = color; return; }   // claims

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
```
Decide per feature whether it **claims** (returns, no fog) or **contributes** (modifies,
falls through) and say so in its header comment.

---

## 3. The varying budget — a real ceiling nobody warns you about

Interpolators are a fixed hardware resource. The practical GL limit is around **32 `vec4`
slots** (`GL_MAX_VARYING_COMPONENTS` = 128 floats) [I — measure on your target if you are
near it]. Exceeding it is a **link failure**, logged as a shader error with no hint about
which variable pushed you over.

Counts observed in shipping multi-feature packs, in `out`/`flat out` declarations:

| Shader | Declarations |
|---|---|
| an everything-pack item-entity shader | 17 |
| an everything-pack text shader | 15 |
| a deferred-renderer block shader | 12 |
| an FPS-pack entity shader | 11 |
| a text-effect subsystem on its own | 12 |

A `mat4` varying costs **4 slots**. A post shader passing two `flat out mat4`s plus three
vectors is already at 11 slots from five declarations.

### Compression techniques, in order of preference

**1. Pack flags into one `flat out int` bitfield.**
```glsl
flat out int featureFlags;
#define F_VIDEO   (1 << 0)
#define F_2D      (1 << 1)
#define F_WAYPOINT (1 << 2)
#define F_HIGHLIGHT_MASK (3 << 3)      // 2 bits of highlight id
// vertex:   featureFlags = F_VIDEO | (highlightId << 3);
// fragment: if ((featureFlags & F_VIDEO) != 0) …
```
Packs routinely declare five or six separate `flat out int`s where one bitfield would do;
merging them is usually the single biggest saving available.

**2. Merge the per-corner varyings.** The B3 trick normally costs four `vec3`s. If you only
need the *bounds*, compute min/max in the vertex shader is impossible (no cross-vertex
access) — but you can halve it by packing screen position and UV into one `vec4` per corner
instead of two `vec3`s:
```glsl
out vec4 corner1;   // xy = clip pos, zw = uv    (was vec3 ipos1 + vec3 uvpos1)
```
and carry the `w`-guard separately as a single `flat out int` corner index.

**3. Reconstruct instead of interpolating.** Anything derivable in the fragment shader from
`gl_FragCoord`, a uniform, or a texture fetch should not be a varying. Pass-through varyings that merely forward an attribute
the fragment shader could refetch are pure waste.

**4. `#ifdef` unused features out entirely.**
```glsl
#ifdef FEATURE_VIDEO
flat out int videoFrameCount;
flat out int videoInitialFrame;
flat out int videoFlags;
#endif
```
Combined with the pack-level feature switches (`§ 5`), a pack that ships 20 features but
enables 6 per render type stays well under the ceiling.

**5. Split render types.** On G1/G2 you can point different render types at different shader
files (`rendertype-inventory.md § 4`), so features that never co-occur never share a
varying budget. On G3+ this is not available — use `#ifdef` instead.

### Symptom
A link error mentioning varyings, or a shader that compiles on NVIDIA and fails on Intel.
Intel drivers typically expose the smallest limits; test there if you can.

---

## 4. The magic-value registry

Every feature consumes identifiers from four shared namespaces. Collisions are silent,
intermittent, and extremely hard to diagnose. Keep a registry at the top of the pack's main
include and **treat it as the source of truth**.

```glsl
// ============================================================================
//  MAGIC VALUE REGISTRY  --  edit here first, then in the feature
// ============================================================================
//  TEXTURE RGB
//    (76,195,86)      data marker          lib/datamarker.glsl
//    (157,211,147)a99 waypoint             lib/waypoint.glsl
//    (255,0,0)a1      third-person only    lib/thirdperson.glsl
//    (112,108,138)    minimap carrier      lib/minimap.glsl
//  TEXTURE ALPHA
//    246..250         shadertoy effect id  lib/effects.glsl
//    251,252          emissive variants    lib/emissive.glsl
//    253              RESERVED
//    254              raw vertex tint      lib/effects.glsl
//  VERTEX COLOR RGB
//    0xCAFEBA         skybox               lib/skybox.glsl
//    0x00BEEF         video player         lib/video.glsl
//    0xABCDEF         highlight 1          lib/highlight.glsl
//  POSITION BANDS (world Y)
//    < -1e6           HUD, anchor in band  lib/hud.glsl
//    < -1024 (scaled) first-person model   lib/firstperson.glsl
//  SCREEN PIXELS (row 0)
//    x 0..19          data marker          lib/datamarker.glsl
//    x 20..27         frame counter        lib/temporal.glsl
// ============================================================================
```

Rules:
- **Exact integer comparison only**: `ivec4(round(x * 255.0)) == ivec4(...)`.
- **Two independent checks** before acting: a magic word plus a per-pixel tag, or a magic
  plus a range assertion.
- **Do not reuse values from other packs** if yours might run alongside them. Values already
  taken in the wild are listed in `new-projects.md § 4`; pick outside that set.
- Alpha 230–250 is claimed by the bloom-emission convention and 248–252 by the dynamic
  emissive convention. If you use both, reconcile them explicitly.
- Reserve a block per feature rather than allocating one value at a time; renumbering later
  means touching art assets, not just code.

### The four namespaces and their collision behaviour

| Namespace | Collides when | Typical symptom |
|---|---|---|
| Texture RGB / alpha | two features tag the same texel value | one feature randomly triggers on the other's art |
| Vertex `Color` | a plugin tints an entity to your magic colour | feature fires on unrelated entities |
| Position bands | two features use overlapping Y ranges | HUD elements teleport |
| Screen pixels (row 0) | two data markers overlap | garbage matrices, effect flickers |

The vertex-colour namespace is the most dangerous, because a server operator can produce your
magic colour by accident. Prefer texture tags for anything the operator does not control.

---

## 5. Feature isolation and bisection

Every feature must be independently switchable at compile time:

```glsl
// in the core shader, before the import
#define FEATURE_WAYPOINT
#define FEATURE_HUD
// #define FEATURE_VIDEO          <- disabled

// in the dispatcher include
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

This buys three things:
1. **Bisection.** Something broke; halve the enabled set, reload, repeat. Minutes instead of
   hours.
2. **Budget control.** Varyings and instruction count scale with enabled features, not
   shipped features.
3. **Per-render-type tailoring.** `core/entity` enables the entity features;
   `core/rendertype_text` enables the text features; one library serves both.

Keep a `features.glsl` per render type listing exactly what that shader enables, so the set
is visible in one place rather than scattered through `#define`s.

---

## 6. Shared state instead of parameter soup

Once four features need the same derived values (screen size, GUI flag, decoded region bits,
glyph bounds), stop passing them around and put them in one struct populated once.

A text-effect system, for example:
```glsl
struct TextData {
    vec4 color, topColor, backColor;
    vec2 position, characterPosition, localPosition;
    vec2 uv, uvMin, uvMax, uvCenter;
    float zDepth;
    bool isShadow, doTextureLookup, shouldScale;
};
TextData textData;      // file-scope, populated once before the dispatch chain
```
Every effect verb then reads and writes `textData` rather than taking twelve parameters.
This is what makes a 20-verb effect vocabulary tractable.

Apply the same idea to the vertex stage:
```glsl
struct Ctx {
    vec3 worldPos;
    ivec3 region;
    int  isGUI;
    vec2 guiSize;
};
Ctx ctx;
```
Compute it once at the top of `main()`; features read it instead of recomputing
`ProjMat[2][3] == 0.0` five times.

---

## 7. Include layering

```
include/
  lib/util.glsl            no dependencies; maths, colour, noise, easing
  lib/encodings.glsl       no dependencies
  lib/ctx.glsl             depends on: nothing but attributes/uniforms
  lib/<feature>.glsl       depends on: util, encodings, ctx
  features.glsl            the #define set for one render type
  dispatch.glsl            depends on: every enabled feature
```

Rules:
- **A feature never includes another feature.** If two need the same helper, the helper moves
  down a layer.
- **Include sentinels everywhere** (`#ifndef X_INCLUDED`), because the graph will produce
  duplicate imports.
- `#version` at the top of every include; the preprocessor tolerates repeats, and it keeps
  each file independently readable.
- Stage guards (`#ifdef VSH` / `#ifdef FSH`) inside the feature file, not at the call site.

---

## 8. Test matrix

A feature is not done until it has been checked in every context its carrier appears in.

| Context | What breaks if unguarded |
|---|---|
| World, third person | usually fine |
| World, first person | hand-pass detection differs |
| Held in hand | item model draws with a different projection |
| Inventory / GUI slot | `ProjMat[2][3] == 0.0`; markers fly off, HUD elements appear in slots |
| Item frame / on the ground | different render type, unexpected `Color` |
| Another player's inventory preview | GUI again, different lighting |
| Under water / in lava / with blindness | fog uniforms change; sentinel detectors misfire |
| 4:3, 16:9, 21:9, tiny window | aspect-derived layout drifts |
| GUI scale 1 / 2 / 3 / auto | `ui` size changes |
| Midnight (`GameTime` wrap) | timeline features stick at 100 % |
| After F3+T and after a resize | persistent targets reset |

Run the matrix per feature, not per pack — a feature added last can break one added first,
and only the matrix catches it.

---

## 9. Merging two packs

When a server needs your pack plus two plugin-generated packs, they will all want the same
files.

1. **Diff the shader file lists.** Overlap is the whole problem; disjoint files merge freely.
2. **Reconcile the magic registries** before touching code. Renumber the pack you control.
3. **Merge the dispatch chains**, not the files: take the other pack's `make_*()` functions
   as modules and insert them into your chain at the right priority.
4. **Sum the varying budgets** and compress (`§ 3`) before you find out the hard way.
5. **Check the pipeline JSONs** on G1/G2 — two packs both defining
   `post_effect/transparency.json` means one silently wins. The winner must supply every
   pass both packs needed.
6. **Preserve upstream attribution comments.**
7. Re-run the test matrix for the union, especially the fog/GUI contexts where two features'
   detectors can now both fire.

If the other pack is generated by a plugin, you cannot edit it — you must adapt to its
conventions. Read its shaders first and treat its magic values as reserved.

---

## 10. Composition checklist

- [ ] Every feature is one include with a recognition predicate and a stage-guarded entry
      point.
- [ ] The vanilla path and every varying reset run before the dispatch chain.
- [ ] No feature has side effects before its guard.
- [ ] Chain order follows the five rules in `§ 2`.
- [ ] Varying count counted, `mat4`s counted as 4, headroom left for the next feature.
- [ ] Flags packed into a bitfield rather than one varying each.
- [ ] Magic-value registry present, exact-integer compares, two-check validation.
- [ ] Every feature switchable by `#define`, with a per-render-type feature list.
- [ ] Shared derived state in one struct, computed once.
- [ ] Include layering respected; no feature-to-feature imports.
- [ ] Test matrix run for the union of features.
