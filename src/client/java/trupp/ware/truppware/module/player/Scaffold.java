package trupp.ware.truppware.module.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import trupp.ware.event.Event;
import trupp.ware.event.events.*;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.util.DirectionalInput;
import trupp.ware.util.RotationUtil;

import java.util.Random;

public class Scaffold extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final Random random = new Random();

    public static boolean enabled  = false;
    public static boolean rotating = false;

    private static final double GRIM_GCD = 0.009600000008940697D;

    private float currentYaw   = 0f;
    private float currentPitch = 80f;
    private float yawVelocity   = 0f;
    private float pitchVelocity = 0f;

    private float lockedYaw = 0f;
    private BridgeDir lockedBridgeDirEnum = null;

    private BlockPos  cachedSupport = null;
    private Direction cachedFace    = null;
    private Vec3      cachedHitVec  = null;
    private BlockPos  targetPlacement = null;
    private BlockPos  lastFeet = null;

    private int tellyLockedY = Integer.MIN_VALUE;

    public BooleanSetting telly = new BooleanSetting("Telly", false);

    public Scaffold() {
        super("Scaffold", Category.PLAYER, "Smart scaffold", GLFW.GLFW_KEY_X);
        addSettings(telly);
    }

    public static boolean isEnabledStatic() { return enabled; }

    private enum BridgeDir {
        NORTH(180f), NORTHEAST(-135f), EAST(-90f), SOUTHEAST(-45f),
        SOUTH(0f), SOUTHWEST(45f), WEST(90f), NORTHWEST(135f);

        final float yaw;
        BridgeDir(float yaw) { this.yaw = yaw; }
    }

    @Override
    public void onEvent(Event e, Timing time) {

        if (e instanceof EventMovementInput moveEvent) {
            if (!rotating) return;
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
            float diff      = (float) Math.toRadians(currentYaw - clientYaw);
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

        if (e instanceof EventRender) {
            if (mc.player == null || mc.level == null) return;


            if (findBlockSlot() == -1) {
                rotating = false;
                currentYaw = mc.player.getYRot();
                currentPitch = mc.player.getXRot();
                yawVelocity = 0f;
                pitchVelocity = 0f;
                lockedBridgeDirEnum = null;
                lockedYaw = mc.player.getYRot();
                cachedSupport = null;
                cachedFace = null;
                cachedHitVec = null;
                targetPlacement = null;
                lastFeet = null;
                RotationUtil.serverYaw = mc.player.getYRot();
                RotationUtil.serverPitch = mc.player.getXRot();
                return;
            }

            rotating = true;
            LocalPlayer player = mc.player;

            BlockPos feet = BlockPos.containing(
                    player.getX(),
                    player.getY() - 0.05,
                    player.getZ()
            );

            boolean needRefresh = cachedSupport == null
                    || cachedHitVec == null
                    || (targetPlacement != null && !mc.level.getBlockState(targetPlacement).isAir())
                    || (lastFeet == null || !lastFeet.equals(feet));

            if (needRefresh) {
                lastFeet = feet;
                cachedSupport = null;
                cachedFace = null;
                cachedHitVec = null;
                targetPlacement = null;

                BlockPos directBelow = feet.below();
                if (!mc.level.getBlockState(directBelow).isAir()) {
                    cachedSupport = directBelow;
                    cachedFace = Direction.UP;
                    cachedHitVec = Vec3.atCenterOf(directBelow).add(0, 0.5, 0);
                    targetPlacement = feet;
                }

                if (cachedSupport == null) {
                    BlockPos nearestBlock = findNearestBlock(feet);
                    if (nearestBlock != null) {
                        BlockPos placeAt = stepTowardFeet(nearestBlock, feet);
                        if (placeAt != null && mc.level.getBlockState(placeAt).isAir()) {
                            int dxs = placeAt.getX() - nearestBlock.getX();
                            int dys = placeAt.getY() - nearestBlock.getY();
                            int dzs = placeAt.getZ() - nearestBlock.getZ();

                            Direction face;
                            Vec3 hitVec;

                            if (dys > 0) {
                                face = Direction.UP;
                                hitVec = Vec3.atCenterOf(nearestBlock).add(0, 0.5, 0);
                            } else if (dys < 0) {
                                face = Direction.DOWN;
                                hitVec = Vec3.atCenterOf(nearestBlock).add(0, -0.5, 0);
                            } else if (dxs != 0) {
                                Direction sideDir = dxs > 0 ? Direction.EAST : Direction.WEST;
                                face = sideDir;
                                hitVec = Vec3.atCenterOf(nearestBlock).add(sideDir.getStepX() * 0.5, 0, 0);
                            } else {
                                Direction sideDir = dzs > 0 ? Direction.SOUTH : Direction.NORTH;
                                face = sideDir;
                                hitVec = Vec3.atCenterOf(nearestBlock).add(0, 0, sideDir.getStepZ() * 0.5);
                            }

                            cachedSupport = nearestBlock;
                            cachedFace = face;
                            cachedHitVec = hitVec;
                            targetPlacement = placeAt;
                        }
                    }
                }

                BridgeDir newBridgeDir = getBridgeDirection(player);
                if (newBridgeDir != null && newBridgeDir != lockedBridgeDirEnum) {
                    lockedBridgeDirEnum = newBridgeDir;
                    lockedYaw = newBridgeDir.yaw;
                }
            }

            if (cachedSupport == null || cachedHitVec == null) {
                RotationUtil.serverYaw = currentYaw;
                RotationUtil.serverPitch = currentPitch;
                return;
            }

            Vec3 eyePos = player.getEyePosition(1.0f);
            double dx = cachedHitVec.x - eyePos.x;
            double dy = cachedHitVec.y - eyePos.y;
            double dz = cachedHitVec.z - eyePos.z;
            double distXZ = Math.sqrt(dx * dx + dz * dz);

            float targetYaw;
            if (lockedBridgeDirEnum != null) {
                targetYaw = lockedYaw;
            } else {
                targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
            }

            float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, distXZ)));
            targetPitch = Math.max(-90f, Math.min(90f, targetPitch));

            float yawDiff = wrapDegrees(targetYaw - currentYaw);
            float pitchDiff = targetPitch - currentPitch;

            float absYaw = Math.abs(yawDiff);
            float absPitch = Math.abs(pitchDiff);


            if (lockedBridgeDirEnum != null) {
                // Predictive panic — detect standing over air or about to be
                boolean panic = false;
                BlockPos curFeet = BlockPos.containing(
                        mc.player.getX(),
                        mc.player.getY() - 0.05,
                        mc.player.getZ());

                if (mc.level.getBlockState(curFeet.below()).isAir()) panic = true;

                Vec3 mo = mc.player.getDeltaMovement();
                if (!panic && mo.lengthSqr() > 0.01) {
                    BlockPos ahead = BlockPos.containing(
                            mc.player.getX() + mo.x * 3,
                            mc.player.getY() - 0.05,
                            mc.player.getZ() + mo.z * 3);
                    if (mc.level.getBlockState(ahead.below()).isAir()) panic = true;
                }

                float speedMult = panic ? 3.0f : 1.0f;
                float maxYaw = panic ? 30f : 8f;
                float maxPitch = panic ? 30f : 8f;

                float yawNoise = 0f;
                if (absYaw > 5f && !panic) {
                    yawNoise = (random.nextFloat() - 0.5f) * 0.4f;
                }

                if (absYaw < 0.5f) {
                    currentYaw = targetYaw;
                    yawVelocity = 0f;
                } else {
                    float yawSpeed = Math.min(absYaw * 0.18f * speedMult, maxYaw);
                    float yawAccel = (yawDiff > 0 ? yawSpeed : -yawSpeed) + yawNoise;
                    yawVelocity = yawVelocity * 0.4f + yawAccel * 0.3f;
                    if (Math.abs(yawVelocity) > Math.abs(yawDiff)) yawVelocity = yawDiff;
                    currentYaw += yawVelocity;
                }

                if (absPitch < 1.0f) {
                    currentPitch = targetPitch;
                    pitchVelocity = 0f;
                } else {
                    float pitchSpeed = Math.min(absPitch * 0.2f * speedMult, maxPitch);
                    float pitchAccel = pitchDiff > 0 ? pitchSpeed : -pitchSpeed;
                    pitchVelocity = pitchVelocity * 0.4f + pitchAccel * 0.3f;
                    if (Math.abs(pitchVelocity) > Math.abs(pitchDiff)) pitchVelocity = pitchDiff;
                    currentPitch += pitchVelocity;
                }

                currentYaw -= currentYaw % GRIM_GCD;
                currentPitch -= currentPitch % GRIM_GCD;
                currentPitch = Math.max(-90f, Math.min(90f, currentPitch));

                RotationUtil.serverYaw = currentYaw;
                RotationUtil.serverPitch = currentPitch;
                return;
            }

            // Fallback for non-locked rotation
            if (absYaw < 1.5f && absPitch < 1.5f) {
                yawVelocity *= 0.5f;
                pitchVelocity *= 0.5f;
                currentYaw += yawVelocity;
                currentPitch += pitchVelocity;
                currentYaw -= currentYaw % GRIM_GCD;
                currentPitch -= currentPitch % GRIM_GCD;
                RotationUtil.serverYaw = currentYaw;
                RotationUtil.serverPitch = currentPitch;
                return;
            }

            float yawSpeed = Math.min(absYaw * 0.25f, 12f);
            float pitchSpeed = Math.min(absPitch * 0.2f, 10f);

            float yawAccel = yawDiff > 0 ? yawSpeed : -yawSpeed;
            float pitchAccel = pitchDiff > 0 ? pitchSpeed : -pitchSpeed;

            yawVelocity = yawVelocity * 0.35f + yawAccel * 0.4f;
            pitchVelocity = pitchVelocity * 0.35f + pitchAccel * 0.4f;

            if (Math.abs(yawVelocity) > Math.abs(yawDiff)) yawVelocity = yawDiff;
            if (Math.abs(pitchVelocity) > Math.abs(pitchDiff)) pitchVelocity = pitchDiff;

            currentYaw += yawVelocity;
            currentPitch += pitchVelocity;

            currentYaw -= (float) (currentYaw % GRIM_GCD);
            currentPitch -= (float) (currentPitch % GRIM_GCD);
            currentPitch = Math.max(-90f, Math.min(90f, currentPitch));

            RotationUtil.serverYaw = currentYaw;
            RotationUtil.serverPitch = currentPitch;
            return;
        }

        if (e instanceof EventTick) {
            if (mc.player == null || mc.level == null) return;
            if (mc.mouseHandler.isRightPressed()) return;

            LocalPlayer player = mc.player;

            int slot = findBlockSlot();
            if (slot == -1) return;

            BlockPos below = BlockPos.containing(
                    player.getX(),
                    player.getY() - 0.05,
                    player.getZ()
            );

            if (telly.isEnabled()) {
                handleTelly(player, below, slot);
                return;
            }

            if (!mc.level.getBlockState(below).isAir()) return;
            if (cachedSupport == null || cachedHitVec == null || cachedFace == null) return;

          //  if (!isAimedAtTarget(player)) return;

            placeBlock(player, slot);
        }
    }

    private BridgeDir getBridgeDirection(LocalPlayer player) {
        Vec3 motion = player.getDeltaMovement();
        double mx = motion.x;
        double mz = motion.z;

        if (Math.abs(mx) < 0.05 && Math.abs(mz) < 0.05) {
            if (lockedBridgeDirEnum != null) return lockedBridgeDirEnum;
            float yaw = wrapDegrees(player.getYRot());
            return yawToBridgeDir(yaw);
        }

        double angle = Math.toDegrees(Math.atan2(mz, mx));
        float motionYaw = (float)(angle - 90.0);
        motionYaw = wrapDegrees(motionYaw);

        float bridgeFaceYaw = wrapDegrees(motionYaw + 180f);

        return yawToBridgeDir(bridgeFaceYaw);
    }

    private BridgeDir yawToBridgeDir(float yaw) {
        yaw = wrapDegrees(yaw);
        if (yaw >= -22.5 && yaw < 22.5) return BridgeDir.SOUTH;
        if (yaw >= 22.5 && yaw < 67.5) return BridgeDir.SOUTHWEST;
        if (yaw >= 67.5 && yaw < 112.5) return BridgeDir.WEST;
        if (yaw >= 112.5 && yaw < 157.5) return BridgeDir.NORTHWEST;
        if (yaw >= 157.5 || yaw < -157.5) return BridgeDir.NORTH;
        if (yaw >= -157.5 && yaw < -112.5) return BridgeDir.NORTHEAST;
        if (yaw >= -112.5 && yaw < -67.5) return BridgeDir.EAST;
        return BridgeDir.SOUTHEAST;
    }

    private void handleTelly(LocalPlayer player, BlockPos below, int slot) {
        if (player.onGround()) {
            tellyLockedY = below.getY();
        }

        ((IKeyMappingExt) mc.options.keyJump).truppware$setPressed(true);

        if (tellyLockedY == Integer.MIN_VALUE) return;
        if (!mc.level.getBlockState(below).isAir()) return;
        if (cachedSupport == null || cachedHitVec == null || cachedFace == null) return;

      //  if (!isAimedAtTarget(player)) return;

        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty() || !(held.getItem() instanceof BlockItem)) {
            switchSlot(slot);
        }

        placeBlock(player, slot);
    }

    private boolean isAimedAtTarget(LocalPlayer player) {
        if (cachedSupport == null || cachedHitVec == null) return false;

        Vec3 eyePos = player.getEyePosition(1.0f);

        if (eyePos.distanceTo(cachedHitVec) > 4.5) return false;

        double yawRad = Math.toRadians(currentYaw);
        double pitchRad = Math.toRadians(currentPitch);

        double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = -Math.sin(pitchRad);
        double lookZ =  Math.cos(yawRad) * Math.cos(pitchRad);

        double reach = 4.5;
        Vec3 end = eyePos.add(lookX * reach, lookY * reach, lookZ * reach);

        BlockHitResult hit = mc.level.clip(new ClipContext(
                eyePos, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) return false;
        if (!hit.getBlockPos().equals(cachedSupport)) return false;
        if (hit.getDirection() != cachedFace) return false;

        return true;
    }

    private void placeBlock(LocalPlayer player, int slot) {
        switchSlot(slot);
        player.setShiftKeyDown(true);

        BlockHitResult hit = new BlockHitResult(cachedHitVec, cachedFace, cachedSupport, false);
        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND);

        player.setShiftKeyDown(false);
    }

    private BlockPos findNearestBlock(BlockPos feet) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = -3; dy <= 0; dy++) {
                    BlockPos pos = feet.offset(dx, dy, dz);
                    if (mc.level.getBlockState(pos).isAir()) continue;

                    double d = pos.distSqr(feet);
                    if (d < bestDist) {
                        bestDist = d;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    private BlockPos stepTowardFeet(BlockPos from, BlockPos feet) {
        int sx = Integer.signum(feet.getX() - from.getX());
        int sy = Integer.signum(feet.getY() - from.getY());
        int sz = Integer.signum(feet.getZ() - from.getZ());

        if (sy > 0 && from.getY() + 1 > feet.getY()) sy = 0;

        int absX = Math.abs(feet.getX() - from.getX());
        int absZ = Math.abs(feet.getZ() - from.getZ());

        BlockPos candidate;

        if (absX >= absZ && sx != 0) {
            candidate = from.offset(sx, 0, 0);
            if (mc.level.getBlockState(candidate).isAir() && candidate.getY() <= feet.getY()) {
                return candidate;
            }
        }
        if (sz != 0) {
            candidate = from.offset(0, 0, sz);
            if (mc.level.getBlockState(candidate).isAir() && candidate.getY() <= feet.getY()) {
                return candidate;
            }
        }
        if (sx != 0) {
            candidate = from.offset(sx, 0, 0);
            if (mc.level.getBlockState(candidate).isAir() && candidate.getY() <= feet.getY()) {
                return candidate;
            }
        }
        if (sx != 0 && sz != 0) {
            candidate = from.offset(sx, 0, sz);
            if (mc.level.getBlockState(candidate).isAir() && candidate.getY() <= feet.getY()) {
                return candidate;
            }
        }
        if (sy > 0) {
            candidate = from.above();
            if (mc.level.getBlockState(candidate).isAir() && candidate.getY() <= feet.getY()) {
                return candidate;
            }
        }
        return null;
    }

    private int findBlockSlot() {
        if (mc.player == null) return -1;
        ItemStack held = mc.player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof BlockItem bi
                && bi.getBlock() != Blocks.AIR) {
            return mc.player.getInventory().getSelectedSlot();
        }
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi
                    && bi.getBlock() != Blocks.AIR) {
                return i;
            }
        }
        return -1;
    }

    private void switchSlot(int slot) {
        try {
            java.lang.reflect.Field f = mc.player.getInventory().getClass().getDeclaredField("selected");
            f.setAccessible(true);
            f.set(mc.player.getInventory(), slot);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ((IKeyMappingExt) mc.options.keyHotbarSlots[slot]).truppware$click();
    }

    private float wrapDegrees(float value) {
        value %= 360f;
        if (value >= 180f)  value -= 360f;
        if (value < -180f) value += 360f;
        return value;
    }

    @Override
    public void onEnable() {
        enabled  = true;
        rotating = true;
        if (mc.player != null) {
            currentYaw   = mc.player.getYRot();
            currentPitch = mc.player.getXRot();
            lockedYaw    = mc.player.getYRot();
            RotationUtil.serverYaw   = currentYaw;
            RotationUtil.serverPitch = currentPitch;
            tellyLockedY = (int) Math.floor(mc.player.getY()) - 1;
        }
        yawVelocity   = 0f;
        pitchVelocity = 0f;
        lastFeet = null;
        lockedBridgeDirEnum = null;
        cachedSupport = null;
        cachedFace = null;
        cachedHitVec = null;
        targetPlacement = null;
    }

    @Override
    public void OnDisable() {
        enabled       = false;
        rotating      = false;
        cachedSupport = null;
        cachedFace    = null;
        cachedHitVec  = null;
        targetPlacement = null;
        lastFeet      = null;
        lockedBridgeDirEnum = null;
        if (mc.player != null) {
            mc.player.setShiftKeyDown(false);
            ((IKeyMappingExt) mc.options.keyJump).truppware$setPressed(false);
            RotationUtil.serverYaw   = mc.player.getYRot();
            RotationUtil.serverPitch = mc.player.getXRot();
        }
        tellyLockedY = Integer.MIN_VALUE;
    }
}