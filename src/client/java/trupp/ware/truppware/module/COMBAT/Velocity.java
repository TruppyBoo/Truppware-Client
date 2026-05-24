package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventKnockback;
import trupp.ware.event.events.EventMovementInput;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.ModeSetting;
import trupp.ware.truppware.module.settings.NumberSetting;

import java.util.Arrays;
import java.util.Random;

public class Velocity extends Module {

    public ModeSetting mode = new ModeSetting("Mode", Arrays.asList("Jump", "Cancel"));
    public NumberSetting chance = new NumberSetting("Chance", 0, 100, 50, 1); // min=0, max=100, default=50, increment=1

    /** Prevents multiple jumps per hit */
    private boolean jumpedThisHit = false;

    private final Random random = new Random();

    public Velocity() {
        super("Velocity", Category.COMBAT, "Modify incoming knockback", -1);
        addSettings(mode, chance);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        String current = mode.getCurrentMode();

        /* ---------------- Cancel mode ---------------- */
        if (current.equals("Cancel")
                && e instanceof EventKnockback kb
                && time == Timing.PRE) {

            kb.setX(0);
            kb.setY(0);
            kb.setZ(0);
            return;
        }

        if (current.equals("Jump")) {
            if (e instanceof EventTick && player.hurtTime == 0) {
                jumpedThisHit = false;
            }

            if (!(e instanceof EventMovementInput movementEvent)) return;

            // Exact knockback tick
            if (player.hurtTime != 9) return;
            if (!player.onGround()) return;
            if (jumpedThisHit) return;

            DamageSource source = player.getLastDamageSource();
            if (source == null || !(source.getEntity() instanceof LivingEntity)) return;

            // Check chance
            int roll = random.nextInt(101); // 0 to 100 inclusive
            if (roll > chance.getNum()) return; // Only jump if roll is within chance

            // ONE precise jump
            movementEvent.setJump(true);
            jumpedThisHit = true;
        }
    }
}