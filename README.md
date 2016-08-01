# Patent annotator for ChEMBL

## Introduction
This document describes the **patent annotator**, the software developed to annotate new patents periodically and collect the list of patents with occurrences of relevant IDG targets.


## How it works
The patent annotator was designed to be as simple as possible to use. When you run it for the first time, it notices the database is empty and annotates all patents from scratch. If, on the other hand, the database already contains some annotations, it does a normal incremental update. Finally, if a run terminates with an error, the annotator will automatically restart from the same position the next time you execute it. This happens automatically, without the need to specify any special configuration or command line arguments.

### Step 0: Preparation

#### IDG Targets loading

On startup, the list of IDG Targets is loaded into the database. This is always done, even if the targets have already been persisted.
The file containing the IDG Targets is embedded in the source code (at `src/main/resources/targets_idg_classes.csv`), but this can be changed in the configuration.

#### Dictionary loading

Before the annotation process itself starts, the software checks if the dictionary for the current annotator has already been loaded into the database. If it is not there, the dictionary is persisted at this point. Each biological entity in the database contains a reference to the dictionary it originated from, so it's possible for the same biological entity to exist multiple times in the database, one for each dictionary.

Next, the version of the dictionary that exists in the database is compared with the dictionary that will be used by the annotator (e.g., the `entities.tsv`, `global.tsv` and `names.tsv` files in the case of **Tagger**). If there are any inconsistencies, the application will fail at this point. This is because the annotator and the database must always be in sync, otherwise the annotator might find things that don't exist in the database and vice-versa.
If you update the dictionaries, you will have to re-annotate everything from scratch!

#### Updating the Ensembl mapping

The patent annotator keeps a mapping between Ensembl Peptide ids to UniProt accessions in the database. This mapping is required because the Tagger dictionary uses Ensembl Peptide ids, while the list of IDG Targets contains UniProt accessions. On startup, the software updates this mapping, if necessary.

The mapping is obtained via the following query:
```sql
SELECT tl.stable_id as ensembl_peptide_id, xr.dbprimary_acc as uniprot_acc
  FROM translation tl, xref xr, object_xref ox, external_db ex
 WHERE tl.translation_id = ox.ensembl_id
   AND xr.xref_id = ox.xref_id 
   AND ex.external_db_id = xr.external_db_id 
   AND ex.db_name IN ('Uniprot/SWISSPROT')
   AND xr.info_type = 'DIRECT'
   AND tl.stable_id IN (<ensemblPeptideIds>)
```
   
The software first tries to map all the Ensembl Peptide ids using the current Ensembl release. If there are unmapped peptides at this point, it checks the previous release, and so on, until there are no more releases to check or all peptides have been mapped. The list of Ensembl releases to use can be configured via the `ensembl.releases` property. If you add a new release to this property, the patent annotator will automatically notice the change and update the mapping.

### Step 1: Search for new patents

After checking the dictionary status, the software will start looking for new patents. In order to do this, it will first retrieve the date of the most recent patent in the database and then fetch newer patents from the SureChEMBL database. At this point the patent itself is not retrieved, just its basic information, such as the patent number and the publication date, which is then stored in an internal database, so that it doesn't need to be retrieved again.

If this is the first time the patent annotator is run and there are no existing patents in the database, it will retrieve the information for all patents from SurePro.


> The patent annotator loads the initial set of patents from SurePro and afterwards checks for new ones in SureChEMBL.

Finally, the software will figure out which patents will be annotated. Please note that this will not only include the new patents, but also patents for which the annotation process failed before, or patents which were never run through the specified annotator. So, if you first do a full run with Tagger and then switch to Becas, all patents will be annotated again.

### Step 2: Download patent XML
The patent annotator will then download and store the XML for any patents for which the XML is not in the file system. The XML is stored locally so that it won't have to be downloaded again in case of an error or if the patent needs to be re-annotated again with a different annotator or a new dictionary. The XML is retrieved from the Alexandria database, 1,000 patents at a time, and stored in a xml.gz file.

Once again, this includes not only the XML for the new patents, but also the XML for those patents for which the .xml.gz file may have been deleted or for which the download failed on a previous run. 

> There are patents for which the XML doesn't exist (for example, US-8129291-B2). The software will always try to download them anyway.

