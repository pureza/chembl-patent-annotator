package uk.ac.ebi.chembl.annotator;


/**
 * Generic interface for initializing an annotator and its auxiliar structures
 */
public interface AnnotatorFactory {

    /**
     * What's the name of the annotator this factory initializes?
     * Used to decide which factory to call on runtime.
     */
    String annotatorName();


    /**
     * Initializes the annotator
     */
    Annotator initAnnotator();


    /**
     * Initializes the dictionary reader for this annotator
     */
    DictionaryReader initDictionaryReader();
}
