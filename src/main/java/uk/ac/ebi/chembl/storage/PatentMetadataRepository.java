package uk.ac.ebi.chembl.storage;

import org.skife.jdbi.v2.sqlobject.CreateSqlObject;
import org.skife.jdbi.v2.sqlobject.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.chembl.model.PatentMetadata;
import uk.ac.ebi.chembl.storage.dao.PatentMetadataDao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Repository for Patent metadata
 */
public abstract class PatentMetadataRepository {

    private Logger logger = LoggerFactory.getLogger(getClass().getSuperclass());


    @CreateSqlObject
    protected abstract PatentMetadataDao dao();


    /**
     * Retrieves a list with the most recently annotated patents
     *
     * We use this to look for newer patents in SureChEMBL.
     */
    public List<PatentMetadata> getMostRecentPatents() {
        return dao().retrieveMostRecentPatents();
    }


    /**
     * Retrieves the list of patents in the database that haven't been
     * annotated yet.
     *
     * Note: Considers only patents that have already been loaded into the
     * annotation database; excludes newer patents that are only in SureChEMBL.
     */
    public List<PatentMetadata> getUnannotatedPatents(String annotator) {
        return dao().retrieveUnannotatedPatents(annotator);
    }


    /**
     * Saves a bunch of patent metadatas into the annotation database
     */
    @Transaction
    public void saveBatch(List<PatentMetadata> patents) {
        List<String> patentNumbers = new ArrayList<>(patents.size());
        List<Date> publicationDates = new ArrayList<>(patents.size());

        for (PatentMetadata patent : patents) {
            patentNumbers.add(patent.getPatentNumber());
            publicationDates.add(patent.getPublicationDate());
        }

        dao().saveBatch(patentNumbers, publicationDates);
    }


    /**
     * Deletes a set of patents from the database
     */
    @Transaction
    public void deleteBatch(List<Long> patentIds) {
        if (!patentIds.isEmpty()) {
            dao().deleteBatch(patentIds);
        }
    }
}
