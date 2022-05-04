package io.quarkiverse.renarde.backoffice.deployment;

import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;

import io.quarkiverse.renarde.util.JavaExtensions;
import io.quarkus.panache.common.deployment.EntityField;

public class ModelField {

    private static final DotName DOTNAME_MANYTOONE = DotName.createSimple(ManyToOne.class.getName());
    private static final DotName DOTNAME_ONETOMANY = DotName.createSimple(OneToMany.class.getName());
    private static final DotName DOTNAME_ONETOONE = DotName.createSimple(OneToOne.class.getName());
    private static final DotName DOTNAME_ENUMERATED = DotName.createSimple(Enumerated.class.getName());

    // For views
    public String name;
    public String label;
    public String type = "text";
    public String relationClass;

    // For processor
    public EntityField entityField;

    public ModelField(EntityField entityField, String entityClass, IndexView index) {
        this.name = entityField.name;
        this.label = JavaExtensions.capitalised(this.name);
        ClassInfo classInfo = index.getClassByName(DotName.createSimple(entityClass));
        FieldInfo field = classInfo.field(entityField.name);
        AnnotationInstance oneToOne = field.annotation(DOTNAME_ONETOONE);
        if (entityField.descriptor.equals("J"))
            this.type = "number";
        else if (entityField.descriptor.equals("Z"))
            this.type = "checkbox";
        else if (entityField.descriptor.equals("Ljava/util/Date;"))
            this.type = "datetime-local";
        else if (field.hasAnnotation(DOTNAME_ENUMERATED)) {
            this.type = "enum";
        } else if (field.hasAnnotation(DOTNAME_ONETOMANY)) {
            this.type = "multi-relation";
            this.relationClass = field.type().asParameterizedType().arguments().get(0).name().toString();
        } else if (oneToOne != null
                && oneToOne.value("mappedBy") != null) {
            // actually we may want to support this in the future too?
            this.type = "ignore";
        } else if (field.hasAnnotation(DOTNAME_MANYTOONE)
                || (oneToOne != null
                        && oneToOne.value("mappedBy") == null)) {
            this.type = "relation";
            this.relationClass = entityField.descriptor.substring(1, entityField.descriptor.length() - 1).replace('/', '.');
        } else {
            // see if we can find what to do with it
            ClassInfo fieldClassInfo = index.getClassByName(field.type().name());
            System.err.println("Unknown field type: " + field.type() + " classinfo: " + fieldClassInfo);
            if (fieldClassInfo != null) {
                if (fieldClassInfo.isEnum()) {
                    this.type = "enum";
                }
            }
        }
        this.entityField = entityField;
    }
}
