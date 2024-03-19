package io.quarkiverse.renarde.jpa;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Lob;

import org.hibernate.engine.jdbc.BlobProxy;

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

    // for JPA
    public NamedBlob() {
    }

    public NamedBlob(String name, String mimeType, Blob contents) {
        this.name = name;
        this.mimeType = mimeType;
        this.contents = contents;
    }

    /**
     * Builds a NamedBlob for UTF8-encoded Strings
     */
    public NamedBlob(String name, String mimeType, String contents) {
        this(name, mimeType, contents.getBytes(StandardCharsets.UTF_8));
    }

    public NamedBlob(String name, String mimeType, byte[] contents) {
        this(name, mimeType, BlobProxy.generateProxy(contents));
    }

    public NamedBlob(String name, String mimeType, InputStream contents, long length) {
        this(name, mimeType, BlobProxy.generateProxy(contents, length));
    }

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
