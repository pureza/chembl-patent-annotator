package uk.ac.ebi.chembl.storage;

import org.skife.jdbi.v2.sqlobject.CreateSqlObject;
import org.skife.jdbi.v2.sqlobject.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.chembl.model.*;
import uk.ac.ebi.chembl.model.Dictionary;
import uk.ac.ebi.chembl.storage.dao.AnnotationDao;
import uk.ac.ebi.chembl.storage.dao.PatentMetadataDao;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Repository for Patent Annotations
 */
public abstract class AnnotationRepository {

    private Logger logger = LoggerFactory.getLogger(getClass().getSuperclass());

    @CreateSqlObject
    protected abstract AnnotationDao annotationDao();

    @CreateSqlObject
    protected abstract PatentMetadataDao patentMetadataDao();


    /**
     * Saves a batch of annotations into the database
     *
     * Besides saving the raw annotations, it stores the number of occurrences
     * of each Biological Entity per Patent and it also marks the patents as
     * annotated by the current annotator.
     *
     * Initially, the number of occurrences was just a database view. However,
     * the amount of time that this view took to create justified the creation
     * of its own table, populated manually during the annotation process.
     *
     * Runs in a single transaction.
     */
    @Transaction
    public void saveAnnotations(List<PatentMetadata> metadatas, List<PatentAnnotations> annotations,
                                Dictionary dictionary) {
        assert (metadatas.size() == annotations.size());

        int annotatorId = dictionary.getAnnotator().getId();

        // Save each individual annotation
        saveRawAnnotations(metadatas, annotations, dictionary);

        // Counts the occurrences of each Biological Entity per Patent and saves that
        Map<BioEntityPerPatentKey, Integer> occurrences = countBioEntitiesPerPatent(metadatas, annotations, dictionary);
        saveBioEntityOccurrencesPerPatent(occurrences);

        // Marks the patents as annotated
        markPatentsAsAnnotated(metadatas, annotatorId);
    }


    /**
     * Saves each individual annotation
     */
    private void saveRawAnnotations(List<PatentMetadata> metadatas, List<PatentAnnotations> annotations,
                                    Dictionary dictionary) {
        assert(metadatas.size() == annotations.size());

        // Deconstruct the annotations into lists with its components
        List<Long> patentIds = new ArrayList<>();
        List<Integer> fieldIds = new ArrayList<>();
        List<Integer> ranks = new ArrayList<>();
        List<Long> bioEntityIds = new ArrayList<>();
        List<Integer> startings = new ArrayList<>();
        List<Integer> endings = new ArrayList<>();
        List<String> terms = new ArrayList<>();

        for (int i = 0; i < metadatas.size(); i++) {
            PatentMetadata metadata = metadatas.get(i);
            PatentAnnotations patentAnnotations = annotations.get(i);

            if (!metadata.getPatentNumber().equals(patentAnnotations.getPatentNumber())) {
                throw new RuntimeException("The metadata and annotations could not be paired!");
            }

            long patentId = metadata.getId();
            Map<Field, List<List<Annotation>>> fieldAnnotations = patentAnnotations.getAnnotations();

            fieldAnnotations.forEach((field, annotationsPerRank) -> {
                for (int rank = 0; rank < annotationsPerRank.size(); rank++) {
                    List<Annotation> annots = annotationsPerRank.get(rank);

                    for (Annotation annotation : annots) {
                        Long bioEntityId = dictionary.getBioEntityId(annotation.getType(), annotation.getName());
                        if (bioEntityId == null) {
                            logger.error("Couldn't find biological entity {} of type {} in the database.",
                                    annotation.getName(), annotation.getType());
                        }

                        // Terms larger than 127 characters don't fit in the database
                        if (annotation.getTerm().length() > Annotation.MAX_TERM_LENGTH) {
                            logger.warn("Skipping annotation for document {} because the term is too big: '{}'",
                                    metadata.getPatentNumber(), annotation.getTerm());
                            continue;
                        }

                        patentIds.add(patentId);
                        fieldIds.add(field.id());
                        ranks.add(rank);
                        bioEntityIds.add(bioEntityId);
                        startings.add(annotation.getStart());
                        endings.add(annotation.getEnd());
                        terms.add(annotation.getTerm());
                    }
                }
            });
        }

        if (!patentIds.isEmpty()) {
            // Persists the annotations
            annotationDao().saveAnnotations(patentIds, fieldIds, ranks, bioEntityIds, startings, endings, terms);
        }
    }


