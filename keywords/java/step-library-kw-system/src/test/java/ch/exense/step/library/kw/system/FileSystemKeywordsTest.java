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
	public void test_ls() throws Exception {
		String path = new File(getClass().getClassLoader().getResource("package.json").getFile()).getParent();

		JsonObject input = Json.createObjectBuilder().add("Folder", path).build();

		Output<JsonObject> output = ctx.run("Ls", input.toString());

		System.out.println(output.getPayload());
	}

	@Test
	public void test_find() throws Exception {
		String path = new File(getClass().getClassLoader().getResource("package.json").getFile()).getParent();

/*
		JsonObject input = Json.createObjectBuilder()
				.add("Folder", "C:\\tmp\\")
				.add("Regex",".*\\\\conf\\\\.*\\.json").build();
*/
		JsonObject input = Json.createObjectBuilder()
				.add("Folder", path)
				.add("Regex",".*\\.json").build();

		Output<JsonObject> output = ctx.run("Find_file", input.toString());

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
