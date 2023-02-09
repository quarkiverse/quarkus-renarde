package model;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;

@Entity
public class ManyToManyNotOwningEntity extends PanacheEntity {
    @ManyToMany(mappedBy = "manyToManyOwning")
    public List<ExampleEntity> manyToMany;
}
