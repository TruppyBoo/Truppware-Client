package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventRender;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.NumberSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class AimAssist extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final Random random = new Random();

    private float yawVelocity = 0.0f;
    private float pitchVelocity = 0.0f;

    private static final double GRIM_GCD = 0.009600000008940697D;

    public NumberSetting range = new NumberSetting("Range", 5, 1, 10, 0.1);
    public NumberSetting strength = new NumberSetting("Strength", 3.0, 0.1, 10.0, 0.1);
    public NumberSetting fov = new NumberSetting("FOV", 10, 90, 90, 5);
    public AimAssist() {
        super("AimAssist", Category.COMBAT, "Human-like smooth aim", -1);
        addSettings(range, strength, fov);
    }

    private boolean isInFov(Player target) {
        LocalPlayer player = mc.player;
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 targetPos = target.getEyePosition(1.0f).add(0, -0.55, 0);

        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;

        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, distXZ)));

        float yawDiff = Math.abs(wrapDegrees(targetYaw - player.getYRot()));
        float pitchDiff = Math.abs(targetPitch - player.getXRot());

        float halfFov = (float) fov.getNum() / 2.0f;
        return yawDiff <= halfFov && pitchDiff <= halfFov;
    }

    @Override
    public void onEvent(Event event, Timing time) {
        if (!(event instanceof EventRender)) return;
        if (mc.player == null || mc.level == null) return;
        if (!mc.mouseHandler.isLeftPressed()) return;
        Player target = getTarget();
        if (target == null) return;

        HitResult hit = mc.hitResult;
        if(hit.getType() == HitResult.Type.BLOCK) return;


        if(!isInFov(target)) return;

        aimAt(target);
    }

    private Player getTarget() {
        LocalPlayer player = mc.player;

        List<AbstractClientPlayer> players = mc.level.players().stream()
                .filter(p -> p != player)
                .filter(p -> !p.isDeadOrDying())
                .filter(p -> p.distanceTo(player) <= range.getNum())
                .sorted(Comparator.comparingDouble(p -> p.distanceTo(player)))
                .toList();

        return players.isEmpty() ? null : players.get(0);
    }

    private void aimAt(Player target) {
        LocalPlayer player = mc.player;
        Vec3 eyePos = player.getEyePosition(1.0f);

        float maxTurn = (float) (0.25f * strength.getNum());
        float baseSmooth = (float) (0.08f * strength.getNum());

        // Aim at chest — no random offset so no jitter
        Vec3 targetPos = target.getEyePosition(1.0f).add(0, -0.35, 0);

        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;

        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, distXZ)));

        float yaw = player.getYRot();
        float pitch = player.getXRot();

        float yawDiff = wrapDegrees(targetYaw - yaw);
        float pitchDiff = targetPitch - pitch;

        float smooth = baseSmooth + random.nextFloat() * 0.04f;

        yawVelocity += yawDiff * smooth;
        pitchVelocity += pitchDiff * smooth;

        yawVelocity *= 0.85f + random.nextFloat() * 0.03f;
        pitchVelocity *= 0.85f + random.nextFloat() * 0.03f;

        yawVelocity = clamp(yawVelocity, -maxTurn, maxTurn);
        pitchVelocity = clamp(pitchVelocity, -maxTurn, maxTurn);

        float newYaw = yaw + yawVelocity;
        float newPitch = pitch + pitchVelocity;

        newYaw -= newYaw % GRIM_GCD;
        newPitch -= newPitch % GRIM_GCD;

        player.setYRot(newYaw);
        player.setXRot(clamp(newPitch, -90.0f, 90.0f));
    }

    private float wrapDegrees(float value) {
        value %= 360.0f;
        if (value >= 180.0f) value -= 360.0f;
        if (value < -180.0f) value += 360.0f;
        return value;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}