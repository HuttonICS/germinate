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

package jhi.germinate.util.importer.genotype;

import org.apache.poi.ss.usermodel.*;

import jhi.germinate.util.importer.reader.*;

/**
 * {@link ExcelGenotypeDataReader} implements {@link IStreamableReader} and reads and streams one {@link String}[] object at a time (one row of
 * the data matrix).
 *
 * @author Sebastian Raubach
 */
public class ExcelGenotypeDataReader extends ExcelStreamableReader<String[]>
{
	private Sheet dataSheet;
	private Row   row;

	private int rowCount   = 0;
	private int colCount   = 0;
	private int currentRow = 1;

	@Override
	public boolean hasNext()
	{
		return ++currentRow < rowCount;
	}

	@Override
	public String[] next()
	{
		row = dataSheet.getRow(currentRow);
		return parse();
	}

	@Override
	public void init(Workbook wb)
	{
		dataSheet = wb.getSheet("DATA");

		rowCount = dataSheet.getPhysicalNumberOfRows();
		colCount = dataSheet.getRow(2).getPhysicalNumberOfCells();
	}

	private String[] parse()
	{
		String[] result = new String[colCount];

		for (int i = 0; i < colCount; i++)
			result[i] = utils.getCellValue(row, i);

		return result;
	}
}
