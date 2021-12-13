package io.quarkiverse.renarde.deployment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;
import javax.transaction.Transactional;
import javax.ws.rs.Priorities;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationsTransformer;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationsTransformer.TransformationContext;
import org.jboss.resteasy.reactive.common.processor.transformation.Transformation;
import org.jboss.resteasy.reactive.common.util.URLUtils;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.deployment.ControllerVisitor.ControllerClass;
import io.quarkiverse.renarde.deployment.ControllerVisitor.ControllerMethod;
import io.quarkiverse.renarde.deployment.ControllerVisitor.UriPart;
import io.quarkiverse.renarde.router.Router;
import io.quarkiverse.renarde.router.RouterMethod;
import io.quarkiverse.renarde.util.AuthenticationFailedExceptionMapper;
import io.quarkiverse.renarde.util.CRSF;
import io.quarkiverse.renarde.util.Filters;
import io.quarkiverse.renarde.util.Flash;
import io.quarkiverse.renarde.util.JavaExtensions;
import io.quarkiverse.renarde.util.MyParamConverters;
import io.quarkiverse.renarde.util.MyValidationInterceptor;
import io.quarkiverse.renarde.util.QuteResolvers;
import io.quarkiverse.renarde.util.RedirectExceptionMapper;
import io.quarkiverse.renarde.util.RenderArgs;
import io.quarkiverse.renarde.util.Validation;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ExcludedTypeBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveEndPointValidationInterceptor;
import io.quarkus.qute.Variant;
import io.quarkus.qute.deployment.TemplateTagBuildItem;
import io.quarkus.resteasy.reactive.server.spi.AnnotationsTransformerBuildItem;
import io.quarkus.resteasy.reactive.spi.AdditionalResourceClassBuildItem;
import io.quarkus.resteasy.reactive.spi.ParamConverterBuildItem;
import io.quarkus.runtime.StartupEvent;

public class RenardeProcessor {

    private static final Logger logger = Logger.getLogger(RenardeProcessor.class);

    public static final DotName DOTNAME_CONTROLLER = DotName.createSimple(Controller.class.getName());
    public static final DotName DOTNAME_ROUTER = DotName.createSimple(Router.class.getName());
    public static final DotName DOTNAME_UNREMOVABLE = DotName.createSimple(Unremovable.class.getName());
    public static final DotName DOTNAME_TRANSACTIONAL = DotName.createSimple(Transactional.class.getName());

    private static final String FEATURE = "renarde";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void removeHibernateLogging(LaunchModeBuildItem launchMode,
            BuildProducer<LogCleanupFilterBuildItem> logFilters) {
        if (launchMode.getLaunchMode().isDevOrTest()) {
            // FIXME: this is too broad, but waits for https://github.com/quarkusio/quarkus/issues/16204 to be fixed
            logFilters
                    .produce(new LogCleanupFilterBuildItem("org.hibernate.engine.jdbc.spi.SqlExceptionHelper",
                            "SQL Warning Code: 0, SQLState: 00000",
                            "relation \"",
                            "table \"",
                            "sequence \""));
        }
    }

