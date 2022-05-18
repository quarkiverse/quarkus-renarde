package model;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class OneToOneNotOwningEntity extends PanacheEntity {
    // we don't own this
    @OneToOne(mappedBy = "oneToOneOwning")
    public ExampleEntity exampleEntity;
}
