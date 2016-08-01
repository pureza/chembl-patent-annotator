package uk.ac.ebi.chembl.storage.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;

import java.util.List;


/**
 * Data Access Object for the local Ensembl Peptide id to UniProt accession
 * mapping
 */
@RegisterMapper(EnsemblPeptideUniProtMapper.class)
@UseStringTemplate3StatementLocator
public interface LocalEnsemblDao {

    /**
     * The Ensembl release that was used in the last mapping
     */
    @SqlQuery("SELECT max(ensembl_release) FROM ensembl_peptide_to_uniprot")
    String retrieveLatestRelease();


    /**
     * Persists a new mapping into the database
     */
    @SqlBatch("INSERT INTO ensembl_peptide_to_uniprot (ensembl_peptide_id, uniprot_acc, ensembl_release) " +
            "       VALUES (:ensemblPeptideId, :uniProtAcc, :ensemblRelease)")
    void saveBatch(@Bind("ensemblPeptideId") List<String> ensemblPeptideIds,
                   @Bind("uniProtAcc") List<String> uniProtAccs,
                   @Bind("ensemblRelease") List<String> ensemblReleases);


    /**
     * Deletes the previous mapping
     */
    @SqlUpdate("DELETE FROM ensembl_peptide_to_uniprot")
    void clear();
}
