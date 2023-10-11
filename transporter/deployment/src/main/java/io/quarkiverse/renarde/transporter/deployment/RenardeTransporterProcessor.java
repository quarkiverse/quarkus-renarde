package io.quarkiverse.renarde.transporter.deployment;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Entity;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.objectweb.asm.Opcodes;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.quarkiverse.renarde.jpa.deployment.ModelField;
import io.quarkiverse.renarde.transporter.EntityTransporter;
import io.quarkiverse.renarde.transporter.InstanceResolver;
import io.quarkiverse.renarde.transporter.ValueTransformer;
import io.quarkiverse.renarde.transporter.impl.TransporterUtil;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.gizmo.Switch.StringSwitch;
import io.quarkus.gizmo.Type;
import io.quarkus.gizmo.WhileLoop;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.HibernateMetamodelForFieldAccessBuildItem;

public class RenardeTransporterProcessor {

    private static final DotName DOTNAME_ENTITY = DotName.createSimple(Entity.class.getName());
    private static final String SERIALIZER_POSTFIX = "__RenardeTransporterSerializer";
    private static final String DESERIALIZER_POSTFIX = "__RenardeTransporterDeserializer";
    private static final DotName DOTNAME_JSON_SERIALIZER = DotName.createSimple(JsonSerializer.class);
    private static final DotName DOTNAME_JSON_DESERIALIZER = DotName.createSimple(JsonDeserializer.class);

    @BuildStep
    public void processModel(HibernateMetamodelForFieldAccessBuildItem metamodel,
            CombinedIndexBuildItem index,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<GeneratedClassBuildItem> output,
            BuildProducer<GeneratedBeanBuildItem> beanOutput,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            ApplicationArchivesBuildItem applicationArchives) {

        Collection<AnnotationInstance> entityAnnotations = index.getIndex().getAnnotations(DOTNAME_ENTITY);
        List<EntityModel> entityModels = new ArrayList<>();
        Graph<String, DefaultEdge> modelGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (AnnotationInstance entityAnnotation : entityAnnotations) {
            if (entityAnnotation.target().kind() != Kind.CLASS)
                continue;
            ClassInfo entityClassInfo = entityAnnotation.target().asClass();
            EntityModel entityModel = metamodel.getMetamodelInfo().getEntityModel(entityClassInfo.name().toString());
            if (entityModel == null)
                continue;
            List<ModelField> modelFields = ModelField.loadModelFields(entityModel, metamodel.getMetamodelInfo(),
                    index.getIndex());
            generateSerializer(entityClassInfo, entityModel, modelFields, output);
            generateDeserializer(entityClassInfo, entityModel, modelFields, output);
            entityModels.add(entityModel);
            modelGraph.addVertex(entityModel.name);
            for (ModelField modelField : modelFields) {
                switch (modelField.type) {
                    case Relation:
                    case MultiRelation:
                    case MultiMultiRelation:
                        if (modelField.relationOwner) {
                            // Add a dependency on it
                            modelGraph.addVertex(modelField.relationClass);
                            modelGraph.addEdge(entityModel.name, modelField.relationClass);
                        }
                        break;
                }
            }
        }
        generateEntityTransporter(entityModels, modelGraph, beanOutput, unremovableBeans);
    }

