package trupp.ware.truppware.module.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.TimerUtil;

public class ChestStealer extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final TimerUtil timer = new TimerUtil();

    public NumberSetting delay = new NumberSetting("Delay", 50, 300, 80, 10);

    public ChestStealer() {
        super("ChestStealer", Category.PLAYER, "Steals items from containers", -1);
        addSettings(delay);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventTick)) return;
        if (mc.player == null || mc.level == null) return;

        // Only works when a container is open
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) return;
        if (!timer.hasElapsed((long) delay.getNum())) return;

        LocalPlayer player = mc.player;
        AbstractContainerMenu menu = screen.getMenu();

        // Find first non-empty slot that belongs to the container not the player
        for (Slot slot : menu.slots) {
            // Skip player inventory
            if (slot.container instanceof Inventory) continue;

            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            // Check player has space
            if (player.getInventory().getFreeSlot() == -1 &&
                    !player.getInventory().canPlaceItem(0, stack)) continue;

            // Shift click to move to inventory
            mc.gameMode.handleInventoryMouseClick(
                    menu.containerId,
                    slot.index,
                    0,
                    ClickType.QUICK_MOVE,
                    player
            );

            timer.reset();
            return;
        }
    }
}