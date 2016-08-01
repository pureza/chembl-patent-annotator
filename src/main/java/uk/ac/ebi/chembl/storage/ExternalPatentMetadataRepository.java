package uk.ac.ebi.chembl.storage;

import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.ac.ebi.chembl.model.PatentMetadata;
import uk.ac.ebi.chembl.storage.dao.SureChemblPatentMetadataDao;
import uk.ac.ebi.chembl.storage.dao.SureProPatentMetadataDao;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.Iterator;

/**
 * Repository to retrieve the patent metadata from the external sources
 */
@Service
public class ExternalPatentMetadataRepository {

    private Logger logger = LoggerFactory.getLogger(getClass());


    @Autowired
    @Qualifier("sureChemblHandle")
    private DBI sureChemblHandle;

    @Autowired
    @Qualifier("sureProHandle")
    private DBI sureProHandle;

    private SureChemblPatentMetadataDao sureChemblDao;

    private SureProPatentMetadataDao sureProDao;


    @PostConstruct
    private void init() {
        this.sureChemblDao = sureChemblHandle.open(SureChemblPatentMetadataDao.class);
        this.sureProDao = sureProHandle.open(SureProPatentMetadataDao.class);
    }


    /**
     * Retrieves all patents in the source database
     */
    public Iterator<PatentMetadata> getAllPatents() {
       return sureProDao.retrieveAll();
    }


    /**
     * Finds all patents published after the given date (inclusive).
     */
    public Iterator<PatentMetadata> getPatentsPublishedAfter(Date date) {
        return sureChemblDao.retrievePatentsPublishedAfter(date);
    }
}
