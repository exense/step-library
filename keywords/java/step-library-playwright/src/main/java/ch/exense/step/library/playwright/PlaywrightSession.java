package ch.exense.step.library.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.io.Closeable;
import java.net.URI;

/**
 * What the Playwright keywords keep in the Step session: the browser opened by {@code Playwright_Start} on the
 * resolved address of the target. Step closes it with the session, which closes the browser if
 * {@code Playwright_Stop} did not.
 */
public class PlaywrightSession implements Closeable {

    private final Playwright playwright;
    private final Browser browser;
    private final Page page;
    private final URI sutUrl;
    private boolean closed;

    PlaywrightSession(Playwright playwright, Browser browser, Page page, URI sutUrl) {
        this.playwright = playwright;
        this.browser = browser;
        this.page = page;
        this.sutUrl = sutUrl;
    }

    public Page getPage() {
        return page;
    }

    /**
     * Resolves a path against the address of the target, refusing any address on another origin: a plan names pages
     * of its target, never an address.
     */
    URI resolve(String path) {
        URI relative = URI.create(path);
        if (relative.isAbsolute() || relative.getRawAuthority() != null) {
            throw new IllegalArgumentException("Give a path relative to the application, not an address: '" + path + "'.");
        }
        return sutUrl.resolve(relative);
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    /** Closes the browser. Does nothing if already done. */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            browser.close();
        } finally {
            playwright.close();
        }
    }
}
