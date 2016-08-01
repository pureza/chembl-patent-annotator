package uk.ac.ebi.chembl.storage;

import org.skife.jdbi.v2.sqlobject.CreateSqlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.chembl.model.PatentMetadata;
import uk.ac.ebi.chembl.storage.dao.PatentMetadataDao;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Repository for Patent Annotations
 */
public abstract class AnnotationRepository {

    private Logger logger = LoggerFactory.getLogger(getClass().getSuperclass());

    @CreateSqlObject
    protected abstract PatentMetadataDao patentMetadataDao();


    /**
     * Marks the given patents as annotated
     */
    public void markPatentsAsAnnotated(List<PatentMetadata> metadatas,
                                       int annotatorId) {
        // Mark all patents as annotated (including those with no annotations)
        List<Long> allPatentIds = metadatas.stream().map(PatentMetadata::getId).collect(toList());
        patentMetadataDao().markAsAnnotated(allPatentIds, annotatorId);
    }
}
