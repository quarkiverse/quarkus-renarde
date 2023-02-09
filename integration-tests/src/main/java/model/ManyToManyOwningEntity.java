package model;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;

@Entity
public class ManyToManyOwningEntity extends PanacheEntity {
    @ManyToMany
    public List<ExampleEntity> manyToMany;
}
