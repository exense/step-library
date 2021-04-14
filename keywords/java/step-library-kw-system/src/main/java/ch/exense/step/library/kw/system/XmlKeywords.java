package ch.exense.step.library.kw.system;
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

    @Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"},\"Xml\":{\"type\":\"string\"}}}")
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
        transformer.transform(new DOMSource(doc), new StreamResult(new File(input.getString("File"))));
        // transformer.
    }

    @Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"},\"Xml\":{\"type\":\"string\"}}}")
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

    @Keyword(schema = "{\"properties\":{\"File\":{\"type\":\"string\"},\"Xml\":{\"type\":\"string\"}}}")
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
                    output.setBusinessError("Error when comparing xpath '" + xpathString + "': "
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
