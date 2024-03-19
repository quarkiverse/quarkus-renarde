package model;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class CustomIdOneToManyEntity extends PanacheEntityBase {

    @Id
    public String myid;

    @OneToMany(mappedBy = "manyToOne")
    public List<CustomIdEntity> oneToMany;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + myid + ">";
    }
}
