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
package ch.exense.step.examples.selenium;

import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import ch.exense.step.examples.selenium.keyword.GenericSeleniumKeyword;
import org.junit.Before;

import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class SeleniumLibTest {
	private ExecutionContext ctx;

	@Before
	public void setUp() {
		Map<String, String> properties = new HashMap<>();
		ctx = KeywordRunner.getExecutionContext(properties, GenericSeleniumKeyword.class);
	}
	
	//@Test
	//Chromedriver test on build server are not working (the driver process hangs and keep an handle on the temp folder,
	// so cleanup fail)
	public void TestSeleniumLib() throws Exception {
		Output<JsonObject> output;
		String inputs = Json.createObjectBuilder().add("headless", false).build().toString();
		output = ctx.run("Open_chrome", inputs);
		System.out.println(output.getPayload());
		assertNull(output.getError());

		inputs = Json.createObjectBuilder().add("url", "https://www.exense.ch").build().toString();
		output = ctx.run("Navigate_to_page", inputs);
		System.out.println(output.getPayload());
		assertNull(output.getError());

		output = ctx.run("Close_chrome", "{}");
		System.out.println(output.getPayload());
		assertNull(output.getError());
	}

}
