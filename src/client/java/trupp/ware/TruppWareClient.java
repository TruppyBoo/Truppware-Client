package trupp.ware;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import trupp.ware.command.CommandManager;
import trupp.ware.config.ConfigManager;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventWorldRender;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Manager;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.render.clickgui.ClickGui;
import trupp.ware.util.Projection;
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

		// Custom font (Fonts.MAIN) builds its GPU atlas lazily on the render thread.
		Manager.trupp.InitializeModules();
		CommandManager.getInstance.start();

		// 3D world rendering hook — dispatches EventWorldRender so modules can draw in the world.
		// BEFORE_DEBUG_RENDER is the phase vanilla uses for line/overlay (debug) rendering, so
		// line draws here are flushed safely by vanilla afterwards — no manual buffer flushing.
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null || mc.level == null) return;
			var camera = mc.gameRenderer.getMainCamera();
			Vec3 camPos = camera.position();

			// Snapshot matrices for HUD world->screen projection (camera-built, always available).
			Projection.snapshot(camPos, camera.xRot(), camera.yRot());

			// The 3D line event needs the engine's PoseStack, which can be null here (e.g. Iris).
			if (context.matrices() == null) return;
			EventWorldRender event = new EventWorldRender(
					context.matrices(), context.consumers(), camPos, camera.getPartialTickTime());
			onEvent(event, Timing.PRE);
		});

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
				try {
					m.onEvent(e, time);
				} catch (Throwable t) {
					// A module throwing here (e.g. on the network thread during a packet event)
					// must never propagate — otherwise it kills the netty channel and disconnects
					// us. Log it and keep going.
					logger.warning("Module '" + m.name + "' threw in onEvent: " + t);
				}
			}
		}

		// After modules have had their chance to claim the silent rotation this frame, advance the
		// release smooth-out (handles disabling / target-out-of-range easing back to real yaw).
		if (e instanceof trupp.ware.event.events.EventRender && time == Timing.PRE) {
			RotationUtil.update();
		}
	}


}