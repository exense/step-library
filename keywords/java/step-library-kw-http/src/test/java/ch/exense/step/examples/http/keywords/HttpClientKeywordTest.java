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

import org.junit.Assert;
import org.junit.Test;
import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

import javax.json.Json;
import javax.json.JsonObject;

import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class HttpClientKeywordTest {

	private final ExecutionContext ctx = KeywordRunner
			.getExecutionContext(Map.of("postman_Password", "password"), HttpClientKeyword.class);
	
	@Test
	public void simpleHttpGetRequest() throws Exception {
		String input = Json.createObjectBuilder().add("URL", "https://www.google.ch/").build().toString();
		Output<JsonObject> output = ctx.run("HttpRequest", input);
		assertEquals(output.getPayload().getString("StatusCode"),"200");
	}

	@Test
	public void simpleHttpGetRequestRedirect() throws Exception {
		String input = Json.createObjectBuilder().add("URL", "https://lh3.google.com/u/0/ogw/ADea4I4hEDIRpLqTIrBziyxcym7YCTNtDLFduNiCVnc=s128-b16-cc-rp-mo")
				.add("Method", "GET")
				.add("Name", "Google 200")
				.add("Enable_Redirect", true)
				.build().toString();
		Output<JsonObject> output = ctx.run("HttpRequest", input);
		assertEquals("200", output.getPayload().getString("StatusCode"));

		input = Json.createObjectBuilder().add("URL", "https://lh3.google.com/u/0/ogw/ADea4I4hEDIRpLqTIrBziyxcym7YCTNtDLFduNiCVnc=s128-b16-cc-rp-mo")
				.add("Method", "GET")
				.add("Name", "Google 302")
				.add("Enable_Redirect", false)
				.build().toString();
		output = ctx.run("HttpRequest", input);
		assertEquals("302", output.getPayload().getString("StatusCode"));

		input = Json.createObjectBuilder().add("URL", "https://lh3.google.com/u/0/ogw/ADea4I4hEDIRpLqTIrBziyxcym7YCTNtDLFduNiCVnc=s128-b16-cc-rp-mo")
				.add("Method", "GET")
				.add("Name", "Google 200")
				.build().toString();
		output = ctx.run("HttpRequest", input);
		assertEquals("200", output.getPayload().getString("StatusCode"));
	}

	@Test
	public void simpleHttpGetRequestTimeoutWithExplicitClientInit() throws Exception {
		
		String inputInitHttpClient = Json.createObjectBuilder().add("TimeoutInMs", "1").build().toString();
		ctx.run("InitHttpClient", inputInitHttpClient);
		
		String inputHttpRequest = Json.createObjectBuilder().add("URL", "https://www.google.ch/").build().toString();

		Exception actualException = null;
		try { 
			ctx.run("HttpRequest", inputHttpRequest);
		} catch (Exception e) {
			actualException = e;
		}
		Assert.assertNotNull(actualException);
		assertTrue(actualException.getMessage().contains("timed out"));
	}

	@Test
	public void simpleHttpGetRequestWithExplicitClientInit() throws Exception {
		// Open http client
		Output<JsonObject> output = ctx.run("InitHttpClient", "{}");
		System.out.println(output.getPayload());

		// HTTP GET www.google.ch
		String input = Json.createObjectBuilder().add("URL", "https://www.google.ch/")
			.add("Method", "GET")
			.add("Name", "Google home")
			.add("Check_Title", "<title>Google</title>")
			.add("Extract_Title", "<title>(.+?)</title>").build().toString();
		output = ctx.run("HttpRequest", input);

		assertEquals(output.getPayload().getString("StatusCode"),"200");
		assertTrue(output.getPayload().getBoolean("Check_Title"));
		assertEquals(output.getPayload().getString("Extract_Title"),"Google");

		ctx.run("CloseHttpClient", "{}");
	}
	
	@Test
	public void simpleHttpGetWithHeaders() throws Exception {
		String input = Json.createObjectBuilder()
				.add("URL", "https://postman-echo.com/headers")
				.add("Header_myHeader", "MyHeaderValue")
				.build().toString();
		Output<JsonObject> output = ctx.run("HttpRequest", input);
		assertTrue(output.getPayload().getString("Response").contains("\"myheader\": \"MyHeaderValue\""));
	}
	
	@Test
	public void getCookies() throws Exception {
		String input = Json.createObjectBuilder()
				.add("URL", "https://www.google.ch/")
				.add("Method", "GET")
				.build().toString();
		Output<JsonObject> output = ctx.run("HttpRequest", input);
		assertEquals(output.getPayload().getString("StatusCode"),"200");
		
		output = ctx.run("GetCookies", input);
		assertTrue(output.getPayload().getString("Cookies").contains("Domain=google.ch; Path=/"));
	}
	
	@Test
	public void httpPostData() throws Exception {
		// Open http client
		Output<JsonObject> output = ctx.run("InitHttpClient", "{}");

		String input = Json.createObjectBuilder().add("URL", "https://postman-echo.com/post")
			.add("Method", "POST")
			.add("Data", "My content")
			.build().toString();
		output = ctx.run("HttpRequest", input);

		assertEquals(output.getPayload().getString("StatusCode"),"200");
		assertTrue(output.getPayload().getString("Response").contains("My content"));
	}
	
	@Test
	public void httpPostFormData() throws Exception {
		String input = Json.createObjectBuilder().add("URL", "https://postman-echo.com/post")
			.add("Method", "POST")
			.add("FormData_myFormInput1", "My form value 1")
			.build().toString();
		Output<JsonObject> output = ctx.run("HttpRequest", input);

		assertEquals(output.getPayload().getString("StatusCode"),"200");
		assertTrue(output.getPayload().getString("Response").contains("My form value 1"));
	}

	@Test
	public void httpPostMultiPartFormData() throws Exception {
		String input = Json.createObjectBuilder().add("URL", "https://postman-echo.com/post")
				.add("Method", "POST")
				.add("MultiPartFormData_input1", "My form value 1")
				.add("MultiPartFormData_input2", "My form value 2")
				.add("MultiPartFormData_filepath", Paths.get(this.getClass().getResource("/test.txt").toURI()).toString())
				.build().toString();

		Output<JsonObject> output = ctx.run("HttpRequest", input);

		assertEquals(output.getPayload().getString("StatusCode"),"200");
		assertTrue(output.getPayload().getString("Response").contains("My form value 1"));
		assertTrue(output.getPayload().getString("Response").contains("My form value 2"));
		assertTrue(output.getPayload().getString("Response").contains("files"));
		assertTrue(output.getPayload().getString("Response").contains("test.txt"));
	}
	
	@Test
	public void httpPut() throws Exception {
		// Open http client
		Output<JsonObject> output = ctx.run("InitHttpClient", "{}");

		String input = Json.createObjectBuilder().add("URL", "https://postman-echo.com/put")
				.add("Method", "PUT")
				.add("Data", "My content")
				.build().toString();
		output = ctx.run("HttpRequest", input);

		assertEquals(output.getPayload().getString("StatusCode"),"200");
		assertTrue(output.getPayload().getString("Response").contains("My content"));
	}
	
	@Test
	public void httpDelete() throws Exception {
		// Open http client
		Output<JsonObject> output = ctx.run("InitHttpClient", "{}");

		String input = Json.createObjectBuilder().add("URL", "https://postman-echo.com/delete")
			.add("Method", "DELETE")
			.build().toString();
		output = ctx.run("HttpRequest", input);

		assertEquals(output.getPayload().getString("StatusCode"),"200");
	}
	
	@Test
	public void basicAuth() throws Exception {
		String input = Json.createObjectBuilder()
				.add("BasicAuthUser", "postman")
				.build().toString();
		Output<JsonObject> output = ctx.run("InitHttpClient", input);

		input = Json.createObjectBuilder().add("URL", "https://postman-echo.com/basic-auth")
			.add("Method", "GET")
			.build().toString();
		output = ctx.run("HttpRequest", input);

		assertEquals(output.getPayload().getString("StatusCode"),"200");
	}
	
	@Test
	public void basicAuthWithoutExplicitClientInit() throws Exception {
		String input = Json.createObjectBuilder().add("URL", "https://postman-echo.com/basic-auth")
			.add("Method", "GET")
			.add("BasicAuthUser", "postman")
			.build().toString();
		Output<JsonObject> output = ctx.run("HttpRequest", input);

		assertEquals(output.getPayload().getString("StatusCode"),"200");
	}
	
	@Test
	public void customDnsResolver() throws Exception {
		// Open http client
		String input = Json.createObjectBuilder()
				.add("CustomDnsResolverTargetIP", "0.0.0.0")
				.add("CustomDnsResolverHostWithCustomDns", "mycustomhost.ch")
				.build().toString();
		ctx.run("InitHttpClient", input);

		input = Json.createObjectBuilder().add("URL", "https://mycustomhost.ch/")
			.add("Method", "GET").build().toString();
		
		Exception actual = null;
		try {
			ctx.run("HttpRequest", input);
		} catch(Exception e) {
			actual = e;
		}
        assert actual != null;
        assertTrue(actual.getMessage().startsWith("Connect to mycustomhost.ch:443 [/0.0.0.0] failed: Connection refused"));
	}
}
