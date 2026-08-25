# Architectures — how whole systems are put together

Individual tricks are in `techniques-index.md`. This file is about **composition at the
system level**: the recurring shapes that real packs take, what interlocks with what, and
what each shape costs. Match your requirement to an archetype before you start building.

Seven archetypes, roughly in order of ambition.

---

## 1. The single-effect pack

**Shape.** One or two core shaders, no post pipeline, no server half.
**Examples of the class.** Fixed or dynamic FOV, emissive textures, an animated font glyph,
a texture-tagged shader effect, a skybox.

```
pack.mcmeta
assets/minecraft/shaders/core/<one or two>.{vsh,fsh}[,.json]
assets/minecraft/shaders/include/<feature>.glsl
[textures / models the effect needs]
```

**Key property: works on every graphics setting.** No Fabulous requirement, no extra passes,
no compatibility caveat beyond Sodium/Iris. If a requirement can be met this way, meet it
this way.

**The design question** is always "what identifies my content?" — a texture tag, a vertex
colour, a position band. Get that right and the rest is a few lines.

**Watch out for:** the same shader running in the GUI, in the hand, in an item frame. One
unguarded case is the usual bug.

**Effort:** hours to a day.

---

## 2. The HUD framework

**Shape.** A position/identity encoding scheme, one text-family core shader, and a server
that emits elements.

```
server ──(entity Y-band or glyph position + vertex Color)──► core/rendertype_text.vsh
                                                                │
                                                        screen-space quad
```

**The three sub-decisions:**

1. **Transport.** Text displays (world-anchored, interpolated, per-entity `Color`) versus
   bossbar/scoreboard glyphs (no entities, packet-rate updates, GUI-space only). Glyphs win
   on cost; displays win on flexibility.
2. **Identity encoding.** Position bands are simple and robust for a handful of anchors; a
   bitfield packed into the Y coordinate scales to hundreds of distinct elements.
3. **Animation.** Send *parameters*, not frames. A birth tick plus a duration in the vertex
   colour lets the shader interpolate at frame rate from a single 20 Hz update
   (`techniques-index.md § A10`, `§ A11`).

**The element table.** Once there are more than a dozen elements, the shader ends up with a
`switch (id)` selecting layout, colour and animation flags. **Generate that table** from the
server's configuration; hand-maintaining it does not scale. A macro-based DSL keeps the
generated part readable (`techniques-index.md § C1`).

**Watch out for:** the `GameTime` midnight wrap, GUI-scale changes, and elements leaking into
inventory screens.

**Effort:** days for the core, then linear in element count.

---

## 3. The everything-pack (server feature library)

**Shape.** Ten to thirty independent features sharing three or four core shaders, each
recognised by its own magic value, dispatched through an early-out chain.

```
core/entity.vsh          core/rendertype_text.vsh      core/rendertype_item_entity_*.vsh
      │                          │                              │
      └──────────── include/lib/<feature>.glsl × N ─────────────┘
                    each exposing make_<feature>() -> bool
```

Typical feature set: waypoints, minimap, HUD, first-person view models, skybox, emissive
textures, screen overlays, text effects, video playback, 2-D side worlds, entity highlights.

**This archetype lives or dies on discipline, not on tricks.** Read `composition.md` in full
before starting one. The three things that kill it:

- **Varying budget.** Twenty features × three varyings each exceeds the hardware limit and
  fails as an unexplained link error.
- **Magic-value collisions.** Two features claiming the same texel value produces
  intermittent, untraceable misbehaviour.
- **Ordering.** A feature that mutates state and then declines to claim corrupts everything
  after it.

**The payoff** is that features compose: a waypoint can be emissive, a HUD element can have
text effects, all without any feature knowing about the others.

**Effort:** weeks for the framework, then days per feature.

---

## 4. The screen-space post effect

**Shape.** A data marker feeding a post pipeline that reconstructs world space from depth.

```
core/terrain.{vsh,fsh}  ──marker quad──►  40px data strip in minecraft:main
                                                │
post pipeline: [G-buffer] ──► [effect] ──► [temporal] ──► composite ──► main
                   ▲                           │
              prev* targets ───────────────────┘
```

**The invariant parts** (build these first, they are the same for every effect):
- a marker carrier on a guaranteed-present draw,
- an encode/decode pair for the matrices,
- world-position reconstruction from depth,
- a normal buffer derived from depth,
- history targets with a canary.

**The variable part** is one kernel: occlusion, blur, velocity, reflection.

**Requires Fabulous.** State it in the pack description; it is the single most common
"why doesn't this work" report for this archetype.

**Watch out for:** the marker row leaking into samples (clamp), TAA jitter in the decoded
projection (zero `[3].xy`), history buffers surviving a resize (canary).

**Effort:** the scaffolding is a few days; each additional effect on top is much cheaper.

---

## 5. The voxel / spatial-data pack

**Shape.** Every block in range writes one pixel; the framebuffer becomes a 3-D data
structure the post pipeline can index.

