package trupp.ware.util;

import java.awt.Font;
import java.io.InputStream;
import java.io.IOException;

public class FontLoader {

    public static Font zakenFont;

    public static void loadFonts() {
        try {
            InputStream stream = FontLoader.class.getResourceAsStream("/assets/trupp/ZakenManus_PERSONAL_USE_ONLY.otf");

            if(stream == null) {
                System.out.println("Font not found!");
                return;
            }

            zakenFont = Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(20f); // 20f = default size
            stream.close();

            System.out.println("Font loaded successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
