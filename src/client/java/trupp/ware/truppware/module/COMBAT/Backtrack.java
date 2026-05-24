package trupp.ware.truppware.module.COMBAT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import trupp.ware.event.Event;
import trupp.ware.event.events.*;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.ColourUtil;
import trupp.ware.util.TimerUtil;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Backtrack extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final TimerUtil holdTimer = new TimerUtil();
    private Entity currentTarget = null;
    private Vec3 realPosition = Vec3.ZERO;
    private boolean active = false;
    private boolean releasing = false;
    public double dis = 0;
    private final List<Packet<?>> heldPackets = new ArrayList<>();
    public NumberSetting range = new NumberSetting("Range", 3, 1, 6, 0.5);
    public NumberSetting maxHoldTime = new NumberSetting("MaxHoldTime", 500, 100, 1000, 50);
    public Backtrack() {
        super("Backtrack", Category.COMBAT, "Delays target movement packets to hit old positions", GLFW.GLFW_KEY_UNKNOWN);
        addSettings(range, maxHoldTime);
    }
    @Override
    public void onEvent(Event e, Timing time) {
        // === TICK ===
        if (e instanceof EventTick) {
            if (mc.player == null || mc.level == null) {
                hardReset();
                return;
            }
            Entity newTarget = findTarget();
            // Target changed or lost
            if (newTarget != currentTarget) {
                releaseAll();
                currentTarget = newTarget;
                if (newTarget != null) {
                    realPosition = newTarget.position();
                }
                holdTimer.reset();
            }
            if (currentTarget == null) {
                active = false;
                return;
            }
            dis = mc.player.distanceTo(currentTarget);
            // Decide whether to hold
            Vec3 eyePos = mc.player.getEyePosition();
            double distDisplayed = boxDist(eyePos, currentTarget.getBoundingBox());
            double distReal = boxDistAtPos(eyePos, currentTarget, realPosition);
            // Hold when: old position is closer AND in range AND not timed out
            boolean shouldHold = distDisplayed <= range.getNum()
                    && distReal > distDisplayed
                    && !holdTimer.hasElapsed((long) maxHoldTime.getNum());
            if (shouldHold) {
                if (!active) {
                    active = true;
                    holdTimer.reset();
                }
            } else {
                if (active) {
                    releaseAll();
                    active = false;
                }
            }
        }
        // === ATTACK: release so hit registers, then entity snaps to real position ===
        if (e instanceof EventAttack) {
            EventAttack event = (EventAttack) e;
            dis = event.entity.distanceTo(mc.player);
            releaseAll();
            active = false;
            if (currentTarget != null) {
                realPosition = currentTarget.position();
            }
            holdTimer.reset();
        }
        // === PACKETS (incoming only — we never touch outgoing) ===
        if (e instanceof EventPacket eventPacket) {
            Packet<?> packet = eventPacket.getPacket();
            if (eventPacket.getPacketDih() == PacketDih.INCOMING) {
                // Safety resets — always let these through and clear state
                if (packet instanceof ClientboundPlayerPositionPacket
                        || packet instanceof ClientboundDisconnectPacket
                        || packet instanceof ClientboundRespawnPacket) {
                    hardReset();
                    return;
                }
                if (packet instanceof ClientboundSetHealthPacket hp && hp.getHealth() <= 0) {
                    hardReset();
                    return;
                }
                // Don't intercept while releasing
                if (releasing) return;
                if (!active) return;
                Entity target = currentTarget;
                if (target == null) return;
                // --- Target move packet: track real position + hold ---
                if (packet instanceof ClientboundMoveEntityPacket movePacket) {
                    Entity packetEntity = movePacket.getEntity(mc.level);
                    if (packetEntity == target && movePacket.hasPosition()) {
                        realPosition = realPosition.add(
                                movePacket.getXa() / 4096.0,
                                movePacket.getYa() / 4096.0,
                                movePacket.getZa() / 4096.0
                        );
                        heldPackets.add(packet);
                        e.canceled = true;
                        return;
                    }
                }
                // --- Target teleport packet: track real position + hold ---
                if (packet instanceof ClientboundTeleportEntityPacket teleportPacket) {
                    if (teleportPacket.id() == target.getId()) {
                        realPosition = teleportPacket.change().position();
                        heldPackets.add(packet);
                        e.canceled = true;
                        return;
                    }
                }
                // Everything else passes through untouched — no packets added, removed, or altered
            }
            // OUTGOING: we do absolutely nothing. All outgoing packets flow normally.
        }
        // === RENDER / HUD ===
        if (e instanceof EventRender) {
            EventRender event = (EventRender) e;
            GuiGraphics graphics = event.getGuiGraphics();
            Font font = mc.font;
            if (mc.player == null || mc.player.isDeadOrDying()) {
                hardReset();
                return;
            }
            DecimalFormat df = new DecimalFormat("0.00");
        //    graphics.drawString(font, df.format(dis), 150, 100,
                    //ColourUtil.getRainbow(15, 4f), false);
            if (active && !heldPackets.isEmpty() && currentTarget != null) {
                Vec3 eyePos = mc.player.getEyePosition();
                double dDisp = Math.round(boxDist(eyePos, currentTarget.getBoundingBox()) * 10.0) / 10.0;
                double dReal = Math.round(boxDistAtPos(eyePos, currentTarget, realPosition) * 10.0) / 10.0;
                String text = "BT: " + dDisp + " -> " + dReal + " [" + heldPackets.size() + "]";
                int cx = graphics.guiWidth() / 2;
                int cy = graphics.guiHeight() / 2;
                //graphics.drawString(font, text, cx - (font.width(text) / 2), cy + 10, 0xFFFFFF, true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void releaseAll() {
        if (heldPackets.isEmpty()) return;
        if (mc.player == null || mc.player.connection == null) {
            heldPackets.clear();
            return;
        }
        releasing = true;
        ClientGamePacketListener listener = mc.player.connection;
        for (Packet<?> packet : heldPackets) {
            try {
                ((Packet<ClientGamePacketListener>) packet).handle(listener);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        heldPackets.clear();
        releasing = false;
    }
    // ========================
    //   DISTANCE HELPERS
    // ========================
    private double boxDist(Vec3 point, AABB box) {
        double cx = Math.max(box.minX, Math.min(point.x, box.maxX));
        double cy = Math.max(box.minY, Math.min(point.y, box.maxY));
        double cz = Math.max(box.minZ, Math.min(point.z, box.maxZ));
        return point.distanceTo(new Vec3(cx, cy, cz));
    }
    private double boxDistAtPos(Vec3 point, Entity entity, Vec3 pos) {
        float hw = entity.getBbWidth() / 2.0f;
        float h = entity.getBbHeight();
        AABB box = new AABB(pos.x - hw, pos.y, pos.z - hw, pos.x + hw, pos.y + h, pos.z + hw);
        return boxDist(point, box);
    }
    // ========================
    //   TARGET FINDING
    // ========================
    private Entity findTarget() {
        if (mc.player == null || mc.level == null) return null;
        HitResult hit = mc.hitResult;
        if (hit instanceof EntityHitResult eResult) {
            if (eResult.getEntity() instanceof LivingEntity target && target != mc.player) {
                if (!target.isDeadOrDying() && mc.player.distanceTo(target) <= range.getNum()) return target;
            }
        }
        Entity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            if (player.isDeadOrDying()) continue;
            double dist = mc.player.distanceTo(player);
            if (dist < closestDist && dist <= range.getNum()) {
                closestDist = dist;
                closest = player;
            }
        }
        return closest;
    }
    // ========================
    //   RESET
    // ========================
    private void hardReset() {
        releaseAll();
        currentTarget = null;
        realPosition = Vec3.ZERO;
        active = false;
        releasing = false;
        dis = 0;
        holdTimer.reset();
    }
    @Override
    public void OnDisable() {
        hardReset();
    }
    // Keep these for FakeLag compatibility (other modules may reference them)
    public static final List<Packet<?>> sharedQueue = new ArrayList<>();
    public static boolean sending = false;
    public static void flushQueue() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) {
            sharedQueue.clear();
            sending = false;
            return;
        }
        if (sharedQueue.isEmpty()) return;
        sending = true;
        List<Packet<?>> toSend = new ArrayList<>(sharedQueue);
        sharedQueue.clear();
        for (Packet<?> packet : toSend) {
            mc.player.connection.getConnection().send(packet);
        }
        sending = false;
    }
}
