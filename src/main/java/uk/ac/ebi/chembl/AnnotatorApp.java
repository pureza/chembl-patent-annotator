package uk.ac.ebi.chembl;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uk.ac.ebi.chembl.annotator.Annotator;
import uk.ac.ebi.chembl.annotator.AnnotatorFactory;
import uk.ac.ebi.chembl.annotator.DictionaryReader;
import uk.ac.ebi.chembl.jobs.AnnotatorJob;
import uk.ac.ebi.chembl.model.AnnotatorMetadata;
import uk.ac.ebi.chembl.model.Dictionary;
import uk.ac.ebi.chembl.services.DictionaryAnalyzer;
import uk.ac.ebi.chembl.services.EnsemblService;
import uk.ac.ebi.chembl.services.IdgTargetLoader;
import uk.ac.ebi.chembl.storage.*;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@SpringBootApplication
public class AnnotatorApp {

    private static Logger logger = LoggerFactory.getLogger("AnnotatorApp");

    /**
     * The main database, where we write the patent annotations to
     */
    @Bean
    DBI patentAnnotHandle(@Value("${${patentannot.url}}") String url,
                          @Value("${patentannot.user}") String user,
                          @Value("${patentannot.password}") String password) {
        PoolProperties props = new PoolProperties();
        props.setDriverClassName(com.mysql.jdbc.Driver.class.getName());
        props.setUrl(url);
        props.setUsername(user);
        props.setPassword(password);

        org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
        dataSource.setPoolProperties(props);
        return new DBI(dataSource);
    }


    /**
     * SurePro database, to retrieve the patent metadata for the first time
     */
    @Bean
    DBI sureProHandle(@Value("${surepro.url}") String url,
                      @Value("${surepro.user}") String user,
                      @Value("${surepro.password}") String password) {
        PoolProperties props = new PoolProperties();
        props.setDriverClassName(oracle.jdbc.OracleDriver.class.getName());
        props.setUrl(url);
        props.setUsername(user);
        props.setPassword(password);

        org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
        dataSource.setPoolProperties(props);
        return new DBI(dataSource);
    }


    /**
     * SureChembl database, to retrieve the patent metadata for new patents
     */
    @Bean
    DBI sureChemblHandle(@Value("${surechem.url}") String url,
                         @Value("${surechem.user}") String user,
                         @Value("${surechem.password}") String password) {
        PoolProperties props = new PoolProperties();
        props.setDriverClassName(com.mysql.jdbc.Driver.class.getName());
        props.setUrl(url);
        props.setUsername(user);
        props.setPassword(password);

        org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
        dataSource.setPoolProperties(props);
        return new DBI(dataSource);
    }


    /**
     * Alexandria database, used to fetch the Patent's XML
     */
    @Bean
    DBI alexandriaHandle(@Value("${alexandria.url}") String url,
                         @Value("${alexandria.user}") String user,
                         @Value("${alexandria.password}") String password) {
        PoolProperties props = new PoolProperties();
        props.setDriverClassName(org.postgresql.Driver.class.getName());
        props.setUrl(url);
        props.setUsername(user);
        props.setPassword(password);

        org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
        dataSource.setPoolProperties(props);
        return new DBI(dataSource);
    }


    /**
     * Ensembl database, to map the Ensembl peptide ids used by Tagger to
     * Uniprot accessions
     */
    @Bean
    DBI ensemblHandle(@Value("${ensembl.url}") String url,
                      @Value("${ensembl.user}") String user,
                      @Value("${ensembl.password}") String password) {
        PoolProperties props = new PoolProperties();
        props.setDriverClassName(com.mysql.jdbc.Driver.class.getName());
        props.setUrl(url);
        props.setUsername(user);
        props.setPassword(password);

        org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
        dataSource.setPoolProperties(props);
        return new DBI(dataSource);
    }


    /**
     * The factory that will setup the annotator that has been chosen to
     * execute according to the configuration
     */
    @Bean
    @Primary
    AnnotatorFactory annotatorFactory(ApplicationContext context, @Value("${annotator}") String annotatorName) {
        Map<String, AnnotatorFactory> factories = context.getBeansOfType(AnnotatorFactory.class);
        List<AnnotatorFactory> applicableFactories = factories.values()
                .stream()
                .filter(factory -> factory.annotatorName().equals(annotatorName))
                .collect(toList());

        if (applicableFactories.isEmpty()) {
            throw new IllegalStateException("No annotator factories were found for " + annotatorName
                    + " in the application context!");
        } else if (applicableFactories.size() > 1) {
            throw new IllegalStateException("More than one annotator factory was found for " + annotatorName +
                    " in the application context: " + applicableFactories);
        } else {
            AnnotatorFactory factory = applicableFactories.get(0);
            logger.debug("Found one applicable annotator factory: {}", factory);
            return factory;
        }
    }



    @Bean
    Supplier<Annotator> annotatorMaker(AnnotatorFactory factory) {
        return factory::initAnnotator;
    }


    @Bean
    DictionaryReader dictionaryReader(AnnotatorFactory factory) {
        return factory.initDictionaryReader();
    };


    @Bean
    PatentMetadataRepository patentMetadataRepository(@Qualifier("patentAnnotHandle") DBI patentAnnotHandle) {
        return patentAnnotHandle.onDemand(PatentMetadataRepository.class);
    }


    @Bean
    AnnotationRepository annotationRepository(@Qualifier("patentAnnotHandle") DBI patentAnnotHandle) {
        return patentAnnotHandle.onDemand(AnnotationRepository.class);
    }


