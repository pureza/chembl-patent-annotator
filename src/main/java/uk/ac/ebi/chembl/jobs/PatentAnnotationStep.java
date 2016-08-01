package uk.ac.ebi.chembl.jobs;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.annotator.Annotator;
import uk.ac.ebi.chembl.annotator.PatentAnnotator;
import uk.ac.ebi.chembl.annotator.ThreadLocalPatentAnnotator;
import uk.ac.ebi.chembl.model.PatentAnnotations;
import uk.ac.ebi.chembl.model.PatentContent;
import uk.ac.ebi.chembl.model.PatentMetadata;
import uk.ac.ebi.chembl.pipeline.PipelineStage;
import uk.ac.ebi.chembl.services.PatentXmlParser;
import uk.ac.ebi.chembl.storage.PatentXmlRepository;

import javax.annotation.PostConstruct;
import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * This stage annotates each patent, one by one, and forwards the annotations
 * onto the next stage
 *
 * The annotation is done in parallel.
 */
@Component
public class PatentAnnotationStep extends PipelineStage<List<PatentMetadata>, PatentMetadataAndAnnotations> {

    /** The actual patent annotator */
    private static ThreadLocal<PatentAnnotator> annotator;

    private Logger logger = LoggerFactory.getLogger(getClass());

    /** Executor to perform the annotation in parallel */
    private ExecutorService executor;

    /** Used to monitor the status of the annotation tasks */
    private CompletionService<Void> completionService;

    @Autowired
    private PatentXmlRepository repository;

    @Autowired
    private PatentXmlParser parser;

    @Autowired
    private Supplier<Annotator> annotatorMaker;

    @Value("${annotator.threads}")
    private int nThreads;

    private NumberFormat nf = NumberFormat.getInstance();

    /** Number of patents processed */
    private AtomicInteger processed = new AtomicInteger();

    /**
     * Number of annotator tasks that have been submitted but haven't completed yet
     * This variable is only accessed within the stage thread, so it doesn't need
     * to be synchronized.
     */
    private int remainingTasks = 0;


    @PostConstruct
    private void init() {
        annotator = new ThreadLocalPatentAnnotator(annotatorMaker);

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("annotator-thread-%d")
                .setThreadFactory(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r) {
                            @Override
                            public void run() {
                                super.run();

                                // Clean up the thread local annotator before the thread shuts down
                                annotator.get().shutdown();
                            }
                        };
                    }
                })
                .build();

        // The executor that will run the stages in parallel
        this.executor = Executors.newFixedThreadPool(nThreads, threadFactory);

        // The stages are submitted through a CompletionService, so we don't
        // need to wait for them in order
        this.completionService = new ExecutorCompletionService<>(executor);
    }


    @Override
    protected int process(List<PatentMetadata> patents) throws Exception {
        for (PatentMetadata patent : patents) {
            // Distributes the annotation process between multiple threads
            completionService.submit(() -> {
                // First, read the XML
                String xml = repository.read(patent.getPatentNumber());
                PatentContent content = parser.parse(xml);

                logger.debug("Annotating {}", content.getPatentNumber());

                // Annotate the patent!
                PatentAnnotations annotations = annotator.get().processPatent(content);

                // Increment the number of processed elements here
                int processed = incProcessed(1);

                out.put(Optional.of(new PatentMetadataAndAnnotations(patent, annotations)));

                if (processed % 10000 == 0) {
                    logger.info("Annotated patent #{}", nf.format(processed));
                }

                return null;
            });
            remainingTasks++;
        }

        // Check for errors on the tasks that have already completed and abort if any failed
        abortOnError(false);

        // 0 means no entries were processed. We update the number of processed entries
        // manually in the annotation task above
        return 0;
    }


    @Override
    protected void injectPoisonPill() throws Exception {
        // Initiate a normal shutdown of the executor
        executor.shutdown();

        // Check for errors on the remaining tasks, waiting if necessary for
        // them to complete, and abort if any fails
        abortOnError(true);

        executor.awaitTermination(1, TimeUnit.DAYS);

        super.injectPoisonPill();
    }


    @Override
    protected void onFailure() throws Exception {
        super.onFailure();

        // Terminate the executor now!
        executor.shutdownNow();
    }


    @Override
    protected int incProcessed(int delta) {
        return processed.addAndGet(delta);
    }


    @Override
    public int processed() {
        return processed.get();
    }


    private void abortOnError(boolean wait) throws Exception {
        Future<Void> future;
        while (remainingTasks > 0) {
            future = wait ? completionService.take() : completionService.poll();
            if (future == null) {
                break;
            }

            future.get();
            remainingTasks--;
        }
    }
}
