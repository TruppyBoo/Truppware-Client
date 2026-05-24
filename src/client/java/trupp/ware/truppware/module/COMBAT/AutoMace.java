package trupp.ware.truppware.module.COMBAT;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventRender;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;

public class AutoMace extends Module {
    public AutoMace() {
        super("AutoMace", Category.COMBAT, "Auto attacks with mace when falling", -1);
    }

    private final Minecraft mc = Minecraft.getInstance();
    private int previousSlot = -1;
    private boolean switched = false;

    @Override
    public void onEvent(Event e, Timing time) {
        if (!(e instanceof EventRender)) return;
        if (mc.player == null) return;

        HitResult hit = mc.hitResult;

        if (!(mc.player.fallDistance > 000.1) || !(hit instanceof EntityHitResult eResult) || !(eResult.getEntity() instanceof LivingEntity target)) {
            if (switched) {
                switchSlot(previousSlot);
                previousSlot = -1;
                switched = false;
            }
            return;
        }

        int maceSlot = findMaceInHotbar();
        if (maceSlot == -1) return;

        if (!switched && !ShieldBreaker.shield) {
            previousSlot = mc.player.getInventory().getSelectedSlot();
            switchSlot(maceSlot);
            for(int i = 0; i <1; i ++)
                ((IKeyMappingExt) mc.options.keyAttack).truppware$click();
            switched = true;
        }
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

    private int findMaceInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof MaceItem) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void OnDisable() {
        if (switched && previousSlot != -1) {
            switchSlot(previousSlot);
        }
        previousSlot = -1;
        switched = false;
    }
}