package uk.ac.ebi.chembl.model;


/**
 * A biological entity type
 */
public class BioType {
    /** The type's unique identifier */
    private final long id;

    /** The type name */
    private final String name;

    /** The annotator which uses this type */
    private final int annotatorId;


    public BioType(long id, String name, int annotatorId) {
        this.id = id;
        this.name = name;
        this.annotatorId = annotatorId;
    }


    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAnnotatorId() {
        return annotatorId;
    }
}
