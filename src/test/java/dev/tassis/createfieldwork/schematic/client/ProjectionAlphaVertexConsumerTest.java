package dev.tassis.createfieldwork.schematic.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ProjectionAlphaVertexConsumerTest {
    private static final class Recorded implements VertexConsumer {
        int r, g, b, a, overlayU, overlayV, lightU, lightV;
        float x, y, z, u, v, nx, ny, nz;
        @Override public VertexConsumer addVertex(float x, float y, float z) { this.x = x; this.y = y; this.z = z; return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { this.r = r; this.g = g; this.b = b; this.a = a; return this; }
        @Override public VertexConsumer setUv(float u, float v) { this.u = u; this.v = v; return this; }
        @Override public VertexConsumer setUv1(int u, int v) { overlayU = u; overlayV = v; return this; }
        @Override public VertexConsumer setUv2(int u, int v) { lightU = u; lightV = v; return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { nx = x; ny = y; nz = z; return this; }
    }
    @Test void fluentVerticesPreserveGeometryTextureLightAndRgb() {
        var recorded = new Recorded(); var ghost = new ProjectionAlphaVertexConsumer(recorded);
        assertSame(ghost, ghost.addVertex(1, 2, 3).setColor(12, 34, 56, 255).setUv(.25f, .75f)
            .setUv1(7, 8).setUv2(9, 10).setNormal(0, 1, 0));
        assertArrayEquals(new float[]{1, 2, 3, .25f, .75f, 0, 1, 0},
            new float[]{recorded.x, recorded.y, recorded.z, recorded.u, recorded.v, recorded.nx, recorded.ny, recorded.nz});
        assertArrayEquals(new int[]{12, 34, 56, 166, 7, 8, 9, 10},
            new int[]{recorded.r, recorded.g, recorded.b, recorded.a, recorded.overlayU, recorded.overlayV, recorded.lightU, recorded.lightV});
    }
    @Test void packedAndBulkNativeDefaultMethodsStillApplyAlphaExactlyOnce() {
        var recorded = new Recorded(); var ghost = new ProjectionAlphaVertexConsumer(recorded);
        ghost.addVertex(4, 5, 6, 0x80402010, .5f, .25f, 7, 8, 0, 1, 0);
        assertArrayEquals(new int[]{64, 32, 16, 83}, new int[]{recorded.r, recorded.g, recorded.b, recorded.a});
        ghost.setWhiteAlpha(0); assertEquals(0, recorded.a);
        ghost.setColor(1f, 1f, 1f, 1f); assertEquals(166, recorded.a);
        // A following native/main-hand emission has no persistent shader/mesh tint.
        recorded.setColor(255, 255, 255, 255); assertEquals(255, recorded.a);
    }
    @Test void auditedCreateHasExactlyTheThreeScopedGhostCallsWithoutLoadingClient() throws IOException {
        String pose = "Lcom/mojang/blaze3d/vertex/PoseStack;";
        String buffers = "Lnet/createmod/catnip/render/SuperRenderTypeBuffer;";
        String render = "(" + pose + buffers + "Lnet/minecraft/world/phys/Vec3;)V";
        var calls = new AtomicInteger();
        try (var source = getClass().getClassLoader().getResourceAsStream("com/simibubi/create/content/schematics/client/SchematicHandler.class")) {
            assertNotNull(source, "Audited Create must be present on the test classpath");
            new ClassReader(source).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (!name.equals("render") || !descriptor.equals(render)) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            if (opcode == Opcodes.INVOKEVIRTUAL && owner.equals("com/simibubi/create/content/schematics/client/SchematicRenderer") &&
                                name.equals("render") && descriptor.equals("(" + pose + buffers + ")V")) calls.incrementAndGet();
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertEquals(3, calls.get(), "Strict hook must cover normal and both mirrored ghost branches only");
    }
    @Test void auditedRendererHasUniqueBlockAndEntityMaskHooks() throws IOException {
        var blocks = new AtomicInteger(); var entities = new AtomicInteger();
        try (var source = getClass().getClassLoader().getResourceAsStream("com/simibubi/create/content/schematics/client/SchematicRenderer.class")) {
            assertNotNull(source);
            new ClassReader(source).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            if (method.equals("drawLayer") && owner.equals("net/createmod/catnip/levelWrappers/SchematicLevel") && name.equals("getBlockState") &&
                                descriptor.equals("(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;")) blocks.incrementAndGet();
                            if (method.equals("render") && owner.equals("com/simibubi/create/foundation/render/BlockEntityRenderHelper") && name.equals("renderBlockEntities") &&
                                descriptor.equals("(Ljava/util/List;Ljava/util/BitSet;Ljava/util/BitSet;Lcom/simibubi/create/foundation/virtualWorld/VirtualRenderWorld;Lnet/minecraft/world/level/Level;Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;F)V")) entities.incrementAndGet();
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertEquals(1, blocks.get()); assertEquals(1, entities.get());
    }
}
