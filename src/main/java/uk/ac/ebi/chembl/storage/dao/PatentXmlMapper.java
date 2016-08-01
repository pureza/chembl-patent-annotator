package uk.ac.ebi.chembl.storage.dao;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import uk.ac.ebi.chembl.model.PatentXml;

import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * Maps ResultSet rows into PatentXml instances
 */
public class PatentXmlMapper implements ResultSetMapper<PatentXml> {
    @Override
    public PatentXml map(int index, ResultSet rs, StatementContext ctx) throws SQLException {
        String patentNumber = rs.getString("patent_number");
        String content = rs.getString("content");
        return new PatentXml(patentNumber, content);
    }
}
