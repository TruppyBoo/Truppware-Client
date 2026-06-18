package trupp.ware.truppware.module.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.NumberSetting;

import java.util.Random;

public class AutoTotem extends Module {

    public NumberSetting minDelay;
    public NumberSetting maxDelay;
    public BooleanSetting openInventory;

    private final Minecraft mc = Minecraft.getInstance();
    private final Random random = new Random();

    private enum State { IDLE, WAIT_OPEN, SUPPRESS_TICK, SWAP, RESTORE_TICK, WAIT_CLOSE }
    private State state = State.IDLE;
    private long stateDeadline = 0;
    private int suppressTicks = 0;

    private boolean inputsSuppressed = false;
    private boolean savedSprint  = false;
    private boolean savedForward = false;
    private boolean savedBack    = false;
    private boolean savedLeft    = false;
    private boolean savedRight   = false;

    private static final int OFFHAND_SLOT = 40;

    public AutoTotem() {
        super("AutoTotem", Category.COMBAT, "Keeps a totem in your offhand", -1);
        addSettings(
                minDelay      = new NumberSetting("MinDelay", 0, 2000, 0, 50),
                maxDelay      = new NumberSetting("MaxDelay", 0, 3000, 0, 50),
                openInventory = new BooleanSetting("OpenInventory", false)
        );
    }

    @Override
    public void OnDisable() {
        if (mc.player != null && state != State.IDLE) {
            if (mc.screen instanceof InventoryScreen) {
                mc.player.closeContainer();
                mc.setScreen(null);
            }
            restoreInputs();
        }
        state = State.IDLE;
        suppressTicks = 0;
        inputsSuppressed = false;
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventTick)) return;
        if(time == Timing.POST) return;
        if (mc.player == null) return;
        if (mc.player.isDeadOrDying()) return;

        ItemStack offhand = mc.player.getOffhandItem();
        boolean hasOffhandTotem = !offhand.isEmpty() && offhand.getItem() == Items.TOTEM_OF_UNDYING;

        if (hasOffhandTotem && state == State.IDLE) {
            if (inputsSuppressed) restoreInputs();
            return;
        }

        if (findTotem() == -1 && state == State.IDLE) {
            if (inputsSuppressed) restoreInputs();
            return;
        }

        long now = System.currentTimeMillis();

        switch (state) {
            case IDLE -> {
                if (openInventory.isEnabled()) {
                    // Inventory mode — open screen, wait one tick, then swap
                    mc.setScreen(new InventoryScreen(mc.player));
                    suppressTicks = 1;
                    state = State.WAIT_OPEN;
                } else {
                    // Silent mode — suppress inputs, wait one tick, then swap
                    suppressInputs();
                    suppressTicks = 1;
                    stateDeadline = now + (long) minDelay.getNum();
                    if (maxDelay.getNum() > minDelay.getNum()) {
                        stateDeadline += random.nextInt((int)(maxDelay.getNum() - minDelay.getNum()));
                    }
                    state = State.SUPPRESS_TICK;
                }
            }

            case WAIT_OPEN -> {
                // Inventory open packet has reached server — now swap
                suppressTicks--;
                if (suppressTicks <= 0) {
                    swapTotem();
                    suppressTicks = 1;
                    state = State.WAIT_CLOSE;
                }
            }

            case WAIT_CLOSE -> {
                // One tick after swap — close the inventory
                suppressTicks--;
                if (suppressTicks <= 0) {
                    if (mc.screen instanceof InventoryScreen) {
                        mc.player.closeContainer();
                        mc.setScreen(null);
                    }
                    state = State.IDLE;
                }
            }

            case SUPPRESS_TICK -> {
                suppressInputs();
                suppressTicks--;
                if (suppressTicks <= 0 && now >= stateDeadline) {
                    state = State.SWAP;
                }
            }

            case SWAP -> {
                suppressInputs();
                swapTotem();
                state = State.RESTORE_TICK;
                suppressTicks = 1;
            }

            case RESTORE_TICK -> {
                suppressInputs();
                suppressTicks--;
                if (suppressTicks <= 0) {
                    restoreInputs();
                    state = State.IDLE;
                }
            }
        }
    }

    private void suppressInputs() {
        if (mc.player == null) return;
        if (!inputsSuppressed) {
            savedSprint  = mc.options.keySprint.isDown();
            savedForward = mc.options.keyUp.isDown();
            savedBack    = mc.options.keyDown.isDown();
            savedLeft    = mc.options.keyLeft.isDown();
            savedRight   = mc.options.keyRight.isDown();
            inputsSuppressed = true;
        }

        mc.player.setSprinting(false);
        ((IKeyMappingExt) mc.options.keySprint).truppware$setPressed(false);
        ((IKeyMappingExt) mc.options.keyUp).truppware$setPressed(false);
        ((IKeyMappingExt) mc.options.keyDown).truppware$setPressed(false);
        ((IKeyMappingExt) mc.options.keyLeft).truppware$setPressed(false);
        ((IKeyMappingExt) mc.options.keyRight).truppware$setPressed(false);
    }

    private void restoreInputs() {
        if (mc.player == null) return;
        if (!inputsSuppressed) return;
        if (savedForward) ((IKeyMappingExt) mc.options.keyUp).truppware$setPressed(true);
        if (savedBack)    ((IKeyMappingExt) mc.options.keyDown).truppware$setPressed(true);
        if (savedLeft)    ((IKeyMappingExt) mc.options.keyLeft).truppware$setPressed(true);
        if (savedRight)   ((IKeyMappingExt) mc.options.keyRight).truppware$setPressed(true);
        if (savedSprint)  ((IKeyMappingExt) mc.options.keySprint).truppware$setPressed(true);
        inputsSuppressed = false;
    }

    private void swapTotem() {
        int slot = findTotem();
        if (slot == -1) return;
        int screenSlot = (slot < 9) ? slot + 36 : slot;
        mc.gameMode.handleInventoryMouseClick(
                mc.player.inventoryMenu.containerId,
                screenSlot,
                OFFHAND_SLOT,
                ClickType.SWAP,
                mc.player
        );
    }

    private int findTotem() {
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() == Items.TOTEM_OF_UNDYING) return i;
        }
        for (int i = 9; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() == Items.TOTEM_OF_UNDYING) return i;
        }
        return -1;
    }
}