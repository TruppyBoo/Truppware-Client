package trupp.ware.truppware.module.render.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Manager;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.ModeSetting;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.truppware.module.settings.Setting;
import trupp.ware.util.ColourUtil;
import trupp.ware.util.Fonts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Modern, mouse-driven click GUI.
 *
 * <p>Left-click a module to toggle it, click the +/- to expand its settings, drag a panel
 * header to move it, and drag the sliders to adjust numbers.</p>
 */
public class ClickGui extends Screen {

    private static final int PANEL_W   = 124;
    private static final int HEADER_H  = 19;
    private static final int ROW_H     = 15;
    private static final int SETTING_H = 14;
    private static final int ARROW_W   = 16;

    private static final float HEADER_SCALE = 0.27f;
    private static final float ROW_SCALE    = 0.23f;
    private static final float SET_SCALE    = 0.21f;

    // Theme
    private static final int PANEL_BG  = 0xE6121218;
    private static final int HEADER_BG = 0xF01C1C26;
    private static final int ROW_BG    = 0xC014141B;
    private static final int SET_BG    = 0xC01A1A22;
    private static final int HOVER     = 0x18FFFFFF;
    private static final int TEXT_ON   = 0xFFFFFFFF;
    private static final int TEXT_OFF  = 0xFF8A8A96;
    private static final int TRACK     = 0xFF2A2A35;

    private static final Map<String, float[]> panelPos = new HashMap<>();
    private static final Set<String> expanded = new HashSet<>();

    private Category dragPanel;
    private float dragOffX, dragOffY;
    private NumberSetting dragSlider;
    private float sliderTrackX, sliderTrackW;

