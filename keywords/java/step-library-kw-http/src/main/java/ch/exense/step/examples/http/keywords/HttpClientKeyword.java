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
package ch.exense.step.examples.http.keywords;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.exense.step.library.commons.AbstractEnhancedKeyword;
import ch.exense.step.library.commons.BusinessException;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ch.exense.step.examples.http.HttpClient;
import ch.exense.step.examples.http.HttpRequest;
import ch.exense.step.examples.http.HttpResponse;
import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.Keyword;

public class HttpClientKeyword extends AbstractEnhancedKeyword {

	private static final String HTTP_CLIENT = "httpClient";
	private static final String DATA = "Data";
	public static final String HEADER_PREFIX = "Header_";
	public static final String PARAM_PREFIX = "FormData_";
	public static final String EXTRACT_PREFIX = "Extract_";
	public static final String CHECK_PREFIX = "Check_";

	/**
	 * step Keyword to init an Apache HTTP client the client will be placed in the
	 * current step session
	 * 
	 * Keyword inputs BasicAuthUser: set basic authenticate user name (require
	 * password) BasicAuthPassword: set basic authentication password BasicAuthHost:
	 * enable preemptive authentication (require the 5 bascic_auth fields)
	 * BasicAuthHostScheme: target host scheme (i.e. https) BasicAuthPort: target
	 * host port number KeyStorePath (optional) KeyStorePassword (mandatory is
	 * keyStorePath is provided) CustomDnsResolverTargetIP: enable a custom DNS
	 * resolver, requires hostWithCustomDns CustomDnsResolverHostWithCustomDns:
	 * requests to this host will be resolved to the "targetIP"
	 * 
	 * @throws IOException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws KeyManagementException
	 * @throws UnrecoverableKeyException
	 * 
	 */
	@Keyword(schema = "{\"properties\":{"
			+ "\"BasicAuthUser\":{\"type\":\"string\"},"
			+ "\"BasicAuthPassword\":{\"type\":\"string\"},"
			+ "\"BasicAuthHost\":{\"type\":\"string\"},"
			+ "\"BasicAuthHostScheme\":{\"type\":\"string\"}," 
			+ "\"BasicAuthPort\":{\"type\":\"string\"},"
			+ "\"KeyStorePath\":{\"type\":\"string\"},"
			+ "\"KeyStorePassword\":{\"type\":\"string\"},"
			+ "\"TimeoutInMs\":{\"type\":\"string\"},"
			+ "\"CustomDnsResolverTargetIP\":{\"type\":\"string\"},"
			+ "\"CustomDnsResolverHostWithCustomDns\":{\"type\":\"string\"}"
			+ "},\"required\":[]}", properties = { "" })
	public void InitHttpClient() throws UnrecoverableKeyException, KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException {
		HttpClient httpClient = null;
		String basicAuthUser = null;
		String basicAuthPassword = null;
		String basicAuthHostScheme = null;
		String basicAuthHost = null;
		int basicAuthPort = 0;
		String keyStorePath = null;
		String keyStorePassword = null;
		String customDnsResolverTargetIP = null;
		String customDnsResolverHostWithCustomDns = null;

		if (input.containsKey("KeyStorePath")) {
			if (!input.containsKey("KeyStorePassword")) {
				throw new BusinessException("KeyStorePath provided without password.");
			} else {
				keyStorePath = input.getString("KeyStorePath");
				keyStorePassword = input.getString("KeyStorePassword");
			}
		}
		if (input.containsKey("CustomDnsResolverTargetIP")) {
			if (!input.containsKey("CustomDnsResolverHostWithCustomDns")) {
				throw new BusinessException(
						"'CustomDnsResolverTargetIP' provided wihtout 'CustomDnsResolverHostWithCustomDns'.");
			} else {
				customDnsResolverTargetIP = input.getString("CustomDnsResolverTargetIP");
				customDnsResolverHostWithCustomDns = input.getString("CustomDnsResolverHostWithCustomDns");
			}
		}
		if (input.containsKey("BasicAuthUser")) {
			if (!input.containsKey("BasicAuthPassword")) {
				throw new BusinessException("'BasicAuthPassword' not set");
			} else {
				basicAuthUser = input.getString("BasicAuthUser");
				basicAuthPassword = input.getString("BasicAuthPassword");
				if (input.containsKey("BasicAuthHost")) {
					if (!input.containsKey("BasicAuthPort") || !input.containsKey("BasicAuthHostScheme")) {
						throw new BusinessException(
								"BasicAuthHost provided without 'BasicAuthPort' or 'BasicAuthHostScheme'.");
					} else {
						basicAuthHost = input.getString("BasicAuthHost");
						basicAuthHostScheme = input.getString("BasicAuthHostScheme");
						basicAuthPort = Integer.parseInt(input.getString("BasicAuthPort"));
					}
				}
			}
		}
		int timeoutInMs = Integer.parseInt(input.getString("TimeoutInMs", "60000"));
		httpClient = new HttpClient(timeoutInMs, keyStorePath, keyStorePassword, customDnsResolverTargetIP,
				customDnsResolverHostWithCustomDns, basicAuthHostScheme, basicAuthHost, basicAuthPort, basicAuthUser,
				basicAuthPassword);

		getSession().put(HTTP_CLIENT, httpClient);
	}

