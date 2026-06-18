package trupp.ware.truppware.module.render;

import net.minecraft.client.gui.GuiGraphics;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventRender;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Manager;
import trupp.ware.truppware.module.Module;
import trupp.ware.util.ColourUtil;
import trupp.ware.util.Fonts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Modulelist extends Module {

    private final Map<String, Float> animations = new HashMap<>();
    private long lastFrame = System.currentTimeMillis();

    // Notification queue — static so Module.toggle() can push from anywhere
    private static final List<Notification> notifications = new ArrayList<>();

    public Modulelist() {
        super("ArrayList", Category.RENDER, "Shows all modules toggled", -1);
        toggled = true;
    }

    /** Static entry point — called from Module.toggle() */
    public static void pushNotification(String moduleName, boolean enabled) {
        notifications.add(new Notification(moduleName, enabled));
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventRender event)) return;

        GuiGraphics graphics = event.getGuiGraphics();
        if (!Fonts.MAIN.isReady()) return;

        final float scale = 0.22f;

        long now = System.currentTimeMillis();
        float delta = Math.min((now - lastFrame) / 1000f, 0.1f);
        lastFrame = now;

        // ── ARRAYLIST ──────────────────────────────────────────────
        List<Module> activeModules = Manager.trupp.modules.stream()
                .filter(Module::getToggled)
                .sorted(Comparator.comparingDouble(m -> -Fonts.MAIN.getWidth(m.getName(), scale)))
                .collect(Collectors.toList());

        for (Module m : activeModules) {
            float current = animations.getOrDefault(m.getName(), 0f);
            current = Math.min(1f, current + delta * 8f);
            animations.put(m.getName(), current);
        }

        animations.entrySet().removeIf(entry -> {
            boolean stillActive = activeModules.stream().anyMatch(m -> m.getName().equals(entry.getKey()));
            if (!stillActive) {
                entry.setValue(entry.getValue() - delta * 10f);
                return entry.getValue() <= 0f;
            }
            return false;
        });

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        float textHeight = Fonts.MAIN.getHeight(scale);
        int barHeight = (int) textHeight + 4;
        float y = 4f;

        for (int i = 0; i < activeModules.size(); i++) {
            Module m = activeModules.get(i);
            String name = m.getName();

            float anim = animations.getOrDefault(name, 1f);
            float ease = anim * anim * (3f - 2f * anim);

            float textWidth = Fonts.MAIN.getWidth(name, scale);
            int barWidth = (int) textWidth + 10;

            float slideOffset = (1f - ease) * 20f;
            int barX = (int)(screenWidth - barWidth + slideOffset);
            float x = screenWidth - textWidth - 5 + slideOffset;
            int yi = (int) y;

            int alpha = (int)(ease * 255);
            int accent = ColourUtil.getRainbow(i * 40, 3f);

            graphics.fillGradient(barX, yi, screenWidth, yi + barHeight + 1,
                    withAlpha(0x12121A, (int)(alpha * 0.85f)),
                    withAlpha(0x08080F, (int)(alpha * 0.85f)));

            graphics.fill(barX, yi, barX + 2, yi + barHeight + 1, withAlpha(accent, alpha));

            float ty = yi + (barHeight - textHeight) / 2f + 1f;
            Fonts.MAIN.drawString(graphics, name, x, ty, withAlpha(0xFFFFFF, alpha), scale);

            y += barHeight + 1;
        }

        // ── NOTIFICATIONS ──────────────────────────────────────────
        float notifY = screenHeight - 28f;

        Iterator<Notification> it = notifications.iterator();
        while (it.hasNext()) {
            Notification n = it.next();
            n.update(delta);
            if (n.isExpired()) {
                it.remove();
                continue;
            }
            n.render(graphics, screenWidth, (int) notifY);
            notifY -= 22;
        }
    }

    private int withAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }


    private static class Notification {
        final String name;
        final boolean enabled;
        final long birthTime = System.currentTimeMillis();
        float anim = 0f;
        boolean fading = false;

        Notification(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
        }

        void update(float delta) {
            long age = System.currentTimeMillis() - birthTime;
            if (age > 1800) fading = true;

            if (fading) {
                anim = Math.max(0f, anim - delta * 5f);
            } else {
                anim = Math.min(1f, anim + delta * 8f);
            }
        }

        boolean isExpired() {
            return fading && anim <= 0f;
        }

        void render(GuiGraphics graphics, int screenWidth, int y) {
            float ease = anim * anim * (3f - 2f * anim);
            float scale = 0.42f;

            String label = (enabled ? "+ " : "- ") + name;
            float textWidth = Fonts.MAIN.getWidth(label, scale);
            int barWidth = (int) textWidth + 14;
            int barHeight = 18;

            float slideOffset = (1f - ease) * 50f;
            int barX = (int) (screenWidth - barWidth - 4 + slideOffset);
            int barEnd = barX + barWidth;

            int alpha = (int) (ease * 255);
            int accent = enabled ? 0x4DFF8C : 0xFF5C5C;

            // Dark background gradient
            graphics.fillGradient(barX, y, barEnd, y + barHeight,
                    withAlphaStatic(0x12121A, (int) (alpha * 0.9f)),
                    withAlphaStatic(0x08080F, (int) (alpha * 0.9f)));

            // Right-edge accent bar
            graphics.fill(barEnd - 2, y, barEnd, y + barHeight, withAlphaStatic(accent, alpha));
            // Soft glow
            graphics.fill(barEnd - 4, y, barEnd - 2, y + barHeight, withAlphaStatic(accent, alpha / 3));

            float textX = barX + 6;
            float textY = y + (barHeight - Fonts.MAIN.getHeight(scale)) / 2f + 1f;
            Fonts.MAIN.drawString(graphics, label, textX, textY, withAlphaStatic(0xFFFFFF, alpha), scale);
        }

        private static int withAlphaStatic(int color, int alpha) {
            alpha = Math.max(0, Math.min(255, alpha));
            return (color & 0x00FFFFFF) | (alpha << 24);
        }
    }
}