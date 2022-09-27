package model;

import java.sql.Blob;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

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
    public LocalDate localDate;
    // seems like a reserved word
    @Column(name = "somethingLocalTime")
    public LocalTime localTime;
    public LocalDateTime localDateTime;

    @Lob
    public byte[] arrayBlob;
    @Lob
    public Blob sqlBlob;

    // not owning
    @OneToOne(mappedBy = "exampleEntity")
    public OneToOneOwningEntity oneToOneNotOwning;

    // owning
    @OneToOne
    public OneToOneNotOwningEntity oneToOneOwning;

    // not owning
    @OneToMany(mappedBy = "manyToOne")
    public List<ManyToOneEntity> oneToMany;

    // owning
    @ManyToOne
    public OneToManyEntity manyToOne;

    @ManyToMany
    public List<ManyToManyNotOwningEntity> manyToManyOwning;

    @ManyToMany(mappedBy = "manyToMany")
    public List<ManyToManyOwningEntity> manyToManyNotOwning;

    @Lob
    public String lobString;

    @Column(nullable = false)
    public String requiredString;
}
