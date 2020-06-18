package ch.exense.step.library.kw.system;

import java.io.File;
import java.nio.file.Paths;

import javax.json.Json;
import javax.json.JsonObject;
import javax.swing.plaf.metal.MetalIconFactory.FolderIcon16;

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
		
		JsonObject input = Json.createObjectBuilder().add("File", path).add("Regex", "\"version\" *: *\"[^\"]*\"")
				.add("Replacement",  "\"version\": \"test\"").build();

		Output<JsonObject> output = ctx.run("Sed_file", input.toString());
		
		System.out.println(output.getPayload());
	}
	
	@Test
	public void test_zip() throws Exception {
		String path = new File(getClass().getClassLoader().getResource("package.json").getFile()).getParent();
		
		JsonObject input = Json.createObjectBuilder().add("Folder", path).add("Destination", path+ File.separator +".." + File.separator + "test_zip.zip").build();
		
		Output<JsonObject> output = ctx.run("Zip_file", input.toString());
		
		System.out.println(output.getPayload());
	}
	
	@Test
	public void test_unzip() throws Exception {
		String path = new File(getClass().getClassLoader().getResource("test.zip").getFile()).getPath();
		
		JsonObject input = Json.createObjectBuilder().add("File", path).add("Destination", path+ File.separator +".." + File.separator).build();

		Output<JsonObject> output = ctx.run("Unzip_file", input.toString());
		
		System.out.println(output.getPayload());
	}
}