    @BuildStep
    void setupJWT(LaunchModeBuildItem launchMode, Capabilities capabilities,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runtimeConfigurationBuildItem)
            throws IOException, NoSuchAlgorithmException {
        if (launchMode.getLaunchMode().isDevOrTest()
                && capabilities.isPresent(Capability.JWT)) {
            // make sure we have minimal config
            final Config config = ConfigProvider.getConfig();

            // PRIVATE
            Optional<String> decryptKeyLocationOpt = config.getOptionalValue("mp.jwt.decrypt.key.location", String.class);
            Optional<String> signKeyLocationOpt = config.getOptionalValue("smallrye.jwt.sign.key.location", String.class);
            // PUBLIC
            Optional<String> verifyKeyLocationOpt = config.getOptionalValue("mp.jwt.verify.publickey.location", String.class);
            Optional<String> encryptKeyLocationOpt = config.getOptionalValue("smallrye.jwt.encrypt.key.location", String.class);
            if (!decryptKeyLocationOpt.isPresent()
                    && !signKeyLocationOpt.isPresent()
                    && !verifyKeyLocationOpt.isPresent()
                    && !encryptKeyLocationOpt.isPresent()) {
                // FIXME: folder
                File privateKey = new File("target/classes/dev.privateKey.pem");
                File publicKey = new File("target/classes/dev.publicKey.pem");
                if (!privateKey.exists() && !publicKey.exists()) {
                    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                    kpg.initialize(2048);
                    KeyPair kp = kpg.generateKeyPair();

                    logger.infof("Generating private/public keys for DEV/TEST in %s and %s", privateKey, publicKey);
                    try (FileWriter fw = new FileWriter(privateKey)) {
                        fw.append("-----BEGIN PRIVATE KEY-----\n");
                        fw.append(Base64.getMimeEncoder().encodeToString(kp.getPrivate().getEncoded()));
                        fw.append("\n");
                        fw.append("-----END PRIVATE KEY-----\n");
                    }
                    try (FileWriter fw = new FileWriter(publicKey)) {
                        fw.append("-----BEGIN PUBLIC KEY-----\n");
                        fw.append(Base64.getMimeEncoder().encodeToString(kp.getPublic().getEncoded()));
                        fw.append("\n");
                        fw.append("-----END PUBLIC KEY-----\n");
                    }
                }
                runtimeConfigurationBuildItem
                        .produce(new RunTimeConfigurationDefaultBuildItem("mp.jwt.decrypt.key.location", privateKey.getName()));
                runtimeConfigurationBuildItem.produce(
                        new RunTimeConfigurationDefaultBuildItem("smallrye.jwt.sign.key.location", privateKey.getName()));
                runtimeConfigurationBuildItem.produce(
                        new RunTimeConfigurationDefaultBuildItem("mp.jwt.verify.publickey.location", publicKey.getName()));
                runtimeConfigurationBuildItem.produce(
                        new RunTimeConfigurationDefaultBuildItem("smallrye.jwt.encrypt.key.location", publicKey.getName()));
            }
        }
    }

