package dev.tassis.createfieldwork.schematic.client;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Structural checks of actual pinned class resources, without loading client/GL state. */
class OffhandProjectionRenderTypeTest {
    private static final String RENDER = "net/minecraft/client/renderer/RenderType";
    private static final String SHARD = "net/minecraft/client/renderer/RenderStateShard";
    private static final String LOCAL = "dev/tassis/createfieldwork/schematic/client/OffhandProjectionRenderType";
    private static final String ADAPTER = "dev/tassis/createfieldwork/schematic/client/OffhandProjectionBuffer";

    @Test void customFactoryChangesOnlyLayeringFromThePinnedNativeState() throws IOException {
        var nativeClass = read(RENDER);
        var nativeFactory = nativeClass.methods.stream()
            .filter(method -> code(method).stream().anyMatch(insn -> insn instanceof LdcInsnNode ldc
                && "entity_translucent".equals(ldc.cst))).findFirst().orElseThrow();
        var custom = method(read(LOCAL), "create", "()L" + RENDER + ";");
        var nativeShards = shards(nativeFactory);
        assertEquals(List.of("RENDERTYPE_ENTITY_TRANSLUCENT_SHADER", "TRANSLUCENT_TRANSPARENCY",
            "NO_CULL", "LIGHTMAP", "OVERLAY"), nativeShards);
        var expected = new ArrayList<>(nativeShards);
        expected.add("VIEW_OFFSET_Z_LAYERING");
        assertEquals(expected, shards(custom));

        for (var factory : List.of(nativeFactory, custom)) {
            var instructions = code(factory);
            var texture = calls(factory).stream().filter(call -> call.owner.equals(SHARD + "$TextureStateShard")
                && call.name.equals("<init>")).findFirst().orElseThrow();
            int textureIndex = instructions.indexOf(texture);
            assertEquals(List.of(Opcodes.ICONST_0, Opcodes.ICONST_0), instructions.subList(textureIndex - 2, textureIndex)
                .stream().map(AbstractInsnNode::getOpcode).toList(), "No blur or mipmap change");
            var create = calls(factory).stream().filter(call -> call.owner.equals(RENDER) && call.name.equals("create"))
                .findFirst().orElseThrow();
            int index = instructions.indexOf(create);
            var args = instructions.subList(index - 6, index);
            assertEquals("NEW_ENTITY", ((FieldInsnNode) args.get(0)).name);
            assertEquals("QUADS", ((FieldInsnNode) args.get(1)).name);
            assertEquals(1536, ((IntInsnNode) args.get(2)).operand);
            assertEquals(Opcodes.ICONST_1, args.get(3).getOpcode(), "Crumbling remains enabled");
            assertEquals(Opcodes.ICONST_1, args.get(4).getOpcode(), "Sorting remains enabled");
        }
        var nativeEntry = method(nativeClass, "entityTranslucent", "(Lnet/minecraft/resources/ResourceLocation;)L" + RENDER + ";");
        var overload = calls(nativeEntry).stream().filter(call -> call.owner.equals(RENDER)
            && call.name.equals("entityTranslucent") && call.desc.equals("(Lnet/minecraft/resources/ResourceLocation;Z)L" + RENDER + ";"))
            .findFirst().orElseThrow();
        assertEquals(Opcodes.ICONST_1, code(nativeEntry).get(code(nativeEntry).indexOf(overload) - 1).getOpcode(), "Native outline=true overload");
        var state = calls(custom).stream().filter(call -> call.name.equals("createCompositeState")).findFirst().orElseThrow();
        assertEquals(Opcodes.ICONST_1, code(custom).get(code(custom).indexOf(state) - 1).getOpcode(), "Retain outline=true");
        assertTrue(code(custom).stream().anyMatch(insn -> insn instanceof FieldInsnNode field
            && field.owner.equals("net/minecraft/client/renderer/texture/TextureAtlas") && field.name.equals("LOCATION_BLOCKS")));
    }

