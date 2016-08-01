package uk.ac.ebi.chembl.model;


import java.util.Objects;


/**
 * A biological entity
 */
public class BioEntity {

    /** The entity's unique identifier */
    private final long id;

    /** The entity's type */
    private final String type;

    /** The entity name */
    private final String name;

    public BioEntity(long id, String type, String name) {
        this.id = id;
        this.type = type;
        this.name = name;
    }


    public long getId() {
        return id;
    }


    public String getType() {
        return type;
    }


    public String getName() {
        return name;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BioEntity bioEntity = (BioEntity) o;
        return id == bioEntity.id &&
                Objects.equals(type, bioEntity.type) &&
                Objects.equals(name, bioEntity.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, name);
    }
}
