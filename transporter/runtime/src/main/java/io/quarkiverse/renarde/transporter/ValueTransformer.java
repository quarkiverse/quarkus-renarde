package io.quarkiverse.renarde.transporter;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@FunctionalInterface
public interface ValueTransformer {

    ValueTransformer IDENTITY_TRANSFORMER = new ValueTransformer() {
        @Override
        public Object transform(Class<? extends PanacheEntityBase> entityType, String attributeName, Object value) {
            return value;
        }
    };

    Object transform(Class<? extends PanacheEntityBase> entityType, String attributeName, Object value);
}
