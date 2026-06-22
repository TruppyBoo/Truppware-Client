package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.EntityHitResult;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.interfaces.IMultiPlayerGameModeExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;

public class AutoMace extends Module {
    public AutoMace() {
        super("AutoMace", Category.COMBAT, "Auto attacks with mace when falling", -1);
    }

    private final Minecraft mc = Minecraft.getInstance();
    private int previousSlot = -1;
    private boolean switched = false;

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventTick)) return;
        if (mc.player == null) return;
        if (time == Timing.POST) return;

        LivingEntity target = getCombatTarget();

        if (!(mc.player.fallDistance > 0.1f) || target == null) {
            if (switched) {
                if (conflictingAction()) return;   // wait for a quiet tick to swap back (PacketOrderE)
                switchSlot(previousSlot);
                flushCarriedItem();
                previousSlot = -1;
                switched = false;
            }
            return;
        }

        int maceSlot = findMaceInHotbar();
        if (maceSlot == -1) return;

        if (!switched && !ShieldBreaker.shield) {
            // Grim PacketOrderE flags a held-slot change that shares a tick with an attack/use. Our
            // switch's packet is normally flushed by the next attack (bundling it with the attack), so
            // only switch on a QUIET tick — Aura runs before us, so Aura.attacking tells us it hit this
            // tick — then flush the slot packet ourselves right now so no later attack carries it.
            if (conflictingAction()) return;
            previousSlot = mc.player.getInventory().getSelectedSlot();
            switchSlot(maceSlot);
            flushCarriedItem();
            // One swing by default; ShieldDestroyer makes it a burst (its configured click count) to pop a shield.
            int clickCount = ShieldDestroyer.clickCount();
            if (Aura.enabled && Aura.currentTarget == target && mc.gameMode != null) {
                for (int i = 0; i < clickCount; i++) {
                    mc.gameMode.attack(mc.player, target);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
            } else {
                for (int i = 0; i < clickCount; i++) {
                    ((IKeyMappingExt) mc.options.keyAttack).truppware$click();
                }
            }
            switched = true;
        }
    }

    /**
     * Resolves the entity we're aiming at. When Aura is on it uses Aura's silent-aim
     * rotation (since the real crosshair never turns to the target); otherwise it
     * falls back to the normal dcrosshair hit result for manual aiming.
     */
    private LivingEntity getCombatTarget() {
        if (Aura.enabled && Aura.currentTarget != null
                && Aura.currentTargetInRange && !Aura.currentTarget.isDeadOrDying()) {
            return Aura.currentTarget;
        }
        if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le && !le.isDeadOrDying()) {
            return le;
        }
        return null;
    }

    /** True on a tick that already carries a conflicting action, so changing the held slot now would
     *  trip Grim PacketOrderE. Aura runs before us each tick, so Aura.attacking means it hit this tick. */
    private boolean conflictingAction() {
        return mc.player.isUsingItem() || (Aura.enabled && Aura.attacking);
    }

    /** Send the held-item-change packet now (its own packet on this quiet tick) so a later attack
     *  doesn't flush it bundled with the attack -> avoids PacketOrderE. */
    private void flushCarriedItem() {
        if (mc.gameMode instanceof IMultiPlayerGameModeExt ext) ext.truppware$ensureCarriedItem();
    }

    private void switchSlot(int slot) {
        // (Original used reflection on a "selected" field — that's "selectedSlot" in 1.21 and threw
        // every call, so it never actually swapped. setSelectedSlot is the working equivalent.)
        if (mc.player == null || slot < 0 || slot > 8) return;
        mc.player.getInventory().setSelectedSlot(slot);
    }

    private int findMaceInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof MaceItem) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void OnDisable() {
        if (switched && previousSlot != -1) {
            switchSlot(previousSlot);
        }
        previousSlot = -1;
        switched = false;
    }
}
