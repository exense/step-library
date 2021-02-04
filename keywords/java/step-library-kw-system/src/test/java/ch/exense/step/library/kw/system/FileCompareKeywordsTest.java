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
        Output<JsonObject> output;
        JsonObject input;

        input = Json.createObjectBuilder().add("File", path)
                .add("/root/otherTest", "otherTestValue")
                .add("//otherTest", "otherTestValue")
                .add("//otherTest/@id", "myId")
                .add("//testEmpty1", "")
                .add("//testEmpty2", "")
                .add("//testEmpty3/@empty", "")
                .add("//testMultiple", ".*").build();
        output = ctx.run("Validate_XML", input.toString());
        System.out.println(output.getPayload());
        assert output.getError() == null;

        input = Json.createObjectBuilder().add("File", path)
				.add("//testMultiple", "[test1,test2,...]")
                .add("//testMultipleDuplicate", "[testSameValue*]").build();
        output = ctx.run("Validate_XML", input.toString());
        System.out.println(output.getPayload());
        assert output.getError() == null;

        input = Json.createObjectBuilder().add("File", path)
                .add("//testMultiple", "[test1,test2,test3]")
                .add("//testMultipleDuplicate", "[testSameValue,testSameValue,testSameValue]").build();
        output = ctx.run("Validate_XML", input.toString());
        System.out.println(output.getPayload());
        assert output.getError() == null;
    }

    @Test
    public void test_extract_xml() throws Exception {
        String path = new File(getClass().getClassLoader().getResource("test.xml").getFile()).getAbsolutePath();
        Output<JsonObject> output;
        JsonObject input;

        input = Json.createObjectBuilder().add("File", path)
                .add("/root/otherTest", "first")
                .add("//otherTest/@id", "all")
                .add("//testMultiple", "all").build();
        output = ctx.run("Extract_XML", input.toString());
        System.out.println(output.getPayload());
        assert output.getError() == null;
    }
}
