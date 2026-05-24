package trupp.ware.event.events;

import trupp.ware.event.Event;

public class EventMoveVector extends Event {

    private float strafe;
    private float forward;
    private boolean modified = false;


    public EventMoveVector(float x, float y) {
        super("MoveVector");
        this.strafe  = x;
        this.forward = y;
    }

    public float getStrafe()  { return strafe; }
    public float getForward() { return forward; }
    public boolean isModified() { return modified; }


    public void set(float strafe, float forward) {
        this.strafe  = strafe;
        this.forward = forward;
        this.modified = true;
    }
}