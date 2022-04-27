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

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HttpClient {

	private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

	protected CloseableHttpClient client;
	protected HttpRequestBase request = null;
	protected HttpClientContext context = null;
	protected String targetIP = "";

	private RequestConfig requestConfig;

	public RequestConfig getRequestConfig() {
		return requestConfig;
	}

	/**
	 * Create a client and its context with provided SSL information, auth cache and
	 * optionally a custom DNS resolved for load balancing
	 * 
	 * @param timeoutInMs
	 * @param jksPath
	 * @param password
	 * @param targetIP
	 * @param hostWithCustomDns
	 * @param basicAuthHostScheme
	 * @param basicAuthHost
	 * @param basicAuthPort
	 * @param basicAuthUser
	 * @param basicAuthPassword
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws IOException
	 * @throws UnrecoverableKeyException
	 * @throws KeyManagementException
	 */
	public HttpClient(int timeoutInMs, String jksPath, String password, String targetIP, String hostWithCustomDns, String basicAuthHostScheme,
			String basicAuthHost, int basicAuthPort, String basicAuthUser, String basicAuthPassword)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
			UnrecoverableKeyException, KeyManagementException {
		
		//Logger LOG = (Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http");
		//((ch.qos.logback.classic.Logger) LOG).setLevel(Level.DEBUG);
		
		HttpClientBuilder httpClientBuilder = HttpClients.custom();
		SSLContext sc = setSSLContext(jksPath, password);
		httpClientBuilder.setSSLContext(sc);
		// If provided add a custom DNS resolver which will resolve the
		// 'hostWithCustomDns' to the provided 'targetIP'
		if (targetIP != null && !targetIP.isEmpty()) {
			this.targetIP = targetIP;
			httpClientBuilder.setDnsResolver(new EntryServerDnsResolver(targetIP, hostWithCustomDns));
		}
		
		// Create the http context and init Auth cache
		context = HttpClientContext.create();
		AuthCache authCache = new BasicAuthCache();
		
		/*basic auth (auth provided upon challenge) */
		 CredentialsProvider provider = null;
		 if (basicAuthUser != null && basicAuthPassword != null) {
			 provider = new BasicCredentialsProvider();
			 UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(basicAuthUser, basicAuthPassword);
			 provider.setCredentials(AuthScope.ANY, credentials);
		 }
		 
		/*if Preemptive basic auth set it in the http context*/
		if (basicAuthHostScheme != null && basicAuthHost != null && basicAuthPort > 0 && provider != null) {
			HttpHost targetHost = new HttpHost(basicAuthHost, basicAuthPort, basicAuthHostScheme);
			authCache.put(targetHost, new BasicScheme());
			context.setCredentialsProvider(provider);
		//else if basic authentication set it as defeult in the client
		} else if (provider != null) {
			httpClientBuilder.setDefaultCredentialsProvider(provider);
		}
		
		requestConfig = RequestConfig.custom()
				  .setConnectTimeout(timeoutInMs)
				  .setConnectionRequestTimeout(timeoutInMs)
				  .setSocketTimeout(timeoutInMs).build();
		
		//Build the client
		this.client = httpClientBuilder.setDefaultRequestConfig(requestConfig).build();
		
		// context.setCredentialsProvider(credsProvider);
		// Add AuthCache to the execution context
		context.setAuthCache(authCache);
	}
	
	private SSLContext setSSLContext(String jksPath, String password) throws KeyStoreException, NoSuchAlgorithmException,
			CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {
		KeyManager[] keyManagers;
		if (jksPath != null && password != null) {
			FileInputStream instream = new FileInputStream(new File(jksPath));
			KeyStore keyStore = KeyStore.getInstance("jks");
			keyStore.load((InputStream) instream, password.toCharArray());

			KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyFactory.init(keyStore, password.toCharArray());
			keyManagers = keyFactory.getKeyManagers();
		} else {
			keyManagers = new KeyManager[0];
		}

		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(keyManagers, trustAllCerts, new SecureRandom());

		return sc;
		//this.client = HttpClients.custom().setSSLContext(sc).build();
	}

	protected byte[] readResponse(CloseableHttpResponse response) throws UnsupportedOperationException, IOException {
		if (response.getEntity() != null) {
			int length = (int) response.getEntity().getContentLength();
			if (length > 0 ) {
				InputStream stream = response.getEntity().getContent();
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				byte[] result = new byte[length];
				int nRead;
				while ((nRead = stream.read(result, 0, result.length)) != -1) {
					buffer.write(result, 0, nRead);
				}
				return buffer.toByteArray();
			} else {
				return EntityUtils.toString(response.getEntity()).getBytes();
			}
		} else {
			return null;
		}
	}

	private List<BasicNameValuePair> toNameValues(List<Header> headerList) {
		return headerList.stream().map(h->new BasicNameValuePair(h.getName(), h.getValue())).collect(Collectors.toList());
	}

	public HttpResponse executeRequestInContext(HttpRequest request)
			throws ClientProtocolException, IOException, Exception {
		request.logDebugInfo();
		try(CloseableHttpResponse httpResponse = this.client.execute(request, context)) {
			int status = httpResponse.getStatusLine().getStatusCode();
			List<BasicNameValuePair> responseHeaders = toNameValues(Arrays.asList(httpResponse.getAllHeaders()));
			byte[] response = readResponse(httpResponse);
			return new HttpResponse(response, responseHeaders, status);
		}
	}

	public void close() {
		try {
			if (this.client != null)
				this.client.close();
		} catch (IOException e) {
			logger.error("Could not close http client.", e);
		}
	}

	/**
	* Extract the list of all cookies in the current client store
	*/
	public List<String> getCookiesFromStore() {
		List<String> cookieNames = new ArrayList<String>();
		for (Cookie cookie : context.getCookieStore().getCookies()) {
			cookieNames.add(cookie.getName() + "=" + cookie.getValue() + "; Domain=" + cookie.getDomain() + "; Path="
					+ cookie.getPath());
		}
		return cookieNames;
	}
	
	public void addCookie(String name, String value, String domain, String path) {
		//Cookie store only created after first request
		CookieStore cookieStore = context.getCookieStore();
		if (cookieStore == null) {
			cookieStore = new BasicCookieStore();
			context.setCookieStore(cookieStore);
		}
		BasicClientCookie cookie = new BasicClientCookie(name, value);
		cookie.setDomain(domain);
		cookie.setPath(path);	
		cookieStore.addCookie(cookie);
	}

	public String getTargetIP() {
		return this.targetIP;
	}													
}
