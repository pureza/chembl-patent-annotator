package uk.ac.ebi.chembl.storage.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;

import java.util.List;


/**
 * Annotation Data Access Object
 */
public interface AnnotationDao {

    /**
     * Persists a batch of annotations
     */
    @SqlBatch("INSERT INTO annotation (patent_id, field_id, rank, bio_entity_id, start_offset, end_offset, term) " +
            "       VALUES (:patentId, :fieldId, :rank, :bioEntityId, :start, :end, :term)")
    void saveAnnotations(@Bind("patentId") List<Long> patentIds, @Bind("fieldId") List<Integer> fieldIds,
                         @Bind("rank") List<Integer> ranks,
                         @Bind("bioEntityId") List<Long> bioEntityIds, @Bind("start") List<Integer> startings,
                         @Bind("end") List<Integer> endings, @Bind("term") List<String> terms);


    /**
     * Persists a batch of occurrences of biological entities per patent
     */
    @SqlBatch("INSERT INTO bioentity_patent_annotation_count (bio_entity_id, patent_id, field_id, term, frequency) " +
            "       VALUES (:bioEntityId, :patentId, :fieldId, :term, :frequency)")
    void saveBioEntityOccurrencesPerPatent(@Bind("bioEntityId") List<Long> bioEntityIds,
                                           @Bind("patentId") List<Long> patentIds,
                                           @Bind("fieldId") List<Integer> fieldIds,
                                           @Bind("term") List<String> terms,
                                           @Bind("frequency") List<Integer> frequencies);


    /**
     * Closes the DAO and releases any allocated database resources
     */
    void close();
}
