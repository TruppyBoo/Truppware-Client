package trupp.ware.event.events;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import trupp.ware.event.Event;

public class EventRender extends Event<Void> {
    private final GuiGraphics graphics;
    private final DeltaTracker delta;

    public EventRender(GuiGraphics graphics, DeltaTracker delta) {
        super("Render");
        this.graphics = graphics;
        this.delta = delta;
    }

    public GuiGraphics getGuiGraphics() {
        return graphics;
    }

    public DeltaTracker getDelta() {
        return delta;
    }
}