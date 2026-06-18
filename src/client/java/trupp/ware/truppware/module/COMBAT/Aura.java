package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import trupp.ware.event.Event;
import trupp.ware.event.events.*;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.ModeSetting;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.DirectionalInput;
import trupp.ware.util.RaycastUtil;
import trupp.ware.util.RotationUtil;
import trupp.ware.util.TimerUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Aura extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final Random random = new Random();
    private final TimerUtil attackTimer = new TimerUtil();
    private final TimerUtil critTimer   = new TimerUtil();
    private long   currentDelay = 0;
    private long   nextAttackAt = 0;   // accumulator timestamp for the next allowed attack (ms)
    private double lastCps      = 0;

    private float   yawVelocity   = 0f;
    private float   pitchVelocity = 0f;
    private boolean smoothingOut  = false;

    private boolean inCrit       = false;
    private boolean critHit      = false;
    private long    critClickAt  = 0;
    private long    critResumeAt = 0;

    private Vec3 targetOffset = Vec3.ZERO;
    private long nextOffsetChange = 0;
    private float currentSpeedMult = 1.0f;

    public static boolean lookingAt;

    // BlockHit state
    private long nextBlockTime = 0L;
    private long unblockTime   = 0L;
    private boolean blocking   = false;
    private boolean lastAttacking = false;

    // Watchdog blockhit state
    private int watchdogBlockTicks = 0;

    public static boolean attacking;
    public static boolean targetInRange;
    public static Player  currentTarget;          // the entity Aura is currently locked onto (null if none)
    public static boolean currentTargetInRange;   // true when currentTarget is within actual attack range
    public static float   yaw, pitch;
    public static boolean enabled;
    public static boolean rotationReleased = true;

    private static final double GRIM_GCD = 0.009600000008940697D;

    public BooleanSetting team = new BooleanSetting("ignore team", true);
    public NumberSetting range    = new NumberSetting("Range",    1,   10,  5,   0.1);
    public NumberSetting preRange = new NumberSetting("PreRange", 0,   3,   1.0, 0.1);
    public NumberSetting minCPS   = new NumberSetting("MinCPS",   1,   20,  8,   1);
    public NumberSetting maxCPS   = new NumberSetting("MaxCPS",   1,   20,  14,  1);
    public NumberSetting strength = new NumberSetting("Strength", 0.1, 1.0, 0.15, 0.01);
    public NumberSetting blockMin = new NumberSetting("BlockMin", 1,   20,  8,   1);
    public NumberSetting blockMax = new NumberSetting("BlockMax", 1,   20,  12,  1);
    public BooleanSetting blockHit = new BooleanSetting("BlockHit", true);
    // Normal / Watchdog = sword block-hit (Hypixel only — these FLAG Grim, by design).
    // Shield = Grim-safe: hold an offhand shield and attack with the mainhand while it's up.
    public ModeSetting blockMode = new ModeSetting("BlockMode", List.of("Shield", "Watchdog", "Normal"));
    public BooleanSetting moveFix  = new BooleanSetting("MoveFix",  true);
    public BooleanSetting mode1_21 = new BooleanSetting("1.21Mode", false);
    public BooleanSetting antiBot  = new BooleanSetting("AntiBot", false);

    public Aura() {
        super("Aura", Category.COMBAT, "Attacks nearest target", 0);
        addSettings(range, preRange, minCPS, maxCPS, strength, blockHit, blockMode, blockMin, blockMax, moveFix, mode1_21, team, antiBot);
    }

    /**
     * Heuristic anti-bot: server-spawned "aura bots" (the fake players that fly around to bait a
     * hit) almost always lack a tab-list (player-info) entry, and often have an abnormal hitbox.
     * Real players are in the tab list with a normal-sized body.
     */
    private boolean isBot(AbstractClientPlayer p) {
        if (mc.getConnection() == null) return false;
        // Not in the tab list -> almost certainly a fake/bot player.
        if (mc.getConnection().getPlayerInfo(p.getUUID()) == null) return true;
        // Weird hitbox (too small / squashed). Normal player is ~0.6 x 1.8.
        if (p.getBbWidth() < 0.5f || p.getBbHeight() < 1.6f) return true;
        return false;
    }

    private Player getTarget() {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            targetInRange = false;
            return null;
        }

        Vec3 eyePos = player.getEyePosition(1.0f);
        double acquireRange = range.getNum() + preRange.getNum();

        // "ignore team" ON  => skip players on your own team (don't attack teammates).
        // "ignore team" OFF => attack everyone, teammates included.
        List<AbstractClientPlayer> players = mc.level.players().stream()
                .filter(p -> p != player)
                .filter(p -> !p.isDeadOrDying())
                .filter(p -> !p.isInvisible())
                .filter(p -> !antiBot.enabled || !isBot(p))
                .filter(p -> !team.enabled || !isTeammate(player, p))
                .filter(p -> boxDist(eyePos, p) <= acquireRange)
                .sorted(Comparator.comparingDouble(p -> boxDist(eyePos, p)))
                .toList();

        Player target = players.isEmpty() ? null : players.get(0);
        targetInRange = target != null;
        currentTarget = target;
        currentTargetInRange = inAttackRange(target);
        return target;
    }

    /**
     * Are we and {@code other} on the same team? Uses the scoreboard team (vanilla alliance) and, as
     * a fallback for servers that just colour each team's names, a matching real team colour.
     */
    private boolean isTeammate(Player self, Player other) {
        if (self.isAlliedTo(other)) return true;                 // same scoreboard team

        net.minecraft.world.scores.Team myTeam = self.getTeam();
        net.minecraft.world.scores.Team theirTeam = other.getTeam();
        if (myTeam != null && theirTeam != null) {
            net.minecraft.ChatFormatting c = myTeam.getColor();
            if (c != null && c.isColor() && c != net.minecraft.ChatFormatting.WHITE
                    && c == theirTeam.getColor()) {
                return true;                                     // same team colour
            }
        }
        return false;
    }

    private boolean inAttackRange(Player target) {
        if (target == null || mc.player == null) return false;
        Vec3 eye = mc.player.getEyePosition(1.0f);
        return boxDist(eye, target) <= range.getNum();
    }

    /**
     * Forgiving "am I aimed at the target" test. Instead of a hair-thin entity raycast (which
     * misses whenever the human-like aim jitter lands a pixel off the hitbox), this clips a ray
     * from the eye along the silent-aim yaw/pitch against the TARGET's bounding box inflated by a
     * small margin, then checks no block is in the way. Much more reliable, so it actually swings
     * when you're visibly looking at them.
     */
    private boolean lookingAtTarget(Player target) {
        if (mc.player == null || mc.level == null || target == null) return false;

        Vec3 eye = mc.player.getEyePosition(1.0f);
        double reach = range.getNum() + 0.5;

        // Use the EXACT silent-aim direction we send to the server.
        double yawR   = Math.toRadians(yaw);
        double pitchR = Math.toRadians(pitch);
        double lx = -Math.sin(yawR) * Math.cos(pitchR);
        double ly = -Math.sin(pitchR);
        double lz =  Math.cos(yawR) * Math.cos(pitchR);
        Vec3 end = eye.add(lx * reach, ly * reach, lz * reach);

        // Test against a SLIGHTLY SHRUNK hitbox. The server raycasts the rotation we send dwaagainst
        // the REAL box (plus its own small tolerance), so by only counting a hit when our aim is
        // solidly INSIDE the real box, the serdver is guaranteed to agree — no hitbox flags. (The
        // old code inflated the box, which let a swing fire up to 0.05 OUTSIDE the real body, which
        // is exactly what anticheats flag.)
        if (target.getBoundingBox().contains(eye)) return true;   // point-blank, ray starts inside
        AABB box = target.getBoundingBox().inflate(-0.001);
        if (box.contains(eye)) return true;

        Optional<Vec3> hit = box.clip(eye, end);
        if (hit.isEmpty()) return false;

        // Don't see them through walls. dwad f
        Vec3 hitPos = hit.get();
        HitResult block = mc.level.clip(new ClipContext(eye, hitPos,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        if (block.getType() != HitResult.Type.BLOCK) return true;
        return block.getLocation().distanceToSqr(eye) >= hitPos.distanceToSqr(eye) - 0.01;
    }

    private double boxDist(Vec3 eye, Player p) {
        double dx = Math.max(p.getBoundingBox().minX - eye.x, Math.max(0, eye.x - p.getBoundingBox().maxX));
        double dy = Math.max(p.getBoundingBox().minY - eye.y, Math.max(0, eye.y - p.getBoundingBox().maxY));
        double dz = Math.max(p.getBoundingBox().minZ - eye.z, Math.max(0, eye.z - p.getBoundingBox().maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void calculateRotation(Player target) {
        LocalPlayer player = mc.player;
        Vec3 eyePos = player.getEyePosition(1.0f);

        long now = System.currentTimeMillis();

        if (now >= nextOffsetChange) {
            // Aim TIGHT to the centre of the chest with only a hair of jitter. Staying dead-centre
            // means the ray sits well inside the body even if the turn lags a strafing target a
            // little — so it doesn't aim off the player.
            double halfW = Math.max(0.04, target.getBbWidth() * 0.5 - 0.24);
            double offX = (random.nextDouble() - 0.5) * 2 * halfW;
            double offZ = (random.nextDouble() - 0.5) * 2 * halfW;
            double offY = target.getBbHeight() * (0.58 + random.nextDouble() * 0.14); // chest
            targetOffset = new Vec3(offX, offY, offZ);

            nextOffsetChange = now + 140 + (long) (random.nextDouble() * 320);
        }

        Vec3 targetPos = target.position().add(targetOffset);

        double dx     = targetPos.x - eyePos.x;
        double dy     = targetPos.y - eyePos.y;
        double dz     = targetPos.z - eyePos.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw   = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, distXZ)));

        float yawDiff   = wrapDegrees(targetYaw - this.yaw);
        float pitchDiff = targetPitch - this.pitch;

        float str = (float) strength.getNum();

        // Per-tick human turn (ease-out, driven by distance + strength + fresh randomness), with a
        // little momentum so the motion glides instead of shaking. Pitch turns notably SLOWER than
        // yaw (humans flick horizontally fast but adjust vertical aim gently) — the lower axis scale
        // feeds both the step fraction and the cap.
        float yawStep   = humanStep(yawDiff,   str, 1.00f);
        float pitchStep = humanStep(pitchDiff, str, 0.55f);

        yawVelocity   = yawVelocity   * 0.22f + yawStep   * 0.78f;
        pitchVelocity = pitchVelocity * 0.22f + pitchStep * 0.78f;

        if (Math.abs(yawVelocity)   > Math.abs(yawDiff))   yawVelocity   = yawDiff;
        if (Math.abs(pitchVelocity) > Math.abs(pitchDiff)) pitchVelocity = pitchDiff;

        float newYaw   = this.yaw   + yawVelocity;
        float newPitch = this.pitch + pitchVelocity;

        newYaw   -= newYaw   % GRIM_GCD;
        newPitch -= newPitch % GRIM_GCD;

        this.yaw   = newYaw;
        this.pitch = clamp(newPitch, -90f, 90f);
    }

    /**
     * One tick of human-like aim toward a target angle. The speed is NOT constant: it scales with
     * how far we still have to turn — a big flick moves fast and decelerates as it lands (ease-out,
     * like a real flick), a small correction moves gently. Overall pace is set by {@code str}, and a
     * fresh random factor every tick keeps it organic. Close in, it covers most of the gap so the
     * crosshair sits tight on the body (accuracy); it never overshoots.
     */
    private float humanStep(float diff, float str, float axisScale) {
        float abs = Math.abs(diff);
        if (abs < 0.05f) return diff;

        // Ease-out SHAPE only (no strength here): cover a bigger share of the angle when close (tight
        // tracking / settle) and a smaller share of a huge angle (so a flick takes a few ticks).
        float ease = 0.30f + 0.45f * (1f - abs / (abs + 30f));   // ~0.75 close, ~0.41 on a 90 flick

        // STRENGTH is the dominant speed control: 0.10 = very slow & smooth, 1.0 = brisk (but not instant).
        float strFactor = 0.035f + 0.45f * str;                  // 0.10 -> 0.08, 1.0 -> 0.49

        float frac = ease * strFactor * axisScale;                // axisScale < 1 makes pitch slower
        frac *= (0.80f + random.nextFloat() * 0.40f);             // per-tick randomness -> never constant
        frac = clamp(frac, 0.015f, 1f);

        float step = diff * frac;

        // Speed cap, also driven by strength so low strength is genuinely slow even on big flicks.
        float cap = (1.5f + abs * 0.30f) * (0.06f + 0.50f * str) * axisScale * (0.85f + random.nextFloat() * 0.30f);
        step = clamp(step, -cap, cap);

        if (Math.abs(step) > abs) step = diff;                   // never overshoot the target angle

        if (abs > 3f) step += (random.nextFloat() - 0.5f) * 0.45f * str;  // subtle hand tremor
        return step;
    }

    private boolean smoothBackToPlayer() {
        if (mc.player == null) return true;

        float realYaw   = mc.player.getYRot();
        float realPitch = mc.player.getXRot();

        float yawDiff   = wrapDegrees(realYaw   - yaw);
        float pitchDiff = realPitch - pitch;

        if (Math.abs(yawDiff) < 1.5f && Math.abs(pitchDiff) < 1.5f) {
            yaw   = realYaw;
            pitch = realPitch;
            yawVelocity   = 0f;
            pitchVelocity = 0f;
            return true;
        }

        float yawSpeed   = Math.min(Math.abs(yawDiff)   * 0.35f, 8.0f);
        float pitchSpeed = Math.min(Math.abs(pitchDiff) * 0.35f, 8.0f);

        float yawAccel   = yawDiff   > 0 ? yawSpeed   : -yawSpeed;
        float pitchAccel = pitchDiff > 0 ? pitchSpeed : -pitchSpeed;

        yawVelocity   = yawVelocity   * 0.35f + yawAccel   * 0.65f;
        pitchVelocity = pitchVelocity * 0.35f + pitchAccel * 0.65f;

        if (Math.abs(yawVelocity)   > Math.abs(yawDiff))   yawVelocity   = yawDiff;
        if (Math.abs(pitchVelocity) > Math.abs(pitchDiff)) pitchVelocity = pitchDiff;

        float newYaw   = yaw   + yawVelocity;
        float newPitch = pitch + pitchVelocity;

        newYaw   -= newYaw   % GRIM_GCD;
        newPitch -= newPitch % GRIM_GCD;

        yaw   = newYaw;
        pitch = clamp(newPitch, -90f, 90f);
        return false;
    }

    private long generateDelay() {
        double min = Math.min(minCPS.getNum(), maxCPS.getNum());
        double max = Math.max(minCPS.getNum(), maxCPS.getNum());

        // nextDouble(min, max) throws if min >= max, so handle the equal case.
        double cps = (max - min) < 0.01 ? min : ThreadLocalRandom.current().nextDouble(min, max);
        if (lastCps != 0) {
            double drift = ThreadLocalRandom.current().nextDouble(-0.5, 0.5);
            cps = Math.max(min, Math.min(max, lastCps + drift));
        }
        lastCps = cps;
        if (cps < 0.1) cps = 0.1;

        long delay = (long) (1000.0 / cps);
        delay += ThreadLocalRandom.current().nextLong(-3, 4);
        return Math.max(1, delay);
    }

    /** True when it's time to attack again. Timing uses an ACCUMULATOR ({@link #nextAttackAt}) so the
     *  average CPS matches the setting even though attacks can only land on 50ms ticks — without this
     *  a 14-CPS delay (71ms) always rounded up to the next tick (100ms = 10 CPS), capping you at 10. */
    private boolean canAttack() {
        return System.currentTimeMillis() >= nextAttackAt;
    }

    /** Schedule the next attack by ADVANCING the target time by the delay (not resetting to now), so
     *  the timing error carries over and the real average CPS hits the setting. */
    private void scheduleNextAttack() {
        long now = System.currentTimeMillis();
        long d = generateDelay();
        nextAttackAt += d;
        if (nextAttackAt <= now) nextAttackAt = now + d;   // fell behind (idle/lag) -> resync
    }

    private void resetCrit() {
        if (mc.player != null)
            ((IKeyMappingExt) mc.options.keyUp).truppware$setPressed(true);
        inCrit  = false;
        critHit = false;
    }

    private long getBlockDelay() {
        double min = Math.min(blockMin.getNum(), blockMax.getNum());
        double max = Math.max(blockMin.getNum(), blockMax.getNum());
        double cps = ThreadLocalRandom.current().nextDouble(min, max);
        return (long) (1000D / cps);
    }

    private void stopBlocking() {
        if (!blocking) return;
        KeyMapping useKey = mc.options.keyUse;
        if (useKey instanceof IKeyMappingExt ext) ext.truppware$setPressed(false);
        blocking = false;
    }


    private void handleNormalBlockHit(Player target) {
        if (mc.player == null) return;
        long now = System.currentTimeMillis();

        // Release during swing so our hits register
        if (mc.player.swingTime >= 1 && mc.player.swingTime <= 2) {
            stopBlocking();
            return;
        }

        // Just attacked — start fresh block cycle
        boolean justAttacked = attacking && !lastAttacking;
        lastAttacking = attacking;

        if (justAttacked && !blocking && now >= nextBlockTime) {
            KeyMapping useKey = mc.options.keyUse;
            if (useKey instanceof IKeyMappingExt ext) {
                ext.truppware$setPressed(true);
                blocking = true;
            }
            unblockTime   = now + ThreadLocalRandom.current().nextLong(75, 110);
            nextBlockTime = now + getBlockDelay();
            return;
        }

        if (blocking) {
            KeyMapping useKey = mc.options.keyUse;
            if (useKey instanceof IKeyMappingExt ext) ext.truppware$setPressed(true);
        }

        if (blocking && now >= unblockTime) {
            stopBlocking();
        }

        if (mc.player.swingTime <= 2 || mc.player.swingTime > 5) return;
        if (blocking || now < nextBlockTime) return;

        KeyMapping useKey = mc.options.keyUse;
        if (useKey instanceof IKeyMappingExt ext) {
            ext.truppware$setPressed(true);
            blocking = true;
        }

        unblockTime   = now + ThreadLocalRandom.current().nextLong(75, 110);
        nextBlockTime = now + getBlockDelay();
    }


    private void handleWatchdogBlockHit(Player target) {
        if (mc.player == null){ return;}
        KeyMapping useKey = mc.options.keyUse;


        blocking = true;

        if(blocking)
            if (useKey instanceof IKeyMappingExt ext) ext.truppware$setPressed(true);




    }

    /**
     * Watchdog block driven by RAW PACKETS instead of the use key. Pressing keyUse makes the vanilla
     * client emit the use/release packet during its own input phase — a different point in the tick
     * than our attack — so we could never guarantee which Grim-tick they landed in. Sending the
     * packets ourselves, edge-triggered, from the same spot as the attack gives full control: the
     * USE goes out exactly on the BLOCK tick, the RELEASE exactly on the RELEASE tick.
     *
     * Over ViaFabric a ServerboundUseItemPacket with a sword becomes a 1.8 bFlock-place (sword block),
     * and RELEASE_USE_ITEM ends it — exactly the 1.8 block mechanic, with no client input timing.
     */
    private boolean wdBlocking = false;
    /** True while the watchdog block is held (server-side). NoSlow reads this for its sword mode. */
    public static boolean watchdogBlocking = false;
    /** True only on a "safe" blocked tick (no attack/release this tick) — when NoSlow may slot-swap. */
    public static boolean watchdogHolding = false;

    private void wdSetBlock(boolean block) {
        if (block == wdBlocking) return;                       // edge only — one packet per change
        if (mc.player == null || mc.player.connection == null) { wdBlocking = block; watchdogBlocking = block; return; }
        if (block) {
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(
                    InteractionHand.MAIN_HAND, 0, RotationUtil.serverYaw, RotationUtil.serverPitch));
        } else {
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                    net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                    net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.DOWN));
        }
        wdBlocking = block;
        watchdogBlocking = block;
    }

    private boolean shieldUp = false;

    /** Raise/lower an OFFHAND shield via packets, edge-triggered (one packet per change). */
    private void shieldSet(boolean up) {
        if (up == shieldUp) return;
        if (mc.player == null || mc.player.connection == null) { shieldUp = up; return; }
        if (up) {
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(
                    InteractionHand.OFF_HAND, 0, RotationUtil.serverYaw, RotationUtil.serverPitch));
        } else {
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                    net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                    net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.DOWN));
        }
        shieldUp = up;
    }

    /**
     * GRIM-SAFE block. A shield is raised once (one USE_ITEM, offhand) and you may attack with the
     * mainhand WHILE it is up — that is legal vanilla behaviour, so there is no per-hit release and
     * nothing for Grim's packet-order checks to catch. Requires a shield in your offhand. (Does not
     * work on a 1.8 server via Via — there is no offhand there; use Watchdog for Hypixel.)
     */
    private void runShield(Player target) {
        if (mc.player == null || mc.gameMode == null) return;

        // Hold the shield up through the whole melee exchange (a little beyond attack range), not
        // just at the exact attack distance — so you're actually protected while trading hits.
        boolean nearTarget = boxDist(mc.player.getEyePosition(1.0f), target) <= range.getNum() + 1.5;
        boolean canShield = blockHit.isEnabled()
                && mc.player.getOffhandItem().is(net.minecraft.world.item.Items.SHIELD)
                && nearTarget
                && mc.player.hurtTime < 8;

        boolean wasUp = shieldUp;
        shieldSet(canShield);   // raise once / hold while eligible; lower automatically otherwise
        // If we raised or lowered the shield THIS tick, don't also attack this tick — a use/release
        // sharing a tick with an attack is exactly PacketOrderI (attack-while-rightClicking, or the
        // release setback). Attack only on ticks where the shield state was unchanged.
        boolean changedThisTick = wasUp != shieldUp;

        if (!changedThisTick && canAttack() && inAttackRange(target)) {
            boolean aimed = lookingAtTarget(target);
            lookingAt = aimed;
            if (aimed) {
                mc.gameMode.attack(mc.player, target);   // attacking with a raised shield is legal
                mc.player.swing(InteractionHand.MAIN_HAND);
                attacking = true;
                scheduleNextAttack();
            }
        } else {
            attacking = false;
        }
    }

    /** Interact (right-click) the target entity — sent between an attack and a block so Grim's
     *  PacketOrderJ allows the use-after-attack. Vanilla (and Grim's PacketOrderC) require the pair
     *  INTERACT_AT then matching INTERACT, so we send both. */
    private void sendInteract(Player target) {
        if (mc.player == null || mc.player.connection == null) return;
        boolean sneak = mc.player.isShiftKeyDown();
        Vec3 loc = new Vec3(0, target.getBbHeight() * 0.5, 0);   // interaction point, relative to entity
        mc.player.connection.send(net.minecraft.network.protocol.game.ServerboundInteractPacket
                .createInteractionPacket(target, sneak, InteractionHand.MAIN_HAND, loc));   // INTERACT_AT
        mc.player.connection.send(net.minecraft.network.protocol.game.ServerboundInteractPacket
                .createInteractionPacket(target, sneak, InteractionHand.MAIN_HAND));        // INTERACT
    }

    /**
     * Grim-safe block-hit, modelled on Unfair's KillAura (case 2 + interactAttack). Maps to Grim's
     * packet-order checks exactly so nothing flags:
     *   - hold the block with ONE use packet (never re-sent while held);
     *   - to hit you must be unblocked, and a RELEASE can't share a tick with an attack, so we
     *     RELEASE on its own tick (phase 1)...
     *   - ...then the next tick (phase 2) ATTACK -> INTERACT -> USE(re-block): the INTERACT between
     *     the attack and the use is what lets PacketOrderJ permit a use right after an attack.
     *   - NEVER swap held-item slots (PacketOrderE setbacks on a swap while using/sprinting).
     *
     * Phases (watchdogBlockTicks): 0 = blocked & waiting for CPS, 1 = release, 2 = attack + re-block.
     */
    private void runWatchdog(Player target) {
        if (mc.player == null || mc.gameMode == null) return;

        boolean canBlock = blockHit.isEnabled()
                && mc.player.getMainHandItem().is(net.minecraft.tags.ItemTags.SWORDS)
                && inAttackRange(target)
                && mc.player.hurtTime < 8;

        // Not eligible to block-hit (no sword / out of range / heavy damage): plain aura, no block.
        if (!canBlock) {
            boolean wasBlocking = wdBlocking;
            wdSetBlock(false);
            watchdogBlockTicks = 0;
            watchdogHolding = false;
            // Don't attack the same tick we released — that's attack-while-releasing (PacketOrderI).
            if (!wasBlocking && canAttack() && inAttackRange(target)) {
                boolean aimed = lookingAtTarget(target);
                lookingAt = aimed;
                if (aimed) {
                    mc.gameMode.attack(mc.player, target);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    attacking = true;
                    scheduleNextAttack();
                }
            } else {
                attacking = false;
            }
            return;
        }

        boolean aimed = lookingAtTarget(target);
        lookingAt = aimed;

        switch (watchdogBlockTicks) {
            case 0:                          // BLOCKED — hold the block up, wait for the CPS delay
                wdSetBlock(true);
                watchdogHolding = true;      // safe tick for NoSlow's slot-swap (no attack/release)
                if (aimed && canAttack()) {
                    watchdogBlockTicks = 1;
                }
                break;
            case 1:                          // RELEASE — on its own tick (no attack/use alongside)
                wdSetBlock(false);
                watchdogHolding = false;
                watchdogBlockTicks = 2;
                break;
            case 2:                          // ATTACK -> INTERACT -> USE, all this tick
                watchdogHolding = false;
                if (aimed) {
                    mc.gameMode.attack(mc.player, target);          // attack (we're unblocked now)
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    sendInteract(target);                           // interact (lets J allow the use)
                    attacking = true;
                    scheduleNextAttack();
                }
                wdSetBlock(true);                                   // re-block
                watchdogBlockTicks = 0;
                break;
            default:
                watchdogBlockTicks = 0;
        }
    }

    private void handleBlockHit(Player target) {
        if (mc.player == null) return;
        HitResult h = RaycastUtil.raycast(mc, 2.9, yaw, pitch);
        if (h == null){
            stopBlocking();
            return;
        }

//
        if(target.hurtTime == 0){
            stopBlocking();
            return;
        }


        if (!blockHit.isEnabled()) {
            stopBlocking();
            watchdogBlockTicks = 0;
            return;
        }

        if (!mc.player.getMainHandItem().is(net.minecraft.tags.ItemTags.SWORDS)) {
            stopBlocking();
            watchdogBlockTicks = 0;
            return;
        }
        if (!inAttackRange(target)) {
            stopBlocking();
            watchdogBlockTicks = 0;
            return;
        }
        if (mc.player.hurtTime >= 8) {
            stopBlocking();
            return;
        }

        if (blockMode.getCurrentMode().equals("Watchdog")) {
            handleWatchdogBlockHit(target);
        } else {
            handleNormalBlockHit(target);
        }
    }

    private float wrapDegrees(float value) {
        value %= 360f;
        if (value >= 180f)  value -= 360f;
        if (value < -180f) value += 360f;
        return value;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onEvent(Event event, Timing time) {
        if(mc.player.isDeadOrDying()) toggle();
        // Hard guard: if we're between worlds / disconnecting the player or level can be null.
        // Touching them here (or letting a later branch touch them) throws an NPE that the game
        // turns into a disconnect, so bail out completely until we're back in a world.
        if (mc.player == null || mc.level == null || mc.player.isDeadOrDying()) {
            return;
        }

        // Only act on the main-thread events we actually handle. The packet event is dispatched
        // on the NETWORK thread; doing world iteration (getTarget -> mc.levdwadsael.pld ayers()) there
        // races the main thread and throws ConcurrentModificationException -> disconnect.
        if(event instanceof EventUpdate){
            if (mc.player == null || mc.level == null) return;
            Player target = getTarget();
            targetInRange = target != null;
            if(target == null) return;
            handleBlockHit(target);
        }

        if (event instanceof EventRender) {
            if (mc.player == null || mc.level == null) return;

            Player target = getTarget();
            targetInRange = target != null;

            if (targetInRange) {
                calculateRotation(target);
                rotationReleased = false;
                // Claim the silent rotation this frame. RotationUtil eases it back to real and
                // releases on its own once we stop claiming (target lost / module disabled).
                RotationUtil.set(yaw, pitch);
            } else {
                // Idle: keep our aim synced to the real rotation so re-acquiring starts smoothly
                // (and the move-fix is a no-op). The central smooth-out handles the server yaw.
                yaw   = mc.player.getYRot();
                pitch = mc.player.getXRot();
                yawVelocity = 0f;
                pitchVelocity = 0f;
                rotationReleased = true;
            }
        }
        if (event instanceof EventMovementInput moveEvent) {

            if (mc.player == null) return;
            // Only correct movement while the silent rotation is actually being sent (incl. the
            // smooth-out), and correct against the REAL sent yaw so it stays aligned as it eases.
            if (!RotationUtil.active) return;

            DirectionalInput input = moveEvent.getDirectionalInput();
            if (!input.isMoving()) return;

            float forward = (input.isForward() ? 1f : 0f) - (input.isBackward() ? 1f : 0f);
            float strafe  = (input.isLeft()    ? 1f : 0f) - (input.isRight()    ? 1f : 0f);

            float mag = (float) Math.sqrt(forward * forward + strafe * strafe);
            if (mag > 0) {
                forward /= mag;
                strafe  /= mag;
            }

            float clientYaw = mc.player.getYRot();
            float diff      = (float) Math.toRadians(RotationUtil.serverYaw - clientYaw);
            double cos      = Math.cos(diff);
            double sin      = Math.sin(diff);

            float nf = (float) (forward * cos - strafe * sin);
            float ns = (float) (forward * sin + strafe * cos);

            final float DEADZONE = 0.35f;

            moveEvent.setDirectionalInput(new DirectionalInput(
                    nf >  DEADZONE,
                    nf < -DEADZONE,
                    ns >  DEADZONE,
                    ns < -DEADZONE
            ));
            return;

        }
        // Combat runs on EventUpdate (sendPosition HEAD = right before the flying packet) so the
        // block-hit's release/attack/use packets are cleanly grouped before it (correct packet
        // order). The aim is still computed in EventRender above, so rotation is unaffected.
        if (event instanceof EventTick) {
            if(time == Timing.POST) return;
            if (mc.player == null || mc.level == null) return;

            Player target = getTarget();
            targetInRange = target != null;





            if (target == null) {
                // No target: stop driving the rotation and let the central smooth-out ease the
                // server yaw back to real. Keep our aim synced to real for a clean re-acquire.
                stopBlocking();
                wdSetBlock(false);
                shieldSet(false);
                watchdogBlockTicks = 0;
                watchdogHolding = false;
                lastAttacking = false;
                yaw   = mc.player.getYRot();
                pitch = mc.player.getXRot();
                yawVelocity   = 0f;
                pitchVelocity = 0f;
                rotationReleased = true;
                attacking = false;
                if (inCrit) resetCrit();
                return;
            }


            rotationReleased = false;

            RotationUtil.set(yaw, pitch);

            if (mode1_21.isEnabled()) {

                if (inCrit) {
                    if (mc.player.onGround()) {
                        resetCrit();
                        return;
                    }
//yest
                    long elapsed = critTimer.getElapsed();
                    if (mc.player == null || mc.level == null) return;
                    if (!critHit && elapsed >= critClickAt && !mc.player.isSprinting()) {
                        if (mc.player.getAttackStrengthScale(0.0f) < 1.0f) return;

                        boolean aimed = lookingAtTarget(target);
                        lookingAt = aimed;
                        if (aimed) {
                            if (!mc.player.isUsingItem()
                                    && !mc.player.isBlocking()
                                    && inAttackRange(target)) {
                                mc.gameMode.attack(mc.player, target);
                                mc.player.swing(InteractionHand.MAIN_HAND);
                                attacking = true;
                                scheduleNextAttack();
                            }
                            critHit = true;
                        }
                    }

                    if (elapsed >= critResumeAt) resetCrit();
                    return;
                }

                if (!mc.player.onGround() && mc.player.getDeltaMovement().y > 0) {
                    attacking = false;
                    return;
                }
                if (mc.player == null || mc.level == null) return;
                if (!mc.player.onGround()) {
                    if (mc.player.isSprinting() && inAttackRange(target)) {
                        ((IKeyMappingExt) mc.options.keyUp).truppware$setPressed(false);
                        mc.player.setSprinting(false);
                        inCrit       = true;
                        critHit      = false;
                        critClickAt  = ThreadLocalRandom.current().nextLong(40, 65);
                        critResumeAt = ThreadLocalRandom.current().nextLong(90, 120);
                        critTimer.reset();
                    }
                    return;
                }

                if (mc.gameMode != null && canAttack() && inAttackRange(target)
                        && mc.player.getAttackStrengthScale(0.0f) >= 1.0f) {
                    boolean aimed = lookingAtTarget(target);
                    lookingAt = aimed;
                    if (aimed && !mc.player.isUsingItem() && !mc.player.isBlocking()) {
                        mc.gameMode.attack(mc.player, target);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        attacking = true;
                        scheduleNextAttack();
                    }
                } else {
                    attacking = false;
                }

            } else {

                if (mc.gameMode != null && canAttack() && inAttackRange(target)) {
                    boolean aimed = lookingAtTarget(target);
                    lookingAt = aimed;
                    if (aimed) {
                        mc.player.magicCrit(target);
                        mc.gameMode.attack(mc.player, target);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        mc.player.magicCrit(target);
                        attacking = true;
                        scheduleNextAttack();
                    }
                } else {
                    attacking = false;
                }

            }


        }
    }

    public static boolean isEnabledStatic() { return enabled; }

    @Override
    public void onEnable() {
        lookingAt = false;
        enabled          = true;
        smoothingOut     = false;
        rotationReleased = true;
        lastCps          = 0;
        nextAttackAt     = System.currentTimeMillis();   // allow an attack right away on enable
        yawVelocity   = 0f;
        pitchVelocity = 0f;
        inCrit        = false;
        critHit       = false;
        nextBlockTime = 0L;
        unblockTime   = 0L;
        blocking      = false;
        wdBlocking    = false;
        lastAttacking = false;
        watchdogBlockTicks = 0;
        nextOffsetChange = 0;
        currentSpeedMult = 1.0f;
        if (mc.player != null) {
            yaw   = mc.player.getYRot();
            pitch = mc.player.getXRot();
            RotationUtil.serverYaw   = yaw;
            RotationUtil.serverPitch = pitch;
        }
    }

    @Override
    public void OnDisable() {
        lookingAt = false;
        enabled          = false;
        attacking        = false;
        targetInRange    = false;
        currentTarget    = null;
        currentTargetInRange = false;
        rotationReleased = true;
        lastCps          = 0;
        smoothingOut     = true;
        stopBlocking();
        wdSetBlock(false);
        shieldSet(false);
        watchdogBlockTicks = 0;
        watchdogHolding = false;
        lastAttacking = false;
        if (inCrit) resetCrit();
    }
}