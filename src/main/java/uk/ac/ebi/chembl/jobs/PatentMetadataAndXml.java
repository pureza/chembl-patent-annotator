package uk.ac.ebi.chembl.jobs;


import uk.ac.ebi.chembl.model.PatentMetadata;
import uk.ac.ebi.chembl.model.PatentXml;


/**
 * A tuple containing patent metadata and its XML
 */
public class PatentMetadataAndXml {

    /** The metadata */
    private final PatentMetadata metadata;

    /** The XML */
    private final PatentXml xml;


    public PatentMetadataAndXml(PatentMetadata metadata, PatentXml xml) {
        this.metadata = metadata;
        this.xml = xml;
    }


    public PatentMetadata getMetadata() {
        return metadata;
    }


    public PatentXml getXml() {
        return xml;
    }
}
