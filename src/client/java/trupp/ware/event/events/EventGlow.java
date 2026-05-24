package trupp.ware.event.events;

import net.minecraft.world.entity.LivingEntity;
import trupp.ware.event.Event;

/**
 * Event that marks a LivingEntity to glow like spectral arrows.
 */
public class EventGlow extends Event {

    private final LivingEntity entity; // the entity we want to glow
    private boolean glow;               // whether it should glow
    private int color = 0xFFFFFF;       // glow color (default white)

    public EventGlow(LivingEntity entity) {
        super("EventGlow");
        this.entity = entity;
        this.glow = false; // default false
    }


    public LivingEntity getEntity() {
        return entity;
    }


    public boolean shouldGlow() {
        return glow;
    }


    public void setGlow(boolean glow) {
        this.glow = glow;
    }

    public int getColor() {
        return color;
    }


    public void setColor(int color) {
        this.color = color;
    }
}