    @Test void nativeBuilderKeepsDepthWritesOcclusionAndMainOutputDefaults() throws IOException {
        var constructor = method(read(RENDER + "$CompositeState$CompositeStateBuilder"), "<init>", "()V");
        var defaults = Map.of("depthTestState", "LEQUAL_DEPTH_TEST", "writeMaskState", "COLOR_DEPTH_WRITE",
            "layeringState", "NO_LAYERING", "outputState", "MAIN_TARGET", "texturingState", "DEFAULT_TEXTURING");
        var instructions = code(constructor);
        defaults.forEach((name, expected) -> {
            var write = instructions.stream().filter(insn -> insn instanceof FieldInsnNode field
                && field.getOpcode() == Opcodes.PUTFIELD && field.name.equals(name)).findFirst().orElseThrow();
            assertEquals(expected, ((FieldInsnNode) instructions.get(instructions.indexOf(write) - 1)).name);
        });
    }

    @Test void nativeViewOffsetPushesScalesAppliesThenPopsAndReapplies() throws IOException {
        var shard = read(SHARD);
        var initializer = code(method(shard, "<clinit>", "()V"));
        var name = initializer.stream().filter(insn -> insn instanceof LdcInsnNode ldc
            && "view_offset_z_layering".equals(ldc.cst)).findFirst().orElseThrow();
        var callbacks = initializer.stream().skip(initializer.indexOf(name) + 1L)
            .filter(InvokeDynamicInsnNode.class::isInstance).limit(2).map(InvokeDynamicInsnNode.class::cast)
            .flatMap(insn -> Arrays.stream(insn.bsmArgs)).filter(Handle.class::isInstance).map(Handle.class::cast).toList();
        assertEquals(2, callbacks.size());
        var setup = method(shard, callbacks.getFirst().getName(), callbacks.getFirst().getDesc());
        var clear = method(shard, callbacks.getLast().getName(), callbacks.getLast().getDesc());
        assertEquals(List.of("getModelViewStack", "pushMatrix", "scale", "applyModelViewMatrix"), callNames(setup));
        assertEquals(List.of("getModelViewStack", "popMatrix", "applyModelViewMatrix"), callNames(clear));
        assertEquals(List.of(.99975586f, .99975586f, .99975586f), code(setup).stream()
            .filter(LdcInsnNode.class::isInstance).map(LdcInsnNode.class::cast).map(insn -> insn.cst).toList());
        assertFalse(code(setup).stream().anyMatch(JumpInsnNode.class::isInstance), "Not a camera-conditional layer");
    }