### Step 3: Annotate

Next, the application starts to annotate the patents. First, it reads the corresponding `xml.gz` file from the file system, then it breaks the XML into the relevant fields (currently: title, abstract, claims, description and non patent citations) and finally it calls the annotator itself, collecting the annotations on the go.

This step is done in parallel.

### Step 4: Persist the annotations

After obtaining the annotations, the software writes them to the database. It does this in chunks of 92 patents at a time, for increased performance.

It will also mark these patents as annotated by the current annotator, so next time it knows that it shouldn't re-annotate them.

### Step 5: Producing the output for IDG

Finally, the patent annotator writes the annotations for IDG targets into a tab-separated file. The file looks like:

```
"Entity"        "UniProt"       "Target name"   "Development level"     "Target family" "Patent"        "Published"     "Total hits"    "Description hits"      "Claims hits"   "Abstract hits" "Title hits"    "Terms"
"ENSP00000000442"       "P11474"        "Steroid hormone receptor ERR1" "Tchem" "Estrogen-related receptors"    "EP-0923645-A1" "1999-06-23"    "2"     "2"     "0"     "0"     "0"     "E-R-R-A:1,E-R R-A:1"
"ENSP00000000442"       "P11474"        "Steroid hormone receptor ERR1" "Tchem" "Estrogen-related receptors"    "EP-0930045-A2" "1999-07-21"    "4"     "4"     "0"     "0"     "0"     "err_a:4"
"ENSP00000000442"       "P11474"        "Steroid hormone receptor ERR1" "Tchem" "Estrogen-related receptors"    "EP-0935000-A2" "1999-08-11"    "1"     "1"     "0"     "0"     "0"     "ERR1:1"
"ENSP00000000442"       "P11474"        "Steroid hormone receptor ERR1" "Tchem" "Estrogen-related receptors"    "EP-0939123-A2" "1999-09-01"    "2"     "0"     "0"     "0"     "0"     "Estrogen Receptor-Like 1:1,ESRL1:1"
"ENSP00000003084"       "P13569"        "Cystic fibrosis transmembrane conductance regulator"   "Tclin" "CFTR transporter subfamily"    "EP-1121419-A1" "2001-08-08"    "1"     "1"     "0"     "0"     "0"     "CFTR:1"
"ENSP00000003084"       "P13569"        "Cystic fibrosis transmembrane conductance regulator"   "Tclin" "CFTR transporter subfamily"    "EP-1123385-A2" "2001-08-16"    "69"    "61"    "3"     "5"     "0"     "CFTR:65,cystic fibrosis transmembrane conductance regulator:4"
"ENSP00000003084"       "P13569"        "Cystic fibrosis transmembrane conductance regulator"   "Tclin" "CFTR transporter subfamily"    "EP-1123405-A1" "2001-08-16"    "5"     "5"     "0"     "0"     "0"     "CFTR:4,cystic fibrosis transmembrane conductance regulator:1"
"ENSP00000005284"       "O60359"        "Voltage-dependent calcium channel gamma-3 subunit"     "Tbio"  "Calcium channel auxiliary subunit 1-8 (cca) family"    "US-20130344168-A1"     "2013-12-26"    "1"     "1"     "0"     "0"     "0"     "CACNG3:1"
"ENSP00000005284"       "O60359"        "Voltage-dependent calcium channel gamma-3 subunit"     "Tbio"  "Calcium channel auxiliary subunit 1-8 (cca) family"    "US-20140045915-A1"     "2014-02-13"    "1"     "1"     "0"     "0"     "0"     "CACNG3:1"
"ENSP00000005284"       "O60359"        "Voltage-dependent calcium channel gamma-3 subunit"     "Tbio"  "Calcium channel auxiliary subunit 1-8 (cca) family"    "US-20140073568-A1"     "2014-03-13"    "6"     "6"     "0"     "0"     "0"     "Hs.7235:2,CACNG3:2,AK095553:2"
...
```

