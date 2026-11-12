package ch.exense.step.library.playwright;

import ch.exense.step.library.commons.AbstractEnhancedKeyword;
import ch.exense.step.registry.ApplicationRegistry;
import ch.exense.step.registry.ExecutionContext;
import ch.exense.step.registry.LaunchSpec;
import ch.exense.step.registry.PersistentEnvironment;
import ch.exense.step.registry.RegistryException;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.Input;
import step.handlers.javahandler.Keyword;

import java.net.URI;
import java.util.Locale;
import java.util.function.Function;

/**
 * Low level keywords driving a web application through Playwright, one browser action per keyword, the element being
 * given by a Playwright selector such as {@code role=button[name="Register"]}.
 * <p>
 * {@code Playwright_Start} opens the target of the application registry in a browser and keeps it in the Step session,
 * where the other keywords find it: a plan calls them within a Session. The browser is closed by
 * {@code Playwright_Stop}, or else when the session ends. A plan never holds an address: the registry gives it.
 */
public class PlaywrightKeywords extends AbstractEnhancedKeyword {

    static final String TOOL = "playwright";
    static final String NO_BROWSER = "No browser in this session: call Playwright_Start first, within a Session.";

    /** The environment the registry is read from; replaced in the tests. */
    static Function<String, String> registryEnvironment = new PersistentEnvironment();

    // ---- lifecycle ----------------------------------------------------------------------------------------------

    @Keyword(name = "Playwright_Start", description = "Opens the web application given by the applicationId and "
        + "environmentId of the application registry in a browser, and keeps it in the session for the other "
        + "Playwright keywords.")
    public void start(@Input(name = "applicationId", required = true) String applicationId,
                      @Input(name = "environmentId", required = true) String environmentId) {
        PlaywrightSession existing = session.get(PlaywrightSession.class);
        if (existing != null && !existing.isClosed()) {
            throw new IllegalStateException("A browser is already open in this session. Call Playwright_Stop first.");
        }
        LaunchSpec spec = ApplicationRegistry.fromEnvironment(registryEnvironment)
            .resolveLaunch(applicationId, environmentId, ExecutionContext.fromEnvironment(registryEnvironment));
        String target = spec.application().id() + "/" + spec.environment().id();
        if (!TOOL.equals(spec.tool())) {
            throw new RegistryException("Registry target '" + target + "' is automated with '" + spec.tool()
                + "', not " + TOOL + ".");
        }
        URI sutUrl = URI.create(spec.sutUrl().orElseThrow(
            () -> new RegistryException("Registry target '" + target + "' declares no sutUrl.")));

        Playwright playwright = Playwright.create();
        try {
            Browser browser = browserType(playwright, setting(spec, "browser", "chromium"))
                .launch(new BrowserType.LaunchOptions()
                    .setHeadless(Boolean.parseBoolean(setting(spec, "headless", "true"))));
            Page page = browser.newContext().newPage();
            PlaywrightSession playwrightSession = new PlaywrightSession(playwright, browser, page, sutUrl);
            session.put(playwrightSession);
            page.navigate(sutUrl.toString());
        } catch (RuntimeException e) {
            playwright.close();
            throw e;
        }
        output.add("target", target);
        output.add("launchProfile", spec.profile().id());
        output.add("registryRevision", spec.registryRevision());
    }

    @Keyword(name = "Playwright_Stop", description = "Closes the browser opened by Playwright_Start. Does nothing if "
        + "none is open.")
    public void stop() {
        PlaywrightSession playwrightSession = session.get(PlaywrightSession.class);
        if (playwrightSession != null) {
            playwrightSession.close();
        }
    }

    // ---- actions ------------------------------------------------------------------------------------------------

    @Keyword(name = "Playwright_Navigate", description = "Opens the page given by its path, relative to the address "
        + "of the application, e.g. /account/register.")
    public void navigate(@Input(name = "path", required = true) String path) {
        PlaywrightSession playwrightSession = requireSession();
        playwrightSession.getPage().navigate(playwrightSession.resolve(path).toString());
    }

    @Keyword(name = "Playwright_Click", description = "Clicks the element given by its Playwright selector.")
    public void click(@Input(name = "locator", required = true) String locator) {
        locator(locator).click();
    }

