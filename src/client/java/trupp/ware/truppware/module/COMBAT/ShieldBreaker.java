package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventRender;
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
        if (!(e instanceof EventRender)) return;
        if (mc.player == null) return;

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

        HitResult hit = mc.hitResult;

        if (!(hit instanceof EntityHitResult eResult) || !(eResult.getEntity() instanceof LivingEntity target)) {
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

            if (!switched) {
                previousSlot = mc.player.getInventory().getSelectedSlot();
                switchSlot(axeSlot);
                ((IKeyMappingExt) mc.options.keyAttack).truppware$click();
                ((IKeyMappingExt) mc.options.keyAttack).truppware$click();
                ((IKeyMappingExt) mc.options.keyAttack).truppware$click();
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

    private void switchSlot(int slot) {
        try {
            java.lang.reflect.Field f = mc.player.getInventory().getClass().getDeclaredField("selected");
            f.setAccessible(true);
            f.set(mc.player.getInventory(), slot);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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