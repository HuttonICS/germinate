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

package jhi.germinate.server.service;

import java.io.*;
import java.util.*;

import javax.servlet.annotation.*;

import jhi.germinate.client.service.*;
import jhi.germinate.server.database.query.*;
import jhi.germinate.server.util.*;
import jhi.germinate.server.util.FlapjackUtils.*;
import jhi.germinate.server.util.FlapjackUtils.FlapjackParams.*;
import jhi.germinate.shared.datastructure.*;
import jhi.germinate.shared.datastructure.database.*;
import jhi.germinate.shared.enums.*;
import jhi.germinate.shared.exception.*;
import jhi.germinate.shared.exception.IOException;

/**
 * {@link GenotypeServiceImpl} is the implementation of {@link GenotypeService}.
 *
 * @author Sebastian Raubach
 */
@WebServlet(urlPatterns = {"/germinate/genotype"})
public class GenotypeServiceImpl extends DataExportServlet implements GenotypeService
{
	private static final long serialVersionUID = -1828922709884114932L;

	//private static final String QUERY_EXPORT_MAP = "SELECT markers.marker_name, mapdefinitions.chromosome, mapdefinitions.definition_start FROM mapdefinitions, mapfeaturetypes, markers WHERE mapdefinitions.mapfeaturetype_id = mapfeaturetypes.id AND mapdefinitions.marker_id = markers.id AND map_id = ? AND marker_name IN (%s) ORDER BY chromosome, definition_start";
	private static final String QUERY_EXPORT_MAP = "SELECT markers.marker_name, mapdefinitions.chromosome, mapdefinitions.definition_start FROM mapdefinitions, mapfeaturetypes, markers WHERE mapdefinitions.mapfeaturetype_id = mapfeaturetypes.id AND mapdefinitions.marker_id = markers.id AND map_id = ? ORDER BY chromosome, definition_start";

	@Override
	public ServerResult<List<String>> computeExportDataset(RequestProperties properties, List<Long> accessionGroups, List<Long> markerGroups, Long datasetId,
																			boolean heterozygousFilter, boolean misingDataFilter, Long mapId) throws InvalidSessionException, DatabaseException, IOException, FlapjackException,
			MissingPropertyException, InvalidArgumentException
	{
		Session.checkSession(properties, this);
		UserAuth userAuth = UserAuth.getFromSession(this, properties);

		DebugInfo sqlDebug = DebugInfo.create(userAuth);
		DataExporter.DataExporterParameters settings = getDataExporterParameters(sqlDebug, userAuth, ExperimentType.genotype, accessionGroups, markerGroups, datasetId, mapId, heterozygousFilter, misingDataFilter);
		CommonServiceImpl.ExportResult result = getExportResult(ExperimentType.genotype, this);

		File mapFile;

        /* Kick off the extraction process */
		try
		{
			DataExporter exporter = new DataExporter(settings, result.subsetWithFlapjackLinks.getAbsolutePath());
			exporter.readInput();
			/* Export the data with the links */
			exporter.exportResult(result.flapjackLinks);

			/* Store the deleted markers */
			List<String> keptMarkers = exporter.getKeptMarkers();

            /* Get the map */
			GerminateTableStreamer mapData = getMap(userAuth, sqlDebug, mapId);

			/* Write the map file */
			File filename = createTemporaryFile("map", "map");
			mapFile = FlapjackUtils.writeTemporaryMapFile(filename, mapData, keptMarkers, null);
		}
		catch (java.io.IOException e)
		{
			throw new IOException(e);
		}

		List<String> list = new ArrayList<>();
		list.add(mapFile.getName());
		list.add(result.subsetWithFlapjackLinks.getName());

		/* Remember the files */
		getRequest().getSession().setAttribute(Session.GENOTYPE_MAP, mapFile.getName());
		getRequest().getSession().setAttribute(Session.GENOTYPE_DATA, result.subsetWithFlapjackLinks.getName());

		return new ServerResult<>(sqlDebug, list);
	}

	@Override
	public ServerResult<FlapjackProjectCreationResult> convertToFlapjack(RequestProperties properties, String map, String genotype) throws InvalidSessionException, FlapjackException
	{
		Session.checkSession(properties, this);

		File mapFile = getFile(FileLocation.temporary, map);
		File genotypeFile = getFile(FileLocation.temporary, genotype);

		/* Now we call Flapjack to create the project file for us */
		File flapjackResultFile = createTemporaryFile("genotype", "flapjack");
		String debugOutput;

		FlapjackParams params = new FlapjackParams()
				.add(Param.map, mapFile)
				.add(Param.genotypes, genotypeFile)
				.add(Param.project, flapjackResultFile);

		/* Get the debug output from flapjack */
		debugOutput = FlapjackUtils.createProject(params);

		Set<String> deletedMarkers = new HashSet<>(); // TODO: Once the HDF5 export supports the CDF (Crap Data Filter), we need to fill this...
		/* Create a list of newly created files */
		FlapjackProjectCreationResult fjExport = new FlapjackProjectCreationResult()
				.setDebugOutput(debugOutput)
				.setProjectFile(flapjackResultFile.getName())
				.setRawDataFile(genotypeFile.getName())
				.setMapFile(mapFile.getName())
				.setDeletedMarkers(deletedMarkers);

		return new ServerResult<>(fjExport);
	}

	@Override
	public ServerResult<String> convertHdf5ToFlapjack(RequestProperties properties, Long datasetId) throws InvalidSessionException, DatabaseException, jhi.germinate.shared.exception.IOException, FlapjackException, InvalidArgumentException
	{
		Session.checkSession(properties, this);
		UserAuth userAuth = UserAuth.getFromSession(this, properties);

		DebugInfo sqlDebug = DebugInfo.create(userAuth);
		DataExporter.DataExporterParameters settings = getDataExporterParameters(sqlDebug, userAuth, ExperimentType.genotype, null, null, datasetId, null, false, false);
		CommonServiceImpl.ExportResult result = getExportResult(ExperimentType.genotype, this);

        /* Kick off the extraction process */
		DataExporter exporter = new DataExporter(settings, result.subsetWithFlapjackLinks.getAbsolutePath());
		exporter.readInput();
		/* Export the data with the links */
		exporter.exportResult(result.flapjackLinks);

		return new ServerResult<>(sqlDebug, result.subsetWithFlapjackLinks.getName());
	}

	/**
	 * Retrieves the map used for genotype export
	 *
	 * @param sqlDebug The {@link DebugInfo} to use
	 * @param mapToUse The map id to use
	 * @return The map information (marker_name, chromosome, definition_start)
	 * @throws DatabaseException Thrown if the database interaction fails
	 */
	private GerminateTableStreamer getMap(UserAuth userAuth, DebugInfo sqlDebug, Long mapToUse) throws DatabaseException
	{
		GerminateTableStreamer streamer = new GerminateTableQuery(QUERY_EXPORT_MAP, userAuth, new String[]{Marker.MARKER_NAME, MapDefinition.CHROMOSOME, MapDefinition.DEFINITION_START})
				.setLong(mapToUse)
				.getStreamer();

		sqlDebug.addAll(streamer.getDebugInfo());

		return streamer;
	}
}
