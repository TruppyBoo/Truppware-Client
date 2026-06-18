package trupp.ware.mixin.client;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the package-private {@link RenderType#create(String, RenderSetup)} factory so we can
 * build custom render types (e.g. a no-depth-test "through walls" line type for ESP).
 */
@Mixin(RenderType.class)
public interface RenderTypeAccessor {
    @Invoker("create")
    static RenderType truppware$create(String name, RenderSetup setup) {
        throw new AssertionError();
    }
}
