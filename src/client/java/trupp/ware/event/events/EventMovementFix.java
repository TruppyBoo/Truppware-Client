package trupp.ware.event.events;


import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import trupp.ware.event.Event;

public class EventMovementFix extends Event {
    private float movementFactor;
    private Vec3 inputVector;
    private boolean canceled = false;

    public EventMovementFix(float movementFactor, Vec3 inputVector) {
        super("EventMovementFix");
        this.movementFactor = movementFactor;
        this.inputVector = inputVector;
    }

    public float getMovementFactor() {
        return movementFactor;
    }

    public void setMovementFactor(float movementFactor) {
        this.movementFactor = movementFactor;
    }

    public Vec3 getInputVector() {
        return inputVector;
    }

    public void setInputVector(Vec3 inputVector) {
        this.inputVector = inputVector;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}