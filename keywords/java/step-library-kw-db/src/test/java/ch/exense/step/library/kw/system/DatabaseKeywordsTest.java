package ch.exense.step.library.kw.system;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

import javax.json.JsonObject;
import java.util.Map;

public class DatabaseKeywordsTest {

    private ExecutionContext ctx = KeywordRunner.getExecutionContext(Map.of("root_Password","xWVK!2Xi23"),DatabaseKeywords.class);

    @After
    public void tearDown() {
        ctx.close();
    }

    @Test
    public void test() throws Exception {
        Output<JsonObject> output = ctx.run("ExecuteQuery", "{\"ConnectionString\":\"jdbc:mysql://34.65.202.8/mysql\", " +
                "\"Query\":\"SELECT * from user where user like ('root')\", " +
                "\"Username\":\"root\", \"ResultLimit\":\"1\"}");
        Assert.assertTrue(output.getPayload().toString().contains("root"));
    }
}
