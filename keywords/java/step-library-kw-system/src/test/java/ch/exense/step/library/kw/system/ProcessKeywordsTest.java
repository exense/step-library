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
		Assert.assertTrue(output.getPayload().getString("stderr").startsWith("java version"));
	}
	
	@Test
	public void testExitCode() throws Exception {
		ctx.setThrowExceptionOnError(false);
		JsonObject input = Json.createObjectBuilder().add("Command", "java").build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());
		Assert.assertEquals("Process exited with code 1", output.getError().getMsg());
	}

	@Test
	public void testTimeout() throws Exception {
		ctx.setThrowExceptionOnError(false);
		JsonObject input = Json.createObjectBuilder().add("Command", "cmd timeout 10").add("Timeout_ms", "1").build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());
		Assert.assertEquals("Process didn't exit within the defined timeout of 1ms", output.getError().getMsg());
	}

	@Test
	public void testMaxPayloadSize() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "java -version").add("Max_Output_Payload_Size", "1")
				.build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());

		Assert.assertEquals("j", output.getPayload().getString("stderr"));
		Assert.assertEquals("stderr.log", output.getAttachments().get(0).getName());
	}

	@Test
	public void testMaxAttachmentSize() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Command", "java -version").add("Max_Output_Payload_Size", "1")
				.add("Max_Output_Attachment_Size", "1").build();
		Output<JsonObject> output = ctx.run("Execute", input.toString());

		Assert.assertEquals("j", output.getPayload().getString("stderr"));
		Assert.assertEquals("ag==", output.getAttachments().get(0).getHexContent());
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}
}
