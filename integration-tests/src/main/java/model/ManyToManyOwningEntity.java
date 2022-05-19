package model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class ManyToManyOwningEntity extends PanacheEntity {
    @ManyToMany
    public List<ExampleEntity> manyToMany;
}
