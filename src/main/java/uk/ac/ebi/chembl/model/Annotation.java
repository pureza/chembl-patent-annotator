package uk.ac.ebi.chembl.model;

/**
 * Represents an annotation generated by an annotator
 */
public class Annotation {

    /** Maximum length of a term. Bigger terms are ignored. */
    public static final int MAX_TERM_LENGTH = 127;

    /** The type of the annotated entity */
    private final String type;

    /** The name of the annotated entity */
    private final String name;

    /** The term that was found in the text */
    private final String term;

    /** Starting position of the term within the text */
    private final int start;

    /** Ending position of the term within the text */
    private final int end;


    public Annotation(String type, String name, String term, int start, int end) {
        this.type = type;
        this.name = name;
        this.term = term;
        this.start = start;
        this.end = end;
    }


    public String getType() {
        return this.type;
    }


    public String getName() {
        return this.name;
    }


    public String getTerm() {
        return this.term;
    }


    public int getStart() {
        return this.start;
    }


    public int getEnd() {
        return this.end;
    }


    public String toString() {
        return "Annotation{type=" + this.type + ", name=\'" + this.name + '\'' + ", term=\'" + this.term + '\'' + ", start=" + this.start + ", end=" + this.end + '}';
    }
}