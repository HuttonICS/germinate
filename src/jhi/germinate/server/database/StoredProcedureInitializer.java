/*
 *  Copyright 2017 Information and Computational Sciences,
 *  The James Hutton Institute.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jhi.germinate.server.database;


/**
 * {@link StoredProcedureInitializer} contains methods to drop and create stored procedures.
 *
 * @author Sebastian Raubach
 */
public class StoredProcedureInitializer extends DatabaseInitializer
{
	public static final String PHENOTYPE_DATA               = "phenotype_data";
	public static final String COMPOUND_DATA                = "compound_data";
	public static final String GERMINATEBASE_ATTRIBUTE_DATA = "germinatebase_attribute_data";
	public static final String DATASET_META                 = "dataset_meta";
	public static final String DATASET_ATTRIBUTES           = "dataset_attributes";
	public static final String UPDATE_AUTO_INCREMENT        = "update_auto_increment";

	private static final String DROP_PROCEDURE   = "DROP PROCEDURE IF EXISTS %s";
	private static final String CREATE_PROCEDURE = "CREATE PROCEDURE %s %s";

	private static final String QUERY_PHENOTYPE_DATA               = "(IN groupIds TEXT, IN datasetIds TEXT, IN phenotypeIds TEXT) BEGIN SET @SQL = NULL; SET @@group_concat_max_len = 64000000; SET @QRY = CONCAT('SELECT GROUP_CONCAT(DISTINCT CONCAT(\"MAX(IF(`phenotype_id` = \", `phenotype_id`,\",phenotype_value,NULL)) AS \", \"`\", CONCAT(phenotypes.`name`, IF(ISNULL(phenotypes.unit_id), \"\", CONCAT(\" [\", units.unit_abbreviation, \"]\"))), \"`\")) INTO @SQL FROM phenotypedata LEFT JOIN phenotypes ON phenotypes.id = phenotypedata.phenotype_id LEFT JOIN germinatebase ON germinatebase.id = phenotypedata.germinatebase_id LEFT JOIN units ON units.id = phenotypes.unit_id WHERE phenotypedata.dataset_id IN (', datasetIds, ') AND ', IF(phenotypeIds IS NULL, '1=1', CONCAT('phenotype_id IN (', phenotypeIds, ')'))); PREPARE stmtone FROM @QRY; EXECUTE stmtone; DEALLOCATE PREPARE stmtone; IF @SQL IS NULL THEN SELECT NULL as ERROR; ELSE SET @SQL = CONCAT('SELECT germinatebase.name, datasets.name AS dataset_name, datasets.description AS dataset_description, datasets.version AS dataset_version, licenses.name AS license_name, locations.site_name AS location_name, DATE_FORMAT(phenotypedata.recording_date, \\'%Y\\') AS year, germinatebase.id AS dbId, germinatebase.general_identifier, treatments.description AS treatments_description, ', @SQL,' FROM phenotypedata LEFT JOIN germinatebase ON germinatebase.id = phenotypedata.germinatebase_id LEFT JOIN groupmembers ON germinatebase.id = groupmembers.foreign_id LEFT JOIN datasets ON datasets.id = phenotypedata.dataset_id LEFT JOIN licenses ON licenses.id = datasets.license_id LEFT JOIN locations ON locations.id = datasets.location_id LEFT JOIN treatments ON treatments.id = phenotypedata.treatment_id WHERE ', IF(groupIds IS NULL, '1=1', CONCAT('groupmembers.group_id IN (', groupIds, ')')), ' AND datasets.id IN (', datasetIds, ') GROUP BY germinatebase.id, treatment_id, dataset_id, year'); PREPARE stmt FROM @SQL; EXECUTE stmt; DEALLOCATE PREPARE stmt; END IF; END";
	private static final String QUERY_COMPOUND_DATA                = "(IN groupIds TEXT, IN datasetIds TEXT, IN compoundIds TEXT)  BEGIN SET @SQL = NULL; SET @@group_concat_max_len = 64000000; SET @QRY = CONCAT('SELECT GROUP_CONCAT(DISTINCT CONCAT(\"MAX(IF(`compound_id`  = \", `compound_id`, \",compound_value, NULL)) AS \", \"`\", CONCAT(compounds.`name`,  IF(ISNULL(compounds.unit_id),  \"\", CONCAT(\" [\", units.unit_abbreviation, \"]\"))), \"`\")) INTO @SQL FROM compounddata  LEFT JOIN compounds  ON compounds.id  = compounddata.compound_id   LEFT JOIN germinatebase ON germinatebase.id = compounddata.germinatebase_id  LEFT JOIN units ON units.id = compounds.unit_id  WHERE compounddata.dataset_id  IN (', datasetIds, ') AND ', IF(compoundIds  IS NULL, '1=1', CONCAT('compound_id IN  (', compoundIds,  ')'))); PREPARE stmtone FROM @QRY; EXECUTE stmtone; DEALLOCATE PREPARE stmtone; IF @SQL IS NULL THEN SELECT NULL as ERROR; ELSE SET @SQL = CONCAT('SELECT germinatebase.name, datasets.name AS dataset_name, datasets.description AS dataset_description, datasets.version AS dataset_version, licenses.name AS license_name,                                                                                                    germinatebase.id AS dbId, germinatebase.general_identifier, ',                                                   @SQL,' FROM compounddata  LEFT JOIN germinatebase ON germinatebase.id = compounddata.germinatebase_id  LEFT JOIN groupmembers ON germinatebase.id = groupmembers.foreign_id LEFT JOIN datasets ON datasets.id = compounddata.dataset_id  LEFT JOIN licenses ON licenses.id = datasets.license_id                                                                                                                               WHERE ', IF(groupIds IS NULL, '1=1', CONCAT('groupmembers.group_id IN (', groupIds, ')')), ' AND datasets.id IN (', datasetIds, ') GROUP BY germinatebase.id,               dataset_id');       PREPARE stmt FROM @SQL; EXECUTE stmt; DEALLOCATE PREPARE stmt; END IF; END";
	private static final String QUERY_GERMINATEBASE_ATTRIBUTE_DATA = "(IN includeAtts TINYINT, IN groupId TEXT, IN accessionIds TEXT) BEGIN SET @SQL = NULL; SET @@group_concat_max_len = 32000; SET @QRY = CONCAT(' SELECT GROUP_CONCAT(DISTINCT CONCAT( \"MAX(CASE WHEN attribute_id = \", `attributes`.`id`, \" THEN attributedata.value END) AS \", \"`\", `attributes`.`name`, \"`\")) INTO @SQL FROM attributedata LEFT JOIN attributes ON attributes.id = attributedata.id WHERE attributes.target_table = \"germinatebase\";'); PREPARE stmtone FROM @QRY; EXECUTE stmtone; DEALLOCATE PREPARE stmtone; IF (@SQL IS NULL) OR NOT includeAtts THEN SET @SQL = CONCAT(' SELECT germinatebase.*, locations.id AS locations_id, locations.state AS locations_state, locations.region AS locations_region, locations.site_name AS locations_site_name, locations.elevation AS locations_elevation, locations.latitude AS locations_latitude, locations.longitude AS locations_longitude, countries.id AS countries_id, countries.country_code2 AS countries_country_code2, countries.country_code3 AS countries_country_code3, countries.country_name AS countries_country_name, taxonomies.id AS taxonomies_id, taxonomies.genus AS taxonomies_genus, taxonomies.species AS taxonomies_species, taxonomies.subtaxa AS taxonomies_subtaxa, taxonomies.species_author AS taxonomies_species_author, taxonomies.subtaxa_author AS taxonomies_subtaxa_author, taxonomies.cropname AS taxonomies_crop_name, taxonomies.ploidy AS taxonomies_ploidy, GROUP_CONCAT(DISTINCT synonyms.synonym SEPARATOR \", \") AS synonyms_synonym FROM germinatebase LEFT JOIN synonyms ON (synonyms.foreign_id = germinatebase.id AND synonyms.synonymtype_id = 1) LEFT JOIN locations ON germinatebase.location_id = locations.id LEFT JOIN countries ON countries.id = locations.country_id LEFT JOIN taxonomies ON taxonomies.id = germinatebase.taxonomy_id LEFT JOIN groupmembers ON groupmembers.foreign_id = germinatebase.id WHERE ', IF(groupId IS NULL, '1=1', CONCAT('groupmembers.group_id = \"', groupId, '\"')), ' AND ', IF(accessionIds IS NULL, '1=1', CONCAT('germinatebase.id IN (', accessionIds, ')')), ' GROUP BY germinatebase.id'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt; ELSE SET @SQL = CONCAT(' SELECT germinatebase.*, locations.id AS locations_id, locations.state AS locations_state, locations.region AS locations_region, locations.site_name AS locations_site_name, locations.elevation AS locations_elevation, locations.latitude AS locations_latitude, locations.longitude AS locations_longitude, countries.id AS countries_id, countries.country_code2 AS countries_country_code2, countries.country_code3 AS countries_country_code3, countries.country_name AS countries_country_name, taxonomies.id AS taxonomies_id, taxonomies.genus AS taxonomies_genus, taxonomies.species AS taxonomies_species, taxonomies.subtaxa AS taxonomies_subtaxa, taxonomies.species_author AS taxonomies_species_author, taxonomies.subtaxa_author AS taxonomies_subtaxa_author, taxonomies.cropname AS taxonomies_crop_name, taxonomies.ploidy AS taxonomies_ploidy, GROUP_CONCAT(DISTINCT synonyms.synonym SEPARATOR \", \") AS synonyms_synonym, ', @SQL, ' FROM germinatebase LEFT JOIN synonyms ON (synonyms.foreign_id = germinatebase.id AND synonyms.synonymtype_id = 1) LEFT JOIN locations ON germinatebase.location_id = locations.id LEFT JOIN countries ON countries.id = locations.country_id LEFT JOIN taxonomies ON taxonomies.id = germinatebase.taxonomy_id LEFT JOIN groupmembers ON groupmembers.foreign_id = germinatebase.id LEFT JOIN attributedata ON germinatebase.id = attributedata.foreign_id LEFT JOIN attributes ON attributes.id = attributedata.attribute_id WHERE attributes.target_table = \"germinatebase\" AND ', IF(groupId IS NULL, '1=1', CONCAT('groupmembers.group_id = \"', groupId, '\"')), ' AND ', IF(accessionIds IS NULL, '1=1', CONCAT('germinatebase.id IN (', accessionIds, ')')), ' GROUP BY germinatebase.id'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt; END IF; END";
	private static final String QUERY_DATASET_META                 = "() BEGIN DELETE FROM datasetmeta WHERE NOT EXISTS (SELECT 1 FROM datasets WHERE datasets.is_external = 1 AND datasets.id = datasetmeta.dataset_id); SELECT @max := IFNULL(MAX(id)+1, 1) FROM datasetmeta; SET @alter_statement = concat('ALTER TABLE `datasetmeta` AUTO_INCREMENT = ', @max); PREPARE stmt FROM @alter_statement; EXECUTE stmt; DEALLOCATE PREPARE stmt; INSERT INTO datasetmeta (dataset_id, nr_of_data_points, nr_of_data_objects) SELECT datasets.id, ( COALESCE(ac.c,0) + COALESCE (pc.c, 0) + COALESCE (cc.c, 0) + COALESCE(cmc.c, 0)) AS 'nr_of_data_points', ( COALESCE (ad.d, 0) + COALESCE (pd.d, 0) + COALESCE (cd.d, 0) + COALESCE(cmd.d, 0)) AS 'nr_of_data_objects' FROM datasets LEFT JOIN datasetstates ON datasetstates.id = datasets.dataset_state_id LEFT JOIN experiments ON experiments.id = datasets.experiment_id LEFT JOIN experimenttypes ON experimenttypes.id = experiments.experiment_type_id LEFT JOIN ( SELECT dataset_id, COUNT(1) AS d FROM datasetmembers WHERE datasetmembers.datasetmembertype_id = 2 GROUP BY dataset_id ) ad ON ad.dataset_id = datasets.id LEFT JOIN ( SELECT dataset_id, COUNT(1) AS c FROM phenotypedata GROUP BY dataset_id ) pc ON pc.dataset_id = datasets.id LEFT JOIN ( SELECT dataset_id, COUNT(1) AS c FROM climatedata GROUP BY dataset_id ) cc ON cc.dataset_id = datasets.id LEFT JOIN ( SELECT dataset_id, COUNT(1) AS c FROM compounddata GROUP BY dataset_id ) cmc ON cmc.dataset_id = datasets.id LEFT JOIN (SELECT datasetmembers.dataset_id, a.count * m.count AS c FROM datasetmembers LEFT JOIN ( SELECT dataset_id, COUNT(1) AS count FROM datasetmembers WHERE datasetmembertype_id = 1 GROUP BY dataset_id ) a ON a.dataset_id = datasetmembers.dataset_id LEFT JOIN ( SELECT dataset_id, COUNT(1) AS count FROM datasetmembers WHERE datasetmembertype_id = 2 GROUP BY dataset_id ) m ON m.dataset_id = datasetmembers.dataset_id GROUP BY datasetmembers.dataset_id) ac ON ac.dataset_id = datasets.id LEFT JOIN ( SELECT distinct_entries.dataset_id, count(1) AS d FROM ( SELECT DISTINCT dataset_id, germinatebase_id FROM phenotypedata ) AS distinct_entries GROUP BY distinct_entries.dataset_id ) pd ON pd.dataset_id = datasets.id LEFT JOIN ( SELECT distinct_entries.dataset_id, count(1) AS d FROM ( SELECT DISTINCT dataset_id, location_id FROM climatedata ) AS distinct_entries GROUP BY distinct_entries.dataset_id ) cd ON cd.dataset_id = datasets.id LEFT JOIN ( SELECT distinct_entries.dataset_id, count(1) AS d FROM ( SELECT DISTINCT dataset_id, germinatebase_id FROM compounddata ) AS distinct_entries GROUP BY distinct_entries.dataset_id ) cmd ON cmd.dataset_id = datasets.id WHERE is_external = 0; END";
	private static final String QUERY_DATASET_ATTRIBUTES           = "(IN `datasetIds` text,IN `attributeIds` text) BEGIN SET @SQL = NULL; SET @@group_concat_max_len = 64000000; SET @QRY = CONCAT('SELECT GROUP_CONCAT(DISTINCT CONCAT(\"GROUP_CONCAT(IF(`attribute_id` = \", `attribute_id`,\",value,NULL) SEPARATOR \\', \\') AS \", \"`\", attributes.`name`, \"`\")) INTO @SQL FROM attributedata LEFT JOIN attributes ON attributes.id = attributedata.attribute_id LEFT JOIN datasets ON datasets.id = attributedata.foreign_id WHERE attributes.target_table = \"datasets\" AND attributedata.foreign_id IN (', datasetIds, ') AND ', IF(attributeIds IS NULL, '1=1', CONCAT('attribute_id IN (', attributeIds, ')'))); PREPARE stmtone FROM @QRY; EXECUTE stmtone; DEALLOCATE PREPARE stmtone; IF @SQL IS NULL THEN SELECT NULL as ERROR; ELSE SET @SQL = CONCAT('SELECT datasets.name AS dataset_name, datasets.description AS dataset_description, datasets.version AS dataset_version, licenses.name AS license_name, ', @SQL,' FROM attributedata LEFT JOIN datasets ON datasets.id = attributedata.foreign_id LEFT JOIN attributes ON attributes.id = attributedata.attribute_id LEFT JOIN licenses ON licenses.id = datasets.license_id WHERE datasets.id IN (', datasetIds, ') GROUP BY datasets.id'); PREPARE stmt FROM @SQL; EXECUTE stmt; DEALLOCATE PREPARE stmt; END IF; END";
	private static final String QUERY_UPDATE_AUTO_INCREMENT        = "(IN tableName TEXT) BEGIN DECLARE theCount BIGINT; SET @SEL = CONCAT('SELECT COALESCE(MAX(id)+1, 1, MAX(id)+1) INTO @theCount FROM `', tableName, '`'); START TRANSACTION; PREPARE stmt FROM @SEL; EXECUTE stmt; DEALLOCATE PREPARE stmt; SET @INS = CONCAT('ALTER TABLE `', tableName, '` AUTO_INCREMENT = ', @theCount); PREPARE stmt FROM @INS; EXECUTE stmt; DEALLOCATE PREPARE stmt; COMMIT; END";

	private static final String[] PROCEDURES = {PHENOTYPE_DATA, COMPOUND_DATA, GERMINATEBASE_ATTRIBUTE_DATA, DATASET_META, DATASET_ATTRIBUTES, UPDATE_AUTO_INCREMENT};
	private static final String[] QUERIES    = {QUERY_PHENOTYPE_DATA, QUERY_COMPOUND_DATA, QUERY_GERMINATEBASE_ATTRIBUTE_DATA, QUERY_DATASET_META, QUERY_DATASET_ATTRIBUTES, QUERY_UPDATE_AUTO_INCREMENT};

	@Override
	protected String[] getNames()
	{
		return PROCEDURES;
	}

	@Override
	protected String[] getQueries()
	{
		return QUERIES;
	}

	@Override
	protected String getDropStatement()
	{
		return DROP_PROCEDURE;
	}

	@Override
	protected String getCreateStatement()
	{
		return CREATE_PROCEDURE;
	}
}
