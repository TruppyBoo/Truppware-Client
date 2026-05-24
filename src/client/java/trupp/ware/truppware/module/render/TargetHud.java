package trupp.ware.truppware.module.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventRender;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.NumberSetting;

public class TargetHud extends Module {

    public NumberSetting range = new NumberSetting("Range", 1, 20, 8, 0.5);
    public BooleanSetting showHealth = new BooleanSetting("HealthBar", true);
    public BooleanSetting showDist   = new BooleanSetting("Distance", true);
    public BooleanSetting showPing   = new BooleanSetting("Ping", true);

    private float anim = 0f;
    private float displayedHealth = 0f;
    private long lastFrame = System.currentTimeMillis();
    private Player lastTarget = null;

    public TargetHud() {
        super("TargetHUD", Category.RENDER, "Shows current target info", -1);
        addSettings(range, showHealth, showDist, showPing);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventRender event)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Font font = mc.font;
        GuiGraphics graphics = event.getGuiGraphics();

        long now = System.currentTimeMillis();
        float delta = Math.min((now - lastFrame) / 1000f, 0.1f);
        lastFrame = now;

        // Find nearest player target ourselves
        Player target = findTarget(mc);

        // Animate based on target presence
        if (target != null) {
            anim = Math.min(1f, anim + delta * 6f);
            lastTarget = target;
        } else {
            anim = Math.max(0f, anim - delta * 4f);
        }

        if (anim <= 0.01f || lastTarget == null) return;
        if (lastTarget.isRemoved() || lastTarget.isDeadOrDying()) {
            anim = Math.max(0f, anim - delta * 6f);
            if (anim <= 0.01f) return;
        }

        Player draw = lastTarget;

        // Smooth health interpolation
        float realHealth = draw.getHealth() + draw.getAbsorptionAmount();
        if (displayedHealth <= 0f) displayedHealth = realHealth;
        displayedHealth += (realHealth - displayedHealth) * delta * 8f;

        float ease = anim * anim * (3f - 2f * anim);
        int alpha = (int)(ease * 255);

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        String name = draw.getName().getString();
        int nameWidth = font.width(name);
        int boxWidth = Math.max(nameWidth + 60, 160);
        int boxHeight = 42;

        float slideOffset = (1f - ease) * -30f;
        int boxX = (int)(8 + slideOffset) + 200;
        int boxY = screenHeight - boxHeight - 300;

        int healthColor = getHealthColor(realHealth, draw.getMaxHealth());

        // Background
        graphics.fillGradient(boxX, boxY, boxX + boxWidth, boxY + boxHeight,
                withAlpha(0x12121A, (int)(alpha * 0.9f)),
                withAlpha(0x08080F, (int)(alpha * 0.9f)));

        // Left accent
        graphics.fill(boxX, boxY, boxX + 2, boxY + boxHeight, withAlpha(healthColor, alpha));
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 1, withAlpha(healthColor, (int)(alpha * 0.4f)));

        int textX = boxX + 8;
        graphics.drawString(font, name, textX + 1, boxY + 5, withAlpha(0x000000, (int)(alpha * 0.6f)), false);
        graphics.drawString(font, name, textX, boxY + 5, withAlpha(0xFFFFFF, alpha), false);

        if (showHealth.isEnabled()) {
            int barX = textX;
            int barY = boxY + 18;
            int barWidth = boxWidth - 16;
            int barHeight = 6;

            graphics.fill(barX, barY, barX + barWidth, barY + barHeight, withAlpha(0x000000, (int)(alpha * 0.6f)));

            float maxHealth = draw.getMaxHealth() + draw.getAbsorptionAmount();
            if (maxHealth <= 0f) maxHealth = 20f;
            float healthPct = Math.max(0f, Math.min(1f, displayedHealth / maxHealth));
            int filledWidth = (int)(barWidth * healthPct);

            int healthDark = darken(healthColor, 0.6f);
            graphics.fillGradient(barX, barY, barX + filledWidth, barY + barHeight,
                    withAlpha(healthDark, alpha),
                    withAlpha(healthColor, alpha));

            String hpText = String.format("%.1f", realHealth);
            int hpWidth = font.width(hpText);
            graphics.drawString(font, hpText, barX + barWidth - hpWidth, barY + barHeight + 3,
                    withAlpha(0xFFFFFF, alpha), false);
        }

        StringBuilder info = new StringBuilder();
        if (showDist.isEnabled()) {
            double dist = mc.player.distanceTo(draw);
            info.append(String.format("%.1fm", dist));
        }
        if (showPing.isEnabled()) {
            int ping = getPing(draw);
            if (info.length() > 0) info.append("  ");
            info.append(ping).append("ms");
        }

        if (info.length() > 0) {
            graphics.drawString(font, info.toString(), textX, boxY + 32,
                    withAlpha(0xAAAAAA, alpha), false);
        }
    }

    private Player findTarget(Minecraft mc) {
        LocalPlayer self = mc.player;
        if (self == null || mc.level == null) return null;

        Vec3 eye = self.getEyePosition(1.0f);
        double maxRange = range.getNum();
        double bestDist = maxRange * maxRange;
        Player best = null;

        for (Player p : mc.level.players()) {
            if (p == self) continue;
            if (p.isDeadOrDying() || p.isInvisible()) continue;
            if (p.getTeam() != null && p.getTeam() == self.getTeam()) continue;
            double d = p.getEyePosition(1.0f).distanceToSqr(eye);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private int getPing(Player p) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                var info = mc.getConnection().getPlayerInfo(p.getUUID());
                if (info != null) return info.getLatency();
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    private int getHealthColor(float health, float max) {
        float pct = Math.max(0f, Math.min(1f, health / max));
        if (pct > 0.6f) return 0x4DFF8C;
        if (pct > 0.3f) return 0xFFD93D;
        return 0xFF5C5C;
    }

    private int darken(int color, float factor) {
        int r = (int)(((color >> 16) & 0xFF) * factor);
        int g = (int)(((color >> 8) & 0xFF) * factor);
        int b = (int)((color & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    private int withAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    @Override
    public void onEnable() {
        anim = 0f;
        displayedHealth = 0f;
        lastTarget = null;
    }
}