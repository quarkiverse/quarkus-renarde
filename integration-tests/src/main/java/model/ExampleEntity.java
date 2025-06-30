package model;

import java.sql.Blob;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.validator.constraints.Length;

import io.quarkiverse.renarde.jpa.NamedBlob;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class ExampleEntity extends PanacheEntity {
    public String string;
    public boolean primitiveBoolean;
    public Boolean wrapperBoolean;
    public byte primitiveByte;
    public short primitiveShort;
    public int primitiveInt;
    public Integer wrapperInt;
    public long primitiveLong;
    public Long wrapperLong;
    public float primitiveFloat;
    public Float wrapperFloat;
    public double primitiveDouble;
    public Double wrapperDouble;
    public char primitiveChar;
    @Enumerated
    public ExampleEnum enumeration;
    public Date date;
    public LocalDate localDate;
    // seems like a reserved word
    @Column(name = "somethingLocalTime")
    public LocalTime localTime;
    public LocalDateTime localDateTime;
    public Timestamp timestamp;

    @Lob
    public byte[] arrayBlob;
    @Lob
    public Blob sqlBlob;
    @Embedded
    public NamedBlob namedBlob;

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

    // jdbc length
    @Column(length = 10000)
    public String longString1;

    // hibernate validation
    @Length(max = 10000)
    public String longString2;

    // jakarta validation
    @Size(max = 10000)
    public String longString3;

    @JdbcTypeCode(Types.LONGVARCHAR)
    public String longString4;

    @Column(nullable = false)
    public String requiredString;

    @JdbcTypeCode(SqlTypes.JSON)
    public List<JsonRecord> jsonRecords;

    public static class JsonRecord {

        private ExampleEnum exampleEnum;
        private int something;

        public JsonRecord() {
        }

        public JsonRecord(ExampleEnum exampleEnum, int something) {
            this.exampleEnum = exampleEnum;
            this.something = something;
        }

        public ExampleEnum getExampleEnum() {
            return exampleEnum;
        }

        public int getSomething() {
            return something;
        }
    }
}
