package trupp.ware;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import trupp.ware.command.CommandManager;
import trupp.ware.config.ConfigManager;
import trupp.ware.event.Event;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Manager;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.render.clickgui.ClickGui;
import trupp.ware.util.BufferedTextRenderer;
import trupp.ware.util.FontLoader;
import trupp.ware.util.RotationUtil;
import trupp.ware.util.TimerUtil;

import java.util.logging.Logger;

public class
TruppWareClient implements ClientModInitializer {


	TimerUtil timer = new TimerUtil();
	// Change your declaration to this:
	public static TruppWareClient trupp = new TruppWareClient();
	public Logger logger = Logger.getLogger("TruppWare");
	public boolean loaded = false;


	@Override
	public void onInitializeClient() {
		logger.info("Launched TruppWare");

		FontLoader.loadFonts();
		BufferedTextRenderer.loadFont("/assets/trupp/ZakenManus_PERSONAL_USE_ONLY.ttf", 35f);

		Manager.trupp.InitializeModules();
		CommandManager.getInstance.start();

		// SAFE HERE
	//	ConfigManager.load("latest");

		logger.info("Config loaded");
	}




	public void onKey(int key, int action){
		//logger.info(String.valueOf(key));
		if(Minecraft.getInstance().screen == null)
		Manager.trupp.toggle(key, action);
		if(key == GLFW.GLFW_KEY_RIGHT_SHIFT){
			Minecraft.getInstance().setScreen(new ClickGui());
		}
	}

	public void onEvent(Event e, Timing time){
		RotationUtil rotationUtil = new RotationUtil();
	//	rotationUtil.onEvent(e, time);

		CommandManager.getInstance.onEvent(e);


		if(!loaded && Minecraft.getInstance().player != null){
			ConfigManager.load("latest");
			loaded = true;
		}
		if(timer.hasElapsed(5000) && loaded){
			ConfigManager.autoSave();
			timer.reset();
		}
		for(Module m : Manager.trupp.modules){
			if(m.toggled){
				m.onEvent(e, time);
			}
		}
	}


}