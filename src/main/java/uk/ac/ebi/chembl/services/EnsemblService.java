package uk.ac.ebi.chembl.services;

import com.clearspring.analytics.util.Lists;
import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.chembl.storage.ExternalEnsemblRepository;
import uk.ac.ebi.chembl.storage.LocalEnsemblRepository;
import uk.ac.ebi.chembl.storage.dao.EnsemblPeptideUniProt;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * The Ensembl Service
 *
 * Keeps track of the mapping between Ensembl Peptide ids to UniProt accessions.
 *
 * This mapping is necessary to produce the final output files for IDG, because
 * the Tagger dictionary uses Ensembl Peptide ids while the list of IDG Targets
 * contains UniProt accessions.
 */
@Component
public class EnsemblService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /** List of Ensembl Releases to check when doing the mapping */
    @Value("${ensembl.releases}")
    private String ensemblReleases;

    /** The Ensembl database */
    @Autowired
    private ExternalEnsemblRepository externalRepository;

    /** Our local database where the mapping is saved to */
    @Autowired
    private LocalEnsemblRepository localRepository;

    private NumberFormat nf = NumberFormat.getInstance();


    /**
     * Checks if the mapping stored in the database needs to be updated, due to
     * a new Ensembl release
     */
    public boolean needsUpdate() {
        List<String> releases = ensemblReleases();
        if (releases.isEmpty()) {
            throw new IllegalArgumentException("Please specify the list of Ensembl releases to check in the configuration file");
        }

        String latestRelease = releases.get(0);
        String dbLatestRelease = localRepository.retrieveLatestRelease();

        return !latestRelease.equals(dbLatestRelease);
    }


    /**
     * Maps the given Ensembl Peptide ids to UniProt accessions and saves the
     * correspondence to the database
     */
    public void updateMapping(Set<String> ensemblPeptideIds) {
        logger.info("The Ensembl Peptide to UniProt mapping will now be updated for the latest Ensembl release: {}...", ensemblReleases().get(0));

        List<EnsemblPeptideUniProt> mapping = mapToUniprot(ensemblPeptideIds);
        localRepository.save(mapping);

        if (logger.isInfoEnabled()) {
            Set<String> mappedPeptides = mapping.stream()
                    .map(EnsemblPeptideUniProt::getEnsemblPeptideId)
                    .collect(Collectors.toSet());
            logger.info("Mapped {} Ensembl Peptide ids to UniProt accessions. {} ids were not mapped!",
                    nf.format(mapping.size()), nf.format(ensemblPeptideIds.size() - mappedPeptides.size()));
        }
    }


    /**
     * Maps the given Ensembl Peptide ids to UniProt accessions, by trying each
     * Ensembl release, from the most recent to the oldest
     *
     * It ignores deprecated mapping to UniProt accessions that are also being
     * mapped by another Ensembl Peptide id in a newer release.
     *
     * Ensembl Peptide ids that can't be mapped to UniProt accessions are
     * ignored.
     */
    private List<EnsemblPeptideUniProt> mapToUniprot(Set<String> ensemblPeptideIds) {
        // List of Ensembl releases to check, in order
        List<String> releases = ensemblReleases();

        Set<String> remaining = new HashSet<>(ensemblPeptideIds);
        Set<String> mappedUniProtAcc = new HashSet<>();
        List<EnsemblPeptideUniProt> mapping = new ArrayList<>();
        for (String release : releases) {
            List<EnsemblPeptideUniProt> releaseMapping = mapToUniprotAtRelease(remaining, release);

            // Ignore old mappings to UniProt accessions for which we already have a newer mapping
            List<EnsemblPeptideUniProt> mappingWithNewUniprot = releaseMapping.stream()
                    .filter(xref -> !mappedUniProtAcc.contains(xref.getUniprotAcc()))
                    .collect(toList());

            Set<String> releaseUniProtAccessions = mappingWithNewUniprot.stream()
                    .map(EnsemblPeptideUniProt::getUniprotAcc)
                    .collect(toSet());

            // Add the UniProt accessions mapped in this release to the global set
            mappedUniProtAcc.addAll(releaseUniProtAccessions);

            Set<String> releaseEnsemblPeptideIds = releaseMapping.stream()
                    .map(EnsemblPeptideUniProt::getEnsemblPeptideId)
                    .collect(toSet());

            // Remove the Ensembl Peptide ids mapped in this release
            remaining.removeAll(releaseEnsemblPeptideIds);

            // Add this release mappings to the global mappings
            mapping.addAll(mappingWithNewUniprot);

            logger.debug("Mapped {} ensembl peptide ids in release {}. {} left.",
                    nf.format(mappingWithNewUniprot.size()), release, nf.format(remaining.size()));

            if (remaining.isEmpty()) {
                break;
            }
        }

        return mapping;
    }


    /**
     * Maps the given Ensembl Peptide ids to UniProt accessions at a specific
     * Ensembl release
     */
    private List<EnsemblPeptideUniProt> mapToUniprotAtRelease(Set<String> ensemblPeptideIds, String ensemblRelease) {
        return externalRepository.mapToUniprotAtRelease(ensemblPeptideIds.stream().collect(toList()), ensemblRelease);
    }


    /**
     * Returns the list of Ensembl releases to check, in order
     */
    private List<String> ensemblReleases() {
        return Lists.newArrayList(Splitter.on(",").trimResults().split(ensemblReleases));
    }
}
