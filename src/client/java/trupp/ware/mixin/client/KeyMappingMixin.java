package trupp.ware.mixin.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import trupp.ware.interfaces.IKeyMappingExt;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements IKeyMappingExt {


    @Shadow
    private boolean isDown;


    @Unique
    public void truppware$setPressed(boolean pressed) {
        KeyMapping self = (KeyMapping) (Object) this;

        if (pressed) {
            if (!this.isDown) {
                self.setDown(true);
                KeyMapping.click(self.getDefaultKey());
            }
        } else {
            self.setDown(false);
        }
    }

    /**
     * Simulate a single tap
     */
    @Unique
    public void truppware$click() {
        KeyMapping self = (KeyMapping) (Object) this;
        KeyMapping.click(self.getDefaultKey());
    }
    @Unique
    public void truppware$rclick() {
        Minecraft mc = Minecraft.getInstance();
        KeyMapping.click(mc.options.keyUse.getDefaultKey());
    }
}
