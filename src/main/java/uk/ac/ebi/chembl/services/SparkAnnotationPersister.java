package uk.ac.ebi.chembl.services;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.model.*;
import uk.ac.ebi.chembl.storage.AnnotationRepository;
import uk.ac.ebi.chembl.storage.AnnotationRow;
import uk.ac.ebi.chembl.storage.DictionaryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;


/**
 * Persists the annotations
 */
@Component
public class SparkAnnotationPersister {

    Logger logger = LogManager.getLogger(getClass());

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private AnnotationRepository annotationRepository;

    @Autowired
    private AnnotatorMetadata annotatorMetadata;

    @Value("${annotations-output.path}")
    private String annotationsOutputPath;


    public void persist(List<PatentMetadata> patents, JavaRDD<PatentAnnotations> annotationsRdd) {
        // Load the dictionary
        Dictionary dictionary = dictionaryRepository.get(annotatorMetadata);

        // Map used to lookup the PatentMetadata using the patent number
        Map<String, PatentMetadata> metadataMap = patents.stream()
                .collect(toMap(PatentMetadata::getPatentNumber, Function.identity()));

        // Converts a set of PatentAnnotations into AnnotationRows, which are then
        // saved by Spark SQL
        JavaRDD<AnnotationRow> rowsRdd = annotationsRdd.mapPartitions(it -> {
            List<AnnotationRow> rows = new ArrayList<>();

            while (it.hasNext()) {
                PatentAnnotations patentAnnotations = it.next();
                long patentId = metadataMap.get(patentAnnotations.getPatentNumber()).getId();

                patentAnnotations.getAnnotations().forEach((field, annotationsPerRank) -> {
                    int fieldId = field.id();

                    // For each rank (we ignore the rank)
                    for (int i = 0; i < annotationsPerRank.size(); i++) {
                        List<Annotation> annots = annotationsPerRank.get(i);

                        // For each annotation...
                        for (Annotation annotation : annots) {
                            Long bioEntityId = dictionary.getBioEntityId(annotation.getType(), annotation.getName());
                            String term = annotation.getTerm();

                            AnnotationRow row = new AnnotationRow(patentId, fieldId, i, bioEntityId, term, annotation.getStart(), annotation.getEnd());
                            rows.add(row);
                        }
                    }
                });
            }

            return rows.iterator();
        });

        SparkSession spark = SparkSession.builder().getOrCreate();

        Encoder<AnnotationRow> annotationRowEncoder = Encoders.bean(AnnotationRow.class);
        Dataset<AnnotationRow> ds = spark.createDataset(rowsRdd.rdd(), annotationRowEncoder);
        ds.write().mode(SaveMode.Append).save(annotationsOutputPath);

        logger.info("Annotations saved to " + annotationsOutputPath);

        spark.stop();

        // Mark the patents as annotated in the MySQL database
        annotationRepository.markPatentsAsAnnotated(patents, annotatorMetadata.getId());
    }
}
