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

package jhi.germinate.util.importer.mcpd;

import org.apache.poi.openxml4j.exceptions.*;

import java.io.*;
import java.util.*;

import jhi.germinate.shared.*;
import jhi.germinate.shared.datastructure.database.*;
import jhi.germinate.util.importer.reader.*;

/**
 * {@link TabDelimitedMcpdReader} is a simple implementation of {@link IStreamableReader} that can parse tab-delimited MCPD files. The column headers
 * have to match the MCPD 2.1 field names, e.g. PUID for the Persistent unique identifier.
 *
 * @author Sebastian Raubach
 */
public class TabDelimitedMcpdReader implements IStreamableReader<Accession>
{
	private String[]     parts;
	private List<String> headers;

	private BufferedReader br;

	private String currentLine = null;

	@Override
	public void close() throws IOException
	{
		if (br != null)
			br.close();

		br = null;
	}

	@Override
	public void init(File input) throws IOException, InvalidFormatException
	{
		br = new BufferedReader(new InputStreamReader(new FileInputStream(input)));

		do
		{
			currentLine = br.readLine();
		} while (currentLine.startsWith("#"));

		headers = new ArrayList<>(Arrays.asList(currentLine.split("\t", -1)));
	}

	@Override
	public boolean hasNext() throws IOException
	{
		boolean hasNext = (currentLine = br.readLine()) != null;

		if (!hasNext)
		{
			close();
		}

		return hasNext;
	}

	@Override
	public Accession next()
	{
		parts = currentLine.split("\t", -1);

		StringUtils.trim(parts);

		return parse();
	}

	protected Accession parse()
	{
		Taxonomy taxonomy = parseTaxonomy();
		Accession accession = new Accession()
				.setPuid(getPart(McpdField.PUID))
				.setNumber(getPart(McpdField.ACCENAME))
				.setCollNumb(getPart(McpdField.COLLNUMB))
				.setCollCode(getPart(McpdField.COLLCODE))
				.setCollMissId(getPart(McpdField.COLLMISSID))
				.setDonorNumber(getPart(McpdField.DONORNUMB))
				.setDonorName(getPart(McpdField.DONORNAME))
				.setDonorCode(getPart(McpdField.DONORCODE))
				.setGeneralIdentifier(getPart(McpdField.ACCENUMB))
				.setName(getPart(McpdField.ACCENUMB))
				.setAcqDate(getPart(McpdField.ACQDATE))
				.setCollDate(getDateFromField(McpdField.COLLDATE))
				.setCollName(getPart(McpdField.COLLNAME))
				.setBreedersCode(getPart(McpdField.BREDCODE))
				.setBreedersName(getPart(McpdField.BREDNAME))
				.setOtherNumb(getPart(McpdField.OTHERNUMB))
				.setDuplSite(getPart(McpdField.DUPLSITE))
				.setDuplInstName(getPart(McpdField.DUPLINSTNAME))
				.setInstitution(parseInstitution())
				.setTaxonomy(taxonomy)
				.setLocation(parseLocation())
				.setBiologicalStatus(new BiologicalStatus(getLong(McpdField.SAMPSTAT)))
				.setCollSrc(new CollectingSource(getLong(McpdField.COLLSRC)))
				.setEntityType(EntityType.ACCESSION)
				.setMlsStatus(new MlsStatus(getLong(McpdField.MLSSTAT)))
				.setCreatedOn(new Date())
				.setUpdatedOn(new Date());
		accession.setExtra(McpdField.REMARKS.name(), getPart(McpdField.REMARKS));
		accession.setExtra(McpdField.STORAGE.name(), getPart(McpdField.STORAGE));
		accession.setExtra(McpdField.ANCEST.name(), getPart(McpdField.ANCEST));

		return accession;
	}

	private Location parseLocation()
	{
		Location location = new Location()
				.setType(LocationType.collectingsites)
				.setName(getPart(McpdField.COLLSITE))
				.setLatitude(getLatitudeLongitude(getDouble(McpdField.DECLATITUDE), getPart(McpdField.LATITUDE)))
				.setLongitude(getLatitudeLongitude(getDouble(McpdField.DECLONGITUDE), getPart(McpdField.LONGITUDE)))
				.setCoordinateUncertainty(getInt(McpdField.COORDUNCERT))
				.setCoordinateDatum(getPart(McpdField.COORDDATUM))
				.setGeoreferencingMethod(getPart(McpdField.GEOREFMETH))
				.setElevation(getPart(McpdField.ELEVATION))
				.setCountry(parseCountry())
				.setCreatedOn(new Date())
				.setUpdatedOn(new Date());

		// In some cases, we only know the country an accession is from, not a specific location.
		// We then still want to be able to link an accession to a country, so make sure the location has a name.
		if (!StringUtils.isEmpty(location.getCountry().getCountryCode3()) && StringUtils.isEmpty(location.getName()))
			location.setName("UNKNOWN LOCATION");

		return location;
	}

	private Country parseCountry()
	{
		return new Country()
				.setCountryCode3(getPart(McpdField.ORIGCTY));
	}

