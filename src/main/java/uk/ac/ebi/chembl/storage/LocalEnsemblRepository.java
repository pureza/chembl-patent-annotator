package uk.ac.ebi.chembl.storage;


import org.skife.jdbi.v2.sqlobject.CreateSqlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.chembl.storage.dao.EnsemblPeptideUniProt;
import uk.ac.ebi.chembl.storage.dao.LocalEnsemblDao;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for the local Ensembl Peptide id to UniProt accession mapping
 */
public abstract class LocalEnsemblRepository {

    private Logger logger = LoggerFactory.getLogger(getClass().getSuperclass());


    @CreateSqlObject
    protected abstract LocalEnsemblDao dao();


    /**
     * The Ensembl release that was used in the last mapping
     */
    public String retrieveLatestRelease() {
        return dao().retrieveLatestRelease();
    }


    /**
     * Persists the given mapping to the database
     */
    public void save(List<EnsemblPeptideUniProt> mapping) {
        // Clear the previous mapping, if any
        dao().clear();

        // Decompose the mapping list into lists with its components
        List<String> ensemblPeptideIds = new ArrayList<>();
        List<String> uniProtAccs = new ArrayList<>();
        List<String> ensemblReleases = new ArrayList<>();
        mapping.forEach(xref -> {
            ensemblPeptideIds.add(xref.getEnsemblPeptideId());
            uniProtAccs.add(xref.getUniprotAcc());
            ensemblReleases.add(xref.getEnsemblRelease());
        });

        // Save the new mapping
        dao().saveBatch(ensemblPeptideIds, uniProtAccs, ensemblReleases);
    }
}


