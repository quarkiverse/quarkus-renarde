package io.quarkiverse.renarde.deployment;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.router.Router;
import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.runtime.util.HashUtil;

public class ControllerVisitor implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public static final DotName DOTNAME_LONG = DotName.createSimple(Long.class.getName());

    public static final String ROUTER_BINARY_NAME = Router.class.getName().replace('.', '/');
    public static final String CONTROLLER_BINARY_NAME = Controller.class.getName().replace('.', '/');
    public static final String CONTROLLER_DESCRIPTOR = "L" + CONTROLLER_BINARY_NAME + ";";

    static abstract class UriPart {
    }

    static class StaticUriPart extends UriPart {
        public final String part;

        public StaticUriPart(String part) {
            this.part = part;
        }
    }

    static class PathParamUriPart extends UriPart {
        public final String name;
        public final int asmParamIndex;
        public final int paramIndex;
        public final boolean declared;

        public PathParamUriPart(String name, int paramIndex, int asmParamIndex, boolean declared) {
            this.name = name;
            this.asmParamIndex = asmParamIndex;
            this.paramIndex = paramIndex;
            this.declared = declared;
        }
    }

    static class QueryParamUriPart extends UriPart {
        public final String name;
        public final int asmParamIndex;
        public final int paramIndex;

        public QueryParamUriPart(String name, int paramIndex, int asmParamIndex) {
            this.name = name;
            this.paramIndex = paramIndex;
            this.asmParamIndex = asmParamIndex;
        }
    }

    static class ControllerMethod {
        public final String name;
        public final String descriptor;
        public final List<UriPart> parts;
        public final List<Type> parameters;

        public ControllerMethod(String name, String descriptor, List<UriPart> parts, List<Type> parameters) {
            this.name = name;
            this.descriptor = descriptor;
            this.parts = parts;
            this.parameters = parameters;
        }
    }

    static class ControllerClass {
        public final String className;
        public final Map<String, ControllerMethod> methods;

        public ControllerClass(String className, Map<String, ControllerMethod> methods) {
            this.className = className;
            this.methods = methods;
        }
    }

    Map<String, ControllerClass> controllers;

    public ControllerVisitor(Map<String, ControllerClass> controllers) {
        this.controllers = controllers;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor visitor) {
        return new ControllerClassVisitor(controllers.get(className), controllers, visitor);
    }

    public static class ControllerClassVisitor extends ClassVisitor {

        private String className;
        private ControllerClass controller;
        private Map<String, ControllerClass> controllers;

        public ControllerClassVisitor(ControllerClass controller, Map<String, ControllerClass> controllers,
                ClassVisitor classVisitor) {
            super(Opcodes.ASM9, classVisitor);
            this.controller = controller;
            this.controllers = controllers;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new RouterMethodVisitor(Opcodes.ASM9, visitor) {

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    boolean cleanupTarget = false;
                    if (opcode == Opcodes.INVOKEVIRTUAL) {
                        String ownerClass = owner.replace('/', '.');
                        ControllerClass ownerController = controllers.get(ownerClass);
                        String key = name + "/" + descriptor;
                        if (ownerController != null && ownerController.methods.get(key) != null) {
                            /*
                             * We turn this.method(…) calls into (static) __redirect$method(…) calls
                             */
                            name = "__redirect$" + name;
                            // turn it into a static call and ignore the target until after the call
                            opcode = Opcodes.INVOKESTATIC;
                            cleanupTarget = true;
                        } else if (owner.equals(className)
                                && name.equals("redirect")
                                && descriptor.equals("(Ljava/lang/Class;)" + CONTROLLER_DESCRIPTOR)) {
                            /*
                             * We replace this.redirect(Class) calls with null on the stack
                             */
                            super.visitInsn(Opcodes.POP); // get rid of Class param
                            super.visitInsn(Opcodes.POP); // get rid of this target
                            super.visitInsn(Opcodes.ACONST_NULL); // insert null target for next static call
                            // do not call the method
                            return;
                        }
                    }
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    if (cleanupTarget) {
                        /*
                         * If we've replaced a virtual call with a static call, we need to remove the extraneous target
                         * instance from the stack, which is sitting behind the call's return value (if any)
                         */
                        if (!descriptor.endsWith(")V")) {
                            // we have a return value to get around
                            super.visitInsn(Opcodes.SWAP); // [null, ret] -> [ret, null]
                        }
                        super.visitInsn(Opcodes.POP); // get rid of null target
                    }
                }
            };
        }

        @Override
        public void visitEnd() {
            for (ControllerMethod method : controller.methods.values()) {
                makeUriMethod(method);
                makeUriVarargsMethod(method);
                makeRedirectMethod(method);
            }
            super.visitEnd();
        }

        private void makeUriVarargsMethod(ControllerMethod method) {
            // add descriptor hash to method name since signature doesn't include parameters
            MethodVisitor visitor = super.visitMethod(
                    Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_VARARGS,
                    uriVarargsName(method.name, method.descriptor), "(Z[Ljava/lang/Object;)Ljava/net/URI;", null, null);

            visitor.visitVarInsn(Opcodes.ILOAD, 0);

            // FIXME: ignore non-path/query params
            int index = 0;
            for (Type parameterType : method.parameters) {
                // varargs.length > paramIndex ? (cast)varargs[paramIndex] : null-value
                // load the varargs
                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                // check the varargs length
                visitor.visitInsn(Opcodes.ARRAYLENGTH);
                Label end = new Label();
                Label elseBranch = new Label();
                // if we don't have enough varargs, jump to else
                visitor.visitIntInsn(Opcodes.BIPUSH, index);
                visitor.visitJumpInsn(Opcodes.IF_ICMPLE, elseBranch);
                // load the varargs value
                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                visitor.visitIntInsn(Opcodes.BIPUSH, index);
                visitor.visitInsn(Opcodes.AALOAD);
                if (parameterType.kind() == Kind.PRIMITIVE) {
                    unboxOrWidenIfRequired(visitor, parameterType);
                } else {
                    visitor.visitTypeInsn(Opcodes.CHECKCAST, parameterType.name().toString('/'));
                }

                visitor.visitJumpInsn(Opcodes.GOTO, end);
                visitor.visitLabel(elseBranch);
                // default value
                if (parameterType.kind() == Kind.PRIMITIVE) {
                    switch (parameterType.asPrimitiveType().primitive()) {
                        case BOOLEAN:
                        case BYTE:
                        case SHORT:
                        case INT:
                        case CHAR:
                            visitor.visitInsn(Opcodes.ICONST_0);
                            break;
                        case DOUBLE:
                            visitor.visitInsn(Opcodes.DCONST_0);
                            break;
                        case FLOAT:
                            visitor.visitInsn(Opcodes.FCONST_0);
                            break;
                        case LONG:
                            visitor.visitInsn(Opcodes.LCONST_0);
                            break;
                    }
                } else {
                    visitor.visitInsn(Opcodes.ACONST_NULL);
                }
                visitor.visitLabel(end);
                // this is a varargs index, not a parameter index
                index++;
            }

            visitor.visitMethodInsn(Opcodes.INVOKESTATIC, className, "__uri$" + method.name, uriDescriptor(method), false);
            visitor.visitInsn(Opcodes.ARETURN);
            visitor.visitMaxs(0, 0);
            visitor.visitEnd();
        }

        public static void unboxOrWidenIfRequired(MethodVisitor mv, Type jandexType) {
            if (jandexType.kind() == Kind.PRIMITIVE) {
                switch (jandexType.asPrimitiveType().primitive()) {
                    case BOOLEAN:
                        unbox(mv, "java/lang/Boolean", "booleanValue", "Z");
                        break;
                    case BYTE:
                        unbox(mv, "java/lang/Number", "byteValue", "B");
                        break;
                    case CHAR:
                        unbox(mv, "java/lang/Character", "charValue", "C");
                        break;
                    case DOUBLE:
                        unbox(mv, "java/lang/Number", "doubleValue", "D");
                        break;
                    case FLOAT:
                        unbox(mv, "java/lang/Number", "floatValue", "F");
                        break;
                    case INT:
                        unbox(mv, "java/lang/Number", "intValue", "I");
                        break;
                    case LONG:
                        unbox(mv, "java/lang/Number", "longValue", "J");
                        break;
                    case SHORT:
                        unbox(mv, "java/lang/Number", "shortValue", "S");
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown primitive type: " + jandexType);
                }
            }
        }

        private static void unbox(MethodVisitor mv, String owner, String methodName, String returnTypeSignature) {
            mv.visitTypeInsn(Opcodes.CHECKCAST, owner);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, methodName, "()" + returnTypeSignature, false);
        }

        static String uriVarargsName(String name, String descriptor) {
            return "__urivarargs$" + name + "$" + HashUtil.sha1(descriptor);
        }

        private void makeRedirectMethod(ControllerMethod method) {
            // same signature makes it easier for callers
            MethodVisitor visitor = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "__redirect$" + method.name, method.descriptor, null, null);
            // redirect(uri(false, param1, param2))
            visitor.visitInsn(Opcodes.ICONST_0);
            int index = 0;
            for (Type parameterType : method.parameters) {
                visitor.visitVarInsn(AsmUtil.getLoadOpcode(parameterType), index);
                index += AsmUtil.getParameterSize(parameterType);
            }
            visitor.visitMethodInsn(Opcodes.INVOKESTATIC, className, "__uri$" + method.name, uriDescriptor(method), false);
            visitor.visitMethodInsn(Opcodes.INVOKESTATIC, CONTROLLER_BINARY_NAME, "seeOther",
                    "(Ljava/net/URI;)Ljavax/ws/rs/core/Response;", false);
            visitor.visitInsn(Opcodes.POP);

            int lastParen = method.descriptor.lastIndexOf(')');
            String returnDescriptor = method.descriptor.substring(lastParen + 1);
            int returnInstruction = AsmUtil.getReturnInstruction(returnDescriptor);
            switch (returnInstruction) {
                case Opcodes.RETURN:
                    break;
                case Opcodes.ARETURN:
                    visitor.visitInsn(Opcodes.ACONST_NULL);
                    break;
                case Opcodes.DRETURN:
                    visitor.visitInsn(Opcodes.DCONST_0);
                    break;
                case Opcodes.FRETURN:
                    visitor.visitInsn(Opcodes.FCONST_0);
                    break;
                case Opcodes.IRETURN:
                    visitor.visitInsn(Opcodes.ICONST_0);
                    break;
                case Opcodes.LRETURN:
                    visitor.visitInsn(Opcodes.LCONST_0);
                    break;
            }
            visitor.visitInsn(returnInstruction);
            visitor.visitMaxs(0, 0);
            visitor.visitEnd();
        }

        private String uriDescriptor(ControllerMethod method) {
            // same signature but takes extra boolean and returns a String
            int lastParen = method.descriptor.lastIndexOf(')');
            return "(Z" + method.descriptor.substring(1, lastParen + 1) + "Ljava/net/URI;";
        }

        private void makeUriMethod(ControllerMethod method) {
            String descriptor = uriDescriptor(method);
            // Figure out the uriBuilder var index
            int uriBuilderIndex = 1;
            for (Type parameterType : method.parameters) {
                uriBuilderIndex += AsmUtil.getParameterSize(parameterType);
            }
            MethodVisitor visitor = super.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "__uri$" + method.name, descriptor, null, null);
            // UriBuilder uri = Router.getUriBuilder(absolute)
            visitor.visitVarInsn(Opcodes.ILOAD, 0);
            visitor.visitMethodInsn(Opcodes.INVOKESTATIC, ROUTER_BINARY_NAME, "getUriBuilder",
                    "(Z)Ljavax/ws/rs/core/UriBuilder;", false);
            visitor.visitVarInsn(Opcodes.ASTORE, uriBuilderIndex);
            for (UriPart part : method.parts) {
                if (part instanceof StaticUriPart) {
                    // uri.path("Users");
                    visitor.visitVarInsn(Opcodes.ALOAD, uriBuilderIndex);
                    visitor.visitLdcInsn(((StaticUriPart) part).part);
                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "javax/ws/rs/core/UriBuilder", "path",
                            "(Ljava/lang/String;)Ljavax/ws/rs/core/UriBuilder;", false);
                    visitor.visitInsn(Opcodes.POP);
                } else if (part instanceof PathParamUriPart) {
                    PathParamUriPart pathPart = (PathParamUriPart) part;
                    if (!pathPart.declared) {
                        // uri.path("{param}");
                        visitor.visitVarInsn(Opcodes.ALOAD, uriBuilderIndex);
                        visitor.visitLdcInsn("{" + pathPart.name + "}");
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "javax/ws/rs/core/UriBuilder", "path",
                                "(Ljava/lang/String;)Ljavax/ws/rs/core/UriBuilder;", false);
                        visitor.visitInsn(Opcodes.POP);
                    }
                    // uri.resolveTemplate("userName", userName);
                    visitor.visitVarInsn(Opcodes.ALOAD, uriBuilderIndex);
                    visitor.visitLdcInsn(pathPart.name);
                    Type paramType = method.parameters.get(pathPart.paramIndex);
                    visitor.visitVarInsn(AsmUtil.getLoadOpcode(paramType), pathPart.asmParamIndex);
                    AsmUtil.boxIfRequired(visitor, paramType);
                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "javax/ws/rs/core/UriBuilder", "resolveTemplate",
                            "(Ljava/lang/String;Ljava/lang/Object;)Ljavax/ws/rs/core/UriBuilder;", false);
                    visitor.visitInsn(Opcodes.POP);
                } else if (part instanceof QueryParamUriPart) {
                    /*
                     * if(queryParam != null){
                     * Object[] params = new Object[1];
                     * params[0] = queryParam;
                     * uri.queryParam("queryParam", params);
                     * }
                     */
                    QueryParamUriPart queryPart = (QueryParamUriPart) part;
                    Type paramType = method.parameters.get(queryPart.paramIndex);
                    Label end = new Label();
                    if (paramType.kind() != Kind.PRIMITIVE) {
                        visitor.visitVarInsn(Opcodes.ALOAD, queryPart.asmParamIndex);
                        visitor.visitJumpInsn(Opcodes.IFNULL, end);
                    }
                    visitor.visitVarInsn(Opcodes.ALOAD, uriBuilderIndex);
                    visitor.visitLdcInsn(queryPart.name);
                    visitor.visitInsn(Opcodes.ICONST_1);
                    visitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                    visitor.visitInsn(Opcodes.DUP);
                    visitor.visitInsn(Opcodes.ICONST_0);
                    visitor.visitVarInsn(AsmUtil.getLoadOpcode(paramType), queryPart.asmParamIndex);
                    AsmUtil.boxIfRequired(visitor, paramType);
                    visitor.visitInsn(Opcodes.AASTORE);
                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "javax/ws/rs/core/UriBuilder", "queryParam",
                            "(Ljava/lang/String;[Ljava/lang/Object;)Ljavax/ws/rs/core/UriBuilder;", false);
                    visitor.visitInsn(Opcodes.POP);
                    if (paramType.kind() != Kind.PRIMITIVE) {
                        visitor.visitLabel(end);
                    }
                }
            }
            /*
             * return uri.build();
             */
            visitor.visitVarInsn(Opcodes.ALOAD, uriBuilderIndex);
            visitor.visitInsn(Opcodes.ICONST_0);
            visitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "javax/ws/rs/core/UriBuilder", "build",
                    "([Ljava/lang/Object;)Ljava/net/URI;", false);
            visitor.visitInsn(Opcodes.ARETURN);
            visitor.visitMaxs(0, 0);
            visitor.visitEnd();
        }
    }
}
