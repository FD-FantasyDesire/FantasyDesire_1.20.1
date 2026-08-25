# Frame anatomy — what is drawn when

Every design decision in this discipline depends on **ordering**. A data marker works only
because terrain draws before the post pipeline. A HUD element escapes bloom only because
the GUI draws after it. Before choosing a carrier or a hook, place your effect on this
timeline.

Evidence grades: **[V]** verified from inspected shipping packs, **[D]** derived from how the
shipping packs behave, **[I]** inferred — verify with `instrumentation.md` before relying on it.

---

## 1. The frame timeline

```
  ┌─ per-frame setup ────────────────────────────────────────────────────┐
  │ 1.  lightmap texture generated       core/lightmap.fsh          [V]  │
  └──────────────────────────────────────────────────────────────────────┘
  ┌─ world, into minecraft:main and the Fabulous side targets ───────────┐
  │ 2.  sky quad                          core/sky                  [V]  │
  │ 3.  sun / moon                        core/position_tex_color   [V]  │
  │ 4.  stars                             core/stars                [V]  │
  │ 5.  horizon / sky gradient            core/position_color       [V]  │
  │ 6.  terrain solid                     core/terrain  (SOLID)     [V]  │
  │ 7.  terrain cutout_mipped, cutout     core/terrain  (+ALPHA_CUTOUT)  │
  │ 8.  entities                          core/entity + rendertype_*     │
  │ 9.  block entities                    (entity family)                │
  │ 10. entity outline pass  ──────────────►  minecraft:entity_outline   │
  │ 11. particles            ──────────────►  minecraft:particles        │
  │ 12. terrain translucent  ──────────────►  minecraft:translucent      │
  │ 13. item entities        ──────────────►  minecraft:item_entity      │
  │ 14. clouds               ──────────────►  minecraft:clouds           │
  │ 15. weather              ──────────────►  minecraft:weather          │
  └──────────────────────────────────────────────────────────────────────┘
  ┌─ post ───────────────────────────────────────────────────────────────┐
  │ 16. post_effect/entity_outline   (glow)                         [V]  │
  │ 17. post_effect/transparency     (Fabulous compositing)         [V]  │
  │       └─ this is where user post pipelines live                      │
  └──────────────────────────────────────────────────────────────────────┘
  ┌─ after post ─────────────────────────────────────────────────────────┐
  │ 18. first-person hand / held item                               [D]  │
  │ 19. GUI, HUD, inventory screens        core/gui, position_tex …      │
  │ 20. post_effect/blur  (menu background only)                    [V]  │
  └──────────────────────────────────────────────────────────────────────┘
```

The layer list in steps 10–15 is **[V]** — it is exactly the set of targets the stock
`transparency` pass consumes, in the order packs feed them
(`translucent, itemEntity, particles, clouds, weather`). The relative order of 8/9 and of
11–15 among themselves is **[I]**; what matters and is certain is that they land in
*separate targets*.

---

## 2. The consequences you must design around

### 2a. Post cannot see the hand or the GUI

Steps 18–19 happen after the post pipeline. Therefore:

- **Bloom, SSAO, motion blur and colour grading do not touch the held item or the HUD.**
  This is usually what you want. If you need a glowing HUD, you must draw it in a *world*
  render type (steps 2–15) rather than as GUI.
- **First-person view models cannot be post-processed.** Everything an FPS pack does for
  weapon models happens in the core vertex shader, not in post — that is not a stylistic
  choice, it is forced by this ordering.
- A post pass that samples `minecraft:main` sees the world *without* the hand — which is why
  a saved off-camera frame comes out clean.

### 2b. A data marker must be written before it is read

The marker rides on terrain (steps 6–7) or on an item entity (step 13); the post pass that
decodes it runs at step 17. That ordering is what makes the whole technique work.

Corollaries:
- A marker on a **GUI** element (step 19) is written *after* post and can never be read by a
  post pass this frame. It would be read one frame late, from the persistent target.
- A marker on terrain is available to *every* post pass. A marker on `item_entity` is only
  available to passes that are given the `minecraft:item_entity` target — but it is
  **isolated from the world**, which is what makes it usable as a control plane.
