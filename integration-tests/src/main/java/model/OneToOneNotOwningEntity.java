package model;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class OneToOneNotOwningEntity extends PanacheEntity {
    // we don't own this
    @OneToOne(mappedBy = "oneToOneOwning")
    public ExampleEntity exampleEntity;
}
