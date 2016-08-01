package uk.ac.ebi.chembl.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.chembl.annotator.DictionaryEntry;
import uk.ac.ebi.chembl.annotator.DictionaryReader;
import uk.ac.ebi.chembl.model.AnnotatorMetadata;
import uk.ac.ebi.chembl.model.Dictionary;
import uk.ac.ebi.chembl.storage.DictionaryRepository;

import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toList;


/**
 * The Dictionary Analyzer loads the dictionary into the database on the first
 * run and looks for changes on subsequent runs.
 *
 * After the initial load, the dictionary used by the annotator must not change.
 * Otherwise, the annotator may detect entities which do not exist in the
 * database, which would lead to errors.
 *
 * If you want to use a newer version of the dictionaries, you will have
 * to annotate everything from scratch again!
 */
@Service
public class DictionaryAnalyzer {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /** Reader for the annotator's dictionary */
    @Autowired
    private DictionaryReader reader;

    /** Dictionary repository */
    @Autowired
    private DictionaryRepository dictionaryRepository;


    public boolean analyze(AnnotatorMetadata annotator) throws IOException {
        // Load the dictionary from the database
        Dictionary dbDictionary = dictionaryRepository.get(annotator);

        List<DictionaryEntry> fsDictionary = reader.read().collect(toList());
        if (dbDictionary.isEmpty()) {
            // If this is the first time we are using this annotator, load the
            // dictionary entities into the database.
            logger.warn("It looks like it's the first time you run the {} annotator. Persisting its dictionary to the database...", annotator);
            saveDictionary(fsDictionary, annotator);
        } else {
            // If the dictionary has already been added to the database before,
            // check for differences between what is in the database and what
            // is going to be used by the annotator. They must be the same.
            logger.info("Checking the dictionary...");
            return !differ(dbDictionary, fsDictionary);
        }

        return true;
    }


    /**
     * Persists the annotator dictionary to the database
     */
    private void saveDictionary(List<DictionaryEntry> dictionary, AnnotatorMetadata annotator) {
        dictionaryRepository.save(dictionary, annotator);
    }


    /**
     * Checks for differences between the entities that were previously
     * persisted to the database and the dictionary that is going to be
     * used be the annotator
     */
    private boolean differ(Dictionary db, List<DictionaryEntry> fs) {
        // First check if the dictionaries have the same size
        if (fs.size() != db.size()) {
            logger.error("The dictionary loaded from the database contains {} entities, while the " +
                    "dictionary files contain {}", db.size(), fs.size());
            return true;
        }

        // Then check if each entry in the dictionary also exists in the database
        for (DictionaryEntry entry : fs) {
            if (db.getBioEntityId(entry.getType(), entry.getName()) == null) {
                logger.error("The dictionary file contains entity {} of type {} which doesn't exist in the database!",
                        entry.getName(), entry.getType());
                return true;
            }
        };

        return false;
    }
}
