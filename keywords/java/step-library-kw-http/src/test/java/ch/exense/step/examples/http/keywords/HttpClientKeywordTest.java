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
import step.core.reports.Error;
import step.core.reports.ErrorType;
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
	public void hostNotFoundErrorHttpGetRequest() throws Exception {
		String input = Json.createObjectBuilder().add("URL", "https://this.is.not.an.existing.host/").build().toString();
		ctx.setThrowExceptionOnError(false);
		try {
			Output<JsonObject> output = ctx.run("HttpRequest", input);
			assertEquals(output.getError().getType(), ErrorType.BUSINESS);
			assertTrue(output.getError().getMsg().startsWith("Unknown host:"));
		} finally {
			ctx.setThrowExceptionOnError(true);
		}
	}

	@Test
	public void sslErrorHttpGetRequest() throws Exception {
		String input = Json.createObjectBuilder().add("URL", "https://noexisting.stepcloud.ch/").build().toString();
		ctx.setThrowExceptionOnError(false);
		try {
			Output<JsonObject> output = ctx.run("HttpRequest", input);
			assertEquals(output.getError().getType(), ErrorType.BUSINESS);
			assertEquals(output.getError().getMsg(), "SSL error: Certificate for <noexisting.stepcloud.ch> doesn't match any of the subject alternative names: [ingress.local]");
		} finally {
			ctx.setThrowExceptionOnError(true);
		}
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

		JsonObject checks = Json.createObjectBuilder().add("TitleCheck", "<title>Google</title>").build();
		JsonObject extractRegexp = Json.createObjectBuilder().add("Title", "<title>(.+?)</title>").build();
		// HTTP GET www.google.ch
		String input = Json.createObjectBuilder().add("URL", "https://www.google.ch/")
			.add("Method", "GET")
			.add("Name", "Google home")
			.add("Checks", checks)
			.add("ExtractRegexp", extractRegexp)
				.build().toString();
		output = ctx.run("HttpRequest", input);

		assertEquals(output.getPayload().getString("StatusCode"),"200");
		assertEquals(output.getPayload().getString("Title"),"Google");

		ctx.run("CloseHttpClient", "{}");
	}

	@Test
	public void checkAndEtractErrors() throws Exception {
		ctx.setThrowExceptionOnError(false);

		JsonObject checks = Json.createObjectBuilder().add("TitleCheck", "<title>Something</title>").build();
		JsonObject extractRegexp = Json.createObjectBuilder().add("Title", "<dummy>(.+?)</dummy>").build();
		JsonObject extractJsonPath = Json.createObjectBuilder().add("myLong", "data.nested.myLong").build();

		// HTTP GET www.google.ch
		String input = Json.createObjectBuilder().add("URL", "https://www.google.ch/")
				.add("Method", "GET")
				.add("Name", "Google home")
				.add("Checks", checks)
				.add("ExtractRegexp", extractRegexp)
				.add("ExtractJsonPath", extractJsonPath)
				.build().toString();
		Output<JsonObject> output = ctx.run("HttpRequest", input);

		assertEquals(output.getPayload().getString("StatusCode"),"200");
		Error error = output.getError();
		assertEquals(ErrorType.BUSINESS, error.getType());
		assertEquals("ExtractRegexp 'Title' with pattern '<dummy>(.+?)</dummy>' has no match, ExtractJsonPath 'myLong' with JSON path 'data.nested.myLong' was not found, Content check 'TitleCheck' with text '<title>Something</title>' was not found",
				error.getMsg());


		ctx.run("CloseHttpClient", "{}");
	}
	
	@Test
	public void simpleHttpGetWithHeaders() throws Exception {
		JsonObject headers = Json.createObjectBuilder()
				.add("myHeader", "MyHeaderValue").build();
		String input = Json.createObjectBuilder()
				.add("URL", "https://postman-echo.com/headers")
				.add("Headers", headers)
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
		JsonObject formData = Json.createObjectBuilder().add("myFormInput1", "My form value 1").build();
		String input = Json.createObjectBuilder().add("URL", "https://postman-echo.com/post")
			.add("Method", "POST")
			.add("FormData", formData)
			.build().toString();
		Output<JsonObject> output = ctx.run("HttpRequest", input);

		assertEquals(output.getPayload().getString("StatusCode"),"200");
		assertTrue(output.getPayload().getString("Response").contains("My form value 1"));
	}

	@Test
	public void httpJsonResponse() throws Exception {
		JsonObject headers = Json.createObjectBuilder().add("Content-Type", "application/json").build();
		String requestBody = Json.createObjectBuilder().add(
						"nested", Json.createObjectBuilder()
								.add("myLong", 123L)
								.add("myString", "some value")
								.add("myBoolean", true)
								.build())
				.build().toString();
		JsonObject extractJsonPath = Json.createObjectBuilder()
				.add("myLong", "data.nested.myLong")
				.add("myString", "$.data.nested.myString")
				.add("myBoolean", "$.data.nested.myBoolean")
				.add("nested", "$.data.nested")
				.build();
		String input = Json.createObjectBuilder().add("URL", "https://postman-echo.com/post")
				.add("Method", "POST")
				.add("Headers", headers)
				.add("Data", requestBody)
				.add("ExtractJsonPath", extractJsonPath)
				.build().toString();
		Output<JsonObject> output = ctx.run("HttpRequest", input);

		assertEquals(output.getPayload().getString("StatusCode"),"200");
		assertEquals(123L, output.getPayload().getInt("myLong"));
		assertEquals("some value", output.getPayload().getString("myString"));
		assertTrue(output.getPayload().getBoolean("myBoolean"));
	}

	@Test
	public void httpPostMultiPartFormData() throws Exception {
		JsonObject multiFormData = Json.createObjectBuilder().add("input1", "My form value 1")
				.add("input2", "My form value 2")
				.add("filepath", Paths.get(this.getClass().getResource("/test.txt").toURI()).toString())
				.build();
		String input = Json.createObjectBuilder().add("URL", "https://postman-echo.com/post")
				.add("Method", "POST")
				.add("MultiPartFormData", multiFormData)
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
