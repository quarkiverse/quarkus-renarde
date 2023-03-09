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
                && descriptor.startsWith("()Lio/quarkiverse/renarde/router/Method")
                && bootstrapMethodArguments.length > 2) {
            Handle targetDescriptor = (Handle) bootstrapMethodArguments[1];
            targetIndyDescriptor = targetDescriptor;
            return;
        }
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (opcode == Opcodes.INVOKESTATIC) {
            if (owner.equals(ControllerVisitor.ROUTER_BINARY_NAME)
                    && (name.equals("getURI") || name.equals("getAbsoluteURI"))) {
                // add the extra abolute boolean param
                if (name.equals("getURI")) {
                    super.visitInsn(Opcodes.ICONST_0);
                } else {
                    super.visitInsn(Opcodes.ICONST_1);
                }
                // At this point, we dropped the method handle (first param) and have the varargs on the heap, followed
                // by the first boolean parameter, so we need to swap them around
                super.visitInsn(Opcodes.SWAP);
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
