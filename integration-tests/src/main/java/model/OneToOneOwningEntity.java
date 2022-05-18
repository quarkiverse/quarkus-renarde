package model;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class OneToOneOwningEntity extends PanacheEntity {
    // we own this
    @OneToOne
    public ExampleEntity exampleEntity;
}
