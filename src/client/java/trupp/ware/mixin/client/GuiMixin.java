package trupp.ware.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import trupp.ware.TruppWareClient;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventRender;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;

@Mixin(Gui.class)
public class GuiMixin {
	@Inject(at = @At("HEAD"),
			method = "renderTabList(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V")
	private void onRenderPlayerList(GuiGraphics context,
									DeltaTracker tickCounter, CallbackInfo ci){
			EventRender event = new EventRender(context, tickCounter);
			TruppWareClient.trupp.onEvent(event, Timing.PRE);


	}
}