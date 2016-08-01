package uk.ac.ebi.chembl.model;

import java.util.List;
import java.util.Map;

/**
 * The annotations found in a patent
 */
public class PatentAnnotations {

    /** Patent number */
    private final String patentNumber;

    /** Annotations, mapped by field and then sorted by rank */
    private final Map<Field, List<List<Annotation>>> annotations;


    public PatentAnnotations(String patentNumber, Map<Field, List<List<Annotation>>> annotations) {
        this.patentNumber = patentNumber;
        this.annotations = annotations;
    }


    public String getPatentNumber() {
        return patentNumber;
    }


    public Map<Field, List<List<Annotation>>> getAnnotations() {
        return annotations;
    }
}
