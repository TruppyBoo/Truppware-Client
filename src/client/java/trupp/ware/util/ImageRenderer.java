package trupp.ware.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import java.io.InputStream;

/**
 * Loads a PNG from the mod's resources and blits it to the HUD (GuiGraphics) — for client logos and
 * icons. Lazy: the texture is uploaded to the GPU the first time it's drawn (on the render thread).
 *
 * <p>Drop your PNG in {@code src/main/resources/assets/trupp/} and construct with the classpath path,
 * e.g. {@code new ImageRenderer("/assets/trupp/mylogo.png")}. PNG (with transparency) is recommended.</p>
 *
 * <p>Usage, from a render module on {@code EventRender}:</p>
 * <pre>
 *   if (Images.LOGO.isReady())
 *       Images.LOGO.draw(graphics, 6, 4, 80, 20);   // x, y, width, height in GUI pixels
 * </pre>
 */
public class ImageRenderer {

    private final String resourcePath;
    private Identifier id;
    private int width, height;
    private boolean initialized, failed;

    public ImageRenderer(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    private void init() {
        if (initialized || failed) return;
        try {
            InputStream is = ImageRenderer.class.getResourceAsStream(resourcePath);
            if (is == null) {
                System.out.println("[TruppWare] Image not found: " + resourcePath);
                failed = true;
                return;
            }
            NativeImage img = NativeImage.read(is);
            is.close();
            width  = img.getWidth();
            height = img.getHeight();

            DynamicTexture tex = new DynamicTexture(() -> "truppware-image", img);
            id = Identifier.fromNamespaceAndPath("truppware", "img/" + sanitize(resourcePath));
            Minecraft.getInstance().getTextureManager().register(id, tex);

            initialized = true;
            System.out.println("[TruppWare] Image ready: " + resourcePath + " (" + width + "x" + height + ")");
        } catch (Exception ex) {
            ex.printStackTrace();
            failed = true;
        }
    }

    private static String sanitize(String path) {
        String s = path.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
        while (s.startsWith("/")) s = s.substring(1);
        return s;
    }

    /** True once the texture is uploaded (call from the render thread). */
    public boolean isReady() { init(); return initialized; }

    /** Native pixel width / height of the source image. */
    public int getWidth()  { init(); return width; }
    public int getHeight() { init(); return height; }

    /** Draw the image at ({@code x},{@code y}) scaled to {@code drawW} x {@code drawH} GUI pixels. */
    public void draw(GuiGraphics g, float x, float y, float drawW, float drawH) {
        draw(g, x, y, drawW, drawH, 0xFFFFFFFF);
    }

    /**
     * Draw the image scaled to {@code drawW} x {@code drawH}, multiplied by ARGB {@code tint}
     * (use {@code 0xFFFFFFFF} for the image's true colours, or e.g. {@code 0x80FFFFFF} for 50% alpha).
     */
    public void draw(GuiGraphics g, float x, float y, float drawW, float drawH, int tint) {
        init();
        if (!initialized || width <= 0 || height <= 0) return;

        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(drawW / width, drawH / height);
        g.blit(RenderPipelines.GUI_TEXTURED, id, 0, 0, 0f, 0f, width, height, width, height, tint);
        pose.popMatrix();
    }

    /** Draw the image at its native pixel size at ({@code x},{@code y}). */
    public void drawNative(GuiGraphics g, float x, float y) {
        init();
        if (!initialized) return;
        draw(g, x, y, width, height, 0xFFFFFFFF);
    }
}
