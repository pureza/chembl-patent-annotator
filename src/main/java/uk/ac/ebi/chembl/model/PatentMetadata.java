package uk.ac.ebi.chembl.model;

import java.io.Serializable;
import java.util.Date;


/**
 * The patent metadata
 */
public class PatentMetadata implements Serializable {

    /** Database Patent ID */
    private final long id;

    /** Patent number (i.e., US-20160074186-A1) */
    private final String patentNumber;

    /** Publication date */
    private final Date publicationDate;


    public PatentMetadata(long id, String patentNumber, Date publicationDate) {
        this.id = id;
        this.patentNumber = patentNumber;
        this.publicationDate = publicationDate;
    }


    public long getId() {
        return id;
    }


    public String getPatentNumber() {
        return patentNumber;
    }


    public Date getPublicationDate() {
        return publicationDate;
    }


    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", patentNumber='" + patentNumber + '\'' +
                ", publicationDate=" + publicationDate +
                '}';
    }
}
