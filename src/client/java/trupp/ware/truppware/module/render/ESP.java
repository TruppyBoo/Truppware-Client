package trupp.ware.truppware.module.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import trupp.ware.TruppWareClient;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventGlow;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.util.ColourUtil;

import java.util.ArrayList;

public class ESP extends Module {

    public ESP() {
        super("GlowEsp", Category.RENDER, "Makes players glow", -1);
    }

    // Stores entities that should glow
    public final ArrayList<LivingEntity> glowingEntities = new ArrayList<>();




    @Override
    public void onEvent(Event e, Timing time) {
        if (e instanceof EventTick) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            glowingEntities.clear();


            for (AbstractClientPlayer player : mc.level.players()) {
                if (player.isRemoved() || player.getHealth() <= 0) continue;


                EventGlow glowEvent = new EventGlow(player);
                glowEvent.setGlow(true);
                glowEvent.setColor(ColourUtil.getRainbow(1, 0.5f));
                TruppWareClient.trupp.onEvent(glowEvent, Timing.PRE);

                if (glowEvent.shouldGlow()) {
                    glowingEntities.add(player);
                }
            }
        }
    }
}