	private Taxonomy parseTaxonomy()
	{
		String genus = getPart(McpdField.GENUS);
		if (genus == null)
			genus = "";

		return new Taxonomy()
				.setGenus(genus)
				.setSpecies(getPart(McpdField.SPECIES))
				.setSubtaxa(getPart(McpdField.SUBTAXA))
				.setTaxonomyAuthor(getPart(McpdField.SPAUTHOR))
				.setSubtaxaAuthor(getPart(McpdField.SUBTAUTHOR))
				.setCropName(getPart(McpdField.CROPNAME))
				.setCreatedOn(new Date())
				.setUpdatedOn(new Date());
	}

	private Institution parseInstitution()
	{
		return new Institution()
				.setCountry(new Country(-1L))
				.setCode(getPart(McpdField.INSTCODE))
				.setName(getPart(McpdField.INSTCODE))
				.setAddress(getPart(McpdField.COLLINSTADDRESS))
				.setCreatedOn(new Date())
				.setUpdatedOn(new Date());
	}

	/**
	 * Returns the MCPD field value. If this value is empty after calling {@link String#trim()} then <code>null</code> is returned.
	 *
	 * @param field The {@link McpdField}
	 * @return The trimmed field value or <code>null</code>.
	 */
	protected String getPart(McpdField field)
	{
		int i = headers.indexOf(field.name());

		if (i < 0 || i > parts.length - 1)
			return null;
		else
		{
			String result = parts[i];

			result = StringUtils.trim(result);

			return StringUtils.isEmpty(result) ? null : result;
		}
	}

	/**
	 * Tries to parse a data from the value of the given {@link McpdField}. Since Java can't represent missing months or days in a
	 * meaningful way, the first of each will be used in the case of a missing field.
	 *
	 * @param field The {@link McpdField}
	 * @return The parsed {@link Date} or null.
	 */
	protected Date getDateFromField(McpdField field)
	{
		String value = getPart(field);

		return IDataReader.getDate(value);
	}

	/**
	 * Tries to parse a {@link Double} from the given {@link McpdField}.
	 *
	 * @param field The {@link McpdField}
	 * @return The parsed {@link Double} or null.
	 */
	protected Double getDouble(McpdField field)
	{
		String value = getPart(field);

		if (!StringUtils.isEmpty(value))
		{
			try
			{
				return Double.parseDouble(value);
			}
			catch (NumberFormatException e)
			{
			}
		}

		return null;
	}

	/**
	 * Tries to parse an {@link Integer} from the given {@link McpdField}.
	 *
	 * @param field The {@link McpdField}
	 * @return The parsed {@link Integer} or null.
	 */
	protected Integer getInt(McpdField field)
	{
		String value = getPart(field);

		if (!StringUtils.isEmpty(value))
		{
			try
			{
				return Integer.parseInt(value);
			}
			catch (NumberFormatException e)
			{
			}
		}

		return null;
	}

	/**
	 * Tries to parse an {@link Integer} from the given {@link McpdField}.
	 *
	 * @param field The {@link McpdField}
	 * @return The parsed {@link Integer} or null.
	 */
	protected Long getLong(McpdField field)
	{
		String value = getPart(field);

		if (!StringUtils.isEmpty(value))
		{
			try
			{
				return Long.parseLong(value);
			}
			catch (NumberFormatException e)
			{
			}
		}

		return null;
	}

	/**
	 * Returns the lat/lng value based on the absence/presence of the DECLATITUTE/LATITUDE/DECLONGITUDE/LONGITUDE fields in the MCPD. The decimals
	 * will be prefered, but the DMS values will be converted as a fallback.
	 *
	 * @param decimal            The decimal lat/lng (can be null)
	 * @param degreeMinuteSecond The DMS lat/lng (can be null)
	 * @return The decimal lat/lng value
	 */
	private static Double getLatitudeLongitude(Double decimal, String degreeMinuteSecond)
	{
		if (decimal != null)
		{
			return decimal;
		}
		else if (!StringUtils.isEmpty(degreeMinuteSecond))
		{
			if (degreeMinuteSecond.length() == 7 || degreeMinuteSecond.length() == 8)
			{
				boolean lat = degreeMinuteSecond.length() == 7;

				Double value;

				int degree;
				int minute = 0;
				int second = 0;

				try
				{
					if (lat)
						degree = Integer.parseInt(degreeMinuteSecond.substring(0, 2));
					else
						degree = Integer.parseInt(degreeMinuteSecond.substring(0, 3));
				}
				catch (NumberFormatException e)
				{
					return null;
				}
				try
				{
					if (lat)
						minute = Integer.parseInt(degreeMinuteSecond.substring(2, 4));
					else
						minute = Integer.parseInt(degreeMinuteSecond.substring(3, 5));
				}
				catch (NumberFormatException e)
				{
				}
				try
				{
					if (lat)
						second = Integer.parseInt(degreeMinuteSecond.substring(4, 6));
					else
						second = Integer.parseInt(degreeMinuteSecond.substring(5, 7));
				}
				catch (NumberFormatException e)
				{
				}

				value = degree + minute / 60d + second / 3600d;

				if (degreeMinuteSecond.endsWith("S") || degreeMinuteSecond.endsWith("W"))
					value = -value;

				return value;
			}
		}

		return null;
	}
}
