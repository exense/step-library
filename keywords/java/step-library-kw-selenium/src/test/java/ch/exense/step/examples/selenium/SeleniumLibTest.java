package ch.exense.step.examples.selenium;

import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import ch.exense.step.examples.selenium.keyword.SeleniumKeyword;
import org.junit.Before;
import org.junit.Test;


import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class SeleniumLibTest {
	private ExecutionContext ctx;
	private Map<String,String> agentProperties;

	@Before
	public void setUp() {
		Map<String, String> properties = new HashMap<>();
		ctx = KeywordRunner.getExecutionContext(properties, SeleniumKeyword.class);
	}
	
	//@Test
	//Chromedriver test on build server are not working (the driver process hangs and keep an handle on the temp folder,
	// so cleanup fail)
	public void TestSeleniumLib() throws Exception {
		Output<JsonObject> output;
		String inputs = Json.createObjectBuilder().add("headless", false).build().toString();
		output = ctx.run("Open_chrome", inputs);
		System.out.println(output.getPayload());
		assertNull(output.getError());

		inputs = Json.createObjectBuilder().add("url", "https://www.exense.ch").build().toString();
		output = ctx.run("Navigate_to_page", inputs);
		System.out.println(output.getPayload());
		assertNull(output.getError());

		output = ctx.run("Close_chrome", "{}");
		System.out.println(output.getPayload());
		assertNull(output.getError());
	}

}
