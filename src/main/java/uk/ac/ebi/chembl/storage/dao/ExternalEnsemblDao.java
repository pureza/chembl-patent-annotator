package uk.ac.ebi.chembl.storage.dao;

import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.Define;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.List;


/**
 * Data Access Object for the Ensembl database
 */
@RegisterMapper(EnsemblPeptideUniProtMapper.class)
@UseStringTemplate3StatementLocator
public interface ExternalEnsemblDao {

    /**
     * Maps the given Ensembl Peptide ids to UniProt accessions, at a specific
     * Ensembl release
     */
    @SqlQuery("SELECT tl.stable_id as ensembl_peptide_id, xr.dbprimary_acc as uniprot_acc, '<release>' as ensembl_release" +
            "    FROM homo_sapiens_core_<release>.translation tl, homo_sapiens_core_<release>.xref xr, " +
            "         homo_sapiens_core_<release>.object_xref ox, homo_sapiens_core_<release>.external_db ex " +
            "   WHERE tl.translation_id = ox.ensembl_id " +
            "     AND xr.xref_id = ox.xref_id " +
            "     AND ex.external_db_id = xr.external_db_id " +
            "     AND ex.db_name IN ('Uniprot/SWISSPROT') " +
            "     AND xr.info_type = 'DIRECT' " +
            "     AND tl.stable_id IN (<ensemblPeptideIds>)")
    List<EnsemblPeptideUniProt> mapToUniprot(@BindIn("ensemblPeptideIds") List<String> ensemblPeptideIds,
                                             @Define("release") String release);
}
