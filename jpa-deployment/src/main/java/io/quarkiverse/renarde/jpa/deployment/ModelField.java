package io.quarkiverse.renarde.jpa.deployment;

import java.lang.annotation.Annotation;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.validator.constraints.Length;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;

import io.quarkiverse.renarde.jpa.NamedBlob;
import io.quarkiverse.renarde.util.JavaExtensions;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;

public class ModelField {

    public static enum Type {
        LargeText,
        Text,
        Number,
        Checkbox,
        DateTimeLocal,
        Date,
        Time,
        Timestamp,
        Enum,
        Relation,
        MultiRelation,
        Ignore,
        MultiMultiRelation,
        Binary,
        JSON;
    }

    private static final DotName DOTNAME_MANYTOMANY = DotName.createSimple(ManyToMany.class.getName());
    private static final DotName DOTNAME_MANYTOONE = DotName.createSimple(ManyToOne.class.getName());
    private static final DotName DOTNAME_ONETOMANY = DotName.createSimple(OneToMany.class.getName());
    private static final DotName DOTNAME_ONETOONE = DotName.createSimple(OneToOne.class.getName());
    private static final DotName DOTNAME_ENUMERATED = DotName.createSimple(Enumerated.class.getName());
    private static final DotName DOTNAME_COLUMN = DotName.createSimple(Column.class.getName());
    private static final DotName DOTNAME_LENGTH = DotName.createSimple(Length.class.getName());
    private static final DotName DOTNAME_SIZE = DotName.createSimple(Size.class.getName());
    private static final DotName DOTNAME_JDBC_TYPE_CODE = DotName.createSimple(JdbcTypeCode.class.getName());
    private static final DotName DOTNAME_TYPES = DotName.createSimple(Types.class.getName());
    private static final DotName DOTNAME_LOB = DotName.createSimple(Lob.class.getName());
    private static final DotName DOTNAME_ID = DotName.createSimple(Id.class.getName());
    public static final String NAMED_BLOB_DESCRIPTOR = "L" + NamedBlob.class.getName().replace('.', '/') + ";";

    // For views
    public String name;
    public String label;
    public Type type = Type.Text;
    public String relationClass;
    public long min, max;
    public double step;
    public String help;

    // For processor
    public EntityField entityField;
    public EntityField inverseField;
    public List<Class<? extends Annotation>> validation = new ArrayList<>();
    // use this rather than EntitiField.signature which is set later (why?)
    public String signature;
    public boolean relationOwner;
    public boolean id;

