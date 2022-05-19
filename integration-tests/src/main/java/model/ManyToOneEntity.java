package model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class ManyToOneEntity extends PanacheEntity {
    @ManyToOne
    public ExampleEntity manyToOne;
}
