package model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Enumerated;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class ExampleEntity extends PanacheEntity {
    public String string;
    public boolean primitiveBoolean;
    public byte primitiveByte;
    public short primitiveShort;
    public int primitiveInt;
    public long primitiveLong;
    public float primitiveFloat;
    public double primitiveDouble;
    public char primitiveChar;
    @Enumerated
    public ExampleEnum enumeration;
    public Date date;
}
