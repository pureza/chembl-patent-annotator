package uk.ac.ebi.chembl.annotator;


import uk.ac.ebi.chembl.model.Annotation;

import java.util.List;


/**
 * Generic text-based annotator
 */
public interface Annotator {

    /**
     * Returns the list of annotations found in the given text
     */
    List<Annotation> annotate(String text);


    /**
     * Shuts down the annotator
     */
    void shutdown();
}