    /**
     * Saves the number of occurrences of each Biological Entity per Patent
     */
    private void saveBioEntityOccurrencesPerPatent(Map<BioEntityPerPatentKey, Integer> occurrences) {
        // Deconstruct the map into lists with its components
        List<Long> bioEntityIds = new ArrayList<>();
        List<Long> patentIds = new ArrayList<>();
        List<Integer> fieldIds = new ArrayList<>();
        List<String> terms = new ArrayList<>();
        List<Integer> frequencies = new ArrayList<>();

        occurrences.forEach((key, frequency) -> {
            // Ignore long terms, because they don't fit in the database anyway
            if (key.getTerm().length() <= Annotation.MAX_TERM_LENGTH) {
                bioEntityIds.add(key.getBioEntityId());
                patentIds.add(key.getPatentId());
                fieldIds.add(key.getFieldId());
                terms.add(key.getTerm());
                frequencies.add(frequency);
            }
        });

        if (!occurrences.isEmpty()) {
            annotationDao().saveBioEntityOccurrencesPerPatent(bioEntityIds, patentIds, fieldIds, terms, frequencies);
        }
    }


    /**
     * Counts the number of occurrences of each Biological Entity per Patent
     */
    private Map<BioEntityPerPatentKey, Integer> countBioEntitiesPerPatent(List<PatentMetadata> metadatas,
                                                                          List<PatentAnnotations> annotations,
                                                                          Dictionary dictionary) {

        // For each patent...
        Map<BioEntityPerPatentKey, Integer> result = new HashMap<>();
        for (int i = 0; i < metadatas.size(); i++) {
            PatentMetadata metadata = metadatas.get(i);
            PatentAnnotations patentAnnotations = annotations.get(i);

            long patentId = metadata.getId();
            Map<Field, List<List<Annotation>>> fieldAnnotations = patentAnnotations.getAnnotations();

            // For each field...
            fieldAnnotations.forEach((field, annotationsPerRank) -> {
                int fieldId = field.id();

                // For each rank (we ignore the rank)
                for (List<Annotation> annots : annotationsPerRank) {

                    // For each annotation...
                    for (Annotation annotation : annots) {
                        Long bioEntityId = dictionary.getBioEntityId(annotation.getType(), annotation.getName());
                        String term = annotation.getTerm();

                        BioEntityPerPatentKey key = new BioEntityPerPatentKey(bioEntityId, patentId, fieldId, term);
                        result.put(key, result.getOrDefault(key, 0) + 1);
                    }
                }
            });
        }

        return result;
    }


    /**
     * Marks the given patents as annotated
     */
    private void markPatentsAsAnnotated(List<PatentMetadata> metadatas,
                                        int annotatorId) {
        // Mark all patents as annotated (including those with no annotations)
        List<Long> allPatentIds = metadatas.stream().map(PatentMetadata::getId).collect(toList());
        patentMetadataDao().markAsAnnotated(allPatentIds, annotatorId);
    }
}


/**
 * Key for the map that counts occurrences of each Biological Entity per Patent
 *
 * The value of this map is the count.
 *
 * The casing of the term is ignored, as well as trailing whitespace, because
 * that's how MySQL compares strings using the default collation.
 */
class BioEntityPerPatentKey {

    /** Biological entity id */
    private final long bioEntityId;

    /** Patent id */
    private final long patentId;

    /** Field id */
    private final int fieldId;

    /** The term annotated */
    private final String term;


    public BioEntityPerPatentKey(long bioEntityId, long patentId, int fieldId, String term) {
        this.bioEntityId = bioEntityId;
        this.patentId = patentId;
        this.fieldId = fieldId;
        this.term = term;
    }


    public long getBioEntityId() {
        return bioEntityId;
    }


    public long getPatentId() {
        return patentId;
    }


    public int getFieldId() {
        return fieldId;
    }


    public String getTerm() {
        return term;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BioEntityPerPatentKey that = (BioEntityPerPatentKey) o;
        return bioEntityId == that.bioEntityId &&
                patentId == that.patentId &&
                fieldId == that.fieldId &&
                mysqlStringEquals(term, that.term);
    }


    @Override
    public int hashCode() {
        return Objects.hash(bioEntityId, patentId, fieldId, lowerAndTrimTrailingWhitespace(term));
    }


    /**
     * Converts the given text to lower case and removes any trailing whitespace
     */
    private String lowerAndTrimTrailingWhitespace(String text) {
        if (text == null) {
            return null;
        }

        // Copy the text to the StringBuilder, converting to lower case as it goes
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            sb.append(Character.toLowerCase(ch));
        }

        // Delete any trailing whitespace
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }


    /**
     * Compares two Strings in the same way that MySQL does it using the default
     * collation, i.e., by ignoring case and any trailing whitespace.
     *
     * We could have changed the default collation, but we think this behavior
     * makes sense for the patent annotation use case.
     */
    private boolean mysqlStringEquals(String a, String b) {
        if (a == null && b == null) {
            // They are both null
            return true;
        } else if (a != null && b != null) {
            // They are both non null
            return lowerAndTrimTrailingWhitespace(a).equals(lowerAndTrimTrailingWhitespace(b));
        } else {
            // One is null and the other isn't
            return false;
        }
    }
}