package trupp.ware.event.events;

import net.minecraft.world.item.Item;
import trupp.ware.event.Event;


public class EventUseItemCooldown extends Event {

    private int cooldown; // in ticks

    public EventUseItemCooldown(int defaultCooldown) {
        super("EventUseItemCooldown");
        this.cooldown = defaultCooldown;
    }




    public int getCooldown() {
        return cooldown;
    }


    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }
}
