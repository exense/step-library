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
package ch.exense.step.examples.http;

import java.net.InetAddress;

import java.net.UnknownHostException;

import org.apache.http.impl.conn.SystemDefaultDnsResolver;

/**
 * 
 * Custom DNS resolver to be used with the http client
 * 
 * Allows to resolve given host name to a specific IP for simulating load
 * balancing
 * 
 * across multiple entry servers
 * 
 * @author stephanda
 * 
 */
public class EntryServerDnsResolver extends SystemDefaultDnsResolver {

	private String hostWithCustomDns;
	private InetAddress[] targetIP;

	public EntryServerDnsResolver(String targetIP, String hostWithCustomDns) throws UnknownHostException {
		this.targetIP = new InetAddress[] { InetAddress.getByName(targetIP) };
		this.hostWithCustomDns = hostWithCustomDns.replaceFirst("https://", "").replaceFirst("http://", "");
	}

	@Override
	public InetAddress[] resolve(String host) throws UnknownHostException {
		// If hostname is the one to be resolved to specific IP
		if (host.contains(hostWithCustomDns)) {
			return targetIP;
			// else fallback to default DNS resolver
		} else {
			return super.resolve(host);
		}
	}

}