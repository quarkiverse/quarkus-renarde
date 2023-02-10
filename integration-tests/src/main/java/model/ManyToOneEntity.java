package model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class ManyToOneEntity extends PanacheEntity {
    @ManyToOne
    public ExampleEntity manyToOne;
}