- Within the world, a marker written at step 6 is visible to a core shader at step 13 only
  through the framebuffer, which core shaders cannot sample. Core-to-core communication in
  the same frame is not possible; use the texture or the previous frame.

### 2c. The lightmap is generated first

`core/lightmap.fsh` runs before anything samples `Sampler2`. That is why the lightmap-alpha
bus (`data-channels.md § 2`) delivers same-frame data to every world shader.

### 2d. Only the Fabulous path splits into targets

Steps 11–15 write to separate targets **only on Graphics = Fabulous!**. On Fancy/Fast
everything composites directly and `post_effect/transparency` does not run. Any pipeline
built on those targets silently does nothing. Say so in the pack description.

### 2e. `entity_outline` runs mid-frame, always

Step 16 runs whenever something is glowing, regardless of graphics setting **[I]**. It is
therefore the cheapest always-on post hook, and packs routinely use it for effects that have
nothing to do with outlines.

### 2f. The sky pass is the only geometry with a known world orientation before terrain

Steps 2–5 draw before anything else. The sun quad in particular has a known direction
(it tracks the time of day), which the vanilla skybox pack exploits as a **clock**
(`techniques-index.md § A25`). If you need something behind all world geometry, this is
where it goes.

### 2g. Depth is shared, targets are not

All world steps write into the same depth attachment per target. That is why
`postfx-cookbook.md § 4` has to `min()` six depth samplers to get "the depth of what the
player actually sees".

---

## 3. Which hook for which job

| You want to affect | Hook | Runs on Fast/Fancy? |
|---|---|---|
| World geometry appearance | the relevant `core/*` shader | yes |
| The held item / hand | `core/entity`-family shader, detect the hand pass | yes |
| HUD / inventory | `core/rendertype_text`, `core/gui`, `position_tex*` | yes |
| Everything on screen, before the hand | `post_effect/transparency` | **Fabulous only** |
| Everything, cheaply, always | `post_effect/entity_outline` | yes (when glowing) |
| Menu backgrounds | `post_effect/blur` | yes, menus only |
| Sky, before all geometry | `core/sky`, `core/stars`, `core/position_tex_color` | yes |
| Per-frame state that must persist | any post pipeline's private target | pipeline-dependent |

---

## 4. What runs how often

- Core shaders run **once per vertex / fragment of every draw** — a terrain shader runs
  millions of times per frame. Keep branches cheap and put decode work in the vertex stage.
- A marker quad runs its expensive path for ~40 fragments. Free.
- Post passes run **once per screen pixel per pass**. This is the budget that matters.
- The lightmap shader runs 256 times per frame. Effectively free; put anything you like in
  it.
- The sky/sun/stars passes run for a handful of vertices. Also effectively free — which is
  why a full skybox raytracer fits there.

---

## 5. Ordering inside a post pipeline

Passes execute top to bottom, exactly once, with no loops or conditionals. Consequences:

- Unroll iteration by writing the pass N times (a bloom pyramid writes 7 downsamples; a
  flood fill writes one pass per propagation step).
- A pass reading target X gets whatever X held when the pass ran — either this frame's write
  from an earlier pass, or **last frame's contents** if nothing wrote it yet. Both are used
  deliberately.
- You cannot read and write the same target in one pass. Bounce through a scratch target.
- Conditional behaviour must be data-driven, not structural: run the pass always and let a
  pixel decide what it does (`techniques-index.md § A23`).

Typical shape:
```
main ─► [produce G-buffer]  ─► [effect]  ─► [temporal]  ─► main'
                                  ▲            │
                         prev* ────┘           └──► blit ──► prev*
        ─► transparency ─► swap ─► blit ─► minecraft:main
```

---

## 6. What to verify before trusting this file

The **[I]** and **[D]** items:
- exact position of the hand relative to post,
- whether `entity_outline` runs on every graphics setting,
- relative order of entities vs block entities vs particles.

**A single frame capture settles all of them in seconds.** RenderDoc's Event Browser is the
complete ordered draw list for the frame — this table is a summary of what that panel shows
directly. See `instrumentation.md § 2`. Without a capture, the fallback is pass tagging
(`instrumentation.md § 3c`).

Whenever this file and a capture disagree, the capture is right.
