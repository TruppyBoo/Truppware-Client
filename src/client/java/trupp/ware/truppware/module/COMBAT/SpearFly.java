package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventAttackStrength;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;

public class SpearFly extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private int originalSlot = -1;
    private boolean onSpear = false;

    public SpearFly() {
        super("SpearFly", Category.COMBAT, "Spam spear lunge while holding right-click", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onEvent(Event event, Timing time) {
        if (event instanceof EventTick) {
            if (mc.player == null || mc.level == null) return;

            // Apply client-side haste 255 every tick
            try {
                mc.player.addEffect(new MobEffectInstance(MobEffects.HASTE, 40, 254, false, false));
            } catch (Throwable ignored) {}

            if (!mc.mouseHandler.isRightPressed()) {
                if (onSpear && originalSlot != -1) {
                    switchSlot(originalSlot);
                }
                originalSlot = -1;
                onSpear = false;
                return;
            }

            int spearSlot = findSpearInHotbar();
            if (spearSlot == -1) return;

            if (onSpear) {
                // Tick 2: swap back to original slot
                switchSlot(originalSlot);
                onSpear = false;
            } else {
                // Tick 1: swap to spear, attack with full strength
                originalSlot = mc.player.getInventory().getSelectedSlot();
                if (originalSlot == spearSlot) return;

                switchSlot(spearSlot);

                IKeyMappingExt attack = (IKeyMappingExt) mc.options.keyAttack;
                attack.truppware$setPressed(true);
                attack.truppware$click();
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                attack.truppware$setPressed(false);


                try {
                    java.lang.reflect.Field f = net.minecraft.world.entity.LivingEntity.class
                            .getDeclaredField("attackStrengthTicker");
                    f.setAccessible(true);
                    f.setInt(mc.player, 100);
                } catch (Exception ignored) {}

                onSpear = true;
            }
        }
    }

    private int findSpearInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.WOODEN_SPEAR
                    || stack.getItem() == Items.STONE_SPEAR
                    || stack.getItem() == Items.COPPER_SPEAR
                    || stack.getItem() == Items.IRON_SPEAR
                    || stack.getItem() == Items.GOLDEN_SPEAR
                    || stack.getItem() == Items.DIAMOND_SPEAR
                    || stack.getItem() == Items.NETHERITE_SPEAR) {
                return i;
            }
        }
        return -1;
    }

    private void switchSlot(int slot) {
        try {
            java.lang.reflect.Field f = mc.player.getInventory().getClass().getDeclaredField("selected");
            f.setAccessible(true);
            f.set(mc.player.getInventory(), slot);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}