package model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Enumerated;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class ExampleEntity extends PanacheEntity {
    public String string;
    public boolean bool;
    @Enumerated
    public ExampleEnum enumeration;
    public Date date;
}
