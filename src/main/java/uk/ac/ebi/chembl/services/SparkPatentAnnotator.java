package uk.ac.ebi.chembl.services;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.annotator.PatentAnnotator;
import uk.ac.ebi.chembl.annotator.tagger.TaggerAnnotatorFactory;
import uk.ac.ebi.chembl.model.PatentAnnotations;
import uk.ac.ebi.chembl.model.PatentContent;
import uk.ac.ebi.chembl.model.PatentMetadata;
import uk.ac.ebi.chembl.storage.PatentXmlRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import static java.util.stream.Collectors.joining;

/**
 * Annotates a set of patents concurrently in Spark/Hadoop
 *
 * Each worker will further process its annotations in multiple threads.
 */
@Component
public class SparkPatentAnnotator {

    @Autowired
    private JavaSparkContext sparkContext;

    @Value("${patents.xml.home}")
    private String patentsXmlHome;

    @Autowired
    private TaggerAnnotatorFactory annotatorFactory;

    @Value("${annotator.threads}")
    private int nThreads;

    @Value("${hadoop.partitions}")
    private int partitions;


    public JavaRDD<PatentAnnotations> annotate(List<PatentMetadata> patents) {
        // The RDD (Resilient Distributed Dataset) with the paths to the
        // patents to annotate, in HDFS
        JavaRDD<String> rdd = initRdd(sparkContext, patents);

        TaggerAnnotatorFactory annotatorFactory = this.annotatorFactory;
        int nThreads = this.nThreads;

        // Each worker in the cluster will get a partition with paths to annotate
        return rdd.mapPartitionsWithIndex((index, it) -> {
            // This bit will execute at the worker

            Logger logger = LogManager.getLogger("SparkPatentAnnotator");
            logger.info("Partition " + index + " started and ready to annotate ");

            // The thread pool to annotate patents in parallel
            ExecutorService executor = Executors.newFixedThreadPool(nThreads);

            PatentAnnotator annotator = new PatentAnnotator(annotatorFactory.initAnnotator());
            PatentXmlParser parser = new PatentXmlParser();

            // All the annotations collected by this partition
            Queue<PatentAnnotations> partitionAnnotations = new ConcurrentLinkedQueue<>();

            // For logging purposes
            long start = System.currentTimeMillis();
            final AtomicInteger count = new AtomicInteger();

            final NumberFormat nf = NumberFormat.getInstance();

            // Create and submit a task to annotate each patent
            while (it.hasNext()) {
                String xml = it.next();

                executor.submit(() -> {
                    PatentContent content = parser.parse(xml);
                    PatentAnnotations annotations = annotator.processPatent(content);
                    
                    // Put the annotations in the shared queue where the Database
                    // Writer thread will take them from
                    partitionAnnotations.offer(annotations);

                    if (count.incrementAndGet() % 10000 == 0) {
                        long now = System.currentTimeMillis();
                        int speed = (int) (count.get() / ((now - start) / 1000.0));
                        logger.debug("Annotated " + nf.format(count.get()) + " patents at a speed of " + nf.format(speed) + " patents per second");
                    };
                });
            }

            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.DAYS);

            logger.info("Finished annotating " + nf.format(count.get()) + " patents");

            return partitionAnnotations.iterator();
        }, true);
    }


    /**
     * Initializes the RDD with all the patent files that will be annotated
     */
    private JavaRDD<String> initRdd(JavaSparkContext sparkContext, List<PatentMetadata> patents) {
        JavaRDD<String> rdd = sparkContext.emptyRDD();

        /*
         * We do this in chunks because:
         * - If we do it one by one, it takes too long and uses too much memory
         * - If we do all at once, the resulting RDD contains few partitions
         */

        int chunkSize = 100000;
        int limit = patents.size();
        int i = 0;
        while (i * chunkSize < limit) {
            int start = i * chunkSize;
            int end = Math.min(limit, (i + 1) * chunkSize);

            List<PatentMetadata> chunk = patents.subList(start, end);
            String paths = chunk.stream()
                    .map(PatentMetadata::getPatentNumber)
                    .map(this::getPath)
                    .map(Path::toString)
                    .collect(joining(","));

            rdd = rdd.union(sparkContext.wholeTextFiles(paths).values());
            i++;
        }

        // Coalesce to the desired number of partitions
        return rdd.coalesce(partitions);
    }


    private Path getPath(String patentNumber) {
        Matcher matcher = PatentXmlRepository.PATH_PATTERN.matcher(patentNumber);
        if (matcher.find()) {
            String authority = matcher.group(1);
            String level1 = matcher.group(3);
            String level2 = matcher.group(2);

            return Paths.get(patentsXmlHome, authority, level1, level2, patentNumber + ".xml.gz");
        } else {
            // A few patents don't match the pattern: put them all together inside foo/
            return Paths.get(patentsXmlHome, "foo", patentNumber + ".xml.gz");
        }
    }
}
