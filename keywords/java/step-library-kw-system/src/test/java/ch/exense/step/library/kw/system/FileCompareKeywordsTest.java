/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package ch.exense.step.library.kw.system;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.File;

public class FileCompareKeywordsTest {

	private ExecutionContext ctx;

	@Before
	public void setUp() {
		ctx = KeywordRunner.getExecutionContext(FileCompareKeywords.class);
	}

	@After
	public void tearDown() {
		ctx.close();
	}

	@Test
	public void test_validate_xml() throws Exception {
		String path = new File(getClass().getClassLoader().getResource("test.xml").getFile()).getAbsolutePath();
		
		JsonObject input = Json.createObjectBuilder().add("File", path)
				.add("/root/otherTest", "otherTest")
				.add("//otherTest/@id", "myId")
				.add("//test", "test").build();

		Output<JsonObject> output = ctx.run("Validate_XML", input.toString());

		System.out.println(output.getPayload());
	}
}
