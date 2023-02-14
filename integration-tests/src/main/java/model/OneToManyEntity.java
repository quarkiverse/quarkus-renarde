package model;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class OneToManyEntity extends PanacheEntity {
    // not owning
    @OneToMany(mappedBy = "manyToOne")
    public List<ExampleEntity> oneToMany;
}
