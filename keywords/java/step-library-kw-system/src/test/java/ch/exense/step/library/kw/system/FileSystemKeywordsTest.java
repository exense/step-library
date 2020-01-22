package ch.exense.step.library.kw.system;

import java.io.File;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class FileSystemKeywordsTest {

	private ExecutionContext ctx;

	@Before
	public void setUp() {
		ctx = KeywordRunner.getExecutionContext(FileSystemKeywords.class);
	}

	@After
	public void tearDown() {
		ctx.close();
	}

	@Test
	public void test_sed() throws Exception {
		String path = new File(getClass().getClassLoader().getResource("package.json").getFile()).getAbsolutePath();

		System.out.println(path);
		
		JsonObject input = Json.createObjectBuilder().add("File", path).add("Regex", "\"version\" *: *\"[^\"]*\"")
				.add("Replacement",  "\"version\": \"test\"").build();

		Output<JsonObject> output = ctx.run("Sed_file", input.toString());
		
		System.out.println(output.getPayload());
	}
}
