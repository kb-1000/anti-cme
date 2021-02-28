package de.kb1000.anticme;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ConcurrentModificationException;

public class AntiCMEClassTransformer implements ClassFileTransformer {

    private static final String STACKTRACE_FIELD = "$anticme$stacktrace";

    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain, final byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className.startsWith("java/util/HashMap")) {
            final ClassReader classReader = new ClassReader(classfileBuffer);
            final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            final ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);
            if (className.equals("java/util/HashMap") && classNode.fields.stream().noneMatch(field -> field.name.equals(STACKTRACE_FIELD))) {
                classNode.fields.add(new FieldNode(Opcodes.ACC_TRANSIENT | Opcodes.ACC_SYNTHETIC, STACKTRACE_FIELD, Type.getDescriptor(Throwable.class), null, null));
            }
            if (!className.equals("java/util/HashMap") && classNode.fields.stream().noneMatch(field -> field.name.equals("this$0") && (field.access & Opcodes.ACC_SYNTHETIC) != 0 && (field.access & Opcodes.ACC_STATIC) == 0)) {
                return classfileBuffer;
            }
            classNode.accept(new ClassVisitor(Opcodes.ASM9, classWriter) {
                @Override
                public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
                    final MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
                    if (methodVisitor != null && (access & Opcodes.ACC_STATIC) == 0) {
                        return new MethodVisitor(Opcodes.ASM9, methodVisitor) {
                            @Override
                            public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
                                if (name.equals("modCount") && opcode == Opcodes.PUTFIELD) {
                                    super.visitInsn(Opcodes.DUP2);
                                    super.visitInsn(Opcodes.POP);
                                    super.visitTypeInsn(Opcodes.NEW, Type.getInternalName(Throwable.class));
                                    super.visitInsn(Opcodes.DUP);
                                    super.visitLdcInsn("Collection was modified here");
                                    super.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Throwable.class), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)), false);
                                    super.visitFieldInsn(Opcodes.PUTFIELD, "java/util/HashMap", STACKTRACE_FIELD, Type.getDescriptor(Throwable.class));
                                }
                                super.visitFieldInsn(opcode, owner, name, desc);
                            }

                            @Override
                            public void visitMethodInsn(final int opcode, final String owner, final String name, String desc, final boolean itf) {
                                if (opcode == Opcodes.INVOKESPECIAL && owner.equals(Type.getInternalName(ConcurrentModificationException.class)) && name.equals("<init>") && desc.equals(Type.getMethodDescriptor(Type.VOID_TYPE))) {
                                    if (className.equals("java/util/HashMap")) {
                                        super.visitVarInsn(Opcodes.ALOAD, 0);
                                    } else {
                                        super.visitVarInsn(Opcodes.ALOAD, 0);
                                        super.visitFieldInsn(Opcodes.GETFIELD, className, "this$0", "Ljava/util/HashMap;");
                                    }
                                    super.visitFieldInsn(Opcodes.GETFIELD, "java/util/HashMap", STACKTRACE_FIELD, Type.getDescriptor(Throwable.class));
                                    desc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Throwable.class));
                                } else if (opcode == Opcodes.INVOKESPECIAL && owner.equals(Type.getInternalName(ConcurrentModificationException.class)) && name.equals("<init>") && desc.equals(Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)))) {
                                    super.visitFieldInsn(Opcodes.GETFIELD, "java/util/HashMap", STACKTRACE_FIELD, Type.getDescriptor(Throwable.class));
                                    desc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(Throwable.class));
                                }
                                super.visitMethodInsn(opcode, owner, name, desc, itf);
                            }
                        };
                    }
                    return methodVisitor;
                }
            });
            return classWriter.toByteArray();
        }
        return classfileBuffer;
    }
}
