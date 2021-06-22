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

import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;

public class HttpResponse {

	private final byte[] responsePayload;
	private final List<BasicNameValuePair> responseHeaders;
	private final List<String> cookies;
	private final int status;

	public HttpResponse(byte[] responsePayload, List<BasicNameValuePair> responseHeaders, int status) throws Exception {
		super();
		this.responsePayload = responsePayload;
		this.responseHeaders = responseHeaders;
		this.cookies = buildCookies(responseHeaders);
		this.status = status;
	}

	public String getResponsePayload() {
		return new String(responsePayload);
	}

	public byte[] getResponsePayloadAsBytes() {
		return responsePayload;
	}

	public BasicNameValuePair getResponseHeader(String name) {
		BasicNameValuePair result;
		for (BasicNameValuePair header: responseHeaders) {
			if (header.getName().toLowerCase().equals(name.toLowerCase())) {
				return header;
			}
		}
		return null;
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
