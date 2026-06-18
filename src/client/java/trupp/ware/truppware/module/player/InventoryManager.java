package trupp.ware.truppware.module.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.TimerUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tidies your inventory: keeps the single best tool of each kind and the best armour piece per
 * slot, throwing out the worse duplicates. Optionally also tosses low-tier (wood/stone/gold/
 * leather/chainmail) junk. Throws one item per delay so it looks natural and avoids lag.
 */
public class InventoryManager extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final TimerUtil timer = new TimerUtil();

    public BooleanSetting tools = new BooleanSetting("Tools", true);
    public BooleanSetting armor = new BooleanSetting("Armor", true);
    public BooleanSetting junk  = new BooleanSetting("Drop Low-Tier", false);
    public NumberSetting delay  = new NumberSetting("Delay", 50, 1000, 250, 25);

    private static final Set<String> TOOL_TYPES  = Set.of("sword", "pickaxe", "axe", "shovel", "hoe");
    private static final Set<String> ARMOR_TYPES = Set.of("helmet", "chestplate", "leggings", "boots");

    // Material quality (higher = better). Iron and above are "good".
    private static final Map<String, Integer> RANK = new HashMap<>();
    private static final int IRON_RANK = 4;
    static {
        RANK.put("netherite", 6);
        RANK.put("diamond", 5);
        RANK.put("turtle", 4);     // turtle helmet
        RANK.put("iron", 4);
        RANK.put("chainmail", 3);
        RANK.put("golden", 2);
        RANK.put("gold", 2);
        RANK.put("stone", 2);
        RANK.put("wooden", 1);
        RANK.put("wood", 1);
        RANK.put("leather", 1);
    }

    public InventoryManager() {
        super("InventoryManager", Category.PLAYER, "Throws out duplicate / junk tools & armour", -1);
        addSettings(tools, armor, junk, delay);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventTick) || time == Timing.POST) return;
        if (mc.player == null || mc.gameMode == null) return;
        // ONLY run while your real survival inventory screen is open.
        if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;
        if (!timer.hasElapsed((long) delay.getNum())) return;

        Inventory inv = mc.player.getInventory();
        int selected = inv.getSelectedSlot();

        // Pass 1: find the keeper slot (best material) for every tool/armour type we hold.
        Map<String, Integer> keeperSlot = new HashMap<>();
        Map<String, Integer> keeperRank = new HashMap<>();
        for (int i = 0; i < 36; i++) {
            Info info = classify(inv.getItem(i));
            if (info == null) continue;
            if (!keeperRank.containsKey(info.type) || info.rank > keeperRank.get(info.type)) {
                keeperRank.put(info.type, info.rank);
                keeperSlot.put(info.type, i);
            }
        }

        // Pass 2: throw the first unnecessary item.
        for (int i = 0; i < 36; i++) {
            if (i == selected) continue;
            Info info = classify(inv.getItem(i));
            if (info == null) continue;
            if (info.armor && !armor.isEnabled()) continue;
            if (!info.armor && !tools.isEnabled()) continue;

            boolean isKeeper = keeperSlot.getOrDefault(info.type, -1) == i;
            boolean lowTier  = junk.isEnabled() && info.rank < IRON_RANK;

            if (!isKeeper || lowTier) {
                throwSlot(i);
                timer.reset();
                return;
            }
        }
    }

    private void throwSlot(int invSlot) {
        // Player inventory menu slot layout: main inventory 9-35 == inv 9-35; hotbar 36-44 == inv 0-8.
        int containerSlot = invSlot < 9 ? invSlot + 36 : invSlot;
        mc.gameMode.handleInventoryMouseClick(
                mc.player.inventoryMenu.containerId, containerSlot, 1, ClickType.THROW, mc.player);
    }

    /** @return classification of a tool/armour stack, or null if it isn't one we manage. */
    private Info classify(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        String id = stack.getItem().getDescriptionId();   // e.g. "item.minecraft.diamond_sword"
        int dot = id.lastIndexOf('.');
        String name = dot >= 0 ? id.substring(dot + 1) : id;

        int us = name.indexOf('_');
        if (us <= 0) return null;
        String material = name.substring(0, us);
        String type     = name.substring(us + 1);

        Integer rank = RANK.get(material);
        if (rank == null) return null;

        if (TOOL_TYPES.contains(type))  return new Info(type, rank, false);
        if (ARMOR_TYPES.contains(type)) return new Info(type, rank, true);
        return null;
    }

    private static final class Info {
        final String type;
        final int rank;
        final boolean armor;
        Info(String type, int rank, boolean armor) { this.type = type; this.rank = rank; this.armor = armor; }
    }
}
