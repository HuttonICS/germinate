/**
 * Germinate 3 is written and developed by Sebastian Raubach and Paul Shaw from the Information and Computational Sciences Group at JHI Dundee. For
 * further information contact us at germinate@hutton.ac.uk or visit our webpages at https://ics.hutton.ac.uk/germinate
 *
 * Copyright © 2005-2017, Information & Computational Sciences, The James Hutton Institute. All rights reserved. Use is subject to the accompanying
 * licence terms.
 */

package jhi.germinate.util.importer.reader;

import org.apache.poi.openxml4j.exceptions.*;

import java.io.*;
import java.text.*;
import java.util.*;

import jhi.germinate.shared.*;

/**
 * {@link IDataReader} extends {@link AutoCloseable} and defines the {@link #init(InputStream)} method that should always be called first.
 *
 * @author Sebastian Raubach
 */
public interface IDataReader extends AutoCloseable
{
	public static final SimpleDateFormat SDF_FULL       = new SimpleDateFormat("yyyyMMdd");
	public static final SimpleDateFormat SDF_YEAR_MONTH = new SimpleDateFormat("yyyyMM");
	public static final SimpleDateFormat SDF_YEAR_DAY   = new SimpleDateFormat("yyyydd");
	public static final SimpleDateFormat SDF_YEAR       = new SimpleDateFormat("yyyy");

	/**
	 * Passes the {@link File} to the {@link IDataReader} for initial preparations.
	 *
	 * @param is The {@link File} that contains the data.
	 * @throws IOException Thrown if the I/O fails.
	 */
	void init(File is) throws IOException, InvalidFormatException;

	public static Date getDate(String value)
	{
		Date date = null;
		if (!StringUtils.isEmpty(value))
		{
			// Replace all hyphens with zeros so that we only have one case to handle.
			value.replace("-", "0");

			try
			{
				boolean noMonth = value.substring(4, 6).equals("00");
				boolean noDay = value.substring(6, 8).equals("00");

				if (noDay && noMonth)
					date = SDF_YEAR.parse(value.substring(0, 4));
				else if (noDay)
					date = SDF_YEAR_MONTH.parse(value.substring(0, 6));
				else if (noMonth)
					date = SDF_YEAR_DAY.parse(value.substring(0, 4) + value.substring(6, 8));
				else
					date = SDF_FULL.parse(value);
			}
			catch (Exception e)
			{
			}
		}

		return date;
	}
}