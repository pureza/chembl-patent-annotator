package uk.ac.ebi.chembl.jobs;


import org.apache.spark.api.java.JavaRDD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.model.PatentAnnotations;
import uk.ac.ebi.chembl.model.PatentMetadata;
import uk.ac.ebi.chembl.services.PatentMetadataLoader;
import uk.ac.ebi.chembl.services.PatentXmlDownloader;
import uk.ac.ebi.chembl.services.SparkAnnotationPersister;
import uk.ac.ebi.chembl.services.SparkPatentAnnotator;
import uk.ac.ebi.chembl.storage.PatentMetadataRepository;

import java.text.NumberFormat;
import java.util.List;

@Component
public class AnnotatorJob {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private PatentMetadataLoader loader;

    @Autowired
    private PatentMetadataRepository metadataRepository;

    @Autowired
    private PatentXmlDownloader patentXmlDownloader;

    @Autowired
    private SparkPatentAnnotator sparkAnnotator;

    @Autowired
    private SparkAnnotationPersister sparkAnnotationPersister;

    @Value("${annotator}")
    private String annotator;

    private NumberFormat nf = NumberFormat.getInstance();


    public void run() throws Exception {
        // Finds the metadata of all new patents and inserts it into the database
        loader.loadNewPatents();

        // Retrieve all patents that haven't been annotated yet: this includes
        // not only the new ones, but also patents that might have failed before
        List<PatentMetadata> unannotated = metadataRepository.getUnannotatedPatents(annotator);

        // Download the XML for new patents only
        List<PatentMetadata> toAnnotate = patentXmlDownloader.downloadNewPatents(unannotated);

        logger.warn("Going to annotate {} patents...", nf.format(toAnnotate.size()));

        // Annotate all patents
        JavaRDD<PatentAnnotations> annotationsRdd = sparkAnnotator.annotate(toAnnotate);

        // Persist the annotations
        sparkAnnotationPersister.persist(toAnnotate, annotationsRdd);
    }
}

