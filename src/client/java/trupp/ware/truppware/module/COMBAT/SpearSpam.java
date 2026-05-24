package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventPacket;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;

public class SpearSpam extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    public SpearSpam() {
        super("SpearSpam", Category.COMBAT, "Auto swaps to spear and attacks when cooldown is ready", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEvent(Event event, Timing time) {
        if (!(event instanceof EventTick)) return;
        if (mc.player == null || mc.level == null) return;

        if (!mc.mouseHandler.isLeftPressed()) return;

        // Wait for full attack cooldown
        if (mc.player.getAttackStrengthScale(0f) < 1.0f) return;

        int spearSlot = findSpearInHotbar();
        if (spearSlot == -1) return;

        int previousSlot = mc.player.getInventory().getSelectedSlot();
        if (previousSlot == spearSlot) return;

        // Switch to spear, click, switch back
        switchSlot(spearSlot);

        IKeyMappingExt attack = (IKeyMappingExt) mc.options.keyAttack;
        attack.truppware$setPressed(true);
        attack.truppware$click();
        attack.truppware$setPressed(false);


       // switchSlot(previousSlot);
    }

    private int findSpearInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            // Check all spear tiers
            if (stack.getItem() == Items.WOODEN_SPEAR
                    || stack.getItem() == Items.STONE_SPEAR
                    || stack.getItem() == Items.COPPER_SPEAR
                    || stack.getItem() == Items.IRON_SPEAR
                    || stack.getItem() == Items.GOLDEN_SPEAR
                    || stack.getItem() == Items.DIAMOND_SPEAR
                    || stack.getItem() == Items.NETHERITE_SPEAR) {
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
    }
}