    @Bean
    DictionaryRepository dictionaryRepository(@Qualifier("patentAnnotHandle") DBI patentAnnotHandle) {
        return patentAnnotHandle.onDemand(DictionaryRepository.class);
    }


    @Bean
    AnnotatorRepository annotatorRepository(@Qualifier("patentAnnotHandle") DBI patentAnnotHandle) {
        return patentAnnotHandle.onDemand(AnnotatorRepository.class);
    }


    @Bean
    IdgTargetRepository idgTargetRepository(@Qualifier("patentAnnotHandle") DBI patentAnnotHandle) {
        return patentAnnotHandle.onDemand(IdgTargetRepository.class);
    }


    @Bean
    ExternalEnsemblRepository externalEnsemblRepository(@Qualifier("ensemblHandle") DBI ensemblHandle) {
        return ensemblHandle.onDemand(ExternalEnsemblRepository.class);
    }


    @Bean
    LocalEnsemblRepository localEnsemblRepository(@Qualifier("patentAnnotHandle") DBI patentAnnotHandle) {
        return patentAnnotHandle.onDemand(LocalEnsemblRepository.class);
    }


    @Bean
    IdgAnnotationRepository idgAnnotationRepository(@Qualifier("patentAnnotHandle") DBI patentAnnotHandle) {
        return patentAnnotHandle.onDemand(IdgAnnotationRepository.class);
    }


    @Bean
    AnnotatorMetadata annotatorMetadata(@Qualifier("patentAnnotHandle") DBI patentAnnotHandle,
                                        @Value("${annotator}") String annotatorName) {
        // Make sure this annotator is known to the database
        return annotatorRepository(patentAnnotHandle).getOrCreate(annotatorName);
    }


    @Bean
    JavaSparkContext sparkContext() {
        SparkConf conf = new SparkConf().setAppName("Patent annotator");
        return new JavaSparkContext(conf);
    }


    @Autowired
    private DictionaryAnalyzer dictionaryAnalyzer;

    @Autowired
    private AnnotatorMetadata annotator;

    @Autowired
    private IdgTargetLoader idgTargetLoader;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private EnsemblService ensemblService;

    @Value("${email.to:#{null}}")
    private String emailTo;

    @Value("${skip.output:#{false}}")
    private boolean skipOutput;

    @Autowired
    private AnnotatorJob annotatorJob;


    /**
     * Prepares the system for the annotation process by:
     *
     * - Updating the list of IDG Targets
     * - Loading and checking the dictionary for the current annotator
     * - Updating the Ensembl mapping, if necessary
     */
    public void prepare() {
        // Set the email address to where the logger should send warning and error messages
        System.setProperty("email.to", emailTo);
        if (!isValidEmailAddress(emailTo)) {
            logger.warn("Warning and error e-mails will not be sent because the configuration property ${email.to} is not a valid e-mail address: '{}'", emailTo);
        }

        // Load the list of IDG targets
        updateIdgTargets();

        // Before annotating, ensure the dictionary is ok
        checkDictionary();

        // Update the Ensembl mapping if necessary
        updateEnsemblMapping();
    }


    /**
     * Runs the annotation pipeline
     */
    public void annotate() {
        try {
            annotatorJob.run();
        } catch (Exception ex) {
            logger.error("The annotation process aborted due to an unexpected error. Please, restart the application to retry.", ex);
            System.exit(1);
        }

        sparkContext().close();
        System.clearProperty("spark.driver.port");}



    /**
     * Updates the list of IDG Targets
     */
    private void updateIdgTargets() {
        try {
            idgTargetLoader.load();
        } catch (IOException ex) {
            logger.error("An error occurred while loading the IDG targets into the database", ex);
            System.exit(1);
        }
    }


    /**
     * Checks if the dictionary is ok
     */
    private void checkDictionary() {
        try {
            if (!dictionaryAnalyzer.analyze(annotator)) {
                logger.error("Unable to proceed, as the annotator's dictionary differs from " +
                        "what's in the database. Aborting...");
                System.exit(1);
            }
        } catch (IOException ex) {
            logger.error("An error occurred while analyzing the dictionaries", ex);
            System.exit(1);
        }
    }


    /**
     * Updates the Ensembl Peptide id to UniProt accession mapping, if necessary
     *
     * This is only applicable to the Tagger annotator.
     */
    private void updateEnsemblMapping() {
        try {
            if (annotator.getName().equals("tagger")) {
                Dictionary dictionary = dictionaryRepository.get(annotator);
                Set<String> ensemblPeptides = dictionary.getEntities().get("HUMAN_GENE").keySet()
                        .stream()
                        .filter(name -> name.startsWith("ENSP"))
                        .collect(Collectors.toSet());

                if (ensemblService.needsUpdate()) {
                    ensemblService.updateMapping(ensemblPeptides);
                }
            }
        } catch (Exception ex) {
            logger.error("An error occurred while updating the Ensembl mapping", ex);
            System.exit(1);
        }
    }


    /**
     * Checks if the given e-mail address is valid
     */
    private boolean isValidEmailAddress(String email) {
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            return false;
        }

        return true;
    }


    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AnnotatorApp.class);
        app.setBannerMode(Banner.Mode.OFF);
        ApplicationContext ctx = app.run(args);

        AnnotatorApp annotatorApp = ctx.getBean(AnnotatorApp.class);
        annotatorApp.prepare();
        annotatorApp.annotate();
        logger.warn("The annotation process finished successfully. Terminating...");
    }
}
