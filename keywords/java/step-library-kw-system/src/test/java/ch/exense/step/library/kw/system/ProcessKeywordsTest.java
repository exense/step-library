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

import ch.exense.commons.io.FileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import step.functions.io.Output;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
		assertTrue(output.getPayload().getString("stderr").startsWith("java version") ||
				output.getPayload().getString("stderr").startsWith("openjdk "));
	}
	
	@Test
	public void testExitCode() throws Exception {
		ctx.setThrowExceptionOnError(false);
		JsonObject input = Json.createObjectBuilder().add("Command", "java").build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());
		assertEquals("Process exited with code 1", output.getError().getMsg());
	}
	
	@Test
	public void testCheckExitCode() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "java").add("Check_Exit_Code", false).build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());
		assertEquals("1", output.getPayload().getString("Exit_code"));
	}

	@Test
	public void testTimeout() throws Exception {
		ctx.setThrowExceptionOnError(false);
		JsonObject input = Json.createObjectBuilder().add("Command", "java -verbose -version").add("Timeout_ms", "1").build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());
		assertEquals("The process did not exit within the configured timeout of 1ms. You can increase this value using the 'Timeout_ms' input.", output.getError().getMsg());
	}

	@Test
	public void testMaxPayloadSize() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "java -version").add("Max_Output_Payload_Size", "1")
				.build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());

		assertTrue(output.getPayload().getString("stderr").equals("j") ||
				output.getPayload().getString("stderr").equals("o"));
		// TODO: the process output is streamed and not returned as attachment anymore. Currently, streamed attachments are not accessible in local executions.
		// As soon as streamed attachments can be accessed locally, we should add a proper assertion
	}

	@Test
	public void testMaxAttachmentSize() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "java -version").add("Max_Output_Payload_Size", "1")
				.add("Max_Output_Attachment_Size", "1").build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());

		assertTrue(output.getPayload().getString("stderr").equals("j") ||
				output.getPayload().getString("stderr").equals("o"));
		// TODO: the process output is streamed and not returned as attachment anymore. Currently, streamed attachments are not accessible in local executions.
		// As soon as streamed attachments can be accessed locally, we should add a proper assertion
	}

	@Test
	public void testArtifacts() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "(echo test)>test.log")
				.add("Artifacts", Json.createArrayBuilder().add("test.log").build()).build();
		Output<JsonObject> output = ctx.run(executeCommandKeyword(), input.toString());

		List<Attachment> attachments = output.getAttachments();
		assertEquals(1, attachments.size());
		assertFirstAttachment(attachments);
	}

	private static String executeCommandKeyword() {
		return isWindows() ? "ExecuteCmd" : "ExecuteBash";
	}

	public static boolean isWindows() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase().startsWith("windows");
	}

	private static void assertFirstAttachment(List<Attachment> attachments) {
		Attachment attachment = attachments.get(0);
		assertAttachment(attachment, "test.log");
	}

	private static void assertAttachment(Attachment attachment, String expected) {
		assertEquals(expected, attachment.getName());
		assertAttachmentContent(attachment);
	}

	private static void assertAttachmentContent(Attachment attachment) {
		assertEquals(isWindows() ? "test\r\n" : "test\n", new String(AttachmentHelper.hexStringToByteArray(attachment.getHexContent())));
	}

	@Test
	public void testArtifacts2() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "(echo test)>test.log")
				.add("Artifacts", Json.createArrayBuilder().add("test.log").add("test.log").build()).build();
		Output<JsonObject> output = ctx.run(executeCommandKeyword(), input.toString());

		List<Attachment> attachments = output.getAttachments();
		assertEquals(2, attachments.size());
		assertFirstAttachment(attachments);
	}

	@Test
	public void testArtifactsWithRegex() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "(echo test)>test1.log && (echo test)>test2.log")
				.add("Artifacts", Json.createArrayBuilder().add("test.*").build()).build();
		Output<JsonObject> output = ctx.run(executeCommandKeyword(), input.toString());

		List<Attachment> attachments = output.getAttachments();
		assertEquals(2, attachments.size());
		assertAttachment(attachments.get(0), "test1.log");
		assertAttachment(attachments.get(1), "test2.log");
	}

	@Test
	public void testArtifactsAsDirectory() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "mkdir test && (echo test)>test/test.log")
				.add("Artifacts", Json.createArrayBuilder().add("test").build()).build();
		Output<JsonObject> output = ctx.run(executeCommandKeyword(), input.toString());

		List<Attachment> attachments = output.getAttachments();
		assertEquals(1, attachments.size());
		Attachment attachment = attachments.get(0);
		assertEquals("test.zip", attachment.getName());
		File tempFile = FileHelper.createTempFolder();
		try {
			FileHelper.unzip(AttachmentHelper.hexStringToByteArray(attachment.getHexContent()), tempFile);
			assertEquals("test.log", tempFile.listFiles()[0].getName());
		} finally {
			FileHelper.deleteFolder(tempFile);
		}
	}

	@Test
	public void testArtifactsWithAbsolutePath() throws Exception {
		Path tempFile = Files.createTempFile("test", ".txt");
		tempFile.toFile().deleteOnExit();
		JsonObject input = Json.createObjectBuilder().add("Command", "(echo test)>" + tempFile)
				.add("Artifacts", Json.createArrayBuilder().add(tempFile.toString()).build()).build();
		Output<JsonObject> output = ctx.run(executeCommandKeyword(), input.toString());

		List<Attachment> attachments = output.getAttachments();
		assertEquals(1, attachments.size());
		Attachment attachment = attachments.get(0);
		assertEquals(tempFile.getFileName().toString(), attachment.getName());
	}
}
