package uk.ac.ebi.chembl.storage;

import org.skife.jdbi.v2.sqlobject.CreateSqlObject;
import org.skife.jdbi.v2.sqlobject.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.chembl.model.AnnotatorMetadata;
import uk.ac.ebi.chembl.storage.dao.AnnotatorDao;

/**
 * Repository for Annotators
 */
public abstract class AnnotatorRepository {

    private Logger logger = LoggerFactory.getLogger(getClass().getSuperclass());

    @CreateSqlObject
    protected abstract AnnotatorDao dao();


    /**
     * Gets the current annotator's id, creating it if necessary
     */
    @Transaction
    public AnnotatorMetadata getOrCreate(String annotatorName) {
        Integer id = dao().retrieve(annotatorName);
        if (id == null) {
            logger.info("Registering new annotator '{}'...", annotatorName);
            id = dao().save(annotatorName);
        }

        return new AnnotatorMetadata(id, annotatorName);
    }
}
