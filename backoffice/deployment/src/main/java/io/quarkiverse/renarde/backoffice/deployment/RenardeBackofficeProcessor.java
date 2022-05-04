package io.quarkiverse.renarde.backoffice.deployment;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.persistence.Entity;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.backoffice.BackUtil;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.HibernateMetamodelForFieldAccessBuildItem;
import io.quarkus.qute.Engine;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.Variant;
import io.quarkus.qute.runtime.TemplateProducer;
import io.quarkus.resteasy.reactive.spi.GeneratedJaxRsResourceBuildItem;
import io.quarkus.resteasy.reactive.spi.GeneratedJaxRsResourceGizmoAdaptor;
import io.smallrye.common.annotation.Blocking;

public class RenardeBackofficeProcessor {

    public static final String URI_PREFIX = "/_renarde/backoffice";
    public static final String PACKAGE_PREFIX = "rest._renarde.backoffice";

    public enum Mode {
        EDIT,
        CREATE
    }

    private static final DotName DOTNAME_ENTITY = DotName.createSimple(Entity.class.getName());

    @BuildStep
    public void processModel(HibernateMetamodelForFieldAccessBuildItem metamodel,
            CombinedIndexBuildItem index,
            BuildProducer<GeneratedResourceBuildItem> output,
            BuildProducer<GeneratedJaxRsResourceBuildItem> jaxrsOutput) {
        Engine engine = Engine.builder()
                .addDefaults()
                .addValueResolver(new ReflectionValueResolver())
                .addLocator(new TemplateLocator() {
                    @Override
                    public Optional<TemplateLocation> locate(String id) {
                        return Optional.of(new TemplateLocation() {

                            @Override
                            public Reader read() {
                                return new InputStreamReader(
                                        RenardeBackofficeProcessor.class.getClassLoader()
                                                .getResourceAsStream("/templates/" + id),
                                        StandardCharsets.UTF_8);
                            }

                            @Override
                            public Optional<Variant> getVariant() {
                                return Optional.empty();
                            }

                        });
                    }
                }).build();

        generateAllController(jaxrsOutput);

        List<String> entities = new ArrayList<>();
        for (String entity : metamodel.getMetamodelInfo().getEntitiesWithPublicFields()) {
            ClassInfo classInfo = index.getIndex().getClassByName(DotName.createSimple(entity));
            // skip mapped superclasses
            if (classInfo.classAnnotation(DOTNAME_ENTITY) == null)
                continue;
            EntityModel entityModel = metamodel.getMetamodelInfo().getEntityModel(entity);
            String simpleName = entityModel.name;
            int nameLastDot = simpleName.lastIndexOf('.');
            if (nameLastDot != -1)
                simpleName = simpleName.substring(nameLastDot + 1);
            entities.add(simpleName);

            // collect fields
            List<ModelField> fields = new ArrayList<>();
            for (Entry<String, EntityField> entry : entityModel.fields.entrySet()) {
                fields.add(new ModelField(entry.getValue(), entity, index.getIndex()));
            }

            generateEntityController(entity, simpleName, fields, jaxrsOutput);

            TemplateInstance indexTemplate = engine.getTemplate("entity-index.qute").instance();
            indexTemplate.data("entity", simpleName);
            render(output, indexTemplate, simpleName + "/index.html");

            TemplateInstance editTemplate = engine.getTemplate("entity-edit.qute").instance();
            editTemplate.data("entity", simpleName);
            editTemplate.data("fields", fields);
            render(output, editTemplate, simpleName + "/edit.html");

            TemplateInstance createTemplate = engine.getTemplate("entity-create.qute").instance();
            createTemplate.data("entity", simpleName);
            createTemplate.data("fields", fields);
            render(output, createTemplate, simpleName + "/create.html");
        }

        TemplateInstance template = engine.getTemplate("index.qute").instance();
        template.data("entities", entities);
        render(output, template, "index.html");
    }

    private void render(BuildProducer<GeneratedResourceBuildItem> output, TemplateInstance template, String templateId) {
        output.produce(new GeneratedResourceBuildItem("templates/" + URI_PREFIX + "/" + templateId,
                template.render().getBytes(StandardCharsets.UTF_8)));
    }

