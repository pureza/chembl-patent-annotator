package uk.ac.ebi.chembl.storage.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;
import uk.ac.ebi.chembl.model.PatentMetadata;

import java.util.Date;
import java.util.List;

/**
 * Manages patent metadata in the annotation database
 */
@RegisterMapper(PatentMetadataMapper.class)
@UseStringTemplate3StatementLocator
public interface PatentMetadataDao {

    /**
     * Retrieves the most recently annotated patents (i.e., those published
     * on the last day we updated the database)
     */
    @SqlQuery("SELECT patent_id, patent_number, publication_date " +
            "    FROM patent " +
            "   WHERE publication_date = (SELECT max(publication_date) FROM patent)")
    List<PatentMetadata> retrieveMostRecentPatents();


    /**
     * Retrieves the patents that haven't been annotated yet by the given
     * annotator
     */
    @SqlQuery("SELECT patent_id, patent_number, publication_date " +
            "    FROM patent pa " +
            "   WHERE NOT EXISTS(SELECT 1 FROM annotator an, patent_annotated_by pab " +
            "                     WHERE pa.patent_id = pab.patent_id " +
            "                       AND pab.annotator_id = an.annotator_id " +
            "                       AND an.name = :annotatorName) ")
    List<PatentMetadata> retrieveUnannotatedPatents(@Bind("annotatorName") String annotatorName);


    /**
     * Saves a batch of patents' metadata into the database
     */
    @SqlBatch("INSERT INTO patent(patent_number, publication_date) " +
            "       VALUES (:patentNumber, :publicationDate)")
    @BatchChunkSize(1024)
    void saveBatch(@Bind("patentNumber") List<String> patentNumber, @Bind("publicationDate") List<Date> publicationDate);


    /**
     * Marks the given patents as annotated
     */
    @SqlBatch("INSERT INTO patent_annotated_by(patent_id, annotator_id) " +
            "       VALUES (:patentId, :annotatorId)")
    void markAsAnnotated(@Bind("patentId") List<Long> patentIds, @Bind("annotatorId") int annotatorId);


    /**
     * Deletes all patents' metadata from the database
     */
    @SqlUpdate("DELETE FROM patent")
    void clear();


    /**
     * Deletes a set of patents from the database
     */
    @SqlUpdate("DELETE FROM patent WHERE patent_id IN (<patentIds>)")
    void deleteBatch(@BindIn("patentIds") List<Long> patentIds);


    /**
     * Closes the DAO and releases any allocated database resources
     */
    void close();
}
