package uk.ac.ebi.chembl.model;

/**
 * An IDG Target
 */
public class IdgTarget {

    /** The UniProt accession */
    private final String uniProtAccession;

    /** The target's name */
    private final String name;

    /** Development level */
    private final String developmentLevel;

    /** Target family */
    private final String targetFamily;


    public IdgTarget(String uniProtAccession, String name, String developmentLevel, String targetFamily) {
        this.uniProtAccession = uniProtAccession;
        this.name = name;
        this.developmentLevel = developmentLevel;
        this.targetFamily = targetFamily;
    }


    public String getUniProtAccession() {
        return uniProtAccession;
    }


    public String getName() {
        return name;
    }


    public String getDevelopmentLevel() {
        return developmentLevel;
    }


    public String getTargetFamily() {
        return targetFamily;
    }
}