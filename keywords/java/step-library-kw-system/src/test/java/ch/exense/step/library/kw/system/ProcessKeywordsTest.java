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

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class ProcessKeywordsTest {

	private ExecutionContext ctx;

	@Before
	public void setUp() {
		ctx = KeywordRunner.getExecutionContext(ProcessKeywords.class);
	}

	@After
	public void tearDown() {
		ctx.close();
	}

	@Test
	public void test1() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "java -version").build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());
		Assert.assertTrue(output.getPayload().getString("stderr").startsWith("java version") ||
				output.getPayload().getString("stderr").startsWith("openjdk "));
	}
	
	@Test
	public void testExitCode() throws Exception {
		ctx.setThrowExceptionOnError(false);
		JsonObject input = Json.createObjectBuilder().add("Command", "java").build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());
		Assert.assertEquals("Process exited with code 1", output.getError().getMsg());
	}
	
	@Test
	public void testCheckExitCode() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "java").add("Check_Exit_Code", false).build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());
		Assert.assertEquals("1", output.getPayload().getString("Exit_code"));
	}

	@Test
	public void testTimeout() throws Exception {
		ctx.setThrowExceptionOnError(false);
		JsonObject input = Json.createObjectBuilder().add("Command", "java -verbose -version").add("Timeout_ms", "1").build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());
		Assert.assertEquals("Process didn't exit within the defined timeout of 1ms", output.getError().getMsg());
	}

	@Test
	public void testMaxPayloadSize() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "java -version").add("Max_Output_Payload_Size", "1")
				.build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());

		Assert.assertTrue(output.getPayload().getString("stderr").equals("j") ||
				output.getPayload().getString("stderr").equals("o"));
		Assert.assertEquals("stderr.log", output.getAttachments().get(0).getName());
	}

	@Test
	public void testMaxAttachmentSize() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "java -version").add("Max_Output_Payload_Size", "1")
				.add("Max_Output_Attachment_Size", "1").build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());

		Assert.assertTrue(output.getPayload().getString("stderr").equals("j") ||
				output.getPayload().getString("stderr").equals("o"));
		Assert.assertTrue(output.getAttachments().get(0).getHexContent().equals("ag") ||
				output.getAttachments().get(0).getHexContent().equals("bw=="));
	}
}
