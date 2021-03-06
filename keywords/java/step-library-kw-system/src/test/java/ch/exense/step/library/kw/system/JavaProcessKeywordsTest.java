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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class JavaProcessKeywordsTest {

	private ExecutionContext ctx;

	@Before
	public void setUp() {
		ctx = KeywordRunner.getExecutionContext(JavaProcessKeywords.class);
	}

	@After
	public void tearDown() {
		ctx.close();
	}

	@Test
	public void testJar() throws Exception {
		String echoJar = new File(getClass().getClassLoader().getResource("echo.jar").getFile()).getAbsolutePath();
		JsonObject input = Json.createObjectBuilder().add("Mainclass_or_Jar", echoJar).build();
		Output<JsonObject> output = ctx.run("Java", input.toString());
		Assert.assertTrue(output.getPayload().getString("stderr").equals(""));
	}

	@Test
	public void testClassPath() throws Exception {
		ctx.setThrowExceptionOnError(false);
		String echoJar = new File(getClass().getClassLoader().getResource("echo.jar").getFile()).getAbsolutePath();
		JsonObject input = Json.createObjectBuilder().add("Mainclass_or_Jar", "echo.Echo").add("Classpath",echoJar).build();
		Output<JsonObject> output = ctx.run("Java", input.toString());
		Assert.assertTrue(output.getPayload().getString("stderr").equals(""));
	}
	
	@Test
	public void testProgramArgs() throws Exception {
		ctx.setThrowExceptionOnError(false);
		String echoJar = new File(getClass().getClassLoader().getResource("echo.jar").getFile()).getAbsolutePath();
		JsonObject input = Json.createObjectBuilder().add("Mainclass_or_Jar", echoJar).add("Program_args", "TEST").build();
		Output<JsonObject> output = ctx.run("Java", input.toString());
		Assert.assertTrue(output.getPayload().getString("stdout").startsWith("TEST"));
		Assert.assertTrue(output.getPayload().getString("stderr").equals(""));
	}
	
	@Test
	public void testVmArgs() throws Exception {
		ctx.setThrowExceptionOnError(false);
		String echoJar = new File(getClass().getClassLoader().getResource("echo.jar").getFile()).getAbsolutePath();
		JsonObject input = Json.createObjectBuilder().add("Mainclass_or_Jar", echoJar).add("VM_args", "-Xmx0G").build();
		Output<JsonObject> output = ctx.run("Java", input.toString());
		Assert.assertTrue(output.getPayload().getString("stderr").contains("Could not create"));
	}
	
}