The columns are:

   * **Entity**: The name of the annotated biological entity as found by the annotator
   * **UniProt**: The UniProt accession of the IDG target (taken from the IDG list)
   * **Target name** (taken from the IDG list)
   * **Development level** (taken from the IDG list)
   * **Target family** (taken from the IDG list)
   * **Patent**: the patent where the annotation was found
   * **Published**: The date of publication of the patent
   * **Total hits**: Total number of times this entity was found in the document
   * **Description hits**: Number of times this entity was found in the description section
   * **Claims hits**: Number of times this entity was found in the claims section
   * **Abstract hits**: Number of times this entity was found in the abstract section
   * **Title hits**: Number of times this entity was found in the title section
   * **Terms**: The actual terms that were found in the document and their frequency
  
This step can be skipped by passing `--skip.output=true` when running the annotator. This may be useful because retrieving the list of IDG annotations alone can take more time than all the other steps together.

> Currently, only annotations generated by Tagger are written to this file. Annotations from other annotators are ignored.

---

One thing to keep in mind is that steps 2-4 are done concurrently, so, for example, the annotator starts as soon as there is a patent XML available, instead of starting only after all patents have been downloaded.

### Error handling
As mentioned in the description for step 4, the annotator records which annotators have been applied to which patents, so that it knows which patents haven't been annotated just by querying the database. Thus, when an error occurs, it should be enough to restart the application to make it resume the whole process from where it left before.

The patent annotator tries to recover from some expected errors (such as patents for which there is no XML), but most errors will cause it to fail abruptly. For example, if the database fails or the disk becomes full, it will fail immediately instead of moving on to the next patent, which could cause the same error to occur millions of times.

## Installation

### Software requirements

The patent annotator requires reasonably up-to-date versions of:

* Git
* Java 8
* Apache Maven
* MySQL

### Building the jar

```bash
$ mvn package
```

If all goes well, the executable jar should be found at `target/patent-annotator-1.0.jar`.

### Database
The patent annotator stores the annotations in a MySQL database. The DDL to create the tables is in the git repository, at `src/main/resources/ddl.mysql.sql`. In order to prepare the database, one just needs to run the SQL statements in this file. It will also be necessary to specify the database connection details in the configuration file, as described next.

## Configuration

The following is the default configuration of the patent annotator:

```
# The annotation database
patentannot.url =
patentannot.user =
patentannot.password =

# SurePro database: used to retrieve the metadata of ALL patents
surepro.url =
surepro.user =
surepro.password =

# SureChembl database: used to retrieve the metadata of NEW patents only
surechem.url =
surechem.user =
surechem.password =

# Alexandria database: used to download the patents XML
alexandria.url =
alexandria.user =
alexandria.password =

# Path where the patent's XML is stored
patents.xml.home =

# The annotator to use
annotator = tagger

# Number of threads that will be used to annotate the patents
annotator.threads = 4

# Path to the IDG targets file
# Use file:// to specify a path in the file system
idg-targets.path = classpath:targets_idg_classes.csv

# Path to the resulting file with the IDG annotations
idg-output.path = idg-output.tsv

# E-mail address to where WARN and ERROR messages are sent (set to blank to disable)
email.to =


## Tagger specific properties

# Path to the Tagger dictionaries
tagger.dictionaries.home =

# Ensembl database: to convert Ensembl Peptide ids to Uniprot accessions
ensembl.url = jdbc:mysql://ensembldb.ensembl.org/homo_sapiens_core_84_38
ensembl.user = anonymous
ensembl.password =

# List of ensembl releases to use when mapping Ensembl Peptide ids to UniProt accessions
# These are actually the suffixes of human_sapiens_core schemas in the database (i.e., homo_sapiens_core_84_38)
# They are tried in order, until all Ensembl Peptide ids are mapped or there are no more releases to check
ensembl.releases = 84_38,83_38,82_38,81_38,80_38,79_38,78_38,77_38,76_38,75_37,74_37,73_37,72_37,71_37,70_37,69_37,68_37,67_37,66_37,65_37
```

In order to override the default configuration, one just needs to put it in a file called `application.properties` file in the same folder as the jar.

## Running the patent annotator
To execute the patent annotator, one just needs to execute the jar:

```bash
$ java -Djava.library.path=./tagger-java/libtagger -Xmx32g -jar patent-annotator-1.0.jar
```
Furthermore, the application accepts two optional command line arguments:

* `--skip.output`, described in Step 5
* `--clear-db` option, explained below 

## Clearing the database
If you wish to discard all the annotations collected before and start from scratch, you can do so by passing the `--clear-db=true` command line argument:

