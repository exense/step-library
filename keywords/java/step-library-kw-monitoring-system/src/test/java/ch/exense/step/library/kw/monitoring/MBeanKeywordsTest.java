package ch.exense.step.library.kw.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class MBeanKeywordsTest {

	protected ExecutionContext ctx;

	@Before
	public void setUp() {
		ctx = KeywordRunner.getExecutionContext(MBeanKeywords.class);
	}

	@After
	public void tearDown() {
		ctx.close();
	}

	@Test
	public void testHealthStats() throws Exception {
		JsonObject input = Json.createObjectBuilder().build();
		Output<JsonObject> output = ctx.run("HealthStats", input.toString());
		assertEquals(7, output.getMeasures().size());
		assertTrue(output.getPayload().containsKey(MBeanKeywords.FREE_PHYSICAL_MEMORY_SIZE));
		assertTrue(output.getPayload().containsKey(MBeanKeywords.FREE_SWAP_MEMORY_SIZE));
		assertTrue(output.getPayload().containsKey(MBeanKeywords.HEAP_MEMORY_USAGE_MAX));
		assertTrue(output.getPayload().containsKey(MBeanKeywords.HEAP_MEMORY_USAGE_USED));
		assertTrue(output.getPayload().containsKey(MBeanKeywords.SYSTEM_CPU_LOAD));
		assertTrue(output.getPayload().containsKey(MBeanKeywords.TOTAL_PHYSICAL_MEMORY_SIZE));
		assertTrue(output.getPayload().containsKey(MBeanKeywords.TOTAL_SWAP_SPACE_SIZE));
	}
}
