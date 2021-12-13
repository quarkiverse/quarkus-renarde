package io.quarkiverse.renarde.deployment;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class RouterMethodVisitor extends MethodVisitor {
    Handle targetIndyDescriptor;

    public RouterMethodVisitor(int api, MethodVisitor methodVisitor) {
        super(api, methodVisitor);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
            Object... bootstrapMethodArguments) {
        if (name.equals("method")
                && descriptor.startsWith("()Lio/quarkus/vixen/router/Method")
                && bootstrapMethodArguments.length > 2) {
            Handle targetDescriptor = (Handle) bootstrapMethodArguments[1];
            // FIXME: extract to all classes using Router, not just controllers
            targetIndyDescriptor = targetDescriptor;
            // replace by the first boolean param to the uri method: false
            super.visitInsn(Opcodes.ICONST_0);
            return;
        }
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (opcode == Opcodes.INVOKESTATIC) {
            if (owner.equals("io/quarkus/vixen/router/Router") && name.equals("getURI")) {
                // replace by a call to the uri/varargs method
                owner = targetIndyDescriptor.getOwner();
                name = ControllerVisitor.ControllerClassVisitor.uriVarargsName(targetIndyDescriptor.getName(),
                        targetIndyDescriptor.getDesc());
                descriptor = "(Z[Ljava/lang/Object;)Ljava/net/URI;";
            }
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}
