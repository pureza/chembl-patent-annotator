package uk.ac.ebi.chembl.jobs;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.pipeline.Pipeline;
import uk.ac.ebi.chembl.pipeline.PipelineStage;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Pipeline for annotating new patents.
 */
@Component
public class AnnotationPipeline {

    @Autowired
    private PatentMetadataLoadStep metadataLoadStep;

    @Autowired
    private PatentXmlDownloadStep xmlDownloadStep;

    @Autowired
    private PatentAnnotationStep annotationStep;

    @Autowired
    private AnnotationPersistenceStep persistenceStep;

    public void run() throws Exception {

        Pipeline pipeline = new Pipeline() {
            @Override
            protected List<PipelineStage<?, ?>> setUp() {
                pipeThrough(metadataLoadStep, xmlDownloadStep);
                pipeThrough(xmlDownloadStep, annotationStep);
                pipeThrough(annotationStep, persistenceStep);

                return asList(metadataLoadStep, xmlDownloadStep, annotationStep, persistenceStep);
            }
        };

        pipeline.run();
    }
}
