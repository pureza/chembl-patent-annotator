DROP VIEW IF EXISTS idg_entity;
DROP TABLE IF EXISTS ensembl_peptide_to_uniprot;
DROP TABLE IF EXISTS idg_target;
DROP TABLE IF EXISTS bioentity_patent_annotation_count;
DROP TABLE IF EXISTS annotation;
DROP TABLE IF EXISTS field;
DROP TABLE IF EXISTS patent_annotated_by;
DROP TABLE IF EXISTS patent;
DROP TABLE IF EXISTS bio_entity;
DROP TABLE IF EXISTS bio_type;
DROP TABLE IF EXISTS annotator;

# An annotator, such as 'tagger' or 'neji'
CREATE TABLE annotator (
  annotator_id  INT(11)     NOT NULL AUTO_INCREMENT,
  name          VARCHAR(50) NOT NULL,
  PRIMARY KEY (annotator_id),
  UNIQUE KEY unique_name (name)
);


# The type of a Biological Entity ('gene', 'disease', etc)
# Each type is linked to an annotator, so we know which entities belong to
# annotators.
CREATE TABLE bio_type (
  bio_type_id     INT(11)     NOT NULL AUTO_INCREMENT,
  name            VARCHAR(45) NOT NULL,
  annotator_id    INT(11)     NOT NULL,
  PRIMARY KEY (bio_type_id),
  UNIQUE KEY unique_biotype (name, annotator_id),
  CONSTRAINT fk_annotator_annotator_id FOREIGN KEY (annotator_id) REFERENCES annotator (annotator_id) ON DELETE CASCADE
);


# A Biological Entity
CREATE TABLE bio_entity (
  bio_entity_id  INT(11)                      NOT NULL AUTO_INCREMENT,
  bio_type_id    INT(11)                      NOT NULL,
  name           VARCHAR(45) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (bio_entity_id),
  UNIQUE KEY unique_biothing (bio_type_id, name),
  CONSTRAINT fk_biothing_biotype FOREIGN KEY (bio_type_id) REFERENCES bio_type (bio_type_id) ON DELETE CASCADE
);


# The metadata of a patent
CREATE TABLE patent (
  patent_id           INT(11)     NOT NULL AUTO_INCREMENT,
  patent_number       VARCHAR(50) NOT NULL,
  publication_date    DATE        NOT NULL,
  PRIMARY KEY (patent_id),
  UNIQUE KEY unique_patent_number (patent_number)
);



# Relates patents to annotators, i.e., which patents have been annotated by
# which annotators
CREATE TABLE patent_annotated_by (
  patent_id     INT(11)   NOT NULL,
  annotator_id  INT(11)   NOT NULL,
  PRIMARY KEY (patent_id, annotator_id),
  CONSTRAINT fk_patent_patent_id2 FOREIGN KEY (patent_id) REFERENCES patent (patent_id) ON DELETE CASCADE,
  CONSTRAINT fk_annotator_annotator_id2 FOREIGN KEY (annotator_id) REFERENCES annotator (annotator_id) ON DELETE CASCADE
);


# A patent field ('title', 'abstract', etc)
CREATE TABLE field (
  field_id   INT(11)      NOT NULL AUTO_INCREMENT,
  name       VARCHAR(50)  NOT NULL,
  PRIMARY KEY (field_id),
  UNIQUE KEY unique_name (name(10))
);


# Represents an annotation
CREATE TABLE annotation (
  patent_id      INT(11)      NOT NULL,
  field_id       INT(11)      NOT NULL,
  rank           INT(11)      NOT NULL,
  bio_entity_id  INT(11)      NOT NULL,
  term           VARCHAR(127) NOT NULL,
  start_offset   INT(11)      NOT NULL,
  end_offset     INT(11)      NOT NULL,
  PRIMARY KEY (patent_id, field_id, rank, bio_entity_id, start_offset),
  CONSTRAINT fk_biothing_id_biothing FOREIGN KEY (bio_entity_id) REFERENCES bio_entity (bio_entity_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_patent_id_patent FOREIGN KEY (patent_id) REFERENCES patent (patent_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_field_id_field FOREIGN KEY (field_id) REFERENCES field (field_id) ON DELETE CASCADE ON UPDATE CASCADE
);


# Auxiliar table that counts annotations per biological entity, patent, patent
# field and annotated term
CREATE TABLE bioentity_patent_annotation_count (
  bio_entity_id  INT(11)      NOT NULL,
  patent_id      INT(11)      NOT NULL,
  field_id       INT(11)      NOT NULL,
  term           VARCHAR(127) NOT NULL,
  frequency      INT(11)      NOT NULL,
  PRIMARY KEY (bio_entity_id, patent_id, field_id, term),
  CONSTRAINT fk_biothing_id_biothing2 FOREIGN KEY (bio_entity_id) REFERENCES bio_entity (bio_entity_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_patent_id_patent2 FOREIGN KEY (patent_id) REFERENCES patent (patent_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_field_id_field2 FOREIGN KEY (field_id) REFERENCES field (field_id) ON DELETE CASCADE ON UPDATE CASCADE
);


# The IDG Targets, taken from the 'targets_idg_classes.csv' file
CREATE TABLE idg_target (
  id                INT(11)       NOT NULL AUTO_INCREMENT,
  uniprot_acc       VARCHAR(15)   NOT NULL,
  name              VARCHAR(127)  NOT NULL,
  development_level VARCHAR(15)   NOT NULL,
  target_family     VARCHAR(127)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY unique_uniprot_acc (uniprot_acc)
);


# Maps Ensembl Peptide idgss to UniProt accessions
CREATE TABLE ensembl_peptide_to_uniprot (
  ensembl_peptide_id  VARCHAR(15)   NOT NULL,
  uniprot_acc         VARCHAR(15)   NOT NULL,
  ensembl_release     VARCHAR(15)   NOT NULL,
  PRIMARY KEY (ensembl_peptide_id, uniprot_acc)
);


# Auxiliar view mapping IDG Targets to Biological Entities
CREATE VIEW idg_entity AS
  SELECT en.bio_entity_id, en.name AS entity_name, eu.uniprot_acc, idg.name AS target_name, idg.development_level, idg.target_family
    FROM idg_target idg, ensembl_peptide_to_uniprot eu, bio_entity en
   WHERE idg.uniprot_acc = eu.uniprot_acc
     AND eu.ensembl_peptide_id = en.name;


INSERT INTO field (field_id, name) VALUES ( 1, 'desc');
INSERT INTO field (field_id, name) VALUES ( 2, 'clms');
INSERT INTO field (field_id, name) VALUES ( 3, 'abst');
INSERT INTO field (field_id, name) VALUES ( 4, 'ttl');
INSERT INTO field (field_id, name) VALUES ( 5, 'image');
INSERT INTO field (field_id, name) VALUES ( 6, 'molattachment');
INSERT INTO field (field_id, name) VALUES ( 7, 'family_id');
INSERT INTO field (field_id, name) VALUES ( 8, 'drws');
INSERT INTO field (field_id, name) VALUES ( 9, 'ipcr');
INSERT INTO field (field_id, name) VALUES (10, 'published');
INSERT INTO field (field_id, name) VALUES (11, 'pubref');
INSERT INTO field (field_id, name) VALUES (12, 'rel_docs');
INSERT INTO field (field_id, name) VALUES (13, 'appref');
INSERT INTO field (field_id, name) VALUES (14, 'parties');
INSERT INTO field (field_id, name) VALUES (15, 'ucid');
INSERT INTO field (field_id, name) VALUES (16, 'citations');