package ch.exense.step.library.playwright;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import org.junit.Assert;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import step.handlers.javahandler.Keyword;
import step.handlers.javahandler.jsonschema.KeywordJsonSchemaCreator;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The published declarations ({@code automation-package.yaml}) against the schemas Step generates from the
 * {@code @Input} parameters: one statement of each input, checked here instead of kept in sync by hand.
 */
public class PlaywrightKeywordsSchemaTest {

    private static Map<String, JsonObject> generated() throws Exception {
        KeywordJsonSchemaCreator creator = new KeywordJsonSchemaCreator();
        Map<String, JsonObject> schemas = new HashMap<>();
        for (Method method : PlaywrightKeywords.class.getMethods()) {
            Keyword keyword = method.getAnnotation(Keyword.class);
            if (keyword != null) {
                schemas.put(keyword.name(), creator.createJsonSchemaForKeyword(method));
            }
        }
        return schemas;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> declared() throws Exception {
        try (InputStream in = PlaywrightKeywordsSchemaTest.class.getResourceAsStream("/automation-package.yaml")) {
            Map<String, Object> descriptor = new Yaml().load(in);
            Map<String, Map<String, Object>> declarations = new HashMap<>();
            for (Map<String, Object> entry : (List<Map<String, Object>>) descriptor.get("keywords")) {
                Map<String, Object> script = (Map<String, Object>) entry.get("GeneralScript");
                Assert.assertEquals("step-library-playwright.jar", script.get("scriptFile"));
                declarations.put((String) script.get("name"), script);
            }
            return declarations;
        }
    }

    private static Set<String> keys(JsonObject schema, String member) {
        if (!schema.containsKey(member)) {
            return Set.of();
        }
        return "properties".equals(member)
            ? new TreeSet<>(schema.getJsonObject(member).keySet())
            : schema.getJsonArray(member).getValuesAs(JsonString.class).stream().map(JsonString::getString)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void declarationsMatchTheKeywordInputs() throws Exception {
        Map<String, JsonObject> generated = generated();
        Map<String, Map<String, Object>> declared = declared();
        Assert.assertEquals(new TreeSet<>(generated.keySet()), new TreeSet<>(declared.keySet()));

        for (Map.Entry<String, JsonObject> keyword : generated.entrySet()) {
            Map<String, Object> schema = (Map<String, Object>) declared.get(keyword.getKey()).get("schema");
            Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) schema.get("properties");
            Assert.assertEquals(keyword.getKey(), keys(keyword.getValue(), "properties"),
                new TreeSet<>(properties.keySet()));
            Assert.assertEquals(keyword.getKey(), keys(keyword.getValue(), "required"),
                new TreeSet<>((List<String>) schema.get("required")));
            for (String property : properties.keySet()) {
                Assert.assertEquals(keyword.getKey() + "." + property,
                    keyword.getValue().getJsonObject("properties").getJsonObject(property).getString("type"),
                    properties.get(property).get("type"));
            }
        }
    }
}
