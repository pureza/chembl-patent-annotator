package uk.ac.ebi.chembl.annotator.tagger;

import org.jensenlab.tagger.EntityType;
import org.jensenlab.tagger.Tag;
import org.jensenlab.tagger.Tagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.chembl.annotator.Annotator;
import uk.ac.ebi.chembl.model.Annotation;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


/**
 * The Tagger annotator
 */
public class TaggerAnnotator implements Annotator {

    public static final Path DICTIONARY_HOME = Paths.get(File.separator, "tagger_dictionary");

    public static final String ENTITIES_TSV = "entities.tsv";

    public static final String NAMES_TSV = "names.tsv";

    public static final String GLOBAL_TSV = "global.tsv";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Tagger tagger;


    public TaggerAnnotator(String entitiesPath, String namesPath, String globalPath) {
        // Initialize tagger
        tagger = new Tagger(entitiesPath, namesPath, globalPath);
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
}
