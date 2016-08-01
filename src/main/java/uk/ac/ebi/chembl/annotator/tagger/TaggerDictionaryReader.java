package uk.ac.ebi.chembl.annotator.tagger;


import com.google.common.base.Splitter;
import org.jensenlab.tagger.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.chembl.annotator.DictionaryEntry;
import uk.ac.ebi.chembl.annotator.DictionaryReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;


/**
 * Reader for Tagger dictionaries
 */
public class TaggerDictionaryReader implements DictionaryReader {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /** Path to the dictionary files */
    private String dictionariesHome;


    public TaggerDictionaryReader(String dictionariesHome) {
        this.dictionariesHome = dictionariesHome;
    }


    @Override
    public Stream<DictionaryEntry> read() throws IOException {
        logger.debug("Reading the Tagger dictionaries from {}...", dictionariesHome);

        Path path = Paths.get(dictionariesHome, TaggerAnnotator.ENTITIES_TSV);
        return Files.readAllLines(path, StandardCharsets.ISO_8859_1)
                .stream()
                .map(line -> {
                    try {
                        List<String> parts = Splitter.on("\t").splitToList(line);
                        String typeName = EntityType.fromId(Integer.valueOf(parts.get(1))).name();
                        String entityName = parts.get(2);
                        return new DictionaryEntry(typeName, entityName);
                    } catch (Exception ex) {
                        logger.error("An error occurred while reading the Tagger dictionary on line {}", line, ex);
                        throw ex;
                    }
                });
    }
}
