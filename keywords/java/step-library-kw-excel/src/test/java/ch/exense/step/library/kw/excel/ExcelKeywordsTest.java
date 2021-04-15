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

import static org.junit.Assert.assertEquals;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;

import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class ExcelKeywordsTest {

	private ExecutionContext ctx = KeywordRunner.getExecutionContext(ExcelKeywords.class);

	@Test
	public void readCell() throws Exception {
		readCellAndAssert("B1", "1/1/16");
		readCellAndAssert("B2", "12:00:00");
		readCellAndAssert("B3", "100");
		readCellAndAssert("B4", "100.1");
		readCellAndAssert("B5", "TRUE");
		readCellAndAssert("B6", "String with\nnew line");
		readCellAndAssert("B7", "0.22");
		readCellAndAssert("B8", "0.016");
		readCellAndAssert("B9", "0.0167777778");
		readCellAndAssert("B10", "0.0167777778");
	}
	
	@Test
	public void readSheet() throws Exception {
		String path = getPathToExcel();
		String input = Json.createObjectBuilder().add("File", path).build().toString();
		Output<JsonObject> output = ctx.run("Read_Excel_Sheet", input);
		JsonObject payload = output.getPayload();
		assertEquals("2", payload.getString("columns"));
		assertEquals("10", payload.getString("rows"));
		assertEquals("1/1/16", payload.getString("B1"));
		assertEquals("12:00:00", payload.getString("B2"));
		assertEquals("100", payload.getString("B3"));
		assertEquals("100.1", payload.getString("B4"));
		assertEquals("TRUE", payload.getString("B5"));
		assertEquals("String with\nnew line", payload.getString("B6"));
		assertEquals("0.22", payload.getString("B7"));
		assertEquals("0.016", payload.getString("B8"));
		assertEquals("0.0167777778", payload.getString("B9"));
		assertEquals("0.0167777778", payload.getString("B10"));
	}

	private String getPathToExcel() {
		return ExcelKeywordsTest.class.getResource("Excel1.xlsx").getPath();
	}

	private void readCellAndAssert(String cellAddress, String expected) throws Exception {
		String path = getPathToExcel();
		String input = Json.createObjectBuilder().add("File", path).add("Cell", cellAddress).build().toString();
		Output<JsonObject> output = ctx.run("Read_Excel_Cell", input);
		assertEquals(expected, output.getPayload().getString("Value"));
	}
}