    private void generateEntityTransporter(List<EntityModel> entityModels,
            Graph<String, DefaultEdge> modelGraph,
            BuildProducer<GeneratedBeanBuildItem> output,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        String className = EntityTransporter.class.getName() + "$Impl";
        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(className));
        try (ClassCreator c = ClassCreator.builder()
                .classOutput(new GeneratedBeanGizmoAdaptor(output)).className(
                        className)
                .interfaces(EntityTransporter.class)
                .build()) {
            c.addAnnotation(ApplicationScoped.class);
            c.getFieldCreator("sortedEntities", Class[].class).setModifiers(Modifier.STATIC | Modifier.FINAL);
            try (MethodCreator m = c
                    .getMethodCreator("<clinit>", void.class)
                    .setModifiers(Modifier.PUBLIC | Modifier.STATIC)) {
                // we get them from most deps to less deps
                TopologicalOrderIterator<String, DefaultEdge> entitySorter = new TopologicalOrderIterator<>(modelGraph);
                List<String> sortedEntities = new ArrayList<>();
                while (entitySorter.hasNext()) {
                    String type = entitySorter.next();
                    sortedEntities.add(type);
                }
                // make sure we turn it around from less deps to more deps, since that's what's useful for deserialisation
                Collections.reverse(sortedEntities);
                ResultHandle array = m.newArray(Class[].class, sortedEntities.size());
                for (int i = 0; i < sortedEntities.size(); i++) {
                    m.writeArrayValue(array, i, m.loadClass(sortedEntities.get(i)));
                }
                m.writeStaticField(FieldDescriptor.of(className, "sortedEntities", Class[].class), array);
                m.returnVoid();
            }
            try (MethodCreator m = c
                    .getMethodCreator("sortedEntityTypes", Class[].class)
                    // FIXME: signature?
                    .setModifiers(Modifier.PUBLIC)) {
                m.returnValue(m.readStaticField(FieldDescriptor.of(className, "sortedEntities", Class[].class)));
            }
            try (MethodCreator m = c
                    .getMethodCreator("addDeserializers", void.class, SimpleModule.class, InstanceResolver.class)
                    .setModifiers(Modifier.PUBLIC)) {
                for (EntityModel entityModel : entityModels) {
                    String deserializerClassName = entityModel.name + DESERIALIZER_POSTFIX;
                    m.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(SimpleModule.class, "addDeserializer", SimpleModule.class, Class.class,
                                    JsonDeserializer.class),
                            m.getMethodParam(0),
                            m.loadClass(entityModel.name),
                            m.newInstance(MethodDescriptor.ofConstructor(deserializerClassName, InstanceResolver.class),
                                    m.getMethodParam(1)));
                }
                m.returnVoid();
            }
            try (MethodCreator m = c.getMethodCreator("addSerializers", void.class, SimpleModule.class, ValueTransformer.class)
                    .setModifiers(Modifier.PUBLIC)) {
                for (EntityModel entityModel : entityModels) {
                    String serializerClassName = entityModel.name + SERIALIZER_POSTFIX;
                    m.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(SimpleModule.class, "addSerializer", SimpleModule.class, Class.class,
                                    JsonSerializer.class),
                            m.getMethodParam(0),
                            m.loadClass(entityModel.name),
                            m.newInstance(MethodDescriptor.ofConstructor(serializerClassName, ValueTransformer.class),
                                    m.getMethodParam(1)));
                }
                m.returnVoid();
            }
            // FIXME: signature?
            try (MethodCreator m = c.getMethodCreator("getEntityClass", Class.class, String.class)
                    .setModifiers(Modifier.PUBLIC)) {
                StringSwitch typeSwitch = m.stringSwitch(m.getMethodParam(0));
                for (EntityModel entityModel : entityModels) {
                    typeSwitch.caseOf(entityModel.name, t -> t.returnValue(t.loadClass(entityModel.name)));
                }
                typeSwitch.defaultCase(t -> t.throwException(AssertionError.class, "Unknown entity model"));
            }
            // FIXME: signature?
            try (MethodCreator m = c.getMethodCreator("instantiate", PanacheEntityBase.class, String.class)
                    .setModifiers(Modifier.PUBLIC)) {
                StringSwitch typeSwitch = m.stringSwitch(m.getMethodParam(0));
                for (EntityModel entityModel : entityModels) {
                    typeSwitch.caseOf(entityModel.name,
                            t -> t.returnValue(t.newInstance(MethodDescriptor.ofConstructor(entityModel.name))));
                }
                typeSwitch.defaultCase(t -> t.throwException(AssertionError.class, "Unknown entity model"));
            }
        }
    }

    private void generateSerializer(ClassInfo entityClassInfo, EntityModel entityModel,
            List<ModelField> modelFields, BuildProducer<GeneratedClassBuildItem> output) {
        String className = entityClassInfo.name().toString() + SERIALIZER_POSTFIX;
        try (ClassCreator c = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(output, true)).className(
                        className)
                .signature(SignatureBuilder.forClass().setSuperClass(
                        Type.parameterizedType(Type.classType(JsonSerializer.class), Type.classType(entityClassInfo.name()))))
                .build()) {
            // private ValueTransformer transformer;
            c.getFieldCreator("transformer", ValueTransformer.class);

            /*
             * public XSerializer(ValueTransformer transformer){
             * this.transformer = transformer;
             * }
             */
            try (MethodCreator m = c.getMethodCreator("<init>", void.class, ValueTransformer.class)
                    .setModifiers(Modifier.PUBLIC)) {
                m.invokeSpecialMethod(MethodDescriptor.ofConstructor(JsonSerializer.class), m.getThis());
                m.writeInstanceField(FieldDescriptor.of(className, "transformer", ValueTransformer.class), m.getThis(),
                        m.getMethodParam(0));
                m.returnVoid();
            }

            // bridge
            try (MethodCreator m = c
                    .getMethodCreator("serialize", void.class, Object.class, JsonGenerator.class,
                            SerializerProvider.class)
                    .addException(IOException.class)
                    .setModifiers(Modifier.PUBLIC | Opcodes.ACC_BRIDGE)) {
                m.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(className, "serialize", void.class, entityClassInfo.name().toString(),
                                JsonGenerator.class, SerializerProvider.class),
                        m.getThis(),
                        m.checkCast(m.getMethodParam(0), entityClassInfo.name().toString()),
                        m.getMethodParam(1),
                        m.getMethodParam(2));
                m.returnVoid();
            }
            // real method
            try (MethodCreator m = c
                    .getMethodCreator("serialize", void.class, entityClassInfo.name().toString(), JsonGenerator.class,
                            SerializerProvider.class)
                    .addException(IOException.class)
                    .setModifiers(Modifier.PUBLIC)) {
                ResultHandle valueParam = m.getMethodParam(0);
                ResultHandle genParam = m.getMethodParam(1);
                /*
                 * if(value == null){
                 * gen.writeNull();
                 * return;
                 * }
                 */
                BranchResult b = m.ifNull(valueParam);
                try (BytecodeCreator isNullBranch = b.trueBranch()) {
                    isNullBranch.invokeVirtualMethod(MethodDescriptor.ofMethod(JsonGenerator.class, "writeNull", void.class),
                            genParam);
                    isNullBranch.returnVoid();
                }
                b.falseBranch().close();

                // gen.writeStartObject(value);
                m.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(JsonGenerator.class, "writeStartObject", void.class, Object.class), genParam,
                        valueParam);

                NEXT_FIELD: for (ModelField modelField : modelFields) {
                    String serializeParamType = modelField.entityField.descriptor;
                    String methodName = "serialize";
                    switch (modelField.type) {
                        case DateTimeLocal:
                        case Text:
                        case LargeText:
                        case Number:
                        case Binary:
                        case Checkbox:
                            // handled
                            break;
                        case Enum:
                            serializeParamType = Enum.class.getName();
                            break;
                        case MultiMultiRelation:
                            serializeParamType = List.class.getName();
                            break;
                        case MultiRelation:
                            // this is never owning
                            continue NEXT_FIELD;
                        case Relation:
                            serializeParamType = PanacheEntity.class.getName();
                            break;
                        default:
                            throw new RuntimeException("Unknown field type: " + modelField);
                    }
                    ResultHandle fieldValue = m.invokeVirtualMethod(MethodDescriptor.ofMethod(entityClassInfo.name().toString(),
                            modelField.entityField.getGetterName(), modelField.entityField.descriptor), valueParam);
                    m.invokeStaticMethod(MethodDescriptor.ofMethod(TransporterUtil.class, methodName, void.class,
                            JsonGenerator.class, String.class, serializeParamType, Class.class, ValueTransformer.class),
                            genParam, m.load(modelField.name),
                            fieldValue,
                            m.loadClass(entityClassInfo.name().toString()),
                            m.readInstanceField(FieldDescriptor.of(className, "transformer", ValueTransformer.class),
                                    m.getThis()));
                }

                // gen.writeEndObject();
                m.invokeVirtualMethod(MethodDescriptor.ofMethod(JsonGenerator.class, "writeEndObject", void.class), genParam);
                m.returnVoid();
            }
        }
    }

    private void generateDeserializer(ClassInfo entityClassInfo, EntityModel entityModel,
            List<ModelField> modelFields, BuildProducer<GeneratedClassBuildItem> output) {
        String className = entityClassInfo.name().toString() + DESERIALIZER_POSTFIX;
        try (ClassCreator c = ClassCreator.builder()
                .classOutput(new GeneratedClassGizmoAdaptor(output, true)).className(
                        className)
                .signature(SignatureBuilder.forClass().setSuperClass(
                        Type.parameterizedType(Type.classType(JsonDeserializer.class), Type.classType(entityClassInfo.name()))))
                .build()) {

            // private InstanceResolver resolver;
            c.getFieldCreator("resolver", InstanceResolver.class);

            /*
             * public XDeserializer(InstanceResolver resolver){
             * this.resolver = resolver;
             * }
             */
            try (MethodCreator m = c.getMethodCreator("<init>", void.class, InstanceResolver.class)
                    .setModifiers(Modifier.PUBLIC)) {
                m.invokeSpecialMethod(MethodDescriptor.ofConstructor(JsonDeserializer.class), m.getThis());
                m.writeInstanceField(FieldDescriptor.of(className, "resolver", InstanceResolver.class), m.getThis(),
                        m.getMethodParam(0));
                m.returnVoid();
            }

            // bridge
            try (MethodCreator m = c
                    .getMethodCreator("deserialize", Object.class, JsonParser.class, DeserializationContext.class)
                    .addException(IOException.class)
                    .addException(JacksonException.class)
                    .setModifiers(Modifier.PUBLIC | Opcodes.ACC_BRIDGE)) {
                m.returnValue(m.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(className, "deserialize", entityClassInfo.name().toString(), JsonParser.class,
                                DeserializationContext.class),
                        m.getThis(),
                        m.getMethodParam(0),
                        m.getMethodParam(1)));
            }

            // deserialize method
            try (MethodCreator m = c
                    .getMethodCreator("deserialize", entityClassInfo.name().toString(), JsonParser.class,
                            DeserializationContext.class)
                    .addException(IOException.class)
                    .addException(JacksonException.class)
                    .setModifiers(Modifier.PUBLIC)) {
                ResultHandle parserParam = m.getMethodParam(0);
                /*
                 * if(!p.isExpectedStartObjectToken()){
                 * throw new AssertionError();
                 * }
                 */
                ResultHandle isStartObject = m.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(JsonParser.class, "isExpectedStartObjectToken", boolean.class), parserParam);
                assertt(m.ifFalse(isStartObject));

                // fieldtype var;
                Map<String, AssignableResultHandle> variables = new HashMap<>();
                for (ModelField modelField : modelFields) {
                    AssignableResultHandle var = m.createVariable(modelField.entityField.descriptor);
                    variables.put(modelField.name, var);
                    if (modelField.entityField.descriptor.startsWith("L")) {
                        m.assign(var, m.loadNull());
                    } else
                        switch (modelField.entityField.descriptor) {
                            case "Z":
                                m.assign(var, m.load(false));
                                break;
                            case "B":
                                m.assign(var, m.load((byte) 0));
                                break;
                            case "C":
                                m.assign(var, m.load('A'));
                                break;
                            case "S":
                                m.assign(var, m.load((short) 0));
                                break;
                            case "I":
                                m.assign(var, m.load(0));
                                break;
                            case "J":
                                m.assign(var, m.load(0l));
                                break;
                            case "F":
                                m.assign(var, m.load(0.0));
                                break;
                            case "D":
                                m.assign(var, m.load(0.0d));
                                break;
                            default:
                                throw new RuntimeException("don't know how to initialise field " + entityModel.name + "."
                                        + modelField.name + " of type " + modelField.entityField.descriptor);
                        }
                }

                /*
                 * String fieldName;
                 * while((fieldName = p.nextFieldName()) != null) {
                 */
                AssignableResultHandle fieldNameVariable = m.createVariable(String.class);
                WhileLoop whileLoop = m.whileLoop(creator -> {
                    ResultHandle nextFieldName = creator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(JsonParser.class, "nextFieldName", String.class), parserParam);
                    creator.assign(fieldNameVariable, nextFieldName);
                    return creator.ifNotNull(fieldNameVariable);
                });
                /*
                 * switch(fieldName){
                 * case name: name = TransporterUtil.deserializeType(p); break;
                 * default: throw new AssertionError("Oops");
                 * }
                 */
                try (BytecodeCreator whileBlock = whileLoop.block()) {
                    StringSwitch fieldNameSwitch = whileBlock.stringSwitch(fieldNameVariable);
                    for (ModelField modelField : modelFields) {
                        fieldNameSwitch.caseOf(modelField.name, t -> {
                            AssignableResultHandle var = variables.get(modelField.name);
                            boolean needsCast = true;
                            Class<?> additionalParameterType = null;
                            ResultHandle additionalParameterValue = null;
                            String returnDescriptor = modelField.entityField.descriptor;
                            String methodName;
                            switch (modelField.type) {
                                case Text:
                                case LargeText:
                                    methodName = "deserializeText";
                                    break;
                                case DateTimeLocal:
                                    methodName = "deserializeDate";
                                    break;
                                case Number:
                                    switch (modelField.entityField.descriptor) {
                                        case "Ljava/lang/Short;":
                                            methodName = "deserializeBoxedShort";
                                            break;
                                        case "S":
                                            methodName = "deserializeShort";
                                            break;
                                        case "Ljava/lang/Integer;":
                                            methodName = "deserializeBoxedInteger";
                                            break;
                                        case "I":
                                            methodName = "deserializeInt";
                                            break;
                                        case "Ljava/lang/Long;":
                                            methodName = "deserializeBoxedLong";
                                            break;
                                        case "J":
                                            methodName = "deserializeLong";
                                            break;
                                        case "Ljava/lang/Float;":
                                            methodName = "deserializeBoxedFloat";
                                            break;
                                        case "F":
                                            methodName = "deserializeFloat";
                                            break;
                                        case "Ljava/lang/Double;":
                                            methodName = "deserializeBoxedDouble";
                                            break;
                                        case "D":
                                            methodName = "deserializeDouble";
                                            break;
                                        case "Ljava/lang/Character;":
                                            methodName = "deserializeBoxedCharacter";
                                            break;
                                        case "C":
                                            methodName = "deserializeChar";
                                            break;
                                        default:
                                            throw new RuntimeException("don't know how to deserialise field " + entityModel.name
                                                    + "."
                                                    + modelField.name + " of type " + modelField.entityField.descriptor);
                                    }
                                    break;
                                case Checkbox:
                                    switch (modelField.entityField.descriptor) {
                                        case "Ljava/lang/Boolean;":
                                            methodName = "deserializeBoxedBoolean";
                                            break;
                                        case "Z":
                                            methodName = "deserializeBoolean";
                                            break;
                                        default:
                                            throw new RuntimeException("don't know how to deserialise field " + entityModel.name
                                                    + "."
                                                    + modelField.name + " of type " + modelField.entityField.descriptor);
                                    }
                                    break;
                                case Binary:
                                    methodName = "deserializeBlob";
                                    break;
                                case MultiRelation:
                                    // this is never owning
                                    return;
                                case MultiMultiRelation:
                                    methodName = "deserializeMultiRelation";
                                    additionalParameterValue = t.readInstanceField(
                                            FieldDescriptor.of(className, "resolver", InstanceResolver.class),
                                            t.getThis());
                                    additionalParameterType = InstanceResolver.class;
                                    break;
                                case Relation:
                                    methodName = "deserializeRelation";
                                    returnDescriptor = "L" + PanacheEntity.class.getName().replace('.', '/') + ";";
                                    needsCast = true;
                                    additionalParameterValue = t.readInstanceField(
                                            FieldDescriptor.of(className, "resolver", InstanceResolver.class),
                                            t.getThis());
                                    additionalParameterType = InstanceResolver.class;
                                    break;
                                case Enum:
                                    methodName = "deserializeEnum";
                                    returnDescriptor = "L" + Enum.class.getName().replace('.', '/') + ";";
                                    needsCast = true;
                                    String enumClass = modelField.entityField.descriptor;
                                    // remove L and ;
                                    enumClass = enumClass.substring(1, enumClass.length() - 1);
                                    additionalParameterValue = t.loadClass(enumClass);
                                    additionalParameterType = Class.class;
                                    break;
                                default:
                                    throw new RuntimeException("Unknown field type: " + modelField);
                            }
                            ResultHandle readValue = additionalParameterType != null ? t.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(TransporterUtil.class, methodName, returnDescriptor,
                                            JsonParser.class, additionalParameterType),
                                    parserParam,
                                    additionalParameterValue)
                                    : t.invokeStaticMethod(
                                            MethodDescriptor.ofMethod(TransporterUtil.class, methodName, returnDescriptor,
                                                    JsonParser.class),
                                            parserParam);
                            if (needsCast) {
                                readValue = t.checkCast(readValue, modelField.entityField.descriptor);
                            }
                            t.assign(var, readValue);
                        });
                        fieldNameSwitch.defaultCase(t -> {
                            t.throwException(AssertionError.class, "Don't know what to do with field");
                        });
                    }
                }
                // end while
                /*
                 * if(p.currentToken() != JsonToken.END_OBJECT){
                 * throw new AssertionError();
                 * }
                 */
                ResultHandle currentToken = m.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(JsonParser.class, "currentToken", JsonToken.class), parserParam);
                assertt(m.ifReferencesNotEqual(currentToken, m.load(JsonToken.END_OBJECT)));

                // Type entity = resolver.resolve(typename, id)
                AssignableResultHandle entityVar = m.createVariable("L" + entityClassInfo.name().toString('/') + ";");
                m.assign(entityVar,
                        m.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(InstanceResolver.class, "resolve", Object.class, String.class,
                                        Object.class),
                                m.readInstanceField(FieldDescriptor.of(className, "resolver", InstanceResolver.class),
                                        m.getThis()),
                                m.load(entityModel.name),
                                variables.get("id")));

                // entity.field = fieldVar;
                for (ModelField modelField : modelFields) {
                    AssignableResultHandle fieldVar = variables.get(modelField.name);
                    m.writeInstanceField(
                            FieldDescriptor.of(entityModel.name, modelField.name, modelField.entityField.descriptor),
                            entityVar, fieldVar);
                }

                // return entity
                m.returnValue(entityVar);
            }
        }
    }

    private void assertt(BranchResult assertion) {
        try (BytecodeCreator assertionFailedBranch = assertion.trueBranch()) {
            assertionFailedBranch.throwException(
                    assertionFailedBranch.newInstance(MethodDescriptor.ofConstructor(AssertionError.class, Object.class),
                            assertionFailedBranch.load("Assertion failed")));
            assertionFailedBranch.returnVoid();
        }
        assertion.falseBranch().close();

    }
}