```bash
$ java -Djava.library.path=./tagger-java/libtagger -Xmx32g -jar patent-annotator-1.0-SNAPSHOT.jar --clear-db=true
```

```
main                 AnnotatorApp                             *** GOING TO CLEAR THE DATABASE IN 10 SECONDS ***
main                 AnnotatorApp                             *** GOING TO CLEAR THE DATABASE IN 9 SECONDS ***
main                 AnnotatorApp                             *** GOING TO CLEAR THE DATABASE IN 8 SECONDS ***
main                 AnnotatorApp                             *** GOING TO CLEAR THE DATABASE IN 7 SECONDS ***
main                 AnnotatorApp                             *** GOING TO CLEAR THE DATABASE IN 6 SECONDS ***
main                 AnnotatorApp                             *** GOING TO CLEAR THE DATABASE IN 5 SECONDS ***
main                 AnnotatorApp                             *** GOING TO CLEAR THE DATABASE IN 4 SECONDS ***
main                 AnnotatorApp                             *** GOING TO CLEAR THE DATABASE IN 3 SECONDS ***
main                 AnnotatorApp                             *** GOING TO CLEAR THE DATABASE IN 2 SECONDS ***
main                 AnnotatorApp                             *** GOING TO CLEAR THE DATABASE IN 1 SECOND ***
main                 AnnotatorApp                             Clearing the database...
main                 AnnotatorApp                             Database cleared!
main                 AnnotatorRepository                      Registering new annotator 'tagger'...
... normal output follows
```

As you can see, the application waits for 10 seconds before clearing the database. This gives some time to the user in case he or she wants to abort this procedure.
Clearing the database actually `DROP`s all tables and `CREATE`s them again.

## Troubleshooting

The software emits three kinds of logging:

* It writes to the standard output, to inform the user on the progress of the annotation task
* It writes more detailed messages to the logs/patent-annotator.log file. These messages can be helpful to diagnose problems
* Finally, it sends warning and error messages via e-mail, to an address specified in the configuration file

### Full run log

This is an annotated example of the logging emitted when the software runs for the first time:

```
# First time this annotator is being run, so it is added to the database
INFO  main                 AnnotatorRepository                      Registering new annotator 'tagger'...
 
# The IDG targets are loaded into the database for the first time
INFO  main                 IdgTargetLoader                          Loaded 1,795 IDG targets into the database (previously there were 0)
 
# Load the dictionary from the database. Since the annotator is new, the dictionary is empty and needs to be read first
INFO  main                 DictionaryRepository                     Loaded the dictionary for tagger with 0 entities
WARN  main                 DictionaryAnalyzer                       It looks like it's the first time you run the tagger annotator. Persisting its dictionary to the database...
INFO  main                 DictionaryRepository                     Stored the dictionary for tagger with 82,653 entities belonging to 2 types
 
# Map the Ensembl Peptide ids in the Tagger dictionary to UniProt accessions
INFO  main                 EnsemblService                           The Ensembl Peptide to UniProt mapping will now be updated for the latest Ensembl release: 84_38...
INFO  main                 EnsemblService                           Mapped 19,504 Ensembl Peptide ids to UniProt accessions. 2,464 ids were not mapped!
 
# Looking for new patents. Since the database is empty, it will retrieve all patents from SurePro
INFO  pipeline-thread-0    PatentMetadataLoader                     Looking for new patents in jdbc:mysql://54.77.227.201/patent_anno_20120302, to copy them to jdbc:mysql://sark.ebi.ac.uk:3306/patent_annot?rewriteBatchedStatements=true&useSSL=false...
WARN  pipeline-thread-0    PatentMetadataLoader                     The annotation database is empty!
INFO  pipeline-thread-0    PatentMetadataLoader                     Loading the metadata for ALL patents. This will take a few minutes...
INFO  pipeline-thread-0    PatentMetadataLoader                     Finished copying the metadata of 3,725,720 patents!
WARN  pipeline-thread-0    PatentMetadataLoadStep                   Going to annotate 3,725,720 patents...
 
# Next, it starts downloading the XML for those patents not in the file system
INFO  pipeline-thread-1    PatentXmlDownloadStep                    Downloading 341 patents to /nfs/panda/chembl/SureChEMBL/pureza/patents/xml...
INFO  pipeline-thread-1    PatentXmlDownloadStep                    Downloaded 0 patents
INFO  pipeline-thread-1    PatentXmlDownloadStep                    341 patents have no XML. I will try to download these again next time.
 
# At the same time, the software starts annotating the patents that are in the file system (and the new ones, as they are downloaded)
INFO  pipeline-thread-3    DictionaryRepository                     Loaded the dictionary for tagger with 82,653 entities
INFO  annotator-thread-15  PatentAnnotationStep                     Annotated patent #10,000
INFO  annotator-thread-10  PatentAnnotationStep                     Annotated patent #20,000
...
 
# After a while, it terminates
INFO  pipeline-thread-3    AnnotationPersistenceStep                Done! 3,725,379 entries processed.
INFO  main                 IdgAnnotationWriter                      Retrieving annotations for IDG targets... this will take a few minutes
INFO  main                 IdgAnnotationWriter                      IDG annotations written to idg-output.tsv
WARN  main                 AnnotatorApp                             The annotation process finished successfully. Terminating...
Incremental run log
```

