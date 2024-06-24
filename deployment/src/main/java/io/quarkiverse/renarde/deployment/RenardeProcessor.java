package io.quarkiverse.renarde.deployment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Priorities;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationsTransformer;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationsTransformer.TransformationContext;
import org.jboss.resteasy.reactive.common.processor.transformation.Transformation;
import org.jboss.resteasy.reactive.common.util.URLUtils;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.deployment.ControllerVisitor.ControllerClass;
import io.quarkiverse.renarde.deployment.ControllerVisitor.ControllerMethod;
import io.quarkiverse.renarde.deployment.ControllerVisitor.UriPart;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkiverse.renarde.impl.RenardeConfigBean;
import io.quarkiverse.renarde.impl.RenardeRecorder;
import io.quarkiverse.renarde.router.Router;
import io.quarkiverse.renarde.router.RouterMethod;
import io.quarkiverse.renarde.util.Filters;
import io.quarkiverse.renarde.util.Flash;
import io.quarkiverse.renarde.util.Globals;
import io.quarkiverse.renarde.util.I18N;
import io.quarkiverse.renarde.util.JavaExtensions;
import io.quarkiverse.renarde.util.MyParamConverters;
import io.quarkiverse.renarde.util.MyValidationInterceptor;
import io.quarkiverse.renarde.util.QuteResolvers;
import io.quarkiverse.renarde.util.RedirectExceptionMapper;
import io.quarkiverse.renarde.util.RenardeJWTAuthMechanism;
import io.quarkiverse.renarde.util.RenardeValidationLocaleResolver;
import io.quarkiverse.renarde.util.RenderArgs;
import io.quarkiverse.renarde.util.Validation;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem;
import io.quarkus.arc.deployment.AutoAddScopeBuildItem.MatchPredicate;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BuildTimeConditionBuildItem;
import io.quarkus.arc.deployment.ExcludedTypeBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceDirectoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveEndPointValidationInterceptor;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.resteasy.reactive.server.spi.AnnotationsTransformerBuildItem;
import io.quarkus.resteasy.reactive.spi.AdditionalResourceClassBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomExceptionMapperBuildItem;
import io.quarkus.resteasy.reactive.spi.ParamConverterBuildItem;
import io.quarkus.runtime.LocalesBuildTimeConfig;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.smallrye.jwt.runtime.auth.JWTAuthMechanism;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class RenardeProcessor {

    private static final Logger logger = Logger.getLogger(RenardeProcessor.class);

    public static final DotName DOTNAME_UNI = DotName.createSimple(Uni.class.getName());
    public static final DotName DOTNAME_MULTI = DotName.createSimple(Multi.class.getName());
    public static final DotName DOTNAME_CONTROLLER = DotName.createSimple(Controller.class.getName());
    public static final DotName DOTNAME_ROUTER = DotName.createSimple(Router.class.getName());
    public static final DotName DOTNAME_UNREMOVABLE = DotName.createSimple(Unremovable.class.getName());
    public static final DotName DOTNAME_TRANSACTIONAL = DotName.createSimple(Transactional.class.getName());
    public static final DotName DOTNAME_USER = DotName.createSimple("io.quarkiverse.renarde.security.RenardeUser");
    public static final DotName DOTNAME_USER_WITH_PASSWORD = DotName
            .createSimple("io.quarkiverse.renarde.security.RenardeUserWithPassword");
    public static final DotName DOTNAME_SECURITY = DotName.createSimple("io.quarkiverse.renarde.security.RenardeSecurity");
    public static final DotName DOTNAME_AUTHENTICATION_HANDLER = DotName
            .createSimple("io.quarkiverse.renarde.security.impl.AuthenticationFailedExceptionMapper");
    public static final DotName DOTNAME_RENARDE_FORM_LOGIN_CONTROLLER = DotName
            .createSimple("io.quarkiverse.renarde.security.impl.RenardeFormLoginController");
    public static final DotName DOTNAME_RENARDE_SECURITY_CONTROLLER = DotName
            .createSimple("io.quarkiverse.renarde.security.impl.RenardeSecurityController");

    public static final DotName DOTNAME_HX_CONTROLLER = DotName.createSimple(HxController.class.getName());
    public static final DotName DOTNAME_LOGIN_PAGE = DotName.createSimple("io.quarkiverse.renarde.security.LoginPage");
    public static final DotName DOTNAME_NAMED = DotName.createSimple(Named.class.getName());
    public static final DotName DOTNAME_TEMPLATE_INSTANCE = DotName.createSimple(TemplateInstance.class.getName());

    private static final String FEATURE = "renarde";

    private static final String PDFBOX_PROBLEMATIC_CLASS = "org.apache.pdfbox.pdmodel.encryption.PublicKeySecurityHandler";
    private static final String PDF_RESPONSE_HANDLER_CLASS = "io.quarkiverse.renarde.pdf.runtime.PdfResponseHandler";

    private static final String[] SUPPORTED_OIDC_PROVIDERS = new String[] { "facebook", "apple", "github", "microsoft",
            "google", "twitter", "spotify" };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void setupPdfBox(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClassBuildItem,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceBuildItem,
            BuildProducer<NativeImageResourceDirectoryBuildItem> resource) {
        // If we have the renarde-pdf module, we'll see this class
        if (QuarkusClassLoader.isClassPresentAtRuntime(PDF_RESPONSE_HANDLER_CLASS)) {
            // Perhaps try to unify with https://github.com/quarkiverse/quarkus-pdfbox ?

            // This one needs to be initialised at runtime on jdk21/graalvm 23.1 because setting the logger starts the java2d disposer thread
            runtimeInitializedClassBuildItem.produce(new RuntimeInitializedClassBuildItem(PDF_RESPONSE_HANDLER_CLASS));
            // This one starts some crypto stuff
            runtimeInitializedClassBuildItem.produce(new RuntimeInitializedClassBuildItem(PDFBOX_PROBLEMATIC_CLASS));
            // This is started by anybody doing graphics at startup time, including pdfbox instantiating an empty image
            runtimeInitializedClassBuildItem.produce(new RuntimeInitializedClassBuildItem("sun.java2d.Disposer"));
            // This causes the pdfbox to log at static init time, which creates a JUL which is forbidden
            runtimeInitializedClassBuildItem
                    .produce(new RuntimeInitializedClassBuildItem("com.openhtmltopdf.resource.FSEntityResolver"));
            // These call java/awt stuff at static init, which may initialise Java2D
            runtimeInitializedClassBuildItem
                    .produce(new RuntimeInitializedClassBuildItem("com.openhtmltopdf.java2d.image.AWTFSImage"));
            runtimeInitializedClassBuildItem
                    .produce(new RuntimeInitializedClassBuildItem("com.openhtmltopdf.java2d.image.AWTFSImage$NullImage"));
            runtimeInitializedClassBuildItem
                    .produce(new RuntimeInitializedClassBuildItem("com.openhtmltopdf.pdfboxout.PdfBoxFastOutputDevice"));
            // These are needed at runtime for native image, and missing from quarkiverse-pdfbox
            nativeImageResourceBuildItem.produce(
                    new NativeImageResourceBuildItem(List.of("resources/css/XhtmlNamespaceHandler.css",
                            "resources/schema/openhtmltopdf/catalog-special.xml")));
            resource.produce(new NativeImageResourceDirectoryBuildItem("org/apache/pdfbox/resources/ttf"));

        }
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
    void setupSecurity(LaunchModeBuildItem launchMode, Capabilities capabilities,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runtimeConfigurationBuildItem)
            throws IOException, NoSuchAlgorithmException {
        // make sure we have minimal config
        final Config config = ConfigProvider.getConfig();
        if (capabilities.isPresent(Capability.JWT)) {
            // this allows me to create an exception mapper for auth failures (expired token, invalid user)
            // even with this I can't use an exception mapper apparently, but need to register a reactive route
            defineUnlessPresent("quarkus.http.auth.proactive", "false", config, runtimeConfigurationBuildItem);
            // not sure this one matters, just has to be set to any value AFAICT
            defineUnlessPresent("mp.jwt.verify.issuer", "https://example.com/issuer", config, runtimeConfigurationBuildItem);
            // those are the better defaults
            defineUnlessPresent("mp.jwt.token.header", "Cookie", config, runtimeConfigurationBuildItem);
            defineUnlessPresent("mp.jwt.token.cookie", "QuarkusUser", config, runtimeConfigurationBuildItem);
        }
        // Apparently, no OIDC capability to check
        for (String provider : SUPPORTED_OIDC_PROVIDERS) {
            if ((config.getOptionalValue("quarkus.oidc." + provider + ".provider", String.class).isPresent()
                    || config.getOptionalValue("quarkus.oidc." + provider + ".client-id", String.class).isPresent())
                    && !config.getOptionalValue("quarkus.oidc." + provider + ".authentication.redirect-path", String.class)
                            .isPresent()) {
                runtimeConfigurationBuildItem
                        .produce(new RunTimeConfigurationDefaultBuildItem(
                                "quarkus.oidc." + provider + ".authentication.redirect-path",
                                "/_renarde/security/oidc-success"));
            }
        }
    }

    private void defineUnlessPresent(String key, String value, Config config,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runtimeConfigurationBuildItem) {
        if (!config.getOptionalValue(key, String.class).isPresent()) {
            runtimeConfigurationBuildItem
                    .produce(new RunTimeConfigurationDefaultBuildItem(key, value));
        }
    }

    @BuildStep
    void setupJWT(LaunchModeBuildItem launchMode, Capabilities capabilities,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runtimeConfigurationBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem)
            throws IOException, NoSuchAlgorithmException {
        if (launchMode.getLaunchMode().isDevOrTest()
                && capabilities.isPresent(Capability.JWT)) {
            // make sure we have minimal config
            final Config config = ConfigProvider.getConfig();

            // PRIVATE
            Optional<String> decryptKeyLocationOpt = config.getOptionalValue("mp.jwt.decrypt.key.location", String.class);
            Optional<String> signKeyLocationOpt = config.getOptionalValue("smallrye.jwt.sign.key.location", String.class);
            // PUBLIC
            Optional<String> verifyKeyOpt = config.getOptionalValue("mp.jwt.verify.publickey", String.class);
            Optional<String> verifyKeyLocationOpt = config.getOptionalValue("mp.jwt.verify.publickey.location", String.class);
            Optional<String> encryptKeyLocationOpt = config.getOptionalValue("smallrye.jwt.encrypt.key.location", String.class);
            if (!decryptKeyLocationOpt.isPresent()
                    && !signKeyLocationOpt.isPresent()
                    && !verifyKeyOpt.isPresent()
                    && !verifyKeyLocationOpt.isPresent()
                    && !encryptKeyLocationOpt.isPresent()) {
                // FIXME: folder

                File buildDir = null;
                ArtifactSources src = curateOutcomeBuildItem.getApplicationModel().getAppArtifact().getSources();
                if (src != null) { // shouldn't be null in dev mode
                    Collection<SourceDir> srcDirs = src.getResourceDirs();
                    if (srcDirs.isEmpty()) {
                        // in the module has no resources dir?
                        srcDirs = src.getSourceDirs();
                    }
                    if (!srcDirs.isEmpty()) {
                        // pick the first resources output dir
                        Path resourcesOutputDir = srcDirs.iterator().next().getOutputDir();
                        buildDir = resourcesOutputDir.toFile();
                    }
                }
                if (buildDir == null) {
                    // the module doesn't have any sources nor resources, stick to the build dir
                    buildDir = new File(
                            curateOutcomeBuildItem.getApplicationModel().getAppArtifact().getWorkspaceModule().getBuildDir(),
                            "classes");
                }

                buildDir.mkdirs();
                File privateKey = new File(buildDir, "dev.privateKey.pem");
                File publicKey = new File(buildDir, "dev.publicKey.pem");
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
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItems,
            BuildProducer<ApplicationClassPredicateBuildItem> applicationClassPredicateBuildItems) {
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(Globals.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(Filters.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(QuteResolvers.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(Flash.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(I18N.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(RenderArgs.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(Validation.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(RenardeValidationLocaleResolver.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(JavaExtensions.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(MyValidationInterceptor.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(RenardeJWTAuthMechanism.class));
        additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(RenardeConfigBean.class));
        // If we have the renarde-security module, we'll see this class
        if (QuarkusClassLoader.isClassPresentAtRuntime(DOTNAME_AUTHENTICATION_HANDLER.toString())) {
            additionalBeanBuildItems.produce(AdditionalBeanBuildItem.unremovableOf(DOTNAME_AUTHENTICATION_HANDLER.toString()));
        }

        paramConverterBuildItems.produce(new ParamConverterBuildItem(MyParamConverters.class.getName(), Priorities.USER, true));

        additionalIndexedClassesBuildItems.produce(
                new AdditionalIndexedClassesBuildItem(Filters.class.getName(), RedirectExceptionMapper.class.getName(),
                        Controller.class.getName(), HxController.class.getName()));

        /*
         * We don't have these beans, but they are endpoints, and they can't be declared FWK classes, otherwise
         * config changes will not reload them. And config changes may affect the number of interceptors declared
         * on beans, thus causing NoSuchMethodError because we generate bean proxy/client methods that take one
         * parameter per interceptor.
         */
        applicationClassPredicateBuildItems.produce(new ApplicationClassPredicateBuildItem(new Predicate<String>() {
            @Override
            public boolean test(String t) {
                return DOTNAME_RENARDE_SECURITY_CONTROLLER.toString('.').equals(t)
                        || DOTNAME_RENARDE_FORM_LOGIN_CONTROLLER.toString('.').equals(t);
            }
        }));
    }

    @BuildStep
    void registerCustomExceptionMappers(BuildProducer<CustomExceptionMapperBuildItem> customExceptionMapper) {
        // If we have the renarde-security module, we'll see this class
        if (QuarkusClassLoader.isClassPresentAtRuntime(DOTNAME_AUTHENTICATION_HANDLER.toString())) {
            customExceptionMapper.produce(new CustomExceptionMapperBuildItem(DOTNAME_AUTHENTICATION_HANDLER.toString()));
        }
    }

    @BuildStep
    ExcludedTypeBuildItem removeOriginalValidatorInterceptor() {
        return new ExcludedTypeBuildItem(ResteasyReactiveEndPointValidationInterceptor.class.getName());
    }

    @BuildStep
    ExcludedTypeBuildItem removeOriginalJWTAuthMechanism() {
        return new ExcludedTypeBuildItem(JWTAuthMechanism.class.getName());
    }

    @BuildStep
    void produceUserInRequestScope(ApplicationIndexBuildItem indexBuildItem,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans) {
        Set<ClassInfo> users = indexBuildItem.getIndex().getAllKnownImplementors(DOTNAME_USER);
        // not sure why I need to do this, but if I don't, I get zero user implementors
        users.addAll(indexBuildItem.getIndex().getAllKnownImplementors(DOTNAME_USER_WITH_PASSWORD));
        if (users.isEmpty())
            return;
        if (users.size() > 2) {
            System.err.println(
                    "Unable to generate request-scoped user producer: more than one user implementation found: " + users);
            return;
        }
        ClassInfo userClass = users.iterator().next();
        /*
         * @RequestScoped
         * public class MySecurity {
         *
         * @Inject
         * RenardeSecurity security;
         *
         * @Named("user")
         *
         * @Produces
         * public User getUser() {
         * return (User) security.getUser();
         * }
         * }
         */
        ClassOutput beansClassOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
        try (ClassCreator beanClassCreator = ClassCreator.builder().classOutput(beansClassOutput)
                .className("__RenardeUserProducer")
                .build()) {
            beanClassCreator.addAnnotation(RequestScoped.class);

            FieldCreator fieldCreator = beanClassCreator.getFieldCreator("security", DOTNAME_SECURITY.toString());
            fieldCreator.addAnnotation(Inject.class);
            fieldCreator.setModifiers(0);

            try (MethodCreator methodCreator = beanClassCreator.getMethodCreator("getUser", userClass.name().toString())) {
                methodCreator.addAnnotation(Produces.class);
                AnnotationInstance annotationInstance = AnnotationInstance.create(DOTNAME_NAMED, null,
                        Arrays.asList(AnnotationValue.createStringValue("value", "user")));
                methodCreator.addAnnotation(annotationInstance);

                ResultHandle security = methodCreator.readInstanceField(
                        FieldDescriptor.of(beanClassCreator.getClassName(), "security", DOTNAME_SECURITY.toString()),
                        methodCreator.getThis());
                ResultHandle user = methodCreator.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(DOTNAME_SECURITY.toString(), "getUser", DOTNAME_USER.toString()), security);
                methodCreator.returnValue(methodCreator.checkCast(user, userClass.name().toString()));
            }
        }
    }

    @BuildStep
    void collectControllers(CombinedIndexBuildItem indexBuildItem,
            List<ExcludedControllerBuildItem> excludedControllerBuildItems,
            BuildProducer<AdditionalResourceClassBuildItem> additionalResourceClassBuildItems,
            BuildProducer<AnnotationsTransformerBuildItem> annotationTransformerBuildItems,
            BuildProducer<io.quarkus.arc.deployment.AnnotationsTransformerBuildItem> arcTransformers,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformers,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<LoginPageBuildItem> loginPageBuildItem,
            BuildProducer<ExecutionModelAnnotationsAllowedBuildItem> executionModelAnnotationsAllowedBuildItems,
            BuildProducer<AutoAddScopeBuildItem> autoAddScopeBuildItems) {
        Set<DotName> excludedControllers = new HashSet<>();
        for (ExcludedControllerBuildItem excludedControllerBuildItem : excludedControllerBuildItems) {
            excludedControllers.add(excludedControllerBuildItem.excludedClass);
        }
        Set<DotName> controllers = new HashSet<>();
        Map<String, ControllerVisitor.ControllerClass> methodsByClass = new HashMap<>();
        for (ClassInfo controllerInfo : indexBuildItem.getIndex().getAllKnownSubclasses(DOTNAME_CONTROLLER)) {
            // skip excluded controllers
            if (excludedControllers.contains(controllerInfo.name())) {
                continue;
            }
            controllers.add(controllerInfo.name());
            // do not register abstract controllers as beans or resources
            if (!Modifier.isAbstract(controllerInfo.flags())) {
                additionalResourceClassBuildItems.produce(new AdditionalResourceClassBuildItem(controllerInfo, ""));
                unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(controllerInfo.name()));
            }
            methodsByClass.put(controllerInfo.name().toString(), scanController(controllerInfo, loginPageBuildItem));
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

        autoAddScopeBuildItems.produce(AutoAddScopeBuildItem.builder()
                .defaultScope(BuiltinScope.REQUEST)
                .match(new MatchPredicate() {
                    @Override
                    public boolean test(ClassInfo klass, Collection<AnnotationInstance> annotations, IndexView index) {
                        return !klass.isInterface()
                                && !Modifier.isAbstract(klass.flags())
                                && controllers.contains(klass.name());
                    }
                }).build());

        arcTransformers.produce(new io.quarkus.arc.deployment.AnnotationsTransformerBuildItem(
                new io.quarkus.arc.processor.AnnotationsTransformer() {
                    @Override
                    public void transform(TransformationContext transformationContext) {
                        if (transformationContext.isMethod()) {
                            MethodInfo method = transformationContext.getTarget().asMethod();
                            if (controllers.contains(method.declaringClass().name())
                                    && !method.hasAnnotation(DOTNAME_TRANSACTIONAL)
                                    && !isAsync(method.returnType())
                                    && (method.hasAnnotation(ResteasyReactiveDotNames.POST)
                                            || method.hasAnnotation(ResteasyReactiveDotNames.PUT)
                                            || method.hasAnnotation(ResteasyReactiveDotNames.DELETE))) {
                                transformationContext.transform().add(DOTNAME_TRANSACTIONAL)
                                        .done();
                            }
                        }
                    }
                }));

        // Make sure the @Blocking annotation checker sees our endpoints, because it's not using the annotation transformer
        executionModelAnnotationsAllowedBuildItems
                .produce(new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
                    @Override
                    public boolean test(MethodInfo method) {
                        return isControllerMethod(method)
                                && controllers.contains(method.declaringClass().name());
                    }
                }));

    }

    protected boolean isAsync(Type type) {
        if (type.kind() == Type.Kind.CLASS
                || type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            return type.name().equals(DOTNAME_UNI)
                    || type.name().equals(DOTNAME_MULTI);
        }
        return false;
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
                methodCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Router.class, "clearRoutes", void.class));
                for (ControllerClass controllerClass : methodsByClass.values()) {
                    // do not register routes for abstract controllers
                    if (controllerClass.isAbstract) {
                        continue;
                    }
                    String simpleControllerName = controllerClass.className;
                    // strip package
                    int lastDot = simpleControllerName.lastIndexOf('.');
                    if (lastDot != -1) {
                        simpleControllerName = simpleControllerName.substring(lastDot + 1);
                    }
                    // strip outer classes
                    int lastDollar = simpleControllerName.lastIndexOf('$');
                    if (lastDollar != -1) {
                        simpleControllerName = simpleControllerName.substring(lastDollar + 1);
                    }
                    // register all methods for this controller, including super methods, but on this controller name
                    for (ControllerMethod method : controllerClass.getMethods(methodsByClass).values()) {
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

    private ControllerVisitor.ControllerClass scanController(ClassInfo controllerInfo,
            BuildProducer<LoginPageBuildItem> loginPageBuildItem) {
        Map<String, ControllerMethod> methods = new HashMap<>();
        for (MethodInfo method : controllerInfo.methods()) {
            if (!isControllerMethod(method))
                continue;
            List<UriPart> parts = new ArrayList<>();

            String path = getMethodPath(controllerInfo, method);
            parts.add(new ControllerVisitor.StaticUriPart(path));

            // collect declared path params
            Set<String> pathParameters = new HashSet<>();
            URLUtils.parsePathParameters(path, pathParameters);

            // collect param annotations
            Map<DotName, AnnotationInstance>[] parameterAnnotations = getParameterAnnotations(method);

            // look for undeclared path params
            for (int paramIndex = 0, asmParamIndex = 1; paramIndex < method.parametersCount(); ++paramIndex) {
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
                asmParamIndex += AsmUtil.getParameterSize(method.parameterType(paramIndex));
            }

            if (method.hasAnnotation(DOTNAME_LOGIN_PAGE)) {
                loginPageBuildItem.produce(new LoginPageBuildItem(parts));
            }

            String descriptor = method.descriptor();
            String key = method.name() + "/" + descriptor;
            methods.put(key, new ControllerMethod(method.name(), descriptor, parts,
                    method.parameterTypes()));
        }
        return new ControllerVisitor.ControllerClass(controllerInfo.name().toString(), controllerInfo.superName().toString(),
                Modifier.isAbstract(controllerInfo.flags()), methods);
    }

    private String getMethodPath(ClassInfo controllerInfo, MethodInfo method) {
        AnnotationInstance classPath = method.declaringClass().declaredAnnotation(ResteasyReactiveDotNames.PATH);
        String className = method.declaringClass().simpleName();
        String classPathValue = classPath != null ? classPath.value().value().toString() : null;

        AnnotationInstance methodPath = method.annotation(ResteasyReactiveDotNames.PATH);
        String methodPathValue = methodPath != null ? methodPath.value().value().toString() : method.name();

        if (classPathValue == null) {
            // defaults to className, unless method part is absolute
            if (methodPathValue.startsWith("/")) {
                return methodPathValue;
            }
            classPathValue = className;
        }

        if (classPathValue.equals("/")) {
            classPathValue = "";
        } else if (!classPathValue.isEmpty() && !classPathValue.startsWith("/")) {
            classPathValue = "/" + classPathValue;
        }
        boolean needsSeparator = !classPathValue.endsWith("/") && !methodPathValue.startsWith("/");
        String ret = classPathValue + (needsSeparator ? "/" : "") + methodPathValue;
        // strip last / if it's not the root uri
        if (ret.length() > 1 && ret.endsWith("/")) {
            ret = ret.substring(0, ret.length() - 1);
        }
        return ret;
    }

    private boolean isControllerMethod(MethodInfo method) {
        return !Modifier.isAbstract(method.flags())
                && Modifier.isPublic(method.flags())
                && !Modifier.isNative(method.flags())
                && !Modifier.isStatic(method.flags())
                && !method.name().equals("<init>")
                && !method.name().equals("<clinit>")
                && !method.hasDeclaredAnnotation(ServerExceptionMapper.class);
    }

    private void transformController(TransformationContext ti, Set<DotName> controllers) {
        ClassInfo klass = ti.getTarget().asClass();
        if (controllers.contains(klass.name()) && !Modifier.isAbstract(klass.flags())) {
            /*
             * Note that we can't change the class @Path annotation, since RR doesn't use AnnotationTransformer for class path
             * scanning.
             */
            if (klass.declaredAnnotation(ResteasyReactiveDotNames.PATH) == null) {
                ti.transform()
                        .add(ResteasyReactiveDotNames.PATH, AnnotationValue.createStringValue("value", ""))
                        .done();
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
             * If the class path is not specified, collect both class path and method path and convert to a method path alone
             * Note that we can't change the class @Path annotation, since RR doesn't use AnnotationTransformer for class path
             * scanning.
             */
            AnnotationInstance classPath = method.declaringClass().declaredAnnotation(ResteasyReactiveDotNames.PATH);
            String path = getMethodPath(method.declaringClass(), method);
            AnnotationInstance methodPath = method.declaredAnnotation(ResteasyReactiveDotNames.PATH);
            String methodPathValue;
            boolean setMethodPath = false;
            if (classPath == null) {
                methodPathValue = path;
                setMethodPath = true;
            } else if (methodPath != null) {
                methodPathValue = methodPath.value().value().toString();
            } else {
                methodPathValue = method.name();
                setMethodPath = true;
            }

            // collect declared path params
            Set<String> pathParameters = new HashSet<>();
            URLUtils.parsePathParameters(path, pathParameters);

            // collect param annotations
            Map<DotName, AnnotationInstance>[] parameterAnnotations = getParameterAnnotations(method);

            // look for undeclared path params
            for (int paramPos = 0; paramPos < method.parametersCount(); ++paramPos) {
                if ((parameterAnnotations[paramPos].get(ResteasyReactiveDotNames.PATH_PARAM) != null
                        || parameterAnnotations[paramPos].get(ResteasyReactiveDotNames.REST_PATH_PARAM) != null)
                        && !pathParameters.contains(method.parameterName(paramPos))) {
                    // add them to the method path
                    methodPathValue += "/{" + method.parameterName(paramPos) + "}";
                    setMethodPath = true;
                }
            }

            if (setMethodPath) {
                Transformation transform = ti.transform();
                if (methodPath != null) {
                    transform.remove(ai -> ai.name().equals(ResteasyReactiveDotNames.PATH));
                }
                transform.add(ResteasyReactiveDotNames.PATH, AnnotationValue.createStringValue("value", methodPathValue));
                transform.done();
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
        @SuppressWarnings("unchecked")
        Map<DotName, AnnotationInstance>[] parameterAnnotations = new Map[method.parametersCount()];
        for (int paramPos = 0; paramPos < method.parametersCount(); ++paramPos) {
            parameterAnnotations[paramPos] = new HashMap<>();
        }
        for (AnnotationInstance i : method.annotations()) {
            if (i.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                parameterAnnotations[i.target().asMethodParameter().position()].put(i.name(), i);
            }
        }
        return parameterAnnotations;
    }

    @BuildStep
    void removeHxController(BuildProducer<ExcludedControllerBuildItem> excludedControllerBuildItems) {
        excludedControllerBuildItems.produce(new ExcludedControllerBuildItem(DOTNAME_HX_CONTROLLER));
    }

    @BuildStep
    void removeLoginControllerIfNoUserWithPassword(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<ExcludedControllerBuildItem> excludedControllerBuildItems,
            BuildProducer<ExcludedTypeBuildItem> excludedTypeBuildItems,
            BuildProducer<BuildTimeConditionBuildItem> buildTimeConditionBuildItems) {
        if (indexBuildItem.getIndex().getAllKnownImplementors(DOTNAME_USER_WITH_PASSWORD).isEmpty()) {
            // for Renarde
            excludedControllerBuildItems.produce(new ExcludedControllerBuildItem(DOTNAME_RENARDE_FORM_LOGIN_CONTROLLER));
            // for RESTEasy Reactive
            excludedTypeBuildItems.produce(new ExcludedTypeBuildItem(DOTNAME_RENARDE_FORM_LOGIN_CONTROLLER.toString()));
            ClassInfo klass = indexBuildItem.getIndex().getClassByName(DOTNAME_RENARDE_FORM_LOGIN_CONTROLLER);
            // don't exclude it if the user didn't import the security module
            if (klass != null) {
                buildTimeConditionBuildItems.produce(new BuildTimeConditionBuildItem(klass, false));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configureLoginPage(RenardeRecorder recorder,
            LoginPageBuildItem loginPageBuildItem,
            BeanContainerBuildItem beanContainerBuildItem) {
        String loginPage;
        if (loginPageBuildItem != null) {
            loginPage = loginPageBuildItem.uri;
        } else {
            Config config = ConfigProvider.getConfig();
            // if we have a single provider it's easy
            String oidcLoginPage = null;
            for (String provider : SUPPORTED_OIDC_PROVIDERS) {
                if ((config.getOptionalValue("quarkus.oidc." + provider + ".provider", String.class).isPresent()
                        || config.getOptionalValue("quarkus.oidc." + provider + ".client-id", String.class).isPresent())) {
                    if (oidcLoginPage == null) {
                        oidcLoginPage = "/_renarde/security/login-" + provider;
                    } else {
                        // two providers, we can't choose, fall back to root
                        oidcLoginPage = null;
                        break;
                    }
                }
            }
            if (oidcLoginPage != null) {
                loginPage = oidcLoginPage;
            } else {
                loginPage = "/";
            }
        }
        recorder.configureLoginPage(beanContainerBuildItem.getValue(), loginPage);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void findMessageFiles(RenardeRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LocalesBuildTimeConfig locales,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> watchedFiles) throws IOException {

        Map<String, Path> languageToPath = new HashMap<>();

        for (ApplicationArchive archive : applicationArchivesBuildItem.getAllApplicationArchives()) {
            archive.accept(tree -> {
                for (Path root : tree.getRoots()) {
                    try (Stream<Path> files = Files.list(root)) {
                        Iterator<Path> iter = files.iterator();
                        while (iter.hasNext()) {
                            Path filePath = iter.next();
                            String name = filePath.getFileName().toString();
                            if (Files.isRegularFile(filePath)
                                    && (name.startsWith("messages.")
                                            || name.startsWith("messages_"))
                                    && name.endsWith(".properties")) {
                                String language;
                                if (name.startsWith("messages.")) {
                                    // default language
                                    language = locales.defaultLocale.getLanguage();
                                } else {
                                    // messages_lang.properties
                                    language = name.substring(9, name.length() - 11);
                                }
                                Path relativePath = root.relativize(filePath);
                                languageToPath.put(language, relativePath);
                            }
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        }

        // FIXME: should not cause a full restart
        // Hot deployment and native-image
        for (Path messageFileName : languageToPath.values()) {
            watchedFiles.produce(new HotDeploymentWatchedFileBuildItem(messageFileName.toString()));
            nativeImageResources.produce(new NativeImageResourceBuildItem(messageFileName.toString()));
        }

        for (Entry<String, Path> entry : languageToPath.entrySet()) {
            recorder.addLanguageBundle(beanContainerBuildItem.getValue(), entry.getKey(), entry.getValue().toString());
        }

        for (Locale locale : locales.locales) {
            String lang = locale.getLanguage();
            if (!languageToPath.containsKey(lang)) {
                logger.warnf(
                        "Locale %s is declared in 'quarkus.locales' but no matching messages_%s.properties resource file found",
                        lang, lang);
            }
        }
        if (!languageToPath.isEmpty()) {
            logger.infof("Supported locales with messages: %s", languageToPath.keySet());
        }
    }
}
