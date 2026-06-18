package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.TimerUtil;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Trigger bot: when your REAL crosshair is on a living entity, it attacks using the same silent
 * attack call Aura uses ({@code mc.gameMode.attack} + swing) at a 1.8-style min/max CPS — no
 * rotation/aim assist, you point, it clicks. While engaged it also auto-blocks using Aura's
 * Watchdog block-hit, copied exactly.
 */
public class BlockBot extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final TimerUtil attackTimer = new TimerUtil();
    private long currentDelay = 0;
    private double lastCps = 0;

    private boolean blocking = false;

    public NumberSetting minCPS   = new NumberSetting("MinCPS",   1, 20, 8,  1);
    public NumberSetting maxCPS   = new NumberSetting("MaxCPS",   1, 20, 14, 1);
    public BooleanSetting blockHit = new BooleanSetting("BlockHit", true);

    public BlockBot() {
        super("BlockBot", Category.COMBAT, "Trigger bot using Aura's attack + Watchdog block", 0);
        addSettings(minCPS, maxCPS, blockHit);
    }

    @Override
    public void onEvent(Event event, Timing time) {
        if (!(event instanceof EventTick)) return;
        if (time == Timing.POST) return;
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.player.isDeadOrDying()) return;

        // Trigger-bot target: whatever your real crosshair is on (vanilla only sets this when you
        // are actually looking at the entity within reach — so this IS the "looking at" check).
        LivingEntity target = null;
        if (mc.hitResult instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity le
                && le != mc.player
                && !le.isDeadOrDying()) {
            target = le;
        }

        if (target == null) {
            stopBlocking();
            return;
        }

        // 1.8-style: pure CPS, no attack-cooldown gate.
        if (canAttack()) {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
            attackTimer.reset();
        }

        handleBlockHit(target);
    }

    // ── CPS timing (copied from Aura) ─────────────────────────────────────────
    private long generateDelay() {
        double min = Math.min(minCPS.getNum(), maxCPS.getNum());
        double max = Math.max(minCPS.getNum(), maxCPS.getNum());

        double cps = ThreadLocalRandom.current().nextDouble(min, max);
        if (lastCps != 0) {
            double drift = ThreadLocalRandom.current().nextDouble(-0.5, 0.5);
            cps = Math.max(min, Math.min(max, lastCps + drift));
        }
        lastCps = cps;

        long delay = (long) (1000.0 / cps);
        delay += ThreadLocalRandom.current().nextLong(-3, 4);
        return Math.max(1, delay);
    }

    private boolean canAttack() {
        if (attackTimer.hasElapsed(currentDelay)) {
            currentDelay = generateDelay();
            return true;
        }
        return false;
    }

    // ── Watchdog block-hit (copied exactly from Aura) ─────────────────────────
    private void stopBlocking() {
        if (!blocking) return;
        KeyMapping useKey = mc.options.keyUse;
        if (useKey instanceof IKeyMappingExt ext) ext.truppware$setPressed(false);
        blocking = false;
    }

    private void handleWatchdogBlockHit() {
        if (mc.player == null) return;
        KeyMapping useKey = mc.options.keyUse;

        blocking = true;

        if (blocking)
            if (useKey instanceof IKeyMappingExt ext) ext.truppware$setPressed(true);
    }

    private void handleBlockHit(LivingEntity target) {
        if (mc.player == null) return;

        if (target.hurtTime == 0) {
            stopBlocking();
            return;
        }
        if (!blockHit.isEnabled()) {
            stopBlocking();
            return;
        }
        if (!mc.player.getMainHandItem().is(ItemTags.SWORDS)) {
            stopBlocking();
            return;
        }
        if (mc.player.hurtTime >= 8) {
            stopBlocking();
            return;
        }

        handleWatchdogBlockHit();
    }

    @Override
    public void onEnable() {
        currentDelay = generateDelay();
        attackTimer.reset();
        lastCps = 0;
        blocking = false;
    }

    @Override
    public void OnDisable() {
        lastCps = 0;
        stopBlocking();
    }
}
