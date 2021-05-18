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

public class XmlKeywordsTest {

    private ExecutionContext ctx;

    @Before
    public void setUp() {
        ctx = KeywordRunner.getExecutionContext(XmlKeywords.class);
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
        assert output.getError() == null;

        input = Json.createObjectBuilder().add("File", path)
				.add("//testMultiple", "[test1,test2,...]")
                .add("//testMultipleDuplicate", "[testSameValue*]").build();
        output = ctx.run("Validate_XML", input.toString());
        assert output.getError() == null;

        input = Json.createObjectBuilder().add("File", path)
                .add("//testMultiple", "[test1,test2,test3]")
                .add("//testMultipleDuplicate", "[testSameValue,testSameValue,testSameValue]").build();
        output = ctx.run("Validate_XML", input.toString());
        assert output.getError() == null;
    }

    @Test
    public void test_extract_value() throws Exception {
        String path = new File(getClass().getClassLoader().getResource("test.xml").getFile()).getAbsolutePath();
        Output<JsonObject> output;
        JsonObject input;

        input = Json.createObjectBuilder().add("File", path)
                .add("value1","/root/otherTest")
                .add("value2","//otherTest/@id")
                .add("value3","//testMultiple").build();
        output = ctx.run("Extract_XML", input.toString());
        System.out.println(output.getPayload());
        assert output.getError() == null;
        assert output.getPayload().getString("value1").equals("otherTestValue");
        assert output.getPayload().getString("value2").equals("myId");
        assert output.getPayload().getString("value3").equals("[test1, test2, test3]");
    }

    @Test
    public void test_extract_xml() throws Exception {
        String path = new File(getClass().getClassLoader().getResource("test.xml").getFile()).getAbsolutePath();
        Output<JsonObject> output;
        JsonObject input;

        input = Json.createObjectBuilder().add("File", path)
                .add("ExtractXml",true)
                .add("value1","/root")
                .add("value2","//otherTest")
                .add("value3","//testMultiple").build();
        output = ctx.run("Extract_XML", input.toString());
        System.out.println(output.getPayload());
        assert output.getError() == null;
        assert output.getPayload().getString("value1").equals("otherTestValue");
        assert output.getPayload().getString("value2").equals("myId");
        assert output.getPayload().getString("value3").equals("[test1, test2, test3]");
    }
    @Test
    public void test_extract_xml_text() throws Exception {
        String xml = "<root>\n" +
                "    <testEmpty1></testEmpty1>\n" +
                "    <testEmpty2/>\n" +
                "    <testEmpty3 empty=\"\" >value</testEmpty3>\n" +
                "    <otherTest id=\"myId\" >otherTestValue</otherTest>\n" +
                "    <testMultiple>test1</testMultiple>\n" +
                "    <testMultiple>test2</testMultiple>\n" +
                "    <testEmbededMultiple>\n" +
                "        <testMultiple>test3</testMultiple>\n" +
                "        <testMultipleDuplicate>testSameValue</testMultipleDuplicate>\n" +
                "    </testEmbededMultiple>\n" +
                "    <testMultipleDuplicate>testSameValue</testMultipleDuplicate>\n" +
                "    <testMultipleDuplicate>testSameValue</testMultipleDuplicate>\n" +
                "</root>";
        Output<JsonObject> output;
        JsonObject input;

        input = Json.createObjectBuilder().add("Xml", xml)
                .add("value1","/root/otherTest")
                .add("value2","//otherTest/@id")
                .add("value3","//testMultiple").build();
        output = ctx.run("Extract_XML", input.toString());
        assert output.getError() == null;
        assert output.getPayload().getString("value1").equals("otherTestValue");
        assert output.getPayload().getString("value2").equals("myId");
        assert output.getPayload().getString("value3").equals("[test1, test2, test3]");
    }

    @Test
    public void test_replace_xml() throws Exception {
        String path = new File(getClass().getClassLoader().getResource("test.xml").getFile()).getAbsolutePath();
        Output<JsonObject> output;
        JsonObject input;

        input = Json.createObjectBuilder().add("File", path)
                .add("value1","/root/otherTest")
                .add("value2","//otherTest/@id")
                .add("value3","(//testMultiple)[1]").build();
        output = ctx.run("Extract_XML", input.toString());
        assert output.getError() == null;

        String xpath1Value = output.getPayload().getString("value1");
        String xpath2Value = output.getPayload().getString("value2");
        String xpath3Value = output.getPayload().getString("value3");

        input = Json.createObjectBuilder().add("File", path)
                .add("/root/otherTest","changed value1")
                .add("//otherTest/@id","changed value2")
                .add("(//testMultiple)[1]","changed value3").build();
        output = ctx.run("Replace_XML", input.toString());
        assert output.getError() == null;

        input = Json.createObjectBuilder().add("File", path)
                .add("value1","/root/otherTest")
                .add("value2","//otherTest/@id")
                .add("value3","(//testMultiple)[1]").build();
        output = ctx.run("Extract_XML", input.toString());
        assert output.getError() == null;
        assert output.getPayload().getString("value1").equals("changed value1");
        assert output.getPayload().getString("value2").equals("changed value2");
        assert output.getPayload().getString("value3").equals("changed value3");

        input = Json.createObjectBuilder().add("File", path)
                .add("/root/otherTest",xpath1Value)
                .add("//otherTest/@id",xpath2Value)
                .add("(//testMultiple)[1]",xpath3Value).build();
        output = ctx.run("Replace_XML", input.toString());
        assert output.getError() == null;
    }

    @Test
    public void test_replace_xml_text() throws Exception {
        String xml = "<root>\n" +
                "    <testEmpty1></testEmpty1>\n" +
                "    <testEmpty2/>\n" +
                "    <testEmpty3 empty=\"\" >value</testEmpty3>\n" +
                "    <otherTest id=\"myId\" >otherTestValue</otherTest>\n" +
                "    <testMultiple>test1</testMultiple>\n" +
                "    <testMultiple>test2</testMultiple>\n" +
                "    <testEmbededMultiple>\n" +
                "        <testMultiple>test3</testMultiple>\n" +
                "        <testMultipleDuplicate>testSameValue</testMultipleDuplicate>\n" +
                "    </testEmbededMultiple>\n" +
                "    <testMultipleDuplicate>testSameValue</testMultipleDuplicate>\n" +
                "    <testMultipleDuplicate>testSameValue</testMultipleDuplicate>\n" +
                "</root>";
        Output<JsonObject> output;
        JsonObject input;

        input = Json.createObjectBuilder().add("Xml", xml)
                .add("/root/otherTest","changed value1")
                .add("//otherTest/@id","changed value2")
                .add("(//testMultiple)[1]","changed value3").build();
        output = ctx.run("Replace_XML", input.toString());
        assert output.getError() == null;
        System.out.println(xml);
        System.out.println(output.getPayload());

        String newXml = output.getPayload().getString("Transformed");

        input = Json.createObjectBuilder().add("Xml", newXml)
                .add("value1","/root/otherTest")
                .add("value2","//otherTest/@id")
                .add("value3","(//testMultiple)[1]").build();
        output = ctx.run("Extract_XML", input.toString());
        assert output.getError() == null;
        System.out.println(output.getPayload());
        assert output.getPayload().getString("value1").equals("changed value1");
        assert output.getPayload().getString("value2").equals("changed value2");
        assert output.getPayload().getString("value3").equals("changed value3");
    }
}
