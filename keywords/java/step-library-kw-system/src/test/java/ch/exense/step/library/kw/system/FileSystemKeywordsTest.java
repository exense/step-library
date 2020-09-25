/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package ch.exense.step.library.kw.system;

import java.io.File;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class FileSystemKeywordsTest {

	private ExecutionContext ctx;

	@Before
	public void setUp() {
		ctx = KeywordRunner.getExecutionContext(FileSystemKeywords.class);
	}

	@After
	public void tearDown() {
		ctx.close();
	}

	@Test
	public void test_sed() throws Exception {
		String path = new File(getClass().getClassLoader().getResource("package.json").getFile()).getAbsolutePath();
		
		JsonObject input = Json.createObjectBuilder().add("File", path).add("Regex", "\"version\" *: *\"[^\"]*\"")
				.add("Replacement",  "\"version\": \"test\"").build();

		Output<JsonObject> output = ctx.run("Sed_file", input.toString());
		
		System.out.println(output.getPayload());
	}
	
	@Test
	public void test_zip() throws Exception {
		String path = new File(getClass().getClassLoader().getResource("package.json").getFile()).getParent();
		
		JsonObject input = Json.createObjectBuilder().add("Folder", path).add("Destination", path+ File.separator +".." + File.separator + "test_zip.zip").build();
		
		Output<JsonObject> output = ctx.run("Zip_file", input.toString());
		
		System.out.println(output.getPayload());
	}
	
	@Test
	public void test_unzip() throws Exception {
		String file = new File(getClass().getClassLoader().getResource("test.zip").getFile()).getPath();
		String path = new File(getClass().getClassLoader().getResource("test.zip").getFile()).getParent();
				
		JsonObject input = Json.createObjectBuilder().add("File", file).add("Destination", path+ File.separator +".." + File.separator).build();

		Output<JsonObject> output = ctx.run("Unzip_file", input.toString());
		
		System.out.println(output.getPayload());
	}
}
