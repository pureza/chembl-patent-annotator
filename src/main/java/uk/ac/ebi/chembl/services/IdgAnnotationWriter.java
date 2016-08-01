package uk.ac.ebi.chembl.services;

import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.model.IdgAnnotation;
import uk.ac.ebi.chembl.storage.IdgAnnotationRepository;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;


/**
 * Produces the output file required by IDG
 */
@Component
public class IdgAnnotationWriter {

    /** Repository of IDG annotations */
    @Autowired
    private IdgAnnotationRepository repository;

    /** Path to the output file */
    @Value("${idg-output.path}")
    private String idgOutputPath;

    private final Logger logger = LoggerFactory.getLogger(getClass());


    /**
     * Writes the IDG annotations to the output file
     */
    public void write() throws IOException {
        logger.info("Retrieving annotations for IDG targets... this will take a few minutes");

        Iterator<IdgAnnotation> it = repository.retrieveIdgAnnotations();

        // Write IDG annotations to a tsv file
        try (CSVWriter writer = new CSVWriter(new FileWriter(idgOutputPath), '\t')) {
            // Header
            writer.writeNext(new String[] {
                    "Entity",
                    "UniProt",
                    "Target name",
                    "Development level",
                    "Target family",
                    "Patent",
                    "Published",
                    "Total hits",
                    "Description hits",
                    "Claims hits",
                    "Abstract hits",
                    "Title hits",
                    "Terms"
            });

            it.forEachRemaining(annotation -> {
                String[] values = new String[] {
                        annotation.getEntityName(),
                        annotation.getUniProtAccession(),
                        annotation.getTargetName(),
                        annotation.getDevelopmentLevel(),
                        annotation.getTargetFamily(),
                        annotation.getPatentNumber(),
                        annotation.getPublicationDate().toString(),
                        String.valueOf(annotation.getTotalFrequency()),
                        String.valueOf(annotation.getDescriptionFrequency()),
                        String.valueOf(annotation.getClaimsFrequency()),
                        String.valueOf(annotation.getAbstractFrequency()),
                        String.valueOf(annotation.getTitleFrequency()),
                        annotation.getTermsFrequency()
                };

                writer.writeNext(values);
            });

            logger.info("IDG annotations written to {}", idgOutputPath);
        }
    }
}
