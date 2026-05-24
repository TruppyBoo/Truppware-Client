package trupp.ware.event.events;

import trupp.ware.event.Event;

public class EventNoSlow extends Event {
    public EventNoSlow(float x, float y) {
        super("EventNoSlow");
        this.x = x;
        this.y = y;
    }

    public float x, y;

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