	/**
	 * step Keyword to close the Apache HTTP clients stored in the step session
	 */
	@Keyword
	public void CloseHttpClient() {
		HttpClient httpClient = getHttpClientFromSession();
		httpClient.close();
	}

	/**
	 * step Keyword to execute one HTTP request
	 * 
	 * Keyword inputs: Name (optional): name of the request used for RTM
	 * measurements (default: URL) URL: the URL of the request Method: GET, POST
	 * (other methods might be supported) Header_* (optional): naming convention to
	 * pass header parameters, ex: key: Header_accept, value: application/json,
	 * text/plain key: Header_accept-language, value:
	 * en-US,en;q=0.9,fr;q=0.8,de;q=0.7 Data (optional): body payload as string
	 * (used only for Post method and only if param_* are not set) FormData_*
	 * (optional): naming convention to pass request parameters, ex: key:
	 * FormData_user, value: myname key: FormData_password, value: mypassword
	 * Extract_*: naming convention to pass content check strings, ex: key:
	 * extract_myId, value: regexp as string with one group Check_* (optional):
	 * naming convention to pass content check strings, ex: key: check_pageTitle,
	 * value: 'my web site title' ReturnResponse: default true
	 * 
	 * 
	 * Keyword output StatusCode: Request status code Headers: headers Cookies:
	 * cookies set in this response's header Response: response payload (depends on
	 * input returnResponsePayload, default true) Extract_*: extracted fields
	 * Check_*: content checks result (true if found)
	 * 
	 * @throws Exception
	 */
	@Keyword(schema = "{\"properties\":{" 
			+ "\"URL\":{\"type\":\"string\"},"
			+ "\"Method\":{\"type\":\"string\"},"
			+ "\"Header_myheader1\":{\"type\":\"string\"},"
			+ "\"FormData_myFormData1\":{\"type\":\"string\"},"
			+ "\"Extract_myField1ToExtract1\":{\"type\":\"string\"},"
			+ "\"Check_myFieldToChec1\":{\"type\":\"string\"},"
			+ "\"Data\":{\"type\":\"string\"},"
			+ "\"ReturnResponse\":{\"type\":\"boolean\"},"
			+ "\"SaveResponseAsAttachment\":{\"type\":\"boolean\"},"
			+ "\"Name\":{\"type\":\"string\"}"
			+ "}, \"required\":[\"URL\"]}", properties = {})
	public void HttpRequest() throws Exception {
		String url = input.getString("URL");
		String method = input.getString("Method", "GET");
		String requestName = input.getString("Name", url);
		boolean returnResponse = input.getBoolean("ReturnResponse", true);
		boolean saveAsAttachment = input.getBoolean("SaveResponseAsAttachment", false);

		HashMap<String, String> headers = new HashMap<String, String>();
		// Extract all dynamic and optional inputs
		String payload = "";
		List<NameValuePair> formData = new ArrayList<NameValuePair>();
		Map<String, Pattern> extractRegexp = new HashMap<String, Pattern>();
		Map<String, String> textChecks = headers;
		for (String key : input.keySet()) {
			try {
				String value = input.getString(key);
				if (key.startsWith(HEADER_PREFIX)) {
					String name = key.substring(HEADER_PREFIX.length());
					headers.put(name, value);
				} else if (key.startsWith(PARAM_PREFIX)) {
					String name = key.substring(PARAM_PREFIX.length());
					formData.add(new BasicNameValuePair(name, value));
				} else if (key.startsWith(EXTRACT_PREFIX)) {
					String name = key.substring(EXTRACT_PREFIX.length());
					extractRegexp.put(name, Pattern.compile(value, Pattern.DOTALL));
				} else if (key.startsWith(CHECK_PREFIX)) {
					String name = key.substring(CHECK_PREFIX.length());
					textChecks.put(name, value);
				} else if (key.equals(DATA)) {
					payload = input.getString(DATA);
				}
			} catch(ClassCastException e) {
				logger.warn("Input " + key + " is not a String but a " + input.get(key).getClass().getSimpleName() + " so it will not be parsed");
			}			
		}

		HttpClient httpClient = getHttpClientFromSession();
		// Init the client if not available in the session
		if (httpClient == null) {
			InitHttpClient();
			httpClient = getHttpClientFromSession();
		}

		// Init request
		HttpRequest request = new HttpRequest(url, method);
		headers.forEach((k, v) -> request.appendHeader(k, v));

		if (!formData.isEmpty()) {
			request.setParams(formData);
		} else if (!payload.isEmpty()) {
			request.setRowPayload(payload);
		}

		Map<String, Object> measureData = new HashMap<>();
		measureData.put("DestIP", httpClient.getTargetIP());

		HttpResponse httpResponse;
		try {
			output.startMeasure(requestName);
			httpResponse = httpClient.executeRequestInContext(request);
		} catch (Exception e) {
			// stop network measure
			measureData.put("ExceptionType", e.getClass().toString());
			measureData.put("ExceptionMessage", (e.getMessage() == null) ? "null" : e.getMessage());
			throw e;
		} finally {
			output.stopMeasure(measureData);
		}

		output.add("StatusCode", Integer.toString(httpResponse.getStatus()));
		output.add("Headers", httpResponse.getResponseHeaders().toString());
		output.add("Cookies", httpResponse.getCookies().toString());
		if (returnResponse) {
			if (saveAsAttachment) {
				String name = "attachment.data";

				BasicNameValuePair result;
				if ((result = httpResponse.getResponseHeader("Content-Disposition"))!=null) {
					Matcher pattern_1 = Pattern.compile(".*filename ?= ?([^\"]+?);?.*").matcher(result.getValue());
					if(pattern_1.find()) {
						name = pattern_1.group(1);
					}
					Matcher pattern_2 = Pattern.compile(".*filename ?= ?\"([^\"]+?)\".*").matcher(result.getValue());
					if(pattern_2.find()) {
						name = pattern_2.group(1);
					}
				}
				output.addAttachment(AttachmentHelper.
						generateAttachmentFromByteArray(httpResponse.getResponsePayloadAsBytes(),name));
			} else {
				output.add("Response", httpResponse.getResponsePayload());
			}
		}

		// extract all fields
		for (String key : extractRegexp.keySet()) {
			String value = "";
			Matcher m = extractRegexp.get(key).matcher(httpResponse.getResponsePayload());
			if (m.find()) {
				// return 1st group if defined or full match
				try {
					value = m.group(1);
				} catch (Exception e) {
					value = m.group(0);
				}
			}
			output.add(EXTRACT_PREFIX.concat(key), value);
		}

		// do all checks
		for (String key : textChecks.keySet()) {
			output.add(CHECK_PREFIX.concat(key), httpResponse.getResponsePayload().contains(textChecks.get(key)));
		}
	}

