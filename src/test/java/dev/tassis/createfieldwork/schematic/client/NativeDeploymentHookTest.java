package dev.tassis.createfieldwork.schematic.client;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Reads the pinned dependency without initializing Minecraft client classes. */
class NativeDeploymentHookTest {
    @Test void nativeInputHasExactlyOneToolActionInvocation() throws IOException {
        var calls = new ArrayList<String>();
        visit("SchematicHandler", "onMouseInput", "(IZ)Z", calls);
        assertEquals(1, calls.stream().filter(call -> call.equals(
            "com/simibubi/create/content/schematics/client/tools/ISchematicTool.handleRightClick()Z")).count());
    }

    @Test void nativePositionWritesOnlyDeployedAndAnchorBeforeMovingAndMarkingDirty() throws IOException {
        var fields = new ArrayList<String>();
        var calls = new ArrayList<String>();
        try (var source = getClass().getClassLoader().getResourceAsStream(
            "com/simibubi/create/content/schematics/client/tools/DeployTool.class")) {
            assertNotNull(source);
            new ClassReader(source).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (!name.equals("handleRightClick") || !descriptor.equals("()Z")) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                            if (owner.equals("com/simibubi/create/AllDataComponents")) fields.add(name);
                        }
                        @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            calls.add(owner + "." + name + descriptor);
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertEquals(List.of("SCHEMATIC_DEPLOYED", "SCHEMATIC_ANCHOR"), fields);
        assertEquals(2, calls.stream().filter(call -> call.equals(
            "net/minecraft/world/item/ItemStack.set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;")).count());
        String set = "net/minecraft/world/item/ItemStack.set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;";
        String transform = "com/simibubi/create/content/schematics/client/SchematicTransformation.";
        int start = calls.indexOf(transform + "startAt(Lnet/minecraft/core/BlockPos;)V");
        int move = calls.indexOf(transform + "moveTo(Lnet/minecraft/core/BlockPos;)V");
        int dirty = calls.indexOf("com/simibubi/create/content/schematics/client/SchematicHandler.markDirty()V");
        int deploy = calls.indexOf("com/simibubi/create/content/schematics/client/SchematicHandler.deploy()V");
        assertTrue(calls.indexOf(set) >= 0 && calls.lastIndexOf(set) < start
            && move > start && dirty > move && deploy > dirty);
    }

    @Test void nativeMoveToStoresTheUnconvertedLogicalTarget() throws IOException {
        var writes = new ArrayList<String>();
        try (var source = getClass().getClassLoader().getResourceAsStream(
            "com/simibubi/create/content/schematics/client/SchematicTransformation.class")) {
            assertNotNull(source);
            new ClassReader(source).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (!name.equals("moveTo") || !descriptor.equals("(Lnet/minecraft/core/BlockPos;)V")) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                            if (opcode == Opcodes.PUTFIELD) writes.add(name + descriptor);
                        }
                        @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            fail("Pinned raw moveTo must not convert the Position target");
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertEquals(List.of("targetLnet/minecraft/core/BlockPos;"), writes);
    }

    private void visit(String type, String method, String descriptor, List<String> calls) throws IOException {
        try (var source = getClass().getClassLoader().getResourceAsStream(
            "com/simibubi/create/content/schematics/client/" + type + ".class")) {
            assertNotNull(source);
            new ClassReader(source).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    if (!name.equals(method) || !desc.equals(descriptor)) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
                            calls.add(owner + "." + name + desc);
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
    }
}
