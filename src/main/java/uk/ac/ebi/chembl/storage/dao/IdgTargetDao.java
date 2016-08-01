package uk.ac.ebi.chembl.storage.dao;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

import java.util.List;


/**
 * Data access object for Annotators
 */
public interface IdgTargetDao {


    /**
     * Persists a batch of IDG Targets into the database
     */
    @SqlBatch("INSERT INTO idg_target (uniprot_acc, name, development_level, target_family) " +
            "       VALUES (:uniprotAcc, :name, :developmentLevel, :targetFamily)")
    void saveBatch(@Bind("uniprotAcc") List<String> uniprotAccessions,
                   @Bind("name") List<String> names,
                   @Bind("developmentLevel") List<String> developmentLevels,
                   @Bind("targetFamily") List<String> targetFamilies);


    /**
     * Deletes all IDG Targets from the database
     */
    @SqlUpdate("DELETE FROM idg_target")
    int clear();


    /**
     * Closes the DAO and releases any allocated database resources
     */
    void close();
}
