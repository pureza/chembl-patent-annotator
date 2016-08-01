package uk.ac.ebi.chembl.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.model.IdgTarget;
import uk.ac.ebi.chembl.storage.IdgTargetRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the list of relevant targets from the file
 */
@Component
public class IdgTargetLoader {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IdgTargetRepository repository;

    @Autowired
    private ApplicationContext ctx;

    @Value("${idg-targets.path}")
    private String idgTargetsPath;

    private NumberFormat nf = NumberFormat.getInstance();

    public void load() throws IOException {
        logger.debug("Loading the IDG targets into the database...");

        int deletedRows = repository.clear();
        List<IdgTarget> targets = loadTargets();
        repository.save(targets);

        if (targets.size() != deletedRows) {
            logger.info("Loaded {} IDG targets into the database (previously there were {})",
                    nf.format(targets.size()), nf.format(deletedRows));
        }
    }


    /**
     * Loads the information for the IDG targets from the file
     */
    private List<IdgTarget> loadTargets() throws IOException {
        List<IdgTarget> targets = new ArrayList<>();

        Resource file = ctx.getResource(idgTargetsPath);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            // Skip the header
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split("\t");

                // The columns:
                // URL | Uniprot ID | Name | Description | Development Level | DTOClass | PantherClass | ChemblClass | Novelty | Target Family | Function | GrantCount | R01Count | PatentCount | AntibodyCount | PubmedCount | PMIDs

                String uniProtAccession = columns[1];
                String name = columns[2];
                String developmentLevel = columns[4];
                String targetFamily = columns[5];

                IdgTarget target = new IdgTarget(uniProtAccession, name, developmentLevel, targetFamily);
                targets.add(target);
            }

        }

        return targets;
    }
}
