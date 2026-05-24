package trupp.ware.truppware.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventAttackStrength;
import trupp.ware.event.events.EventItemRender;
import trupp.ware.event.events.EventSwingSpeed;
import trupp.ware.event.events.Timing;
import trupp.ware.truppware.module.Category;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.ModeSetting;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.truppware.module.COMBAT.Aura;

import java.util.List;

public class Animation extends Module {

    public static Animation instance;

    public ModeSetting blockMode = new ModeSetting("BlockAnim", List.of(
            "1.7",      // classic 1.7 slide down
            "Smooth",   // slower raise into block
            "Snap",     // instant snap with slight overshoot feel
            "Low",      // block held low like a guard
            "Off"       // no block animation
    ));

    public NumberSetting swingSpeed = new NumberSetting("SwingSpeed", 6, 1, 20, 1);
    public NumberSetting scaleMain     = new NumberSetting("Scale",         1.0, 0.1, 3.0,    0.05);
    public NumberSetting translateX    = new NumberSetting("TranslateX",    0.0, -2.0, 2.0,   0.01);
    public NumberSetting translateY    = new NumberSetting("TranslateY",    0.0, -2.0, 2.0,   0.01);
    public NumberSetting translateZ    = new NumberSetting("TranslateZ",    0.0, -2.0, 2.0,   0.01);
    public NumberSetting rotateX       = new NumberSetting("RotateX",       0.0, -180.0, 180.0, 1.0);
    public NumberSetting rotateY       = new NumberSetting("RotateY",       0.0, -180.0, 180.0, 1.0);
    public NumberSetting rotateZ       = new NumberSetting("RotateZ",       0.0, -180.0, 180.0, 1.0);
    public NumberSetting swingDuration = new NumberSetting("SwingDuration", 6,   1, 20,        1);
    public NumberSetting oneSevenY     = new NumberSetting("1.7Y",          0.1, 0.05, 0.3,   0.01);
    public NumberSetting swingScale    = new NumberSetting("SwingScale",    0.9, 0.1, 1.0,    0.05);

    public Animation() {
        super("Animations", Category.RENDER, "Custom item animations", -1);
        instance = this;
        addSettings(blockMode, scaleMain, translateX, translateY, translateZ,
                rotateX, rotateY, rotateZ, swingDuration,
                oneSevenY, swingScale, swingSpeed);
    }

    @Override
    public void onEvent(Event e, Timing time) {
        if (e instanceof EventSwingSpeed swingEvent) {
            swingEvent.setDuration((int) swingSpeed.getNum());
            swingEvent.canceled = true;
            return;
        }
        if (e instanceof EventAttackStrength strengthEvent) {
                //strengthEvent.setStrength(1.0f);
               // strengthEvent.canceled = true;
        }
        if (!(e instanceof EventItemRender event)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        boolean clicking = Minecraft.getInstance().mouseHandler.isLeftPressed() || Aura.targetInRange;
        boolean isSword  = event.getItem().is(net.minecraft.tags.ItemTags.SWORDS);

        // Apply block animation mode
        if (isSword && clicking && !blockMode.getCurrentMode().equals("Off")) {
            event.fakeSwordBlock(true);
        }

        // Apply transforms
        if (translateX.getNum() != 0 || translateY.getNum() != 0 || translateZ.getNum() != 0) {
            event.translate(
                    (float) translateX.getNum(),
                    (float) translateY.getNum(),
                    (float) translateZ.getNum()
            );
        }
        if (rotateX.getNum() != 0 || rotateY.getNum() != 0 || rotateZ.getNum() != 0) {
            event.rotate(
                    (float) rotateX.getNum(),
                    (float) rotateY.getNum(),
                    (float) rotateZ.getNum()
            );
        }
        if (scaleMain.getNum() != 1.0) {
            event.scale((float) scaleMain.getNum());
        }
    }

    // Called by AnimationMixin after applyItemArmTransform in the block branch
    public void applyBlockAnimation(PoseStack matrices, HumanoidArm arm, float equipProgress, float swingProgress) {
        switch (blockMode.getCurrentMode()) {
            case "1.7" -> applyOneSeven(matrices, arm, swingProgress);
            case "Smooth" -> applySmooth(matrices, arm, equipProgress, swingProgress);
            case "Snap" -> applySnap(matrices, arm, equipProgress, swingProgress);
            case "Low" -> applyLow(matrices, arm, equipProgress, swingProgress);
        }
    }

    // ── 1.7 — classic slide down with swing offset ───────────────────────────
    public void applyOneSeven(PoseStack matrices, HumanoidArm arm, float swingProgress) {
        float ty = (float) oneSevenY.getNum();
        float ss = (float) swingScale.getNum();

        matrices.translate(
                (arm == HumanoidArm.RIGHT ? -0.1f : 0.1f) + (float) translateX.getNum(),
                ty + (float) translateY.getNum(),
                (float) translateZ.getNum()
        );
        applySwingOffset(matrices, arm, swingProgress * ss);
    }

    // ── Smooth — item eases up into guard position, gentle arc ───────────────
    private void applySmooth(PoseStack matrices, HumanoidArm arm, float equipProgress, float swingProgress) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float ease = Mth.clamp(equipProgress, 0f, 1f);

        matrices.translate(
                side * -0.05f,
                0.05f + ease * 0.05f,
                0.0f
        );
        matrices.mulPose(Axis.ZP.rotationDegrees(side * ease * -5f));
        applySwingOffset(matrices, arm, swingProgress * (float) swingScale.getNum());
    }

    // ── Snap — fast raise with slight tilt, feels snappy and reactive ────────
    private void applySnap(PoseStack matrices, HumanoidArm arm, float equipProgress, float swingProgress) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float snap = Mth.clamp(equipProgress * 1.4f, 0f, 1f);

        matrices.translate(
                side * -0.08f,
                0.12f * snap,
                -0.05f * snap
        );
        matrices.mulPose(Axis.XP.rotationDegrees(-8f * snap));
        matrices.mulPose(Axis.ZP.rotationDegrees(side * -4f * snap));
        applySwingOffset(matrices, arm, swingProgress * (float) swingScale.getNum());
    }

    // ── Low — item held low like a guard stance, closer to the hip ──────────
    private void applyLow(PoseStack matrices, HumanoidArm arm, float equipProgress, float swingProgress) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;

        matrices.translate(
                side * -0.05f,
                -0.05f,
                0.05f
        );
        matrices.mulPose(Axis.XP.rotationDegrees(12f));
        matrices.mulPose(Axis.ZP.rotationDegrees(side * -6f));
        applySwingOffset(matrices, arm, swingProgress * (float) swingScale.getNum());
    }

    public static void applySwingOffset(PoseStack matrices, HumanoidArm arm, float swingProgress) {
        int armSide = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.mulPose(Axis.YP.rotationDegrees(armSide * (45.0f + f * -20.0f)));
        float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        matrices.mulPose(Axis.ZP.rotationDegrees(armSide * g * -20.0f));
        matrices.mulPose(Axis.XP.rotationDegrees(g * -80.0f));
        matrices.mulPose(Axis.YP.rotationDegrees(armSide * -45.0f));
    }
}