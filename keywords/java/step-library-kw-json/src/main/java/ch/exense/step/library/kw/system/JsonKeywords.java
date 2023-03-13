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

import ch.exense.step.library.commons.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.*;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class JsonKeywords extends AbstractKeyword {

    private static final String FILE_OPT = "File";
    private static final String JSON_OPT = "Json";

    private static final Configuration conf = Configuration.defaultConfiguration().setOptions(Option.ALWAYS_RETURN_LIST);

    private List<String> listOptionsExtract = Arrays.asList(new String[]{FILE_OPT, JSON_OPT});

    private Object getJson(boolean writable) {
        String fileName = input.getString(FILE_OPT, "");
        File file = null;
        String jsonContent = null;
        if (!fileName.isEmpty()) {

            file = new File(fileName);

            if (!file.exists()) {
                output.setBusinessError("File \"" + fileName + "\" do not exist.");
                return null;
            }
            if (!file.canRead()) {
                output.setBusinessError("File \"" + fileName + "\" is not readable.");
                return null;
            }
            if (writable && !file.canWrite()) {
                output.setBusinessError("File \"" + fileName + "\" is not writable.");
                return null;
            }
        } else {
            jsonContent = input.getString(JSON_OPT, "");
            if (jsonContent.isEmpty()) {
                throw new BusinessException("One of the input parameter '" + FILE_OPT + "' or '" + JSON_OPT + "' should exist");
            }
        }

        if (file != null) {
            try {
                jsonContent =  new String(Files.readAllBytes(file.toPath()));
            } catch (IOException io) {
                output.setError("IOException when trying to parse the file: " + io.getMessage(), io);
                return null;
            } catch (OutOfMemoryError oom) {
                output.setError("OutOfMemoryError when trying to parse the file. The file is too big: " + oom.getMessage(), oom);
                return null;
            }
        }
        try {
            return Configuration.defaultConfiguration().jsonProvider().parse(jsonContent);
        } catch (InvalidJsonException e) {
            output.setError("InvalidJsonException when trying to parse the file: " + e.getMessage(), e);
            return null;
        }
    }

    @Keyword(schema = "{\"properties\":{" +
            "\""+FILE_OPT + "\":{\"type\":\"string\"}," +
            "\""+JSON_OPT + "\":{\"type\":\"string\"}" +
            "},\"oneOf\": [{\"required\":[\"" + FILE_OPT + "\"]}," +
            "            {\"required\":[\"" + JSON_OPT + "\"]}]" +
            "}",
        description = "Extract the value given a list of jsonPath. See https://github.com/json-path/JsonPath")
    /**
     * Extract the value given a list of jsonPath.
     *
     * This keyword can read json from a file (using the "File" input) or directly from the "Json" input.
     * It will then evaluate any other input parameters as a set of output name and JsonPath to extract the text content.
     * If the JsonPath leads to multiple values, they are returned in a json object "[{"a":1},{"a":5}]"
     *
     * @See <a href="https://github.com/json-path/JsonPath"/>
     */
    public void Extract_Json() throws Exception {
        Object json = getJson(false);
        if (json == null) return;

        for (String jsonPathKey : input.keySet().stream()
                .filter(key -> !listOptionsExtract.contains(key))
                .toArray(String[]::new)) {

            List<Object> list;
            String path = input.getString(jsonPathKey);
            try {
                list = JsonPath.using(conf).parse(json).read(path);
            } catch (InvalidPathException e) {
                output.setError("Invalid jsonPath '" + path + "': " + e.getMessage(), e);
                return;
            }
            if (list.size() == 0) {
                output.setBusinessError("jsonPath '" + path + "' not found!");
                return;
            } else {
                output.add(jsonPathKey, "["+list.stream().map(obj -> {
                    try {
                        return new ObjectMapper().writeValueAsString(obj);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.joining(","))+"]");
            }
        }
    }


    @Keyword(schema = "{\"properties\":{\""+FILE_OPT+"\":{\"type\":\"string\"},\""+JSON_OPT+"\":{\"type\":\"string\"}},\n" +
            "\"oneOf\": [{\"required\":[\""+FILE_OPT+"\"]}," +
            "            {\"required\":[\""+JSON_OPT+"\"]}]" +
            "}",
            description = "\"Replace the value of xml nodes given a list of jsonPath. See https://github.com/json-path/JsonPath")
    /**
     * Replace the value of json given a list of jsonPaths.
     *
     * This keyword can read json from a file (using the "File" input) or directly from the "Json" input.
     * It will then evaluate any other input parameters as a set of jsonPath and value to be replaced.
     * Note that the jsonPath should return only one node
     *
     * @See <a href="https://github.com/json-path/JsonPath"/>
     */
    public void Replace_Json() throws Exception {
        Object json = getJson(true);
        if (json == null) return;

        DocumentContext context = JsonPath.using(conf).parse(json);

        for (String jsonPath : input.keySet().stream()
                .filter(key -> !listOptionsExtract.contains(key))
                .toArray(String[]::new)) {

            Object value;
            try {
                value = input.getString(jsonPath);
            } catch (ClassCastException e) {
                try {
                    value = input.getInt(jsonPath);
                } catch (ClassCastException e2) {
                    value = input.getBoolean(jsonPath);
                }
            }
            try {
                context = context.set(jsonPath,value);
            } catch (InvalidPathException e) {
                output.setError("Invalid jsonPath '" + jsonPath + "': " + e.getMessage(), e);
                return;
            }
        }
        String fileName=input.getString("File","");
        if (fileName.isEmpty()) {
            output.add("Transformed",context.jsonString());
        } else {
            try (BufferedWriter br = new BufferedWriter(new FileWriter(fileName))) {
                br.write(context.jsonString());
            }
        }
    }
}
