package uk.ac.ebi.chembl.storage.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;


/**
 * Data access object for Annotators
 */
public interface AnnotatorDao {

    /**
     * Retrieves the id of an annotator, or null if it doesn't exist
     */
    @SqlQuery("SELECT annotator_id FROM annotator WHERE name = :name")
    Integer retrieve(@Bind("name") String name);


    /**
     * Stores a new annotator in the database and returns its id
     */
    @SqlUpdate("INSERT INTO annotator(name) VALUES (:name)")
    @GetGeneratedKeys
    int save(@Bind("name") String name);


    /**
     * Clears the annotator table and all other tables that depend on it
     */
    @SqlUpdate("DELETE FROM annotator")
    void clear();


    /**
     * Closes the DAO and releases any allocated database resources
     */
    void close();
}
