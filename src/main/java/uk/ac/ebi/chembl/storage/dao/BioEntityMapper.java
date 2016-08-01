package uk.ac.ebi.chembl.storage.dao;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import uk.ac.ebi.chembl.model.BioEntity;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps rows in the bio_entity table to instances of BioEntity
 */
public class BioEntityMapper implements ResultSetMapper<BioEntity> {

    @Override
    public BioEntity map(int i, ResultSet rs, StatementContext statementContext) throws SQLException {
        long id = rs.getLong("bio_entity_id");
        String typeName = rs.getString("type");
        String name = rs.getString("name");

        return new BioEntity(id, typeName, name);
    }
}
