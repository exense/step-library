package ch.exense.step.examples.selenium;

import ch.exense.step.examples.selenium.keyword.EdgeDriverKeyword;
import ch.exense.step.examples.selenium.keyword.GenericSeleniumKeyword;
import ch.exense.step.examples.selenium.keyword.ShadowSeleniumKeyword;
import ch.exense.step.library.tests.LocalOnly;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNull;

@Category(LocalOnly.class)
public class EdgeTest {
    private KeywordRunner.ExecutionContext ctx;
    private Output<JsonObject> output;
    private String inputs;

    @Before
    public void setUp() throws Exception {
        Map<String, String> properties = new HashMap<>();
        //properties.put("Edge_Driver", "C:\\Path\\To\\EdgeDriver\\msedgedriver.exe");
        ctx = KeywordRunner.getExecutionContext(properties, ShadowSeleniumKeyword.class, GenericSeleniumKeyword.class, EdgeDriverKeyword.class);

        inputs = Json.createObjectBuilder()
                .add("Headless", false)
                .add("Login_With_Client_Certificate", true)
                .add("Browser_Proxy_Port", 8081)
                //.add("Browser_Proxy_Client_Certificate_Path", "C:\\Path\\To\\Client\\Certificate\\client.p12")
                //.add("Browser_Proxy_Client_Certificate_Password", "ClientCertificatePassword")
                .build().toString();
        output = ctx.run("Open_Edge", inputs);
        System.out.println(output.getPayload());
        assertNull(output.getError());
    }

    @Test
    public void TestNavigateTo() throws Exception{
        inputs = Json.createObjectBuilder()
                // URL MUST START WITH HTTP
                .add("Url", "http://client.certificate.authentication.website")
                .build().toString();
        output = ctx.run("Navigate_To", inputs);
        System.out.println(output.getError());
        assertNull(output.getError());
    }

    @After
    public void destroy() throws Exception{
        output = ctx.run("Close_Driver", "{}");
        System.out.println(output.getPayload());
        assertNull(output.getError());
    }
}
