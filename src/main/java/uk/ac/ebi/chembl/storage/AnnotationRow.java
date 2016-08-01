package uk.ac.ebi.chembl.storage;

import java.io.Serializable;

/**
 * Represents an annotation row in the database
 */
public class AnnotationRow implements Serializable {

    /** The patent id */
    private long patentId;

    /** The id of the field inside the patent where the annotation was found */
    private int fieldId;

    /** The rank. Each rank corresponds to a language and we only care about English */
    private int rank;

    /** The id of the biological entity that was identified in the text */
    private long bioEntityId;

    /** The term that was found in the text */
    private String term;

    /** Starting position of the term within the text */
    private int start;

    /** Ending position of the term within the text */
    private int end;


    public AnnotationRow(long patentId, int fieldId, int rank, long bioEntityId, String term, int start, int end) {
        this.patentId = patentId;
        this.fieldId = fieldId;
        this.rank = rank;
        this.bioEntityId = bioEntityId;
        this.term = term;
        this.start = start;
        this.end = end;
    }


    public long getPatentId() {
        return patentId;
    }


    public void setPatentId(long patentId) {
        this.patentId = patentId;
    }


    public int getFieldId() {
        return fieldId;
    }


    public void setFieldId(int fieldId) {
        this.fieldId = fieldId;
    }


    public int getRank() {
        return rank;
    }


    public void setRank(int rank) {
        this.rank = rank;
    }


    public long getBioEntityId() {
        return bioEntityId;
    }


    public void setBioEntityId(long bioEntityId) {
        this.bioEntityId = bioEntityId;
    }


    public String getTerm() {
        return term;
    }


    public void setTerm(String term) {
        this.term = term;
    }


    public int getStart() {
        return start;
    }


    public void setStart(int start) {
        this.start = start;
    }


    public int getEnd() {
        return end;
    }


    public void setEnd(int end) {
        this.end = end;
    }
}