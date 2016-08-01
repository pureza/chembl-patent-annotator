package uk.ac.ebi.chembl.annotator;


import java.io.IOException;
import java.util.stream.Stream;


/**
 * Dictionary reader and parser
 */
public interface DictionaryReader {

    /**
     * Reads the annotator's dictionary files, entry by entry
     */
    Stream<DictionaryEntry> read() throws IOException;
}
