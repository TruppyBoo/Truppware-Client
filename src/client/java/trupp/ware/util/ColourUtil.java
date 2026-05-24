package trupp.ware.util;

import java.awt.Color;

public class ColourUtil {


    public static int getRainbow(int delay, float speed) {
        long time = System.currentTimeMillis();
        float hue = ((time + delay) % (int)(speed * 1000)) / (speed * 1000f);
        Color color = Color.getHSBColor(hue, 1f, 1f);
        return (0xFF << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }

}
