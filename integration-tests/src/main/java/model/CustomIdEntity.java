package model;

import java.sql.Blob;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

import io.quarkiverse.renarde.jpa.NamedBlob;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class CustomIdEntity extends PanacheEntityBase {
    @Id
    public String identifiant;

    @Lob
    public byte[] arrayBlob;
    @Lob
    public Blob sqlBlob;
    @Embedded
    public NamedBlob namedBlob;

    @ManyToOne
    public CustomIdOneToManyEntity manyToOne;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + identifiant + ">";
    }
}
