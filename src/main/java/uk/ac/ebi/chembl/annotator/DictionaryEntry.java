package uk.ac.ebi.chembl.annotator;

/**
 * An entry in the annotator's dictionary
 *
 * This entry is read directly from the dictionary files before being persisted
 * to the database.
 */
public class DictionaryEntry {

    /** The type of the entity represented by this entry (e.g., HUMAN_GENE) */
    private final String type;

    /** The name of the entity represented by this entry (e.g., ENSP00000327545) */
    private final String name;


    public DictionaryEntry(String type, String name) {
        this.name = name;
        this.type = type;
    }


    public String getType() {
        return type;
    }


    public String getName() {
        return name;
    }
}
