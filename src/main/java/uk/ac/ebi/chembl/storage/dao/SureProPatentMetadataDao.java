package uk.ac.ebi.chembl.storage.dao;

import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import uk.ac.ebi.chembl.model.PatentMetadata;

import java.util.Iterator;

/**
 * Interfaces with the SurePro database to retrieve the list of all the patents
 */
@RegisterMapper(PatentMetadataMapper.class)
public interface SureProPatentMetadataDao {

    /**
     * Retrieves all patent metadata in the database
     *
     * The patent_id is discarded because our database generates it.
     */
    @SqlQuery("SELECT -1 as patent_id, scpn as patent_number, published as publication_date" +
            "    FROM surechembl_map.schembl_document " +
            "   WHERE life_sci_relevant = 1" +
            "     AND substr(scpn, 1, 2) IN ('EP', 'WO', 'US') ")
    Iterator<PatentMetadata> retrieveAll();


    /**
     * Closes the DAO and releases any allocated database resources
     */
    void close();
}
