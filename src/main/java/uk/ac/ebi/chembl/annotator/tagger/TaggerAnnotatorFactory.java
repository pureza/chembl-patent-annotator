package uk.ac.ebi.chembl.annotator.tagger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.annotator.Annotator;
import uk.ac.ebi.chembl.annotator.AnnotatorFactory;
import uk.ac.ebi.chembl.annotator.DictionaryReader;
import uk.ac.ebi.chembl.util.Resources;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Initializes the Tagger annotator and its DictionaryReader
 */
@Component
public class TaggerAnnotatorFactory implements AnnotatorFactory, Serializable {

    private final Logger logger = LoggerFactory.getLogger(getClass());


    @Override
    public String annotatorName() {
        return "tagger";
    }


    @Override
    public Annotator initAnnotator() {
        try {
            // Extract the native libraries into a temporary file and load them
            Resources.loadResourceLibrary(Paths.get("/libicudata.so.42"));
            Resources.loadResourceLibrary(Paths.get("/libicuuc.so.42"));
            Resources.loadResourceLibrary(Paths.get("/libicui18n.so.42"));
            Resources.loadResourceLibrary(Paths.get("/libboost_regex.so.5"));
            Resources.loadResourceLibrary(Paths.get("/libtagger.so"));

            // Extract the dictionary into temporary files that libtagger can read
            Path dictionariesHome = TaggerAnnotator.DICTIONARY_HOME;
            Path entitiesPath = Resources.extractResourceIntoTmp(dictionariesHome.resolve(TaggerAnnotator.ENTITIES_TSV));
            Path namesPath = Resources.extractResourceIntoTmp(dictionariesHome.resolve(TaggerAnnotator.NAMES_TSV));
            Path globalPath = Resources.extractResourceIntoTmp(dictionariesHome.resolve(TaggerAnnotator.GLOBAL_TSV));

            return new TaggerAnnotator(entitiesPath.toString(), namesPath.toString(), globalPath.toString());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    @Override
    public DictionaryReader initDictionaryReader() {
        return new TaggerDictionaryReader();
    }
}
