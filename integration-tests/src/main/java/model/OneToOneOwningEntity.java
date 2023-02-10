package model;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class OneToOneOwningEntity extends PanacheEntity {
    // we own this
    @OneToOne
    public ExampleEntity exampleEntity;
}
