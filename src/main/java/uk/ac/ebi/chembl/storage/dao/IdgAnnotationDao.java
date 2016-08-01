package uk.ac.ebi.chembl.storage.dao;

import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import uk.ac.ebi.chembl.model.IdgAnnotation;

import java.util.Iterator;


/**
 * Data Access Object for IDG annotations
 */
@RegisterMapper(IdgAnnotationMapper.class)
public interface IdgAnnotationDao {

    /**
     * Retrieves all IDG annotations
     */
    @SqlQuery("SELECT entity_name, uniprot_acc, target_name, development_level, target_family, patent_number, publication_date," +
            "         sum(total_frequency) AS total_frequency, sum(description) AS description, sum(clms) AS clms, sum(abst) AS abst, sum(ttl) AS ttl," +
            "         group_concat(concat(term, ':', total_frequency) ORDER BY total_frequency DESC) AS term_frequency" +
            "    FROM (" +
            "      SELECT idg.entity_name, idg.uniprot_acc, idg.target_name, idg.development_level, idg.target_family, pat.patent_number, pat.publication_date," +
            "              sum(stats.frequency) AS total_frequency," +
            "              sum(CASE stats.field_id WHEN 1 THEN stats.frequency ELSE 0 END) AS description," +
            "              sum(CASE stats.field_id WHEN 2 THEN stats.frequency ELSE 0 END) AS clms," +
            "              sum(CASE stats.field_id WHEN 3 THEN stats.frequency ELSE 0 END) AS abst," +
            "              sum(CASE stats.field_id WHEN 4 THEN stats.frequency ELSE 0 END) AS ttl," +
            "              term" +
            "        FROM bioentity_patent_annotation_count stats, patent pat, idg_entity idg, bio_entity en, bio_type ty, annotator an" +
            "       WHERE idg.bio_entity_id = stats.bio_entity_id" +
            "         AND stats.patent_id = pat.patent_id" +
            "         AND stats.bio_entity_id = en.bio_entity_id" +
            "         AND en.bio_type_id = ty.bio_type_id" +
            "         AND ty.annotator_id = an.annotator_id" +
            "         AND an.name = 'tagger'" +
            "       GROUP BY idg.bio_entity_id, idg.entity_name, idg.uniprot_acc, idg.target_name, idg.development_level, idg.target_family," +
            "          pat.patent_number, pat.publication_date, term" +
            "   ) s" +
            "   GROUP BY entity_name, uniprot_acc, target_name, development_level, target_family, patent_number, publication_date")
    Iterator<IdgAnnotation> retrieveIdgAnnotations();


    /**
     * Closes the DAO and releases any allocated database resources
     */
    void close();
}
