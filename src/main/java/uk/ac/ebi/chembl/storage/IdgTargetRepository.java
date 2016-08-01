package uk.ac.ebi.chembl.storage;

import org.skife.jdbi.v2.sqlobject.CreateSqlObject;
import org.skife.jdbi.v2.sqlobject.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.chembl.model.IdgTarget;
import uk.ac.ebi.chembl.storage.dao.IdgTargetDao;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for IDG Targets
 */
public abstract class IdgTargetRepository {

    private Logger logger = LoggerFactory.getLogger(getClass().getSuperclass());

    @CreateSqlObject
    protected abstract IdgTargetDao dao();


    /**
     * Persists a list of targets
     */
    @Transaction
    public void save(List<IdgTarget> targets) {
        // Decompose a list of targets into multiple lists with its components
        List<String> uniprotAccessions = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<String> developmentLevels = new ArrayList<>();
        List<String> targetFamilies = new ArrayList<>();

        targets.forEach(target -> {
            uniprotAccessions.add(target.getUniProtAccession());
            names.add(target.getName());
            developmentLevels.add(target.getDevelopmentLevel());
            targetFamilies.add(target.getTargetFamily());
        });

        dao().saveBatch(uniprotAccessions, names, developmentLevels, targetFamilies);
    }


    /**
     * Deletes all targets from the database
     */
    @Transaction
    public int clear() {
        return dao().clear();
    }

}
