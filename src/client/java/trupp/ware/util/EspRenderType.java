package trupp.ware.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import trupp.ware.mixin.client.RenderTypeAccessor;

/**
 * A "through walls" line {@link RenderType}: a clone of vanilla {@code RenderPipelines.LINES} with
 * depth testing disabled, so ESP lines draw on top of everything (visible through terrain).
 */
public class EspRenderType {

    private static RenderType linesNoDepth;
    private static boolean failed;

    public static RenderType lines() {
        if (linesNoDepth == null && !failed) {
            try {
                linesNoDepth = build();
            } catch (Throwable t) {
                t.printStackTrace();
                failed = true;
            }
        }
        return linesNoDepth;
    }

    /** True if the custom type built successfully (otherwise callers should fall back). */
    public static boolean ready() {
        lines();
        return linesNoDepth != null;
    }

    private static RenderType build() {
        RenderPipeline base = RenderPipelines.LINES;

        RenderPipeline.Builder b = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("truppware", "pipeline/esp_lines"))
                .withVertexShader(base.getVertexShader())
                .withFragmentShader(base.getFragmentShader())
                .withVertexFormat(base.getVertexFormat(), base.getVertexFormatMode())
                .withCull(base.isCull())
                .withDepthWrite(false)
                .withColorWrite(base.isWriteColor(), base.isWriteAlpha())
                .withColorLogic(base.getColorLogic())
                .withPolygonMode(base.getPolygonMode())
                .withDepthBias(base.getDepthBiasScaleFactor(), base.getDepthBiasConstant())
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST); // <- through walls

        if (base.getBlendFunction().isPresent()) {
            b.withBlend(base.getBlendFunction().get());
        } else {
            b.withoutBlend();
        }

        for (String sampler : base.getSamplers()) {
            b.withSampler(sampler);
        }
        for (RenderPipeline.UniformDescription u : base.getUniforms()) {
            if (u.type() != null) {
                b.withUniform(u.name(), u.type());
            }
        }

        RenderPipeline pipeline = b.build();
        RenderSetup setup = RenderSetup.builder(pipeline)
                .bufferSize(RenderType.SMALL_BUFFER_SIZE)
                .createRenderSetup();

        return RenderTypeAccessor.truppware$create("truppware_esp_lines", setup);
    }
}