    @Keyword(name = "Playwright_Hover", description = "Moves the mouse over the element given by its Playwright "
        + "selector.")
    public void hover(@Input(name = "locator", required = true) String locator) {
        locator(locator).hover();
    }

    @Keyword(name = "Playwright_Fill", description = "Replaces the text of the field given by its Playwright selector.")
    public void fill(@Input(name = "locator", required = true) String locator,
                     @Input(name = "text", required = true) String text) {
        locator(locator).fill(text);
    }

    @Keyword(name = "Playwright_Select", description = "Selects the option, given by its value or label, in the "
        + "selection field given by its Playwright selector.")
    public void select(@Input(name = "locator", required = true) String locator,
                       @Input(name = "value", required = true) String value) {
        locator(locator).selectOption(value);
    }

    @Keyword(name = "Playwright_Check", description = "Checks, or with checked false unchecks, the checkbox or radio "
        + "button given by its Playwright selector.")
    public void check(@Input(name = "locator", required = true) String locator,
                      @Input(name = "checked", defaultValue = "true") Boolean checked) {
        locator(locator).setChecked(checked == null || checked);
    }

    @Keyword(name = "Playwright_Press", description = "Presses a key, such as Enter or Control+A, on the element given "
        + "by its Playwright selector.")
    public void press(@Input(name = "locator", required = true) String locator,
                      @Input(name = "key", required = true) String key) {
        locator(locator).press(key);
    }

    @Keyword(name = "Playwright_GetText", description = "Returns the visible text of the element given by its "
        + "Playwright selector, as output text, or the value of a field.")
    public void getText(@Input(name = "locator", required = true) String locator) {
        Locator element = locator(locator);
        boolean field = (Boolean) element.evaluate(
            "e => e instanceof HTMLInputElement || e instanceof HTMLTextAreaElement || e instanceof HTMLSelectElement");
        output.add("text", field ? element.inputValue() : element.innerText());
    }

    @Keyword(name = "Playwright_WaitFor", description = "Waits until the element given by its Playwright selector is "
        + "in the state: visible (default), hidden, attached or detached. Actions wait on their own; use it only for "
        + "a change no following action waits for.")
    public void waitFor(@Input(name = "locator", required = true) String locator,
                        @Input(name = "state", defaultValue = "visible") String state) {
        WaitForSelectorState selectorState = WaitForSelectorState.valueOf(
            (state == null ? "visible" : state).toUpperCase(Locale.ROOT));
        locator(locator).waitFor(new Locator.WaitForOptions().setState(selectorState));
    }

    // ---- failures -----------------------------------------------------------------------------------------------

    /** Attaches a screenshot of the page to a failed keyword, then lets Step report the error. */
    @Override
    public boolean onError(Exception e) {
        PlaywrightSession playwrightSession = session != null ? session.get(PlaywrightSession.class) : null;
        if (playwrightSession != null && !playwrightSession.isClosed()) {
            try {
                output.addAttachment(AttachmentHelper.generateAttachmentFromByteArray(
                    playwrightSession.getPage().screenshot(), "screenshot.png"));
            } catch (RuntimeException screenshotError) {
                logger.warn("Could not take a screenshot of the failure", screenshotError);
            }
        }
        return super.onError(e);
    }

    // ---- internals ----------------------------------------------------------------------------------------------

    private Locator locator(String selector) {
        return requireSession().getPage().locator(selector);
    }

    private PlaywrightSession requireSession() {
        PlaywrightSession playwrightSession = session != null ? session.get(PlaywrightSession.class) : null;
        if (playwrightSession == null || playwrightSession.isClosed()) {
            throw new IllegalStateException(NO_BROWSER);
        }
        return playwrightSession;
    }

    private static String setting(LaunchSpec spec, String key, String fallback) {
        Object value = spec.toolSettings().get(key);
        return value == null ? fallback : value.toString();
    }

    private static BrowserType browserType(Playwright playwright, String browser) {
        switch (browser.toLowerCase(Locale.ROOT)) {
            case "chromium":
                return playwright.chromium();
            case "firefox":
                return playwright.firefox();
            case "webkit":
                return playwright.webkit();
            default:
                throw new RegistryException("Unsupported browser '" + browser + "': use chromium, firefox or webkit.");
        }
    }
}
