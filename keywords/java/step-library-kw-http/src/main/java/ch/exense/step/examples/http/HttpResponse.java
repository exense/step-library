package ch.exense.step.examples.http;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;

public class HttpResponse {

	private final String responsePayload;
	private final List<BasicNameValuePair> responseHeaders;
	private final List<String> cookies;
	private final int status;

	public HttpResponse(String responsePayload, List<BasicNameValuePair> responseHeaders, int status) throws Exception {
		super();
		this.responsePayload = responsePayload;
		this.responseHeaders = responseHeaders;
		this.cookies = buildCookies(responseHeaders);
		this.status = status;
	}

	public String getResponsePayload() {
		return responsePayload;
	}

	public List<BasicNameValuePair> getResponseHeaders() {
		return responseHeaders;
	}

	public List<String> getCookies() {
		return cookies;
	}

	public int getStatus() {
		return status;
	}

	protected List<String> buildCookies(List<BasicNameValuePair> headers) throws Exception {
		if (headers == null)
			throw new Exception("Header list not initialized. Did you execute a (successful) request?");
		List<String> cookies = new ArrayList<String>();
		for (BasicNameValuePair nvp : headers) {
			if (nvp.getName().trim().contains("Set-Cookie"))
				cookies.add(nvp.getValue());
		}
		return cookies;
	}
}
