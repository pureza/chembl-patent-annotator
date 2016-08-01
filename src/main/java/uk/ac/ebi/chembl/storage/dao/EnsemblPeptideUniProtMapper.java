package uk.ac.ebi.chembl.storage.dao;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Auxiliar ResultSetMapper to map database rows to EnsemblPeptideUniProt
 * instances
 */
public class EnsemblPeptideUniProtMapper implements ResultSetMapper<EnsemblPeptideUniProt> {

    @Override
    public EnsemblPeptideUniProt map(int index, ResultSet rs, StatementContext ctx) throws SQLException {
        String ensemblPeptideId = rs.getString("ensembl_peptide_id");
        String uniprotAcc = rs.getString("uniprot_acc");
        String ensemblRelease = rs.getString("ensembl_release");
        return new EnsemblPeptideUniProt(ensemblPeptideId, uniprotAcc, ensemblRelease);
    }
}
