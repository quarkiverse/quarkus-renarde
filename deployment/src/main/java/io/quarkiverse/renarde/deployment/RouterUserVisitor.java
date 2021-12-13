package io.quarkiverse.renarde.deployment;

import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class RouterUserVisitor implements BiFunction<String, ClassVisitor, ClassVisitor> {

    @Override
    public ClassVisitor apply(String className, ClassVisitor visitor) {
        return new RouterUserClassVisitor(visitor);
    }

    public static class RouterUserClassVisitor extends ClassVisitor {

        public RouterUserClassVisitor(ClassVisitor classVisitor) {
            super(Opcodes.ASM9, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new RouterMethodVisitor(Opcodes.ASM9, visitor);
        }
    }
}
