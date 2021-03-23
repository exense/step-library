package ch.exense.step.examples.selenium.helper;

import java.io.Closeable;

import net.lightbody.bmp.BrowserMobProxy;

/**
 * Wrapper class for BrowserMobProxy instance. 
 * The class implements the Closeable interface in order to easily manage the ProxyWrapper instance.
 */
public class ProxyWrapper implements Closeable {
	/**
	 * The BrowserMobProxy instance to be wrapped.
	 */
	final BrowserMobProxy proxy;

	/**
	 * Constructor for ProxyWrapper
	 * @param proxy the BrowserMobProxy instance to be wrapped
	 */
	public ProxyWrapper(BrowserMobProxy proxy) {
		super();
		this.proxy = proxy;
	}

	/**
	 * Method to automatically and properly close the wrapped BrowserMobProxy when not used anymore
	 */
	@Override
	public void close() {
		proxy.stop();
	}

	/**
	 * Getter to retrieve the wrapped proxy
	 * @return the wrapped BrowserMobProxy instance
	 */
	public BrowserMobProxy getProxy() {
		return proxy;
	}
}
