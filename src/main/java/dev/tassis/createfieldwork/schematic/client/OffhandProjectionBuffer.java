package dev.tassis.createfieldwork.schematic.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.schematics.client.SchematicHandler;
import dev.tassis.createfieldwork.mixin.schematic.client.SchematicHandlerAccessor;
import dev.tassis.createfieldwork.schematic.input.BlueprintSelection;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

/** Only the selected handler's ghost receives this adapter. Chunk layers need
 * both alpha vertices AND a blended render type; alpha on a solid shader alone
 * would not produce transparency. Custom BE texture/shader types stay native. */
public final class OffhandProjectionBuffer implements SuperRenderTypeBuffer {
    private final SuperRenderTypeBuffer delegate;
    private OffhandProjectionBuffer(SuperRenderTypeBuffer delegate) { this.delegate = delegate; }

    public static SuperRenderTypeBuffer forGhost(SchematicHandler handler, SuperRenderTypeBuffer nativeBuffer) {
        return activeOffhand(handler) ? new OffhandProjectionBuffer(nativeBuffer) : nativeBuffer;
    }
    public static boolean activeOffhand(SchematicHandler handler) {
        var player = Minecraft.getInstance().player;
        if (player == null || !handler.isActive() || ((SchematicHandlerAccessor) handler).fieldwork$slot() != BlueprintSelection.OFFHAND_SLOT)
            return false;
        var selected = BlueprintSelection.select(player);
        return selected != null && selected.slot() == BlueprintSelection.OFFHAND_SLOT && selected.stack() == handler.getActiveSchematicItem();
    }
    private static boolean chunkLayer(RenderType type) { return RenderType.chunkBufferLayers().contains(type); }
    private static RenderType layer(RenderType type) {
        // Ghosts are immediate-mode geometry, not terrain chunk meshes. Iris can
        // replace the terrain pass/format and lose this alpha; use the native
        // entity-translucent shader path with the same block atlas instead.
        return chunkLayer(type) ? RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS) : type;
    }
    private static VertexConsumer alpha(RenderType type, VertexConsumer consumer) {
        return chunkLayer(type) ? new ProjectionAlphaVertexConsumer(consumer) : consumer;
    }
    @Override public VertexConsumer getEarlyBuffer(RenderType type) { return alpha(type, delegate.getEarlyBuffer(layer(type))); }
    @Override public VertexConsumer getBuffer(RenderType type) { return alpha(type, delegate.getBuffer(layer(type))); }
    @Override public VertexConsumer getLateBuffer(RenderType type) { return alpha(type, delegate.getLateBuffer(layer(type))); }
    @Override public void draw() { delegate.draw(); }
    @Override public void draw(RenderType type) { delegate.draw(layer(type)); }
}
