package trupp.ware.event.events;

import trupp.ware.event.Event;

public class EventSwingSpeed extends Event {
    private int duration;

    public EventSwingSpeed(int duration) {
        super("swingspeed");
        this.duration = duration;
    }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}