    @BuildStep
    void produceBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItems,
            BuildProducer<ParamConverterBuildItem> paramConverterBuildItems,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItems) {
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(Filters.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(QuteResolvers.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(CRSF.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(Flash.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(RenderArgs.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(Validation.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(JavaExtensions.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(MyValidationInterceptor.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(AuthenticationFailedExceptionMapper.class));

        paramConverterBuildItems.produce(new ParamConverterBuildItem(MyParamConverters.class.getName(), Priorities.USER, true));

        additionalIndexedClassesBuildItems.produce(
                new AdditionalIndexedClassesBuildItem(Filters.class.getName(), RedirectExceptionMapper.class.getName()));

    }

    @BuildStep
    ExcludedTypeBuildItem removeOriginalValidatorInterceptor() {
        return new ExcludedTypeBuildItem(ResteasyReactiveEndPointValidationInterceptor.class.getName());
    }

    @BuildStep
    void produceTags(BuildProducer<TemplateTagBuildItem> tags) {
        tags.produce(addTag("authenticityToken"));
        tags.produce(addTag("error"));
        tags.produce(addTag("form"));
        tags.produce(addTag("gravatar"));
        tags.produce(addTag("ifError"));
    }

    private TemplateTagBuildItem addTag(String tagName) {
        URL resource = QuteResolvers.class.getClassLoader().getResource("/templates/tags/" + tagName + ".html");
        byte[] bytes;
        try {
            bytes = IoUtil.readBytes(resource.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new TemplateTagBuildItem(tagName, new String(bytes, StandardCharsets.UTF_8),
                Variant.forContentType(Variant.TEXT_HTML));
    }

    @BuildStep
    void collectControllers(ApplicationIndexBuildItem indexBuildItem,
            BuildProducer<AdditionalResourceClassBuildItem> additionalResourceClassBuildItems,
            BuildProducer<AnnotationsTransformerBuildItem> annotationTransformerBuildItems,
            BuildProducer<io.quarkus.arc.deployment.AnnotationsTransformerBuildItem> arcTransformers,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformers,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans) {
        Set<DotName> controllers = new HashSet<>();
        Map<String, ControllerVisitor.ControllerClass> methodsByClass = new HashMap<>();
        for (ClassInfo controllerInfo : indexBuildItem.getIndex().getAllKnownSubclasses(DOTNAME_CONTROLLER)) {
            // skip abstract classes
            if (Modifier.isAbstract(controllerInfo.flags()))
                continue;
            additionalResourceClassBuildItems.produce(new AdditionalResourceClassBuildItem(controllerInfo, ""));
            controllers.add(controllerInfo.name());
            unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(controllerInfo.name()));
            methodsByClass.put(controllerInfo.name().toString(), scanController(controllerInfo));
        }
        for (DotName controller : controllers) {
            bytecodeTransformers
                    .produce(new BytecodeTransformerBuildItem(controller.toString(), new ControllerVisitor(methodsByClass)));
        }
        for (ClassInfo routerUserInfo : indexBuildItem.getIndex().getKnownUsers(DOTNAME_ROUTER)) {
            if (!controllers.contains(routerUserInfo.name())) {
                bytecodeTransformers
                        .produce(new BytecodeTransformerBuildItem(routerUserInfo.name().toString(), new RouterUserVisitor()));
            }
        }
        generateRouterInit(generatedBeans, methodsByClass);
        annotationTransformerBuildItems.produce(new AnnotationsTransformerBuildItem(
                AnnotationsTransformer.builder().appliesTo(Kind.METHOD)
                        .transform(ti -> transformControllerMethod(ti, controllers))));
        annotationTransformerBuildItems.produce(new AnnotationsTransformerBuildItem(
                AnnotationsTransformer.builder().appliesTo(Kind.CLASS).transform(ti -> transformController(ti, controllers))));

        arcTransformers.produce(new io.quarkus.arc.deployment.AnnotationsTransformerBuildItem(
                new io.quarkus.arc.processor.AnnotationsTransformer() {
                    @Override
                    public void transform(TransformationContext transformationContext) {
                        if (transformationContext.isClass()
                                && controllers.contains(transformationContext.getTarget().asClass().name())) {
                            // FIXME: probably don't add a scope annotation if it has one already?
                            transformationContext.transform().add(ResteasyReactiveDotNames.REQUEST_SCOPED)
                                    .done();
                        }
                        if (transformationContext.isMethod()) {
                            MethodInfo method = transformationContext.getTarget().asMethod();
                            if (controllers.contains(method.declaringClass().name())
                                    && !method.hasAnnotation(DOTNAME_TRANSACTIONAL)
                                    && (method.hasAnnotation(ResteasyReactiveDotNames.POST)
                                            || method.hasAnnotation(ResteasyReactiveDotNames.PUT)
                                            || method.hasAnnotation(ResteasyReactiveDotNames.DELETE))) {
                                transformationContext.transform().add(DOTNAME_TRANSACTIONAL)
                                        .done();
                            }
                        }
                    }
                }));

    }

    private void generateRouterInit(BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            Map<String, ControllerClass> methodsByClass) {
        ClassOutput beansClassOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
        try (ClassCreator beanClassCreator = ClassCreator.builder().classOutput(beansClassOutput)
                .className("__RenardeInit")
                .build()) {
            beanClassCreator.addAnnotation(Singleton.class);

            try (MethodCreator methodCreator = beanClassCreator.getMethodCreator("init", void.class, StartupEvent.class)) {
                methodCreator.getParameterAnnotations(0).addAnnotation(Observes.class);
                for (ControllerClass controllerClass : methodsByClass.values()) {
                    String simpleControllerName = controllerClass.className;
                    int lastDot = simpleControllerName.lastIndexOf('.');
                    if (lastDot != -1) {
                        simpleControllerName = simpleControllerName.substring(lastDot + 1);
                    }
                    for (ControllerMethod method : controllerClass.methods.values()) {
                        FunctionCreator function = methodCreator.createFunction(RouterMethod.class);
                        String uriMethodName = ControllerVisitor.ControllerClassVisitor.uriVarargsName(method.name,
                                method.descriptor);
                        try (BytecodeCreator functionBytecode = function.getBytecode()) {
                            functionBytecode.returnValue(functionBytecode.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(controllerClass.className, uriMethodName,
                                            URI.class, boolean.class, Object[].class),
                                    functionBytecode.getMethodParam(0),
                                    functionBytecode.getMethodParam(1)));
                        }
                        methodCreator.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Router.class, "registerRoute", void.class, String.class,
                                        RouterMethod.class),
                                methodCreator.load(simpleControllerName + "." + method.name),
                                function.getInstance());
                    }
                }
                methodCreator.returnValue(null);
            }
        }
    }

    private ControllerVisitor.ControllerClass scanController(ClassInfo controllerInfo) {
        Map<String, ControllerMethod> methods = new HashMap<>();
        for (MethodInfo method : controllerInfo.methods()) {
            if (!isControllerMethod(method))
                continue;
            List<UriPart> parts = new ArrayList<>();

            AnnotationInstance classPath = method.declaringClass().classAnnotation(ResteasyReactiveDotNames.PATH);
            String className = method.declaringClass().simpleName();
            String classPathValue = classPath != null ? classPath.value().value().toString() : className;

            AnnotationInstance methodPath = method.annotation(ResteasyReactiveDotNames.PATH);
            String methodPathValue = methodPath != null ? methodPath.value().value().toString() : method.name();

            String path = classPathValue + (methodPathValue.startsWith("/") ? "" : "/") + methodPathValue;

            // path annotations
            if (!methodPathValue.startsWith("/")) {
                parts.add(new ControllerVisitor.StaticUriPart(classPathValue));
            }
            parts.add(new ControllerVisitor.StaticUriPart(methodPathValue));

            // collect declared path params
            Set<String> pathParameters = new HashSet<>();
            URLUtils.parsePathParameters(path, pathParameters);

            // collect param annotations
            Map<DotName, AnnotationInstance>[] parameterAnnotations = getParameterAnnotations(method);

            // look for undeclared path params
            for (int paramIndex = 0, asmParamIndex = 1; paramIndex < method.parameters().size(); ++paramIndex) {
                String paramName = method.parameterName(paramIndex);
                AnnotationInstance pathParam = parameterAnnotations[paramIndex].get(ResteasyReactiveDotNames.PATH_PARAM);
                if (pathParam != null) {
                    String name = (String) pathParam.value().value();
                    parts.add(new ControllerVisitor.PathParamUriPart(name, paramIndex, asmParamIndex,
                            pathParameters.contains(paramName)));
                }
                AnnotationInstance restPathParam = parameterAnnotations[paramIndex]
                        .get(ResteasyReactiveDotNames.REST_PATH_PARAM);
                if (restPathParam != null) {
                    String name = restPathParam.value() != null ? (String) restPathParam.value().value() : "";
                    if (name != null)
                        name = paramName;
                    parts.add(new ControllerVisitor.PathParamUriPart(name, paramIndex, asmParamIndex,
                            pathParameters.contains(paramName)));
                }
                if (pathParameters.contains(paramName)) {
                    parts.add(new ControllerVisitor.PathParamUriPart(paramName, paramIndex, asmParamIndex, true));
                }
                AnnotationInstance queryParam = parameterAnnotations[paramIndex].get(ResteasyReactiveDotNames.QUERY_PARAM);
                if (queryParam != null) {
                    String name = (String) queryParam.value().value();
                    parts.add(new ControllerVisitor.QueryParamUriPart(name, paramIndex, asmParamIndex));
                }
                AnnotationInstance restQueryParam = parameterAnnotations[paramIndex]
                        .get(ResteasyReactiveDotNames.REST_QUERY_PARAM);
                if (restQueryParam != null) {
                    String name = restQueryParam.value() != null ? (String) restQueryParam.value().value() : "";
                    if (name.isEmpty())
                        name = paramName;
                    parts.add(new ControllerVisitor.QueryParamUriPart(name, paramIndex, asmParamIndex));
                }
                asmParamIndex += AsmUtil.getParameterSize(method.parameters().get(paramIndex));
            }

            String descriptor = AsmUtil.getDescriptor(method, v -> v);
            String key = method.name() + "/" + descriptor;
            methods.put(key, new ControllerMethod(method.name(), descriptor, parts,
                    method.parameters()));
        }
        return new ControllerVisitor.ControllerClass(controllerInfo.name().toString(), methods);
    }

    private boolean isControllerMethod(MethodInfo method) {
        return !Modifier.isAbstract(method.flags())
                && Modifier.isPublic(method.flags())
                && !Modifier.isNative(method.flags())
                && !Modifier.isStatic(method.flags())
                && !method.name().equals("<init>")
                && !method.name().equals("<clinit>");
    }

    private void transformController(TransformationContext ti, Set<DotName> controllers) {
        ClassInfo klass = ti.getTarget().asClass();
        if (controllers.contains(klass.name())) {
            if (klass.classAnnotation(ResteasyReactiveDotNames.PATH) == null) {
                ti.transform().add(ResteasyReactiveDotNames.PATH, AnnotationValue.createStringValue("value", "")).done();
            }
        }
    }

    private void transformControllerMethod(TransformationContext ti, Set<DotName> controllers) {
        MethodInfo method = ti.getTarget().asMethod();
        if (!isControllerMethod(method)) {
            return;
        }
        if (controllers.contains(method.declaringClass().name())) {
            /*
             * @Path("foo") class Class { @Path("bar") method(); } -> no change
             * 
             * @Path("foo") class Class { method(); } -> @Path("foo") class Class { @Path("method") method(); }
             * class Class { @Path("/bar") method(); } -> @Path("") class Class { @Path("/bar") method(); }
             * class Class { @Path("bar") method(); } -> @Path("") class Class { @Path("Class/bar") method(); }
             * class Class { method(); } -> @Path("") class Class { @Path("Class/method") method(); }
             */
            // class @Path or class name first
            AnnotationInstance classPath = method.declaringClass().classAnnotation(ResteasyReactiveDotNames.PATH);
            String className = method.declaringClass().simpleName();
            String classPathValue = classPath != null ? classPath.value().value().toString() : className;

            AnnotationInstance methodPath = method.annotation(ResteasyReactiveDotNames.PATH);
            String methodPathValue = methodPath != null ? methodPath.value().value().toString() : method.name();

            String path = classPathValue + (methodPathValue.startsWith("/") ? "" : "/") + methodPathValue;

            String newMethodPathValue = methodPathValue;
            boolean setMethodPath = false;

            if (classPath != null && methodPath == null) {
                // can remain the method name
                setMethodPath = true;
            } else if (classPath == null) {
                if (methodPath == null || !methodPathValue.startsWith("/")) {
                    // prepend the class name
                    setMethodPath = true;
                    newMethodPathValue = className + "/" + methodPathValue;
                }
            }

            // collect declared path params
            Set<String> pathParameters = new HashSet<>();
            URLUtils.parsePathParameters(path, pathParameters);

            // collect param annotations
            Map<DotName, AnnotationInstance>[] parameterAnnotations = getParameterAnnotations(method);

            // look for undeclared path params
            for (int paramPos = 0; paramPos < method.parameters().size(); ++paramPos) {
                if ((parameterAnnotations[paramPos].get(ResteasyReactiveDotNames.PATH_PARAM) != null
                        || parameterAnnotations[paramPos].get(ResteasyReactiveDotNames.REST_PATH_PARAM) != null)
                        && !pathParameters.contains(method.parameterName(paramPos))) {
                    // add them to the method path
                    setMethodPath = true;
                    newMethodPathValue += "/{" + method.parameterName(paramPos) + "}";
                }
            }

            if (setMethodPath) {
                Transformation transform = ti.transform();
                if (methodPathValue != null)
                    transform.remove(ai -> ai.name().equals(ResteasyReactiveDotNames.PATH));
                transform.add(ResteasyReactiveDotNames.PATH, AnnotationValue.createStringValue("value", newMethodPathValue))
                        .done();
            }

            // FIXME: doesn't work for custom Http methods, whatever
            if (!method.hasAnnotation(ResteasyReactiveDotNames.GET)
                    && !method.hasAnnotation(ResteasyReactiveDotNames.PUT)
                    && !method.hasAnnotation(ResteasyReactiveDotNames.POST)
                    && !method.hasAnnotation(ResteasyReactiveDotNames.HEAD)
                    && !method.hasAnnotation(ResteasyReactiveDotNames.OPTIONS)
                    && !method.hasAnnotation(ResteasyReactiveDotNames.DELETE)) {
                ti.transform().add(ResteasyReactiveDotNames.GET).done();
            }
        }
    }

    private Map<DotName, AnnotationInstance>[] getParameterAnnotations(MethodInfo method) {
        // collect param annotations
        Map<DotName, AnnotationInstance>[] parameterAnnotations = new Map[method.parameters().size()];
        for (int paramPos = 0; paramPos < method.parameters().size(); ++paramPos) {
            parameterAnnotations[paramPos] = new HashMap<>();
        }
        for (AnnotationInstance i : method.annotations()) {
            if (i.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                parameterAnnotations[i.target().asMethodParameter().position()].put(i.name(), i);
            }
        }
        return parameterAnnotations;
    }
}
