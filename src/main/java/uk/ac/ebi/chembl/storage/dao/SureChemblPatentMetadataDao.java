package uk.ac.ebi.chembl.storage.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import uk.ac.ebi.chembl.model.PatentMetadata;

import java.util.Date;
import java.util.Iterator;

/**
 * Interfaces with the SureChembl database to retrieve the most
 * recently added patents
 */
@RegisterMapper(PatentMetadataMapper.class)
public interface SureChemblPatentMetadataDao {

    /**
     * Retrieves the patent metadata of those patents published after
     * (inclusive) the given date
     *
     * The patent_id is discarded because our database generates it.
     */
    @SqlQuery("SELECT DISTINCT -1 as patent_id, externalkey as patent_number, sort_date as publication_date" +
            "    FROM documents " +
            "   WHERE sort_date >= :date "+
            "     AND life_sci_relevant = 1 " +
            "     AND substr(externalkey, 1, 2) IN ('EP', 'WO', 'US') " +
            "     AND (docid IN (SELECT docid FROM locations) " +
            "          OR docid IN (SELECT doc_id FROM document_biothings) " +
            "          OR docid IN (SELECT doc_id FROM doc_field_biothings) " +
            "          OR docid IN (SELECT docid FROM images))")
    Iterator<PatentMetadata> retrievePatentsPublishedAfter(@Bind("date") Date date);


    /**
     * Closes the DAO and releases any allocated database resources
     */
    void close();
}
