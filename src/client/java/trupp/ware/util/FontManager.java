package trupp.ware.util;

import java.awt.Font;
import java.io.InputStream;

public class FontManager {
    public static Font loadFont(String resourcePath, float size) {
        try {
            InputStream is = FontManager.class.getResourceAsStream(resourcePath); // e.g., "/fonts/MyFont.ttf"
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            font = font.deriveFont(size);
            return font;
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("Arial", Font.PLAIN, (int)size); // fallback
        }
    }
}
