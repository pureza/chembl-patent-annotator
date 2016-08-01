package uk.ac.ebi.chembl.jobs;


import uk.ac.ebi.chembl.model.PatentAnnotations;
import uk.ac.ebi.chembl.model.PatentMetadata;


/**
 * A tuple containing patent metadata and its annotations
 */
public class PatentMetadataAndAnnotations {

    /** The metadata */
    private final PatentMetadata metadata;

    /** The annotations */
    private final PatentAnnotations annotations;


    public PatentMetadataAndAnnotations(PatentMetadata metadata, PatentAnnotations annotations) {
        this.metadata = metadata;
        this.annotations = annotations;
    }


    public PatentMetadata getMetadata() {
        return metadata;
    }


    public PatentAnnotations getAnnotations() {
        return annotations;
    }
}
