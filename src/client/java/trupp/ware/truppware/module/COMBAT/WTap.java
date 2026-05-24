package trupp.ware.truppware.module.COMBAT;


import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventAttack;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.NumberSetting;

public class WTap extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    // Adjustable delay after releasing W
    private final NumberSetting releaseDelay =
            new NumberSetting("ReleaseDelay", 1, 6, 3, 1);

    private final NumberSetting blockDelay =
            new NumberSetting("BlockDelay", 0, 3, 1, 1);

    private int tickCounter = 0;
    private boolean inSequence = false;
    private boolean blocking = false;

    public WTap() {
        super("WTap", Category.COMBAT, "Temporarily releases W on attack", -1);
        addSettings(blockDelay, releaseDelay);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (e instanceof EventTick) {
            Window window = mc.getWindow();
            int keyCode = mc.options.keyUp.getDefaultKey().getValue();

            boolean physicallyHoldingW =
                    com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, keyCode);

            if (!physicallyHoldingW) {
                KeyMapping forward = mc.options.keyUp;
                if (forward instanceof IKeyMappingExt keyExt) {
                    keyExt.truppware$setPressed(false);
                }
            }
        }
        /* ───── ATTACK TRIGGER ───── */
        if (e instanceof EventAttack) {
            KeyMapping forward = mc.options.keyUp;
            if (!(forward instanceof IKeyMappingExt keyExt)) return;
            if (mc.player == null) return;
            if (!mc.player.isSprinting()) return;
            if (inSequence) return;

            inSequence = true;
            blocking = false;
            tickCounter = 0;
        }

        /* ───── SEQUENCE LOGIC ───── */
        if (e instanceof EventTick && inSequence) {
            tickCounter++;

            KeyMapping forward = mc.options.keyUp;
            if (!(forward instanceof IKeyMappingExt keyExt)) return;

            // Wait before releasing W
            if (!blocking && tickCounter >= blockDelay.getNum()) {
                keyExt.truppware$setPressed(false);
                blocking = true;
                tickCounter = 0;
                return;
            }

            // Wait longer before pressing W again
            if (blocking && tickCounter >= releaseDelay.getNum()) {
                keyExt.truppware$setPressed(true);
                blocking = false;
                inSequence = false;
            }
        }
    }

    @Override
    public void OnDisable() {
        KeyMapping forward = mc.options.keyUp;
        if (forward instanceof IKeyMappingExt keyExt) {
            keyExt.truppware$setPressed(true);
        }
        inSequence = false;
        blocking = false;
    }
}
