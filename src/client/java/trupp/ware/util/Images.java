package trupp.ware.util;

/**
 * Loaded HUD images / logos. Add your own:
 *   1. drop a PNG in  src/main/resources/assets/trupp/   (e.g. logo.png)
 *   2. add a field here:  public static final ImageRenderer LOGO = new ImageRenderer("/assets/trupp/logo.png");
 *   3. draw it from a render module:  Images.LOGO.draw(graphics, x, y, w, h);
 */
public class Images {

    /** Client logo shown on the HUD. Replace this PNG (or point at your own) to rebrand. */
    public static final ImageRenderer LOGO = new ImageRenderer("/assets/truppware/icon.png");
}
