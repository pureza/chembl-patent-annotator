package uk.ac.ebi.chembl.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.model.PatentMetadata;
import uk.ac.ebi.chembl.model.PatentXml;
import uk.ac.ebi.chembl.pipeline.PipelineStage;
import uk.ac.ebi.chembl.storage.PatentXmlRepository;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toMap;

/**
 * Downloads the XML of all new patents and stores it in the filesystem
 *
 * This stage of the pipeline receives the metadata for all new patents at once,
 * but writes the XML one by one, because downloading the XML takes time.
 *
 * If the XML for a patent has already been downloaded before, it is skipped.
 *
 * This stage will terminate abruptly on the first error and does not try to
 * perform any kind of recovery. We decided to do it this way because I/O
 * errors usually don't occur in isolation and it's better to fail immediately
 * and fix the cause than to get the same error thousands of times for different
 * patents.
 */
@Component
public class PatentXmlDownloadStep extends PipelineStage<List<PatentMetadata>, List<PatentMetadata>> {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private PatentXmlRepository repository;

    @Value("${patents.xml.home}")
    private String patentsXmlHome;

    private NumberFormat nf = NumberFormat.getInstance();


    @Override
    protected int process(List<PatentMetadata> patents) throws Exception {
        // Partition the new patents according to whether we already have the
        // XML or not
        Map<Boolean, List<PatentMetadata>> partition = splitIntoExistingAndNewFiles(patents);

        List<PatentMetadata> newPatents = partition.get(false);
        List<PatentMetadata> existingPatents = partition.get(true);

        // Forward the existing patents onto the next stage
        out.put(Optional.of(existingPatents));

        // Download the patents we don't have in the file system yet
        downloadAndForwardPatents(newPatents);

        return patents.size();
    }


    /**
     * Partitions the patents into two groups: the ones that will be downloaded
     * and the ones that will be read from the filesystem
     */
    private Map<Boolean, List<PatentMetadata>> splitIntoExistingAndNewFiles(List<PatentMetadata> patents) throws Exception {
        AtomicInteger counter = new AtomicInteger();

        // Running this in parallel increases performance when using modern storage
        // TODO Check SARK
        ForkJoinPool pool = new CustomForkJoinPool();
        return pool.submit(() ->
                patents.stream()
                        .parallel()
                        .collect(partitioningBy(patent -> {
                            // Log progress, because this can take a longe time
                            int cnt = counter.incrementAndGet();
                            if (cnt % 100000 == 0) {
                                logger.debug("Collecting existing patents in {}... ({}%)", patentsXmlHome,
                                        Math.round((double) cnt / patents.size() * 100));
                            }

                            return repository.exists(patent.getPatentNumber());
                        }))).get();
    }


    /**
     * Downloads the XML for a set of new patents, writes it into the file
     * system and forwards it to the next pipeline stage
     *
     * This method will fail immediately when encountering the first I/O error.
     */
    private void downloadAndForwardPatents(List<PatentMetadata> newPatents) throws Exception {
        if (newPatents.isEmpty()) {
            return;
        }

        Map<String, PatentMetadata> metadataMap = newPatents.stream()
                .collect(toMap(PatentMetadata::getPatentNumber, Function.identity()));

        List<String> patentNumbers = new ArrayList<>(metadataMap.keySet());

        // Download the XML (in chunks)
        logger.info("Downloading {} patents to {}...", nf.format(metadataMap.size()), patentsXmlHome);
        Iterator<PatentXml> it = repository.downloadPatentsXml(patentNumbers);

        int counter = 0;
        int noXmlCounter = 0;
        while (it.hasNext()) {
            PatentXml patentXml = it.next();

            if (patentXml.getXml() == null) {
                logger.debug("XML for patent {} doesn't seem to exist. Skipping...", patentXml.getPatentNumber());
                noXmlCounter++;
                continue;
            }

            try {
                // Save the XML in a file, gzipped
                repository.write(patentXml);
                logger.debug("Downloaded patent {}", patentXml.getPatentNumber());
            } catch (IOException ex) {
                // Fail-fast
                logger.error("Unable to save the XML for patent {}.", patentXml.getPatentNumber(), ex);
                throw new RuntimeException(ex);
            }

            // Place this patent in the outgoing queue, to be annotated
            out.put(Optional.of(Collections.singletonList(metadataMap.get(patentXml.getPatentNumber()))));

            // Log progress
            if (++counter % 100 == 0) {
                logger.info("Downloaded patent #{} ({}%)", nf.format(counter), Math.round(((double) counter / patentNumbers.size()) * 100));
            }
        }

        logger.info("Downloaded {} patents", nf.format(counter));
        logger.info("{} patents have no XML. I will try to download these again next time.", nf.format(noXmlCounter));
    }
}


/**
 * Custom ForkJoinPool with our own thread names for prettier logging ;)
 */
class CustomForkJoinPool extends ForkJoinPool {

    CustomForkJoinPool() {
        super(Runtime.getRuntime().availableProcessors() - 1,
                new ForkJoinPool.ForkJoinWorkerThreadFactory() {
                    private AtomicInteger threadCounter = new AtomicInteger();

                    @Override
                    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                        return new ForkJoinWorkerThread(pool) {
                            {
                                this.setName("fork-join-thread-" + threadCounter.incrementAndGet());
                            }
                        };
                    }
                }, null, false);
    }
}
