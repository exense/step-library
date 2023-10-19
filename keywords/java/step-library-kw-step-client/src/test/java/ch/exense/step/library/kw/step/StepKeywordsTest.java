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
package ch.exense.step.library.kw.step;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.File;

public class StepKeywordsTest {

	private final static String DEFAULT_INSTANCE = "https://stepee-nhy.stepcloud-test.ch/";

	private ExecutionContext ctx;
	private Output<JsonObject> output;
	private JsonObject input;

	@Before
	public void setUp() {
		ctx = KeywordRunner.getExecutionContext(StepClientKeyword.class);
	}

	@After
	public void tearDown() {
		ctx.close();
	}

	@Test
	public void test_listTenant() throws Exception {
		input = Json.createObjectBuilder().add("User", "admin")
			.add("admin_Password", "init")
			.add("Url",DEFAULT_INSTANCE).build();
		output = ctx.run("InitStepClient", input.toString());
		assert output.getError() == null;

		output = ctx.run("ListTenants");
		System.out.println(output.getPayload());
		assert output.getError() == null;
	}

	@Test
	@Ignore
	public void test_findExecutions() throws Exception {
		input = Json.createObjectBuilder()
				.add("User", "admin")
				.add("admin_Password", "init")
				.add("Url",DEFAULT_INSTANCE).build();
		output = ctx.run("InitStepClient", input.toString());
		assert output.getError() == null;

		input = Json.createObjectBuilder()
				.add("executionParameters.customParameters.env","TEST")
				.add("status","ENDED")
				.build();
		output = ctx.run("FindExecution", input.toString());
		System.out.println(output.getPayload());
		assert output.getError() == null;
	}

	@Test
	@Ignore
	public void test_stopExecution() throws Exception {
		input = Json.createObjectBuilder()
				.add("User", "admin")
				.add("admin_Password", "init")
				.add("Url",DEFAULT_INSTANCE).build();
		output = ctx.run("InitStepClient", input.toString());
		assert output.getError() == null;

		input = Json.createObjectBuilder()
				.add("Id","6512315c8f4a8548ec7a399f")
				.build();
		output = ctx.run("StopExecution", input.toString());
		System.out.println(output.getPayload());
		assert output.getError() == null;
	}

	@Test
	public void test_upload() throws Exception {

		input = Json.createObjectBuilder().add("User", "admin")
				.add("admin_Password", "init")
				.add("Url", DEFAULT_INSTANCE).build();

		output = ctx.run("InitStepClient",input);

		String file = new File(getClass().getClassLoader().getResource("test.zip").getFile()).getPath();

		JsonObject input = Json.createObjectBuilder().add("File", file).build();

		output = ctx.run("UploadResource", input.toString());

		System.out.println(output.getPayload());
	}
}
