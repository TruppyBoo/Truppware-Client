# TruppWare — Session Handoff

Paste this into a new chat to bring it up to speed.

## Project
- **TruppWare**: Minecraft **Fabric** utility/cheat client, **MC 1.21.11**, **Java 21**.
- Root: `C:\Users\Misa\Desktop\TruppWare\truppware-template-1.21.11`
- Build/verify: `./gradlew compileClientJava` (no output = success). Run client to test.
- Crash reports: `run/crash-reports/*.txt`.

## Mappings / API quirks (1.21.11, loom-layered)
- Mojmap-style names BUT: `net.minecraft.resources.Identifier` (not ResourceLocation),
  `net.minecraft.world.inventory.ClickType`, `net.minecraft.client.gui.Font`.
- `Inventory.getSelectedSlot()` / `setSelectedSlot(int)` (the field is `selectedSlot`, NOT `selected`).
- New GPU pipeline: `RenderPipelines`, `GuiGraphics.blit(RenderPipelines.GUI_TEXTURED,...)`.
  Lines need **per-vertex `setLineWidth` AND `setNormal`**. `RenderType.create` is package-private
  (reach it via `@Invoker`). Through-walls = `DepthTestFunction.NO_DEPTH_TEST`.
- `GuiGraphics.pose()` returns `Matrix3x2fStack` (2D) for HUD.
- To inspect real signatures: `javap` the deobf jar under
  `~/.gradle/caches/fabric-loom/minecraftMaven/.../minecraft-clientonly-1.21.11-*.jar`
  (the `fabric-loom/1.21.11/minecraft-client.jar` is obfuscated).

