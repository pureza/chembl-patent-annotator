package uk.ac.ebi.chembl.storage.dao;

/**
 * Holds a specific Ensembl Peptide id and the UniProt accession to which it
 * corresponds
 */
public class EnsemblPeptideUniProt {

    /** The Ensembl Peptide id */
    private final String ensemblPeptideId;

    /** The UniProt accession */
    private final String uniprotAcc;

    /** The Ensembl release at which this mapping was taken from */
    private final String ensemblRelease;


    public EnsemblPeptideUniProt(String ensemblPeptideId, String uniprotAcc, String ensemblRelease) {
        this.ensemblPeptideId = ensemblPeptideId;
        this.uniprotAcc = uniprotAcc;
        this.ensemblRelease = ensemblRelease;
    }


    public String getEnsemblPeptideId() {
        return ensemblPeptideId;
    }


    public String getUniprotAcc() {
        return uniprotAcc;
    }


    public String getEnsemblRelease() {
        return ensemblRelease;
    }
}
