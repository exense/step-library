package ch.exense.step.examples.selenium;

import ch.exense.step.examples.selenium.keyword.ChromeDriverKeyword;
import ch.exense.step.examples.selenium.keyword.GenericSeleniumKeyword;
import ch.exense.step.examples.selenium.keyword.ShadowSeleniumKeyword;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNull;

public class BrowserProxyTest {
    private KeywordRunner.ExecutionContext ctx;
    private Output<JsonObject> output;
    private String inputs;

    @Before
    public void setUp() throws Exception {
        Map<String, String> properties = new HashMap<>();
        ctx = KeywordRunner.getExecutionContext(properties, ShadowSeleniumKeyword.class, GenericSeleniumKeyword.class, ChromeDriverKeyword.class);

        inputs = Json.createObjectBuilder().add("Headless", true).add("Enable_Har_Capture", true).build().toString();
        output = ctx.run("Open_Chrome", inputs);
        System.out.println(output.getPayload());
        assertNull(output.getError());
    }

    @After
    public void destroy() throws Exception{
        output = ctx.run("Close_Driver", "{}");
        output = ctx.run("Close_Proxy", "{}");
        System.out.println(output.getPayload());
        assertNull(output.getError());
    }

    @Test
    public void TestNavigateTo() throws Exception{
        inputs = Json.createObjectBuilder()
                .add("Url", "https://step.dev")
                .build().toString();
        output = ctx.run("Navigate_To", inputs);
        System.out.println(output.getError());
        assertNull(output.getError());
    }
}
