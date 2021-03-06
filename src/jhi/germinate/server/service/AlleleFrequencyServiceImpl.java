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
import javax.servlet.http.*;

import jhi.germinate.client.service.*;
import jhi.germinate.server.database.query.*;
import jhi.germinate.server.util.*;
import jhi.germinate.server.util.FlapjackUtils.*;
import jhi.germinate.server.util.FlapjackUtils.FlapjackParams.*;
import jhi.germinate.shared.*;
import jhi.germinate.shared.datastructure.*;
import jhi.germinate.shared.datastructure.Tuple.*;
import jhi.germinate.shared.datastructure.database.*;
import jhi.germinate.shared.enums.*;
import jhi.germinate.shared.exception.*;
import jhi.germinate.shared.exception.IOException;

/**
 * {@link AlleleFrequencyServiceImpl} is the implementation of {@link AlleleFrequencyService}.
 *
 * @author Sebastian Raubach
 */
@WebServlet(urlPatterns = {"/germinate/allelefreq"})
public class AlleleFrequencyServiceImpl extends DataExportServlet implements AlleleFrequencyService
{
	private static final long serialVersionUID = 8117627823769568395L;

	@Override
	public Pair<String, HistogramImageData> getHistogramImageData(RequestProperties properties, HistogramParams params) throws InvalidSessionException,
			jhi.germinate.shared.exception.IOException, FlapjackException
	{
		Session.checkSession(properties, this);

		File mapFile = new File((String) getFromSession(SESSION_PARAM_MAP));
		File subsetForFlapjack = new File((String) getFromSession(SESSION_PARAM_ALLELE_DATA_FILE));
		File histogramFile = new File((String) getFromSession(SESSION_PARAM_HISTOGRAM));

		/* Update the last modified date => it won't get deleted by the automatic file delete thread */
		FileUtils.setLastModifyDateNow(mapFile);
		FileUtils.setLastModifyDateNow(histogramFile);
		FileUtils.setLastModifyDateNow(subsetForFlapjack);

		File imageDataFile = createTemporaryFile("histogram_" + params.method, params.datasetIds, FileType.txt.name());
		String output = FlapjackUtils.createImage(imageDataFile.getAbsolutePath(), histogramFile.getAbsolutePath(), params);

		return new Pair<>(output, readImageDataFile(imageDataFile));
	}

	/**
	 * Reads the widths and colors for the flapjack histogram image from the given {@link File}
	 *
	 * @param file The {@link File}
	 * @return The arrays of widths and colors
	 * @throws jhi.germinate.shared.exception.IOException Thrown if any file interaction fails
	 */
	private HistogramImageData readImageDataFile(File file) throws IOException
	{
		try (BufferedReader br = new BufferedReader(new FileReader(file)))
		{
			/* Read the two lines */
			String widths = br.readLine();
			String colors = br.readLine();

			if (!StringUtils.isEmpty(widths, colors))
			{
				/* Split them into parts */
				String[] widthArray = widths.split(",");
				String[] colorArray = colors.split(",");

				if (widthArray.length == colorArray.length && widthArray.length > 0)
				{
					double[] widthArrayDouble = new double[widthArray.length];

					for (int i = 0; i < widthArray.length; i++)
					{
						widthArrayDouble[i] = Double.parseDouble(widthArray[i]);
					}

					return new HistogramImageData(widthArrayDouble, colorArray);
				}
			}

			return null;
		}
		catch (java.io.IOException e)
		{
			throw new IOException(e);
		}
	}