This is an example of the log emitted during an incremental run:

```
# Load and verify the dictionary
INFO  main                 DictionaryRepository                     Loaded the dictionary for tagger with 82,653 entities
INFO  main                 DictionaryAnalyzer                       Checking the dictionary...
 
# Looking for new patents. The difference between the number of new patents and the number of patents to annotate (341) corresponds to the patents for which it was unable to download the XML last time
INFO  pipeline-thread-0    PatentMetadataLoader                     Looking for new patents in jdbc:mysql://54.77.227.201/patent_anno_20120302, to copy them to jdbc:mysql://sark.ebi.ac.uk:3306/patent_annot?rewriteBatchedStatements=true&useSSL=false...
INFO  pipeline-thread-0    PatentMetadataLoader                     The most recent patent in the annotation database was published on 2016-04-07
INFO  pipeline-thread-0    PatentMetadataLoader                     Finished copying the metadata of 57,819 patents!
WARN  pipeline-thread-0    PatentMetadataLoadStep                   Going to annotate 58,160 patents...
 
# Download and annotate the missing patents
INFO  pipeline-thread-1    PatentXmlDownloadStep                    Downloading 49,689 patents to /nfs/panda/chembl/SureChEMBL/pureza/patents/xml...
INFO  pipeline-thread-1    PatentXmlDownloadStep                    Downloaded patent #100 (0%)
INFO  pipeline-thread-1    PatentXmlDownloadStep                    Downloaded patent #200 (0%)
...
INFO  pipeline-thread-1    PatentXmlDownloadStep                    Downloaded 49,312 patents
INFO  pipeline-thread-1    PatentXmlDownloadStep                    377 patents have no XML. I will try to download these again next time.
INFO  pipeline-thread-3    AnnotationPersistenceStep                Done! 57,783 entries processed.
INFO  main                 IdgAnnotationWriter                      Retrieving annotations for IDG targets... this will take a few minutes
INFO  main                 IdgAnnotationWriter                      IDG annotations written to idg-output.tsv
WARN  main                 AnnotatorApp                             The annotation process finished successfully. Terminating...
```

## Supporting new annotators

Supporting a new annotator should be straightforward. One just needs to implement the following interfaces:

* `uk.ac.ebi.chembl.annotator.Annotator`: encapsulates the annotator. The most important method, `annotate()`, receives a piece of text and returns a list of annotations.
* `uk.ac.ebi.chembl.annotator.DictionaryReader`: Reads the annotator's dictionary.
* `uk.ac.ebi.chembl.annotator.AnnotatorFactory`: Instantiates the `Annotator` and the `DictionaryReader`. There can be multiple annotator factories (one for each annotator), but only the one corresponding to the annotator selected in the configuration will be executed. The factory needs to be annotated with `@Component`, so that it is discoverable via the Spring Context.

### Tagger specific information
In order to annotate patents with **Tagger**, the user will need to specify the path to the Tagger dictionaries in the configuration property `tagger.dictionaries.home`.
Furthermore, since Tagger is a library developed in C++, it also necessary to add the command line argument `-Djava.library.path=/path/to/libtagger` when running the jar.

