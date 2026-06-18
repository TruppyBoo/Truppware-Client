package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.TimerUtil;

import java.util.concurrent.ThreadLocalRandom;

public class CrystalAutoClicker extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final TimerUtil timer = new TimerUtil();
    private boolean placeNext = true;

    public NumberSetting minCPS =
            new NumberSetting("MinCPS", 1, 40, 18, 1);
    public NumberSetting maxCPS =
            new NumberSetting("MaxCPS", 1, 40, 22, 1);

    public CrystalAutoClicker() {
        super("CrystalAutoClicker", Category.COMBAT,
                "Place then break spam",
                GLFW.GLFW_KEY_J);
        addSettings(minCPS, maxCPS);
    }

    @Override
    public void onEvent(Event event, Timing time) {
        if (!(event instanceof EventTick)) return;
        if(time == Timing.POST) return;
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        // Only run while right mouse is held
        if (!mc.mouseHandler.isRightPressed()) return;

        // Only run if holding a crystal
        ItemStack mainHand = mc.player.getMainHandItem();
        if (mainHand.isEmpty() || mainHand.getItem() != Items.END_CRYSTAL) return;

        // Random CPS calculation
        double min = Math.min(minCPS.getNum(), maxCPS.getNum());
        double max = Math.max(minCPS.getNum(), maxCPS.getNum());
        double cps = ThreadLocalRandom.current().nextDouble(min, max + 0.01);
        long delay = (long) (1000D / cps);

        if (!timer.hasElapsed(delay)) return;
        timer.reset();

        IKeyMappingExt attack = (IKeyMappingExt) mc.options.keyAttack;
        IKeyMappingExt use = (IKeyMappingExt) mc.options.keyUse;

        if (placeNext) {
            use.truppware$setPressed(true);
            use.truppware$click();
            use.truppware$setPressed(false);
        } else {
            attack.truppware$setPressed(true);
            attack.truppware$click();
            attack.truppware$setPressed(false);
        }

        placeNext = !placeNext;
    }
}