```
core/terrain.vsh  ── one 1px quad per block ──►  linearised voxel grid in a target
                                                        │
                              flood fill / accumulate / reproject (N passes)
                                                        │
                                    lookup during the lighting or overlay pass
```

**Two variants:**
- **2-D projection** (world XZ → pixel XY, height in NDC z). The depth test performs the
  "topmost block" reduction for free. This is the shape of a GPU-built world map.
- **3-D linearised grid** (a cube of blocks flattened into scanlines). This is the shape of
  voxel lighting.

**The three constraints that shape the design:**
1. **Addressing must be checkerboarded** — neighbouring voxels must not be neighbouring
   pixels, or blur and flood-fill passes leak between unrelated cells.
2. **Grid size is derived from the framebuffer**, so it changes when the window changes. Copy
   into a fixed-size history target if the data must survive a resize.
3. **Every participating block needs a marker model.** That means generating hundreds or
   thousands of model JSONs — write the generator, commit the generator.

**Cost:** one extra quad per visible block, plus the passes. Noticeable at high render
distance.

**Effort:** the addressing is the hard part; budget a week before anything renders correctly.

---

## 6. The deferred renderer

**Shape.** The post pipeline stops being an effect and becomes the lighting model.

```
core/*  ──► albedo + tags ──► main
                              │
   normals ─► shadow map ─► shading ─► volumetrics ─► reflections ─► bloom ─► grade ─► main
                  ▲
        an extra scene render, forced by a pass sentinel
```

Interlocking systems, all of which must exist together:

| System | Depends on |
|---|---|
| Normals from depth | matrices via data marker |
| Shadow map | an extra scene render with a substituted orthographic projection |
| Shadow persistence | packing depth to RGBA8 and restoring it via `gl_FragDepth` |
| Soft shadows | a per-fragment PRNG and a tangent basis |
| Volumetrics | the shadow map, plus a phase function |
| Sun colour | a transmittance LUT indexed by elevation |
| Reflections | screen-space raymarch with thickness rejection |
| Tone mapping | an invertible curve, applied consistently everywhere |
| Flattened vanilla lighting | an overridden `light.glsl` so the lightmap stops colouring the scene |

**The last row is easy to miss and essential.** If vanilla lighting still tints everything,
your deferred lighting is fighting it. Override the lightmap sampler to return luminance
only, and re-colour from your own light sources.

**Pass sentinels.** The extra scene render is triggered by the server putting the client into
a recognisable state; every core shader dispatches on it. Choose sentinel values no natural
state produces, and **measure them on the target version** rather than copying constants.

**Cost:** dominated by the extra scene render, not the passes. Expect to halve frame rate per
extra view and to ship with an explicit "reduce your render distance" instruction.

**Effort:** months. This is the deep end.

---

## 7. The server-driven data channel

**Shape.** A plugin owns a data stream; the shader is a decoder and renderer.

Three transports, in increasing modernity:

| Transport | Payload | Latency | Availability |
|---|---|---|---|
| Map items | 16 KB per map | packet | all versions |
| Entity attributes (`Color`, `brightness`, position) | tens of bits per entity | 1 tick | all versions |
| Named post-effect toggles | 1 bit per declared effect | packet | 26.2+ |

**The map-item variant** is the highest bandwidth: a palettised image the server rewrites and
the shader decodes. It supports arbitrary screen content — full-colour images, compressed
tile-and-palette formats, even video. Design the wire format deliberately: a magic word, a
version byte, then payload, and **fail loudly** rather than rendering garbage.

**The post-effect-toggle variant** is the cleanest modern answer: one tiny post effect per
bit, a header effect that blanks the strip and stamps a magic pattern, and a decoder effect
that grabs the strip into a scratch target, patches it out of the visible image, and renders
the UI. Latency is one packet and cost is negligible.

**Always include:**
- a magic/version header, so a stale or foreign payload is rejected,
- a visible "no signal" state, so users read a dead channel correctly,
- a plan for the strip being visible (hide it, patch it, or put it under the hotbar).

**Effort:** the encoder is usually more work than the shader.

---

## Choosing an archetype

| Requirement | Archetype |
|---|---|
| Change how something already on screen looks | 1 |
| Show information to the player | 2, or 7 on modern versions |
| A server pack with many unrelated features | 3 |
| An image-space effect (AO, blur, glow, distortion) | 4 |
| Something that needs to know about the world beyond one pixel | 5 |
| Replace the lighting model | 6 |
| Stream arbitrary data from a plugin | 7 |

Most real packs are two or three of these stacked. A server pack is typically 2 + 3 + 7; a
graphics pack is 1 + 4, sometimes reaching 5 or 6.

**Rule of thumb on ambition:** archetypes 1–4 are a solved problem — the patterns in this
skill get you there reliably. Archetype 5 is hard but tractable. Archetype 6 is a project,
not a feature; do not promise it casually.
