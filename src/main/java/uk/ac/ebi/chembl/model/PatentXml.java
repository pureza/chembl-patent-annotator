package uk.ac.ebi.chembl.model;


/**
 * The raw Patent XML
 */
public class PatentXml {

    /** Patent number */
    private final String patentNumber;

    /** Patent XML */
    private final String xml;


    public PatentXml(String patentNumber, String xml) {
        this.patentNumber = patentNumber;
        this.xml = xml;
    }


    public String getPatentNumber() {
        return patentNumber;
    }


    public String getXml() {
        return xml;
    }


    @Override
    public String toString() {
        return "PatentXml{" +
                "patentNumber='" + patentNumber + '\'' +
                ", xml='" + xml + '\'' +
                '}';
    }
}
