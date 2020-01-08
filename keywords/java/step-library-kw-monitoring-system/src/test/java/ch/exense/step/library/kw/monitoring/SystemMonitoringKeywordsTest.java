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

public class SystemMonitoringKeywordsTest {

	protected ExecutionContext ctx;

	@Before
	public void setUp() {
		ctx = KeywordRunner.getExecutionContext(WindowsServiceStatusKeywords.class);
	}

	@After
	public void tearDown() {
		ctx.close();
	}

	@Test
	public void testPowershellServiceStatusKeyword() throws Exception {
		JsonObject input = Json.createObjectBuilder().add("Service_Display_Name", "DHCP Client").build();
		Output<JsonObject> output = ctx.run("Windows_Service_Status", input.toString());
		assertTrue(output.getPayload().getString("Status").contains("running"));
	}
}
