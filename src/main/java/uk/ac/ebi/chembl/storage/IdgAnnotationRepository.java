package uk.ac.ebi.chembl.storage;

import org.skife.jdbi.v2.sqlobject.CreateSqlObject;
import uk.ac.ebi.chembl.model.IdgAnnotation;
import uk.ac.ebi.chembl.storage.dao.IdgAnnotationDao;

import java.util.Iterator;

/**
 * Repository for IDG annotations
 */
public abstract class IdgAnnotationRepository {

    @CreateSqlObject
    protected abstract IdgAnnotationDao dao();


    /**
     * Retrieves all IDG annotations
     */
    public Iterator<IdgAnnotation> retrieveIdgAnnotations() {
        return dao().retrieveIdgAnnotations();
    }
}
