package trupp.ware.truppware.module.render;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventRender;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;

public class Fullbright extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private double previousGamma = 1.0;

    public Fullbright() {
        super("Fullbright", Category.RENDER, "Makes everything bright", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventRender)) return;
        if (mc.player == null) return;
        setGamma(1000.0);
    }

    @Override
    public void onEnable() {
        previousGamma = getGamma();
    }

    @Override
    public void OnDisable() {
        setGamma(previousGamma);
    }

    private void setGamma(double value) {
        try {
            java.lang.reflect.Field f = mc.options.gamma().getClass().getDeclaredField("value");
            f.setAccessible(true);
            f.set(mc.options.gamma(), value);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private double getGamma() {
        try {
            java.lang.reflect.Field f = mc.options.gamma().getClass().getDeclaredField("value");
            f.setAccessible(true);
            return (double) f.get(mc.options.gamma());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 1.0;
    }
}