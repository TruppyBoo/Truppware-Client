package trupp.ware.util;

import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BufferedTextRenderer {

    private static Font awtFont;

    public static void loadFont(String path, float size) {
        try {
            awtFont = Font.createFont(Font.TRUETYPE_FONT, BufferedTextRenderer.class.getResourceAsStream(path))
                    .deriveFont(size);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static int getWidth(String text) {
        if (awtFont == null || text == null) return 0;
        BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = temp.createGraphics();
        g2.setFont(awtFont);
        int width = g2.getFontMetrics().stringWidth(text);
        g2.dispose();
        return width;
    }

    public static int getHeight(String text) {
        if (awtFont == null || text == null) return 0;
        BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = temp.createGraphics();
        g2.setFont(awtFont);
        int height = g2.getFontMetrics().getHeight();
        g2.dispose();
        return height;
    }


    public static void drawString(GuiGraphics guiGraphics, String text, int x, int y, int color, float scale) {
        if (text == null || text.isEmpty() || awtFont == null) return;

        // Create temporary image to measure text
        BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = temp.createGraphics();
        g2.setFont(awtFont);
        FontMetrics fm = g2.getFontMetrics();
        int width = fm.stringWidth(text);
        int height = fm.getHeight();
        g2.dispose();

        // Create actual text image
        BufferedImage textImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2 = textImage.createGraphics();
        g2.setFont(awtFont);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(color, true));
        g2.drawString(text, 0, fm.getAscent());
        g2.dispose();

        // Draw text pixel-by-pixel with scaling
        for (int yy = 0; yy < height; yy++) {
            for (int xx = 0; xx < width; xx++) {
                int argb = textImage.getRGB(xx, yy);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 0) {
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;

                    // Draw scaled pixel
                    int scaledX = x + (int)(xx * scale);
                    int scaledY = y + (int)(yy * scale);
                    int scaledW = (int)Math.ceil(scale);
                    int scaledH = (int)Math.ceil(scale);

                    guiGraphics.fill(scaledX, scaledY, scaledX + scaledW, scaledY + scaledH,
                            (alpha << 24) | (r << 16) | (g << 8) | b);
                }
            }
        }
    }
}
