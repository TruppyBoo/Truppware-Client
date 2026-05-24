package trupp.ware.truppware.module.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.TimerUtil;

public class AutoArmour extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final TimerUtil timer = new TimerUtil();

    public NumberSetting delay = new NumberSetting("Delay", 50, 500, 150, 10);

    public AutoArmour() {
        super("AutoArmour", Category.PLAYER, "Automatically equips best armor", -1);
        addSettings(delay);
    }

    private boolean fitsSlot(ItemStack stack, int containerArmorSlot) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return switch (containerArmorSlot) {
            case 5 -> item == Items.NETHERITE_HELMET    || item == Items.DIAMOND_HELMET     ||
                    item == Items.IRON_HELMET          || item == Items.GOLDEN_HELMET      ||
                    item == Items.CHAINMAIL_HELMET     || item == Items.LEATHER_HELMET;
            case 6 -> item == Items.NETHERITE_CHESTPLATE || item == Items.DIAMOND_CHESTPLATE ||
                    item == Items.IRON_CHESTPLATE       || item == Items.GOLDEN_CHESTPLATE  ||
                    item == Items.CHAINMAIL_CHESTPLATE  || item == Items.LEATHER_CHESTPLATE;
            case 7 -> item == Items.NETHERITE_LEGGINGS  || item == Items.DIAMOND_LEGGINGS   ||
                    item == Items.IRON_LEGGINGS         || item == Items.GOLDEN_LEGGINGS    ||
                    item == Items.CHAINMAIL_LEGGINGS    || item == Items.LEATHER_LEGGINGS;
            case 8 -> item == Items.NETHERITE_BOOTS     || item == Items.DIAMOND_BOOTS      ||
                    item == Items.IRON_BOOTS            || item == Items.GOLDEN_BOOTS       ||
                    item == Items.CHAINMAIL_BOOTS       || item == Items.LEATHER_BOOTS;
            default -> false;
        };
    }

    private int getDefense(ItemStack stack, int containerArmorSlot) {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        return switch (containerArmorSlot) {
            case 5 ->
                    item == Items.NETHERITE_HELMET    ? 3 :
                            item == Items.DIAMOND_HELMET      ? 3 :
                                    item == Items.IRON_HELMET         ? 2 :
                                            item == Items.GOLDEN_HELMET       ? 2 :
                                                    item == Items.CHAINMAIL_HELMET    ? 2 :
                                                            item == Items.LEATHER_HELMET      ? 1 : 0;
            case 6 ->
                    item == Items.NETHERITE_CHESTPLATE ? 8 :
                            item == Items.DIAMOND_CHESTPLATE   ? 8 :
                                    item == Items.IRON_CHESTPLATE      ? 6 :
                                            item == Items.GOLDEN_CHESTPLATE    ? 5 :
                                                    item == Items.CHAINMAIL_CHESTPLATE ? 5 :
                                                            item == Items.LEATHER_CHESTPLATE   ? 3 : 0;
            case 7 ->
                    item == Items.NETHERITE_LEGGINGS  ? 6 :
                            item == Items.DIAMOND_LEGGINGS    ? 6 :
                                    item == Items.IRON_LEGGINGS       ? 5 :
                                            item == Items.GOLDEN_LEGGINGS     ? 3 :
                                                    item == Items.CHAINMAIL_LEGGINGS  ? 4 :
                                                            item == Items.LEATHER_LEGGINGS    ? 2 : 0;
            case 8 ->
                    item == Items.NETHERITE_BOOTS     ? 3 :
                            item == Items.DIAMOND_BOOTS       ? 3 :
                                    item == Items.IRON_BOOTS          ? 2 :
                                            item == Items.GOLDEN_BOOTS        ? 1 :
                                                    item == Items.CHAINMAIL_BOOTS     ? 1 :
                                                            item == Items.LEATHER_BOOTS       ? 1 : 0;
            default -> 0;
        };
    }

    private int findEmptySlot(LocalPlayer player) {
        // Look for empty slot in main inventory (9-35) first, then hotbar (36-44)
        for (int i = 9; i <= 44; i++) {
            if (player.inventoryMenu.getSlot(i).getItem().isEmpty()) return i;
        }
        return -1;
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventTick)) return;
        if (mc.player == null || mc.level == null) return;
        if (mc.screen == null) return;
        if (!timer.hasElapsed((long) delay.getNum())) return;

        LocalPlayer player = mc.player;
        int[] armorSlots = {5, 6, 7, 8};

        for (int armorSlot : armorSlots) {
            ItemStack current = player.inventoryMenu.getSlot(armorSlot).getItem();
            int currentDefense = getDefense(current, armorSlot);
            // Find best armor in inventory
            int bestSlot = -1;
            int bestDefense = currentDefense;

            for (int i = 9; i <= 44; i++) {
                ItemStack stack = player.inventoryMenu.getSlot(i).getItem();
                if (!fitsSlot(stack, armorSlot)) continue;

                int defense = getDefense(stack, armorSlot);
                if (defense > bestDefense) {
                    bestDefense = defense;
                    bestSlot = i;
                }
            }

            if (bestSlot == -1) continue;

            // If current armor exists and is worse — unequip it first
            if (!current.isEmpty()) {
                int emptySlot = findEmptySlot(player);
                if (emptySlot == -1) {
                    // No room — skip this slot
                    continue;
                }

                // Shift-click current armor off into inventory
                mc.gameMode.handleInventoryMouseClick(
                        player.inventoryMenu.containerId,
                        armorSlot,
                        0,
                        ClickType.QUICK_MOVE,
                        player
                );
                timer.reset();
                return; // Wait next tick to equip the new piece
            }

            // Slot is empty — equip best armor
            mc.gameMode.handleInventoryMouseClick(
                    player.inventoryMenu.containerId,
                    bestSlot,
                    0,
                    ClickType.QUICK_MOVE,
                    player
            );

            timer.reset();
            return;
        }
    }
}