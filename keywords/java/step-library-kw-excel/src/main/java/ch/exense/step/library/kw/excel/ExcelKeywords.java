/*******************************************************************************
 * Copyright 2021 exense GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package ch.exense.step.library.kw.excel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;

import ch.exense.step.library.commons.AbstractEnhancedKeyword;
import ch.exense.step.library.commons.BusinessException;
import step.handlers.javahandler.Keyword;

public class ExcelKeywords extends AbstractEnhancedKeyword {

	@Keyword(schema = "{\"properties\":{" + "\"File\":{\"type\":\"string\"}," + "\"Sheet\":{\"type\":\"string\"},"
			+ "\"Cell\":{\"type\":\"string\"}" + "},\"required\":[\"File\", \"Cell\"]}", properties = { "" })
	public void Read_Excel_Cell() throws FileNotFoundException, IOException {
		String fileName = input.getString("File");
		String sheetName = input.getString("Sheet", null);
		String cellAddress = input.getString("Cell");

		File workbookFile = getWorkbookFile(fileName);
		CellAddress parseCellAddress = parseCellAddress(cellAddress);

		try (Workbook workbook = WorkbookFactory.create(workbookFile, null, true)) {
			Sheet sheet = getSheet(sheetName, workbook);
			Row row = getRow(sheet, parseCellAddress.row);
			Cell cell = getCell(row, parseCellAddress.column);
			String value = getCellValueAsString(workbook, cell);
			output.add("Value", value);
		}
	}

	@Keyword(schema = "{\"properties\":{" + "\"File\":{\"type\":\"string\"}," + "\"Sheet\":{\"type\":\"string\"}"
			 + "},\"required\":[\"File\", \"Cell\"]}", properties = { "" })
	public void Read_Excel_Sheet() throws FileNotFoundException, IOException {
		String fileName = input.getString("File");
		String sheetName = input.getString("Sheet", null);

		File workbookFile = getWorkbookFile(fileName);

		int numberOfRows = 0, numberOfColumns = 0;
		try (Workbook workbook = WorkbookFactory.create(workbookFile)) {
			Sheet sheet = getSheet(sheetName, workbook);
			int firstRowNum = sheet.getFirstRowNum();
			int lastRowNum = sheet.getLastRowNum();
			numberOfRows = lastRowNum + 1;
			for (int rowNum = firstRowNum; rowNum <= lastRowNum; rowNum++) {
				Row row = sheet.getRow(rowNum);
				if(row != null) {
					short firstCellNum = row.getFirstCellNum();
					short lastCellNum = row.getLastCellNum();
					for (int cellNum = firstCellNum; cellNum < lastCellNum; cellNum++) {
						Cell cell = row.getCell(cellNum);
						if(cell != null) {
							String value = getCellValueAsString(workbook, cell);
							String colString = CellReference.convertNumToColString(cellNum);
							String cellAddress = colString + (rowNum + 1);
							output.add(cellAddress, value);
						}
					}
					if(lastCellNum > numberOfColumns) {
						numberOfColumns = lastCellNum;
					}
				}
			}
		}
		output.add("columns", Integer.toString(numberOfColumns));
		output.add("rows", Integer.toString(numberOfRows));
	}

	private File getWorkbookFile(String fileName) {
		File workbookFile = new File(fileName);
		if (!workbookFile.exists() || !workbookFile.canRead()) {
			throw new BusinessException("The file " + workbookFile + " doesn't exist or cannot be read");
		}
		return workbookFile;
	}

	private Sheet getSheet(String sheetName, Workbook workbook) {
		Sheet sheet;
		if (sheetName != null) {
			sheet = workbook.getSheet(sheetName);
		} else {
			sheet = workbook.getSheetAt(0);
		}
		return sheet;
	}

	private String getCellValueAsString(Workbook workbook, Cell cell) {
		FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
		DataFormatter dataFormatter = new DataFormatter();
		String value = dataFormatter.formatCellValue(cell, evaluator);
		return value;
	}

	private Cell getCell(Row row, int columnId) {
		Cell cell = row.getCell(columnId);
		if (cell == null) {
			throw new BusinessException("The column " + columnId + " doesn't exist");
		}
		return cell;
	}

	private Row getRow(Sheet sheet, int rowId) {
		Row row = sheet.getRow(rowId);
		if (row == null) {
			throw new BusinessException("The row " + rowId + 1 + " doesn't exist");
		}
		return row;
	}

	private static final Pattern pattern = Pattern.compile("([A-Z]+)([0-9]+)");

	private CellAddress parseCellAddress(String cellAddress) {
		Matcher matcher = pattern.matcher(cellAddress);
		if (matcher.matches()) {
			String column = matcher.group(1);
			String line = matcher.group(2);

			int columnId = CellReference.convertColStringToIndex(column);
			int rowId = Integer.parseInt(line) - 1;

			return new CellAddress(columnId, rowId);
		} else {
			throw new IllegalArgumentException("Invalid cell descriptor '" + cellAddress + "'");
		}
	}

	private static class CellAddress {

		int column;
		int row;

		public CellAddress(int column, int line) {
			super();
			this.column = column;
			this.row = line;
		}
	}
}