    @Test void adapterUsesOneStableKeyAndLeavesNonChunkLayersUnmodified() throws IOException {
        var type = read(LOCAL);
        assertEquals(1, type.fields.stream().filter(field -> field.desc.equals("L" + RENDER + ";")
            && (field.access & (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) == (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)).count());
        assertEquals(List.of("create"), callNames(method(type, "<clinit>", "()V")));
        var adapter = read(ADAPTER);
        var layer = method(adapter, "layer", "(L" + RENDER + ";)L" + RENDER + ";");
        assertEquals(List.of(Opcodes.ALOAD, Opcodes.INVOKESTATIC, Opcodes.IFEQ, Opcodes.GETSTATIC,
            Opcodes.GOTO, Opcodes.ALOAD, Opcodes.ARETURN), code(layer).stream().map(AbstractInsnNode::getOpcode).toList());
        var singleton = code(layer).stream().filter(FieldInsnNode.class::isInstance).map(FieldInsnNode.class::cast).findFirst().orElseThrow();
        assertEquals(LOCAL, singleton.owner);
        assertEquals("TYPE", singleton.name);
        assertEquals(0, ((VarInsnNode) code(layer).get(5)).var, "Non-chunk branch returns original argument");
        for (var name : List.of("getEarlyBuffer", "getBuffer", "getLateBuffer", "draw")) {
            var entry = adapter.methods.stream().filter(method -> method.name.equals(name) && method.desc.startsWith("(L" + RENDER + ";)"))
                .findFirst().orElseThrow();
            assertEquals(1, calls(entry).stream().filter(call -> call.owner.equals(ADAPTER) && call.name.equals("layer")).count());
        }
        assertEquals(List.of("draw"), callNames(method(adapter, "draw", "()V")), "No new eager phase flushes");
        var selection = adapter.methods.stream().filter(method -> method.name.equals("forGhost")).findFirst().orElseThrow();
        assertEquals(1, calls(selection).stream().filter(call -> call.name.equals("activeOffhand")).count());
        assertTrue(code(selection).stream().anyMatch(JumpInsnNode.class::isInstance), "Adapter remains selection-gated");
        assertFalse(Stream.concat(adapter.methods.stream(), type.methods.stream()).flatMap(method -> calls(method).stream())
            .anyMatch(call -> call.owner.contains("RenderSystem")), "Submission must not switch global render state");
    }

    @Test void pinnedCatnipForwardsTheSameTypeThroughEveryPhaseAndFlush() throws IOException {
        var catnip = read("net/createmod/catnip/render/DefaultSuperRenderTypeBuffer");
        String bufferSource = "net/minecraft/client/renderer/MultiBufferSource$BufferSource";
        var phases = Map.of("getEarlyBuffer", "earlyBuffer", "getBuffer", "defaultBuffer", "getLateBuffer", "lateBuffer");
        for (var name : List.of("getEarlyBuffer", "getBuffer", "getLateBuffer")) {
            var entry = catnip.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow();
            var calls = calls(entry);
            assertEquals(1, calls.size());
            assertEquals(bufferSource, calls.getFirst().owner);
            assertEquals("getBuffer", calls.getFirst().name);
            var instructions = code(entry);
            assertEquals(List.of(phases.get(name)), phaseReceivers(entry, catnip.name));
            assertEquals(1, ((VarInsnNode) instructions.get(instructions.indexOf(calls.getFirst()) - 1)).var);
        }
        for (var descriptor : List.of("()V", "(L" + RENDER + ";)V")) {
            var flush = method(catnip, "draw", descriptor);
            assertEquals(List.of("endBatch", "endBatch", "endBatch"), callNames(flush));
            assertTrue(calls(flush).stream().allMatch(call -> call.owner.equals(bufferSource) && call.desc.equals(descriptor)));
            assertEquals(List.of("earlyBuffer", "defaultBuffer", "lateBuffer"), phaseReceivers(flush, catnip.name));
            if (!descriptor.equals("()V")) {
                for (var call : calls(flush)) {
                    assertEquals(1, ((VarInsnNode) code(flush).get(code(flush).indexOf(call) - 1)).var);
                }
            }
        }
        var draw = method(read(RENDER), "draw", "(Lcom/mojang/blaze3d/vertex/MeshData;)V");
        var names = callNames(draw);
        assertTrue(names.indexOf("setupRenderState") >= 0
            && names.indexOf("drawWithShader") > names.indexOf("setupRenderState")
            && names.indexOf("clearRenderState") > names.indexOf("drawWithShader"));
    }

    private ClassNode read(String name) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, name);
            var node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private MethodNode method(ClassNode type, String name, String descriptor) {
        return type.methods.stream().filter(method -> method.name.equals(name) && method.desc.equals(descriptor)).findFirst().orElseThrow();
    }

    private List<AbstractInsnNode> code(MethodNode method) {
        return Arrays.stream(method.instructions.toArray()).filter(insn -> insn.getOpcode() >= 0).toList();
    }

    private List<MethodInsnNode> calls(MethodNode method) {
        return code(method).stream().filter(MethodInsnNode.class::isInstance).map(MethodInsnNode.class::cast).toList();
    }

    private List<String> callNames(MethodNode method) {
        return calls(method).stream().map(call -> call.name).toList();
    }

    private List<String> shards(MethodNode method) {
        return code(method).stream().filter(FieldInsnNode.class::isInstance).map(FieldInsnNode.class::cast)
            .filter(field -> field.getOpcode() == Opcodes.GETSTATIC && field.desc.startsWith("L" + SHARD + "$"))
            .map(field -> field.name).toList();
    }

    private List<String> phaseReceivers(MethodNode method, String owner) {
        return code(method).stream().filter(FieldInsnNode.class::isInstance).map(FieldInsnNode.class::cast)
            .filter(field -> field.getOpcode() == Opcodes.GETFIELD && field.owner.equals(owner))
            .map(field -> field.name).toList();
    }
}
