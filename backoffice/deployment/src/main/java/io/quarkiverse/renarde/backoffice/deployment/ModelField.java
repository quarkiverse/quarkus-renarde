package io.quarkiverse.renarde.backoffice.deployment;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotEmpty;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;

import io.quarkiverse.renarde.util.JavaExtensions;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;

public class ModelField {

    public static enum Type {
        Text,
        Number,
        Checkbox,
        DateTimeLocal,
        Date,
        Time,
        Enum,
        Relation,
        MultiRelation,
        Ignore;
    }

    private static final DotName DOTNAME_MANYTOONE = DotName.createSimple(ManyToOne.class.getName());
    private static final DotName DOTNAME_ONETOMANY = DotName.createSimple(OneToMany.class.getName());
    private static final DotName DOTNAME_ONETOONE = DotName.createSimple(OneToOne.class.getName());
    private static final DotName DOTNAME_ENUMERATED = DotName.createSimple(Enumerated.class.getName());
    private static final DotName DOTNAME_COLUMN = DotName.createSimple(Column.class.getName());

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

    public ModelField(EntityField entityField, String entityClass, MetamodelInfo metamodelInfo, IndexView index) {
        this.name = entityField.name;
        this.label = JavaExtensions.capitalised(this.name);
        ClassInfo classInfo = index.getClassByName(DotName.createSimple(entityClass));
        FieldInfo field = classInfo.field(entityField.name);
        AnnotationInstance oneToOne = field.annotation(DOTNAME_ONETOONE);
        if (entityField.descriptor.equals("B")) {
            this.type = Type.Number;
            min = Byte.MIN_VALUE;
            max = Byte.MAX_VALUE;
            step = 1;
        } else if (entityField.descriptor.equals("S")) {
            this.type = Type.Number;
            min = Short.MIN_VALUE;
            max = Short.MAX_VALUE;
            step = 1;
        } else if (entityField.descriptor.equals("I")) {
            this.type = Type.Number;
            min = Integer.MIN_VALUE;
            max = Integer.MAX_VALUE;
            step = 1;
        } else if (entityField.descriptor.equals("J")) {
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
        } else if (entityField.descriptor.equals("Z")) {
            this.type = Type.Checkbox;
        } else if (entityField.descriptor.equals("Ljava/lang/String;")) {
            this.type = Type.Text;
        } else if (entityField.descriptor.equals("Ljava/util/Date;")
                || entityField.descriptor.equals("Ljava/time/LocalDateTime;")) {
            this.type = Type.DateTimeLocal;
        } else if (entityField.descriptor.equals("Ljava/time/LocalDate;")) {
            this.type = Type.Date;
        } else if (entityField.descriptor.equals("Ljava/time/LocalTime;")) {
            this.type = Type.Time;
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
        } else if (oneToOne != null
                && oneToOne.value("mappedBy") != null) {
            // actually we may want to support this in the future too?
            this.type = Type.Ignore;
        } else if (field.hasAnnotation(DOTNAME_MANYTOONE)
                || (oneToOne != null
                        && oneToOne.value("mappedBy") == null)) {
            this.type = Type.Relation;
            this.relationClass = entityField.descriptor.substring(1, entityField.descriptor.length() - 1).replace('/', '.');
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
        AnnotationInstance column = field.annotation(DOTNAME_COLUMN);
        if (column != null && column.value("nullable") != null && !column.value("nullable").asBoolean()) {
            validation.add(NotEmpty.class);
            help = "This field is required";
        }
        this.entityField = entityField;
    }

    public String getClassName() {
        return entityField.descriptor.substring(1, entityField.descriptor.length() - 1).replace('/', '.');
    }
}
