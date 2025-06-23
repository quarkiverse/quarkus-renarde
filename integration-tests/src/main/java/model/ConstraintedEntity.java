package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class ConstraintedEntity extends PanacheEntity {
    @Column(nullable = false)
    public String nonNullableString;

    @NotEmpty
    public String nonEmptyString;

    @NotNull
    public String nonNullString;

    @NotBlank
    public String nonBlankString;

    @URL
    public String urlString;

    @Length(min = 2, max = 100)
    public String lengthString;

    @Size(min = 2, max = 100)
    public String sizeString;

    @JoinColumn(nullable = false)
    @NotNull
    @ManyToOne
    public RelatedConstraintedEntity relatedEntity;
}
