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

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class HttpRequest extends HttpEntityEnclosingRequestBase {

	private static final Logger logger = LoggerFactory.getLogger(HttpRequest.class);
	String method = HttpGet.METHOD_NAME;
	String body = "";

	public HttpRequest(String uri, String method) {
		this.method = method;
		setURI(URI.create(uri));
	}

	@Override
	public String getMethod() {
		return method;
	}

	public HttpRequest appendHeader(String key, String value) {
		this.addHeader(key, value);
		return this;
	}

	public HttpRequest setHeaders(JsonObject headers) {
		for (String key : headers.keySet()) {
			this.appendHeader(key, headers.getString(key));
		}
		return this;
	}

	public HttpRequest setParams(List<NameValuePair> params) throws UnsupportedEncodingException {
		setEntity(new UrlEncodedFormEntity(params));
		return this;
	}

	public void setMultiPartParams(List<NameValuePair> multiPartFormData) {
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

		multiPartFormData.forEach(nvp -> {
			if(nvp.getName().equals("filepath")) {
				File fileToUpload = new File(nvp.getValue());
				FileBody fileBody = null;
				try {
					fileBody = new FileBody(fileToUpload, ContentType.parse(Files.probeContentType(Paths.get(fileToUpload.toURI()))));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				builder.addPart("file", fileBody);
			} else {
				builder.addPart(nvp.getName(), new StringBody(nvp.getValue(), ContentType.MULTIPART_FORM_DATA));
			}
		});
		HttpEntity entity = builder.build();
		setEntity(entity);
	}

	public HttpRequest setRowPayload(String payload) {
		body = payload;
		setEntity(new StringEntity(payload, ContentType.create("text/plain", "UTF-8")));
		return this;
	}

	public void logDebugInfo() {
		if(logger.isDebugEnabled()) {
			logger.debug("Request URI: " + (this.getURI()));
			logger.debug("Request headers: " + this.getAllHeaders());
			logger.debug("Request method: " + this.getMethod());
			if (method.equals(HttpPost.METHOD_NAME)) {
				logger.debug("Request body: " + body);
			}
		}
	}
}
