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
		JsonObject input = Json.createObjectBuilder().add("mainClassOrJar", echoJar).build();
		Output<JsonObject> output = ctx.run("Java", input.toString());
		Assert.assertTrue(output.getPayload().getString("stderr").equals(""));
	}

	@Test
	public void testClassPath() throws Exception {
		ctx.setThrowExceptionOnError(false);
		String echoJar = new File(getClass().getClassLoader().getResource("echo.jar").getFile()).getAbsolutePath();
		JsonObject input = Json.createObjectBuilder().add("mainClassOrJar", "echo.Echo").add("classPath",echoJar).build();
		Output<JsonObject> output = ctx.run("Java", input.toString());
		Assert.assertTrue(output.getPayload().getString("stderr").equals(""));
	}
	
	@Test
	public void testProgramArgs() throws Exception {
		ctx.setThrowExceptionOnError(false);
		String echoJar = new File(getClass().getClassLoader().getResource("echo.jar").getFile()).getAbsolutePath();
		JsonObject input = Json.createObjectBuilder().add("mainClassOrJar", echoJar).add("programArgs", "TEST").build();
		Output<JsonObject> output = ctx.run("Java", input.toString());
		Assert.assertTrue(output.getPayload().getString("stdout").startsWith("TEST"));
		Assert.assertTrue(output.getPayload().getString("stderr").equals(""));
	}
	
	@Test
	public void testVmArgs() throws Exception {
		ctx.setThrowExceptionOnError(false);
		String echoJar = new File(getClass().getClassLoader().getResource("echo.jar").getFile()).getAbsolutePath();
		JsonObject input = Json.createObjectBuilder().add("mainClassOrJar", echoJar).add("vmArgs", "-Xmx0G").build();
		Output<JsonObject> output = ctx.run("Java", input.toString());
		Assert.assertTrue(output.getPayload().getString("stderr").contains("Could not create"));
	}
	
}
