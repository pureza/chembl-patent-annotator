package uk.ac.ebi.chembl.storage.dao;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import uk.ac.ebi.chembl.model.PatentMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * Converts document table rows to PatentMetadata objects
 */
public class PatentMetadataMapper implements ResultSetMapper<PatentMetadata> {

    @Override
    public PatentMetadata map(int index, ResultSet rs, StatementContext ctx) throws SQLException {
        long docId = rs.getLong("patent_id");
        String patentNumber = rs.getString("patent_number");
        Date publicationDate = rs.getDate("publication_date");
        return new PatentMetadata(docId, patentNumber, publicationDate);
    }
}
