package uk.ac.ebi.chembl.storage.dao;


import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;


/**
 * Data Access Object for Biological Entity Types
 */
public interface BioTypeDao {

    /**
     * Persists a new Biological Type
     */
    @SqlUpdate("INSERT INTO bio_type (name, annotator_id) VALUES (:name, :annotatorId)")
    @GetGeneratedKeys
    long save(@Bind("name") String name, @Bind("annotatorId") int annotatorId);


    /**
     * Clears all Biological Types from the database
     */
    @SqlUpdate("DELETE FROM bio_type " +
            "    WHERE annotator_id = (SELECT annotator_id FROM annotator WHERE name = :annotatorName)")
    void clear(@Bind("annotatorName") String annotatorName);


    /**
     * Closes the DAO and releases any allocated database resources
     */
    void close();
}



