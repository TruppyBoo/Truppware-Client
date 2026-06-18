package trupp.ware.truppware.module.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ProjectileItem;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventUseItemCooldown;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.NumberSetting;

import java.util.Random;


public class FastPlace extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final Random random = new Random();

    // Settings
    public final NumberSetting minCooldown = new NumberSetting("MinCooldown", 0, 5, 0, 1);
    public final NumberSetting maxCooldown = new NumberSetting("MaxCooldown", 0, 5, 0, 1);
    public final BooleanSetting blocks = new BooleanSetting("Blocks", true);
    public final BooleanSetting projectiles = new BooleanSetting("Projectiles", false);

    public FastPlace() {
        super("FastPlace", Category.PLAYER, "Places blocks or items faster", -1);


        this.addSettings(projectiles, blocks, maxCooldown, minCooldown);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (e instanceof EventUseItemCooldown event) {
            Item mainHand = mc.player.getMainHandItem().getItem();
            Item offHand = mc.player.getOffhandItem().getItem();

            boolean shouldApply = false;

            if (blocks.isEnabled() && (mainHand instanceof BlockItem || offHand instanceof BlockItem)) {
                shouldApply = true;
            }

            if (projectiles.isEnabled() && (mainHand instanceof ProjectileItem || offHand instanceof ProjectileItem)) {
                shouldApply = true;
            }

            if (!shouldApply) return;


            int cooldown = (int) (minCooldown.getNum() +
                                (maxCooldown.getNum() > minCooldown.getNum() ?
                                        random.nextInt((int) (maxCooldown.getNum() - minCooldown.getNum() + 1)) : 0));

            event.setCooldown(cooldown);
        }
    }
}
