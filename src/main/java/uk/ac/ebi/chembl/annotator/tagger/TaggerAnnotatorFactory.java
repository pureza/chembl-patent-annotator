package uk.ac.ebi.chembl.annotator.tagger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.annotator.Annotator;
import uk.ac.ebi.chembl.annotator.AnnotatorFactory;
import uk.ac.ebi.chembl.annotator.DictionaryReader;

/**
 * Initializes the Tagger annotator and its DictionaryReader
 */
@Component
public class TaggerAnnotatorFactory implements AnnotatorFactory {

    @Autowired
    private Environment env;


    @Override
    public String annotatorName() {
        return "tagger";
    }


    @Override
    public Annotator initAnnotator() {
        String dictionariesHome = env.getProperty("tagger.dictionaries.home");
        return new TaggerAnnotator(dictionariesHome);
    }

    @Override
    public DictionaryReader initDictionaryReader() {
        String dictionariesHome = env.getProperty("tagger.dictionaries.home");
        return new TaggerDictionaryReader(dictionariesHome);
    }
}
