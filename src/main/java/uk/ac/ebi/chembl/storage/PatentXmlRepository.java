package uk.ac.ebi.chembl.storage;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.api.java.JavaSparkContext;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ebi.chembl.model.PatentXml;
import uk.ac.ebi.chembl.storage.dao.PatentXmlDao;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * Repository for Patent's XML
 */
@Service
public class PatentXmlRepository {

    /** How many patents to download per batch? */
    private static final int DOWNLOAD_BATCH_SIZE = 1000; // Must be <= 1664

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private JavaSparkContext sparkContext;

    @Autowired
    @Qualifier("alexandriaHandle")
    private DBI alexandriaHandle;

    @Value("${patents.xml.home}")
    private String patentsXmlHome;

    private PatentXmlDao dao;

    /** Regular expression to extract US/67/45 from US-1234567-A1 */
    public static final Pattern PATH_PATTERN = Pattern.compile("(\\w\\w)-\\w*(\\w\\w)(\\w\\w)-");


    @PostConstruct
    private void init() {
        this.dao = alexandriaHandle.open(PatentXmlDao.class);
    }


    /**
     * Downloads the XML for the given patents
     */
    public Iterator<PatentXml> downloadPatentsXml(List<String> patentNumbers) {
        // Download the XML in batches, to avoid "Too many parameters" Postgres error
        return new Iterator<PatentXml>() {

            /** Current batch */
            private int batchIndex = 0;

            /** Current batch iterator */
            private Iterator<PatentXml> batchIt;


            @Override
            public boolean hasNext() {
                return (batchIt != null && batchIt.hasNext()) || hasNextBatch();
            }


            @Override
            public PatentXml next() {
                if (batchIt != null && batchIt.hasNext()) {
                    return batchIt.next();
                }

                if (hasNextBatch()) {
                    batchIt = nextBatch();
                    return batchIt.next();
                }

                throw new NoSuchElementException();
            }


            private Iterator<PatentXml> nextBatch() {
                int start = batchIndex * DOWNLOAD_BATCH_SIZE;
                int end = Math.min((batchIndex + 1) * DOWNLOAD_BATCH_SIZE, patentNumbers.size());
                List<String> chunk = patentNumbers.subList(start, end);
                batchIndex++;
                return dao.retrieveBatch(chunk);
            }


            private boolean hasNextBatch() {
                int nextChunkStart = batchIndex * DOWNLOAD_BATCH_SIZE;
                return nextChunkStart < patentNumbers.size();
            }
        };
    }


    /**
     * Checks if a patent exists in the file system
     */
    public boolean exists(String patentNumber) {
        try {
            Configuration conf = sparkContext.hadoopConfiguration();
            FileSystem fs = FileSystem.get(conf);
            return fs.exists(getPath(patentNumber));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * Writes a patent to the file system
     */
    public void write(PatentXml patentXml) throws IOException {
        byte[] xml = patentXml.getXml().getBytes();

        Configuration conf = sparkContext.hadoopConfiguration();
        FileSystem fs = FileSystem.get(conf);

        org.apache.hadoop.fs.Path path = getPath(patentXml.getPatentNumber());

        try (FSDataOutputStream out = fs.create(path);
             GZIPOutputStream gzOut = new GZIPOutputStream(out)) {
            gzOut.write(xml);
        }

        logger.debug("Downloaded XML for patent " + patentXml.getPatentNumber());
    }


    private org.apache.hadoop.fs.Path getPath(String patentNumber) {
        Matcher matcher = PATH_PATTERN.matcher(patentNumber);
        Path path;
        if (matcher.find()) {
            String authority = matcher.group(1);
            String level1 = matcher.group(3);
            String level2 = matcher.group(2);

            path = Paths.get(patentsXmlHome, authority, level1, level2, patentNumber + ".xml.gz");
        } else {
            // A few patents don't match the pattern: put them all together inside foo/
            path = Paths.get(patentsXmlHome, "foo", patentNumber + ".xml.gz");
        }

        return new org.apache.hadoop.fs.Path(path.toString());
    }
}