## Architecture
- **Event bus**: `TruppWareClient.onEvent(Event, Timing)` loops toggled modules, each wrapped in a
  per-module **try/catch firewall** (one module throwing can't disconnect you).
  Events: `EventTick` (PRE/POST), `EventRender` (HUD GuiGraphics, from GuiMixin.renderTabList),
  `EventMovementInput`, `EventAttack`, `EventWorldRender`, `EventGlow`, `EventMovementFix`, `EventPacket`.
- **3D world drawing**: `WorldRenderEvents.BEFORE_DEBUG_RENDER` → `Projection.snapshot(...)` +
  dispatch `EventWorldRender`.
- Modules registered in `module/Manager.java`.

## Rotation system (the core of Aura/Scaffold silent aim)
`util/RotationUtil.java` owns ALL silent rotation:
- `set(yaw,pitch)` — a module calls this each frame it wants to aim; sets `serverYaw/serverPitch`,
  `active=true`, `claimed=true`.
- `update()` — run once per render frame AFTER modules (from TruppWareClient on EventRender PRE).
  If a module aimed this frame, hold. Otherwise ease `serverYaw/Pitch` + render head/body/pitch back
  to real and then release (`active=false`). Ease factor `0.08f`. This gives smooth ease-out on
  disable / target-out-of-range (no snap).
- `renderHeadYaw/renderBodyYaw/renderPitch` — separately eased for the third-person model.
All rotation consumers gate on `RotationUtil.active`:
- `ClientPlayerEntityMixin` — redirects `sendPosition` getYRot/getXRot → serverYaw/serverPitch.
- `EntityMixin.moveRelative` — move-fix: sets player yaw to serverYaw while active (so movement
  matches the silent aim direction).
- `LivingEntityMixin.hookJumpYaw` — jump uses serverYaw. (head/body force-write was REMOVED; it
  caused third-person snap.)
- `LivingEntityRendererMixin.extractRenderState` — returns renderHeadYaw/renderBodyYaw/renderPitch
  when active (third-person model follows the aim & eases out). Also GlowEsp outline.
- `GameRendererMixin.pick` (HEAD/RETURN) — while active, temporarily sets player rotation to server
  aim during `pick()` so your **crosshair interacts with what Aura/Scaffold is aiming at**, not the
  block under your real crosshair. Camera still renders real view (restored on RETURN).
  Also captures `getProjectionMatrix` for world→screen.

## Key modules
- **Aura** (`COMBAT/Aura.java`): central aim via `RotationUtil.set`. Smoothed human-like rotation
  with GCD quantization (`GRIM_GCD`). `lookingAtTarget()` raycasts the EXACT sent aim vs the **real
  hitbox shrunk by 0.04** (only fires when solidly inside → server agrees → no hitbox flags); plus
  point-blank `contains(eye)` and a wall check. Aim sits near hitbox CENTER (jitter ~±0.10, chest)
  for margin against moving targets. AntiBot (`isBot()` tab-list+hitbox), team filter
  (ON=attack all, OFF=skip own team), null guards, defensive player-list copies (CME safety).
  Exposes `currentTarget` / `currentTargetInRange` statics (AutoMace/ShieldBreaker attack these).
  **Possible next step if still rare flags on laggy servers: lag-compensated aiming (aim ~ping/2
  behind at server-perceived position).**
- **Scaffold** (`player/Scaffold.java`): user's "old scaffold" + added: `safeWalk` BooleanSetting
  (sneak on edge), smooth GCD-compliant rotations via `RotationUtil.set`, `snapGcd()`/`grimGcd()`,
  smooth disable. **KNOWN BUG (flagged, not fixed per user): `switchSlot` uses broken reflection on
  field `"selected"` — should use `Inventory.setSelectedSlot(int)` + `ServerboundSetCarriedItemPacket`.
  This causes "doesn't recognize block until manually placed."**
- **InventoryManager** (`player/InventoryManager.java`): keeps best tool/armor per type, throws
  duplicates/junk. Classifies by item description-id (1.21 dropped SwordItem/PickaxeItem).
  **ONLY runs while the real `InventoryScreen` is open.** Slot map: `invSlot<9 ? invSlot+36 : invSlot`,
  throw via `handleInventoryMouseClick(containerId, slot, 1, ClickType.THROW, player)`.
- **Fly / Speed** (`modules/MOVEMENT/`): velocity-based (setDeltaMovement), not creative flight.
- **ClickGui** (`ClickGui.java`): mouse-driven panels, GLFW mouse polling, Fonts.MAIN. NO
  `renderBackground()` (causes "Can only blur once per frame" crash) — uses a dim `g.fill`.

## Rendering utils
- `util/CustomFontRenderer.java` + `util/Fonts.java`: GPU atlas font (AWT-rasterized TTF →
  NativeImage → DynamicTexture, blit via GUI_TEXTURED). `Fonts.MAIN`. Swap font = change
  `FONT_PATH` in Fonts.java. Used by HUD/ArrayList/DamageTag/ClickGui.
- `util/Render3DUtil.java` + `util/EspRenderType.java`: line-based 3D (drawBox/drawCircle/drawLine)
  using a no-depth clone of `RenderPipelines.LINES` (NO_DEPTH_TEST via RenderTypeAccessor `@Invoker`).
  Lines MUST set `.setLineWidth(2.0f)` per vertex (else native "Missing LineWidth" crash). Do NOT mix
  render types in one buffer (debugQuads corrupted the line buffer — broke ESP/ring).
- `util/Projection.java`: worldToScreen via captured projection matrix + `snapshot(cam,pitch,yaw)`.
- Render modules: `BoxESP`, `TargetRing`, `DamageTag` (floating damage numbers via custom font in 3D),
  `RiceHat` (wireframe cone hat). All iterate `new ArrayList<>(mc.level.players())` for CME safety.

## Gotchas learned
- Never iterate `mc.level.players()` on the netty/packet thread → ConcurrentModificationException
  disconnect. Gate modules to render/tick threads + copy lists.
- NumberSetting ctor is `(name, min, max, default, increment)` — getting arg order wrong makes
  min>max and sliders break. Audit any new module's settings.
- Grim GCD min step = `(sensitivity*0.6+0.2)^3 * 1.2` (the constant 0.0096 is only sensitivity 0).
- **FakeLag** (`COMBAT/FakeLag.java`) buffers outbound packets in `sharedQueue` and re-sends via
  `connection.send()`. On `ClientboundStartConfigurationPacket` (sub-server switch) it now DROPS the
  queue without sending — flushing PLAY packets across the PLAY→CONFIGURATION pipeline rebuild
  crashes netty ("unsupported message type ... OutboundConfigurationTask$$Lambda") → disconnect.
  Any packet-buffering module must drop its queue on that packet.

## Current state
Last task: made Aura raycast accurate (real-box-shrunk check + center aim) to stop hitbox flags.
Compiles clean. InventoryManager confirmed inventory-screen-only.
