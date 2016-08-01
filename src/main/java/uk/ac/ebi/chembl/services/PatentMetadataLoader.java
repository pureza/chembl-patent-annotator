package uk.ac.ebi.chembl.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.model.PatentMetadata;
import uk.ac.ebi.chembl.storage.ExternalPatentMetadataRepository;
import uk.ac.ebi.chembl.storage.PatentMetadataRepository;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads patent metadata from SurePro into the annotation database
 */
@Component
public class PatentMetadataLoader {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private PatentMetadataRepository repository;

    @Autowired
    private ExternalPatentMetadataRepository externalRepository;

    @Autowired
    private Environment env;

    private NumberFormat nf = NumberFormat.getInstance();


    /**
     * Loads the metadata of new patents from SureChembl into the annotation
     * database
     */
    public void loadNewPatents() {
        logger.info("Looking for new patents in {}, to copy them to {}...", env.getProperty("surechem.url"),
                env.getProperty("patentannot.url"));

        // First, retrieve the patents that were added last
        List<PatentMetadata> previousPatents = repository.getMostRecentPatents();

        Iterator<PatentMetadata> newPatents;
        if (previousPatents.isEmpty()) {
            logger.warn("The annotation database is empty!");
            logger.info("Loading the metadata for ALL patents. This will take a few minutes...");
            newPatents = externalRepository.getAllPatents();
        } else {
            Date previousDate = previousPatents.get(0).getPublicationDate();
            logger.info("The most recent patent in the annotation database was published on {}", previousDate);

            // Retrieve only the patents added after the last execution from SureChembl
            newPatents = externalRepository.getPatentsPublishedAfter(previousDate);
        }

        // These patents must not be added again!
        Set<String> ignore = previousPatents.stream()
                .map(PatentMetadata::getPatentNumber)
                .collect(Collectors.toSet());

        copy(newPatents, ignore);
    }


    /**
     * Persists the new metadata.
     */
    private void copy(Iterator<PatentMetadata> it, Set<String> ignore) {
        List<PatentMetadata> newPatents = new ArrayList<>();

        while (it.hasNext()) {
            PatentMetadata patent = it.next();
            if (ignore.contains(patent.getPatentNumber())) {
                // Don't add duplicates
                continue;
            }

            newPatents.add(patent);
        }

        // Save the new patents. This method is transactional
        repository.saveBatch(newPatents);

        if (newPatents.isEmpty()) {
            logger.info("No new patents found: the database was already up to date!");
        } else {
            logger.info("Finished copying the metadata of {} patents!", nf.format(newPatents.size()));
        }
    }
}
