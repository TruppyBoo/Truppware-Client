package trupp.ware.truppware.module.COMBAT;

import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.NumberSetting;

/**
 * Config module for AutoMace's shield-popping multi-click. AutoMace swings ONCE by default; turn this
 * on to make it swing {@link #clicks} times in a single window (the burst that breaks a shield). The
 * click count lives here so the multi-click is opt-in and tunable from its own module.
 */
public class ShieldDestroyer extends Module {

    public NumberSetting clicks = new NumberSetting("clicks", 1, 100, 3, 1);

    public static ShieldDestroyer INSTANCE;
    public static boolean enabled;

    public ShieldDestroyer() {
        super("ShieldDestroyer", Category.COMBAT, "Lets AutoMace burst-click to break shields", -1);
        addSettings(clicks);
        INSTANCE = this;
    }

    /** Clicks AutoMace should fire per window: the configured burst when this is on, otherwise 1. */
    public static int clickCount() {
        return (enabled && INSTANCE != null) ? (int) INSTANCE.clicks.getNum() : 1;
    }

    @Override
    public void onEnable() {
        enabled = true;
    }

    @Override
    public void OnDisable() {
        enabled = false;
    }
}
