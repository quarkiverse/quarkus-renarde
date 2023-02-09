package model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class ManyToOneEntity extends PanacheEntity {
    @ManyToOne
    public ExampleEntity manyToOne;
}
