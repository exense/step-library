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
package ch.exense.step.library.kw.monitoring;

import static org.junit.Assert.assertTrue;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class TypePerfKeywordsTest {

	protected ExecutionContext ctx;

	@Before
	public void setUp() {
		ctx = KeywordRunner.getExecutionContext(TypePerfKeywords.class);
	}

	@After
	public void tearDown() {
		ctx.close();
	}

	@Test
	public void testTypePerfManagedProcessKeyword() throws Exception {
		if (System.getProperty("os.name", "generic").toLowerCase().contains("win")) {
			JsonObject input = Json.createObjectBuilder().build();
			Output<JsonObject> output = ctx.run("Typeperf", input.toString());
			System.out.println(output.getPayload());
			assertTrue(output.getMeasures().size() > 0);
			assertTrue(output.getPayload().containsKey("MemoryAvailableMB"));
			assertTrue(output.getPayload().containsKey("CPU(%)"));
		}
	}
}
