package uk.ac.ebi.chembl.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.model.PatentContent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the XML of a patent
 *
 * Uses simple regular expressions to detect the beginning and the end of
 * certain XML tags and it doesn't strip out inner tags or attributes, so it's
 * not exactly what's usually considered to be an "XML parser", but it works
 * well enough for our purposes.
 */
@Component
public class PatentXmlParser {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /** Regular expression patterns used to extract some fields */
    private Map<String, Pattern> patterns;


    public PatentXmlParser() {
        patterns = new HashMap<>();
        patterns.put("abstract", Pattern.compile("<abstract.*?</abstract>", Pattern.DOTALL));
        patterns.put("description", Pattern.compile("<description.*?</description>", Pattern.DOTALL));
        patterns.put("claims", Pattern.compile("<claims.*?</claims>", Pattern.DOTALL));
        patterns.put("invention-title", Pattern.compile("<invention-title.*?</invention-title>", Pattern.DOTALL));
        patterns.put("non-patent-citations", Pattern.compile("<non-patent-citations.*?</non-patent-citations>", Pattern.DOTALL));
    }


    /**
     * Parses a patent XML and extracts the relevant fields out of it
     */
    public PatentContent parse(String xml) {
        String patentNumber = extractExternalId(xml);
        List<String> abstracts = extractInnerXml("abstract", xml);
        List<String> descriptions = extractInnerXml("description", xml);
        List<String> claims = extractInnerXml("claims", xml);
        List<String> titles = extractInnerXml("invention-title", xml);
        List<String> nonPatentCitations = extractInnerXml("non-patent-citations", xml);

        return new PatentContent(patentNumber, abstracts, descriptions, claims, titles, nonPatentCitations);
    }


    /**
     * Extracts multiple occurrences of the same tag within the XML
     *
     * Each occurrence corresponds to a different language. We ignore all
     * languages other than English.
     */
    private List<String> extractInnerXml(String tag, String xml) {
        Pattern pattern = patterns.get(tag);
        Matcher matcher = pattern.matcher(xml);

        List<String> results = new ArrayList<>();
        while (matcher.find()) {
            String match = matcher.group(0);
            String lang = extractLang(match);

            // <non-patent-citations> doesn't have the lang attribute, so don't
            // show any warnings if there is no lang attribute
            if (lang == null) {
                results.add(match);
            } else {
                switch (lang.toUpperCase()) {
                    case "EN":
                    case "\">": // Matches <... lang=""> which happens once in a while.
                                // The text is usually in English, however.
                        results.add(match);
                        break;
                    case "DE":
                    case "FR":
                    case "ES":
                    case "JA":
                    case "RU":
                    case "FI":
                    case "NL":
                    case "PT":
                    case "KO":
                    case "SV":
                    case "DA":
                    case "ZH":
                    case "NO":
                    case "IT":
                    case "AR":
                    case "HU":
                        // Ignore these languages
                        break;
                    default:
                        logger.warn("Unexpected lang {} in {}", lang, match);
                        break;
                }
            }
        }

        return results;
    }


    private String extractExternalId(String xml) {
        String UCID = "ucid=\"";
        int index = xml.indexOf(UCID);
        int endQuoteIndex = xml.indexOf("\"", index + UCID.length());
        return xml.substring(index + UCID.length(), endQuoteIndex);
    }


    private String extractLang(String xml) {
        String LANG = "lang=\"";
        int index = xml.indexOf(LANG);
        return index == -1 ? null : xml.substring(index + LANG.length(), index + LANG.length() + 2);
    }
}
