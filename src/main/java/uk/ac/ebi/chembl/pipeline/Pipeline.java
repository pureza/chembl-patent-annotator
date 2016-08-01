package uk.ac.ebi.chembl.pipeline;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

public abstract class Pipeline {

    private Logger logger = LoggerFactory.getLogger(getClass());


    /**
     * Run the pipeline
     *
     * Each stage is executed its own thread.
     */
    public void run() throws Exception {
        final List<PipelineStage<?, ?>> stages = setUp();
        int nThreads = stages.size();

        // The executor that will run the stages in parallel
        ExecutorService executor = Executors.newFixedThreadPool(nThreads,
                new ThreadFactoryBuilder().setNameFormat("pipeline-thread-%d").build());

        // The stages are submitted through a CompletionService, so we don't
        // need to wait for them in order
        CompletionService<Void> completionService =
                new ExecutorCompletionService<>(executor);

        // Run the stages concurrently
        for (final PipelineStage<?, ?> stage : stages) {
            completionService.submit(() -> runStage(stage));
        }

        // Wait for all stages to terminate, in any order. If any fails, abort
        // the whole thing!
        for (int i = 0; i < stages.size(); i++) {
            Future<Void> future = completionService.take();

            try {
                future.get();
                // This stage terminated normally. Wait for the next one!
            } catch (Exception ex) {
                // This stage failed with an error. Terminate the whole pipeline!
                executor.shutdownNow();
            }
        }

        // Shutdown the executor when all stages completed normally
        executor.shutdown();
    }


    /**
     * Setup the pipeline, by creating the necessary stages and connecting them
     */
    protected abstract List<PipelineStage<?, ?>> setUp();


    protected <O> void pipeThrough(PipelineStage<?, O> in, PipelineStage<O, ?> out) {
        out.setIn(in.getOut());
    }


    /**
     * Runs the main loop of a stage and does some error handling
     */
    private Void runStage(final PipelineStage<?, ?> stage) throws Exception {
        try {
            // Run the main loop of each stage
            stage.run();
        } catch (InterruptedException ex) {
            // Another stage has failed and aborted the whole pipeline!
            logger.debug("Stage {} was interrupted by a failure on another stage!", stage, ex);

            try {
                // Give the stage the opportunity to do some cleanup
                stage.onFailure();
            } catch (Exception e) {
                logger.debug("An error occurred while invoking onFailure on stage {}", stage, e);
            }

            logger.error("Stage {} has been cancelled! {} entries processed.", stage, stage.processed());
        } catch (Exception e) {
            logger.error("An exception occurred in stage {}. Aborting...", stage, e);

            // Give the stage the opportunity to do some cleanup
            stage.onFailure();

            // This will cause an Exception on the future.get() which will
            // initiate a shutdownNow()
            throw e;
        }

        return null;
    }
}