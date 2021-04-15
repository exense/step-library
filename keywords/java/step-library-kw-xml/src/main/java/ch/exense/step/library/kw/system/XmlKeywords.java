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
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.util.*;

public class XmlKeywords extends AbstractKeyword {

    private Document getDocument(boolean writable) {
        String fileName = input.getString("File", "");
        File file = null;
        String xmlContent = null;
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
            xmlContent = input.getString("Xml", "");
            if (xmlContent.isEmpty()) {
                throw new BusinessException("The input parameter 'File' of 'Xml' should exist");
            }
        }
        Document doc = null;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            if (file!=null) {
                doc = builder.parse(file);
            } else {
                assert xmlContent!=null;
                doc = builder.parse( new InputSource(new StringReader(xmlContent)));
            }
        } catch (IOException io) {
            output.setError("IOException when trying to parse the XML: " + io.getMessage(), io);
            return null;
        } catch (SAXException sax) {
            output.setError("IOException when trying to parse the XML: " + sax.getMessage(), sax);
            return null;
        } catch (ParserConfigurationException parse) {
            output.setError("ParserConfigurationException when trying to parse the XML: " + parse.getMessage(), parse);
        } catch (Exception e) {
            output.setError("Unknown exception when trying to parse the XML",e);
        }
        return doc;
    }

    @Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"},\"Xml\":{\"type\":\"string\"}},\n" +
            "\"oneOf\": [{\"required\":[\"File\"]}," +
            "            {\"required\":[\"Xml\"]}]" +
            "}")
    public void Replace_XML() throws Exception {

        Document doc = getDocument(true);
        if (doc == null) return;

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xPath = xpathFactory.newXPath();

        for (Object xpathObj : input.keySet().stream().filter(key -> !key.equals("File") && !key.equals("Xml")).toArray()) {
            String xpathString = (String) xpathObj;
            String value = input.getString(xpathString);

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
            } else if (nodeList.getLength() == 1) {
                nodeList.item(0).setTextContent(value);
            } else {
                output.setBusinessError("Multiple xpath were found matching '" + xpathString + "'");
                return;
            }
        }
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        String fileName=input.getString("File","");
        if (fileName.isEmpty()) {
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            output.add("Transformed",writer.getBuffer().toString());
        } else {
            transformer.transform(new DOMSource(doc), new StreamResult(new File(fileName)));
        }
    }

    @Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"},\"Xml\":{\"type\":\"string\"}},\n" +
            "\"oneOf\": [{\"required\":[\"File\"]}," +
            "            {\"required\":[\"Xml\"]}]" +
            "}")
    public void Extract_XML() throws Exception {

        Document doc = getDocument(false);
        if (doc == null) return;

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xPath = xpathFactory.newXPath();

        for (Object xpathObj : input.keySet().stream().filter(key -> !key.equals("File") && !key.equals("Xml")).toArray()) {
            String name = (String) xpathObj;

            String xpathString = input.getString(name);

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
            } else if (nodeList.getLength() == 1) {
                output.add(name, nodeList.item(0).getTextContent());
            } else {
                List<String> actualValues = new LinkedList<>();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    actualValues.add(nodeList.item(i).getTextContent());
                }
                output.add(name, actualValues.toString());
            }
        }
    }

    @Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"},\"Xml\":{\"type\":\"string\"}},\n" +
            "\"oneOf\": [{\"required\":[\"File\"]}," +
            "            {\"required\":[\"Xml\"]}]" +
            "}}")
    public void Validate_XML() throws Exception {

        Document doc = getDocument(false);
        if (doc == null) return;

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xPath = xpathFactory.newXPath();

        for (Object xpathObj : input.keySet().stream().filter(key -> !key.equals("File") && !key.equals("Xml")).toArray()) {
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
                List<String> actualValues = new LinkedList<>();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    actualValues.add(nodeList.item(i).getTextContent());
                }

                boolean exactCount = true;
                List<String> expectedValues;
                //only check the presence of the values given:
                if (expected.endsWith(",...]")) {
                    exactCount = false;
                    expectedValues = Arrays.asList(expected.substring(1, expected.length() - ",...]".length()).split(","));
                }
                // Consider all values to be equals:
                else if (expected.endsWith("*]") && !expected.contains(",")) {
                    expectedValues = Collections.nCopies(nodeList.getLength(), expected.substring(1, expected.length() - "*]".length()));
                }
                // Otherwise split by ","
                else {
                    expectedValues = Arrays.asList(expected.substring(1, expected.length() - 1).split(","));
                }
                if (exactCount && expectedValues.size() != nodeList.getLength()) {
                    output.setBusinessError("Error when comparing xpath '" + xpathString + "': "
                            + expectedValues.size() + " values were expected, " + nodeList.getLength() + " were found.\n"
                            + "Use the " + expected.substring(0, expected.length() - 1) + ",...] notation to only validate a subset of values"
                            + " or the " + expected.substring(0, expected.length() - 1) + "*] notation to test that all values are expected to be the same");
                    return;
                }
                String actualValuesString = actualValues.toString();
                for (String value : expectedValues) {
                    if (!actualValues.contains(value)) {
                        output.setBusinessError("Error when comparing xpath '" + xpathString + "': "
                                + "value '" + value + "' was not found. Expected was '" + expectedValues + "' and actual values were '" + actualValuesString + "'");
                        return;
                    }
                    actualValues.remove(value);
                }
            }
            // case: one value
            else if (nodeList.getLength() == 1) {
                String result = nodeList.item(0).getTextContent();
                if (!result.equals(expected)) {
                    output.setBusinessError("Error when comparing xpath '" + xpathString
                            + "value was expected to be: '" + expected + "' but was '" + result + "'");
                    return;
                }
            } else {
                output.setBusinessError("Error when comparing xpath '" + xpathString + "': "
                        + "the xpath was supposed to be unique, but was found " + nodeList.getLength() + " times.\n"
                        + "Use the [" + expected + ",...] notation to validate the presence of one value"
                        + " or the [" + expected + "*] notation to test that all values are expected to be the same");
                return;
            }
        }
    }
}
