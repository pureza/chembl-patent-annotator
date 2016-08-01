package uk.ac.ebi.chembl.annotator.tagger;

import org.jensenlab.tagger.EntityType;
import org.jensenlab.tagger.Tag;
import org.jensenlab.tagger.Tagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.chembl.annotator.Annotator;
import uk.ac.ebi.chembl.model.Annotation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


/**
 * The Tagger annotator
 */
public class TaggerAnnotator implements Annotator {

    final static String ENTITIES_TSV = "entities.tsv";

    final static String NAMES_TSV = "names.tsv";

    final static String GLOBAL_TSV = "global.tsv";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Tagger tagger;


    static {
        // Loads the Tagger library
        // Requires the command line argument -Djava.library.path=/path/to/libtagger
        System.loadLibrary("tagger");
    }


    public TaggerAnnotator(String dictionariesHome) {
        // Initialize tagger
        Path entitiesTsv = Paths.get(dictionariesHome, ENTITIES_TSV);
        Path namesTsv = Paths.get(dictionariesHome, NAMES_TSV);
        Path globalTsv = Paths.get(dictionariesHome, GLOBAL_TSV);

        tagger = new Tagger(entitiesTsv.toString(), namesTsv.toString(), globalTsv.toString());
        logger.debug("Tagger initialized!");
    }


    @Override
    public List<Annotation> annotate(String text) {
        // Convert Tagger tags to our annotations
        List<Tag> tags = tagger.getMatches(text, EntityType.all);
        return tags.stream()
                .map(tag -> new Annotation(tag.getType().name(), tag.getId(), tag.getTerm(), tag.getStart(), tag.getEnd()))
                .collect(Collectors.toList());

    }


    @Override
    public void shutdown() { /* Nothing to do */ }
}
