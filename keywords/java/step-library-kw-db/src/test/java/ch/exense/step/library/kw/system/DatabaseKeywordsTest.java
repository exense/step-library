package ch.exense.step.library.kw.system;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

import javax.json.JsonObject;

public class DatabaseKeywordsTest {

    private ExecutionContext ctx;

    @Before
    public void setUp() {
        ctx = KeywordRunner.getExecutionContext(DatabaseKeywords.class);
    }

    @After
    public void tearDown() {
        ctx.close();
    }

    @Test
    public void test() throws Exception {
        Output<JsonObject> output = ctx.run("ExecuteQuery", "{\"ConnectionString\":\"jdbc:mysql://mysql-qa1.exense.ch/TEST\", " +
                "\"Query\":\"SELECT * from mysqldataset\", " +
                "\"Username\":\"root\", \"Password\":\"init\", \"ResultLimit\":\"1\"}");
        Assert.assertEquals("{\"ColumnCount\":2,\"ResultAsJson\":\"[{\\\"ID\\\":\\\"1\\\",\\\"VALUE\\\":\\\"value1\\\"}]\"}", output.getPayload().toString());
    }
}
