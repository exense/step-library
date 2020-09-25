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
