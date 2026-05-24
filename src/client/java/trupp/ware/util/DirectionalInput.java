package trupp.ware.util;

import net.minecraft.world.entity.player.Input;


public class DirectionalInput {

    private boolean forwards;
    private boolean backwards;
    private boolean left;
    private boolean right;



    public DirectionalInput(boolean forwards, boolean backwards, boolean left, boolean right) {
        this.forwards = forwards;
        this.backwards = backwards;
        this.left = left;
        this.right = right;
    }


    public DirectionalInput(Input input) {
        this(
                input.forward(),
                input.backward(),
                input.left(),
                input.right()
        );
    }

    /** Construct from movement impulses */
    public DirectionalInput(float movementForward, float movementSideways) {
        this(
                movementForward > 0.0F,
                movementForward < 0.0F,
                movementSideways > 0.0F,
                movementSideways < 0.0F
        );
    }

    /* ---------------- Logic ---------------- */

    public DirectionalInput invert() {
        return new DirectionalInput(
                backwards,
                forwards,
                right,
                left
        );
    }

    public boolean isMoving() {
        return (forwards && !backwards)
                || (backwards && !forwards)
                || (left && !right)
                || (right && !left);
    }

    /* ---------------- Getters ---------------- */

    public boolean isForward() {
        return forwards;
    }

    public boolean isBackward() {
        return backwards;
    }

    public boolean isLeft() {
        return left;
    }

    public boolean isRight() {
        return right;
    }

    /* ---------------- Setters ---------------- */

    public void setForward(boolean forwards) {
        this.forwards = forwards;
    }

    public void setBackward(boolean backwards) {
        this.backwards = backwards;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }

    public void setRight(boolean right) {
        this.right = right;
    }

    /* ---------------- Presets ---------------- */

    public static final DirectionalInput NONE =
            new DirectionalInput(false, false, false, false);

    public static final DirectionalInput FORWARDS =
            new DirectionalInput(true, false, false, false);

    public static final DirectionalInput BACKWARDS =
            new DirectionalInput(false, true, false, false);

    public static final DirectionalInput LEFT =
            new DirectionalInput(false, false, true, false);

    public static final DirectionalInput RIGHT =
            new DirectionalInput(false, false, false, true);

    public static final DirectionalInput FORWARDS_LEFT =
            new DirectionalInput(true, false, true, false);

    public static final DirectionalInput FORWARDS_RIGHT =
            new DirectionalInput(true, false, false, true);

    public static final DirectionalInput BACKWARDS_LEFT =
            new DirectionalInput(false, true, true, false);

    public static final DirectionalInput BACKWARDS_RIGHT =
            new DirectionalInput(false, true, false, true);
}
