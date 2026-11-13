package dev.tassis.createfieldwork.schematic.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

/** Native entity-translucent state with a draw-scoped contact bias, never a deployment offset. */
final class OffhandProjectionRenderType extends RenderStateShard {
    // One identity for acquisition and targeted flushes in every Catnip buffer phase.
    static final RenderType TYPE = create();

    // Inheritance exposes native protected shards; this holder is never instantiated.
    private OffhandProjectionRenderType() {
        super("fieldwork_projection", () -> {}, () -> {});
    }

    private static RenderType create() {
        var state = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
            .setTextureState(new TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(true);

        // Retain native LEQUAL, color/depth writes, output and outline defaults.
        // The native layer pushes/scales/applies model-view at the actual batch draw,
        // then pops/reapplies it on clear, including implicit buffer-switch flushes.
        return RenderType.create("create_fieldwork_offhand_projection", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, true, true, state);
    }
}
