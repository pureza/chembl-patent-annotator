package uk.ac.ebi.chembl.storage;


import org.skife.jdbi.v2.sqlobject.CreateSqlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.chembl.storage.dao.EnsemblPeptideUniProt;
import uk.ac.ebi.chembl.storage.dao.ExternalEnsemblDao;

import java.util.ArrayList;
import java.util.List;


/**
 * A repository that abstracts the Ensembl database
 */
public abstract class ExternalEnsemblRepository {

    private Logger logger = LoggerFactory.getLogger(getClass().getSuperclass());

    /** Maximum number of Ensembl Peptide ids per batch */
    private final static int BATCH_SIZE = 1000;

    @CreateSqlObject
    protected abstract ExternalEnsemblDao dao();


    /**
     * Maps Ensembl Peptide ids to UniProt accessions using a specific Ensembl
     * release
     */
    public List<EnsemblPeptideUniProt> mapToUniprotAtRelease(List<String> ensemblPeptideIds, String ensemblRelease) {
        List<EnsemblPeptideUniProt> rows = new ArrayList<>();

        // Do this in batches, to avoid "to many items in IN" error
        for (int i = 0; i * BATCH_SIZE < ensemblPeptideIds.size(); i++) {
            int end = Math.min(ensemblPeptideIds.size(), (i + 1) * BATCH_SIZE);
            List<String> batch = ensemblPeptideIds.subList(i * BATCH_SIZE, end);
            rows.addAll(dao().mapToUniprot(batch, ensemblRelease));
        }

        return rows;
    }
}


