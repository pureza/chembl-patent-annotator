package uk.ac.ebi.chembl.model;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Represents a Dictionary specific for an Annotator
 */
public class Dictionary {

    /** The null dictionary */
    private static final Dictionary EMPTY_DICTIONARY = new Dictionary(null, Collections.emptyList());

    public static Dictionary emptyDictionary() {
        return EMPTY_DICTIONARY;
    }

    /** Which annotator uses this dictionary? */
    private final AnnotatorMetadata annotator;

    /** The dictionary entity ids, first mapped by Type and then by Name */
    private final Map<String, Map<String, Long>> entities = new HashMap<>();


    public Dictionary(AnnotatorMetadata annotator, List<BioEntity> entities) {
        this.annotator = annotator;

        // Transform the list of BioEntities into a two-level map, where the
        // first level groups by type and the second by name
        entities.forEach(entity -> {
            Map<String, Long> typeEntities = this.entities.getOrDefault(entity.getType(), new HashMap<>());
            typeEntities.put(entity.getName(), entity.getId());
            this.entities.put(entity.getType(), typeEntities);
        });
    }


    /**
     * Is this dictionary empty?
     */
    public boolean isEmpty() {
        return entities.values().stream().allMatch(Map::isEmpty);
    }


    /**
     * Returns the number of entities in this dictionary
     */
    public int size() {
        return entities.values().stream().mapToInt(Map::size).sum();
    }


    public AnnotatorMetadata getAnnotator() {
        return annotator;
    }


    /**
     * Retrieves the id of a specific entity, identified by its type and name
     */
    public Long getBioEntityId(String typeName, String entityName) {
        return entities.get(typeName).get(entityName);
    }


    /**
     * Retrieves all entities, first mapped by Type and then by Name
     */
    public Map<String, Map<String, Long>> getEntities() {
        return entities;
    }
}
