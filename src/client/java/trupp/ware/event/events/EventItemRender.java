package trupp.ware.event.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import trupp.ware.event.Event;

public class EventItemRender extends Event {


    public static EventItemRender LAST = null;

    private final PoseStack matrices;
    private final InteractionHand hand;
    private final ItemStack item;
    private final float swingProgress;
    private final float equipProgress;


    private float translateX = 0;
    private float translateY = 0;
    private float translateZ = 0;
    private float scale = 1.0f;
    private float rotateX = 0;
    private float rotateY = 0;
    private float rotateZ = 0;
    private boolean fakeSwordBlock = false;

    public EventItemRender(PoseStack matrices, InteractionHand hand, ItemStack item, float swingProgress, float equipProgress) {
        super("ItemRender");
        this.matrices = matrices;
        this.hand = hand;
        this.item = item;
        this.swingProgress = swingProgress;
        this.equipProgress = equipProgress;
        LAST = this;
    }

    public PoseStack getMatrices() { return matrices; }
    public InteractionHand getHand() { return hand; }
    public ItemStack getItem() { return item; }
    public float getSwingProgress() { return swingProgress; }
    public float getEquipProgress() { return equipProgress; }

    public float getTranslateX() { return translateX; }
    public float getTranslateY() { return translateY; }
    public float getTranslateZ() { return translateZ; }
    public float getScale() { return scale; }
    public float getRotateX() { return rotateX; }
    public float getRotateY() { return rotateY; }
    public float getRotateZ() { return rotateZ; }
    public boolean isFakeSwordBlock() { return fakeSwordBlock; }

    public EventItemRender translate(float x, float y, float z) {
        this.translateX = x;
        this.translateY = y;
        this.translateZ = z;
        return this;
    }

    public EventItemRender scale(float scale) {
        this.scale = scale;
        return this;
    }

    public EventItemRender rotate(float x, float y, float z) {
        this.rotateX = x;
        this.rotateY = y;
        this.rotateZ = z;
        return this;
    }

    public EventItemRender fakeSwordBlock(boolean value) {
        this.fakeSwordBlock = value;
        return this;
    }
}