    public ModelField(EntityField entityField, String entityClass, MetamodelInfo metamodelInfo, IndexView index) {
        this.name = entityField.name;
        this.label = JavaExtensions.capitalised(this.name);
        ClassInfo classInfo = index.getClassByName(DotName.createSimple(entityClass));
        FieldInfo field = classInfo.field(entityField.name);
        this.signature = field.genericSignature();
        AnnotationInstance oneToOne = field.annotation(DOTNAME_ONETOONE);
        AnnotationInstance column = field.annotation(DOTNAME_COLUMN);
        AnnotationInstance jdbcTypeCode = field.annotation(DOTNAME_JDBC_TYPE_CODE);
        this.id = field.annotation(DOTNAME_ID) != null;
        if (jdbcTypeCode != null
                && jdbcTypeCode.value().asInt() == SqlTypes.JSON) {
            this.type = Type.JSON;
        } else if (entityField.descriptor.equals("B")) {
            this.type = Type.Number;
            min = Byte.MIN_VALUE;
            max = Byte.MAX_VALUE;
            step = 1;
        } else if (entityField.descriptor.equals("S")) {
            this.type = Type.Number;
            min = Short.MIN_VALUE;
            max = Short.MAX_VALUE;
            step = 1;
        } else if (entityField.descriptor.equals("I")
                || entityField.descriptor.equals("Ljava/lang/Integer;")) {
            this.type = Type.Number;
            min = Integer.MIN_VALUE;
            max = Integer.MAX_VALUE;
            step = 1;
        } else if (entityField.descriptor.equals("J")
                || entityField.descriptor.equals("Ljava/lang/Long;")) {
            this.type = Type.Number;
            min = Long.MIN_VALUE;
            max = Long.MAX_VALUE;
            step = 1;
        } else if (entityField.descriptor.equals("C")) {
            this.type = Type.Text;
            min = 1;
            max = 1;
        } else if (entityField.descriptor.equals("D")) {
            this.type = Type.Number;
            // this allows floats in number fields
            step = 0.00001;
        } else if (entityField.descriptor.equals("F")) {
            this.type = Type.Number;
            // this allows floats in number fields
            step = 0.00001;
        } else if (entityField.descriptor.equals("Z")
                || entityField.descriptor.equals("Ljava/lang/Boolean;")) {
            this.type = Type.Checkbox;
        } else if (entityField.descriptor.equals("[B")
                || entityField.descriptor.equals("Ljava/sql/Blob;")
                || entityField.descriptor.equals(NAMED_BLOB_DESCRIPTOR)) {
            this.type = Type.Binary;
        } else if (entityField.descriptor.equals("Ljava/lang/String;")) {
            AnnotationInstance length = field.annotation(DOTNAME_LENGTH);
            AnnotationInstance size = field.annotation(DOTNAME_SIZE);
            if (column != null && column.value("length") != null && column.value("length").asInt() > 255) {
                this.type = Type.LargeText;
            } else if (length != null && length.value("max") != null && length.value("max").asInt() > 255) {
                this.type = Type.LargeText;
            } else if (size != null && size.value("max") != null && size.value("max").asInt() > 255) {
                this.type = Type.LargeText;
            } else if (jdbcTypeCode != null && jdbcTypeCode.value() != null
                    && jdbcTypeCode.value().asInt() == Types.LONGVARCHAR) {
                this.type = Type.LargeText;
            } else if (field.hasAnnotation(DOTNAME_LOB)) {
                this.type = Type.LargeText;
            } else {
                this.type = Type.Text;
            }
        } else if (entityField.descriptor.equals("Ljava/util/Date;")
                || entityField.descriptor.equals("Ljava/time/LocalDateTime;")) {
            this.type = Type.DateTimeLocal;
        } else if (entityField.descriptor.equals("Ljava/time/LocalDate;")) {
            this.type = Type.Date;
        } else if (entityField.descriptor.equals("Ljava/time/LocalTime;")) {
            this.type = Type.Time;
        } else if (entityField.descriptor.equals("Ljava/sql/Timestamp;")) {
            this.type = Type.Timestamp;
        } else if (field.hasAnnotation(DOTNAME_ENUMERATED)) {
            this.type = Type.Enum;
        } else if (field.hasAnnotation(DOTNAME_ONETOMANY)) {
            this.type = Type.MultiRelation;
            this.relationClass = field.type().asParameterizedType().arguments().get(0).name().toString();
            EntityModel relationModel = metamodelInfo.getEntityModel(this.relationClass);
            AnnotationValue mappedBy = field.annotation(DOTNAME_ONETOMANY).value("mappedBy");
            String inverseField = mappedBy.asString();
            // FIXME: inheritance
            this.inverseField = relationModel.fields.get(inverseField);
            this.relationOwner = false;
        } else if (field.hasAnnotation(DOTNAME_MANYTOMANY)) {
            this.type = Type.MultiMultiRelation;
            this.relationClass = field.type().asParameterizedType().arguments().get(0).name().toString();
            EntityModel relationModel = metamodelInfo.getEntityModel(this.relationClass);
            AnnotationValue mappedBy = field.annotation(DOTNAME_MANYTOMANY).value("mappedBy");
            if (mappedBy != null) {
                // non-owning
                String inverseField = mappedBy.asString();
                // FIXME: inheritance
                this.inverseField = relationModel.fields.get(inverseField);
                this.relationOwner = false;
            } else {
                ClassInfo relationClassInfo = index.getClassByName(DotName.createSimple(relationClass));
                for (FieldInfo relationField : relationClassInfo.fields()) {
                    AnnotationInstance manyToMany = relationField.annotation(DOTNAME_MANYTOMANY);
                    if (manyToMany != null) {
                        AnnotationValue value = manyToMany.value("mappedBy");
                        if (value != null && value.asString().equals(field.name())) {
                            // we found it
                            this.inverseField = relationModel.fields.get(relationField.name());
                            break;
                        }
                    }
                }
                if (this.inverseField == null) {
                    throw new RuntimeException(
                            "Failed to find owning side of @ManyToMany from " + field + " in relation type " + relationClass);
                }
                this.relationOwner = true;
            }
        } else if (oneToOne != null
                && oneToOne.value("mappedBy") != null) {
            // actually we may want to support this in the future too?
            this.type = Type.Ignore;
            this.relationOwner = false;
        } else if (field.hasAnnotation(DOTNAME_MANYTOONE)
                || (oneToOne != null
                        && oneToOne.value("mappedBy") == null)) {
            this.type = Type.Relation;
            this.relationClass = entityField.descriptor.substring(1, entityField.descriptor.length() - 1).replace('/', '.');
            this.relationOwner = true;
        } else {
            // see if we can find what to do with it
            ClassInfo fieldClassInfo = index.getClassByName(field.type().name());
            //            System.err.println("Unknown field type: " + field.type() + " classinfo: " + fieldClassInfo);
            if (fieldClassInfo != null) {
                if (fieldClassInfo.isEnum()) {
                    this.type = Type.Enum;
                }
            }
        }
        if (column != null && column.value("nullable") != null && !column.value("nullable").asBoolean()) {
            validation.add(NotEmpty.class);
            help = "This field is required";
        }
        this.entityField = entityField;
    }

    public String getClassName() {
        return entityField.descriptor.substring(1, entityField.descriptor.length() - 1).replace('/', '.');
    }

    @Override
    public String toString() {
        return "ModelField " + name + " of type " + entityField.descriptor;
    }

    public static List<ModelField> loadModelFields(EntityModel entityModel, MetamodelInfo metamodelInfo,
            IndexView index) {
        List<ModelField> fields = new ArrayList<>();
        addFields(fields, entityModel, metamodelInfo, index);
        return fields;
    }

    private static void addFields(List<ModelField> fields, EntityModel entityModel, MetamodelInfo metamodelInfo,
            IndexView index) {
        for (Entry<String, EntityField> entry : entityModel.fields.entrySet()) {
            ModelField mf = new ModelField(entry.getValue(), entityModel.name, metamodelInfo,
                    index);
            if (mf.type != Type.Ignore) {
                fields.add(mf);
            }
        }
        if (entityModel.superClassName != null) {
            EntityModel superModel = metamodelInfo.getEntityModel(entityModel.superClassName);
            if (superModel != null) {
                addFields(fields, superModel, metamodelInfo, index);
            }
        }
    }
}
