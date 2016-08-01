package uk.ac.ebi.chembl.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.model.PatentMetadata;
import uk.ac.ebi.chembl.model.PatentXml;
import uk.ac.ebi.chembl.storage.PatentXmlRepository;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

@Component
public class PatentXmlDownloader {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private PatentXmlRepository repository;

    @Value("${patents.xml.home}")
    private String patentsXmlHome;

    private NumberFormat nf = NumberFormat.getInstance();


    /**
     * Checks which patents already exist in the file system and downloads the
     * new ones
     *
     * Returns the set of unannotated patents that exist in the file system and
     * can be annotated. Patents for which the XML does not exist are not
     * returned.
     */
    public List<PatentMetadata> downloadNewPatents(List<PatentMetadata> patents) throws Exception {
        // Partition the new patents according to whether we already have the
        // XML or not
        Map<Boolean, List<PatentMetadata>> partition = splitIntoExistingAndNewFiles(patents);

        List<PatentMetadata> newPatents = partition.get(false);
        List<PatentMetadata> existingPatents = partition.get(true);

        // Download the new patents
        List<PatentMetadata> downloaded = downloadPatents(newPatents);
        return Stream.concat(existingPatents.stream(), downloaded.stream()).collect(toList());
    }


    /**
     * Partitions the patents into two groups: the ones that will be downloaded
     * and the ones that will be read from the filesystem
     */
    private Map<Boolean, List<PatentMetadata>> splitIntoExistingAndNewFiles(List<PatentMetadata> patents) throws Exception {
        AtomicInteger counter = new AtomicInteger();

        // Running this in parallel increases performance when using modern storage
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
     * Downloads the XML for a set of new patents and writes it into the file
     * system
     *
     * Returns the set of patents which were effectively downloaded.
     *
     * This method will fail immediately when encountering the first I/O error.
     */
    private List<PatentMetadata> downloadPatents(List<PatentMetadata> newPatents) throws Exception {
        if (newPatents.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, PatentMetadata> metadataMap = newPatents.stream()
                .collect(toMap(PatentMetadata::getPatentNumber, Function.identity()));

        List<PatentMetadata> downloaded = new ArrayList<>();
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
                downloaded.add(metadataMap.get(patentXml.getPatentNumber()));
                logger.debug("Downloaded patent {}", patentXml.getPatentNumber());
            } catch (IOException ex) {
                // Fail-fast
                logger.error("Unable to save the XML for patent {}.", patentXml.getPatentNumber(), ex);
                throw new RuntimeException(ex);
            }

            // Log progress
            if (++counter % 100 == 0) {
                logger.info("Downloaded patent #{} ({}%)", nf.format(counter), Math.round(((double) counter / patentNumbers.size()) * 100));
            }
        }

        logger.info("Downloaded {} patents", nf.format(counter));
        logger.info("{} patents have no XML. I will try to download these again next time.", nf.format(noXmlCounter));

        return downloaded;
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