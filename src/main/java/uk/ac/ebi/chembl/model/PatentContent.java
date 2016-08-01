package uk.ac.ebi.chembl.model;

import java.util.List;


/**
 * A Patent's content
 */
public class PatentContent {

    /** Patent number */
    private final String patentNumber;

    /** Abstracts (one per language) */
    private final List<String> abstracts;

    /** Descriptions (one per language) */
    private final List<String> descriptions;

    /** Claims (one per language) */
    private final List<String> claims;

    /** Titles (one per language) */
    private final List<String> titles;

    /** Non patent citations (one per language) */
    private final List<String> nonPatentCitations;


    public PatentContent(String patentNumber, List<String> abstracts, List<String> descriptions, List<String> claims,
                         List<String> titles, List<String> nonPatentCitations) {
        this.patentNumber = patentNumber;
        this.abstracts = abstracts;
        this.descriptions = descriptions;
        this.claims = claims;
        this.titles = titles;
        this.nonPatentCitations = nonPatentCitations;
    }


    public String getPatentNumber() {
        return patentNumber;
    }


    public List<String> getAbstracts() {
        return abstracts;
    }


    public List<String> getDescriptions() {
        return descriptions;
    }


    public List<String> getClaims() {
        return claims;
    }


    public List<String> getTitles() {
        return titles;
    }


    public List<String> getNonPatentCitations() {
        return nonPatentCitations;
    }


    @Override
    public String toString() {
        return "PatentContent{" +
                "patentNumber='" + patentNumber + '\'' +
                '}';
    }
}
