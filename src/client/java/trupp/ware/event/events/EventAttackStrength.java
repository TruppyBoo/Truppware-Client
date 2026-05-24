package trupp.ware.event.events;

import trupp.ware.event.Event;

public class EventAttackStrength extends Event {
    private float strength;

    public EventAttackStrength(float strength) {
        super("EventAttackStrength");
        this.strength = strength;
    }

    public float getStrength() {
        return strength;
    }

    public void setStrength(float strength) {
        this.strength = strength;
    }
}