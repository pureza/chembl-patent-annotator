package uk.ac.ebi.chembl.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.model.AnnotatorMetadata;
import uk.ac.ebi.chembl.model.Dictionary;
import uk.ac.ebi.chembl.model.PatentAnnotations;
import uk.ac.ebi.chembl.model.PatentMetadata;
import uk.ac.ebi.chembl.pipeline.PipelineStage;
import uk.ac.ebi.chembl.storage.AnnotationRepository;
import uk.ac.ebi.chembl.storage.DictionaryRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;


/**
 * This stage stores the annotations in the database.
 *
 * It persists the annotations in batches, for increased performance.
 */
@Component
public class AnnotationPersistenceStep extends PipelineStage<PatentMetadataAndAnnotations, Void> {

    /** How many patents in one batch? */
    private static final int BATCH_SIZE = 92;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AnnotationRepository annotationRepository;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private AnnotatorMetadata annotator;

    /** Batch of patents to persist */
    private List<PatentMetadataAndAnnotations> batch = new ArrayList<>(BATCH_SIZE);


    @Override
    protected int process(PatentMetadataAndAnnotations patent) throws Exception {
        batch.add(patent);

        int added = 0;
        if (batch.size() == BATCH_SIZE) {
            added = persistAnnotations(batch);
        }

        return added;
    }


    @Override
    protected int onSuccess() throws Exception {
        // Don't forget the last batch
        return persistAnnotations(batch);
    }


    private int persistAnnotations(Collection<PatentMetadataAndAnnotations> batch) {
        List<PatentMetadata> patentMetadatas = batch.stream()
                .map(PatentMetadataAndAnnotations::getMetadata)
                .collect(toList());

        List<PatentAnnotations> patentAnnotationses = batch.stream()
                .map(PatentMetadataAndAnnotations::getAnnotations)
                .collect(toList());

        // Load the dictionary
        Dictionary dictionary = dictionaryRepository.get(annotator);

        // Save the annotations
        annotationRepository.saveAnnotations(patentMetadatas, patentAnnotationses, dictionary);

        // Clear the batch
        int added = batch.size();
        batch.clear();
        return added;
    }
}
