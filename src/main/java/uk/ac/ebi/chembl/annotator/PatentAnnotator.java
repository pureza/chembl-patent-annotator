package uk.ac.ebi.chembl.annotator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.chembl.model.Annotation;
import uk.ac.ebi.chembl.model.Field;
import uk.ac.ebi.chembl.model.PatentAnnotations;
import uk.ac.ebi.chembl.model.PatentContent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;


/**
 * Patent specific annotator
 */
public class PatentAnnotator {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /** The actual text-based annotator */
    private Annotator annotator;


    public PatentAnnotator(Annotator annotator) {
        this.annotator = annotator;
    }


    /**
     * Annotates a patent and returns the annotations found
     */
    public PatentAnnotations processPatent(PatentContent patent) {
        Map<Field, List<String>> fields = new HashMap<>();
        fields.put(Field.CLAIMS, patent.getClaims());
        fields.put(Field.ABSTRACT, patent.getAbstracts());
        fields.put(Field.DESCRIPTION, patent.getDescriptions());
        fields.put(Field.TITLE, patent.getTitles());
        fields.put(Field.CITATIONS, patent.getNonPatentCitations());

        Map<Field, List<List<Annotation>>> annotations = fields.entrySet()
                .stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> e.getValue()
                                .stream()
                                .map(annotator::annotate)
                                .collect(Collectors.toList())));

        return new PatentAnnotations(patent.getPatentNumber(), annotations);
    }
}
