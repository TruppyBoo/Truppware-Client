package trupp.ware.truppware.module.modules.MOVEMENT;

import trupp.ware.event.Event;
import trupp.ware.event.events.EventNoSlow;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;

public class Noslow extends Module {

    public Noslow() {
        super("Noslow", Category.MOVEMENT, "Local visual noslow", 0);
    }

    @Override
    public void onEnable() {}

    @Override
    public void OnDisable() {}

    @Override
    public void onEvent(Event e, Timing t) {
        if (e instanceof EventNoSlow event) {
            event.setX(1);
            event.setY(1);
        }
    }
}