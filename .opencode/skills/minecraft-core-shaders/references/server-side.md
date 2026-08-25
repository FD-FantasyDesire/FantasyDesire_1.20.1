# The server half — datapacks, plugins, and getting the shader triggered

Every advanced pack in this discipline is a **pair**: a resource pack that knows how to
render, and a datapack or plugin that supplies the geometry and the data. A perfect shader
with nothing to trigger it renders nothing.

Evidence grades: **[V]** verified from inspected shipping packs, **[D]** derived, **[I]** inferred —
confirm against the target version's own documentation or by testing before shipping.

---

## 1. Display entities — the universal carrier

Since 1.19.4 the three display entities are the standard way to put controllable geometry in
front of a core shader: `item_display`, `block_display`, `text_display`.

Why they beat armour stands and item frames:
- exact position, rotation, scale and shear via one `transformation` field,
- **client-side interpolation**, so you get smooth motion at frame rate from 20 tps updates,
- no collision, no physics, no gravity,
- a `brightness` override that writes straight into `UV2`,
- `view_range` so they can be forced to render far away.

Fields that matter to a shader author [I — field names are stable but confirm per version]:

| Field | Why a shader author cares |
|---|---|
| `transformation` (`translation`, `left_rotation`, `scale`, `right_rotation`) | your model matrix; `scale: [0,0,0]` makes it invisible but still submitted |
| `interpolation_duration`, `start_interpolation` | client interpolates the transform over N ticks — free smooth animation |
| `teleport_duration` | interpolates *position* changes; without it, movement is a jump |
| `billboard` (`fixed`/`vertical`/`horizontal`/`center`) | vanilla billboarding, so you do not have to do it in the shader |
| `brightness` (`{sky, block}`) | **writes `UV2` directly** — a free 4+4-bit per-entity channel |
| `glow_color_override` | tints the `entity_outline` target — a channel into the outline post pipeline |
| `view_range` | keeps far-away markers (waypoints) alive |
| `shadow_radius`, `shadow_strength` | set to 0 for HUD elements |
| `text`, `background`, `alignment`, `line_width`, `see_through`, `text_opacity` (text_display) | selects `rendertype_text` vs `*_see_through`, and whether a background quad is drawn |
| `item`, `item_display` (item_display) | the display context — `gui`, `head`, `fixed`, `firstperson_righthand`, … which decides which `display` block of the model applies |

### Two channels people forget

**`brightness` → `UV2`.** [D] A per-entity 8 bits that arrives through the lightmap
attribute. Cheap, and it does not disturb `Color`. Read it as
`texelFetch(Sampler2, UV2/16, 0)` for the colour, or as `UV2/16` for the raw levels.

**`glow_color_override` → the outline target.** [D] The entity outline pass writes the glow
colour into `minecraft:entity_outline`; a post effect on that pipeline can read it as data.
This is an always-on hook that does not require Fabulous.

### Positioning tricks the shaders expect

The position-band and coordinate-region schemes in `data-channels.md § 7` are implemented
server-side simply by summoning the display at an absurd coordinate:
```
# HUD element anchored bottom-left, using the position-band convention
summon text_display ~ -1500000 ~ {text:'…', billboard:"fixed", …}
```
Keep `|y|` under about 8·10⁶ so float precision still resolves the low bits.

---

## 2. Item models and `custom_model_data`, per version

This is the most version-sensitive part of the server half.

| Version | How the server selects a custom model |
|---|---|
| ≤ 1.20.4 | item NBT `{CustomModelData: 1}`, matched by an `overrides` predicate in `models/item/<id>.json` [V] |
| 1.20.5 – 1.21.3 | item **component** `minecraft:custom_model_data=1`, still matched by `overrides` [I] |
| 1.21.4+ | `overrides` is gone. The item points at an **item definition** in `assets/<ns>/items/<id>.json`; the component becomes a structured `minecraft:custom_model_data={floats:[…],flags:[…],strings:[…],colors:[…]}` and is matched by `minecraft:select` / `minecraft:condition` providers. `minecraft:item_model` can also name a definition directly. [V for the pack side, I for the exact component shape] |

Pack side for each era is in `mesh-tricks.md § 1`. **Confirm the component syntax against the
target version before writing commands** — this changed twice in a year.

Multi-version servers usually solve this by sending a different resource pack per protocol,
or by shipping overlays and duplicating the item definition on both sides of the line.

---

## 3. Map items as a data channel

The highest-bandwidth server→client path (`data-channels.md § 8`).

- The server owns a `map_id`'s 128×128 byte array and can rewrite it every tick.
- Colours are palette indices; 0–3 are transparent, so a byte-oriented protocol should offset
  by 4. [D]
- Delivery is a map-update packet per changed region; a full 128×128 rewrite every tick is
  expensive at scale. Send dirty rectangles.
