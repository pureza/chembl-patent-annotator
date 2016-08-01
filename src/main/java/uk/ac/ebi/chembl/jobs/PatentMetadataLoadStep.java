package uk.ac.ebi.chembl.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.model.PatentMetadata;
import uk.ac.ebi.chembl.pipeline.PipelineStage;
import uk.ac.ebi.chembl.services.PatentMetadataLoader;
import uk.ac.ebi.chembl.storage.PatentMetadataRepository;

import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;

/**
 * Loads the metadata of all new patents into the database and retrieves the
 * metadata of all unannotated patents.
 *
 * This stage executes in one go, i.e, it terminates before the remaining
 * stages start processing. As such, if an error occurs during this stage, it
 * is enough to restart the application to retry.
 */
@Component
public class PatentMetadataLoadStep extends PipelineStage<Void, List<PatentMetadata>> {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private PatentMetadataLoader loader;

    @Autowired
    private PatentMetadataRepository repository;

    @Value("${annotator}")
    private String annotator;

    private NumberFormat nf = NumberFormat.getInstance();


    @Override
    public void run() throws Exception {
        // Finds the metadata of all new patents and inserts it into the database
        loader.loadNewPatents();

        // Retrieve all patents that haven't been annotated yet: this includes
        // not only the new ones, but also patents that might have failed before
        List<PatentMetadata> toAnnotate = repository.getUnannotatedPatents(annotator);

        logger.warn("Going to annotate {} patents...", nf.format(toAnnotate.size()));

        // Pass all the metadata onto the next stage
        out.put(Optional.of(toAnnotate));

        // Nothing else to do!
        injectPoisonPill();
    }


    @Override
    protected int process(Void incoming) throws Exception {
        // This will never be called.
        throw new UnsupportedOperationException();
    }
}
