package uk.ac.ebi.chembl.storage.dao;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import uk.ac.ebi.chembl.model.IdgAnnotation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * Auxiliar ResultSetMapper to map database rows to IDGAnnotation
 * instances
 */
public class IdgAnnotationMapper implements ResultSetMapper<IdgAnnotation> {

    @Override
    public IdgAnnotation map(int index, ResultSet rs, StatementContext ctx) throws SQLException {
        String entityName = rs.getString("entity_name");
        String uniProtAccession = rs.getString("uniprot_acc");
        String targetName = rs.getString("target_name");
        String developmentLevel = rs.getString("development_level");
        String targetFamily = rs.getString("target_family");
        String patentNumber = rs.getString("patent_number");
        Date publicationDate = rs.getDate("publication_date");

        int totalFrequency = rs.getInt("total_frequency");
        int descriptionFrequency = rs.getInt("description");
        int claimsFrequency = rs.getInt("clms");
        int abstractFrequency = rs.getInt("abst");
        int titleFrequency = rs.getInt("ttl");
        String termsFrequency = rs.getString("term_frequency");

        return new IdgAnnotation(entityName, uniProtAccession, targetName, developmentLevel, targetFamily, patentNumber,
                publicationDate, totalFrequency, descriptionFrequency, claimsFrequency, abstractFrequency,
                titleFrequency, termsFrequency);
    }
}
