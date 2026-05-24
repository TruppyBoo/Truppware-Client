package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import trupp.ware.TruppWareClient;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventPacket;
import trupp.ware.event.events.EventRender;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.NumberSetting;

import java.util.concurrent.ThreadLocalRandom;

public class RealAutoClicker extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private long lastClickTime;


    private double lastCps = 0;

    public NumberSetting minCPS = new NumberSetting("Min CPS", 1, 20, 8, 1);
    public NumberSetting maxCPS = new NumberSetting("Max CPS", 1, 20, 12, 1);

    public RealAutoClicker() {
        super("AutoClicker", Category.COMBAT, "Clicks while holding left click", -1);
        this.addSettings(minCPS, maxCPS);
    }
    private long generateHumanDelay() {

        double min = Math.min(minCPS.getNum(), maxCPS.getNum());
        double max = Math.max(minCPS.getNum(), maxCPS.getNum());

        // Base CPS
        double cps = ThreadLocalRandom.current().nextDouble(min, max);

        // Smooth drift from previous CPS
        if (lastCps != 0) {
            double drift = ThreadLocalRandom.current().nextDouble(-0.5, 0.5);
            cps = Math.max(min, Math.min(max, lastCps + drift));
        }

        lastCps = cps;

        long delay = (long) (1000.0 / cps);

        // Tiny jitter (±3ms)
        delay += ThreadLocalRandom.current().nextLong(-3, 4);

        return Math.max(1, delay);
    }


    @Override
    public void onEvent(Event event, Timing time) {
        if (event instanceof EventRender) {
            if (mc.player == null || mc.level == null || mc.gameMode == null) return;


            if (!mc.mouseHandler.isLeftPressed()) return;

            TruppWareClient.trupp.logger.info(String.valueOf(mc.player.swingTime));


            double min = Math.min(minCPS.getNum(), maxCPS.getNum());
            double max = Math.max(minCPS.getNum(), maxCPS.getNum());

            double cps = ThreadLocalRandom.current().nextDouble(min, max + 0.01);
            long delay = (generateHumanDelay());



            HitResult hit = mc.hitResult;

            if (hit == null || hit.getType() == HitResult.Type.MISS || hit.getType() == HitResult.Type.ENTITY) {
                if (System.currentTimeMillis() - lastClickTime > delay) {
                    ((IKeyMappingExt) mc.options.keyAttack).truppware$click();

                    lastClickTime = System.currentTimeMillis();
                    KeyMapping useKey = mc.options.keyAttack;
                } else {
                    KeyMapping useKey = mc.options.keyAttack;
                    if (!(useKey instanceof IKeyMappingExt keyExt)) return;
                        keyExt.truppware$setPressed(false);
                }

            }
            else if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit; if (mc.level.getBlockState(blockHit.getBlockPos()).isAir()) {
                    if (System.currentTimeMillis() - lastClickTime > delay) {
                        ((IKeyMappingExt) mc.options.keyAttack).truppware$click();

                        lastClickTime = System.currentTimeMillis();
                        KeyMapping useKey = mc.options.keyAttack;
                        if (!(useKey instanceof IKeyMappingExt keyExt)) return;
                        keyExt.truppware$setPressed(true);
                    } else {
                        KeyMapping useKey = mc.options.keyAttack;
                        if (!(useKey instanceof IKeyMappingExt keyExt)) return;
                        keyExt.truppware$setPressed(false);
                    }

                }
            }
        }
    }
}
