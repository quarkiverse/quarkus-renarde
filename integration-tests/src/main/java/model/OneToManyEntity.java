package model;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

@Entity
public class OneToManyEntity extends PanacheEntity {
    // not owning
    @OneToMany(mappedBy = "manyToOne")
    public List<ExampleEntity> oneToMany;
}
