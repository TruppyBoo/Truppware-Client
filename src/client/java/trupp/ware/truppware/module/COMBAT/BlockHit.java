package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.NumberSetting;

import java.util.concurrent.ThreadLocalRandom;

public class BlockHit extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private long nextBlockTime = 0L;
    private long unblockTime   = 0L;
    private boolean blocking   = false;

    public NumberSetting minCPS = new NumberSetting("MinBlock", 1, 20, 8, 1);
    public NumberSetting maxCPS = new NumberSetting("MaxBlock", 1, 20, 12, 1);
    public BooleanSetting smart = new BooleanSetting("Smart", true);

    public BlockHit() {
        super("BlockHit", Category.COMBAT, "Block hits while left-click attacking", -1);
        addSettings(minCPS, maxCPS, smart);
    }

    @Override
    public void OnDisable() {
        stopBlocking();
        blocking = false;
        nextBlockTime = 0L;
        unblockTime = 0L;
    }

    @Override
    public void onEvent(Event event, Timing time) {
        if (!(event instanceof EventTick)) return;
        if(time == Timing.POST) return;
        if (mc.player == null || mc.level == null) return;

        if (!mc.player.getMainHandItem().is(net.minecraft.tags.ItemTags.SWORDS)) {
            stopBlocking();
            return;
        }

        boolean leftClick = mc.mouseHandler.isLeftPressed();

        // Only run when manually left-clicking — Aura handles its own blockhit
        if (!leftClick || Aura.targetInRange) {
            stopBlocking();
            return;
        }

        // Don't block jumping up
        if (!mc.player.onGround() && mc.player.getDeltaMovement().y > 0) {
            stopBlocking();
            return;
        }

        if (mc.player.hurtTime >= 8) {
            stopBlocking();
            return;
        }

        // Must be looking at entity
        HitResult hit = mc.hitResult;
        if (!(hit instanceof EntityHitResult ehr) || !(ehr.getEntity() instanceof LivingEntity t)) {
            stopBlocking();
            return;
        }
        if (t.hurtTime >= 8) {
            stopBlocking();
            return;
        }

        long now = System.currentTimeMillis();

        if (smart.isEnabled()) {
            if (blocking) {
                KeyMapping useKey = mc.options.keyUse;
                if (useKey instanceof IKeyMappingExt ext) ext.truppware$setPressed(true);
            }

            if (blocking && now >= unblockTime) stopBlocking();

            if (mc.player.swingTime >= 1 && mc.player.swingTime <= 2) {
                stopBlocking();
                return;
            }

            if (mc.player.swingTime <= 2 || mc.player.swingTime > 5) return;
            if (blocking || now < nextBlockTime) return;

            KeyMapping useKey = mc.options.keyUse;
            if (useKey instanceof IKeyMappingExt ext) {
                ext.truppware$setPressed(true);
                blocking = true;
            }

            unblockTime   = now + ThreadLocalRandom.current().nextLong(45, 85);
            nextBlockTime = now + getRandomDelay();

        } else {
            if (mc.player.swingTime >= 1 && mc.player.swingTime <= 2) {
                stopBlocking();
                return;
            }

            if (mc.player.swingTime < 1) return;
            if (now < nextBlockTime) return;

            ((IKeyMappingExt) mc.options.keyUse).truppware$click();
            nextBlockTime = now + getRandomDelay();
        }
    }

    private void stopBlocking() {
        if (!blocking) return;
        KeyMapping useKey = mc.options.keyUse;
        if (useKey instanceof IKeyMappingExt ext) ext.truppware$setPressed(false);
        blocking = false;
    }

    private long getRandomDelay() {
        double min = Math.min(minCPS.getNum(), maxCPS.getNum());
        double max = Math.max(minCPS.getNum(), maxCPS.getNum());
        double cps = ThreadLocalRandom.current().nextDouble(min, max);
        return (long) (1000D / cps);
    }
}