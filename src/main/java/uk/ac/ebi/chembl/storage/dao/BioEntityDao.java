package uk.ac.ebi.chembl.storage.dao;


import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import uk.ac.ebi.chembl.model.BioEntity;

import java.util.List;


/**
 * Data Access Object for Biological Entities
 */
@RegisterMapper(BioEntityMapper.class)
@UseStringTemplate3StatementLocator
public interface BioEntityDao {

    /**
     * Saves a batch of new Biological Entities into the database
     */
    @SqlBatch("INSERT INTO bio_entity (bio_type_id, name) VALUES (:typeId, :name)")
    void saveBatch(@Bind("typeId") List<Long> typeIds, @Bind("name") List<String> names);


    /**
     * Retrieves all Biological Entities in the database from a given annotator
     */
    @SqlQuery("SELECT en.bio_entity_id, ty.name as type, en.name " +
            "    FROM bio_entity en, bio_type ty, annotator an " +
            "   WHERE en.bio_type_id = ty.bio_type_id " +
            "     AND ty.annotator_id = an.annotator_id " +
            "     AND an.name = :annotatorName")
    List<BioEntity> retrieveAll(@Bind("annotatorName") String annotatorName);


    /**
     * Deletes all Biological Entities used by the given annotator
     */
    @SqlUpdate("DELETE " +
            "     FROM bio_entity " +
            "    WHERE (SELECT an.name FROM annotator an, bio_type ty " +
            "            WHERE an.annotator_id = ty.annotator_id " +
            "              AND ty.bio_type_id = bio_entity.bio_type_id) = :annotator")
    void clear(@Bind("annotator") String annotator);


    /**
     * Closes the DAO and releases any allocated database resources
     */
    void close();
}



