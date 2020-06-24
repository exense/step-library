package ch.exense.step.examples.http.keywords;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;

import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class HttpClientKeywordTest {

	private ExecutionContext ctx = KeywordRunner.getExecutionContext(HttpClientKeyword.class);
	
	@Test
	public void simpleHttpGetRequest() throws Exception {
		String input = Json.createObjectBuilder().add("URL", "https://www.google.ch/").build().toString();
		Output<JsonObject> output = ctx.run("HttpRequest", input);
		assertEquals(output.getPayload().getString("StatusCode"),"200");
	}

	@Test
	public void simpleHttpGetRequestWithExplicitClientInit() throws Exception {
		// Open http client
		Output<JsonObject> output = ctx.run("InitHttpClient", "{}");

		// HTTP GET www.google.ch
		String input = Json.createObjectBuilder().add("URL", "https://www.google.ch/")
			.add("Method", "GET")
			.add("Name", "Google home")
			.add("Check_Title", "<title>Google</title>")
			.add("Extract_Title", "<title>(.+?)</title>").build().toString();
		output = ctx.run("HttpRequest", input);

		ctx.run("CloseHttpClient", "{}");
		
		assertEquals(output.getPayload().getString("StatusCode"),"200");
		assertTrue(output.getPayload().getBoolean("Check_Title"));
		assertEquals(output.getPayload().getString("Extract_Title"),"Google");
	}
	
	@Test
	public void simpleHttpGetWithHeaders() throws Exception {
		String input = Json.createObjectBuilder()
				.add("URL", "https://postman-echo.com/headers")
				.add("Header_myHeader", "MyHeaderValue")
				.build().toString();
		Output<JsonObject> output = ctx.run("HttpRequest", input);
		assertTrue(output.getPayload().getString("Response").contains("\"myheader\":\"MyHeaderValue\""));
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
		assertEquals(output.getPayload().getString("StatusCode"),"200");
		assertTrue(output.getPayload().getString("Response").contains("My form value 1"));
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
				.add("BasicAuthPassword", "password")
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
			.add("BasicAuthPassword", "password")
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
		ctx.run("InitHttpClient", input.toString());

		input = Json.createObjectBuilder().add("URL", "https://mycustomhost.ch/")
			.add("Method", "GET").build().toString();
		
		Exception actual = null;
		try {
			ctx.run("HttpRequest", input);
		} catch(Exception e) {
			actual = e;
		}
		assertTrue(actual.getMessage().startsWith("Connect to mycustomhost.ch:443 [/0.0.0.0] failed: Connection refused"));
	}

}
