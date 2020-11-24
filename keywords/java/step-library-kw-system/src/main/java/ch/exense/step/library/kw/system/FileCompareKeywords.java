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

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileCompareKeywords extends AbstractKeyword {

    @Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"}},\"required\":[\"File\"]}")
    public void Validate_XML() throws Exception {
        String fileName = input.getString("File");

        File file = new File(fileName);

        if (!file.exists()) {
            output.setBusinessError("File \"" + fileName + "\" do not exist.");
            return;
        }
        if (!file.canRead()) {
            output.setBusinessError("File \"" + fileName + "\" is not readable.");
            return;
        }

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc;
        try {
            doc = builder.parse(file);
        } catch (IOException io) {
            output.setError("IOException when trying to parse the File '" + file.getName() + "': " + io.getMessage(), io);
            return;
        } catch (SAXException sax) {
            output.setError("Invalid XML found when parsing the File '" + file.getName() + "': " + sax.getMessage(), sax);
            return;
        }

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xPath = xpathFactory.newXPath();

        for (Object xpathObj : input.keySet().stream().filter(key -> !key.equals("File")).toArray()) {
            String xpathString = (String) xpathObj;
            String expected = input.getString(xpathString);

            NodeList nodeList;
            try {
                XPathExpression expr = xPath.compile(xpathString);
                nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            } catch (XPathExpressionException expr) {
                output.setError("Invalid xpath found'" + xpathString + "': " + expr.getMessage(), expr);
                return;
            }
            if (nodeList.getLength() == 0) {
                output.setBusinessError("Xpath '" + xpathString + "' not found in the document");
                return;
            }

            // case: just testing the presence
            if (expected.equals(".*")) {
                return;
            }
            // case: list of values
            else if (expected.startsWith("[") && expected.endsWith("]")) {
                List<String> actualValues = new ArrayList<>();
                for (int i=0;i<nodeList.getLength();i++) {
                    actualValues.add(nodeList.item(i).getTextContent());
                }

                boolean exactCount = true;
                List<String> expectedValues;
                if (expected.endsWith(",...]")) {
                    exactCount = false;
                    expectedValues = Arrays.asList(expected.substring(1, expected.length() - ",...]".length()).split(","));
                } else {
                    expectedValues = Arrays.asList(expected.substring(1, expected.length() - 1).split(","));
                }
                if (exactCount && expectedValues.size()!=nodeList.getLength()) {
                    output.setBusinessError("Error when comparing xpath '" + xpathString + "': "
                            + expectedValues.size() + " values were expected, "+nodeList.getLength()+" were found.\n"
                            + "Use the "+expected.substring(0,expected.length()-1)+",...]"+" notation to only validate a subset of values");
                    return;
                }
                if (!actualValues.containsAll(expectedValues)) {
                    output.setBusinessError("Error when comparing xpath '" + xpathString + "': "
                            + "values were expected to contains: '" + expectedValues + "' but were '" + actualValues + "'");
                    return;
                }
            }
            // case: one value
            else if (nodeList.getLength() == 1) {
                String result = nodeList.item(0).getTextContent();
                if (!result.equals(expected)) {
                    output.setBusinessError("Error when comparing xpath '" + xpathString + "': "
                            + "value was expected to be: '" + expected + "' but was '" + result + "'");
                    return;
                }
            } else {
                output.setBusinessError("Error when comparing xpath '" + xpathString + "': "
                        + "the xpath was supposed to be unique, but was found " + nodeList.getLength() + " times.\n"
                        + "Use the ["+expected+",...] notation to validate the presence of one value");
                return;
            }
        }
    }
}
