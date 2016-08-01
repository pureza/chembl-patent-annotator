package uk.ac.ebi.chembl.annotator;


import java.util.function.Supplier;


/**
 * An annotator may not be thread-safe, so each thread in the thread pool will
 * have it's own local annotator
 */
public class ThreadLocalPatentAnnotator extends ThreadLocal<PatentAnnotator> {

    /** Makes a new annotator */
    private Supplier<Annotator> annotatorMaker;


    public ThreadLocalPatentAnnotator(Supplier<Annotator> annotatorMaker) {
        this.annotatorMaker = annotatorMaker;
    }


    @Override
    protected PatentAnnotator initialValue() {
        return new PatentAnnotator(annotatorMaker.get());
    }
}
