package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.TimerUtil;

import java.lang.reflect.Field;

public class AutoAnchor extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final TimerUtil timer = new TimerUtil();

    public NumberSetting delay = new NumberSetting("Delay", 15, 0, 200, 5);

    private enum State { IDLE, CHARGE, EXPLODE, RESTORE }
    private State state = State.IDLE;
    private boolean wasRightPressed = false;
    private int previousSlot = -1;
    private int explodeSlot = -1;
    private int anchorSlot = -1;
    private int glowstoneSlot = -1;

    public AutoAnchor() {
        super("AutoAnchor", Category.COMBAT, "Place charge and explode anchor sequence", GLFW.GLFW_KEY_UNKNOWN);
        addSettings(delay);
    }

    @Override
    public void onEvent(Event event, Timing time) {
        if (!(event instanceof EventTick)) return;
        if (mc.player == null || mc.level == null) return;

        IKeyMappingExt use = (IKeyMappingExt) mc.options.keyUse;
        boolean rightPressed = mc.mouseHandler.isRightPressed();
        boolean risingEdge   = rightPressed && !wasRightPressed;
        wasRightPressed = rightPressed;

        // Trigger on right click while holding anchor
        if (risingEdge && state == State.IDLE) {
            ItemStack mainHand = mc.player.getMainHandItem();
            if (!mainHand.isEmpty() && mainHand.getItem() == Items.RESPAWN_ANCHOR) {
                previousSlot  = mc.player.getInventory().getSelectedSlot();
                anchorSlot    = previousSlot;
                glowstoneSlot = findInHotbar(Items.GLOWSTONE);
                explodeSlot   = findExplodeItem();
                state = State.CHARGE;
                timer.reset();
                return;
            }
        }

        if (state == State.IDLE) return;
        if (!timer.hasElapsed((long) delay.getNum())) return;

        switch (state) {
            case CHARGE -> {
                if (glowstoneSlot != -1) {
                    // Switch + use on same tick
                    switchSlot(glowstoneSlot);
                    simulateUse(use);
                }
                timer.reset();
                state = State.EXPLODE;
            }
            case EXPLODE -> {
                // Switch to explode item + use on same tick
                if (explodeSlot != -1) {
                    switchSlot(explodeSlot);
                } else {
                    // No safe item — use anchor (might place but at least detonates)
                    switchSlot(anchorSlot);
                }
                simulateUse(use);
                timer.reset();
                state = State.RESTORE;
            }
            case RESTORE -> {
                restoreSlot();
                state = State.IDLE;
            }
        }
    }

    private int findExplodeItem() {
        // First pass — sword/pickaxe/totem (instant detonate, no risk of placing)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            net.minecraft.world.item.Item it = stack.getItem();
            if (it == Items.NETHERITE_SWORD || it == Items.DIAMOND_SWORD ||
                    it == Items.IRON_SWORD || it == Items.GOLDEN_SWORD ||
                    it == Items.STONE_SWORD || it == Items.WOODEN_SWORD ||
                    it == Items.NETHERITE_PICKAXE || it == Items.DIAMOND_PICKAXE ||
                    it == Items.NETHERITE_AXE || it == Items.DIAMOND_AXE ||
                    it == Items.TOTEM_OF_UNDYING) {
                return i;
            }
        }
        // Second pass — anything non-block that isn't anchor/glowstone
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            net.minecraft.world.item.Item it = stack.getItem();
            if (it == Items.RESPAWN_ANCHOR || it == Items.GLOWSTONE) continue;
            if (!(it instanceof net.minecraft.world.item.BlockItem)) {
                return i;
            }
        }
        return -1;
    }

    private void simulateUse(IKeyMappingExt use) {
        use.truppware$setPressed(true);
        use.truppware$click();
        use.truppware$setPressed(false);
    }

    private void switchSlot(int slot) {
        try {
            Field f = mc.player.getInventory().getClass().getDeclaredField("selected");
            f.setAccessible(true);
            f.set(mc.player.getInventory(), slot);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // Hotbar key click sends ServerboundSetCarriedItemPacket so server knows
        ((IKeyMappingExt) mc.options.keyHotbarSlots[slot]).truppware$click();
    }

    private void restoreSlot() {
        if (previousSlot != -1) {
            switchSlot(previousSlot);
            previousSlot = -1;
        }
    }

    private int findInHotbar(net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) return i;
        }
        return -1;
    }

    @Override
    public void OnDisable() {
        restoreSlot();
        state = State.IDLE;
        wasRightPressed = false;
        explodeSlot = -1;
        anchorSlot = -1;
        glowstoneSlot = -1;
    }
}