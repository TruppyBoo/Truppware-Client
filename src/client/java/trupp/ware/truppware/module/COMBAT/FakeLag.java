package trupp.ware.truppware.module.COMBAT;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import trupp.ware.event.Event;
import trupp.ware.event.events.*;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.TimerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FakeLag extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final TimerUtil burstTimer = new TimerUtil();
    private final TimerUtil recoilTimer = new TimerUtil();
    private final Random random = new Random();
    private boolean wasNearPlayer = false;
    private long currentDelay = 400;

    public static final List<Packet<?>> sharedQueue = new ArrayList<>();
    public static boolean sending = false;

    public NumberSetting range       = new NumberSetting("Range",        1,   10,   5,    0.5);
    public NumberSetting minDelay    = new NumberSetting("MinDelay",     50,  2000, 200,  10);
    public NumberSetting maxDelay    = new NumberSetting("MaxDelay",     50,  2000, 600,  10);
    public BooleanSetting flushOnHit = new BooleanSetting("FlushOnHit", true);

    public FakeLag() {
        super("FakeLag", Category.COMBAT, "Buffers outgoing packets to simulate lag", GLFW.GLFW_KEY_UNKNOWN);
        addSettings(range, minDelay, maxDelay, flushOnHit);
        currentDelay = pickDelay();
    }

    private long pickDelay() {
        long min = (long) minDelay.getNum();
        long max = (long) maxDelay.getNum();
        if (min >= max) return min;
        return min + (long) (random.nextDouble() * (max - min));
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (e instanceof EventTick) {
            if(time == Timing.POST) return;
            if (mc.player == null || mc.level == null) {
                hardReset();
                return;
            }

            boolean nearPlayer = isNearPlayer();
            if (nearPlayer && !wasNearPlayer) {
                burstTimer.reset();
                currentDelay = pickDelay(); // pick fresh delay when we enter range
            }
            if (!nearPlayer && wasNearPlayer) {
                flushQueue();
            }
            wasNearPlayer = nearPlayer;
            if (!nearPlayer) return;

            if (burstTimer.hasElapsed(currentDelay)) {
                flushQueue();
                currentDelay = pickDelay(); // pick new random delay after each flush
                burstTimer.reset();
            }
        }

        if (e instanceof EventAttack) {
            if (flushOnHit.isEnabled()) {
                flushQueue();
                currentDelay = pickDelay();
                recoilTimer.reset();
            }
        }

        if (e instanceof EventPacket eventPacket) {
            Packet<?> packet = eventPacket.getPacket();

            if (eventPacket.getPacketDih() == PacketDih.INCOMING) {
                // Server is switching us to the CONFIGURATION phase (e.g. swapping sub-servers
                // inside a network). The netty pipeline gets torn down and rebuilt; flushing our
                // held PLAY packets across that transition races the reconfiguration task and
                // corrupts the channel (UnsupportedOperationException: unsupported message type ...
                // -> disconnect). Drop the queue WITHOUT sending and stop buffering.
                if (packet instanceof ClientboundStartConfigurationPacket) {
                    sharedQueue.clear();
                    sending = false;
                    wasNearPlayer = false;
                    currentDelay = pickDelay();
                    burstTimer.reset();
                    recoilTimer.reset();
                    return;
                }
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
                if (packet instanceof ClientboundSetEntityMotionPacket velPacket) {
                    if (mc.player != null && velPacket.getId() == mc.player.getId()) {
                        Vec3 motion = velPacket.getMovement();
                        if (motion.lengthSqr() > 0) {
                            flushQueue();
                            currentDelay = pickDelay();
                            recoilTimer.reset();
                            return;
                        }
                    }
                }
                if (packet instanceof ClientboundExplodePacket) {
                    flushQueue();
                    currentDelay = pickDelay();
                    recoilTimer.reset();
                    return;
                }
            }

            if (eventPacket.getPacketDih() == PacketDih.OUTGOING) {
                if (sending) return;
                if (e.canceled) return;
                if (!wasNearPlayer) return;
                if (!recoilTimer.hasElapsed(150L)) return;
                sharedQueue.add(packet);
                e.canceled = true;
            }
        }
    }

    private void hardReset() {
        flushQueue();
        wasNearPlayer = false;
        currentDelay = pickDelay();
        burstTimer.reset();
        recoilTimer.reset();
    }

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

    private boolean isNearPlayer() {
        if (mc.player == null || mc.level == null) return false;
        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            if (player instanceof LivingEntity living && living.isDeadOrDying()) continue;
            if (mc.player.distanceTo(player) <= range.getNum()) return true;
        }
        return false;
    }

    @Override
    public void OnDisable() {
        hardReset();
    }
}