	protected HttpClient getHttpClientFromSession() {
		return (HttpClient) getSession().get(HTTP_CLIENT);
	}

	@Keyword
	public void GetCookies() {
		HttpClient httpClient = getHttpClientFromSession();
		output.add("Cookies", httpClient.getCookiesFromStore().toString());
	}

	@Keyword(schema = "{\"properties\":{"
			+ "\"Name\":{\"type\":\"string\"},"
			+ "\"Value\":{\"type\":\"string\"},"
			+ "\"Domain\":{\"type\":\"string\"},"
			+ "\"Path\":{\"type\":\"string\"}"
			+ "}, \"required\":[\"cookies\",\"value\",\"domain\",\"path\"]}", properties = {})
	public void AddCookie() {
		HttpClient httpClient = getHttpClientFromSession();
		String name = input.getString("Name");
		String value = input.getString("Value");
		String domain = input.getString("Domain");
		String path = input.getString("Path");
		httpClient.addCookie(name, value, domain, path);
		GetCookies();
	}

	@Keyword
	public void EnableProxy() {
		String nonProxyHosts = input.getString("nonProxyHosts");
		String proxyHost = input.getString("proxyHost");
		String proxyPort = input.getString("proxyPort");
		System.setProperty("http.proxyHost", proxyHost);
		System.setProperty("http.proxyPort", proxyPort);
		System.setProperty("http.nonProxyHosts", nonProxyHosts);
		System.setProperty("https.proxyHost", proxyHost);
		System.setProperty("https.proxyPort", proxyPort);
		System.setProperty("https.nonProxyHosts", nonProxyHosts);
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	@Keyword
	public void DisableProxy() {
		System.setProperty("http.proxyHost", "");
		System.setProperty("http.proxyPort", "");
		System.setProperty("http.nonProxyHosts", "");
		System.setProperty("https.proxyHost", "");
		System.setProperty("https.proxyPort", "");
		System.setProperty("https.nonProxyHosts", "");
		System.setProperty("java.net.preferIPv4Stack", "false");
	}

	@Keyword
	public void ShowProxySettings() {
		output.add("http.proxyHost", System.getProperty("http.proxyHost"));
		output.add("http.proxyPort", System.getProperty("http.proxyPort"));
		output.add("https.proxyHost", System.getProperty("https.proxyHost"));
		output.add("https.proxyPort", System.getProperty("https.proxyPort"));
		output.add("java.net.preferIPv4Stack", System.getProperty("java.net.preferIPv4Stack"));
	}
}
