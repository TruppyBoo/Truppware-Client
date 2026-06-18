package trupp.ware.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;



public class CustomFontRenderer {

    private static final int FIRST_CHAR = 32;   // space
    private static final int LAST_CHAR  = 255;  // Latin-1 supplement
    private static final int COUNT      = LAST_CHAR - FIRST_CHAR + 1;
    private static final int PAD        = 2;     // padding around each glyph to avoid bleeding
    private static final int COLS       = 16;    // atlas columns

    private final String resourcePath;
    private final float  rasterSize;

    private Identifier atlasId;
    private int atlasWidth, atlasHeight;
    private boolean initialized = false;
    private boolean failed      = false;

    private final float[] glyphU = new float[COUNT];   // texel x of glyph in atlas
    private final float[] glyphV = new float[COUNT];   // texel y of glyph in atlas
    private final int[]   glyphW = new int[COUNT];      // glyph quad width  (texels)
    private final int[]   glyphAdv = new int[COUNT];    // horizontal advance
    private int cellHeight;                             // glyph quad height (texels)
    private int ascent;

    /**
     * @param resourcePath classpath path to the .ttf/.otf, e.g. "/assets/trupp/MyFont.ttf"
     * @param rasterSize   point size the atlas is rasterised at. Bigger = crisper when scaled
     *                     down, but more VRAM. 32-48 is a good range.
     */
    public CustomFontRenderer(String resourcePath, float rasterSize) {
        this.resourcePath = resourcePath;
        this.rasterSize   = rasterSize;
    }

    private void init() {
        if (initialized || failed) return;
        try {
            InputStream is = CustomFontRenderer.class.getResourceAsStream(resourcePath);
            if (is == null) {
                System.out.println("[TruppWare] Font not found: " + resourcePath);
                failed = true;
                return;
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(rasterSize);
            is.close();

            // ── Measure the font ────────────────────────────────────────────
            BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D pg = probe.createGraphics();
            pg.setFont(font);
            FontMetrics fm = pg.getFontMetrics();
            ascent     = fm.getAscent();
            cellHeight = fm.getHeight();

            int maxW = 1;
            int[] charW = new int[COUNT];
            for (int i = 0; i < COUNT; i++) {
                int w = fm.charWidth((char) (FIRST_CHAR + i));
                charW[i]   = Math.max(w, 0);
                glyphAdv[i] = Math.max(w, 0);
                if (w > maxW) maxW = w;
            }
            pg.dispose();

            int cellW = maxW + PAD * 2;
            int cellH = cellHeight + PAD * 2;
            int rows  = (COUNT + COLS - 1) / COLS;
            atlasWidth  = COLS * cellW;
            atlasHeight = rows * cellH;

            // ── Rasterise all glyphs (anti-aliased, white) ──────────────────
            BufferedImage atlas = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = atlas.createGraphics();
            g.setFont(font);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setColor(Color.WHITE);

            for (int i = 0; i < COUNT; i++) {
                int col = i % COLS;
                int row = i / COLS;
                int px  = col * cellW + PAD;
                int py  = row * cellH + PAD;

                g.drawString(String.valueOf((char) (FIRST_CHAR + i)), px, py + ascent);

                glyphU[i] = px;
                glyphV[i] = py;
                glyphW[i] = Math.max(charW[i], 1);
            }
            g.dispose();

            // ── Upload to the GPU ───────────────────────────────────────────
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, atlasWidth, atlasHeight, false);
            for (int y = 0; y < atlasHeight; y++) {
                for (int x = 0; x < atlasWidth; x++) {
                    int argb = atlas.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int gg = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    // NativeImage wants packed ABGR
                    img.setPixelABGR(x, y, (a << 24) | (b << 16) | (gg << 8) | r);
                }
            }

            DynamicTexture tex = new DynamicTexture(() -> "truppware-font", img);
            atlasId = Identifier.fromNamespaceAndPath("truppware", "font/zaken_" + (int) rasterSize);
            Minecraft.getInstance().getTextureManager().register(atlasId, tex);

            initialized = true;
            System.out.println("[TruppWare] Font atlas ready (" + atlasWidth + "x" + atlasHeight + ")");
        } catch (Exception e) {
            e.printStackTrace();
            failed = true;
        }
    }

    private int index(char c) {
        if (c < FIRST_CHAR || c > LAST_CHAR) return '?' - FIRST_CHAR;
        return c - FIRST_CHAR;
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /** True once the atlas has been built (call from the render thread). */
    public boolean isReady() {
        init();
        return initialized;
    }

    /** Width of the text at the given scale (in GUI pixels). */
    public float getWidth(String text, float scale) {
        init();
        if (!initialized || text == null) return 0;
        float w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) { i++; continue; } // skip § colour codes
            w += glyphAdv[index(c)];
        }
        return w * scale;
    }

    /** Line height of the text at the given scale (in GUI pixels). */
    public float getHeight(float scale) {
        init();
        return cellHeight * scale;
    }

    /**
     * Draw {@code text} with its top-left corner at ({@code x},{@code y}).
     *
     * @param color ARGB colour (include alpha, e.g. 0xFFFFFFFF for opaque white)
     * @param scale on-screen scale of the rasterised atlas (e.g. 0.5)
     * @return the advance width drawn (GUI pixels)
     */
    public float drawString(GuiGraphics gfx, String text, float x, float y, int color, float scale) {
        init();
        if (!initialized || text == null || text.isEmpty()) return 0;

        Matrix3x2fStack pose = gfx.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);

        int penX = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) { i++; continue; } // skip § colour codes

            int gi = index(c);
            if (c != ' ') {
                gfx.blit(RenderPipelines.GUI_TEXTURED, atlasId,
                        penX, 0,
                        glyphU[gi], glyphV[gi],
                        glyphW[gi], cellHeight,
                        atlasWidth, atlasHeight,
                        color);
            }
            penX += glyphAdv[gi];
        }

        pose.popMatrix();
        return penX * scale;
    }

    /** Draw with a soft 1px drop shadow for readability over any background. */
    public float drawStringWithShadow(GuiGraphics gfx, String text, float x, float y, int color, float scale) {
        int shadow = (((color >>> 24) * 5 / 8) << 24); // ~62% alpha, black
        drawString(gfx, text, x + scale, y + scale, shadow, scale);
        return drawString(gfx, text, x, y, color, scale);
    }
}
