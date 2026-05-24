package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventAttack;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.TimerUtil;

import java.util.concurrent.ThreadLocalRandom;

public class Reach extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final TimerUtil timer = new TimerUtil();

    public NumberSetting minReach = new NumberSetting("MinReach", 3.0, 3.0, 6.0, 0.1);
    public NumberSetting maxReach = new NumberSetting("MaxReach", 3.2, 3.0, 6.0, 0.1);
    public NumberSetting aboveChance = new NumberSetting("AboveChance", 60, 0, 100, 1);

    public static double currentReach = 3.0;

    public Reach() {
        super("Reach", Category.COMBAT, "Randomizes reach distance", GLFW.GLFW_KEY_UNKNOWN);
        addSettings(minReach, maxReach, aboveChance);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventTick)) return;
        if (mc.player == null) return;

        if (!mc.mouseHandler.isLeftPressed()) {
            currentReach = 3.0;
            return;
        }

        if(mc.player.swingTime == 3) randomizeReach();

      //  }
    }

    private void randomizeReach() {
        double min = minReach.getNum();
        double max = maxReach.getNum();

        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }

        // Roll against the chance to go above 3.0
        double roll = ThreadLocalRandom.current().nextDouble(100);
        if (roll >= aboveChance.getNum()) {
            // Failed the roll — stay at exactly 3.0
            currentReach = 3.0;
            return;
        }

        // Passed the roll — pick a random value between min and max
        int steps = (int) Math.round((max - min) / 0.1);
        if (steps <= 0) {
            currentReach = min;
            return;
        }
        int chosen = ThreadLocalRandom.current().nextInt(steps + 1);
        currentReach = Math.round((min + chosen * 0.1) * 10.0) / 10.0;
    }

    @Override
    public void OnDisable() {
        currentReach = 3.0;
        timer.reset();
    }
}