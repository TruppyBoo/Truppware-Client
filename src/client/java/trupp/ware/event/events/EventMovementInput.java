package trupp.ware.event.events;

import trupp.ware.event.Event;
import trupp.ware.util.DirectionalInput;


public class EventMovementInput extends Event {

    private DirectionalInput directionalInput;
    private boolean jump;
    private boolean sneak;

    public EventMovementInput(DirectionalInput directionalInput, boolean jump, boolean sneak) {
        super("EventMovementInput");
        this.directionalInput = directionalInput;
        this.jump = jump;
        this.sneak = sneak;
    }



    public DirectionalInput getDirectionalInput() {
        return directionalInput;
    }

    public void setDirectionalInput(DirectionalInput directionalInput) {
        this.directionalInput = directionalInput;
    }



    public boolean isJump() {
        return jump;
    }

    public void setJump(boolean jump) {
        this.jump = jump;
    }

    public boolean isSneak() {
        return sneak;
    }

    public void setSneak(boolean sneak) {
        this.sneak = sneak;
    }
}
