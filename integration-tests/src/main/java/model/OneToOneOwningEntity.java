package model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

@Entity
public class OneToOneOwningEntity extends PanacheEntity {
    // we own this
    @OneToOne
    public ExampleEntity exampleEntity;
}
