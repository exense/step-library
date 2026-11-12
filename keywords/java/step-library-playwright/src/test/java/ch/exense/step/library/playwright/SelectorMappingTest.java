package ch.exense.step.library.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The mapping the assistant applies from the {@code getBy...} locators that Playwright MCP generates to the selector
 * strings the keywords take: each mapped selector must find the same element as its original.
 */
public class SelectorMappingTest {

    private static Playwright playwright;
    private static Page page;

    @BeforeClass
    public static void open() throws Exception {
        playwright = Playwright.create();
        Browser browser = playwright.chromium().launch();
        page = browser.newPage();
        try (InputStream html = SelectorMappingTest.class.getResourceAsStream("/app/index.html")) {
            page.setContent(new String(html.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @AfterClass
    public static void close() {
        playwright.close();
    }

    @Test
    public void mappedSelectorsFindTheSameElement() {
        Map<String, Locator> mapping = new LinkedHashMap<>();
        mapping.put("role=button[name=\"Register\"]",
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Register")));
        mapping.put("role=textbox[name=\"E-mail\"]", page.getByLabel("E-mail"));
        mapping.put("role=checkbox[name=\"I accept the terms\"]", page.getByLabel("I accept the terms"));
        mapping.put("role=combobox[name=\"Country\"]", page.getByLabel("Country"));
        mapping.put("data-testid=email", page.getByTestId("email"));
        mapping.put("[placeholder=\"Your name\"]", page.getByPlaceholder("Your name"));
        mapping.put("text=Full name", page.getByText("Full name"));

        for (Map.Entry<String, Locator> entry : mapping.entrySet()) {
            Locator mapped = page.locator(entry.getKey());
            Assert.assertEquals(entry.getKey(), 1, mapped.count());
            Assert.assertEquals(entry.getKey(), entry.getValue().evaluate("e => e.outerHTML"),
                mapped.evaluate("e => e.outerHTML"));
        }
    }
}
