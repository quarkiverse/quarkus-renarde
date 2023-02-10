package model;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class ManyToManyNotOwningEntity extends PanacheEntity {
    @ManyToMany(mappedBy = "manyToManyOwning")
    public List<ExampleEntity> manyToMany;
}
