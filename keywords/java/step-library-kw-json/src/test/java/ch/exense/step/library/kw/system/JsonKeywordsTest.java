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

public class JsonKeywordsTest {

    private ExecutionContext ctx;

    @Before
    public void setUp() {
        ctx = KeywordRunner.getExecutionContext(JsonKeywords.class);
    }

    @After
    public void tearDown() {
        ctx.close();
    }

    @Test
    public void test_extract_value() throws Exception {
        String path = new File(getClass().getClassLoader().getResource("test.json").getFile()).getAbsolutePath();
        Output<JsonObject> output;
        JsonObject input;

        input = Json.createObjectBuilder().add("File", path)
                .add("author","$.store.book[*].author")
                .add("cheapBooks","$.store.book[?(@.price < 10)]")
                .add("firstAuthor","$.store.book[0].author")
                .build();
        output = ctx.run("Extract_Json", input.toString());
        System.out.println(output.getPayload());
        assert output.getError() == null;
        assert output.getPayload().getString("author")
                .equals("[\"Nigel Rees\",\"Evelyn Waugh\",\"Herman Melville\",\"J. R. R. Tolkien\"]");
        assert output.getPayload().getString("cheapBooks")
                .equals("[{\"category\":\"reference\",\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"price\":8.95}," +
                        "{\"category\":\"fiction\",\"author\":\"Herman Melville\",\"title\":\"Moby Dick\",\"isbn\":\"0-553-21311-3\",\"price\":8.99}]");
        assert output.getPayload().getString("firstAuthor")
                .equals("[\"Nigel Rees\"]");
    }

}
