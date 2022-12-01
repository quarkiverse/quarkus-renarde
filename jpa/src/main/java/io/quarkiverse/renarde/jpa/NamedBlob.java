package io.quarkiverse.renarde.jpa;

import java.sql.Blob;
import java.sql.SQLException;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Lob;

/**
 * You can use this in your entity to represent named blobs, don't forget to use {@link Embedded} on
 * you field.
 */
@Embeddable
public class NamedBlob {
    /**
     * This blob's file name.
     */
    public String name;
    /**
     * The blob's contents.
     */
    @Lob
    public Blob contents;
    /**
     * The blob's mime type.
     */
    public String mimeType;
    // FIXME: charset? only useful for text blobs, but still

    /**
     * Returns this Blob's length. Not very useful, except for the backoffice template.
     *
     * @return this Blob's length;
     */
    public long length() {
        try {
            return contents != null ? contents.length() : 0;
        } catch (SQLException e) {
            return 0;
        }
    }
}
