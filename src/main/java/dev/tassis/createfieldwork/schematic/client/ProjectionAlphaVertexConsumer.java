package dev.tassis.createfieldwork.schematic.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Multiplies emitted alpha, preserving the native cached mesh and all other
 * vertex attributes. Scoped to one ghost submission; no global shader color. */
public final class ProjectionAlphaVertexConsumer implements VertexConsumer {
    public static final int OFFHAND_ALPHA = 166; // Approximately 65% opacity.
    private final VertexConsumer delegate;

    public ProjectionAlphaVertexConsumer(VertexConsumer delegate) { this.delegate = delegate; }
    @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) {
        delegate.setColor(r, g, b, (a * OFFHAND_ALPHA + 127) / 255); return this;
    }
    @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
}
