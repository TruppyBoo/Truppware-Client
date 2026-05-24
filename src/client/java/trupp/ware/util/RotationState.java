package trupp.ware.util;

public final class RotationState {
    public static boolean active = false;
    public static float yaw;
    public static float pitch;

    public static void set(float y, float p) {
        yaw = y;
        pitch = p;
        active = true;
    }

    public static void clear() {
        active = false;
    }
}
