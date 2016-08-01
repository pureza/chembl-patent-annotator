package uk.ac.ebi.chembl.model;

import java.util.Objects;

/**
 * The metadata associated with an annotator
 */
public class AnnotatorMetadata {

    /** Annotator's identifier */
    private final int id;

    /** Annotator's name */
    private final String name;


    public AnnotatorMetadata(int id, String name) {
        this.id = id;
        this.name = name;
    }


    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }


    @Override
    public String toString() {
        return name;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnotatorMetadata that = (AnnotatorMetadata) o;
        return id == that.id &&
                Objects.equals(name, that.name);
    }


    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
