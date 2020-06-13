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

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ch.exense.step.examples.common.helper.AbstractEnhancedKeyword;
import ch.exense.step.examples.common.helper.BusinessException;
import ch.exense.step.examples.http.HttpClient;
import ch.exense.step.examples.http.HttpRequest;
import ch.exense.step.examples.http.HttpResponse;
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
					if (!input.containsKey("BasicAuthPort") || !input.containsKey("basicAuthHostScheme")) {
						throw new BusinessException(
								"BasicAuthHost provided wihtout 'BasicAuthPort' or 'basicAuthHostScheme'.");
					} else {
						basicAuthHost = input.getString("BasicAuthHost");
						basicAuthHostScheme = input.getString("basicAuthHostScheme");
						basicAuthPort = input.getInt("basicAuthPort");
					}
				}
			}
		}
		httpClient = new HttpClient(keyStorePath, keyStorePassword, customDnsResolverTargetIP,
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
			+ "\"Data\":{\"type\":\"string\"},"
			+ "\"Name\":{\"type\":\"string\"}"
			+ "}, \"required\":[\"URL\"]}", properties = {})
	public void HttpRequest() throws Exception {
		String url = input.getString("URL");
		String method = input.getString("Method", "GET");
		String requestName = input.getString("Name", url);
		boolean returnResponse = input.getBoolean("ReturnResponse", true);

		HashMap<String, String> headers = new HashMap<String, String>();
		// Extract all dynamic and optional inputs
		String payload = "";
		List<NameValuePair> formData = new ArrayList<NameValuePair>();
		Map<String, Pattern> extractRegexp = new HashMap<String, Pattern>();
		Map<String, String> textChecks = headers;
		for (String key : input.keySet()) {
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

		output.add("StatusCode", httpResponse.getStatus());
		output.add("Headers", httpResponse.getResponseHeaders().toString());
		output.add("Cookies", httpResponse.getCookies().toString());
		if (returnResponse) {
			output.add("Response", httpResponse.getResponsePayload());
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
