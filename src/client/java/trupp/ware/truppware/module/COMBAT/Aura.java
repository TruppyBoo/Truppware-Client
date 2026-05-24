package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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

    // BlockHit state
    private long nextBlockTime = 0L;
    private long unblockTime   = 0L;
    private boolean blocking   = false;
    private boolean lastAttacking = false;

    // Watchdog blockhit state
    private int watchdogBlockTicks = 0;

    public static boolean attacking;
    public static boolean targetInRange;
    public static float   yaw, pitch;
    public static boolean enabled;
    public static boolean rotationReleased = true;

    private static final double GRIM_GCD = 0.009600000008940697D;

    public BooleanSetting team = new BooleanSetting("ignore team", true);
    public NumberSetting range    = new NumberSetting("Range",    1,   10,  5,   0.1);
    public NumberSetting preRange = new NumberSetting("PreRange", 0,   3,   1.0, 0.1);
    public NumberSetting minCPS   = new NumberSetting("MinCPS",   1,   20,  8,   1);
    public NumberSetting maxCPS   = new NumberSetting("MaxCPS",   1,   20,  14,  1);
    public NumberSetting strength = new NumberSetting("Strength", 0.1, 10.0, 5.0, 0.01);
    public NumberSetting blockMin = new NumberSetting("BlockMin", 1,   20,  8,   1);
    public NumberSetting blockMax = new NumberSetting("BlockMax", 1,   20,  12,  1);
    public BooleanSetting blockHit = new BooleanSetting("BlockHit", true);
    public ModeSetting blockMode = new ModeSetting("BlockMode", List.of("Normal", "Watchdog"));
    public BooleanSetting moveFix  = new BooleanSetting("MoveFix",  true);
    public BooleanSetting mode1_21 = new BooleanSetting("1.21Mode", false);

    public Aura() {
        super("Aura", Category.COMBAT, "Attacks nearest target", 0);
        addSettings(range, preRange, minCPS, maxCPS, strength, blockHit, blockMode, blockMin, blockMax, moveFix, mode1_21, team);
    }

    private Player getTarget() {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            targetInRange = false;
            return null;
        }

        Vec3 eyePos = player.getEyePosition(1.0f);
        double acquireRange = range.getNum() + preRange.getNum();
        List<AbstractClientPlayer> players;
        if(team.enabled) {
            players = mc.level.players().stream()
                    .filter(p -> p != player)
                    .filter(p -> !p.isDeadOrDying())
                    .filter(p -> !p.isInvisible())
                    .filter(p -> p.getTeam() == null || p.getTeam() != player.getTeam())
                    .filter(p -> boxDist(eyePos, p) <= acquireRange)
                    .sorted(Comparator.comparingDouble(p -> boxDist(eyePos, p)))
                    .toList();
        }else{
            players = mc.level.players().stream()
                    .filter(p -> p != player)
                    .filter(p -> !p.isDeadOrDying())
                    .filter(p -> !p.isInvisible())
                    .filter(p -> p.getTeam() == null)
                    .filter(p -> boxDist(eyePos, p) <= acquireRange)
                    .sorted(Comparator.comparingDouble(p -> boxDist(eyePos, p)))
                    .toList();
        }

        Player target = players.isEmpty() ? null : players.get(0);
        targetInRange = target != null;
        return target;
    }

    private boolean inAttackRange(Player target) {
        if (target == null || mc.player == null) return false;
        Vec3 eye = mc.player.getEyePosition(1.0f);
        return boxDist(eye, target) <= range.getNum();
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
            double offX = (random.nextDouble() - 0.5) * 1;
            double offY = 1.0 + random.nextDouble() * 0.2;
            double offZ = (random.nextDouble() - 0.5) * 1;
            targetOffset = new Vec3(offX, offY, offZ);

            nextOffsetChange = now + 100 + (long)(random.nextDouble() * 400);
            currentSpeedMult = 0.5f + random.nextFloat() * 1.0f;
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
        float absYaw = Math.abs(yawDiff);
        float absPitch = Math.abs(pitchDiff);

        float yawT = Math.min(absYaw / 90f, 1.0f);
        float yawEase = yawT * yawT * (3f - 2f * yawT);
        float yawSpeed = (absYaw * 0.18f + yawEase * 12f) * str * currentSpeedMult;
        yawSpeed = Math.min(yawSpeed, 25f * str);

        float pitchT = Math.min(absPitch / 90f, 1.0f);
        float pitchEase = pitchT * pitchT * (3f - 2f * pitchT);
        float pitchSpeed = (absPitch * 0.18f + pitchEase * 8f) * str * currentSpeedMult;
        pitchSpeed = Math.min(pitchSpeed, 18f * str);

        float yawAccel   = yawDiff   > 0 ? yawSpeed   : -yawSpeed;
        float pitchAccel = pitchDiff > 0 ? pitchSpeed : -pitchSpeed;

        yawAccel   += (random.nextFloat() - 0.5f) * 0.08f * str;
        pitchAccel += (random.nextFloat() - 0.5f) * 0.04f * str;

        float friction = 0.32f + random.nextFloat() * 0.08f;
        yawVelocity   = yawVelocity   * friction + yawAccel   * (1f - friction);
        pitchVelocity = pitchVelocity * friction + pitchAccel * (1f - friction);

        if (Math.abs(yawVelocity)   > Math.abs(yawDiff))   yawVelocity   = yawDiff;
        if (Math.abs(pitchVelocity) > Math.abs(pitchDiff)) pitchVelocity = pitchDiff;

        float newYaw   = this.yaw   + yawVelocity;
        float newPitch = this.pitch + pitchVelocity;

        newYaw   -= newYaw   % GRIM_GCD;
        newPitch -= newPitch % GRIM_GCD;

        this.yaw   = newYaw;
        this.pitch = clamp(newPitch, -90f, 90f);
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

        double cps = ThreadLocalRandom.current().nextDouble(min, max);
        if (lastCps != 0) {
            double drift = ThreadLocalRandom.current().nextDouble(-0.5, 0.5);
            cps = Math.max(min, Math.min(max, lastCps + drift));
        }
        lastCps = cps;

        long delay = (long) (1000.0 / cps);
        delay += ThreadLocalRandom.current().nextLong(-3, 4);
        return Math.max(1, delay);
    }

    private boolean canAttack() {
        if (attackTimer.hasElapsed(currentDelay)) {
            currentDelay = generateDelay();
            return true;
        }
        return false;
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
        if(blocking)
            if (useKey instanceof IKeyMappingExt ext) ext.truppware$setPressed(true);
        blocking = true;
        mc.player.magicCrit(target);
        if(!targetInRange)
            stopBlocking();
    }

    private void handleBlockHit(Player target) {

        HitResult h = RaycastUtil.raycast(mc, 2.9, yaw, pitch);
        if (h == null){
            stopBlocking();
            return;
        }

        if (!blockHit.isEnabled()) {
            stopBlocking();
            watchdogBlockTicks = 0;
            return;
        }
        if (mc.player == null) return;
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
        if (!mc.player.onGround()&& mc.player.fallDistance <= 0) {
//            stopBlocking();
//            return;
        }
        if(mc.player.isSprinting()){
            stopBlocking();
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

        if (event instanceof EventRender) {
            if (mc.player == null) return;

            if (smoothingOut) {
                boolean done = smoothBackToPlayer();
                if (done) smoothingOut = false;
                return;
            }

            Player target = getTarget();
            targetInRange = target != null;

            if (targetInRange) {
                calculateRotation(target);
            } else if (!rotationReleased) {
                smoothBackToPlayer();
            }

            //RotationUtil.serverYaw   = yaw;
            //RotationUtil.serverPitch = pitch;
        }
        if (event instanceof EventMovementInput moveEvent) {

            if (mc.player == null) return;

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
            float diff      = (float) Math.toRadians(yaw - clientYaw);
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
        if (event instanceof EventTick) {
            if (mc.player == null || mc.level == null) return;

            if (smoothingOut) {
                RotationUtil.serverYaw   = yaw;
                RotationUtil.serverPitch = pitch;
                stopBlocking();
                watchdogBlockTicks = 0;
                return;
            }

            Player target = getTarget();
            targetInRange = target != null;

            if (target == null) {
                stopBlocking();
                watchdogBlockTicks = 0;
                lastAttacking = false;

                if (rotationReleased) {
                    yaw   = mc.player.getYRot();
                    pitch = mc.player.getXRot();
                    RotationUtil.serverYaw   = yaw;
                    RotationUtil.serverPitch = pitch;
                    yawVelocity   = 0f;
                    pitchVelocity = 0f;
                    attacking = false;
                    if (inCrit) resetCrit();
                    return;
                }

                boolean done = smoothBackToPlayer();
                if (done) {
                    rotationReleased = true;
                    yaw   = mc.player.getYRot();
                    pitch = mc.player.getXRot();
                    RotationUtil.serverYaw   = yaw;
                    RotationUtil.serverPitch = pitch;
                    attacking = false;
                    if (inCrit) resetCrit();
                    return;
                }


                attacking = false;
                if (inCrit) resetCrit();
                return;
            }

            rotationReleased = false;
            if (!mode1_21.isEnabled() && canAttack() && !inAttackRange(target)) {
                ((IKeyMappingExt) mc.options.keyAttack).truppware$click();
                ((IKeyMappingExt) mc.options.keyAttack).truppware$click();
            }

            RotationUtil.serverYaw   = yaw;
            RotationUtil.serverPitch = pitch;

            if (mode1_21.isEnabled()) {

                if (inCrit) {
                    if (mc.player.onGround()) {
                        resetCrit();
                        return;
                    }

                    long elapsed = critTimer.getElapsed();

                    if (!critHit && elapsed >= critClickAt && !mc.player.isSprinting()) {
                        if (mc.player.getAttackStrengthScale(0.0f) < 1.0f) return;

                        HitResult h = RaycastUtil.raycast(mc, range.getNum(), yaw, pitch);
                        if (h instanceof EntityHitResult ehr
                                && ehr.getEntity() instanceof LivingEntity
                                && !mc.player.isUsingItem()
                                && !mc.player.isBlocking()
                                && inAttackRange(target)) {
                            mc.gameMode.attack(mc.player, target);
                            mc.player.swing(InteractionHand.MAIN_HAND);
                            attacking = true;
                            attackTimer.reset();
                        }
                        critHit = true;
                    }

                    if (elapsed >= critResumeAt) resetCrit();
                    return;
                }

                if (!mc.player.onGround() && mc.player.getDeltaMovement().y > 0) {
                    attacking = false;
                    return;
                }

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
                    HitResult h = RaycastUtil.raycast(mc, range.getNum(), yaw, pitch);
                    if (h == null) return;
                    if (h instanceof EntityHitResult ehr
                            && ehr.getEntity() instanceof LivingEntity
                            && !mc.player.isUsingItem()
                            && !mc.player.isBlocking()) {
                        mc.gameMode.attack(mc.player, target);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        attacking = true;
                        attackTimer.reset();
                    }
                } else {
                    attacking = false;
                }

            } else {

                if (mc.gameMode != null && canAttack() && inAttackRange(target)) {
                    HitResult h = RaycastUtil.raycast(mc, range.getNum(), yaw, pitch);
                    if (h == null) return;
                    if (h instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity) {
                        mc.gameMode.attack(mc.player, target);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        attacking = true;
                        attackTimer.reset();
                    }
                } else {
                    attacking = false;
                }
                handleBlockHit(target);

            }


        }
    }

    public static boolean isEnabledStatic() { return enabled; }

    @Override
    public void onEnable() {
        enabled          = true;
        smoothingOut     = false;
        rotationReleased = true;
        currentDelay     = generateDelay();
        attackTimer.reset();
        yawVelocity   = 0f;
        pitchVelocity = 0f;
        inCrit        = false;
        critHit       = false;
        nextBlockTime = 0L;
        unblockTime   = 0L;
        blocking      = false;
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
        enabled          = false;
        attacking        = false;
        targetInRange    = false;
        rotationReleased = true;
        lastCps          = 0;
        smoothingOut     = true;
        stopBlocking();
        watchdogBlockTicks = 0;
        lastAttacking = false;
        if (inCrit) resetCrit();
    }
}