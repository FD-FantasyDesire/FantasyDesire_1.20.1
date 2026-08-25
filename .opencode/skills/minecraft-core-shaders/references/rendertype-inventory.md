# Render-type inventory — what to hijack, and what you get

Choosing the carrier render type is the first real decision in any pack. This table answers
it. Names and availability are **[V]** observed directly; vertex formats and target routing
are **[D]** unless noted.

---

## 1. How render types map to shader files, per generation

**G1 (≤ pf 41)** — one `.vsh`/`.fsh`/`.json` triple per render type. Full vanilla list, as
as shipped by the game (48 files):

```
particle  position  position_color  position_color_lightmap  position_color_tex_lightmap
position_tex  position_tex_color
rendertype_armor_cutout_no_cull   rendertype_armor_entity_glint
rendertype_beacon_beam  rendertype_breeze_wind  rendertype_clouds  rendertype_crumbling
rendertype_cutout  rendertype_cutout_mipped  rendertype_end_portal  rendertype_energy_swirl
rendertype_entity_alpha  rendertype_entity_cutout  rendertype_entity_cutout_no_cull
rendertype_entity_cutout_no_cull_z_offset  rendertype_entity_decal  rendertype_entity_glint
rendertype_entity_glint_direct  rendertype_entity_no_outline  rendertype_entity_shadow
rendertype_entity_smooth_cutout  rendertype_entity_solid  rendertype_entity_translucent
rendertype_entity_translucent_cull  rendertype_entity_translucent_emissive
rendertype_eyes  rendertype_glint  rendertype_glint_translucent
rendertype_item_entity_translucent_cull  rendertype_leash  rendertype_lightning
rendertype_lines  rendertype_outline  rendertype_solid  rendertype_text
rendertype_text_background  rendertype_text_background_see_through  rendertype_text_intensity
rendertype_text_intensity_see_through  rendertype_text_see_through  rendertype_translucent
rendertype_translucent_moving_block  rendertype_tripwire  rendertype_water_mask
```

**G2 (pf 42–55)** — the same render-type *names* remain as `.json` files, but they point at
a handful of shared sources with `defines`:
- `core/terrain` ← `rendertype_solid`, `rendertype_cutout`, `rendertype_cutout_mipped`,
  `rendertype_translucent`, `rendertype_tripwire` [V]
- `core/entity` ← the whole `rendertype_entity_*` family [V]
- `core/rendertype_text*`, `core/particle`, `core/position*` keep their own sources
- You may also point a `.json` at **your own** file: `"vertex": "minecraft:text/hand"` [V]

**G3+ (pf ≥ 56)** — no `.json` in resource packs. You override the shared sources only.
Observed file set in a G4 production pack (pf 75):
```
entity  terrain  particle  glint  gui  panorama  screenquad  sky  stars
position  position_color  position_tex  position_tex_color
rendertype_beacon_beam  rendertype_clouds  rendertype_crumbling  rendertype_end_portal
rendertype_entity_alpha  rendertype_entity_cutout  rendertype_entity_decal
rendertype_entity_shadow  rendertype_item_entity_translucent_cull  rendertype_leash
rendertype_lightning  rendertype_lines  rendertype_outline
rendertype_text  rendertype_text_background  rendertype_text_background_see_through
rendertype_text_intensity  rendertype_text_intensity_see_through  rendertype_text_see_through
rendertype_translucent_moving_block  rendertype_water_mask  rendertype_world_border
```
Note the additions in this era: `gui`, `panorama`, `screenquad`, `sky`, `stars`, `glint`,
`rendertype_world_border`.

**Practical rule:** on G3+, overriding `core/entity` affects *every* entity render type, so
your code must be correct under every `#ifdef` combination. Prefer gating on a magic value
that only your content has.

---

## 2. Carrier selection table

| Carrier | Render type | Attributes you get | Lands in | Available in GUI? | Best for |
|---|---|---|---|---|---|
| **Block face** | `terrain` / `rendertype_solid`, `cutout`, `cutout_mipped` | Position, Color, UV0, UV2, Normal | `main` | yes (inventory icon) | data markers, voxelization — anything that must exist wherever blocks are |
| **Item display entity** | `rendertype_item_entity_translucent_cull` | Position, Color, UV0, UV1, UV2, Normal | **`item_entity`** (Fabulous) | yes | control planes, waypoints, view models, skyboxes-on-items — *isolated from the world in post* |
| **Text display / bossbar / scoreboard** | `rendertype_text`, `*_see_through` | Position, Color, UV0, UV2 | `main` | yes | HUDs, high glyph throughput, map contents |
| **Map item contents** | `rendertype_text` | as above | `main` | yes | bulk server→client pixels |
| **Armor stand / mob** | `rendertype_entity_*` | full set | `main` | yes | anything that should be lit and sorted like an entity |
| **Sun / moon quad** | `position_tex_color` | Position, UV0, Color | `main` | no | skyboxes, a time-of-day clock, anything behind all geometry |
| **Sky quad** | `sky` | Position | `main` | no | replacing the sky colour outright |
| **Stars** | `stars` | Position | `main` | no | usually: deleting them |
| **Particles** | `particle` | Position, Color, UV0, UV2 | `particles` | no | a second isolated data plane |
| **Held item / hand** | entity family, detect the hand pass | full set | `main`, **after post** | n/a | view models, full-screen overlays that must escape post |
| **GUI widgets** | `gui`, `position_tex`, `position_color`, `position_tex_color` | varies | after post | n/a | inventory/menu changes only |
| **Entity outline** | `rendertype_outline` → `entity_outline` target | Position, Color, UV0 | `entity_outline` | no | an always-on post hook |

