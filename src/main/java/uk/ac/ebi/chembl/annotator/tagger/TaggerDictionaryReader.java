package uk.ac.ebi.chembl.annotator.tagger;


import com.clearspring.analytics.util.Lists;
import com.google.common.base.Splitter;
import org.jensenlab.tagger.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.chembl.annotator.DictionaryEntry;
import uk.ac.ebi.chembl.annotator.DictionaryReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


/**
 * Reader for Tagger dictionaries
 */
public class TaggerDictionaryReader implements DictionaryReader {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Stream<DictionaryEntry> read() throws IOException {
        String entitiesTsvPath = TaggerAnnotator.DICTIONARY_HOME.resolve(TaggerAnnotator.ENTITIES_TSV).toString();
        logger.debug("Reading the Tagger dictionaries from {}", entitiesTsvPath);

        List<DictionaryEntry> entries = new ArrayList<>();
        try (InputStream in = getClass().getResourceAsStream(entitiesTsvPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    List<String> parts = Lists.newArrayList(Splitter.on("\t").split(line));
                    String typeName = EntityType.fromId(Integer.valueOf(parts.get(1))).name();
                    String entityName = parts.get(2);
                    entries.add(new DictionaryEntry(typeName, entityName));
                } catch (Exception ex) {
                    logger.error("An error occurred while reading the Tagger dictionary on line {}", line, ex);
                    throw ex;
                }
            }
        }

        return entries.stream();
    }
}