	@Override
	public Pair<String, FlapjackProjectCreationResult> createProject(RequestProperties properties, HistogramParams params) throws InvalidSessionException, FlapjackException
	{
		HttpServletRequest req = getThreadLocalRequest();
		Session.checkSession(properties, req);

		File mapFile = new File((String) getFromSession(SESSION_PARAM_MAP));
		File subsetForFlapjack = new File((String) getFromSession(SESSION_PARAM_ALLELE_DATA_FILE));
		File histogramFile = new File((String) getFromSession(SESSION_PARAM_HISTOGRAM));
		File binnedFile = createTemporaryFile("allelefreq_binned", params.datasetIds, FileType.txt.name());

		/* Update the last modified date => it won't get deleted by the automatic file delete thread */
		if (mapFile.exists())
			FileUtils.setLastModifyDateNow(mapFile);

		String debugOutput = FlapjackUtils.createBinnedFile(params, subsetForFlapjack.getAbsolutePath(), binnedFile.getAbsolutePath(), histogramFile.getAbsolutePath());

		/* Now we call Flapjack to create the project file for us */
		File flapjackResultFile = createTemporaryFile("genotype", params.datasetIds, "flapjack");

		FlapjackParams flapjackParams = new FlapjackParams()
				.add(Param.map, mapFile)
				.add(Param.genotypes, binnedFile)
				.add(Param.project, flapjackResultFile);
		debugOutput += "     " + FlapjackUtils.createProject(flapjackParams);

		FlapjackProjectCreationResult fjExportResult = new FlapjackProjectCreationResult()
				.setMapFile(new CreatedFile(mapFile))
				.setRawDataFile(new CreatedFile(subsetForFlapjack))
				.setTabDelimitedFile(new CreatedFile(binnedFile))
				.setProjectFile(new CreatedFile(flapjackResultFile));

		return new Pair<>(debugOutput, fjExportResult);
	}

	@Override
	public ServerResult<FlapjackAllelefreqBinningResult> createHistogram(RequestProperties properties, List<Long> accessionGroups, Set<String> markedAccessionIds, List<Long> markerGroups, Set<String> markedMarkerIds, Long datasetId, boolean missingOn, Long mapId, int nrOfBins) throws InvalidSessionException, DatabaseException, InvalidArgumentException, IOException, FlapjackException
	{
		Session.checkSession(properties, this);
		UserAuth userAuth = UserAuth.getFromSession(this, properties);

		FlapjackAllelefreqBinningResult result = new FlapjackAllelefreqBinningResult();

		DebugInfo sqlDebug = DebugInfo.create(userAuth);
		DataExporter.DataExporterParameters settings = getDataExporterParameters(sqlDebug, userAuth, ExperimentType.allelefreq, accessionGroups, markedAccessionIds, markerGroups, markedMarkerIds, datasetId, mapId, false, missingOn);
		CommonServiceImpl.ExportResult exportResult = getExportResult(datasetId, ExperimentType.allelefreq, this);

		// Kick off the extraction process, because we need the exported data before we can start with the histogram
		try
		{
			AlleleFrequencyDataExporter exporter = new AlleleFrequencyDataExporter(settings);
			exporter.readInput();
			int size = exporter.exportResult(exportResult.subsetWithFlapjackLinks.getAbsolutePath(), "# fjFile = ALLELE_FREQUENCY\n" + exportResult.flapjackLinks);

			/* Now we call Flapjack to create histogram file for us */
			File histogramFile = createTemporaryFile("histogram", datasetId, FileType.txt.name());
			String debugOutput = FlapjackUtils.makeHistogram(exportResult.subsetWithFlapjackLinks.getAbsolutePath(), histogramFile.getAbsolutePath(), nrOfBins);

			storeInSession(SESSION_PARAM_HISTOGRAM, histogramFile.getAbsolutePath());
			storeInSession(SESSION_PARAM_ALLELE_DATA_FILE, exportResult.subsetWithFlapjackLinks.getAbsolutePath());

			/* Get the map */
			DefaultStreamer mapData = GenotypeServiceImpl.getMap(userAuth, sqlDebug, mapId);

			if (!mapData.hasData())
				throw new InvalidArgumentException("There is no data to export for the current selection.");

			try
			{
				Set<String> keptMarkers = exporter.getUsedColumnNames();
				Set<String> deletedMarkers = exporter.getDeletedMarkers();
				File filename = createTemporaryFile("map", datasetId, "map");
				FlapjackUtils.writeTemporaryMapFile(filename, mapData, keptMarkers, null);

				/* Remember the map file location in the session */
				storeInSession(SESSION_PARAM_MAP, filename.getAbsolutePath());
				storeInSession(SESSION_PARAM_DELETED_MARKERS, deletedMarkers);
			}
			catch (java.io.IOException e1)
			{
				throw new IOException(e1);
			}

			if (size < 1)
				throw new InvalidArgumentException("There is no data to export for the current selection.");

			result.setDebugOutput(debugOutput);
			result.setHistogramFile(histogramFile.getName());
		}
		catch (java.io.IOException e)
		{
			e.printStackTrace();
			throw new IOException(e);
		}

		return new ServerResult<>(sqlDebug, result);
	}
}
