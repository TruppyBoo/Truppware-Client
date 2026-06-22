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

    private long nextOffsetChange = 0;
    private float currentSpeedMult = 1.0f;

    // Smooth aim-point wander, expressed as ANGLES/FRACTIONS rather than a positional offset. A fixed
    // positional lateral offset turns into a huge yaw the moment you're point-blank (a 0.16-block
    // offset over a 0.3-block separation is ~28 deg) — that's what made the aim spin a whole turn to
    // reach someone right beside you. Driving the wander as a fraction of how big the target looks
    // keeps it a few degrees at every range, so it never spins and always stays on the body.
    private float  aimYawOff;          // horizontal wander, fraction of the target's angular half-width [-0.6..0.6]
    private float  aimYawAnchor;       // value aimYawOff eases toward
    private double aimHeight = 0.6;    // vertical aim point, fraction of hitbox height (chest..head)
    private double aimHeightAnchor = 0.6;

    // --- Human aim model: discrete min-jerk submovements (ballistic flick -> corrective settles ->
    //     closed-loop tracking), a reaction delay before big flicks, and band-limited hand tremor.
    //     This reproduces the accelerate-peak-decelerate velocity profile of a real hand, which is
    //     what ML/heuristic aim checks key on, while still settling tight on the target (accurate). ---
    private float moveStartYaw, moveStartPitch;   // where the current submovement began
    private float moveEndYaw,   moveEndPitch;     // point captured when it began (ballistic target)
    private long  moveStartMs;                    // when it began (ms)
    private long  moveDurMs;                      // its planned duration (ms)
    private boolean moving;                        // a ballistic submovement is in flight
    private boolean reacting;                      // counting down a reaction delay before a flick
    private long  reactUntil;                     // flick may start once now >= this
    private double tremorPhase1, tremorPhase2;     // band-limited tremor oscillator phases
    private long  lastFrameMs;                     // previous frame time, for tremor dt

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
    // % of swings that actually connect. The rest are genuine whiffs (arm swings, no damage), so a
    // perfect 100%-hit pattern never shows up — that robotic tell is what gets a legit-aim flagged.
    public NumberSetting hitChance = new NumberSetting("HitChance", 1, 100, 92, 1);
    // Hard cap on how far up/down the aim tilts. Real players don't pin the crosshair straight
    // down/up in a fight — a dead-centre steep pitch (esp. point-blank / on top of someone) is the
    // obvious killaura tell. 90 = no cap (vanilla full range).
    public NumberSetting maxPitch = new NumberSetting("MaxPitch", 30, 90, 75, 1);
    // How fast the vertical aim moves relative to the horizontal. <1 makes pitch lag yaw (a real
    // hand snaps left/right and the wrist catches the up/down a beat later). 1 = pitch keeps pace.
    public NumberSetting pitchSpeed = new NumberSetting("PitchSpeed", 0.2, 1.0, 0.6, 0.05);
    // Raycast: ON = only swing when the silent-aim ray actually lands on the target's hitbox with a
    // clear line of sight (legit — won't hit through walls or when the aim isn't on them yet). OFF =
    // attack on range alone (aggressive; ignores aim/line-of-sight).
    public BooleanSetting raycast = new BooleanSetting("Raycast", true);

    public Aura() {
        super("Aura", Category.COMBAT, "Attacks nearest target", 0);
        addSettings(range, preRange, minCPS, maxCPS, strength, hitChance, maxPitch, pitchSpeed, raycast, blockHit, blockMode, blockMin, blockMax, moveFix, mode1_21, team, antiBot);
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
     * from the eye along the silent-aim yaw/pitch against the TARGET'ds bounding box inflated by a
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

    /**
     * True if ANY part of {@code target} is visible from our eye (no solid block fully blocking the
     * way). Checked at the head, centre and feet, so a target merely peeking counts as visible. Used
     * to gate 1.8 pre-range clicking — so it never swings into a wall when the target is behind blocks.
     */
    private boolean targetVisible(Player target) {
        if (mc.player == null || mc.level == null || target == null) return false;
        Vec3 eye = mc.player.getEyePosition(1.0f);
        AABB box = target.getBoundingBox();
        Vec3 c = box.getCenter();
        Vec3[] points = {
                target.getEyePosition(1.0f),                 // head
                c,                                           // centre mass
                new Vec3(c.x, box.minY + 0.1, c.z)           // feet
        };
        for (Vec3 p : points) {
            HitResult r = mc.level.clip(new ClipContext(eye, p,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (r.getType() != HitResult.Type.BLOCK) return true;   // this point is in the clear
        }
        return false;
    }

    /** Whether we're "aimed" enough to swing, honouring the Raycast toggle. Raycast ON requires the
     *  silent-aim ray to land on the hitbox (with line of sight); OFF treats us as always aimed so the
     *  attack is gated by range alone. */
    private boolean aimedAtTarget(Player target) {
        return !raycast.isEnabled() || lookingAtTarget(target);
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

        // ---- Aim point. A real player's aim wanders instead of pinning dead-centre, so we pick a
        //      fresh random wander now and then and EASE toward it (smooth, never a snap). But the
        //      wander is kept as an ANGLE, scaled to how big the target looks, NOT a fixed position
        //      on the body — a positional offset becomes a wild angle point-blank and spins the aim.
        if (now >= nextOffsetChange) {
            aimYawAnchor    = (random.nextFloat() - 0.5f) * 1.2f;            // +-0.6 of the body's half-width
            aimHeightAnchor = 0.52 + random.nextDouble() * 0.32;            // chest..head, fraction of height
            nextOffsetChange = now + 380 + (long) (random.nextDouble() * 620);
        }
        aimYawOff += (aimYawAnchor - aimYawOff) * 0.05f;                     // smooth horizontal wander
        aimHeight += (aimHeightAnchor - aimHeight) * 0.05;                  // smooth vertical wander

        // Yaw aims at the target's CENTRE LINE (x,z) — always well-defined — plus the small angular
        // wander; pitch aims at the wandering height. distXZ is floored so a point-blank target can't
        // blow the pitch (and the centre line keeps the yaw stable) — no more 360s when you're beside them.
        double dx     = target.getX() - eyePos.x;
        double dz     = target.getZ() - eyePos.z;
        double aimY   = target.getY() + target.getBbHeight() * aimHeight;
        double dy     = aimY - eyePos.y;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float maxP = (float) maxPitch.getNum();
        float truePitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.max(distXZ, 0.15))));
        truePitch = clamp(truePitch, -maxP, maxP);

        float trueYaw;
        if (distXZ < 0.08) {
            // Essentially standing inside them: the horizontal angle is undefined, so don't chase it
            // (that's the degenerate case that spins). Hold the current yaw.
            trueYaw = this.yaw;
        } else {
            float centerYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            // Half the target's apparent width in degrees -> the wander (a fraction of it) is a few
            // degrees up close and less at range, always landing inside the body. Accurate everywhere.
            float halfAngle = (float) Math.toDegrees(Math.atan2(target.getBbWidth() * 0.5, distXZ));
            trueYaw = centerYaw + aimYawOff * halfAngle;
        }

        // ---- Angular error from where we currently aim to that point. ----
        float errYaw   = wrapDegrees(trueYaw - this.yaw);
        float errPitch = truePitch - this.pitch;
        float err      = (float) Math.sqrt(errYaw * errYaw + errPitch * errPitch);

        float baseYaw, basePitch;

        // Below TRACK we follow continuously (closed-loop, always moving); a bigger gap is a quick
        // flick. Only a GENUINELY large gap (fresh target / big reposition) gets an occasional, short
        // reaction — and even then the aim keeps creeping, never freezing, so it's always rotating.
        final float TRACK_THRESH = 10.0f;
        final float BIG_THRESH   = 28.0f;

        if (moving) {
            // Ballistic submovement to the point captured when the flick began. It ramps up quickly
            // from rest, then HOLDS a high speed across the travel and only eases off in the final
            // stretch — the way a real fast flick covers most of the distance at speed and brakes
            // just as it arrives, instead of coasting down the whole way in. Smooth ramps at both
            // ends keep the acceleration continuous (no robotic jerk for ML checks to catch).
            float tau = moveDurMs <= 0 ? 1f : clamp((now - moveStartMs) / (float) moveDurMs, 0f, 1f);
            baseYaw   = moveStartYaw + wrapDegrees(moveEndYaw - moveStartYaw) * flickEase(tau);
            // Pitch runs a beat behind yaw (PitchSpeed), on the same profile so it's smooth too.
            float tauP = clamp(tau * (float) pitchSpeed.getNum(), 0f, 1f);
            basePitch  = moveStartPitch + (moveEndPitch - moveStartPitch) * flickEase(tauP);
            if (tau >= 1f) moving = false;
        } else if (err <= TRACK_THRESH) {
            // Locked on: closed-loop micro-tracking. Cover a chunk of the (small) error each frame so
            // we sit tight on a strafing target — this is what keeps the aim accurate between flicks.
            reacting = false;
            float fy = 0.36f + random.nextFloat() * 0.22f;   // snappier -> stays glued -> swings far more
            float fp = fy * (float) pitchSpeed.getNum();   // vertical correction gentler than yaw
            baseYaw   = this.yaw   + errYaw   * fy;
            basePitch = this.pitch + errPitch * fp;
        } else if (err > BIG_THRESH) {
            // Large gap: a real player sometimes reacts before a big flick, but not every time. Only
            // occasionally arm a SHORT reaction, and creep toward the target meanwhile (no dead pause).
            if (!reacting && random.nextFloat() < 0.35f) {
                reacting = true; reactUntil = now + 40 + (long) (random.nextDouble() * 90);
            }
            if (reacting && now < reactUntil) {
                baseYaw   = this.yaw   + errYaw   * 0.14f;                 // keep rotating, just slowly
                basePitch = this.pitch + errPitch * 0.14f * (float) pitchSpeed.getNum();
            } else {
                planFlick(now, errYaw, errPitch, err);
                reacting = false;
                baseYaw = this.yaw; basePitch = this.pitch;               // the flick takes over next frame
            }
        } else {
            // Moderate gap (the bulk of combat): flick immediately, no reaction -> continuous motion.
            planFlick(now, errYaw, errPitch, err);
            baseYaw = this.yaw; basePitch = this.pitch;
        }

        // ---- Physiological hand tremor: band-limited (~9 & ~11 Hz sinusoids), NOT white noise, so
        //      the frequency content matches a real hand. Tiny amplitude (well inside the hitbox),
        //      growing slightly with motion and fading when settled. ----
        float dt = (lastFrameMs == 0L) ? 0.016f : Math.min(0.05f, (now - lastFrameMs) / 1000f);
        lastFrameMs = now;
        tremorPhase1 += 2.0 * Math.PI * 9.0  * dt;
        tremorPhase2 += 2.0 * Math.PI * 11.3 * dt;
        float amp = 0.035f + 0.06f * Math.min(1f, err / 30f);
        baseYaw   += (float) (Math.sin(tremorPhase1) * amp + Math.sin(tremorPhase2) * amp * 0.5);
        basePitch += (float) (Math.cos(tremorPhase1 * 1.07) * amp * 0.55);

        // Snap to the rotation GCD (consecutive deltas stay GCD multiples -> no GcdRotation flag).
        float newYaw   = baseYaw;
        float newPitch = basePitch;
        newYaw   -= newYaw   % GRIM_GCD;
        newPitch -= newPitch % GRIM_GCD;

        this.yaw   = newYaw;
        this.pitch = clamp(newPitch, -90f, 90f);
    }

    /**
     * Flick position profile s(t), 0->1. Unlike a symmetric min-jerk (which starts braking at the
     * halfway point, so you feel it slow the whole way in), this holds a near-constant high speed
     * across the middle of the travel and only decelerates in the final stretch: a quick ease-in from
     * rest, a fast plateau, then a short ease-out as it lands on the target. The ease ramps are
     * smoothstep, so acceleration stays continuous at both ends (no detectable jerk).
     */
    private float flickEase(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        final float a = 0.16f;                       // accel fraction: quick speed-up from rest
        final float d = 0.22f;                       // decel fraction: short, at the very end ("over them")
        final float plateauEnd = 1f - d;
        final float n = 1f - 0.5f * a - 0.5f * d;    // area under the velocity profile (normaliser)
        float area;
        if (t < a) {
            area = rampArea(t / a) * a;                                       // within the accel ramp
        } else if (t < plateauEnd) {
            area = 0.5f * a + (t - a);                                        // accel done + plateau so far
        } else {
            float td = (t - plateauEnd) / d;                                  // progress through the decel ramp
            area = 0.5f * a + (plateauEnd - a) + (td - rampArea(td)) * d;
        }
        return area / n;
    }

    /** Integral of the smoothstep velocity ramp u^2(3-2u) from 0..x (x in [0,1]) = x^3 - 0.5 x^4. */
    private float rampArea(float x) {
        return x * x * x - 0.5f * x * x * x * x;
    }

    /** Begin a ballistic submovement toward the current error. It undershoots only slightly so a
     *  natural corrective follows without dragging, and — crucially — sizes the duration by the LOG
     *  of the distance (Fitts's law), not linearly: a full 180 turn-around takes only a little longer
     *  than a 60, so spinning to face someone behind you is quick instead of a slow grind. Capped so
     *  it can never take "forever", and scaled by the Strength knob. */
    private void planFlick(long now, float errYaw, float errPitch, float err) {
        float cover = 0.90f + random.nextFloat() * 0.07f;               // land 90-97% (small correction)
        moveStartYaw   = this.yaw;
        moveStartPitch = this.pitch;
        moveEndYaw     = this.yaw   + errYaw   * cover;
        moveEndPitch   = this.pitch + errPitch * cover;

        float strengthMul = 1.25f - 0.85f * (float) strength.getNum();   // higher Strength -> shorter
        // Logarithmic growth: big angles cost only a bit more time than small ones (a 180 ~ 230ms at
        // default Strength, a 45 ~ 160ms), so turn-arounds are fast yet still ease in/out smoothly.
        float dur = (55f + 55f * (float) Math.log1p(err / 12.0)) * Math.max(0.30f, strengthMul);
        dur = Math.min(dur, 420f * Math.max(0.30f, strengthMul));        // hard cap -> never drags
        moveDurMs   = (long) dur + (long) (random.nextDouble() * 40);    // organic spread
        moveStartMs = now;
        moving      = true;
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

    /**
     * Hit-chance roll, evaluated once per click. True = this click deals damage; false = a clean
     * whiff (no attack packet, so there's no reach/hitbox event for the server to validate). The arm
     * still swings either way — see {@link #clickAttack} — so a miss never interrupts the clicking.
     */
    private boolean rollHitChance() {
        double hc = hitChance.getNum();
        if (hc >= 100.0) return true;
        return random.nextDouble() * 100.0 < hc;
    }

    /**
     * Single point of truth for a click. Whenever it's CALLED the arm swings and the CPS cadence
     * advances; the attack packet (the actual damage) is sent only when {@code aimed} is true AND the
     * hit-chance roll passes — so a miss still swings, it just deals no damage. Returns true if damage
     * was dealt, so callers can chain the bits that only pair with a real hit (block-hit interact).
     *
     * <p>Whether it's called every cycle (continuous clicking) or only when on-target is the caller's
     * choice: 1.8/legacy calls it every cycle so the arm spams like a real 1.8 player; 1.21 is
     * cooldown-gated, so it's only called when we can actually hit — no extra arm-resetting clicks.
     */
    private boolean clickAttack(Player target, boolean aimed) {
        boolean hit = aimed && rollHitChance();
        if (hit && mc.gameMode != null) mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
        attacking = true;
        scheduleNextAttack();
        return hit;
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
            boolean aimed = aimedAtTarget(target);
            lookingAt = aimed;
            clickAttack(target, aimed);   // attacking with a raised shield is legal
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
                boolean aimed = aimedAtTarget(target);
                lookingAt = aimed;
                clickAttack(target, aimed);
            } else {
                attacking = false;
            }
            return;
        }

        boolean aimed = aimedAtTarget(target);
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
                boolean hit = clickAttack(target, aimed);          // always clicks; deals damage if aimed
                if (hit) sendInteract(target);                     // interact only pairs a real hit (lets J allow the use)
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
                // Claim the silent rotation tdhis frame. RotationUtil eases it back to real and
                // releases on its own once we stop claiming (target lost / module disabled).
                RotationUtil.set(yaw, pitch);
                //mc.player.setYRot(yaw);
                //mc.player.setXRot(pitch);
            } else {
                // Idle: keep our aim synced to the real rotation so re-acquiring starts smoothly
                // (and the move-fix is a no-op). The central smooth-out handles the server yaw.
                yaw   = mc.player.getYRot();
                pitch = mc.player.getXRot();
                yawVelocity = 0f;
                pitchVelocity = 0f;
                moving   = false;
                reacting = false;
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
                moving   = false;
                reacting = false;
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

                        boolean aimed = aimedAtTarget(target);
                        lookingAt = aimed;
                        boolean canHit = aimed && !mc.player.isUsingItem()
                                && !mc.player.isBlocking() && inAttackRange(target);
                        // 1.21: only swing when we can actually hit (no extra cooldown-wasting clicks).
                        if (canHit) {
                            clickAttack(target, true);
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
                    boolean aimed = aimedAtTarget(target);
                    lookingAt = aimed;
                    boolean canHit = aimed && !mc.player.isUsingItem() && !mc.player.isBlocking();
                    // 1.21 is cooldown-gated: only swing when we can actually hit, so there are no
                    // extra arm-resetting clicks between real attacks. (1.8 spams; 1.21 must not.)
                    if (canHit) {
                        clickAttack(target, true);
                    } else {
                        attacking = false;
                    }
                } else {
                    attacking = false;
                }

            } else {

                // 1.8: click throughout the whole pre-range (you keep swinging as they close in).
                // With Raycast ON we only click with a real line of sight (never into a wall) and only
                // deal damage when actually aimed; with Raycast OFF there are NO aim/LOS checks at all
                // — when the timer is up we just attack (still bounded by real reach). Damage is dealt
                // only at TRUE attack range and when the hit-chance roll passes.
                boolean los = !raycast.isEnabled() || targetVisible(target);   // Raycast off -> no LOS gate
                if (mc.gameMode != null && canAttack() && los) {
                    boolean aimed = aimedAtTarget(target);
                    lookingAt = aimed;
                    boolean hit = aimed && inAttackRange(target) && rollHitChance();
                    if (hit) {
                        mc.player.magicCrit(target);
                        mc.gameMode.attack(mc.player, target);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        mc.player.magicCrit(target);
                    } else {
                        mc.player.swing(InteractionHand.MAIN_HAND);   // pre-range click (no damage out of true reach / not yet aimed)
                    }
                    attacking = true;
                    scheduleNextAttack();
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
        aimYawOff       = 0f;
        aimYawAnchor    = 0f;
        aimHeight       = 0.6;
        aimHeightAnchor = 0.6;
        moving      = false;
        reacting    = false;
        reactUntil  = 0;
        moveStartMs = 0;
        lastFrameMs = 0;
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