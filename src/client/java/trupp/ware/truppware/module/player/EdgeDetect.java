package trupp.ware.truppware.module.player;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventMovementInput;
import trupp.ware.event.events.EventTick;
import trupp.ware.event.events.Timing;
import trupp.ware.interfaces.IKeyMappingExt;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;

public class EdgeDetect extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    private boolean shouldSneak = false;

    public EdgeDetect() {
        super("EdgeDetect", Category.PLAYER,
                "Automatically sneaks when partially unsupported",
                -1);
    }

    @Override
    public void onEvent(Event e, Timing time) {

        if (mc.player == null || mc.level == null) return;

        // Update edge state every tick (stable)
        if (e instanceof EventTick) {
            shouldSneak = isOnEdge();
        }

        if(e instanceof EventTick){
            Window window = mc.getWindow();
            int keyCode = mc.options.keyShift.getDefaultKey().getValue();

            boolean physicallyHoldingW =
                    com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, keyCode);

            if (physicallyHoldingW) {
                KeyMapping forward = mc.options.keyShift;
                if (forward instanceof IKeyMappingExt keyExt) {
                    keyExt.truppware$setPressed(false);
                }
            }
        }
        // Apply movement override cleanly
        if (e instanceof EventMovementInput movement) {
            Window window = mc.getWindow();
            int keyCode = mc.options.keyShift.getDefaultKey().getValue();
            boolean physicallyHoldingW =
                    com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, keyCode);
            if (shouldSneak && physicallyHoldingW) {
                movement.setSneak(true);
            }
        }
    }

    private boolean isOnEdge() {

        if (!mc.player.onGround()) return false;

        AABB box = mc.player.getBoundingBox();

        double checkY = box.minY - 0.001;

        // Check 4 corners under player
        return !isSolid(box.minX, checkY, box.minZ) ||
                !isSolid(box.minX, checkY, box.maxZ) ||
                !isSolid(box.maxX, checkY, box.minZ) ||
                !isSolid(box.maxX, checkY, box.maxZ);
    }

    private boolean isSolid(double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = mc.level.getBlockState(pos);
        return !state.isAir();
    }
}
