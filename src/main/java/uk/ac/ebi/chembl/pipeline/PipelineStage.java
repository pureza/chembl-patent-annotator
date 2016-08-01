package uk.ac.ebi.chembl.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A stage of the pipeline.
 *
 * A stage receives items from an input queue, processes them and passes the
 * results to an output queue.
 *
 *
 * Error semantics:
 * When an unrecoverable error occurs at a certain stage of the pipeline, an
 * exception should be thrown by the process() method. The Pipeline will then
 * take care of shutting down the remaining stages orderly.
 */
public abstract class PipelineStage<I, O> {

    /** Size of the outgoing queue */
    private static final int QUEUE_SIZE = 8192;

    /** Logging interval, in number of items processed */
    private static final int LOG_INTERVAL = 1000;

    protected Logger logger = LoggerFactory.getLogger(getClass());

    /** Input queue */
    protected BlockingQueue<Optional<I>> in;

    /** Output queue */
    protected BlockingQueue<Optional<O>> out;

    /* Number of items processed (for statistics purposes) */
    private int processed = 0;

    private NumberFormat nf = NumberFormat.getInstance();


    public PipelineStage() {
        out = new ArrayBlockingQueue<>(QUEUE_SIZE);
    }


    public void run() throws Exception {
        long start = System.currentTimeMillis();

        beforeRun();

        while (true) {
            Optional<I> opt = in.take();

            // Check for termination
            if (!opt.isPresent()) {
                // Process any buffered elements
                incProcessed(onSuccess());

                // Pass on the termination signal to the next stage.
                injectPoisonPill();
                break;
            }

            I incoming = opt.get();
            int beforeProcessed = processed();
            int processed = incProcessed(process(incoming));

            // If at least LOG_INTERVAL entries were processed since the last
            // log, log now.
            if (processed > 0 && ((processed / LOG_INTERVAL) != (beforeProcessed / LOG_INTERVAL))) {
                double elapsed = (System.currentTimeMillis() - start) / 1000.0;
                int speed = (int) (processed / elapsed);
                logger.debug("Processed entry #{} at {} entries/s ", nf.format(processed), speed);
            }
        }

        logger.info("Done! {} entries processed.", nf.format(processed()));
    }


    /**
     * Processes an item
     */
    protected abstract int process(I incoming) throws Exception;


    /**
     * Do something before this stage processes the first item
     */
    protected void beforeRun() throws Exception { }


    /**
     * Do something when the stage finishes normally
     */
    protected int onSuccess() throws Exception {
        return 0;
    }


    /**
     * Do something when there was a failure in the pipeline (possibly in
     * another stage)
     */
    protected void onFailure() throws Exception { }


    /**
     * Number of items processed
     */
    public int processed() {
        return processed;
    }


    /**
     * Increments and returns the number of items processed
     */
    protected int incProcessed(int amount) {
        this.processed += amount;
        return this.processed;
    }


    /**
     * Injects the termination signal into the output queue
     */
    protected void injectPoisonPill() throws Exception {
        out.put(Optional.<O>empty());
    }


    public BlockingQueue<Optional<I>> getIn() {
        return in;
    }


    public void setIn(BlockingQueue<Optional<I>> in) {
        this.in = in;
    }


    public BlockingQueue<Optional<O>> getOut() {
        return out;
    }
}