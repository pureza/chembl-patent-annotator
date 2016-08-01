package uk.ac.ebi.chembl.model;


/**
 * A patent field
 */
public enum Field {
    DESCRIPTION(1),
    CLAIMS(2),
    ABSTRACT(3),
    TITLE(4),
    CITATIONS(16);

    /** Field id. Must be the same as the field database table */
    private int id;

    Field(int id) {
        this.id = id;
    }


    public int id() {
        return id;
    }
}
