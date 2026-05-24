package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.util.TimerUtil;

import java.util.concurrent.ThreadLocalRandom;

public class Autoclicker extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final TimerUtil critTimer = new TimerUtil();
    private boolean inCritSequence = false;
    private boolean hasClicked = false;
    private long currentClickAt = 75;
    private long currentResumeAt = 150;

    public NumberSetting minClickAt  = new NumberSetting("MinClickAt",  60,  10, 500, 5);
    public NumberSetting maxClickAt  = new NumberSetting("MaxClickAt",  90,  10, 500, 5);
    public NumberSetting minResumeAt = new NumberSetting("MinResumeAt", 130, 10, 500, 5);
    public NumberSetting maxResumeAt = new NumberSetting("MaxResumeAt", 170, 10, 500, 5);
    public NumberSetting hitChance   = new NumberSetting("HitChance",   85,  1,  100, 1);

    public Autoclicker() {
        super("Triggerbot", Category.COMBAT, "Automatically clicks on enemies", GLFW.GLFW_KEY_H);
        addSettings(minClickAt, maxClickAt, minResumeAt, maxResumeAt, hitChance);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventTick)) return;
        if (mc.player == null || mc.level == null) return;

        KeyMapping useKey = mc.options.keyUp;
        if (!(useKey instanceof IKeyMappingExt keyExt)) return;

        if (inCritSequence && mc.player.onGround()) {
            keyExt.truppware$setPressed(true);
            inCritSequence = false;
            hasClicked = false;
            return;
        }


        if (inCritSequence) {
            long elapsed = critTimer.getElapsed();
            if (!hasClicked && elapsed >= currentClickAt && !mc.player.isSprinting()) {
                ((IKeyMappingExt) mc.options.keyAttack).truppware$click();
                hasClicked = true;
            }
            if (elapsed >= currentResumeAt) {
                keyExt.truppware$setPressed(true);
                inCritSequence = false;
                hasClicked = false;
            }
            return;
        }


        if (!(mc.hitResult instanceof EntityHitResult eResult)) return;
        if (!(eResult.getEntity() instanceof LivingEntity target)) return;
        if (target.hurtTime > 0) return;
        if (mc.player.getAttackStrengthScale(0.0F) < 1.0F) return;


        if (mc.player.onGround()) {
            attack(keyExt, true);
            return;
        }


        if (mc.player.fallDistance < 0.1) return;

        if (mc.player.isSprinting()) {
            currentClickAt  = randomBetween((long) minClickAt.getNum(),  (long) maxClickAt.getNum());
            currentResumeAt = randomBetween((long) minResumeAt.getNum(), (long) maxResumeAt.getNum());
            keyExt.truppware$setPressed(false);
            inCritSequence = true;
            hasClicked = false;
            critTimer.reset();
            return;
        }


        attack(keyExt, true);
    }

    private void attack(IKeyMappingExt keyExt, boolean allowWeakHit) {
        boolean fullHit = rollHitChance();

        if (fullHit) {

            keyExt.truppware$setPressed(false);
            ((IKeyMappingExt) mc.options.keyAttack).truppware$click();
            keyExt.truppware$setPressed(true);
        } else if (allowWeakHit) {

            ((IKeyMappingExt) mc.options.keyAttack).truppware$click();
        }
    }

    private boolean rollHitChance() {
        return ThreadLocalRandom.current().nextInt(100) < (int) hitChance.getNum();
    }

    private long randomBetween(long min, long max) {
        if (min >= max) return min;
        return min + ThreadLocalRandom.current().nextLong(max - min + 1);
    }
}