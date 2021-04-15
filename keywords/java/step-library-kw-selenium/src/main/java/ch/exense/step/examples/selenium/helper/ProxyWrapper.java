/*******************************************************************************
 * Copyright 2021 exense GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
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
