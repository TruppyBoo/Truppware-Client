package trupp.ware.util;


public class Fonts {

    /** The font file used everywhere. Change this path to swap fonts. */
    private static final String FONT_PATH = "/assets/trupp/ZakenManus_PERSONAL_USE_ONLY.ttf";

    /** Main UI font (HUD, ArrayList, notifications). Rasterised large, drawn scaled for crisp text. */
    public static final CustomFontRenderer MAIN = new CustomFontRenderer(FONT_PATH, 40f);
}
