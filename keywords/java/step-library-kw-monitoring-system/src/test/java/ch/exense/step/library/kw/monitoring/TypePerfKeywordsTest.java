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
		JsonObject input = Json.createObjectBuilder().build();
		Output<JsonObject> output = ctx.run("Typeperf", input.toString());
		System.out.println(output.getPayload());
		assertTrue(output.getMeasures().size()>0);
		assertTrue(output.getPayload().containsKey("MemoryAvailableMB"));
		assertTrue(output.getPayload().containsKey("CPU(%)"));
	}
}