### Why `rendertype_item_entity_translucent_cull` is the community favourite

Three properties at once [V]:
1. It is an **item display**, so a plugin or datapack can place it anywhere with full
   control of position, rotation, scale and tint.
2. In the Fabulous pipeline it lands in **its own target**, so a post pass can read it
   without the world in the way, which is what makes an isolated control plane possible.
3. It carries the full attribute set including `Normal` and `Color`.

Its cost: it is translucent-sorted, so a lot of them is expensive, and it only exists where
the server puts one.

---

## 3. Vertex formats — what you can actually read

| Attribute | Present in | Type |
|---|---|---|
| `Position` | everything | `vec3` |
| `Color` | most; **not** `position`, `position_tex` | `vec4` (byte-quantised) |
| `UV0` | anything textured | `vec2` |
| `UV1` | entity family only (overlay) | `ivec2` |
| `UV2` | anything lit (terrain, entity, text, particle) | `ivec2` |
| `Normal` | terrain, entity family | `vec3` |

If your trick needs `Normal` (billboard detection, normal-based gating) you cannot use the
text or `position_*` families. If it needs `Color` (per-entity parameters) you cannot use
`position_tex`. Check this **before** designing the channel.

On G1 you must also list the attributes you use in the pipeline `.json` (optional but
safer); from G2 on they are inferred.

---

## 4. Reproducing a vanilla shader you intend to override

Resource packs cannot patch — replacing `core/entity.vsh` means shipping a **complete** file.
The `#ifdef` sets, varying names and helper calls are load-bearing and change between
versions, so this file must come from the target version, not from memory.

**Get the real source. Two routes, in order of convenience:**

1. **Ask the user for a vanilla core-shader template.** Community "core shader template"
   packs — a copy of the vanilla `assets/minecraft/shaders/` tree for a given version — are
   published for every release and are the normal way people start a pack. If the target
   version's sources are not already available in the working directory, **ask the user to
   fetch the template for that exact version and point you at it.** One request, and every
   override afterwards is exact.
2. **Extract from the client jar.** `.minecraft/versions/<ver>/<ver>.jar` is a zip:
   ```bash
   unzip -o "<ver>.jar" "assets/minecraft/shaders/*" -d vanilla-<ver>
   ```

Do this **before** writing any override. It also answers, for free, most of the questions in
`instrumentation.md § 7`: which includes exist, what each declares, which `defines` each
render type sets, and — by diffing against the previous version's template — exactly what
changed.

**If neither is available:** `version-matrix.md` has a faithful skeleton per generation and
`worked-examples.md` has complete working files. Build from those, expect to iterate against
`latest.log`, and tell the user that an exact template would remove the guesswork.

**Then apply the minimal-diff rule:** copy the vanilla file verbatim; add your include and
one call. A diff of two lines is reviewable and portable; a rewritten file is neither.

**Isolate where you can.** On G1/G2, instead of overriding the shared `core/entity`, add
your own `shaders/mine/entity.vsh` and point only the render type you care about at it:
```json
{ "vertex": "minecraft:mine/entity", "fragment": "minecraft:mine/entity",
  "defines": { "values": { "ALPHA_CUTOUT": "0.1" } },
  "samplers": [ … ], "uniforms": [ … ] }
```
On G3+ this is not possible; gate on a magic value instead.

---

## 5. Deleting vanilla behaviour

Some effects are best achieved by removing something. The vanilla skybox pack is the
reference [V]:

```glsl
// core/sky.vsh — force the sky to a fullscreen quad, then paint it black in the fsh
switch (gl_VertexID) {
    case 0: gl_Position = vec4(-2.0, 10.0, 0.0, 1.0); break;
    case 1: gl_Position = vec4(-2.0, -2.0, 0.0, 1.0); break;
    case 2: gl_Position = vec4(10.0, -2.0, 0.0, 1.0); break;
    default: gl_Position = vec4(10.0, 10.0, 0.0, 1.0); break;
}

// core/stars.vsh — remove stars entirely
void main() { gl_Position = vec4(-1.0); }     // w = -1: degenerate, never rasterised

// core/position_color.* — removes the sunset horizon band
// include/fog.glsl  — a stub that returns the colour unchanged removes fog globally
```
Ship each removal as its own file and **document that deleting the file reverts it**. That
pack's README does exactly this, and it makes the pack composable.

The `(-2,10) (-2,-2) (10,-2)` triple is the oversized-triangle fullscreen trick: one
triangle that covers the whole viewport with no diagonal seam. The fourth vertex duplicates
the third so the second triangle of the quad is degenerate; belt and braces is
`if (gl_PrimitiveID >= 1) discard;` in the fragment shader.

---

## 6. Choosing, in practice

1. Where must the pixels appear? → narrows to world / GUI / hand / sky.
2. Must it exist without server support? → block face or sky; otherwise an entity.
3. Does a post pass need to read it separately from the world? → `item_entity` or
   `particles`.
4. Which attributes does the channel need? → check `§ 3`.
5. Will it also render in a GUI/inventory where you do not want it? → add
   `ProjMat[2][3] == 0.0` or `ModelOffset == vec3(0.0)` guards.
6. Is the render type shared with vanilla content? → gate on a magic value, and confirm the
   vanilla path is untouched.
