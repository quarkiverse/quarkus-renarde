package io.quarkiverse.renarde.transporter;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import tools.jackson.databind.module.SimpleModule;

public interface EntityTransporter {
    Class<? extends PanacheEntityBase>[] sortedEntityTypes();

    void addDeserializers(SimpleModule module, InstanceResolver resolver);

    void addSerializers(SimpleModule module, ValueTransformer transformer);

    Class<? extends PanacheEntityBase> getEntityClass(String type);

    PanacheEntityBase instantiate(String type);
}
