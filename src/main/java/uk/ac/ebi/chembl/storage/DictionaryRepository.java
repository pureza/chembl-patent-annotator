package uk.ac.ebi.chembl.storage;


import org.skife.jdbi.v2.sqlobject.CreateSqlObject;
import org.skife.jdbi.v2.sqlobject.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.chembl.annotator.DictionaryEntry;
import uk.ac.ebi.chembl.model.AnnotatorMetadata;
import uk.ac.ebi.chembl.model.BioEntity;
import uk.ac.ebi.chembl.model.Dictionary;
import uk.ac.ebi.chembl.storage.dao.BioEntityDao;
import uk.ac.ebi.chembl.storage.dao.BioTypeDao;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * Repository for Dictionary Entities
 */
@Service
public abstract class DictionaryRepository {

    private Logger logger = LoggerFactory.getLogger(getClass().getSuperclass());

    @CreateSqlObject
    protected abstract BioTypeDao bioTypeDao();

    @CreateSqlObject
    protected abstract BioEntityDao bioEntityDao();

    /** The whole dictionary, cached for increased performance */
    private Dictionary dictionary = Dictionary.emptyDictionary();

    private NumberFormat nf = NumberFormat.getInstance();


    /**
     * Returns the dictionary for the given annotator
     *
     * It's not currently possible to load the dictionaries for multiple
     * annotators simultaneously.
     */
    public Dictionary get(AnnotatorMetadata annotator) {
        // If no dictionary has been loaded, load it now
        if (dictionary.isEmpty()) {
            this.dictionary = load(annotator);
        }

        // If we already cached the dictionary for this annotator, just return it
        if (annotator.equals(dictionary.getAnnotator())) {
            return dictionary;
        }

        // Otherwise, we are trying to load a new dictionary. You can only work
        // with a single dictionary at a time!
        throw new IllegalArgumentException("Unable to load the dictionary for " + annotator.getName() + ", " +
                "because I've already loaded the dictionary for " + dictionary.getAnnotator().getName() + "");
    }


    /**
     * Persists all the biological entities in a new dictionary to the database
     */
    @Transaction
    public void save(List<DictionaryEntry> entries, AnnotatorMetadata annotator) {
        // Updating the dictionary after it has been loaded is not allowed, because if
        // someone is using the old dictionary this will lead to inconsistencies in the
        // annotations. You must update the dictionary only on startup.
        if (!dictionary.isEmpty()) {
            throw new IllegalStateException("Not allowed to update a non-empty dictionary");
        }

        // Save the entity types first
        Map<String, Long> bioTypes = saveBioTypes(entries, annotator);

        // Then, save the entities
        saveBioEntities(entries, bioTypes, annotator);

        logger.info("Stored the dictionary for {} with {} entities belonging to {} types",
                annotator.getName(), nf.format(entries.size()), bioTypes.size());
    }


    private Map<String, Long> saveBioTypes(List<DictionaryEntry> entries, AnnotatorMetadata annotator) {
        bioTypeDao().clear(annotator.getName());

        return entries.stream()
                .map(DictionaryEntry::getType)
                .distinct()
                .collect(toMap(Function.identity(), name -> bioTypeDao().save(name, annotator.getId())));
    }


    private void saveBioEntities(List<DictionaryEntry> entries, Map<String, Long> bioTypes, AnnotatorMetadata annotator) {
        // At this point there should be no entities for this annotator, but
        // being explicit doesn't hurt
        bioEntityDao().clear(annotator.getName());

        // Deconstruct the list of entries into lists of its components
        List<Long> typeIds = new ArrayList<>();
        List<String> names = new ArrayList<>();
        entries.forEach(entry -> {
            Long typeId = bioTypes.get(entry.getType());
            typeIds.add(typeId);
            names.add(entry.getName());
        });

        bioEntityDao().saveBatch(typeIds, names);
    }


    private Dictionary load(AnnotatorMetadata annotator) {
        List<BioEntity> entities = bioEntityDao().retrieveAll(annotator.getName());

        logger.info("Loaded the dictionary for {} with {} entities",
                annotator.getName(), nf.format(entities.size()));

        return new Dictionary(annotator, entities);
    }
}
