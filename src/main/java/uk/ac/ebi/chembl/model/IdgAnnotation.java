package uk.ac.ebi.chembl.model;

import java.util.Date;

/**
 * An IDG annotation
 *
 * IDG annotations are annotations of IDG targets.
 */
public class IdgAnnotation {

    /** The name of the annotated biological entity as found by the annotator */
    private final String entityName;

    /** The UniProt accession of the IDG target */
    private final String uniProtAccession;

    /** The target name */
    private final String targetName;

    /** The development level */
    private final String developmentLevel;

    /** The target family */
    private final String targetFamily;

    /** The patent number where the annotation was found */
    private final String patentNumber;

    /** The publication date of the patent */
    private final Date publicationDate;

    /** Number of times the entity appeared in the patent */
    private final int totalFrequency;

    /** Number of times the entity appeared in the description field */
    private final int descriptionFrequency;

    /** Number of times the entity appeared in the claims field */
    private final int claimsFrequency;

    /** Number of times the entity appeared in the abstract field */
    private final int abstractFrequency;

    /** Number of times the entity appeared in the title field */
    private final int titleFrequency;

    /** The actual terms that mapped to this entity and their frequency */
    private final String termsFrequency;


    public IdgAnnotation(String entityName, String uniProtAccession, String targetName, String developmentLevel,
                         String targetFamily, String patentNumber, Date publicationDate, int totalFrequency,
                         int descriptionFrequency, int claimsFrequency, int abstractFrequency, int titleFrequency,
                         String termsFrequency) {
        this.entityName = entityName;
        this.uniProtAccession = uniProtAccession;
        this.targetName = targetName;
        this.developmentLevel = developmentLevel;
        this.targetFamily = targetFamily;
        this.patentNumber = patentNumber;
        this.publicationDate = publicationDate;
        this.totalFrequency = totalFrequency;
        this.descriptionFrequency = descriptionFrequency;
        this.claimsFrequency = claimsFrequency;
        this.abstractFrequency = abstractFrequency;
        this.titleFrequency = titleFrequency;
        this.termsFrequency = termsFrequency;
    }


    public String getEntityName() {
        return entityName;
    }


    public String getUniProtAccession() {
        return uniProtAccession;
    }


    public String getTargetName() {
        return targetName;
    }


    public String getDevelopmentLevel() {
        return developmentLevel;
    }


    public String getTargetFamily() {
        return targetFamily;
    }


    public String getPatentNumber() {
        return patentNumber;
    }


    public Date getPublicationDate() {
        return publicationDate;
    }


    public int getTotalFrequency() {
        return totalFrequency;
    }


    public int getDescriptionFrequency() {
        return descriptionFrequency;
    }


    public int getClaimsFrequency() {
        return claimsFrequency;
    }


    public int getAbstractFrequency() {
        return abstractFrequency;
    }


    public int getTitleFrequency() {
        return titleFrequency;
    }


    public String getTermsFrequency() {
        return termsFrequency;
    }
}
