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
import ch.exense.step.library.kw.system.StreamingAttachmentsExecutionContext.StreamedAttachment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import step.functions.io.Output;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProcessKeywordsTest {

	private static final String COMMAND_KEYWORD = executeCommandKeyword();
	private static final String ECHO_ENV_PASSWORD = "echo " + setEnvVariableSyntax("Password");
	private static final String UNRESOLVED_ENV_VARIABLE_VALUE = setEnvVariableOutput("Password") + "\n";
	private static final String EXPECTED_ARTIFACT_CONTENT = isWindows() ? "test\r\n" : "test\n";
	private StreamingAttachmentsExecutionContext ctx;

	@Before
	public void setUp() throws Exception {
		ctx = new StreamingAttachmentsExecutionContext(ProcessKeywords.class);
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
		// The process output isn't attached to the output anymore. It is streamed as attachment instead
		assertTrue(output.getAttachments() == null || output.getAttachments().isEmpty());
		String streamedProcessError = ctx.getStreamedAttachment(StreamingAttachmentsExecutionContext.PROCESS_ERROR_LOG).getContentAsString();
		assertTrue(streamedProcessError.startsWith("java version") || streamedProcessError.startsWith("openjdk "));
	}

	@Test
	public void testMaxAttachmentSize() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "java -version").add("Max_Output_Payload_Size", "1")
				.add("Max_Output_Attachment_Size", "1").build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());

		assertTrue(output.getPayload().getString("stderr").equals("j") ||
				output.getPayload().getString("stderr").equals("o"));
		// The process output is streamed as attachment and therefore not truncated by Max_Output_Attachment_Size
		assertTrue(output.getAttachments() == null || output.getAttachments().isEmpty());
		String streamedProcessError = ctx.getStreamedAttachment(StreamingAttachmentsExecutionContext.PROCESS_ERROR_LOG).getContentAsString();
		assertTrue(streamedProcessError.startsWith("java version") || streamedProcessError.startsWith("openjdk "));
	}

	@Test
	public void testArtifacts() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "(echo test)>test.log")
				.add("Artifacts", Json.createArrayBuilder().add("test.log").build()).build();
		ctx.run(COMMAND_KEYWORD, input.toString());

		List<StreamedAttachment> attachments = ctx.getStreamedAttachmentsWithoutProcessOutputs();
		assertEquals(1, attachments.size());
		assertFirstAttachment(attachments);
	}

	@Test
	public void testEnvironment() throws Exception {
		StreamingAttachmentsExecutionContext old_ctx = ctx;

		ctx = new StreamingAttachmentsExecutionContext(Map.of("Password","glop"),ProcessKeywords.class);
		// set the env variables
		JsonObject input = Json.createObjectBuilder().add("Command", ECHO_ENV_PASSWORD).add("Pass_Properties_As_Env_Variables", true)
				.build();
		Output<JsonObject> output = ctx.run(COMMAND_KEYWORD, input.toString());

		System.out.println(output.getPayload());
		assertTrue(output.getPayload().getString("stdout").startsWith("glop"));

		// DO NOT set the env variables
		input = Json.createObjectBuilder().add("Command", ECHO_ENV_PASSWORD).add("Pass_Properties_As_Env_Variables", false)
				.build();
		output = ctx.run(COMMAND_KEYWORD, input.toString());

		assertTrue(output.getPayload().getString("stdout").equals(UNRESOLVED_ENV_VARIABLE_VALUE));

		// with no properties - Pass_Properties_As_Env_Variables = false
		ctx.close();
		ctx = old_ctx;

		input = Json.createObjectBuilder().add("Command", ECHO_ENV_PASSWORD).add("Pass_Properties_As_Env_Variables", false)
				.build();
		output = ctx.run(COMMAND_KEYWORD, input.toString());

		assertTrue(output.getPayload().getString("stdout").equals(UNRESOLVED_ENV_VARIABLE_VALUE));

		// with no properties - Pass_Properties_As_Env_Variables = true
		input = Json.createObjectBuilder().add("Command", ECHO_ENV_PASSWORD).add("Pass_Properties_As_Env_Variables", true)
				.build();
		output = ctx.run(COMMAND_KEYWORD, input.toString());

		assertTrue(output.getPayload().getString("stdout").equals(UNRESOLVED_ENV_VARIABLE_VALUE));
	}

	private static String executeCommandKeyword() {
		return isWindows() ? "ExecuteCmd" : "ExecuteBash";
	}

	private static String setEnvVariableSyntax(String enVariable) {
		return isWindows() ? String.format("%%%s%%", enVariable): String.format("$%s", enVariable) ;
	}

	private static String setEnvVariableOutput(String enVariable) {
		return isWindows() ? String.format("%%%s%%", enVariable): "" ;
	}

	public static boolean isWindows() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase().startsWith("windows");
	}

	private static void assertFirstAttachment(List<StreamedAttachment> attachments) throws Exception {
		assertAttachment(attachments.get(0), "test.log");
	}

	private static void assertAttachment(StreamedAttachment attachment, String expected) throws Exception {
		assertEquals(expected, attachment.getName());
		assertAttachmentContent(attachment);
	}

	private static void assertAttachmentContent(StreamedAttachment attachment) throws Exception {
		assertEquals(EXPECTED_ARTIFACT_CONTENT, attachment.getContentAsString());
	}

	@Test
	public void testArtifacts2() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "(echo test)>test.log")
				.add("Artifacts", Json.createArrayBuilder().add("test.log").add("test.log").build()).build();
		ctx.run(COMMAND_KEYWORD, input.toString());

		List<StreamedAttachment> attachments = ctx.getStreamedAttachmentsWithoutProcessOutputs();
		assertEquals(2, attachments.size());
		assertAttachment(attachments.get(0), "test.log");
		assertAttachment(attachments.get(1), "test.log");
	}

	@Test
	public void testArtifactsWithRegex() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "(echo test)>test1.log && (echo test)>test2.log")
				.add("Artifacts", Json.createArrayBuilder().add("test.*").build()).build();
		ctx.run(COMMAND_KEYWORD, input.toString());

		List<StreamedAttachment> attachments = ctx.getStreamedAttachmentsWithoutProcessOutputs();
		assertEquals(2, attachments.size());
		assertEquals(List.of("test1.log", "test2.log"),
				attachments.stream().map(StreamedAttachment::getName).sorted().collect(Collectors.toList()));
		assertAttachmentContent(attachments.get(0));
		assertAttachmentContent(attachments.get(1));
	}

	@Test
	public void testArtifactsAsDirectory() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "mkdir test && (echo test)>test/test.log")
				.add("Artifacts", Json.createArrayBuilder().add("test").build()).build();
		ctx.run(COMMAND_KEYWORD, input.toString());

		List<StreamedAttachment> attachments = ctx.getStreamedAttachmentsWithoutProcessOutputs();
		assertEquals(1, attachments.size());
		StreamedAttachment attachment = attachments.get(0);
		assertEquals("test.zip", attachment.getName());
		File tempFolder = FileHelper.createTempFolder();
		try {
			FileHelper.unzip(attachment.getContent(), tempFolder);
			assertEquals("test.log", tempFolder.listFiles()[0].getName());
		} finally {
			FileHelper.deleteFolder(tempFolder);
		}
	}

	@Test
	public void testArtifactsWithAbsolutePath() throws Exception {
		Path tempFile = Files.createTempFile("test", ".txt");
		tempFile.toFile().deleteOnExit();
		JsonObject input = Json.createObjectBuilder().add("Command", "(echo test)>" + tempFile)
				.add("Artifacts", Json.createArrayBuilder().add(tempFile.toString()).build()).build();
		ctx.run(COMMAND_KEYWORD, input.toString());

		List<StreamedAttachment> attachments = ctx.getStreamedAttachmentsWithoutProcessOutputs();
		assertEquals(1, attachments.size());
		StreamedAttachment attachment = attachments.get(0);
		assertEquals(tempFile.getFileName().toString(), attachment.getName());
		assertAttachmentContent(attachment);
	}
}
