package trupp.ware.util;

public class TimerUtil {
    private long lastTime = 0;

    public TimerUtil() {
        lastTime = System.currentTimeMillis();
    }

    public long getElapsed() { return System.currentTimeMillis() - lastTime; }

    public boolean hasElapsed(long ms) {
        return System.currentTimeMillis() - lastTime >= ms;
    }

    public void reset() {
        lastTime = System.currentTimeMillis();
    }
}