    private void generateEntityController(String entityClass, String simpleName,
            List<ModelField> fields, BuildProducer<GeneratedJaxRsResourceBuildItem> jaxrsOutput) {
        // TODO: hand it off to RenardeProcessor to generate annotations and uri methods
        // TODO: generate templateinstance native build item or what?
        // TODO: authenticated
        try (ClassCreator c = ClassCreator.builder()
                .classOutput(new GeneratedJaxRsResourceGizmoAdaptor(jaxrsOutput)).className(
                        PACKAGE_PREFIX + "." + simpleName + "Controller")
                .superClass(Controller.class).build()) {
            c.addAnnotation(Blocking.class.getName());
            c.addAnnotation(RequestScoped.class.getName());
            c.addAnnotation(Path.class).addValue("value", URI_PREFIX + "/" + simpleName);

            try (MethodCreator m = c.getMethodCreator("index", TemplateInstance.class)) {
                m.addAnnotation(Path.class).addValue("value", "index");
                m.addAnnotation(GET.class);
                m.addAnnotation(Produces.class).addValue("value", new String[] { MediaType.TEXT_HTML });

                //              Template template = Arc.container().instance(TemplateProducer.class).get().getInjectableTemplate("_renarde/backoffice/index");
                //              TemplateInstance instance = template.instance();
                //              instance = template.data("entities", Todo.listAll());
                //              return instance;

                ResultHandle entities = m.invokeStaticMethod(MethodDescriptor.ofMethod(entityClass, "listAll", List.class));

                ResultHandle instance = getTemplateInstance(m, URI_PREFIX + "/" + simpleName + "/index");
                instance = m.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(TemplateInstance.class, "data", TemplateInstance.class, String.class,
                                Object.class),
                        instance, m.load("entities"), entities);
                m.returnValue(instance);
            }

            try (MethodCreator m = c.getMethodCreator("edit", TemplateInstance.class, Long.class)) {
                editOrCreateView(m, entityClass, simpleName, fields, Mode.EDIT);
            }

            editOrCreateAction(c, entityClass, simpleName, fields, Mode.EDIT);

            try (MethodCreator m = c.getMethodCreator("create", TemplateInstance.class)) {
                editOrCreateView(m, entityClass, simpleName, fields, Mode.CREATE);
            }

            editOrCreateAction(c, entityClass, simpleName, fields, Mode.CREATE);

            deleteAction(c, entityClass, simpleName);
        }
    }

    //    @POST
    //    @Transactional
    //    @Path("delete/{id}")
    //    public void delete(@RestPath("id") Long id) {
    //        User user = User.findById(id);
    //        notFoundIfNull(user);
    //        user.delete();
    //        flash("message", "Deleted: "+user);
    //        //index();
    //        seeOther("/_renarde/backoffice/index");
    //    }
    private void deleteAction(ClassCreator c, String entityClass, String simpleName) {
        try (MethodCreator m = c.getMethodCreator("delete", void.class, Long.class)) {
            m.getParameterAnnotations(0).addAnnotation(RestPath.class).addValue("value", "id");
            m.addAnnotation(Path.class).addValue("value", "delete/{id}");
            m.addAnnotation(POST.class);
            m.addAnnotation(Transactional.class);

            AssignableResultHandle variable = findEntityById(m, entityClass);
            m.invokeVirtualMethod(MethodDescriptor.ofMethod(entityClass, "delete", void.class), variable);
            ResultHandle message = m.newInstance(MethodDescriptor.ofConstructor(StringBuilder.class, String.class),
                    m.load("Deleted: "));
            message = m.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(StringBuilder.class, "append", StringBuilder.class, Object.class), message,
                    variable);
            message = m.invokeVirtualMethod(MethodDescriptor.ofMethod(StringBuilder.class, "toString", String.class),
                    message);
            m.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(Controller.class, "flash", void.class, String.class, Object.class),
                    m.getThis(), m.load("message"), message);
            m.invokeVirtualMethod(MethodDescriptor.ofMethod(Controller.class, "seeOther", Response.class, String.class),
                    m.getThis(), m.load(URI_PREFIX + "/" + simpleName + "/index"));
        }
    }

    // @Path("edit/{id}")
    // @POST
    // @Transactional
    // public void edit(@RestPath("id") Long id,
    //                    @RestForm("task") String task,
    //                    @RestForm("done") String done,
    //                    @RestForm("doneDate") String doneDate,
    //                    @RestForm("owner") String owner) {
    //   Todo todo = Todo.findById(id);
    //   notFoundIfNull(todo);
    //   todo.setTask(BackUtil.stringField(task));
    //   todo.setDone(BackUtil.booleanField(done));
    //   todo.setDoneDate(BackUtil.dateField(doneDate));
    //   todo.setOwner(BackUtil.isSet(owner) ? User.findById(Long.valueOf(owner)) : null);
    //   flash("message", "Updated: "+todo);
    //   // edit(id);
    //   seeOther("/_renarde/backoffice/edit/"+id);
    // }

    // @Path("create")
    // @POST
    // @Transactional
    // public void create(@RestForm("task") String task,
    //                    @RestForm("done") String done,
    //                    @RestForm("doneDate") String doneDate,
    //                    @RestForm("owner") String owner) {
    //   Todo todo = new Todo();
    //   todo.setTask(BackUtil.stringField(task));
    //   todo.setDone(BackUtil.booleanField(done));
    //   todo.setDoneDate(BackUtil.dateField(doneDate));
    //   todo.setOwner(BackUtil.isSet(owner) ? User.findById(Long.valueOf(owner)) : null);
    //   todo.persist();
    //   flash("message", "Created: "+todo);
    //   // edit(id);
    //   seeOther("/_renarde/backoffice/edit/"+id);
    // }
    private void editOrCreateAction(ClassCreator c, String entityClass, String simpleName, List<ModelField> fields, Mode mode) {
        Class[] editParams;
        int offset = 0;
        if (mode == Mode.EDIT) {
            editParams = new Class[fields.size() + 1];
            editParams[0] = Long.class;
            offset = 1;
        } else {
            editParams = new Class[fields.size()];
        }
        for (int i = 0; i < fields.size(); i++) {
            editParams[i + offset] = String.class;
        }
        try (MethodCreator m = c.getMethodCreator(mode == Mode.CREATE ? "create" : "edit", void.class, editParams)) {
            if (mode == Mode.EDIT) {
                m.getParameterAnnotations(0).addAnnotation(RestPath.class).addValue("value", "id");
            }
            for (int i = 0; i < fields.size(); i++) {
                m.getParameterAnnotations(i + offset).addAnnotation(RestForm.class).addValue("value", fields.get(i).name);
            }
            m.addAnnotation(Path.class).addValue("value", mode == Mode.CREATE ? "create" : "edit/{id}");
            m.addAnnotation(POST.class);
            m.addAnnotation(Transactional.class);

            AssignableResultHandle variable;
            if (mode == Mode.EDIT) {
                variable = findEntityById(m, entityClass);
            } else {
                String entityTypeDescriptor = "L" + entityClass.replace('.', '/') + ";";
                variable = m.createVariable(entityTypeDescriptor);
                m.assign(variable, m.newInstance(MethodDescriptor.ofConstructor(entityTypeDescriptor)));
            }
            for (int i = 0; i < fields.size(); i++) {
                ModelField field = fields.get(i);
                ResultHandle value = null;
                if (field.entityField.descriptor.equals("Ljava/lang/String;")) {
                    value = m.invokeStaticMethod(
                            MethodDescriptor.ofMethod(BackUtil.class, "stringField", String.class, String.class),
                            m.getMethodParam(i + offset));
                } else if (field.entityField.descriptor.equals("Z")) {
                    value = m.invokeStaticMethod(
                            MethodDescriptor.ofMethod(BackUtil.class, "booleanField", boolean.class, String.class),
                            m.getMethodParam(i + offset));
                } else if (field.entityField.descriptor.equals("J")) {
                    value = m.invokeStaticMethod(
                            MethodDescriptor.ofMethod(BackUtil.class, "longField", long.class, String.class),
                            m.getMethodParam(i + offset));
                } else if (field.entityField.descriptor.equals("Ljava/util/Date;")) {
                    value = m.invokeStaticMethod(
                            MethodDescriptor.ofMethod(BackUtil.class, "dateField", Date.class, String.class),
                            m.getMethodParam(i + offset));
                } else if (field.type.equals("enum")) {
                    value = m.invokeStaticMethod(
                            MethodDescriptor.ofMethod(BackUtil.class, "enumField", Enum.class, Class.class, String.class),
                            m.loadClass(field.entityField.descriptor),
                            m.getMethodParam(i + offset));
                    value = m.checkCast(value, field.entityField.descriptor);
                } else if (field.type.equals("multi-relation")) {
                    // FIXME
                } else if (field.type.equals("ignore")) {
                    continue;
                } else if (field.type.equals("relation")) {
                    BranchResult branch = m.ifTrue(m.invokeStaticMethod(
                            MethodDescriptor.ofMethod(BackUtil.class, "isSet", boolean.class, String.class),
                            m.getMethodParam(i + offset)));
                    AssignableResultHandle valueVar = m.createVariable(field.entityField.descriptor);
                    try (BytecodeCreator tb = branch.trueBranch()) {
                        value = tb.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Long.class, "valueOf", Long.class, String.class),
                                tb.getMethodParam(i + offset));
                        value = tb.invokeStaticMethod(
                                MethodDescriptor.ofMethod(field.entityField.descriptor, "findById", PanacheEntityBase.class,
                                        Object.class),
                                value);
                        value = tb.checkCast(value, field.entityField.descriptor);
                        tb.assign(valueVar, value);
                    }
                    try (BytecodeCreator fb = branch.falseBranch()) {
                        fb.assign(valueVar, fb.loadNull());
                    }
                    value = valueVar;
                } else {
                    throw new RuntimeException("Don't know what to do with field of type " + field.entityField.descriptor
                            + " from field " + simpleName + "." + field.name);
                }
                // FIXME: temporary
                if (value != null)
                    m.invokeVirtualMethod(MethodDescriptor.ofMethod(entityClass, field.entityField.getSetterName(), void.class,
                            field.entityField.descriptor), variable, value);
            }
            if (mode == Mode.CREATE) {
                m.invokeVirtualMethod(MethodDescriptor.ofMethod(entityClass, "persist", void.class), variable);
            }
            ResultHandle message = m.newInstance(MethodDescriptor.ofConstructor(StringBuilder.class, String.class),
                    m.load(mode == Mode.CREATE ? "Created: " : "Updated: "));
            message = m.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(StringBuilder.class, "append", StringBuilder.class, Object.class), message,
                    variable);
            message = m.invokeVirtualMethod(MethodDescriptor.ofMethod(StringBuilder.class, "toString", String.class),
                    message);
            m.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(Controller.class, "flash", void.class, String.class, Object.class),
                    m.getThis(), m.load("message"), message);
            String uriTarget = URI_PREFIX + "/" + simpleName + (mode == Mode.CREATE ? "/create" : "/edit/");
            ResultHandle uri = m.newInstance(MethodDescriptor.ofConstructor(StringBuilder.class, String.class),
                    m.load(uriTarget));
            if (mode == Mode.EDIT) {
                uri = m.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(StringBuilder.class, "append", StringBuilder.class, Object.class), uri,
                        m.getMethodParam(0));
            }
            uri = m.invokeVirtualMethod(MethodDescriptor.ofMethod(StringBuilder.class, "toString", String.class), uri);
            m.invokeVirtualMethod(MethodDescriptor.ofMethod(Controller.class, "seeOther", Response.class, String.class),
                    m.getThis(), uri);
        }
    }

    // @GET
    // @Produces(html)
    // @Path("edit/{id}")
    // public TemplateInstance edit(@RestPath("id") Long id) {
    //   Todo todo = Todo.findById(id);
    //   notFoundIfNull(todo);
    //   return Templates.edit(todo, BackUtil.entityValues(User.listAll()));
    // }

    // @GET
    // @Produces(html)
    // @Path("create")
    // public TemplateInstance create() {
    //   return Templates.create(BackUtil.entityValues(User.listAll()));
    // }
    private void editOrCreateView(MethodCreator m, String entityClass, String simpleName, List<ModelField> fields, Mode mode) {
        if (mode == Mode.EDIT) {
            m.getParameterAnnotations(0).addAnnotation(RestPath.class).addValue("value", "id");
            m.addAnnotation(Path.class).addValue("value", "edit/{id}");
        } else {
            m.addAnnotation(Path.class).addValue("value", "create");
        }
        m.addAnnotation(GET.class);
        m.addAnnotation(Produces.class).addValue("value", new String[] { MediaType.TEXT_HTML });

        ResultHandle instance = getTemplateInstance(m,
                URI_PREFIX + "/" + simpleName + "/" + (mode == Mode.CREATE ? "create" : "edit"));
        if (mode == Mode.EDIT) {
            AssignableResultHandle variable = findEntityById(m, entityClass);

            instance = m.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(TemplateInstance.class, "data", TemplateInstance.class, String.class,
                            Object.class),
                    instance, m.load("entity"), variable);
        }

        for (ModelField field : fields) {
            ResultHandle data = null;
            if (field.type.equals("relation")) {
                ResultHandle list = m
                        .invokeStaticMethod(MethodDescriptor.ofMethod(field.relationClass, "listAll", List.class));
                data = m.invokeStaticMethod(
                        MethodDescriptor.ofMethod(BackUtil.class, "entityValues", Map.class, List.class), list);
            } else if (field.type.equals("enum")) {
                ResultHandle list = m
                        .invokeStaticMethod(
                                MethodDescriptor.ofMethod(field.entityField.descriptor, "values",
                                        "[" + field.entityField.descriptor));
                data = m.invokeStaticMethod(
                        MethodDescriptor.ofMethod(BackUtil.class, "enumValues", Map.class, Enum[].class), list);
            }
            if (data != null) {
                instance = m.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(TemplateInstance.class, "data", TemplateInstance.class, String.class,
                                Object.class),
                        instance, m.load(field.name + "Values"), data);
            }
        }
        m.returnValue(instance);
    }

    private AssignableResultHandle findEntityById(MethodCreator m, String entityClass) {
        ResultHandle entity = m.invokeStaticMethod(
                MethodDescriptor.ofMethod(entityClass, "findById", PanacheEntityBase.class, Object.class),
                m.getMethodParam(0));
        String entityTypeDescriptor = "L" + entityClass.replace('.', '/') + ";";
        AssignableResultHandle variable = m.createVariable(entityTypeDescriptor);
        m.assign(variable, m.checkCast(entity, entityTypeDescriptor));

        m.invokeVirtualMethod(MethodDescriptor.ofMethod(Controller.class, "notFoundIfNull", void.class, Object.class),
                m.getThis(), variable);
        return variable;
    }

    private ResultHandle getTemplateInstance(MethodCreator m, String templateName) {
        ResultHandle container = m
                .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
        ResultHandle instanceHandle = m.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                        Annotation[].class),
                container, m.loadClass(TemplateProducer.class), m.newArray(Annotation.class, 0));
        ResultHandle templateProducer = m
                .checkCast(m.invokeInterfaceMethod(MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class),
                        instanceHandle), TemplateProducer.class);
        ResultHandle template = m.invokeVirtualMethod(MethodDescriptor.ofMethod(TemplateProducer.class,
                "getInjectableTemplate", Template.class, String.class), templateProducer, m.load(templateName));
        return m.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Template.class, "instance", TemplateInstance.class), template);
    }

    private void generateAllController(BuildProducer<GeneratedJaxRsResourceBuildItem> jaxrsOutput) {
        // TODO: hand it off to RenardeProcessor to generate annotations and uri methods
        // TODO: generate templateinstance native build item or what?
        try (ClassCreator c = ClassCreator.builder()
                .classOutput(new GeneratedJaxRsResourceGizmoAdaptor(jaxrsOutput)).className(
                        PACKAGE_PREFIX + ".Index")
                .superClass(Controller.class).build()) {
            c.addAnnotation(Blocking.class.getName());
            c.addAnnotation(RequestScoped.class.getName());
            c.addAnnotation(Path.class).addValue("value", URI_PREFIX);

            try (MethodCreator m = c.getMethodCreator("index", TemplateInstance.class)) {
                m.addAnnotation(Path.class).addValue("value", "index");
                m.addAnnotation(GET.class);
                m.addAnnotation(Produces.class).addValue("value", new String[] { MediaType.TEXT_HTML });

                //              Template template = Arc.container().instance(TemplateProducer.class).get().getInjectableTemplate("Back/index");
                //              TemplateInstance instance = template.instance();
                //              return instance;

                ResultHandle instance = getTemplateInstance(m, URI_PREFIX + "/index");
                m.returnValue(instance);
            }
        }
    }
}
