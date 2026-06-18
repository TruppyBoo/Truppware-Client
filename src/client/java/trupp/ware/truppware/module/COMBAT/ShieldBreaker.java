package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;

public class ShieldBreaker extends Module {

    public ShieldBreaker() {
        super("ShieldBreaker", Category.COMBAT, "Breaks shields", -1);
    }

    public static boolean shield = false;
    private final Minecraft mc = Minecraft.getInstance();
    private int previousSlot = -1;
    private boolean switched = false;

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventTick)) return;
        if (mc.player == null) return;
        if (time == Timing.POST) return;

        // If player starts using item, reset immediately so we don't get stuck
        if (mc.player.isUsingItem()) {
            if (switched) {
                switchSlot(previousSlot);
                previousSlot = -1;
                switched = false;
                shield = false;
            }
            return;
        }

        LivingEntity target = getCombatTarget();

        if (target == null) {
            if (switched) {
                switchSlot(previousSlot);
                previousSlot = -1;
                switched = false;
                shield = false;
            }
            return;
        }

        if (target.isBlocking()) {
            int axeSlot = findAxeInHotbar();
            if (axeSlot == -1) {
                // No axe — make sure we reset so automace can work
                if (switched) {
                    switchSlot(previousSlot);
                    previousSlot = -1;
                    switched = false;
                    shield = false;
                }
                return;
            }

            if (!switched && target.isBlocking()) {
                previousSlot = mc.player.getInventory().getSelectedSlot();
                switchSlot(axeSlot);
                if (Aura.enabled && Aura.currentTarget == target && mc.gameMode != null) {
                    // Aura aims silently, so attack the locked target directly instead of crosshair clicks.
                    for (int i = 0; i < 3; i++) {
                        mc.gameMode.attack(mc.player, target);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                } else {
                    ((IKeyMappingExt) mc.options.keyAttack).truppware$click();
                    ((IKeyMappingExt) mc.options.keyAttack).truppware$click();
                    ((IKeyMappingExt) mc.options.keyAttack).truppware$click();
                }
                shield = true;
                switched = true;
            }
        } else {
            if (switched) {
                switchSlot(previousSlot);
                previousSlot = -1;
                switched = false;
                shield = false;
            }
        }
    }

    /**
     * Resolves the entity we're aiming at. When Aura is on it uses Aura's silent-aim
     * rotation (since the real crosshair never turns to the target); otherwise it
     * falls back to the normal crosshair hit result for manual aiming.
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

    private void switchSlot(int slot) {
        // (Original used reflection on a "selected" field — that's "selectedSlot" in 1.21 and threw
        // every call, so it never actually swapped. setSelectedSlot is the working equivalent;
        // vanilla sends the slot packet itself.)
        if (mc.player == null || slot < 0 || slot > 8) return;
        mc.player.getInventory().setSelectedSlot(slot);
    }

    private int findAxeInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof AxeItem) {
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
        shield = false;
    }
}
