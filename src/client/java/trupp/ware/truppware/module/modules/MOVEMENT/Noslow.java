package trupp.ware.truppware.module.modules.MOVEMENT;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventNoSlow;
import trupp.ware.event.events.EventUpdate;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.COMBAT.Aura;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.ModeSetting;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.RotationUtil;

import java.util.List;
import java.util.Random;

/**
 * Sword/item noslow.
 *   Client     – removes the using-item slowdown LOCALLY only. Sends NO packets, never trips a
 *                packet-order check (use on Grim).
 *   Prediction – Unfair's "Prediction" sword noslow, ported: while blocking, swap the held item away
 *                and back BEFORE the movement packet so the server doesn't apply the block slowdown
 *                to that move, then re-raise the block AFTER the movement packet so you stay
 *                defended. Sends slot packets -> flags Grim, so this is Hypixel-only.
 *   Off        – nothing.
 */
public class Noslow extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final Random random = new Random();

    public final ModeSetting mode = new ModeSetting("Mode", List.of("Client", "Prediction", "Off"));
    public final NumberSetting swapDelay = new NumberSetting("Swap Delay", 0, 5, 0, 1);
    public final BooleanSetting c17 = new BooleanSetting("C17 Packet", false); // anti-detect filler

    private int delay = 0;
    private boolean post = false;

    public Noslow() {
        super("Noslow", Category.MOVEMENT, "Removes the using-item movement slowdown", 0);
        addSettings(mode, swapDelay, c17);
    }

    @Override
    public void onEnable() {}

    @Override
    public void OnDisable() {
        post = false;
        delay = 0;
    }

    @Override
    public void onEvent(Event e, Timing t) {
        String m = mode.getCurrentMode();
        if (m.equals("Off")) return;

        // Client-side input noslow — packet-free (applies in Client AND Prediction).
        if (e instanceof EventNoSlow event) {
            event.setX(1);
            event.setY(1);
            return;
        }

        if (!m.equals("Prediction") || !(e instanceof EventUpdate)) return;
        if (mc.player == null || mc.player.connection == null) return;

        // Active while the (watchdog) sword block is actually held server-side.
        boolean active = Aura.watchdogBlocking && mc.player.getMainHandItem().is(ItemTags.SWORDS);

        if (t == Timing.PRE) {
            // PRE = sendPosition HEAD (pre-motion). Swap the held item away and back: the server
            // cancels the item-use (block), so the movement packet that follows this tick is NOT
            // slowed. The block is re-raised in POST (after that packet).
            if (!active) { post = false; return; }
            if (delay > 0) { delay--; return; }
            delay = (int) swapDelay.getNum();

            int slot  = mc.player.getInventory().getSelectedSlot();
            int other = slot;
            while (other == slot) other = random.nextInt(9);     // random slot != current (like Unfair)
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(other));
            // C17 anti-detect filler between the two slot swaps (the 1.8 C17PacketCustomPayload
            // equivalent — an empty custom payload on its own channel).
            if (c17.isEnabled()) {
                mc.player.connection.send(new ServerboundCustomPayloadPacket(
                        new DiscardedPayload(Identifier.fromNamespaceAndPath("truppware", "woshijiejue"))));
            }
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            post = true;
        } else if (t == Timing.POST) {
            // POST = sendPosition RETURN (after the movement packet). Re-raise the block so you stay
            // defended — the move already went out at full speed because the server saw "not using".
            if (post) {
                post = false;
                mc.player.connection.send(new ServerboundUseItemPacket(
                        InteractionHand.MAIN_HAND, 0, RotationUtil.serverYaw, RotationUtil.serverPitch));
            }
        }
    }
}