    public ClickGui() {
        super(Component.literal("TruppWare"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private float[] pos(Category c, int index) {
        return panelPos.computeIfAbsent(c.name(),
                k -> new float[]{ 12 + index * (PANEL_W + 10), 14 });
    }

    private int accent() {
        return ColourUtil.getRainbow(0, 4f) | 0xFF000000;
    }

    private static boolean in(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // ── Text via custom font (falls back to vanilla font until the atlas is built) ──
    private void txt(GuiGraphics g, String s, float x, float y, int color, float scale) {
        if (Fonts.MAIN.isReady()) Fonts.MAIN.drawString(g, s, x, y, color, scale);
        else g.drawString(Minecraft.getInstance().font, s, (int) x, (int) y, color, false);
    }

    private float tw(String s, float scale) {
        if (Fonts.MAIN.isReady()) return Fonts.MAIN.getWidth(s, scale);
        return Minecraft.getInstance().font.width(s);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        // No renderBackground() — its blur can only run once per frame and crashes here.
        g.fill(0, 0, this.width, this.height, 0x66000000);

        // Drive drags here by polling the real mouse button — mouseDragged/mouseReleased don't
        // fire reliably in this version, so this is the robust way to make sliders/panels drag.
        long win = Minecraft.getInstance().getWindow().handle();
        boolean lmb = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!lmb) {
            dragPanel = null;
            dragSlider = null;
        } else {
            if (dragPanel != null) {
                float[] p = panelPos.get(dragPanel.name());
                if (p != null) { p[0] = mouseX - dragOffX; p[1] = mouseY - dragOffY; }
            }
            if (dragSlider != null) setSlider(dragSlider, mouseX);
        }

        int accent = accent();
        Category[] cats = Category.values();

        for (int ci = 0; ci < cats.length; ci++) {
            Category cat = cats[ci];
            float[] p = pos(cat, ci);
            float x = p[0], y = p[1];
            List<Module> modules = Manager.trupp.getModuleByCat(cat);

            // total height
            float cy = y + HEADER_H;
            for (Module m : modules) {
                cy += ROW_H;
                if (expanded.contains(m.name)) cy += m.getSettings().size() * SETTING_H;
            }

            g.fill((int) x, (int) y, (int) (x + PANEL_W), (int) cy, PANEL_BG);
            g.fill((int) x, (int) y, (int) (x + PANEL_W), (int) (y + HEADER_H), HEADER_BG);
            g.fill((int) x, (int) y, (int) (x + PANEL_W), (int) (y + 2), accent);
            txt(g, cat.name(), x + 8, y + 7, TEXT_ON, HEADER_SCALE);

            float ry = y + HEADER_H;
            for (Module m : modules) {
                boolean on = m.toggled;
                boolean hov = in(mouseX, mouseY, x, ry, PANEL_W, ROW_H);

                g.fill((int) x, (int) ry, (int) (x + PANEL_W), (int) (ry + ROW_H), ROW_BG);
                if (hov) g.fill((int) x, (int) ry, (int) (x + PANEL_W), (int) (ry + ROW_H), HOVER);
                if (on) g.fill((int) x, (int) ry, (int) x + 2, (int) (ry + ROW_H), accent);

                txt(g, m.name, x + 8, ry + 4, on ? TEXT_ON : TEXT_OFF, ROW_SCALE);
                if (!m.getSettings().isEmpty()) {
                    String arrow = expanded.contains(m.name) ? "-" : "+";
                    txt(g, arrow, x + PANEL_W - 11, ry + 4, on ? accent : TEXT_OFF, ROW_SCALE);
                }
                ry += ROW_H;

                if (expanded.contains(m.name)) {
                    for (Setting s : m.getSettings()) {
                        g.fill((int) x, (int) ry, (int) (x + PANEL_W), (int) (ry + SETTING_H), SET_BG);
                        renderSetting(g, s, x, ry, accent);
                        ry += SETTING_H;
                    }
                }
            }
        }

        txt(g, "Left-click: toggle    +/-: settings    Drag header: move",
                8, this.height - 11, 0xFF6A6A72, 0.2f);
    }

    private void renderSetting(GuiGraphics g, Setting s, float x, float ry, int accent) {
        txt(g, s.name, x + 12, ry + 3, 0xFFC8C8D0, SET_SCALE);

        if (s instanceof BooleanSetting b) {
            String v = b.isEnabled() ? "ON" : "OFF";
            int col = b.isEnabled() ? accent : 0xFFFF5C5C;
            txt(g, v, x + PANEL_W - tw(v, SET_SCALE) - 8, ry + 3, col, SET_SCALE);
        } else if (s instanceof ModeSetting m) {
            String v = m.getCurrentMode();
            txt(g, v, x + PANEL_W - tw(v, SET_SCALE) - 8, ry + 3, accent, SET_SCALE);
        } else if (s instanceof NumberSetting n) {
            float trackX = x + 12;
            float trackW = PANEL_W - 24;
            float barY = ry + SETTING_H - 4;
            double frac = (n.getNum() - n.getMin()) / Math.max(1e-9, (n.getMax() - n.getMin()));
            frac = Math.max(0, Math.min(1, frac));
            g.fill((int) trackX, (int) barY, (int) (trackX + trackW), (int) barY + 2, TRACK);
            g.fill((int) trackX, (int) barY, (int) (trackX + trackW * frac), (int) barY + 2, accent);
            // knob
            int kx = (int) (trackX + trackW * frac);
            g.fill(kx - 1, (int) barY - 2, kx + 2, (int) barY + 4, 0xFFFFFFFF);
            String v = fmt(n.getNum());
            txt(g, v, x + PANEL_W - tw(v, SET_SCALE) - 8, ry + 2, TEXT_ON, SET_SCALE);
        }
    }

    private static String fmt(double val) {
        double r = Math.round(val * 100.0) / 100.0;
        if (r == (long) r) return String.valueOf((long) r);
        return String.format("%.2f", r);
    }

    // ── Mouse ────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        double mx = e.x(), my = e.y();
        int btn = e.button();
        Category[] cats = Category.values();

        for (int ci = 0; ci < cats.length; ci++) {
            Category cat = cats[ci];
            float[] p = pos(cat, ci);
            float x = p[0], y = p[1];

            if (in(mx, my, x, y, PANEL_W, HEADER_H)) {
                if (btn == 0) { dragPanel = cat; dragOffX = (float) (mx - x); dragOffY = (float) (my - y); }
                return true;
            }

            float ry = y + HEADER_H;
            for (Module m : Manager.trupp.getModuleByCat(cat)) {
                if (in(mx, my, x, ry, PANEL_W, ROW_H)) {
                    boolean onArrow = !m.getSettings().isEmpty() && mx >= x + PANEL_W - ARROW_W;
                    if (btn == 1 || onArrow) {
                        if (!m.getSettings().isEmpty() && !expanded.remove(m.name)) expanded.add(m.name);
                    } else if (btn == 0) {
                        m.toggle();
                    }
                    return true;
                }
                ry += ROW_H;

                if (expanded.contains(m.name)) {
                    for (Setting s : m.getSettings()) {
                        if (in(mx, my, x, ry, PANEL_W, SETTING_H)) {
                            handleSettingClick(s, mx, btn, x);
                            return true;
                        }
                        ry += SETTING_H;
                    }
                }
            }
        }
        return super.mouseClicked(e, dbl);
    }

    private void handleSettingClick(Setting s, double mx, int btn, float x) {
        if (s instanceof BooleanSetting b) {
            b.setEnabled(!b.isEnabled());
        } else if (s instanceof ModeSetting m) {
            m.cycle();
        } else if (s instanceof NumberSetting n) {
            sliderTrackX = x + 12;
            sliderTrackW = PANEL_W - 24;
            dragSlider = n;
            setSlider(n, mx);
        }
    }

    private void setSlider(NumberSetting n, double mx) {
        double frac = (mx - sliderTrackX) / sliderTrackW;
        frac = Math.max(0, Math.min(1, frac));
        double raw = n.getMin() + frac * (n.getMax() - n.getMin());
        double inc = n.getInc() <= 0 ? 1 : n.getInc();
        double snapped = Math.round(raw / inc) * inc;
        snapped = Math.max(n.getMin(), Math.min(n.getMax(), snapped));
        n.setValue(snapped);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dragX, double dragY) {
        double mx = e.x(), my = e.y();
        if (dragPanel != null) {
            float[] p = panelPos.get(dragPanel.name());
            if (p != null) { p[0] = (float) (mx - dragOffX); p[1] = (float) (my - dragOffY); }
            return true;
        }
        if (dragSlider != null) {
            setSlider(dragSlider, mx);
            return true;
        }
        return super.mouseDragged(e, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        dragPanel = null;
        dragSlider = null;
        return super.mouseReleased(e);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == 256) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(keyEvent);
    }
}