- On the client the map renders through `rendertype_text`, which is why every map-based
  shader lives in the text shader. [V]
- Detect the carrier with `textureSize(Sampler0, 0) == ivec2(128, 128)`. [V]

Libraries exist for the packet work because doing it by hand is tedious; the encoding format
on top is yours to choose.

---

## 4. Bossbar / scoreboard text as a HUD transport

HUD frameworks commonly draw with **font glyphs in a bossbar or scoreboard string** rather
than with entities. [V]

- Cheap: no entities, no tick cost, updates as fast as you can send a bossbar update.
- The glyphs come from a custom font whose characters are your HUD art, with negative-width
  spacer glyphs for positioning.
- Element identity and timing ride in the glyph's Y position and vertex colour
  (`data-channels.md § 6`, `§ 7`; `gpu-ui.md § 6`).
- Constraint: you only get what the text renderer will draw, positioned in GUI pixel space.

Use this when the HUD is text-shaped and you control the server. Use display entities when
you need world-space anchoring.

---

## 5. Driving the fog uniforms

Pass dispatch depends on the shader recognising specific `FogStart`/`FogEnd`/`FogColor`
values (`data-channels.md § 11`). [V that a shader can read them; **[I]** how a datapack best
sets them.]

Vanilla fog varies with: render distance, dimension, weather, being in water / lava /
powder snow, and the blindness and darkness status effects. **Anything the server can change
that alters fog is a channel into the fog uniforms.** Status effects are the most precise
lever because they are per-player and instantaneous.

If you build on this: measure the actual values in the target version
(`instrumentation.md`), pick sentinels that no natural state produces, and write the measured
table into your pack's comments. Never copy sentinel constants from another pack — they are
version-specific.

---

## 6. `GameTime` as a channel

Send a time-update packet whose world age encodes your value. [V]
```java
long worldAge = (long) Math.floor(((float) fov + 0.5F) / 180.0F * 24000.0F);
// 0 unlocks
```
Costs the day/night cycle and every time-based animation. Per-player if you send per-player
time packets.

---

## 7. Post-effect toggles (26.2+)

The modern answer to "send data from a plugin to a shader". The server enables or disables
named post effects from the resource pack, which becomes an N-bit bus with one tiny post
effect per bit (`data-channels.md § 10`). [V for the pack side; confirm the server-side
command or packet name against the target version.]

Properties: one-packet latency, no entities, no tick cost, trivially scalable by generating
more JSON files. If the target version supports it, prefer it over everything else in this
file for pure data.

---

## 8. Timing and latency

| Channel | Latency | Update rate | Interpolated? |
|---|---|---|---|
| Vertex `Color`, `brightness`, transformation | 1 tick (50 ms) | 20 Hz | transform yes, colour no |
| Display entity position | 1 tick | 20 Hz | only with `teleport_duration` |
| Map contents | 1 packet | as often as you send | no |
| Bossbar / scoreboard text | 1 packet | as often as you send | no |
| `GameTime` | 1 packet | 20 Hz | no |
| Post-effect toggle | 1 packet | on change | no |
| Fog (via status effect) | 1 tick | 20 Hz | vanilla fades some fog transitions |

**Design rule:** the server sets *parameters*, the shader does the *animation*. Sending 20
updates a second to animate something is both laggy and expensive; send a start tick and a
duration once, and let the shader interpolate at frame rate (`techniques-index.md § A10`,
`§ A11`). Every good HUD works this way.

---

## 9. Splitting work between the halves

| Do it server-side | Do it shader-side |
|---|---|
| Decide *what* exists and where | Decide how it looks |
| Anything needing world knowledge (block lookups, entity queries, permissions) | Anything per-pixel or per-frame |
| Anything needing persistence | Anything derivable from what is already on screen |
| Anything a player could exploit by editing the pack | Presentation |
| Bulk pixel data (maps) | Decoding and layout |

Security note: the client can be told to load any resource pack, and a player can edit or
replace it. Never let the shader be the authority on anything that matters — it is a
renderer, not a rule.

---

## 10. Checklist for the server half

- [ ] Which entity/item/map/text actually triggers each shader path, and is it guaranteed to
      exist when the effect should show?
- [ ] Are the magic values (colour, position band, texture) set exactly, with no rounding on
      the way out?
- [ ] Does the carrier also appear somewhere you did not intend (inventory, item frame,
      dropped item, third person)? The shader needs a guard for each.
- [ ] Is `custom_model_data` / item-definition syntax right for **this** version?
- [ ] Are you sending parameters and letting the shader animate, rather than animating from
      the server?
- [ ] `view_range`, `shadow_radius`, `billboard` set appropriately so vanilla behaviour does
      not fight your shader?
- [ ] Does everything degrade sensibly for a player who has no resource pack loaded?
