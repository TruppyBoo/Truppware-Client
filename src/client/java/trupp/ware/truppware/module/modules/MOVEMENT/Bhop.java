package trupp.ware.truppware.module.modules.MOVEMENT;

import net.minecraft.client.Minecraft;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventMovementInput;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;

public class Bhop extends Module {
    public Bhop() {
        super("Bhop", Category.MOVEMENT, "Makes u hop", 0);
    }



    @Override
    public void onEvent(Event e, Timing time){
        if (!(e instanceof EventMovementInput movementEvent)) return;
        Minecraft mc = Minecraft.getInstance();
        if(mc.player.onGround()) movementEvent.setJump(true);
    }
}
