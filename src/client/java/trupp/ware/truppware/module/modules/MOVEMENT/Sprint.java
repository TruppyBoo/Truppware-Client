package trupp.ware.truppware.module.modules.MOVEMENT;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import trupp.ware.TruppWareClient;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;

public class Sprint extends Module {
    public Sprint() {
        super("Sprint", Category.MOVEMENT, "Sprint", -1);
    }


    public Player player = Minecraft.getInstance().player;


    @Override
    public void onEnable(){
        TruppWareClient.trupp.logger.info("Toggled on");
        Minecraft mc = Minecraft.getInstance();
     //   mc.player.getAbilities().flying = true;
    }

    @Override
    public void OnDisable() {
        TruppWareClient.trupp.logger.info("Toggled off");
        Minecraft mc = Minecraft.getInstance();
        mc.player.setSprinting(false);
    }

    @Override
    public void onEvent(Event e, Timing time){
        if(e instanceof EventTick && Minecraft.getInstance().player != null){
            Minecraft mc = Minecraft.getInstance();
            mc.player.setSprinting(true);

        }
    }


}
