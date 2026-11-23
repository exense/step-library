package ch.exense.step.library.playwright;

import ch.exense.step.registry.ApplicationRegistry;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import step.functions.io.Output;
import step.grid.io.Attachment;
import step.handlers.javahandler.KeywordRunner;

import javax.json.JsonObject;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ch.exense.step.registry.ExecutionContext.LAUNCH_PROFILE_ENV;
import static ch.exense.step.registry.ExecutionContext.currentOs;

/**
 * Runs the keywords as Step does, through the Keyword API, against a registration form served locally and registered
 * as a Playwright target. Needs the Playwright browsers, which Playwright downloads at the first run.
 */
public class PlaywrightKeywordsTest {

    private static HttpServer server;
    private static Path registry;

    private KeywordRunner.ExecutionContext context;

    @BeforeClass
    public static void serveAndRegister() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            try (InputStream page = PlaywrightKeywordsTest.class.getResourceAsStream(
                path.endsWith("/") ? path + "index.html" : path)) {
                byte[] body = page == null ? new byte[0] : page.readAllBytes();
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(page == null ? 404 : 200, page == null ? -1 : body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();

        registry = Files.createTempFile("application-registry", ".yaml");
        Files.writeString(registry, String.join("\n",
            "schemaVersion: 1",
            "revision: r7",
            "applications:",
            "  shop:",
            "    environments:",
            "      test:",
            "        sutUrl: http://localhost:" + server.getAddress().getPort() + "/app/",
            "        automation: {tool: playwright, settings: {browser: chromium}}",
            "        launchProfiles:",
            "          local: {os: " + currentOs() + "}",
            "  desk:",
            "    environments:",
            "      test:",
            "        automation: {tool: oryon}",
            "        launchProfiles:",
            "          local: {os: " + currentOs() + ", startCommandTemplate: 'java {oryon-agent}'}",
            ""), StandardCharsets.UTF_8);
        PlaywrightKeywords.registryEnvironment = Map.of(
            ApplicationRegistry.REGISTRY_FILE_ENV, registry.toString(),
            LAUNCH_PROFILE_ENV, "local")::get;
    }

    @AfterClass
    public static void stopServing() throws Exception {
        server.stop(0);
        Files.deleteIfExists(registry);
    }

    @Before
    public void openContext() {
        context = KeywordRunner.getExecutionContext(PlaywrightKeywords.class);
        context.setThrowExceptionOnError(false);
    }

    @After
    public void closeContext() {
        context.close();
    }

    private Output<JsonObject> run(String keyword, String input) throws Exception {
        return context.run(keyword, input);
    }

    private Output<JsonObject> ok(String keyword, String input) throws Exception {
        Output<JsonObject> output = run(keyword, input);
        Assert.assertNull(keyword + ": " + output.getError(), output.getError());
        return output;
    }

    private void start() throws Exception {
        ok("Playwright_Start", "{\"applicationId\":\"shop\",\"environmentId\":\"test\"}");
    }

    @Test
    public void registersAUserStepByStep() throws Exception {
        Output<JsonObject> started = ok("Playwright_Start", "{\"applicationId\":\"shop\",\"environmentId\":\"test\"}");
        Assert.assertEquals("shop/test", started.getPayload().getString("target"));
        Assert.assertEquals("local", started.getPayload().getString("launchProfile"));
        Assert.assertEquals("r7", started.getPayload().getString("registryRevision"));

        ok("Playwright_Fill", "{\"locator\":\"role=textbox[name=\\\"Full name\\\"]\",\"text\":\"Ada\"}");
        ok("Playwright_Fill", "{\"locator\":\"data-testid=email\",\"text\":\"ada@example.invalid\"}");
        ok("Playwright_Select", "{\"locator\":\"role=combobox[name=\\\"Country\\\"]\",\"value\":\"France\"}");
        ok("Playwright_Check", "{\"locator\":\"role=checkbox[name=\\\"I accept the terms\\\"]\"}");
        ok("Playwright_Hover", "{\"locator\":\"role=button[name=\\\"Register\\\"]\"}");
        ok("Playwright_Click", "{\"locator\":\"role=button[name=\\\"Register\\\"]\"}");
        ok("Playwright_WaitFor", "{\"locator\":\"#result\"}");

        Assert.assertEquals("Welcome Ada (fr)",
            ok("Playwright_GetText", "{\"locator\":\"#result\"}").getPayload().getString("text"));
        Assert.assertEquals("ada@example.invalid",
            ok("Playwright_GetText", "{\"locator\":\"data-testid=email\"}").getPayload().getString("text"));

        ok("Playwright_Press", "{\"locator\":\"#name\",\"key\":\"Control+A\"}");
        ok("Playwright_Navigate", "{\"path\":\"account/profile.html\"}");
        Assert.assertEquals("Your profile",
            ok("Playwright_GetText", "{\"locator\":\"role=heading\"}").getPayload().getString("text"));

        ok("Playwright_Stop", "{}");
        Assert.assertTrue(run("Playwright_Click", "{\"locator\":\"#register\"}").getError().getMsg()
            .contains(PlaywrightKeywords.NO_BROWSER));
    }

    @Test
    public void actionsOutsideASessionFail() throws Exception {
        Assert.assertEquals(PlaywrightKeywords.NO_BROWSER,
            run("Playwright_Click", "{\"locator\":\"#register\"}").getError().getMsg());
        Assert.assertNull(run("Playwright_Stop", "{}").getError());
    }

    @Test
    public void startRefusesATargetOfAnotherTool() throws Exception {
        String error = run("Playwright_Start", "{\"applicationId\":\"desk\",\"environmentId\":\"test\"}")
            .getError().getMsg();
        Assert.assertTrue(error, error.contains("automated with 'oryon', not playwright"));
        Assert.assertTrue(run("Playwright_Start", "{\"applicationId\":\"shop\",\"environmentId\":\"prod\"}")
            .getError().getMsg().contains("does not support target environment 'prod'"));
    }

    @Test
    public void navigateRefusesAnAddress() throws Exception {
        start();
        for (String address : List.of("https://example.invalid/", "//example.invalid/x")) {
            String error = run("Playwright_Navigate", "{\"path\":\"" + address + "\"}").getError().getMsg();
            Assert.assertTrue(error, error.contains("not an address"));
        }
    }

    @Test
    public void aFailedActionAttachesAScreenshot() throws Exception {
        start();
        // two textboxes match: a strict-mode violation fails at once
        Output<JsonObject> failed = run("Playwright_Click", "{\"locator\":\"role=textbox\"}");
        Assert.assertNotNull(failed.getError());
        List<String> attachments = failed.getAttachments().stream().map(Attachment::getName)
            .collect(Collectors.toList());
        Assert.assertTrue(attachments.toString(), attachments.contains("screenshot.png"));
    }
}
