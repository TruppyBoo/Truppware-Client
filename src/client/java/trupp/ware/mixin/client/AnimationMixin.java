package trupp.ware.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import trupp.ware.TruppWareClient;
import trupp.ware.event.events.EventItemRender;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.render.Animation;

@Mixin(ItemInHandRenderer.class)
public class AnimationMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    // ── 1. General transform hook (translate / scale / rotate from settings) ──
    @Inject(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void hookItemTransform(
            AbstractClientPlayer player,
            float tickProgress,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            PoseStack matrices,
            SubmitNodeCollector collector,
            int light,
            CallbackInfo ci
    ) {
        if (player != minecraft.player) return;

        EventItemRender event = new EventItemRender(matrices, hand, item, swingProgress, equipProgress);
        TruppWareClient.trupp.onEvent(event, Timing.PRE);

        if (event.getTranslateX() != 0 || event.getTranslateY() != 0 || event.getTranslateZ() != 0) {
            matrices.translate(event.getTranslateX(), event.getTranslateY(), event.getTranslateZ());
        }
        if (event.getScale() != 1.0f) {
            matrices.scale(event.getScale(), event.getScale(), event.getScale());
        }
        if (event.getRotateX() != 0) matrices.mulPose(Axis.XP.rotationDegrees(event.getRotateX()));
        if (event.getRotateY() != 0) matrices.mulPose(Axis.YP.rotationDegrees(event.getRotateY()));
        if (event.getRotateZ() != 0) matrices.mulPose(Axis.ZP.rotationDegrees(event.getRotateZ()));
    }

    // ── 2. 1.7 block animation — fires AFTER applyItemArmTransform in the block branch ──
    @Inject(
            method = "renderArmWithItem",
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/ItemUseAnimation;")
            ),
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void hookBlockAnimation(
            AbstractClientPlayer player,
            float tickProgress,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            PoseStack matrices,
            SubmitNodeCollector collector,
            int light,
            CallbackInfo ci
    ) {
        if (player != minecraft.player) return;

        EventItemRender event = EventItemRender.LAST;
        if (event == null || !event.isFakeSwordBlock()) return;

        Animation anim = Animation.instance;
        if (anim == null || !anim.toggled) return;

        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();


        Animation.instance.applyBlockAnimation(matrices, arm, equipProgress, swingProgress);

    }


    @ModifyExpressionValue(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/ItemUseAnimation;", ordinal = 0)
    )
    private ItemUseAnimation hookUseAnimation(ItemUseAnimation original) {
        if (minecraft.player == null) return original;
        EventItemRender event = EventItemRender.LAST;
        if (event != null && event.isFakeSwordBlock()) return ItemUseAnimation.BLOCK;
        return original;
    }


    @ModifyExpressionValue(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z", ordinal = 1)
    )
    private boolean hookIsUsingItem(boolean original) {
        EventItemRender event = EventItemRender.LAST;
        if (event != null && event.isFakeSwordBlock()) return true;
        return original;
    }


    @ModifyExpressionValue(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUsedItemHand()Lnet/minecraft/world/InteractionHand;", ordinal = 1)
    )
    private InteractionHand hookUsedItemHand(InteractionHand original) {
        EventItemRender event = EventItemRender.LAST;
        if (event != null && event.isFakeSwordBlock()) return InteractionHand.MAIN_HAND;
        return original;
    }


    @ModifyExpressionValue(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUseItemRemainingTicks()I", ordinal = 2)
    )
    private int hookUseItemTicks(int original) {
        EventItemRender event = EventItemRender.LAST;
        if (event != null && event.isFakeSwordBlock()) return 7200;
        return original;
    }
}