package trupp.ware.truppware.module.modules.MOVEMENT;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import jdk.jfr.Enabled;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import trupp.ware.TruppWareClient;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.NumberSetting;

public class Fly extends Module {
    public Fly() {
        super("Flight", Category.MOVEMENT, "Test Flight not bypassing", GLFW.GLFW_KEY_G);
        this.addSettings(speed);
    }



    NumberSetting speed = new NumberSetting("Speed", 0.1, 10, 1, 0.1);
    public Player player = Minecraft.getInstance().player;



    @Override
    public void onEvent(Event e, Timing time){
        if(e instanceof EventTick && Minecraft.getInstance().player != null) {
            Minecraft mc = Minecraft.getInstance();
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlyingSpeed((float) speed.num);




        }
    }

    @Override
    public void onEnable(){
        TruppWareClient.trupp.logger.info("Toggled on");
        Minecraft mc = Minecraft.getInstance();
      //  mc.player.getAbilities().flying = true;
    }

    @Override
    public void OnDisable() {
        TruppWareClient.trupp.logger.info("Toggled off");
        Minecraft mc = Minecraft.getInstance();
        mc.player.getAbilities().flying = false;
    }



}
