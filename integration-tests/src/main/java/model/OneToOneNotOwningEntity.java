package model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

@Entity
public class OneToOneNotOwningEntity extends PanacheEntity {
    // we don't own this
    @OneToOne(mappedBy = "oneToOneOwning")
    public ExampleEntity exampleEntity;
}
